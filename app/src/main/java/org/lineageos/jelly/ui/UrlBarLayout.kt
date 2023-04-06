/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.net.http.SslCertificate
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.view.isVisible
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.lineageos.jelly.R
import org.lineageos.jelly.suggestions.SuggestionsAdapter
import org.lineageos.jelly.utils.UiUtils
import java.text.DateFormat

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

    private val layoutInflater = LayoutInflater.from(context)

    enum class UrlBarMode {
        URL,
        SEARCH,
    }
    var currentMode = UrlBarMode.URL
        set(value) {
            field = value

            urlBarLayoutGroupUrl.isVisible = value == UrlBarMode.URL
            urlBarLayoutGroupSearch.isVisible = value == UrlBarMode.SEARCH
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

            secureButton.isVisible = !value && url?.startsWith("https://") == true
            loadingProgressIndicator.isVisible = value
        }

    var url: String? = null
        set(value) {
            field = value

            autoCompleteTextView.setText(value)
        }
    private var certificate: SslCertificate? = null

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

    // SSL dialog
    private val sslDialogView = layoutInflater.inflate(
        R.layout.dialog_ssl_certificate_info, LinearLayout(context)
    )
    private val domainView = sslDialogView.findViewById<TextView>(R.id.domain)
    private val issuedToCNView = sslDialogView.findViewById<KeyValueView>(R.id.issuedToCnView)
    private val issuedToOView = sslDialogView.findViewById<KeyValueView>(R.id.issuedToOView)
    private val issuedToUNView = sslDialogView.findViewById<KeyValueView>(R.id.issuedToUnView)
    private val issuedByCNView = sslDialogView.findViewById<KeyValueView>(R.id.issuedByCnView)
    private val issuedByOView = sslDialogView.findViewById<KeyValueView>(R.id.issuedByOView)
    private val issuedByUNView = sslDialogView.findViewById<KeyValueView>(R.id.issuedByUnView)
    private val issuedOnView = sslDialogView.findViewById<KeyValueView>(R.id.issuedOnView)
    private val expiresOnView = sslDialogView.findViewById<KeyValueView>(R.id.expiresOnView)
    private val sslDialog = AlertDialog.Builder(context)
        .setTitle(R.string.ssl_cert_dialog_title)
        .setView(sslDialogView)
        .setNegativeButton(R.string.ssl_cert_dialog_dismiss, null)
        .create()

    init {
        inflate(context, R.layout.url_bar_layout, this)
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
                    UiUtils.hideKeyboard(autoCompleteTextView)
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
                    UiUtils.hideKeyboard(autoCompleteTextView)
                    onLoadUrlCallback?.invoke(autoCompleteTextView.text.toString())
                    autoCompleteTextView.clearFocus()
                    true
                }
                else -> false
            }
        }
        autoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { _, view: View, _, _ ->
                val titleTextView = (view.findViewById<View>(R.id.titleTextView) as TextView).text
                val url = titleTextView.toString()
                UiUtils.hideKeyboard(autoCompleteTextView)
                autoCompleteTextView.clearFocus()
                onLoadUrlCallback?.invoke(url)
            }
        if (isIncognito) {
            autoCompleteTextView.imeOptions = autoCompleteTextView.imeOptions or
                    EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        }

        moreButton.setOnClickListener { onMoreButtonClickCallback?.invoke() }

        // Set secure button callback
        secureButton.setOnClickListener {
            certificate?.let { cert ->
                // Get the domain name
                val domainString = url?.let { Uri.parse(it).host }

                // Get the validity dates
                val startDate = cert.validNotBeforeDate
                val endDate = cert.validNotAfterDate

                // Update TextViews
                domainView.text = domainString
                issuedToCNView.setText(
                    R.string.ssl_cert_dialog_common_name,
                    cert.issuedTo.cName
                )
                issuedToOView.setText(
                    R.string.ssl_cert_dialog_organization,
                    cert.issuedTo.oName
                )
                issuedToUNView.setText(
                    R.string.ssl_cert_dialog_organizational_unit,
                    cert.issuedTo.uName
                )
                issuedByCNView.setText(
                    R.string.ssl_cert_dialog_common_name,
                    cert.issuedBy.cName
                )
                issuedByOView.setText(
                    R.string.ssl_cert_dialog_organization,
                    cert.issuedBy.oName
                )
                issuedByUNView.setText(
                    R.string.ssl_cert_dialog_organizational_unit,
                    cert.issuedBy.uName
                )
                issuedOnView.setText(
                    R.string.ssl_cert_dialog_issued_on,
                    DateFormat.getDateTimeInstance().format(startDate)
                )
                expiresOnView.setText(
                    R.string.ssl_cert_dialog_expires_on,
                    DateFormat.getDateTimeInstance().format(endDate)
                )

                // Show the dialog
                sslDialog.show()
            }
        }

        // Set search callbacks
        searchEditText.setOnFocusChangeListener { view, hasFocus ->
            onFocusChange(view, hasFocus)
        }
        searchEditText.setOnEditorActionListener { view, actionId, _ ->
            return@setOnEditorActionListener when(actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    UiUtils.hideKeyboard(view)
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
    }

    fun onPageLoadFinished(certificate: SslCertificate?) {
        this.certificate = certificate
        isLoading = false
    }

    private fun clearSearch() {
        searchEditText.setText("")
        searchPositionInfo = EMPTY_SEARCH_RESULT
        onClearSearchCallback?.invoke()
    }

    private fun onFocusChange(view: View, hasFocus: Boolean) {
        if (!hasFocus) {
            UiUtils.hideKeyboard(view)
        }
    }

    companion object {
        private val EMPTY_SEARCH_RESULT = Pair(0, 0)
    }
}
