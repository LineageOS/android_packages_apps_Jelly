/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.js

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import org.lineageos.jelly.ui.UrlBarLayout
import org.lineageos.jelly.webview.WebViewExtActivity

@Keep
class JsSyncUrl(
    private val urlBarLayout: UrlBarLayout,
    private val activity: WebViewExtActivity,
) {
    @JavascriptInterface
    fun onPopState(url: String) {
        urlBarLayout.url = url
    }

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
        const val INTERFACE = "JsSyncUrl"
        const val SCRIPT = """
            (() => {
                if (!window.registeredPopState) {
                    window.registeredPopState = true;
                    window.addEventListener('popstate', () => {
                        $INTERFACE.onPopState(window.location.href);
                    });
                }
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
                    $INTERFACE.onPushState(currentTitle, currentUrl);
                };
                window.history.replaceState = function (state, title, url) {
                    const previousUrl = window.location.href;
                    window.originalReplaceState.apply(window.history, [state, title, url]);
                    const currentUrl = window.location.href;
                    const currentTitle = document.title;
                    $INTERFACE.onReplaceState(currentTitle, previousUrl, currentUrl);
                };
            })();
        """
    }
}
