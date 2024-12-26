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
    fun onReplaceState(title: String, previousUrl: String, url: String) {
        urlBarLayout.url = url
        activity.replaceHistory(title, previousUrl, url)
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
                    window.originalPushState.apply(window.history, [state, title, url]);
                    const currentUrl = window.location.href;
                    const currentTitle = document.title;
                    ${JS_INTERFACE}.onPushState(currentTitle, currentUrl);
                };

                window.history.replaceState = function (state, title, url) {
                    const previousUrl = window.location.href;
                    window.originalReplaceState.apply(window.history, [state, title, url]);
                    const currentUrl = window.location.href;
                    const currentTitle = document.title;
                    ${JS_INTERFACE}.onReplaceState(currentTitle, previousUrl, currentUrl);
                };
            })();
        """
    }
}
