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

import android.graphics.Bitmap;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebChromeClient;

public abstract class WebViewExtActivity extends AppCompatActivity {

    public abstract void downloadFileAsk(String url, String contentDisposition, String mimeType);

    public abstract boolean hasLocationPermission();

    public abstract void requestLocationPermission();

    public abstract void showSheetMenu(String url, boolean shouldAllowDownload);

    public abstract void onThemeColorSet(int color);

    public abstract void onFaviconLoaded(Bitmap favicon);

    public abstract void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback);

    public abstract void onHideCustomView();
}
