/*
 * Copyright (C) 2020 The LineageOS Project
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
package org.lineageos.jelly.ui

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.net.http.SslCertificate
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.lineageos.jelly.R
import java.text.DateFormat

class UrlBarController(
    private val mEditor: EditText,
    private val mSecureIcon: ImageView
) : OnFocusChangeListener {
    private var mUrl: String? = null
    private var mTitle: String? = null
    private var mLoading = false
    private var mUrlBarHasFocus = false
    fun onPageLoadStarted(url: String?) {
        mUrl = url
        mLoading = true
        if (!mUrlBarHasFocus) {
            updateUrlBarText()
        }
        updateSecureIconVisibility()
    }

    fun onPageLoadFinished(context: Context, certificate: SslCertificate?) {
        mLoading = false
        if (!mUrlBarHasFocus) {
            updateUrlBarText()
        }
        updateSecureIconVisibility()
        updateSSLCertificateDialog(context, certificate)
    }

    fun onTitleReceived(title: String?) {
        mTitle = title
        if (!mUrlBarHasFocus) {
            updateUrlBarText()
        }
    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {
        mUrlBarHasFocus = hasFocus
        updateUrlBarText()
        updateSecureIconVisibility()
    }

    private fun updateSecureIconVisibility() {
        mSecureIcon.visibility = if (!mLoading && !mUrlBarHasFocus && isSecure) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun updateUrlBarText() {
        val text = if (!mUrlBarHasFocus && !mLoading && mTitle != null) mTitle else mUrl
        mEditor.setTextKeepState(text ?: "")
    }

    private val isSecure = mUrl != null && mUrl!!.startsWith("https")

    private fun updateSSLCertificateDialog(context: Context, certificate: SslCertificate?) {
        if (certificate == null) return

        // Show the dialog if you tap the lock icon and the cert is valid
        mSecureIcon.setOnClickListener {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_ssl_certificate_info, LinearLayout(context))

            // Get the text views
            val domainView: TextView = view.findViewById(R.id.domain)
            val issuedToCNView: KeyValueView = view.findViewById(R.id.issued_to_cn)
            val issuedToOView: KeyValueView = view.findViewById(R.id.issued_to_o)
            val issuedToUNView: KeyValueView = view.findViewById(R.id.issued_to_un)
            val issuedByCNView: KeyValueView = view.findViewById(R.id.issued_by_cn)
            val issuedByOView: KeyValueView = view.findViewById(R.id.issued_by_o)
            val issuedByUNView: KeyValueView = view.findViewById(R.id.issued_by_un)
            val issuedOnView: KeyValueView = view.findViewById(R.id.issued_on)
            val expiresOnView: KeyValueView = view.findViewById(R.id.expires_on)

            // Get the domain name
            val domainString = Uri.parse(mUrl).host

            // Get the validity dates
            val startDate = certificate.validNotBeforeDate
            val endDate = certificate.validNotAfterDate

            // Update TextViews
            domainView.text = domainString
            issuedToCNView.setText(
                R.string.ssl_cert_dialog_common_name,
                certificate.issuedTo.cName
            )
            issuedToOView.setText(
                R.string.ssl_cert_dialog_organization,
                certificate.issuedTo.oName
            )
            issuedToUNView.setText(
                R.string.ssl_cert_dialog_organizational_unit,
                certificate.issuedTo.uName
            )
            issuedByCNView.setText(
                R.string.ssl_cert_dialog_common_name,
                certificate.issuedBy.cName
            )
            issuedByOView.setText(
                R.string.ssl_cert_dialog_organization,
                certificate.issuedBy.oName
            )
            issuedByUNView.setText(
                R.string.ssl_cert_dialog_organizational_unit,
                certificate.issuedBy.uName
            )
            issuedOnView.setText(
                R.string.ssl_cert_dialog_issued_on,
                DateFormat.getDateTimeInstance().format(startDate)
            )
            expiresOnView.setText(
                R.string.ssl_cert_dialog_expires_on,
                DateFormat.getDateTimeInstance().format(endDate)
            )

            // Build and show the dialog
            AlertDialog.Builder(context)
                .setTitle(R.string.ssl_cert_dialog_title)
                .setView(view)
                .setNegativeButton(R.string.ssl_cert_dialog_dismiss, null)
                .create()
                .show()
        }
    }

    init {
        mEditor.onFocusChangeListener = this
        mEditor.setSelectAllOnFocus(true)
    }
}