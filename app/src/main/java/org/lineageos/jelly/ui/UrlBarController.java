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
            TextViewExt domainView = (TextViewExt) view.findViewById(R.id.domain);
            TextViewExt issuedToCNView = (TextViewExt) view.findViewById(R.id.issued_to_cn);
            TextViewExt issuedToOView = (TextViewExt) view.findViewById(R.id.issued_to_o);
            TextViewExt issuedToUNView = (TextViewExt) view.findViewById(R.id.issued_to_un);
            TextViewExt issuedByCNView = (TextViewExt) view.findViewById(R.id.issued_by_cn);
            TextViewExt issuedByOView = (TextViewExt) view.findViewById(R.id.issued_by_o);
            TextViewExt issuedByUNView = (TextViewExt) view.findViewById(R.id.issued_by_un);
            TextViewExt startDateView = (TextViewExt) view.findViewById(R.id.start_date);
            TextViewExt endDateView = (TextViewExt) view.findViewById(R.id.end_date);

            // Generate the labels
            String domainLabel = context.getString(R.string.ssl_cert_dialog_domain_label);
            String cnLabel = context.getString(R.string.ssl_cert_dialog_common_name);
            String orgLabel = context.getString(R.string.ssl_cert_dialog_organization);
            String unLabel = context.getString(R.string.ssl_cert_dialog_organizational_unit);
            String startDateLabel = context.getString(R.string.ssl_cert_dialog_valid_not_before);
            String endDateLabel = context.getString(R.string.ssl_cert_dialog_valid_not_after);

            // Get the domain name
            String domainString = Uri.parse(mUrl).getHost();

            // Get the validity dates
            Date startDate = certificate.getValidNotBeforeDate();
            Date endDate = certificate.getValidNotAfterDate();

            // Update TextViews
            domainView.setText(domainLabel, domainString);
            issuedToCNView.setText(cnLabel, certificate.getIssuedTo().getCName());
            issuedToOView.setText(orgLabel, certificate.getIssuedTo().getOName());
            issuedToUNView.setText(unLabel, certificate.getIssuedTo().getUName());
            issuedByCNView.setText(cnLabel, certificate.getIssuedBy().getCName());
            issuedByOView.setText(orgLabel, certificate.getIssuedBy().getOName());
            issuedByUNView.setText(unLabel, certificate.getIssuedBy().getUName());
            startDateView.setText(startDateLabel,
                    DateFormat.getDateTimeInstance().format(startDate));
            endDateView.setText(endDateLabel,
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
