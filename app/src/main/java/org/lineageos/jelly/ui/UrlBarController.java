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
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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

    public void onPageLoadFinished() {
        mLoading = false;
        if (!mUrlBarHasFocus) {
            updateUrlBarText();
        }
        updateSecureIconVisibility();
    }

    public void onTitleReceived(String title) {
        mTitle = title;
        if (!mUrlBarHasFocus) {
            updateUrlBarText();
        }
    }

    public void updateSSLCertificateDialog(SslCertificate certificate) {
        // This will only update the dialog if the site we're accessing has SSL enabled
        if (certificate != null) {
            Context context = this.mSecureIcon.getContext();
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ssl_certificate_info, null);

            // Get the text views
            TextView domainView = (TextView) dialogView.findViewById(R.id.domain);
            TextView issuedToCNView = (TextView)dialogView.findViewById(R.id.issued_to_cn);
            TextView issuedToOView = (TextView)dialogView.findViewById(R.id.issued_to_o);
            TextView issuedToUNView = (TextView)dialogView.findViewById(R.id.issued_to_un);
            TextView issuedByCNView = (TextView)dialogView.findViewById(R.id.issued_by_cn);
            TextView issuedByOView = (TextView)dialogView.findViewById(R.id.issued_by_o);
            TextView issuedByUNView = (TextView)dialogView.findViewById(R.id.issued_by_un);
            TextView startDateView = (TextView)dialogView.findViewById(R.id.start_date);
            TextView endDateView = (TextView)dialogView.findViewById(R.id.end_date);

            // Generate the labels
            String domainLabel = context.getString(R.string.ssl_cert_dialog_domain_label) + "  ";
            String cnLabel = context.getString(R.string.ssl_cert_dialog_common_name) + "  ";
            String orgLabel = context.getString(R.string.ssl_cert_dialog_organization) + "  ";
            String unLabel = context.getString(R.string.ssl_cert_dialog_organizational_unit) + "  ";
            String startDateLabel = context.getString(R.string.ssl_cert_dialog_valid_not_before) + "  ";
            String endDateLabel = context.getString(R.string.ssl_cert_dialog_valid_not_after) + "  ";

            // Get the domain name
            String domainString = Uri.parse(mUrl).getHost();

            // Get the validity dates
            Date startDate = certificate.getValidNotBeforeDate();
            Date endDate = certificate.getValidNotAfterDate();

            // Create a spannable string builder to apply color to the values
            SpannableStringBuilder domainStringBuilder = new SpannableStringBuilder(domainLabel + domainString);
            SpannableStringBuilder issuedToCNStringBuilder = new SpannableStringBuilder(cnLabel + certificate.getIssuedTo().getCName());
            SpannableStringBuilder issuedToOStringBuilder = new SpannableStringBuilder(orgLabel + certificate.getIssuedTo().getOName());
            SpannableStringBuilder issuedToUNStringBuilder = new SpannableStringBuilder(unLabel + certificate.getIssuedTo().getUName());
            SpannableStringBuilder issuedByCNStringBuilder = new SpannableStringBuilder(cnLabel + certificate.getIssuedBy().getCName());
            SpannableStringBuilder issuedByOStringBuilder = new SpannableStringBuilder(orgLabel + certificate.getIssuedBy().getOName());
            SpannableStringBuilder issuedByUNStringBuilder = new SpannableStringBuilder(unLabel + certificate.getIssuedBy().getUName());
            SpannableStringBuilder startDateStringBuilder = new SpannableStringBuilder(startDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(startDate));
            SpannableStringBuilder endDateStringBuilder = new SpannableStringBuilder(endDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(endDate));

            // Define the color we want to use to highlight the certificate's values
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(context.getColor(R.color.colorAccent));

            // Apply color to certificate values
            domainStringBuilder.setSpan(colorSpan, domainLabel.length(), domainStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            issuedToCNStringBuilder.setSpan(colorSpan, cnLabel.length(), issuedToCNStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            issuedToOStringBuilder.setSpan(colorSpan, orgLabel.length(), issuedToOStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            issuedToUNStringBuilder.setSpan(colorSpan, unLabel.length(), issuedToUNStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            issuedByCNStringBuilder.setSpan(colorSpan, cnLabel.length(), issuedByCNStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            issuedByOStringBuilder.setSpan(colorSpan, orgLabel.length(), issuedByOStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            issuedByUNStringBuilder.setSpan(colorSpan, unLabel.length(), issuedByUNStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            startDateStringBuilder.setSpan(colorSpan, startDateLabel.length(), startDateStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            endDateStringBuilder.setSpan(colorSpan, endDateLabel.length(), endDateStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);

            // Update TextViews
            domainView.setText(domainStringBuilder);
            issuedToCNView.setText(issuedToCNStringBuilder);
            issuedToOView.setText(issuedToOStringBuilder);
            issuedToUNView.setText(issuedToUNStringBuilder);
            issuedByCNView.setText(issuedByCNStringBuilder);
            issuedByOView.setText(issuedByOStringBuilder);
            issuedByUNView.setText(issuedByUNStringBuilder);
            startDateView.setText(startDateStringBuilder);
            endDateView.setText(endDateStringBuilder);

            // Build the dialog
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert)
                    .setTitle(context.getString(R.string.ssl_cert_dialog_title))
                    .setView(dialogView)
                    .setNegativeButton(android.R.string.cancel, null);

            // Create the dialog
            final AlertDialog dialog = dialogBuilder.create();

            // Make it so the dialog will show if you tap the lock icon
            mSecureIcon.setOnTouchListener((v, event) -> {
                dialog.show();
                return false;
            });
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
}
