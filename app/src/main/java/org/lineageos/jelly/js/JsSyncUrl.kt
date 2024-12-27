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

        private const val MONKEY_PATCH_ONCE_KEY = "JsSyncUrlMonkeyPatch"
        const val SCRIPT = """
            (() => {
                if (window.$MONKEY_PATCH_ONCE_KEY) {
                    return;
                }

                window.$MONKEY_PATCH_ONCE_KEY = true;
                window._JsPushState = window.history.pushState;
                window._JsReplaceState = window.history.replaceState;

                window.addEventListener('popstate', () => {
                    $INTERFACE.onPopState(window.location.href);
                });
                window.history.pushState = function (state, title, url) {
                    window._JsPushState.apply(window.history, [state, title, url]);
                    const currentUrl = window.location.href;
                    const currentTitle = document.title;
                    $INTERFACE.onPushState(currentTitle, currentUrl);
                };
                window.history.replaceState = function (state, title, url) {
                    const previousUrl = window.location.href;
                    window._JsReplaceState.apply(window.history, [state, title, url]);
                    const currentUrl = window.location.href;
                    const currentTitle = document.title;
                    $INTERFACE.onReplaceState(currentTitle, previousUrl, currentUrl);
                };
            })();
        """
    }
}
