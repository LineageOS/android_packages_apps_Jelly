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
    private View mProgress;
    private OnCancelListener mListener;
    private boolean mHasStartedSearch;
    private boolean mSearchDone;
    private int mCurrentResultPosition;
    private int mTotalResultCount;

    public SearchBarController(WebView webView, EditText editor, TextView status,
                               ImageButton prevButton, ImageButton nextButton,
                               ImageButton cancelButton, View progress,
                               OnCancelListener listener) {
        mWebView = webView;
        mEditor = editor;
        mStatus = status;
        mNextButton = nextButton;
        mPrevButton = prevButton;
        mCancelButton = cancelButton;
        mProgress = progress;
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
        updateProgressAndStatusVisibility();
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
        mSearchDone = isDoneCounting;

        // Update status text only when search is done
        if (numberOfMatches > 0 && isDoneCounting) {
            mStatus.setText((activeMatchOrdinal + 1) + "/" + numberOfMatches);
        } else {
            mStatus.setText(null);
        }

        updateNextAndPrevButtonEnabledState();
        updateProgressAndStatusVisibility();
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
            mSearchDone = false;
        }
        updateProgressAndStatusVisibility();
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

    private void updateProgressAndStatusVisibility() {
        mProgress.setVisibility(mHasStartedSearch && !mSearchDone ? View.VISIBLE : View.GONE);
        mStatus.setVisibility(mHasStartedSearch && mSearchDone ? View.VISIBLE : View.GONE);
    }

    private String getQuery() {
        Editable s = mEditor.getText();
        return s != null ? s.toString() : null;
    }
}
