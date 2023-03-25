/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebView.FindListener
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import org.lineageos.jelly.utils.UiUtils

class SearchBarController(
    private val webView: WebView,
    private val editor: EditText,
    private val status: TextView,
    private val prevButton: ImageButton,
    private val nextButton: ImageButton,
    private val cancelButton: ImageButton,
    private val listener: OnCancelListener
) : TextWatcher, OnEditorActionListener, FindListener, View.OnClickListener {
    private var hasStartedSearch = false
    private var currentResultPosition = 0
    private var totalResultCount = 0
    private val query: String?
        get() = editor.text?.toString()

    init {
        editor.addTextChangedListener(this)
        editor.setOnEditorActionListener(this)
        webView.setFindListener(this)
        prevButton.setOnClickListener(this)
        nextButton.setOnClickListener(this)
        cancelButton.setOnClickListener(this)
    }

    fun onShow() {
        editor.requestFocus()
        UiUtils.showKeyboard(editor)
        clearSearchResults()
        updateNextAndPrevButtonEnabledState()
        updateStatusText()
    }

    fun onCancel() {
        status.text = null
        webView.clearMatches()
        listener.onCancelSearch()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable) {
        startSearch()
        updateNextAndPrevButtonEnabledState()
    }

    override fun onEditorAction(view: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            UiUtils.hideKeyboard(view)
            startSearch()
            return true
        }
        return false
    }

    override fun onFindResultReceived(
        activeMatchOrdinal: Int, numberOfMatches: Int,
        isDoneCounting: Boolean
    ) {
        currentResultPosition = activeMatchOrdinal
        totalResultCount = numberOfMatches
        updateNextAndPrevButtonEnabledState()
        updateStatusText()
    }

    override fun onClick(view: View) {
        UiUtils.hideKeyboard(editor)
        when {
            view === cancelButton -> {
                onCancel()
            }
            !hasStartedSearch -> {
                startSearch()
            }
            else -> {
                webView.findNext(view === nextButton)
            }
        }
    }

    private fun startSearch() {
        query?.takeUnless { it.isEmpty() }?.also {
            webView.findAllAsync(it)
            hasStartedSearch = true
        } ?: run {
            clearSearchResults()
            status.text = null
        }
        updateStatusText()
    }

    private fun clearSearchResults() {
        currentResultPosition = -1
        totalResultCount = -1
        webView.clearMatches()
        hasStartedSearch = false
    }

    private fun updateNextAndPrevButtonEnabledState() {
        val hasText = !query.isNullOrEmpty()
        UiUtils.setImageButtonEnabled(
            prevButton,
            hasText && (!hasStartedSearch || currentResultPosition > 0)
        )
        UiUtils.setImageButtonEnabled(
            nextButton,
            hasText && (!hasStartedSearch || currentResultPosition < totalResultCount - 1)
        )
    }

    private fun updateStatusText() {
        if (totalResultCount > 0) {
            status.text = (currentResultPosition + 1).toString() + "/" + totalResultCount
        } else {
            status.text = null
        }
    }

    interface OnCancelListener {
        fun onCancelSearch()
    }
}
