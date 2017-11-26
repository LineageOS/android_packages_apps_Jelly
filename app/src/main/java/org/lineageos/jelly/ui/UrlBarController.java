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

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import org.lineageos.jelly.R;

import java.text.DateFormat;
import java.util.Date;

public class UrlBarController implements View.OnFocusChangeListener {
    private EditText mEditor;
    private ImageView mSecureIcon;

    private String mUrl;
    private String mTitle;
    private boolean mLoading;
    private boolean mUrlBarHasFocus;

    public UrlBarController(EditText editor, ImageView secureIcon) {
        mEditor = editor;
        mSecureIcon = secureIcon;
        mEditor.setOnFocusChangeListener(this);
        mEditor.setSelectAllOnFocus(true);
    }

    public void onPageLoadStarted(String url) {
        mUrl = url;
        mLoading = true;
        if (!mUrlBarHasFocus) {
            updateUrlBarText();
        }
        updateSecureIconVisibility();
    }

    public void onPageLoadFinished(Context context, SslCertificate certificate) {
        mLoading = false;
        if (!mUrlBarHasFocus) {
            updateUrlBarText();
        }
        updateSecureIconVisibility();
        updateSSLCertificateDialog(context, certificate);
    }

    public void onTitleReceived(String title) {
        mTitle = title;
        if (!mUrlBarHasFocus) {
            updateUrlBarText();
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        mUrlBarHasFocus = hasFocus;
        updateUrlBarText();
        updateSecureIconVisibility();
    }

    private void updateSecureIconVisibility() {
        mSecureIcon.setVisibility(
                !mLoading && !mUrlBarHasFocus && isSecure() ? View.VISIBLE : View.GONE);
    }

    private void updateUrlBarText() {
        final String text = !mUrlBarHasFocus && !mLoading && mTitle != null ? mTitle : mUrl;
        mEditor.setTextKeepState(text != null ? text : "");
    }

    private boolean isSecure() {
        return mUrl != null && mUrl.startsWith("https");
    }

    private void updateSSLCertificateDialog(Context context, SslCertificate certificate) {
        if (certificate == null) return;

        // Show the dialog if you tap the lock icon and the cert is valid
        mSecureIcon.setOnClickListener((v) -> {
            View view = LayoutInflater
                    .from(context).inflate(R.layout.dialog_ssl_certificate_info, null);

            // Get the text views
            KeyValueView domainView = (KeyValueView) view.findViewById(R.id.domain);
            KeyValueView issuedToCNView = (KeyValueView) view.findViewById(R.id.issued_to_cn);
            KeyValueView issuedToOView = (KeyValueView) view.findViewById(R.id.issued_to_o);
            KeyValueView issuedToUNView = (KeyValueView) view.findViewById(R.id.issued_to_un);
            KeyValueView issuedByCNView = (KeyValueView) view.findViewById(R.id.issued_by_cn);
            KeyValueView issuedByOView = (KeyValueView) view.findViewById(R.id.issued_by_o);
            KeyValueView issuedByUNView = (KeyValueView) view.findViewById(R.id.issued_by_un);
            KeyValueView startDateView = (KeyValueView) view.findViewById(R.id.start_date);
            KeyValueView endDateView = (KeyValueView) view.findViewById(R.id.end_date);

            // Get the domain name
            String domainString = Uri.parse(mUrl).getHost();

            // Get the validity dates
            Date startDate = certificate.getValidNotBeforeDate();
            Date endDate = certificate.getValidNotAfterDate();

            // Update TextViews
            domainView.setText(R.string.ssl_cert_dialog_domain_label, domainString);
            issuedToCNView.setText(R.string.ssl_cert_dialog_common_name,
                    certificate.getIssuedTo().getCName());
            issuedToOView.setText(R.string.ssl_cert_dialog_organization,
                    certificate.getIssuedTo().getOName());
            issuedToUNView.setText(R.string.ssl_cert_dialog_organizational_unit,
                    certificate.getIssuedTo().getUName());
            issuedByCNView.setText(R.string.ssl_cert_dialog_common_name,
                    certificate.getIssuedBy().getCName());
            issuedByOView.setText(R.string.ssl_cert_dialog_organization,
                    certificate.getIssuedBy().getOName());
            issuedByUNView.setText(R.string.ssl_cert_dialog_organizational_unit,
                    certificate.getIssuedBy().getUName());
            startDateView.setText(R.string.ssl_cert_dialog_valid_not_before,
                    DateFormat.getDateTimeInstance().format(startDate));
            endDateView.setText(R.string.ssl_cert_dialog_valid_not_after,
                    DateFormat.getDateTimeInstance().format(endDate));

            // Build and show the dialog
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.ssl_cert_dialog_title))
                    .setView(view)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
        });
    }
}
