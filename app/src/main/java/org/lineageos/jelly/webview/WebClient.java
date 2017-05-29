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
package org.lineageos.jelly.webview;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.lineageos.jelly.R;

class WebClient extends WebViewClient {

    WebClient() {
        super();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        Context context = view.getContext();
        if (!url.startsWith("http")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(request.getUrl());
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Snackbar.make(view, context.getString(R.string.error_no_activity_found),
                        Snackbar.LENGTH_LONG).show();
            }
            return true;
        }

        return false;
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view,
                                          HttpAuthHandler handler, String host, String realm) {
        Context mContext = view.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        // Get the layout inflater
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View dialogView = layoutInflater.inflate(R.layout.auth_dialog, new LinearLayout(mContext));
        EditText username = (EditText) dialogView.findViewById(R.id.username);
        EditText password = (EditText) dialogView.findViewById(R.id.password);
        String text = mContext.getString(R.string.auth_dialog_detail, view.getUrl());
        TextView auth_detail = (TextView) dialogView.findViewById(R.id.auth_detail);
        auth_detail.setText(text);
        builder.setView(dialogView)
                .setCancelable(false)
                .setTitle(R.string.auth_dialog_title)
                .setPositiveButton(R.string.auth_dialog_login,
                        (dialog, whichButton) -> handler.proceed(
                                username.getText().toString(), password.getText().toString()))
                .setNegativeButton(android.R.string.cancel,
                        (dialog, whichButton) -> handler.cancel())
                .show();
    }
}
