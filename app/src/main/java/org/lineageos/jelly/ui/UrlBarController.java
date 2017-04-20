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

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

import org.lineageos.jelly.R;

public class UrlBarController implements View.OnFocusChangeListener {
    private EditTextExt mEditor;
    private ImageView mIndicatorIcon;

    private String mUrl;
    private String mTitle;
    private boolean mLoading;
    private boolean mUrlBarHasFocus;
    private boolean mIncognito;

    public UrlBarController(EditTextExt editor, ImageView customIcon, boolean incognito) {
        mEditor = editor;
        mIndicatorIcon = customIcon;
        mIncognito = incognito;
        mEditor.setOnFocusChangeListener(this);

        if (mIncognito) {
            customIcon.setImageDrawable(
                    ContextCompat.getDrawable(editor.getContext(), R.drawable.ic_incognito));
            customIcon.setVisibility(View.VISIBLE);
        } else {
            customIcon.setImageDrawable(
                    ContextCompat.getDrawable(editor.getContext(), R.drawable.ic_lock));
        }
    }

    public void onPageLoadStarted(String url) {
        mUrl = url;
        mLoading = true;
        updateUrlBarText();
        updateIconVisibility();
    }

    public void onPageLoadFinished() {
        mLoading = false;
        updateUrlBarText();
        updateIconVisibility();
    }

    public void onTitleReceived(String title) {
        mTitle = title;
        updateUrlBarText();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        mUrlBarHasFocus = hasFocus;
        updateUrlBarText();
        updateIconVisibility();

        if (hasFocus) {
            mEditor.selectAll();
        }
    }

    private void updateIconVisibility() {
        boolean showIcon = mIncognito
                ? !mUrlBarHasFocus
                : !mLoading && !mUrlBarHasFocus && isSecure();
        mIndicatorIcon.setVisibility(showIcon ? View.VISIBLE : View.GONE);
    }

    private void updateUrlBarText() {
        boolean showTitle = !mUrlBarHasFocus && !mLoading && mTitle != null && isSecure();
        mEditor.setText(showTitle ? mTitle : mUrl);
    }

    private boolean isSecure() {
        return mUrl != null && mUrl.startsWith("https");
    }
}
