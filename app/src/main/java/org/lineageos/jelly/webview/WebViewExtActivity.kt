/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.jelly.webview

import android.graphics.Bitmap
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient.CustomViewCallback
import androidx.appcompat.app.AppCompatActivity

abstract class WebViewExtActivity : AppCompatActivity() {
    abstract fun downloadFileAsk(url: String?, contentDisposition: String?, mimeType: String?)
    abstract fun showSheetMenu(url: String, shouldAllowDownload: Boolean)
    abstract fun onFaviconLoaded(favicon: Bitmap?)
    abstract fun onShowCustomView(view: View?, callback: CustomViewCallback)
    abstract fun onHideCustomView()
    abstract fun showLocationDialog(origin: String, callback: GeolocationPermissions.Callback)
}