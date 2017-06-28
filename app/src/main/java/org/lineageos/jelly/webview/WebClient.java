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

import android.content.Context;
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
import org.lineageos.jelly.utils.IntentUtils;

class WebClient extends WebViewClient {
    WebClient() {
        super();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        WebViewExt webViewExt = (WebViewExt) view;
        String url = request.getUrl().toString();
        if (!webViewExt.isIncognito() &&
                (IntentUtils.handleIntentUrl(view, url) ||
                        IntentUtils.startActivityForUrl(view, url))) {
            return true;
        }

        if (webViewExt.getRequestHeaders().isEmpty()) {
            return false;
        } else {
            webViewExt.loadUrl(url);
            return true;
        }
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view,
                                          HttpAuthHandler handler, String host, String realm) {
        Context context = view.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View dialogView = layoutInflater.inflate(R.layout.auth_dialog, new LinearLayout(context));
        EditText username = (EditText) dialogView.findViewById(R.id.username);
        EditText password = (EditText) dialogView.findViewById(R.id.password);
        TextView auth_detail = (TextView) dialogView.findViewById(R.id.auth_detail);
        String text = context.getString(R.string.auth_dialog_detail, view.getUrl());
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
