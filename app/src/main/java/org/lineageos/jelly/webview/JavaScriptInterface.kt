package org.lineageos.jelly.webview

import android.util.Log
import android.webkit.JavascriptInterface
import org.lineageos.jelly.ui.UrlBarLayout

class JavaScriptInterface(
    private val urlBarLayout: UrlBarLayout,
) {
    @JavascriptInterface
    fun onPushState(url: String) {
        Log.i(TAG, "JavaScriptInterface#onPushState: $url")
        urlBarLayout.url = url
    }

    @JavascriptInterface
    fun onReplaceState(url: String) {
        Log.i(TAG, "JavaScriptInterface#onReplaceState: $url")
        urlBarLayout.url = url
    }

    companion object {
        private const val TAG = "JavaScriptInterface"
    }
}
