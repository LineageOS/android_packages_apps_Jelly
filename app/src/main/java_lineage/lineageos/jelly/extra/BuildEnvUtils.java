package org.lineageos.jelly.extra;

import org.lineageos.jelly.webview.WebViewExt;

public final class BuildEnvUtils {

    private BuildEnvUtils() {
    }

    public static boolean isWebViewThemeColorSupported(WebViewExt webView) {
        return webView.isThemeColorSupported();
    }
}

