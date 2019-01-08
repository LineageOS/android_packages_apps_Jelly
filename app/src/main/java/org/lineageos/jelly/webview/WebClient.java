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

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.lineageos.jelly.IntentFilterCompat;
import org.lineageos.jelly.R;
import org.lineageos.jelly.ui.UrlBarController;
import org.lineageos.jelly.utils.IntentUtils;
import org.lineageos.jelly.utils.UrlUtils;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

class WebClient extends WebViewClient {
    private UrlBarController mUrlBarController;

    WebClient(UrlBarController urlBarController) {
        super();
        mUrlBarController = urlBarController;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        mUrlBarController.onPageLoadStarted(url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        mUrlBarController.onPageLoadFinished(view.getContext(), view.getCertificate());
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (request.isForMainFrame()) {
            WebViewExt webViewExt = (WebViewExt) view;
            String url = request.getUrl().toString();
            boolean needsLookup = request.hasGesture()
                    || !TextUtils.equals(url, webViewExt.getLastLoadedUrl());

            if (!webViewExt.isIncognito()
                    && needsLookup
                    && !request.isRedirect()
                    && startActivityForUrl(view, url)) {
                return true;
            } else if (!webViewExt.getRequestHeaders().isEmpty()) {
                webViewExt.followUrl(url);
                return true;
            }
        }

        return false;
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view,
                                          HttpAuthHandler handler, String host, String realm) {
        Context context = view.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View dialogView = layoutInflater.inflate(R.layout.auth_dialog, new LinearLayout(context));
        EditText username = dialogView.findViewById(R.id.username);
        EditText password = dialogView.findViewById(R.id.password);
        TextView auth_detail = dialogView.findViewById(R.id.auth_detail);
        String text = context.getString(R.string.auth_dialog_detail, view.getUrl());
        auth_detail.setText(text);
        builder.setView(dialogView)
                .setTitle(R.string.auth_dialog_title)
                .setPositiveButton(R.string.auth_dialog_login,
                        (dialog, whichButton) -> handler.proceed(
                                username.getText().toString(), password.getText().toString()))
                .setNegativeButton(android.R.string.cancel,
                        (dialog, whichButton) -> handler.cancel())
                .setOnDismissListener(dialog -> handler.cancel())
                .show();
    }

    private boolean startActivityForUrl(WebView view, String url) {
        Intent intent;
        Context context = view.getContext();
        try {
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException ex) {
            return false;
        }

        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setComponent(null);
        intent.setSelector(null);

        Matcher m = UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url);
        if (m.matches()) {
            Intent chooserIntent = makeHandlerChooserIntent(context, intent, url);
            if (chooserIntent != null) {
                intent = chooserIntent;
            } else {
                // There only are browsers for this URL, handle it ourselves
                return false;
            }
        } else {
            String packageName = intent.getPackage();
            if (packageName != null
                    && context.getPackageManager().resolveActivity(intent, 0) == null) {
                // Explicit intent, but app is not installed - try to redirect to Play Store
                Uri storeUri = Uri.parse("market://search?q=pname:" + packageName);
                intent = new Intent(Intent.ACTION_VIEW, storeUri)
                        .addCategory(Intent.CATEGORY_BROWSABLE);
            }
        }

        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Snackbar.make(view, context.getString(R.string.error_no_activity_found),
                    Snackbar.LENGTH_LONG).show();
        }
        return false;
    }

    private Intent makeHandlerChooserIntent(Context context, Intent intent, String url) {
        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> activities = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
        if (activities == null || activities.isEmpty()) {
            return null;
        }

        final ArrayList<Intent> chooserIntents = new ArrayList<>();
        final String ourPackageName = context.getPackageName();

        activities.sort(new ResolveInfo.DisplayNameComparator(pm));

        for (ResolveInfo resolveInfo : activities) {
            IntentFilter filter = resolveInfo.filter;
            ActivityInfo info = resolveInfo.activityInfo;
            if (!info.enabled || !info.exported) {
                continue;
            }
            if (filter == null) {
                continue;
            }
            if (IntentFilterCompat.filterIsBrowser(filter)
                    && !TextUtils.equals(info.packageName, ourPackageName)) {
                continue;
            }

            Intent targetIntent = new Intent(intent);
            targetIntent.setPackage(info.packageName);
            chooserIntents.add(targetIntent);
        }

        if (chooserIntents.isEmpty()) {
            return null;
        }

        final Intent lastIntent = chooserIntents.remove(chooserIntents.size() - 1);
        if (chooserIntents.isEmpty()) {
            // there was only one, no need to show the chooser
            return TextUtils.equals(lastIntent.getPackage(), ourPackageName) ? null : lastIntent;
        }

        Intent changeIntent = new Intent(IntentUtils.EVENT_URL_RESOLVED)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
                .putExtra(IntentUtils.EXTRA_URL, url);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, changeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        Intent chooserIntent = Intent.createChooser(lastIntent, null);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                chooserIntents.toArray(new Intent[chooserIntents.size()]));
        chooserIntent.putExtra(Intent.EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER, pi.getIntentSender());
        return chooserIntent;
    }
}
