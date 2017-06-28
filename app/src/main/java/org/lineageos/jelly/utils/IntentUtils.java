/*
 * Copyright (C) 2010 The Android Open Source Project
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
package org.lineageos.jelly.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.webkit.WebView;

import org.lineageos.jelly.R;

import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;

public final class IntentUtils {
    public static boolean startActivityForUrl(WebView view, String url) {
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

        if (context.getPackageManager().resolveActivity(intent, 0) == null) {
            String packageName = intent.getPackage();
            if (packageName != null) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:"
                        + packageName));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                try {
                    context.startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e) {
                    Snackbar.make(view, context.getString(R.string.error_no_activity_found),
                            Snackbar.LENGTH_LONG).show();
                    return false;
                }
            } else {
                return false;
            }
        }

        Matcher m = UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url);
        if (m.matches() && !isSpecializedHandlerAvailable(context, intent)) {
            return false;
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

    /**
     * Search for intent handlers that are specific to this URL aka, specialized
     * apps like google maps or youtube
     */
    private static boolean isSpecializedHandlerAvailable(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> handlers = pm.queryIntentActivities(intent,
                PackageManager.GET_RESOLVED_FILTER);
        if (handlers == null || handlers.isEmpty()) {
            return false;
        }
        for (ResolveInfo resolveInfo : handlers) {
            IntentFilter filter = resolveInfo.filter;
            if (filter == null) {
                // No intent filter matches this intent?
                // Error on the side of staying in the browser, ignore
                continue;
            }

            if (filter.countDataAuthorities() == 0) {
                // Generic handler, skip
                continue;
            }
            return true;
        }
        return false;
    }

    public static boolean handleIntentUrl(WebView view, String url) {
        if (url.startsWith("intent://")) {
            Intent intent;
            Context context = view.getContext();
            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException ignored) {
                intent = null;
            }
            if (intent != null) {
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setComponent(null);
                intent.setSelector(null);
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Snackbar.make(view, context.getString(R.string.error_no_activity_found),
                            Snackbar.LENGTH_LONG).show();
                }
                return true;
            }
        }

        return false;
    }
}
