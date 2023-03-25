/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
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
import android.widget.ProgressBar
import org.lineageos.jelly.ui.UrlBarController
import org.lineageos.jelly.utils.PrefsUtils
import org.lineageos.jelly.utils.UrlUtils
import java.util.regex.Pattern

class WebViewExt @JvmOverloads constructor(
    context: Context, // Note that this is never null, a View can't have a null Context
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : WebView(context, attrs, defStyle) {
    private lateinit var activity: WebViewExtActivity
    val requestHeaders: MutableMap<String?, String?> = HashMap()
    private var mobileUserAgent: String? = null
    private var desktopUserAgent: String? = null
    var isIncognito = false
        private set
    private var desktopMode = false
    var lastLoadedUrl: String? = null
        private set

    override fun loadUrl(url: String) {
        lastLoadedUrl = url
        followUrl(url)
    }

    fun followUrl(url: String) {
        var fixedUrl = UrlUtils.smartUrlFilter(url)
        if (fixedUrl != null) {
            super.loadUrl(fixedUrl, this.requestHeaders)
            return
        }
        val templateUri = PrefsUtils.getSearchEngine(activity)
        fixedUrl = UrlUtils.getFormattedUri(templateUri, url)
        super.loadUrl(fixedUrl, this.requestHeaders)
    }

    private fun setup() {
        settings.javaScriptEnabled = PrefsUtils.getJavascript(activity)
        settings.javaScriptCanOpenWindowsAutomatically = PrefsUtils.getJavascript(activity)
        settings.setGeolocationEnabled(PrefsUtils.getLocation(activity))
        settings.setSupportMultipleWindows(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.databaseEnabled = !isIncognito
        settings.domStorageEnabled = !isIncognito
        setOnLongClickListener(object : OnLongClickListener {
            var shouldAllowDownload = false
            override fun onLongClick(v: View): Boolean {
                val result = hitTestResult
                result.extra?.let {
                    when (result.type) {
                        HitTestResult.IMAGE_TYPE, HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                            shouldAllowDownload = true
                            activity.showSheetMenu(it, shouldAllowDownload)
                            shouldAllowDownload = false
                            return true
                        }
                        HitTestResult.SRC_ANCHOR_TYPE -> {
                            activity.showSheetMenu(it, shouldAllowDownload)
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
        setDownloadListener { url: String?, _, contentDisposition: String?, mimeType: String?, _ ->
            activity.downloadFileAsk(url, contentDisposition, mimeType)
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
        if (PrefsUtils.getDoNotTrack(activity)) {
            this.requestHeaders[HEADER_DNT] = "1"
        }
    }

    fun init(
        activity: WebViewExtActivity, urlBarController: UrlBarController,
        progressBar: ProgressBar, incognito: Boolean
    ) {
        this.activity = activity
        isIncognito = incognito
        val chromeClient = ChromeClient(
            activity, incognito,
            urlBarController, progressBar
        )
        webChromeClient = chromeClient
        webViewClient = WebClient(urlBarController)
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
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36"
        private const val HEADER_DNT = "DNT"
    }
}
