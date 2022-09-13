/*
 * Copyright (C) 2020-2021 The LineageOS Project
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

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import org.lineageos.jelly.R
import org.lineageos.jelly.history.HistoryProvider
import org.lineageos.jelly.ui.UrlBarController
import org.lineageos.jelly.utils.TabUtils.openInNewTab

internal class ChromeClient(
    private val mActivity: WebViewExtActivity,
    private val mIncognito: Boolean,
    private val mUrlBarController: UrlBarController,
    private val mProgressBar: ProgressBar
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView, progress: Int) {
        mProgressBar.visibility = if (progress == 100) View.INVISIBLE else View.VISIBLE
        mProgressBar.progress = if (progress == 100) 0 else progress
        super.onProgressChanged(view, progress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        mUrlBarController.onTitleReceived(title)
        view.url?.let {
            if (!mIncognito) {
                HistoryProvider.addOrUpdateItem(mActivity.contentResolver, title, it)
            }
        }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        mActivity.onFaviconLoaded(icon)
    }

    override fun onShowFileChooser(
        view: WebView, path: ValueCallback<Array<Uri>>,
        params: FileChooserParams
    ): Boolean {
        val getContent = mActivity.registerForActivityResult(OpenMultipleDocuments()) {
            path.onReceiveValue(it.toTypedArray())
        }

        try {
            getContent.launch(params.acceptTypes.map {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)!!
            }.toTypedArray())
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                mActivity, mActivity.getString(R.string.error_no_activity_found),
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
        if (!mActivity.hasLocationPermission()) {
            mActivity.requestLocationPermission()
        } else {
            callback.invoke(origin, true, false)
        }
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        mActivity.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        mActivity.onHideCustomView()
    }

    override fun onCreateWindow(
        view: WebView, isDialog: Boolean,
        isUserGesture: Boolean, resultMsg: Message
    ): Boolean {
        val result = view.hitTestResult
        val url = result.extra
        openInNewTab(mActivity, url, mIncognito)
        return true
    }
}
