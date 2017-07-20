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

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.lineageos.jelly.utils.UiUtils;

public class SearchBarController implements
        TextWatcher, TextView.OnEditorActionListener, WebView.FindListener, View.OnClickListener {
    public interface OnCancelListener {
        void onCancelSearch();
    }

    private WebView mWebView;
    private EditText mEditor;
    private TextView mStatus;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;
    private ImageButton mCancelButton;
    private OnCancelListener mListener;
    private boolean mHasStartedSearch;
    private int mCurrentResultPosition;
    private int mTotalResultCount;

    public SearchBarController(WebView webView, EditText editor, TextView status,
                               ImageButton prevButton, ImageButton nextButton,
                               ImageButton cancelButton, OnCancelListener listener) {
        mWebView = webView;
        mEditor = editor;
        mStatus = status;
        mNextButton = nextButton;
        mPrevButton = prevButton;
        mCancelButton = cancelButton;
        mListener = listener;

        mEditor.addTextChangedListener(this);
        mEditor.setOnEditorActionListener(this);
        mWebView.setFindListener(this);
        mPrevButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
    }

    public void onShow() {
        mEditor.requestFocus();
        UiUtils.showKeyboard(mEditor);
        clearSearchResults();
        updateNextAndPrevButtonEnabledState();
        updateStatusText();
    }

    public void onCancel() {
        mStatus.setText(null);
        mWebView.clearMatches();
        mListener.onCancelSearch();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        startSearch();
        updateNextAndPrevButtonEnabledState();
    }

    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            UiUtils.hideKeyboard(view);
            startSearch();
            return true;
        }
        return false;
    }

    @Override
    public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches,
                                     boolean isDoneCounting) {
        mCurrentResultPosition = activeMatchOrdinal;
        mTotalResultCount = numberOfMatches;

        updateNextAndPrevButtonEnabledState();
        updateStatusText();
    }

    @Override
    public void onClick(View view) {
        UiUtils.hideKeyboard(mEditor);
        if (view == mCancelButton) {
            onCancel();
        } else if (!mHasStartedSearch) {
            startSearch();
        } else {
            mWebView.findNext(view == mNextButton);
        }
    }

    private void startSearch() {
        String query = getQuery();
        if (TextUtils.isEmpty(query)) {
            clearSearchResults();
            mStatus.setText(null);
        } else {
            mWebView.findAllAsync(query);
            mHasStartedSearch = true;
        }
        updateStatusText();
    }

    private void clearSearchResults() {
        mCurrentResultPosition = -1;
        mTotalResultCount = -1;
        mWebView.clearMatches();
        mHasStartedSearch = false;
    }

    private void updateNextAndPrevButtonEnabledState() {
        boolean hasText = !TextUtils.isEmpty(getQuery());
        UiUtils.setImageButtonEnabled(mPrevButton,
                hasText && (!mHasStartedSearch || mCurrentResultPosition > 0));
        UiUtils.setImageButtonEnabled(mNextButton,
                hasText && (!mHasStartedSearch || mCurrentResultPosition < (mTotalResultCount - 1)));
    }

    private void updateStatusText() {
        if (mTotalResultCount > 0) {
            mStatus.setText((mCurrentResultPosition + 1) + "/" + mTotalResultCount);
        } else {
            mStatus.setText(null);
        }
    }

    private String getQuery() {
        Editable s = mEditor.getText();
        return s != null ? s.toString() : null;
    }
}
