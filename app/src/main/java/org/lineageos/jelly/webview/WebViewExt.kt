/*
 * Copyright (C) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.jelly.webview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.util.ArrayMap
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import org.lineageos.jelly.ui.AutoCompleteTextViewExt
import org.lineageos.jelly.utils.PrefsUtils
import org.lineageos.jelly.utils.UrlUtils
import java.util.regex.Pattern

class WebViewExt : WebView {
    private val mRequestHeaders = ArrayMap<String, String>()
    private var mActivity: WebViewExtActivity? = null
    private var mMobileUserAgent: String? = null
    private var mDesktopUserAgent: String? = null
    var isIncognito: Boolean = false
        private set
    var isDesktopMode: Boolean = false
        set(desktopMode) {
            field = desktopMode
            val settings = settings
            settings.userAgentString = if (desktopMode) mDesktopUserAgent else mMobileUserAgent
            settings.useWideViewPort = desktopMode
            settings.loadWithOverviewMode = desktopMode
            reload()
        }
    var lastLoadedUrl: String? = null
        private set

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun loadUrl(url: String) {
        lastLoadedUrl = url
        followUrl(url)
    }

    internal fun followUrl(url: String) {
        val fixedUrl: String? = UrlUtils.smartUrlFilter(url)
        if (fixedUrl != null) {
            super.loadUrl(fixedUrl, mRequestHeaders)
            return
        }

        val templateUri = PrefsUtils.getSearchEngine(mActivity as Context)
        super.loadUrl(UrlUtils.getFormattedUri(templateUri, url), mRequestHeaders)
    }

    private fun setup() {
        settings.javaScriptEnabled = PrefsUtils.getJavascript(mActivity as Context)
        settings.javaScriptCanOpenWindowsAutomatically = PrefsUtils.getJavascript(mActivity as Context)
        settings.setGeolocationEnabled(PrefsUtils.getLocation(mActivity as Context))
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.domStorageEnabled = true

        webViewClient = WebClient()

        setOnLongClickListener(object : View.OnLongClickListener {
            internal var shouldAllowDownload: Boolean = false

            override fun onLongClick(v: View): Boolean {
                val result = hitTestResult
                when (result.type) {
                    WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                        shouldAllowDownload = true
                        mActivity!!.showSheetMenu(result.extra, shouldAllowDownload)
                        shouldAllowDownload = false
                        return true
                    }
                    WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                        mActivity!!.showSheetMenu(result.extra, shouldAllowDownload)
                        shouldAllowDownload = false
                        return true
                    }
                }
                return false
            }
        })

        setDownloadListener { url, _, contentDescription, mimeType, _ -> mActivity!!.downloadFileAsk(url, contentDescription, mimeType) }

        // Mobile: Remove "wv" from the WebView's user agent. Some websites don't work
        // properly if the browser reports itself as a simple WebView.
        // Desktop: Generate the desktop user agent starting from the mobile one so that
        // we always report the current engine version.
        val pattern = Pattern.compile("([^)]+ \\()([^)]+)(\\) .*)")
        val matcher = pattern.matcher(settings.userAgentString)
        if (matcher.matches()) {
            val mobileDevice = matcher.group(2).replace("; wv", "")
            mMobileUserAgent = matcher.group(1) + mobileDevice + matcher.group(3)
            mDesktopUserAgent = matcher.group(1) + DESKTOP_DEVICE + matcher.group(3)
            settings.userAgentString = mMobileUserAgent
        } else {
            Log.e(TAG, "Couldn't parse the user agent")
            mMobileUserAgent = settings.userAgentString
            mDesktopUserAgent = DESKTOP_USER_AGENT_FALLBACK
        }

        if (PrefsUtils.getDoNotTrack(mActivity as Context)) {
            mRequestHeaders.put(HEADER_DNT, "1")
        }
    }

    fun init(activity: WebViewExtActivity, incognito: Boolean,
            url_bar: AutoCompleteTextViewExt, load_progress: ProgressBar) {
        mActivity = activity
        isIncognito = incognito
        webChromeClient = ChromeClient(activity, incognito, url_bar, load_progress)
        setup()
    }

    val snap: Bitmap
        get() {
            measure(View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            layout(0, 0, measuredWidth, measuredHeight)
            isDrawingCacheEnabled = true
            buildDrawingCache()
            val size = if (measuredWidth > measuredHeight)
                measuredHeight
            else
                measuredWidth
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val height = bitmap.height
            canvas.drawBitmap(bitmap, 0f, height.toFloat(), paint)
            draw(canvas)
            return bitmap
        }

    internal val requestHeaders: Map<String, String>
        get() = mRequestHeaders

    companion object {

        private val TAG = "WebViewExt"

        private val DESKTOP_DEVICE = "X11; Linux x86_64"
        private val DESKTOP_USER_AGENT_FALLBACK = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36"
        private val HEADER_DNT = "DNT"
    }
}
