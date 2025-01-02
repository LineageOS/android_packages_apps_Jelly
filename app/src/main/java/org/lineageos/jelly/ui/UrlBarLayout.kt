/*
 * SPDX-FileCopyrightText: 2023-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslCertificate
import android.net.http.SslError
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.lineageos.jelly.R
import org.lineageos.jelly.ext.requireActivity
import org.lineageos.jelly.suggestions.SuggestionProvider
import org.lineageos.jelly.suggestions.SuggestionsAdapter
import org.lineageos.jelly.utils.UiUtils
import kotlin.reflect.safeCast

/**
 * App's main URL and search view.
 */
class UrlBarLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    // Views
    private val autoCompleteTextView by lazy { findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView) }
    private val incognitoIcon by lazy { findViewById<ImageButton>(R.id.incognitoIcon) }
    private val loadingProgressIndicator by lazy { findViewById<LinearProgressIndicator>(R.id.loadingProgressIndicator) }
    private val moreButton by lazy { findViewById<ImageButton>(R.id.moreButton)!! }
    private val searchCancelButton by lazy { findViewById<ImageButton>(R.id.searchCancelButton) }
    private val searchClearButton by lazy { findViewById<ImageButton>(R.id.searchClearButton) }
    private val searchEditText by lazy { findViewById<EditText>(R.id.searchEditText) }
    private val searchNextButton by lazy { findViewById<ImageButton>(R.id.searchNextButton) }
    private val searchPreviousButton by lazy { findViewById<ImageButton>(R.id.searchPreviousButton) }
    private val searchResultCountTextView by lazy { findViewById<TextView>(R.id.searchResultCountTextView) }
    private val secureButton by lazy { findViewById<ImageButton>(R.id.secureButton) }
    private val urlBarLayoutGroupSearch by lazy { findViewById<Group>(R.id.urlBarLayoutGroupSearch) }
    private val urlBarLayoutGroupUrl by lazy { findViewById<Group>(R.id.urlBarLayoutGroupUrl) }

    enum class UrlBarMode {
        URL,
        SEARCH,
    }

    var currentMode = UrlBarMode.URL
        set(value) {
            field = value

            urlBarLayoutGroupUrl.isVisible = value == UrlBarMode.URL
            urlBarLayoutGroupSearch.isVisible = value == UrlBarMode.SEARCH

            if (value == UrlBarMode.SEARCH) {
                searchEditText.requestFocus()
            }
        }

    var isIncognito = false
        set(value) {
            field = value

            incognitoIcon.isVisible = value
        }

    var loadingProgress: Int = 100
        set(value) {
            field = value

            loadingProgressIndicator.progress = value
        }
    private var isLoading = false
        set(value) {
            field = value

            loadingProgressIndicator.isVisible = value
        }

    var url: String? = null
        set(value) {
            field = value

            autoCompleteTextView.setText(value)
            secureButton.isVisible = value?.startsWith("https://") == true
        }

    private var certificate: SslCertificate? = null
    private var sslError: SslError? = null

    private var wasKeyboardVisible = false

    // Search
    var searchPositionInfo = Pair(0, 0)
        @SuppressLint("SetTextI18n")
        set(value) {
            field = value

            val hasResults = value.second > 0
            searchPreviousButton.isEnabled = hasResults && value.first > 0
            searchNextButton.isEnabled = hasResults && value.first + 1 < value.second
            searchResultCountTextView.text =
                "${if (hasResults) value.first + 1 else 0}/${value.second}"

            val hasInput = searchEditText.text.isNotEmpty()
            searchResultCountTextView.isVisible = hasInput
            searchClearButton.isVisible = hasInput
        }

    // Callbacks
    var onMoreButtonClickCallback: (() -> Unit)? = null
    var onLoadUrlCallback: ((url: String) -> Unit)? = null
    var onStartSearchCallback: ((query: String) -> Unit)? = null
    var onSearchPositionChangeCallback: ((next: Boolean) -> Unit)? = null
    var onClearSearchCallback: (() -> Unit)? = null

    // Dialogs
    private val sslCertificateInfoDialog by lazy {
        SslCertificateInfoDialog(context).apply {
            create()
        }
    }

    // Listeners
    private val keyboardListener = ViewTreeObserver.OnGlobalLayoutListener {
        val isKeyboardOpen = ViewCompat.getRootWindowInsets(this)
            ?.isVisible(WindowInsetsCompat.Type.ime()) ?: true

        if (!isKeyboardOpen && wasKeyboardVisible) {
            autoCompleteTextView.clearFocus()
            searchEditText.clearFocus()
        }

        wasKeyboardVisible = isKeyboardOpen
    }

    init {
        inflate(context, R.layout.url_bar_layout, this)
        viewTreeObserver.addOnGlobalLayoutListener(keyboardListener)
    }

    override fun onViewRemoved(view: View?) {
        viewTreeObserver.removeOnGlobalLayoutListener(keyboardListener)
    }

    private val suggestionsAdapter = SuggestionsAdapter(context)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        autoCompleteTextView.setOnFocusChangeListener { view, hasFocus ->
            onFocusChange(view, hasFocus)
        }
        autoCompleteTextView.setAdapter(suggestionsAdapter)
        autoCompleteTextView.setOnEditorActionListener { _, actionId: Int, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    UiUtils.hideKeyboard(requireActivity().window, autoCompleteTextView)
                    onLoadUrlCallback?.invoke(autoCompleteTextView.text.toString())
                    autoCompleteTextView.clearFocus()
                    true
                }

                else -> false
            }
        }
        autoCompleteTextView.setOnKeyListener { _, keyCode: Int, _ ->
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    UiUtils.hideKeyboard(requireActivity().window, autoCompleteTextView)
                    onLoadUrlCallback?.invoke(autoCompleteTextView.text.toString())
                    autoCompleteTextView.clearFocus()
                    true
                }

                else -> false
            }
        }
        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            val text = String::class.safeCast(autoCompleteTextView.adapter.getItem(position))
                ?: return@setOnItemClickListener
            UiUtils.hideKeyboard(requireActivity().window, autoCompleteTextView)
            autoCompleteTextView.clearFocus()
            onLoadUrlCallback?.invoke(text)
        }
        if (isIncognito) {
            autoCompleteTextView.imeOptions = autoCompleteTextView.imeOptions or
                    EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        }

        moreButton.setOnClickListener { onMoreButtonClickCallback?.invoke() }

        // Set secure button callback
        secureButton.setOnClickListener {
            certificate?.let { cert ->
                url?.let { url ->
                    sslCertificateInfoDialog.setUrlAndCertificate(url, cert)
                    sslCertificateInfoDialog.onSslError(sslError)
                    sslCertificateInfoDialog.show()
                }
            }
        }

        // Set search callbacks
        searchEditText.setOnFocusChangeListener { view, hasFocus ->
            onFocusChange(view, hasFocus)
        }
        searchEditText.setOnEditorActionListener { view, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    UiUtils.hideKeyboard(requireActivity().window, view)
                    searchEditText.text?.toString()?.takeUnless { it.isEmpty() }?.also {
                        onStartSearchCallback?.invoke(it)
                    } ?: run {
                        clearSearch()
                    }
                    true
                }

                else -> {
                    false
                }
            }
        }
        searchCancelButton.setOnClickListener {
            currentMode = UrlBarMode.URL
            clearSearch()
        }
        searchClearButton.setOnClickListener { clearSearch() }
        searchPreviousButton.setOnClickListener { onSearchPositionChangeCallback?.invoke(false) }
        searchNextButton.setOnClickListener { onSearchPositionChangeCallback?.invoke(true) }
    }

    fun onPageLoadStarted(url: String?) {
        this.url = url
        certificate = null
        isLoading = true
        sslError = null
        secureButton.setImageResource(R.drawable.ic_lock)
    }

    fun onPageLoadFinished(certificate: SslCertificate?) {
        this.certificate = certificate
        isLoading = false
    }

    fun onSslError(error: SslError?) {
        sslError = error
        secureButton.setImageResource(R.drawable.ic_warning)
    }

    fun setSuggestionsProvider(provider: SuggestionProvider) {
        suggestionsAdapter.suggestionProvider = provider
    }

    private fun clearSearch() {
        searchEditText.setText("")
        searchPositionInfo = EMPTY_SEARCH_RESULT
        onClearSearchCallback?.invoke()
    }

    private fun onFocusChange(view: View, hasFocus: Boolean) {
        if (hasFocus) {
            UiUtils.showKeyboard(requireActivity().window, view)
        } else {
            UiUtils.hideKeyboard(requireActivity().window, view)
        }
    }

    companion object {
        private val EMPTY_SEARCH_RESULT = Pair(0, 0)
    }
}
