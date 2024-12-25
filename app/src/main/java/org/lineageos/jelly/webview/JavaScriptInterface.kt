/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.webview

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import org.lineageos.jelly.ui.UrlBarLayout

@Keep
class JavaScriptInterface(
    private val urlBarLayout: UrlBarLayout,
    private val activity: WebViewExtActivity,
) {
    @JavascriptInterface
    fun onPushState(title: String, url: String) {
        urlBarLayout.url = url
        activity.updateHistory(title, url)
    }

    @JavascriptInterface
    fun onReplaceState(url: String) {
        urlBarLayout.url = url
    }

    companion object {
        const val JS_INTERFACE = "Jelly"

        const val SYNC_URL_JS = """
            (() => {
                if (!window.originalPushState) {
                    window.originalPushState = window.history.pushState;
                }
                if (!window.originalReplaceState) {
                    window.originalReplaceState = window.history.replaceState;
                }
                window.history.pushState = function (state, title, url) {
                    window.originalPushState.apply(this, arguments);
                    ${JS_INTERFACE}.onPushState(title, url);
                };
                window.history.replaceState = function (state, title, url) {
                    window.originalReplaceState.apply(this, arguments);
                    ${JS_INTERFACE}.onReplaceState(url);
                };
            })();
        """
    }
}
