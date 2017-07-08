/*
 * Copyright (C) 2017 The LineageOS Project
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
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import org.lineageos.jelly.R
import org.lineageos.jelly.history.HistoryProvider
import org.lineageos.jelly.ui.AutoCompleteTextViewExt

internal class ChromeClient(
        private val mActivity: WebViewExtActivity,
        private val mIncognito: Boolean,
        private val url_bar: AutoCompleteTextViewExt,
        private val load_progress: ProgressBar) : WebChromeClientCompat() {

    override fun onProgressChanged(view: WebView, progress: Int) {
        load_progress.visibility = if (progress == 100) View.INVISIBLE else View.VISIBLE
        load_progress.progress = if (progress == 100) 0 else progress
        super.onProgressChanged(view, progress)
    }

    override fun onThemeColorChanged(view: WebView, color: Int) {
        mActivity.onThemeColorSet(color)
        super.onThemeColorChanged(view, color)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        url_bar.setText(view.url)
        if (!mIncognito) {
            HistoryProvider.addOrUpdateItem(mActivity.contentResolver, title, view.url)
        }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        mActivity.onFaviconLoaded(icon)
    }

    override fun onShowFileChooser(view: WebView, path: ValueCallback<Array<Uri>>,
            params: WebChromeClient.FileChooserParams): Boolean {
        val intent = params.createIntent()
        try {
            mActivity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(mActivity, mActivity.getString(R.string.error_no_activity_found),
                    Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String,
            callback: GeolocationPermissions.Callback) {
        if (!mActivity.hasLocationPermission()) {
            mActivity.requestLocationPermission()
        } else {
            callback.invoke(origin, true, false)
        }
    }
}
