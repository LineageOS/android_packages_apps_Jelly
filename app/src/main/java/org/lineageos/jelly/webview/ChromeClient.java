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
package org.lineageos.jelly.webview;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.lineageos.jelly.MainActivity;
import org.lineageos.jelly.R;
import org.lineageos.jelly.history.HistoryDatabaseHandler;
import org.lineageos.jelly.history.HistoryItem;
import org.lineageos.jelly.ui.EditTextExt;


class ChromeClient extends WebChromeClient {

    private final Context mContext;
    private final HistoryDatabaseHandler mHistoryHandler;
    private final boolean mIncognito;

    private EditTextExt mEditTextExt;
    private ProgressBar mProgressBar;

    ChromeClient(Context context, boolean incognito) {
        super();
        mContext = context;
        mHistoryHandler = new HistoryDatabaseHandler(context);
        mIncognito = incognito;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
        mProgressBar.setVisibility(progress == 100 ? View.INVISIBLE : View.VISIBLE);
        mProgressBar.setProgress(progress == 100 ? 0 : progress);
        super.onProgressChanged(view, progress);
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        String url = view.getUrl();
        mEditTextExt.setTitle(title);
        mEditTextExt.setUrl(url);
        if (url.startsWith("https://")) {
            mEditTextExt.setText(title);
        } else {
            mEditTextExt.setText(url);
        }

        if (!mIncognito) {
            mHistoryHandler.addItem(new HistoryItem(title, url));
        }
    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        ((MainActivity) mContext).setColor(icon, mIncognito);
    }

    @Override
    public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> path,
                                     FileChooserParams params) {
        Intent intent = params.createIntent();
        try {
            ((MainActivity) mContext).startActivityForResult(intent, MainActivity.FILE_CHOOSER_REQ);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, mContext.getString(R.string.error_no_activity_found),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin,
                                                   GeolocationPermissions.Callback callback) {
        MainActivity activity = ((MainActivity) mContext);
        if (!activity.hasLocationPermission()) {
            activity.requestLocationPermission();
        } else {
            callback.invoke(origin, true, false);
        }
    }

    void bindEditText(EditTextExt editText) {
        mEditTextExt = editText;
    }

    void bindProgressBar(ProgressBar progressBar) {
        mProgressBar = progressBar;
    }
}
