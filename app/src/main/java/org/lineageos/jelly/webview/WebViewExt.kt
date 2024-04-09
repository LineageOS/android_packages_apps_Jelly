/*
 * SPDX-FileCopyrightText: 2020-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.webview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.WebView
import androidx.lifecycle.findViewTreeLifecycleOwner
import org.lineageos.jelly.ext.viewModels
import org.lineageos.jelly.model.Event
import org.lineageos.jelly.utils.SharedPreferencesExt
import org.lineageos.jelly.utils.UrlUtils
import org.lineageos.jelly.viewmodels.WebViewModel
import java.util.regex.Pattern

class WebViewExt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : WebView(context, attrs, defStyle) {
    // View models
    private val model by viewModels<WebViewModel>()

    val requestHeaders = mutableMapOf<String?, String?>()
    private var mobileUserAgent: String? = null
    private var desktopUserAgent: String? = null
    var isIncognito = false
    private var desktopMode = false
    var lastLoadedUrl: String? = null
        private set

    private val sharedPreferencesExt by lazy { SharedPreferencesExt(context) }

    override fun loadUrl(url: String) {
        lastLoadedUrl = url
        followUrl(url)
    }

    override fun clearMatches() {
        super.clearMatches()

        model.searchPosition.value = null
    }

    fun followUrl(url: String) {
        UrlUtils.smartUrlFilter(url)?.let {
            super.loadUrl(it, this.requestHeaders)
            return
        }
        val templateUri = sharedPreferencesExt.searchEngine
        super.loadUrl(UrlUtils.getFormattedUri(templateUri, url), this.requestHeaders)
    }

    private fun setup() {
        settings.javaScriptEnabled = sharedPreferencesExt.javascriptEnabled
        settings.javaScriptCanOpenWindowsAutomatically = sharedPreferencesExt.javascriptEnabled
        settings.setGeolocationEnabled(sharedPreferencesExt.locationEnabled)
        settings.setSupportMultipleWindows(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        setOnLongClickListener(object : OnLongClickListener {
            var shouldAllowDownload = false
            override fun onLongClick(v: View): Boolean {
                val result = hitTestResult
                result.extra?.let {
                    when (result.type) {
                        HitTestResult.IMAGE_TYPE, HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                            shouldAllowDownload = true
                            model.onShowSheetMenu.value = ShowSheetMenuData(it, shouldAllowDownload)
                            shouldAllowDownload = false
                            return true
                        }

                        HitTestResult.SRC_ANCHOR_TYPE -> {
                            model.onShowSheetMenu.value = ShowSheetMenuData(it, shouldAllowDownload)
                            shouldAllowDownload = false
                            return true
                        }

                        else -> {
                            return false
                        }
                    }
                }
                return false
            }
        })
        setDownloadListener { url: String?, userAgent: String?, contentDisposition: String?,
                              mimeType: String?, contentLength: Long ->
            model.onDownloadStart.value = OnDownloadStartData(
                url, userAgent, contentDisposition, mimeType, contentLength
            )
        }

        // Mobile: Remove "wv" from the WebView's user agent. Some websites don't work
        // properly if the browser reports itself as a simple WebView.
        // Desktop: Generate the desktop user agent starting from the mobile one so that
        // we always report the current engine version.
        val pattern = Pattern.compile("([^)]+ \\()([^)]+)(\\) .*)")
        val matcher = pattern.matcher(settings.userAgentString)
        if (matcher.matches()) {
            val mobileDevice = matcher.group(2)!!.replace("; wv", "")
            mobileUserAgent = matcher.group(1)!! + mobileDevice + matcher.group(3)
            desktopUserAgent = matcher.group(1)!! + DESKTOP_DEVICE + matcher.group(3)!!
                .replace(" Mobile ", " ")
            settings.userAgentString = mobileUserAgent
        } else {
            Log.e(TAG, "Couldn't parse the user agent")
            mobileUserAgent = settings.userAgentString
            desktopUserAgent = DESKTOP_USER_AGENT_FALLBACK
        }
        if (sharedPreferencesExt.doNotTrackEnabled) {
            this.requestHeaders[HEADER_DNT] = "1"
        }

        val viewTreeLifecycleOwner = findViewTreeLifecycleOwner()!!

        model.isIncognito.observe(viewTreeLifecycleOwner) { isIncognito ->
            this.isIncognito = isIncognito

            settings.databaseEnabled = !isIncognito
            settings.domStorageEnabled = !isIncognito
        }

        model.desktopMode.observe(viewTreeLifecycleOwner) { desktopMode ->
            // Calling reload() too early will make reload() stop working altogether until
            // we load a new page, apply the logic when we receive desktopMode=true
            if (this.desktopMode == desktopMode) {
                return@observe
            }

            this.desktopMode = desktopMode

            val settings = settings

            settings.userAgentString = when (desktopMode) {
                true -> desktopUserAgent
                false -> mobileUserAgent
            }
            settings.useWideViewPort = desktopMode
            settings.loadWithOverviewMode = desktopMode

            reload()
        }
    }

    fun init() {
        webChromeClient = model.chromeClient
        webViewClient = model.webClient

        setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            model.searchPosition.value = Pair(activeMatchOrdinal, numberOfMatches)
        }

        setup()
    }

    val snap: Bitmap
        get() {
            measure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            layout(0, 0, measuredWidth, measuredHeight)
            val size = if (measuredWidth > measuredHeight) measuredHeight else measuredWidth
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val height = bitmap.height
            canvas.drawBitmap(bitmap, 0f, height.toFloat(), paint)
            draw(canvas)
            return bitmap
        }

    data class ShowSheetMenuData(
        val url: String,
        val shouldAllowDownload: Boolean,
    ) : Event<ShowSheetMenuData>()

    data class OnDownloadStartData(
        val url: String?,
        val userAgent: String?,
        val contentDisposition: String?,
        val mimeType: String?,
        val contentLength: Long,
    ) : Event<OnDownloadStartData>()

    companion object {
        private const val TAG = "WebViewExt"
        private const val DESKTOP_DEVICE = "X11; Linux x86_64"
        private const val DESKTOP_USER_AGENT_FALLBACK =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36"
        private const val HEADER_DNT = "DNT"
    }
}
