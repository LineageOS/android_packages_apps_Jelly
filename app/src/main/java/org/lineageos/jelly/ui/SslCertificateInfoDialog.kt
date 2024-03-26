/*
 * SPDX-FileCopyrightText: 2023-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.net.http.SslCertificate
import android.net.http.SslError
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.annotation.UiContext
import org.lineageos.jelly.R
import java.text.DateFormat

class SslCertificateInfoDialog(
    @UiContext context: Context,
) : Dialog(context) {
    private val trustedView by lazy { findViewById<TextView>(R.id.trusted) }
    private val domainView by lazy { findViewById<TextView>(R.id.domain) }
    private val issuedToCNView by lazy { findViewById<KeyValueView>(R.id.issuedToCnView) }
    private val issuedToOView by lazy { findViewById<KeyValueView>(R.id.issuedToOView) }
    private val issuedToUNView by lazy { findViewById<KeyValueView>(R.id.issuedToUnView) }
    private val issuedByCNView by lazy { findViewById<KeyValueView>(R.id.issuedByCnView) }
    private val issuedByOView by lazy { findViewById<KeyValueView>(R.id.issuedByOView) }
    private val issuedByUNView by lazy { findViewById<KeyValueView>(R.id.issuedByUnView) }
    private val issuedOnView by lazy { findViewById<KeyValueView>(R.id.issuedOnView) }
    private val expiresOnView by lazy { findViewById<KeyValueView>(R.id.expiresOnView) }
    private val dismissButton by lazy { findViewById<Button>(R.id.dismissButton) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.ssl_certificate_info_dialog)
        setTitle(R.string.ssl_cert_dialog_title)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dismissButton.setOnClickListener {
            dismiss()
        }
    }

    fun setUrlAndCertificate(
        url: String, certificate: SslCertificate
    ) {
        // Get the domain name
        val domain = Uri.parse(url).host

        // Get the validity dates
        val startDate = certificate.validNotBeforeDate
        val endDate = certificate.validNotAfterDate

        // Update TextViews
        domainView.text = domain
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
    }

    fun onSslError(error: SslError?) {
        val message = when (error?.primaryError) {
            null -> R.string.ssl_cert_dialog_trusted
            SslError.SSL_DATE_INVALID -> R.string.ssl_error_date_invalid
            SslError.SSL_EXPIRED -> R.string.ssl_error_expired
            SslError.SSL_IDMISMATCH -> R.string.ssl_error_mismatch
            SslError.SSL_INVALID -> R.string.ssl_error_invalid
            SslError.SSL_NOTYETVALID -> R.string.ssl_error_not_yet_valid
            SslError.SSL_UNTRUSTED -> R.string.ssl_error_untrusted
            else -> R.string.ssl_error_unknown
        }
        trustedView.setText(message)
    }
}
