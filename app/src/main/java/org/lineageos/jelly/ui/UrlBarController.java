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
package org.lineageos.jelly.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

public class UrlBarController implements View.OnFocusChangeListener {
    private EditText mEditor;
    private ImageView mSecureIcon;

    private String mUrl;
    private String mTitle;
    private boolean mLoading;
    private boolean mUrlBarHasFocus;

    public UrlBarController(EditText editor, ImageView secureIcon) {
        mEditor = editor;
        mSecureIcon = secureIcon;
        mEditor.setOnFocusChangeListener(this);
    }

    public void onPageLoadStarted(String url) {
        mUrl = url;
        mLoading = true;
        updateUrlBarText();
        updateSecureIconVisibility();
    }

    public void onPageLoadFinished() {
        mLoading = false;
        updateUrlBarText();
        updateSecureIconVisibility();
    }

    public void onTitleReceived(String title) {
        mTitle = title;
        updateUrlBarText();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        mUrlBarHasFocus = hasFocus;
        updateUrlBarText();
        updateSecureIconVisibility();
        if (hasFocus) {
            mEditor.selectAll();
        }
    }

    private void updateSecureIconVisibility() {
        mSecureIcon.setVisibility(
                !mLoading && !mUrlBarHasFocus && isSecure() ? View.VISIBLE : View.GONE);
    }

    private void updateUrlBarText() {
        boolean showTitle = !mUrlBarHasFocus && !mLoading && mTitle != null && isSecure();
        mEditor.setText(showTitle ? mTitle : mUrl);
    }

    private boolean isSecure() {
        return mUrl != null && mUrl.startsWith("https");
    }
}
