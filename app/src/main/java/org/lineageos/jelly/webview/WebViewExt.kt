/*
 * SPDX-FileCopyrightText: 2020-2025 The LineageOS Project
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
import org.lineageos.jelly.js.JsManifest
import org.lineageos.jelly.js.JsSyncUrl
import org.lineageos.jelly.ui.UrlBarLayout
import org.lineageos.jelly.utils.SharedPreferencesExt
import org.lineageos.jelly.utils.UrlUtils
import java.util.regex.Pattern

class WebViewExt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : WebView(context, attrs, defStyle) {
    private lateinit var activity: WebViewExtActivity
    val requestHeaders = mutableMapOf<String?, String?>()
    private var mobileUserAgent: String? = null
    private var desktopUserAgent: String? = null
    var isIncognito = false
        private set
    private var desktopMode = false
    var lastLoadedUrl: String? = null
        private set

    private val sharedPreferencesExt by lazy { SharedPreferencesExt(context) }

    override fun loadUrl(url: String) {
        lastLoadedUrl = url
        followUrl(url)
    }

    fun followUrl(url: String) {
        UrlUtils.smartUrlFilter(url)?.let {
            super.loadUrl(it, this.requestHeaders)
            return
        }
        val templateUri = sharedPreferencesExt.searchEngine
        super.loadUrl(UrlUtils.getFormattedUri(templateUri, url), this.requestHeaders)
    }

    private fun setup(urlBarLayout: UrlBarLayout) {
        settings.javaScriptEnabled = sharedPreferencesExt.javascriptEnabled
        settings.javaScriptCanOpenWindowsAutomatically = sharedPreferencesExt.javascriptEnabled
        settings.setGeolocationEnabled(sharedPreferencesExt.locationEnabled)
        settings.setSupportMultipleWindows(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.domStorageEnabled = !isIncognito
        setOnLongClickListener(object : OnLongClickListener {
            override fun onLongClick(v: View): Boolean {
                val result = hitTestResult
                result.extra?.let {
                    when (result.type) {
                        HitTestResult.IMAGE_TYPE, HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                            activity.showSheetMenu(it, true)
                            return true
                        }

                        HitTestResult.SRC_ANCHOR_TYPE -> {
                            activity.showSheetMenu(it, false)
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
            activity.downloadFileAsk(url, userAgent, contentDisposition, mimeType, contentLength)
        }

        // Mobile: Remove "wv" and "Version/4.0" from the WebView's user agent.
        // Some websites don't work properly if the browser reports itself as a simple WebView.
        // Desktop: Generate the desktop user agent starting from the mobile one so that
        // we always report the current engine version.
        val pattern = Pattern.compile("([^)]+ \\()([^)]+)(\\) .*)")
        val matcher = pattern.matcher(settings.userAgentString)
        if (matcher.matches()) {
            val mobileDevice = matcher.group(2)!!.replace("; wv", "")
            mobileUserAgent = matcher.group(1)!! + mobileDevice + matcher.group(3)!!
                .replace(" Version/4.0 ", " ")
            desktopUserAgent = matcher.group(1)!! + DESKTOP_DEVICE + matcher.group(3)!!
                .replace(" Mobile ", " ")
                .replace(" Version/4.0 ", " ")
            settings.userAgentString = mobileUserAgent
        } else {
            Log.e(TAG, "Couldn't parse the user agent")
            mobileUserAgent = settings.userAgentString
            desktopUserAgent = DESKTOP_USER_AGENT_FALLBACK
        }
        if (sharedPreferencesExt.doNotTrackEnabled) {
            this.requestHeaders[HEADER_DNT] = "1"
        }

        if (settings.javaScriptEnabled) {
            addJavascriptInterface(
                JsSyncUrl(urlBarLayout, activity),
                JsSyncUrl.INTERFACE
            )
            addJavascriptInterface(
                JsManifest(activity),
                JsManifest.INTERFACE
            )
        }
    }

    fun init(
        activity: WebViewExtActivity, urlBarLayout: UrlBarLayout, incognito: Boolean
    ) {
        this.activity = activity
        isIncognito = incognito
        val chromeClient = ChromeClient(
            activity, incognito, urlBarLayout, sharedPreferencesExt
        )
        webChromeClient = chromeClient
        webViewClient = WebClient(urlBarLayout)
        setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            urlBarLayout.searchPositionInfo = Pair(activeMatchOrdinal, numberOfMatches)
        }
        urlBarLayout.onLoadUrlCallback = { loadUrl(it) }
        urlBarLayout.onStartSearchCallback = { findAllAsync(it) }
        urlBarLayout.onClearSearchCallback = { clearMatches() }
        urlBarLayout.onSearchPositionChangeCallback = { findNext(it) }
        setup(urlBarLayout)
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

    var isDesktopMode: Boolean
        get() = desktopMode
        set(desktopMode) {
            this.desktopMode = desktopMode
            val settings = settings
            settings.userAgentString = if (desktopMode) desktopUserAgent else mobileUserAgent
            settings.useWideViewPort = desktopMode
            settings.loadWithOverviewMode = desktopMode
            reload()
        }

    companion object {
        private const val TAG = "WebViewExt"
        private const val DESKTOP_DEVICE = "X11; Linux x86_64"
        private const val DESKTOP_USER_AGENT_FALLBACK =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val HEADER_DNT = "DNT"
    }
}
