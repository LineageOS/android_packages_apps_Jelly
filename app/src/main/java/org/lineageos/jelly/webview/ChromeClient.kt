/*
 * SPDX-FileCopyrightText: 2020-2021 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.webview

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.MimeTypeMap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.lineageos.jelly.R
import org.lineageos.jelly.ui.UrlBarLayout
import org.lineageos.jelly.utils.TabUtils.openInNewTab
import kotlin.reflect.cast

internal class ChromeClient(
    private val activity: WebViewExtActivity,
    private val incognito: Boolean,
    private val urlBarLayout: UrlBarLayout
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView, progress: Int) {
        urlBarLayout.loadingProgress = progress
        super.onProgressChanged(view, progress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        view.url?.let {
            if (!incognito) {
                activity.updateHistory(title, url = it)
            }
        }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        activity.onFaviconLoaded(icon)
    }

    override fun onShowFileChooser(
        view: WebView, path: ValueCallback<Array<Uri>>,
        params: FileChooserParams
    ): Boolean {
        activity.setFileRequestCallback {
            path.onReceiveValue(it.toTypedArray())
        }

        try {
            activity.launchFileRequest(params.acceptTypes.mapNotNull {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
            }.toTypedArray().takeIf { it.isNotEmpty() } ?: arrayOf("*/*"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                activity, activity.getString(R.string.error_no_activity_found),
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        return true
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback
    ) {
        activity.showLocationDialog(origin, callback)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        activity.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        activity.onHideCustomView()
    }

    override fun onCreateWindow(
        view: WebView, isDialog: Boolean,
        isUserGesture: Boolean, resultMsg: Message
    ): Boolean {
        val result = view.hitTestResult
        val url = result.extra

        if (url == null) {
            val transport = WebView.WebViewTransport::class.cast(resultMsg.obj)
            val tempWebView = WebView(view.context)
            tempWebView.webViewClient = object : WebViewClient() {
                override fun onLoadResource(view: WebView, url: String) {
                    tempWebView.destroy()
                    openInNewTab(activity, url, incognito)
                }
            }
            transport.webView = tempWebView
            resultMsg.sendToTarget()
            return true
        }

        openInNewTab(activity, url, incognito)
        return false
    }
}
