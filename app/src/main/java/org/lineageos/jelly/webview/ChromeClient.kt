/*
 * SPDX-FileCopyrightText: 2020-2021 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.webview

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.lifecycle.MutableLiveData
import org.lineageos.jelly.model.Event

class ChromeClient : WebChromeClient() {
    // Values
    val loadingProgress = MutableLiveData<Int>()
    val title = MutableLiveData<String>()
    val favicon = MutableLiveData<Bitmap?>(null)

    // Callbacks
    val onShowFileChooser = MutableLiveData<OnShowFileChooserData>()
    val onGeolocationPermissionsShowPrompt =
        MutableLiveData<OnGeolocationPermissionsShowPromptData>()
    val onShowCustomView = MutableLiveData<OnShowCustomViewData>()
    val onHideCustomView = MutableLiveData<OnHideCustomViewData>()
    val onCreateWindow = MutableLiveData<OnCreateWindowData>()

    override fun onProgressChanged(view: WebView, progress: Int) {
        loadingProgress.value = progress
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        this.title.value = title
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        if (icon.isRecycled) {
            return
        }

        favicon.value = icon.copy(icon.config, true)

        icon.recycle()
    }

    override fun onShowFileChooser(
        view: WebView, path: ValueCallback<Array<Uri>>,
        params: FileChooserParams
    ): Boolean {
        onShowFileChooser.value = OnShowFileChooserData(view, path, params)

        return true
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback
    ) {
        onGeolocationPermissionsShowPrompt.value = OnGeolocationPermissionsShowPromptData(
            origin, callback
        )
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        onShowCustomView.value = OnShowCustomViewData(view, callback)
    }

    override fun onHideCustomView() {
        onHideCustomView.value = OnHideCustomViewData()
    }

    override fun onCreateWindow(
        view: WebView, isDialog: Boolean,
        isUserGesture: Boolean, resultMsg: Message
    ): Boolean {
        onCreateWindow.value = OnCreateWindowData(view, isDialog, isUserGesture, resultMsg)

        return true
    }

    data class OnShowFileChooserData(
        val view: WebView,
        val path: ValueCallback<Array<Uri>>,
        val params: FileChooserParams,
    ) : Event<OnShowFileChooserData>()

    data class OnGeolocationPermissionsShowPromptData(
        val origin: String,
        val callback: GeolocationPermissions.Callback,
    ) : Event<OnGeolocationPermissionsShowPromptData>()

    data class OnShowCustomViewData(
        val view: View,
        val callback: CustomViewCallback,
    ) : Event<OnShowCustomViewData>()

    class OnHideCustomViewData : Event<OnHideCustomViewData>()

    data class OnCreateWindowData(
        val view: WebView,
        val isDialog: Boolean,
        val isUserGesture: Boolean,
        val resultMsg: Message,
    ) : Event<OnCreateWindowData>()
}
