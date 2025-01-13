/*
 * SPDX-FileCopyrightText: 2020-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.webview

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient.CustomViewCallback
import androidx.appcompat.app.AppCompatActivity
import org.lineageos.jelly.models.PwaManifest

abstract class WebViewExtActivity : AppCompatActivity() {
    abstract fun downloadFileAsk(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    )

    abstract fun showSheetMenu(url: String, shouldAllowDownload: Boolean)
    abstract fun onFaviconLoaded(favicon: Bitmap?)
    abstract fun onShowCustomView(view: View?, callback: CustomViewCallback)
    abstract fun onHideCustomView()
    abstract fun launchFileRequest(input: Array<String>)
    abstract fun setFileRequestCallback(cb: ((data: List<Uri>) -> Unit))
    abstract fun showLocationDialog(origin: String, callback: GeolocationPermissions.Callback)
    abstract fun updateHistory(title: String, url: String)
    abstract fun replaceHistory(title: String, url: String, newUrl: String)
    abstract fun setPwaManifest(manifest: PwaManifest?)
    abstract fun webRequestPermissions(
        permissions: Array<String>,
        cb: ((granted: Array<String>) -> Unit)
    )
    abstract fun webProtectedMedia(origin: String, cb: ((granted: Boolean) -> Unit))
}
