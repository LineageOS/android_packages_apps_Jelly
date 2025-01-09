/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.js

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.lineageos.jelly.webview.WebViewExtActivity
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.reflect.cast

@Keep
class JsManifest(
    private val activity: WebViewExtActivity,
) {
    private lateinit var url: String
    private lateinit var manifest: JSONObject

    @JavascriptInterface
    fun onResolved(url: String, value: String) {
        this.url = url
        manifest = JSONObject(value)
        getIconUrl()?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val bitmap = getIconBitmap(it) ?: return@launch
                withContext(Dispatchers.Main) {
                    activity.onFaviconLoaded(bitmap)
                }
            }
        }
    }

    private fun getIconUrl(): String? {
        var url: String? = null
        var minWidth = 30
        val maxWidth = 300
        val icons = manifest.optJSONArray("icons") ?: return null
        (0 until icons.length()).forEach {
            val icon = JSONObject::class.cast(icons[it])
            val src = icon.getString("src")
            val sizes = icon.optString("sizes", "")
            val purpose = icon.optString("purpose", "any")
            if (sizes.isEmpty()) return@forEach
            if (purpose.contains("monochrome")) return@forEach
            val width = sizes.split("x")[0].toInt()
            if (width in minWidth..maxWidth) {
                minWidth = width
                url = src
            }
        }
        return if (url != null) URI(this.url).resolve(url).toString() else null
    }

    private fun getIconBitmap(url: String): Bitmap? {
        try {
            val connection = HttpURLConnection::class.cast(
                URL(url).openConnection()
            )
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val inputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            return bitmap
        } catch (_: Exception) {
            return null
        }
    }

    companion object {
        const val INTERFACE = "JsManifest"
        const val URL = "(() => document.querySelector('link[rel=\"manifest\"]')?.href ?? '')"

        private const val MONKEY_PATCH_ONCE_KEY = "JsManifestMonkeyPatch"
        const val SCRIPT = """
            (() => {
                if (window.$MONKEY_PATCH_ONCE_KEY) return;

                window.$MONKEY_PATCH_ONCE_KEY = true;
                const url = $URL();

                if (!url) return;
                
                fetch(url)
                    .then((res) => res.json())
                    .then((data) => JSON.stringify(data))
                    .then((manifest) => $INTERFACE.onResolved(url, manifest));
            })();
        """
    }
}
