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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.lineageos.jelly.MainActivity;
import org.lineageos.jelly.utils.PrefsUtils;
import org.lineageos.jelly.utils.UrlUtils;

public class WebViewExt extends WebView {

    private final Context mContext;

    public WebViewExt(Context context) {
        super(context);
        mContext = context;
        setup();
    }

    public WebViewExt(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setup();
    }

    public WebViewExt(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setup();
    }

    @Override
    public void loadUrl(String url) {
        // Try to add http prefix
        if (!url.startsWith("http")) {
            String withHttp = "http://" + url;
            if ((Patterns.WEB_URL.matcher(withHttp).matches() ||
                    UrlUtils.ACCEPTED_URI_SCHEMA.matcher(withHttp).matches()) &&
                    withHttp.contains(".")) {
                super.loadUrl(withHttp);
                return;
            }
        }

        // http url
        String query = UrlUtils.fixUrl(url).trim();
        if (Patterns.WEB_URL.matcher(query).matches() ||
                UrlUtils.ACCEPTED_URI_SCHEMA.matcher(query).matches() ||
                query.isEmpty()) {
            super.loadUrl(url);
            return;
        }

        // Query
        String baseUrl = PrefsUtils.getSearchEngine(mContext);
        super.loadUrl(UrlUtils.getFormattedUri(baseUrl, query));
    }

    private void setup() {
        getSettings().setJavaScriptEnabled(PrefsUtils.getJavascript(mContext));
        getSettings().setGeolocationEnabled(PrefsUtils.getLocation(mContext));
        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(false);

        setWebViewClient(new WebClient());

        setOnLongClickListener(new OnLongClickListener() {
            boolean shouldAllowDownload;

            @Override
            public boolean onLongClick(View v) {
                HitTestResult result = getHitTestResult();
                switch (result.getType()) {
                    case HitTestResult.IMAGE_TYPE:
                    case HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                        shouldAllowDownload = true;
                    case HitTestResult.SRC_ANCHOR_TYPE:
                        ((MainActivity) mContext).showSheetMenu(result.getExtra(),
                                shouldAllowDownload);
                        shouldAllowDownload = false;
                        return true;
                }
                return false;
            }
        });

        setDownloadListener((url, userAgent, contentDescription, mimeType, contentLength) ->
                ((MainActivity) mContext).downloadFileAsk(url,
                        URLUtil.guessFileName(url, contentDescription, mimeType)));
    }

    public void init(Context context, EditText editText,
                     ProgressBar progressBar, boolean incognito) {
        ChromeClient chromeClient = new ChromeClient(context, incognito);
        chromeClient.bindEditText(editText);
        chromeClient.bindProgressBar(progressBar);
        setWebChromeClient(chromeClient);
    }

    public Bitmap getSnap() {
        measure(MeasureSpec.makeMeasureSpec(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
        setDrawingCacheEnabled(true);
        buildDrawingCache();
        int size = getMeasuredWidth() > getMeasuredHeight() ?
                getMeasuredHeight() : getMeasuredWidth();
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        int height = bitmap.getHeight();
        canvas.drawBitmap(bitmap, 0, height, paint);
        draw(canvas);
        return bitmap;
    }

}
