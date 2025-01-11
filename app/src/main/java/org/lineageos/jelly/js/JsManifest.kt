/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.js

import android.graphics.BitmapFactory
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.jelly.PwaManifest
import org.lineageos.jelly.webview.WebViewExtActivity
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.reflect.cast

@Keep
class JsManifest(
    private val activity: WebViewExtActivity,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @JavascriptInterface
    fun onResolved(
        baseUrl: String, startUrl: String, display: String,
        themeColor: String, shortName: String, name: String
    ) {
        val url = URI(baseUrl).resolve(startUrl).toString()
        val pwaManifest = PwaManifest(url, display, themeColor, shortName, name)
        activity.setPwaManifest(pwaManifest)
    }

    @JavascriptInterface
    fun onError() {
        activity.setPwaManifest(null)
    }

    @JavascriptInterface
    fun onIconResolved(baseUrl: String, iconUrl: String) {
        val url = URI(baseUrl).resolve(iconUrl).toString()
        scope.launch {
            val bitmap = getIconBitmap(url)
            if (bitmap != null) {
                withContext(Dispatchers.Main) {
                    activity.onFaviconLoaded(bitmap)
                }
            }
        }
    }

    private fun getIconBitmap(url: String) = runCatching {
        val connection = HttpURLConnection::class.cast(
            URL(url).openConnection()
        )
        connection.connect()
        if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
        connection.inputStream.buffered().use {
            BitmapFactory.decodeStream(it)
        }
    }.getOrNull()

    companion object {
        const val INTERFACE = "JsManifest"
        const val URL = "(() => document.querySelector('link[rel=\"manifest\"]')?.href ?? '')"

        private const val MONKEY_PATCH_ONCE_KEY = "JsManifestMonkeyPatch"
        const val SCRIPT = """
            (async () => {
                if (window.$MONKEY_PATCH_ONCE_KEY) return;

                window.$MONKEY_PATCH_ONCE_KEY = true;

                try {
                    const baseUrl = $URL();
                    if (!baseUrl) throw Error('Manifest url not found');

                    const res = await fetch(baseUrl);
                    const manifest = await res.json();

                    const startUrl = manifest.start_url	?? window.location.href;
                    const display = manifest.display ?? 'browser';
                    const themeColor = manifest.theme_color	?? '#000000';
                    const shortName = manifest.short_name ?? document.title;
                    const name = manifest.name ?? document.title;

                    $INTERFACE.onResolved(
                        baseUrl, startUrl, display,
                        themeColor, shortName, name
                    );

                    let iconUrl = null;
                    let minWidth = 33;
                    const maxWidth = 333;
                    (manifest.icons ?? []).forEach((icon) => {
                        if (!icon.sizes) return;
                        if (icon.purpose?.includes('monochrome')) return;
                        const width = Number(icon.sizes.split('x')[0]);
                        if (width >= minWidth && width <= maxWidth) {
                            minWidth = width;
                            iconUrl = icon.src;
                        }
                    });
                    if (iconUrl !== null) {
                        $INTERFACE.onIconResolved(baseUrl, iconUrl);
                    }
                } catch (error) {
                    $INTERFACE.onError();
                }
            })();
        """
    }
}
