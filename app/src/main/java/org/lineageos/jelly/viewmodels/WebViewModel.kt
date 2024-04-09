/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.DownloadListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.lineageos.jelly.model.LoadingStatus
import org.lineageos.jelly.webview.ChromeClient
import org.lineageos.jelly.webview.WebClient
import org.lineageos.jelly.webview.WebViewExt

/**
 * View model for a web session.
 */
class WebViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * The [WebClient] for this session.
     */
    val webClient = WebClient()

    /**
     * The [ChromeClient] for this session.
     */
    val chromeClient = ChromeClient()

    /**
     * Whether the current session is in incognito mode.
     */
    val isIncognito = MutableLiveData(false)

    /**
     * Desktop mode.
     */
    val desktopMode = MutableLiveData(false)

    /**
     * The title of the currently loaded web page.
     */
    val title: LiveData<String> = chromeClient.title

    /**
     * The current page favicon.
     */
    val favicon: LiveData<Bitmap?> = chromeClient.favicon

    /**
     * Loading progress, from 0 to 100 if loading is happening, else null will be returned.
     */
    val loadingProgress: LiveData<Int> = chromeClient.loadingProgress

    /**
     * The current URL.
     */
    val loadingStatus: LiveData<LoadingStatus> = webClient.loadingStatus

    /**
     * SSL error.
     */
    val sslError: LiveData<SslError?> = webClient.sslError

    /**
     * Search position.
     */
    val searchPosition = MutableLiveData<Pair<Int, Int>?>(null)

    // Activity callback LiveData

    val onShowSheetMenu = MutableLiveData<WebViewExt.ShowSheetMenuData>()

    /**
     * @see DownloadListener.onDownloadStart
     */
    val onDownloadStart = MutableLiveData<WebViewExt.OnDownloadStartData>()

    /**
     * @see ChromeClient.onShowFileChooser
     */
    val onShowFileChooser: LiveData<ChromeClient.OnShowFileChooserData> =
        chromeClient.onShowFileChooser

    /**
     * @see ChromeClient.onGeolocationPermissionsShowPrompt
     */
    val onGeolocationPermissionsShowPrompt:
            LiveData<ChromeClient.OnGeolocationPermissionsShowPromptData> =
        chromeClient.onGeolocationPermissionsShowPrompt

    /**
     * @see ChromeClient.onShowCustomView
     */
    val onShowCustomView: LiveData<ChromeClient.OnShowCustomViewData> =
        chromeClient.onShowCustomView

    /**
     * @see ChromeClient.onHideCustomView
     */
    val onHideCustomView: LiveData<ChromeClient.OnHideCustomViewData> =
        chromeClient.onHideCustomView

    /**
     * @see ChromeClient.onCreateWindow
     */
    val onCreateWindow: LiveData<ChromeClient.OnCreateWindowData> = chromeClient.onCreateWindow
}
