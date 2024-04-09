/*
 * SPDX-FileCopyrightText: 2023-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ui

import android.content.Context
import android.util.AttributeSet
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
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.lineageos.jelly.R
import org.lineageos.jelly.ext.requireActivity
import org.lineageos.jelly.ext.viewModels
import org.lineageos.jelly.model.LoadingStatus
import org.lineageos.jelly.suggestions.SuggestionsAdapter
import org.lineageos.jelly.utils.UiUtils
import org.lineageos.jelly.viewmodels.WebViewModel
import kotlin.reflect.safeCast

/**
 * App's main URL and search view.
 */
class UrlBarLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    // View models
    private val model: WebViewModel by viewModels<WebViewModel>()

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

    private var wasKeyboardVisible = false

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

    // Adapters
    private val suggestionsAdapter = SuggestionsAdapter(context)

    init {
        inflate(context, R.layout.url_bar_layout, this)

        autoCompleteTextView.setOnFocusChangeListener { view, hasFocus ->
            onFocusChange(view, hasFocus)
        }
        autoCompleteTextView.setAdapter(suggestionsAdapter)
        autoCompleteTextView.setOnEditorActionListener { _, actionId: Int, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_UNSPECIFIED,
                EditorInfo.IME_ACTION_SEARCH -> {
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

        moreButton.setOnClickListener { onMoreButtonClickCallback?.invoke() }

        // Set secure button callback
        secureButton.setOnClickListener {
            model.loadingStatus.value?.let { loadingStatus ->
                LoadingStatus.Success::class.safeCast(loadingStatus)?.let {
                    sslCertificateInfoDialog.setLoadingStatus(it)
                    sslCertificateInfoDialog.onSslError(model.sslError.value)
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
                EditorInfo.IME_ACTION_UNSPECIFIED,
                EditorInfo.IME_ACTION_SEARCH -> {
                    UiUtils.hideKeyboard(requireActivity().window, view)
                    searchEditText.text?.toString()?.takeUnless { it.isEmpty() }?.also {
                        onStartSearchCallback?.invoke(it)
                    } ?: run {
                        clearSearch()
                    }
                    searchEditText.clearFocus()
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        viewTreeObserver.addOnGlobalLayoutListener(keyboardListener)

        val viewTreeLifecycleOwner = findViewTreeLifecycleOwner()!!

        model.isIncognito.observe(viewTreeLifecycleOwner) {
            incognitoIcon.isVisible = it

            autoCompleteTextView.imeOptions = when (it) {
                true -> autoCompleteTextView.imeOptions or
                        EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING

                false -> autoCompleteTextView.imeOptions and
                        EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING.inv()
            }
        }

        model.loadingProgress.observe(viewTreeLifecycleOwner) { loadingProgress ->
            loadingProgressIndicator.progress = loadingProgress
        }

        model.loadingStatus.observe(viewTreeLifecycleOwner) { loadingStatus ->
            loadingProgressIndicator.isVisible = loadingStatus is LoadingStatus.Loading

            autoCompleteTextView.setText(loadingStatus.url)
            secureButton.isVisible = loadingStatus.url.startsWith("https://") == true
        }

        model.sslError.observe(viewTreeLifecycleOwner) { sslError ->
            secureButton.setImageResource(
                sslError?.let {
                    R.drawable.ic_warning
                } ?: R.drawable.ic_lock
            )
        }

        model.searchPosition.observe(viewTreeLifecycleOwner) { searchPosition ->
            val hasInput = searchPosition != null

            searchResultCountTextView.isVisible = hasInput
            searchClearButton.isVisible = hasInput

            searchPosition?.let {
                val hasResults = it.second > 0
                searchPreviousButton.isEnabled = hasResults && it.first > 0
                searchNextButton.isEnabled = hasResults && it.first + 1 < it.second
                @Suppress("SetTextI18n")
                searchResultCountTextView.text =
                    "${if (hasResults) it.first + 1 else 0}/${it.second}"
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        viewTreeObserver.removeOnGlobalLayoutListener(keyboardListener)
    }

    private fun clearSearch() {
        searchEditText.setText("")
        onClearSearchCallback?.invoke()
    }

    private fun onFocusChange(view: View, hasFocus: Boolean) {
        if (hasFocus) {
            UiUtils.showKeyboard(requireActivity().window, view)
        } else {
            UiUtils.hideKeyboard(requireActivity().window, view)
        }
    }
}
