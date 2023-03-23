/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.webview

import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient.CustomViewCallback
import androidx.appcompat.app.AppCompatActivity

abstract class WebViewExtActivity : AppCompatActivity() {
    abstract fun downloadFileAsk(url: String?, contentDisposition: String?, mimeType: String?)
    abstract fun hasLocationPermission(): Boolean
    abstract fun requestLocationPermission()
    abstract fun showSheetMenu(url: String, shouldAllowDownload: Boolean)
    abstract fun onFaviconLoaded(favicon: Bitmap?)
    abstract fun onShowCustomView(view: View?, callback: CustomViewCallback)
    abstract fun onHideCustomView()
}
