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
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import org.lineageos.jelly.model.LoadingStatus
import org.lineageos.jelly.webview.ChromeClient
import org.lineageos.jelly.webview.WebClient
import org.lineageos.jelly.webview.WebViewExt

/**
 * View model for a web session.
 */
class WebViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    /**
     * The [WebClient] for this session.
     */
    val webClient = WebClient()

    /**
     * The [ChromeClient] for this session.
     */
    val chromeClient = ChromeClient()

    /**
     * The current URL.
     * Source of truth is the [WebViewExt].
     */
    val url = savedStateHandle.getLiveData<String>(KEY_URL)

    /**
     * Whether the current session is in incognito mode.
     * Source of truth is the [WebViewModel].
     */
    val isIncognito = savedStateHandle.getLiveData(KEY_IS_INCOGNITO, false)

    /**
     * Whether the browser should request desktop web page to the server.
     * Source of truth is the [WebViewModel].
     */
    val desktopMode = savedStateHandle.getLiveData(KEY_DESKTOP_MODE, false)

    /**
     * The title of the currently loaded web page.
     * Source of truth is the [ChromeClient].
     */
    val title: LiveData<String> = chromeClient.title

    /**
     * The current page favicon.
     * Source of truth is the [ChromeClient].
     */
    val favicon: LiveData<Bitmap?> = chromeClient.favicon

    /**
     * Loading progress, from 0 to 100 if loading is happening, else null will be returned.
     * Source of truth is the [ChromeClient].
     */
    val loadingProgress: LiveData<Int> = chromeClient.loadingProgress

    /**
     * The current URL.
     * Source of truth is the [WebClient].
     */
    val loadingStatus: LiveData<LoadingStatus> = webClient.loadingStatus

    /**
     * SSL error.
     * Source of truth is the [WebClient].
     */
    val sslError: LiveData<SslError?> = webClient.sslError

    /**
     * Search position.
     * Source of truth is the [WebViewExt].
     */
    val searchPosition = MutableLiveData<Pair<Int, Int>?>(null)

    // Activity callback LiveData

    /**
     * Show the sheet menu for user's long press on a page element.
     */
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

    private val loadingStatusObserver = Observer { loadingStatus: LoadingStatus ->
        // Track the latest URL we receive
        savedStateHandle[KEY_URL] = loadingStatus.url
    }

    init {
        loadingStatus.observeForever(loadingStatusObserver)
    }

    override fun onCleared() {
        super.onCleared()

        loadingStatus.removeObserver(loadingStatusObserver)
    }

    /**
     * Set the initial URL if not present.
     * @param initialUrl The initial URL to load
     */
    fun setInitialUrl(initialUrl: String) {
        if (!savedStateHandle.contains(KEY_URL)) {
            savedStateHandle[KEY_URL] = initialUrl
        }
    }

    /**
     * Change whether this session is in incognito mode.
     * @param isIncognito Whether this session is in incognito mode
     */
    fun setIncognitoMode(isIncognito: Boolean) {
        savedStateHandle[KEY_IS_INCOGNITO] = isIncognito
    }

    /**
     * Change whether the browser should request desktop web page to the server.
     * @param desktopMode Whether the browser should request desktop web page to the server
     */
    fun setDesktopMode(desktopMode: Boolean) {
        savedStateHandle[KEY_DESKTOP_MODE] = desktopMode
    }

    companion object {
        private const val KEY_URL = "url"
        private const val KEY_IS_INCOGNITO = "is_incognito"
        private const val KEY_DESKTOP_MODE = "desktop_mode"
    }
}
