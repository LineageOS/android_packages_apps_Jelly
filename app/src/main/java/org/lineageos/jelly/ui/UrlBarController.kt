/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
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
    private val editor: EditText,
    private val secureIcon: ImageView
) : OnFocusChangeListener {
    private var url: String? = null
    private var title: String? = null
    private var loading = false
    private var urlBarHasFocus = false
    fun onPageLoadStarted(url: String?) {
        this.url = url
        loading = true
        if (!urlBarHasFocus) {
            updateUrlBarText()
        }
        updateSecureIconVisibility()
    }

    fun onPageLoadFinished(context: Context, certificate: SslCertificate?) {
        loading = false
        if (!urlBarHasFocus) {
            updateUrlBarText()
        }
        updateSecureIconVisibility()
        updateSSLCertificateDialog(context, certificate)
    }

    fun onTitleReceived(title: String?) {
        this.title = title
        if (!urlBarHasFocus) {
            updateUrlBarText()
        }
    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {
        urlBarHasFocus = hasFocus
        updateUrlBarText()
        updateSecureIconVisibility()
    }

    private fun updateSecureIconVisibility() {
        secureIcon.visibility = if (!loading && !urlBarHasFocus && isSecure) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun updateUrlBarText() {
        val text = if (!urlBarHasFocus && !loading && title != null) title else url
        editor.setTextKeepState(text ?: "")
    }

    private val isSecure = url?.startsWith("https") == true

    private fun updateSSLCertificateDialog(context: Context, certificate: SslCertificate?) {
        certificate?.let { cert ->
            // Show the dialog if you tap the lock icon and the cert is valid
            secureIcon.setOnClickListener {
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
                val domainString = Uri.parse(url).host

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

                // Build and show the dialog
                AlertDialog.Builder(context)
                    .setTitle(R.string.ssl_cert_dialog_title)
                    .setView(view)
                    .setNegativeButton(R.string.ssl_cert_dialog_dismiss, null)
                    .create()
                    .show()
            }
        }
    }

    init {
        editor.onFocusChangeListener = this
        editor.setSelectAllOnFocus(true)
    }
}
