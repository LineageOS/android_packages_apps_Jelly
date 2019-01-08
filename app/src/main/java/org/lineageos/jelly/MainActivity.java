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
package org.lineageos.jelly;

import android.Manifest;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.lineageos.jelly.favorite.FavoriteActivity;
import org.lineageos.jelly.favorite.FavoriteProvider;
import org.lineageos.jelly.history.HistoryActivity;
import org.lineageos.jelly.suggestions.SuggestionsAdapter;
import org.lineageos.jelly.ui.SearchBarController;
import org.lineageos.jelly.ui.UrlBarController;
import org.lineageos.jelly.utils.IntentUtils;
import org.lineageos.jelly.utils.PrefsUtils;
import org.lineageos.jelly.utils.TabUtils;
import org.lineageos.jelly.utils.UiUtils;
import org.lineageos.jelly.webview.WebViewCompat;
import org.lineageos.jelly.webview.WebViewExt;
import org.lineageos.jelly.webview.WebViewExtActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class MainActivity extends WebViewExtActivity implements
         SearchBarController.OnCancelListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PROVIDER = "org.lineageos.jelly.fileprovider";
    private static final String STATE_KEY_THEME_COLOR = "theme_color";
    private static final int STORAGE_PERM_REQ = 423;
    private static final int LOCATION_PERM_REQ = 424;

    private final BroadcastReceiver mUrlResolvedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent resolvedIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (TextUtils.equals(getPackageName(), resolvedIntent.getPackage())) {
                String url = intent.getStringExtra(IntentUtils.EXTRA_URL);
                mWebView.loadUrl(url);
            } else {
                startActivity(resolvedIntent);
            }
            ResultReceiver receiver = intent.getParcelableExtra(Intent.EXTRA_RESULT_RECEIVER);
            receiver.send(RESULT_CANCELED, new Bundle());
        }
    };

    private final BroadcastReceiver mUiModeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setUiMode();
        }
    };

    private CoordinatorLayout mCoordinator;
    private AppBarLayout mAppBar;
    private FrameLayout mWebViewContainer;
    private WebViewExt mWebView;
    private ProgressBar mLoadingProgress;
    private SearchBarController mSearchController;
    private RelativeLayout mToolbarSearchBar;
    private boolean mHasThemeColorSupport;
    private Drawable mLastActionBarDrawable;
    private int mThemeColor;

    private String mWaitingDownloadUrl;

    private Bitmap mUrlIcon;

    private boolean mIncognito;

    private View mCustomView;
    private WebChromeClient.CustomViewCallback mFullScreenCallback;

    private boolean mSearchActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mCoordinator = findViewById(R.id.coordinator_layout);
        mAppBar = findViewById(R.id.app_bar_layout);
        mWebViewContainer = findViewById(R.id.web_view_container);
        mLoadingProgress = findViewById(R.id.load_progress);
        mToolbarSearchBar = findViewById(R.id.toolbar_search_bar);
        AutoCompleteTextView autoCompleteTextView = findViewById(R.id.url_bar);
        autoCompleteTextView.setAdapter(new SuggestionsAdapter(this));
        autoCompleteTextView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                UiUtils.hideKeyboard(autoCompleteTextView);

                mWebView.loadUrl(autoCompleteTextView.getText().toString());
                autoCompleteTextView.clearFocus();
                return true;
            }
            return false;
        });
        autoCompleteTextView.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                UiUtils.hideKeyboard(autoCompleteTextView);

                mWebView.loadUrl(autoCompleteTextView.getText().toString());
                autoCompleteTextView.clearFocus();
                return true;
            }
            return false;
        });
        autoCompleteTextView.setOnItemClickListener((adapterView, view, pos, l) -> {
            CharSequence searchString = ((TextView) view.findViewById(R.id.title)).getText();
            String url = searchString.toString();

            UiUtils.hideKeyboard(autoCompleteTextView);

            autoCompleteTextView.clearFocus();
            mWebView.loadUrl(url);
        });

        Intent intent = getIntent();
        String url = intent.getDataString();
        mIncognito = intent.getBooleanExtra(IntentUtils.EXTRA_INCOGNITO, false);
        boolean desktopMode = false;

        // Restore from previous instance
        if (savedInstanceState != null) {
            mIncognito = savedInstanceState.getBoolean(IntentUtils.EXTRA_INCOGNITO, mIncognito);
            if (url == null || url.isEmpty()) {
                url = savedInstanceState.getString(IntentUtils.EXTRA_URL, null);
            }
            desktopMode = savedInstanceState.getBoolean(IntentUtils.EXTRA_DESKTOP_MODE, false);
            mThemeColor = savedInstanceState.getInt(STATE_KEY_THEME_COLOR, 0);
        }

        if (mIncognito) {
            autoCompleteTextView.setImeOptions(autoCompleteTextView.getImeOptions() |
                    EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        }

        // Make sure prefs are set before loading them
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        // Listen for local broadcasts
        registerLocalBroadcastListeners();

        setUiMode();

        ImageView incognitoIcon = findViewById(R.id.incognito);
        incognitoIcon.setVisibility(mIncognito ? View.VISIBLE : View.GONE);

        setupMenu();

        UrlBarController urlBarController = new UrlBarController(autoCompleteTextView,
                findViewById(R.id.secure));

        mWebView = findViewById(R.id.web_view);
        mWebView.init(this, urlBarController, mLoadingProgress, mIncognito);
        mWebView.setDesktopMode(desktopMode);
        mWebView.loadUrl(url == null ? PrefsUtils.getHomePage(this) : url);

        mHasThemeColorSupport = WebViewCompat.isThemeColorSupported(mWebView);

        mSearchController = new SearchBarController(mWebView,
                findViewById(R.id.search_menu_edit),
                findViewById(R.id.search_status),
                findViewById(R.id.search_menu_prev),
                findViewById(R.id.search_menu_next),
                findViewById(R.id.search_menu_cancel),
                this);

        applyThemeColor(mThemeColor);

        try {
            File httpCacheDir = new File(getCacheDir(), "suggestion_responses");
            long httpCacheSize = 1024 * 1024; // 1 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.i(TAG, "HTTP response cache installation failed:" + e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mUrlResolvedReceiver, new IntentFilter(IntentUtils.EVENT_URL_RESOLVED));
    }

    @Override
    protected void onStop() {
        CookieManager.getInstance().flush();
        unregisterReceiver(mUrlResolvedReceiver);
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
        super.onStop();
    }

    @Override
    public void onPause() {
        mWebView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
        CookieManager.getInstance()
                .setAcceptCookie(!mWebView.isIncognito() && PrefsUtils.getCookie(this));
        if (PrefsUtils.getLookLock(this)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    protected void onDestroy() {
        // Unregister the local broadcast receiver because the activity is being trashed
        unregisterLocalBroadcastsListeners();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mSearchActive) {
            mSearchController.onCancel();
        } else if (mCustomView != null) {
            onHideCustomView();
        } else if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] results) {
        switch (requestCode) {
            case LOCATION_PERM_REQ:
                if (hasLocationPermission()) {
                    mWebView.reload();
                }
                break;
            case STORAGE_PERM_REQ:
                if (hasStoragePermission() && mWaitingDownloadUrl != null) {
                    downloadFileAsk(mWaitingDownloadUrl, null, null);
                } else {
                    if (shouldShowRequestPermissionRationale(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.permission_error_title)
                                .setMessage(R.string.permission_error_storage)
                                .setCancelable(false)
                                .setPositiveButton(getString(R.string.permission_error_ask_again),
                                        ((dialog, which) -> requestStoragePermission()))
                                .setNegativeButton(getString(R.string.dismiss),
                                        (((dialog, which) -> dialog.dismiss())))
                                .show();
                    } else {
                        Snackbar.make(mCoordinator, getString(R.string.permission_error_forever),
                                Snackbar.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Preserve webView status
        outState.putString(IntentUtils.EXTRA_URL, mWebView.getUrl());
        outState.putBoolean(IntentUtils.EXTRA_INCOGNITO, mWebView.isIncognito());
        outState.putBoolean(IntentUtils.EXTRA_DESKTOP_MODE, mWebView.isDesktopMode());
        outState.putInt(STATE_KEY_THEME_COLOR, mThemeColor);
    }

    private void setupMenu() {
        ImageButton menu = findViewById(R.id.search_menu);
        menu.setOnClickListener(v -> {
            boolean isDesktop = mWebView.isDesktopMode();
            ContextThemeWrapper wrapper = new ContextThemeWrapper(this,
                    R.style.AppTheme_PopupMenuOverlapAnchor);

            PopupMenu popupMenu = new PopupMenu(wrapper, menu, Gravity.NO_GRAVITY,
                    R.attr.actionOverflowMenuStyle, 0);
            popupMenu.inflate(R.menu.menu_main);

            MenuItem desktopMode = popupMenu.getMenu().findItem(R.id.desktop_mode);
            desktopMode.setTitle(getString(isDesktop ?
                    R.string.menu_mobile_mode : R.string.menu_desktop_mode));
            desktopMode.setIcon(ContextCompat.getDrawable(this, isDesktop ?
                    R.drawable.ic_mobile : R.drawable.ic_desktop));

            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.menu_new:
                        TabUtils.openInNewTab(this, null, false);
                        break;
                    case R.id.menu_incognito:
                        TabUtils.openInNewTab(this, null, true);
                        break;
                    case R.id.menu_reload:
                        mWebView.reload();
                        break;
                    case R.id.menu_add_favorite:
                        setAsFavorite(mWebView.getTitle(), mWebView.getUrl());
                        break;
                    case R.id.menu_share:
                        // Delay a bit to allow popup menu hide animation to play
                        new Handler().postDelayed(() -> shareUrl(mWebView.getUrl()), 300);
                        break;
                    case R.id.menu_search:
                        // Run the search setup
                        showSearch();
                        break;
                    case R.id.menu_favorite:
                        startActivity(new Intent(this, FavoriteActivity.class));
                        break;
                    case R.id.menu_history:
                        startActivity(new Intent(this, HistoryActivity.class));
                        break;
                    case R.id.menu_shortcut:
                        addShortcut();
                        break;
                    case R.id.menu_settings:
                        startActivity(new Intent(this, SettingsActivity.class));
                        break;
                    case R.id.desktop_mode:
                        mWebView.setDesktopMode(!isDesktop);
                        desktopMode.setTitle(getString(isDesktop ?
                                R.string.menu_desktop_mode : R.string.menu_mobile_mode));
                        desktopMode.setIcon(ContextCompat.getDrawable(this, isDesktop ?
                                R.drawable.ic_desktop : R.drawable.ic_mobile));
                        break;
                }
                return true;
            });

            // Fuck you, lint
            //noinspection RestrictedApi
            MenuPopupHelper helper = new MenuPopupHelper(wrapper,
                    (MenuBuilder) popupMenu.getMenu(), menu);
            //noinspection RestrictedApi
            helper.setForceShowIcon(true);
            //noinspection RestrictedApi
            helper.show();
        });
    }

    private void showSearch() {
        mToolbarSearchBar.setVisibility(View.GONE);
        findViewById(R.id.toolbar_search_page).setVisibility(View.VISIBLE);
        mSearchController.onShow();
        mSearchActive = true;
    }

    @Override
    public void onCancelSearch() {
        findViewById(R.id.toolbar_search_page).setVisibility(View.GONE);
        mToolbarSearchBar.setVisibility(View.VISIBLE);
        mSearchActive = false;
    }

    private void shareUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, url);

        if (PrefsUtils.getAdvancedShare(this) && url.equals(mWebView.getUrl())) {
            try {
                File file = new File(getCacheDir(),
                        String.valueOf(System.currentTimeMillis()) + ".png");
                FileOutputStream out = new FileOutputStream(file);
                Bitmap bm = mWebView.getSnap();
                if (bm == null) {
                    out.close();
                    return;
                }
                bm.compress(Bitmap.CompressFormat.PNG, 70, out);
                out.flush();
                out.close();
                intent.putExtra(Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(this, PROVIDER, file));
                intent.setType("image/png");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            intent.setType("text/plain");
        }

        startActivity(Intent.createChooser(intent, getString(R.string.share_title)));
    }

    private void setAsFavorite(String title, String url) {
        boolean hasValidIcon = mUrlIcon != null && !mUrlIcon.isRecycled();
        int color = hasValidIcon ? UiUtils.getColor(mUrlIcon, false) : Color.TRANSPARENT;
        if (color == Color.TRANSPARENT) {
            color = ContextCompat.getColor(this, R.color.colorAccent);
        }
        new SetAsFavoriteTask(getContentResolver(), title, url, color, mCoordinator).execute();
    }

    public void downloadFileAsk(String url, String contentDisposition, String mimeType) {
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);

        if (!hasStoragePermission()) {
            mWaitingDownloadUrl = url;
            requestStoragePermission();
            return;
        }
        mWaitingDownloadUrl = null;

        new AlertDialog.Builder(this)
                .setTitle(R.string.download_title)
                .setMessage(getString(R.string.download_message, fileName))
                .setPositiveButton(getString(R.string.download_positive),
                        (dialog, which) -> fetchFile(url, fileName))
                .setNegativeButton(getString(R.string.dismiss),
                        ((dialog, which) -> dialog.dismiss()))
                .show();
    }

    private void fetchFile(String url, String fileName) {
        DownloadManager.Request request;

        try {
            request = new DownloadManager.Request(Uri.parse(url));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Cannot download non http or https scheme");
            return;
        }

        // Let this downloaded file be scanned by MediaScanner - so that it can
        // show up in Gallery app, for example.
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setMimeType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(url)));
        getSystemService(DownloadManager.class).enqueue(request);
    }

    public void showSheetMenu(String url, boolean shouldAllowDownload) {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);

        View view = getLayoutInflater().inflate(R.layout.sheet_actions, new LinearLayout(this));
        View tabLayout = view.findViewById(R.id.sheet_new_tab);
        View shareLayout = view.findViewById(R.id.sheet_share);
        View favouriteLayout = view.findViewById(R.id.sheet_favourite);
        View downloadLayout = view.findViewById(R.id.sheet_download);

        tabLayout.setOnClickListener(v -> {
            TabUtils.openInNewTab(this, url, mIncognito);
            sheet.dismiss();
        });
        shareLayout.setOnClickListener(v -> {
            shareUrl(url);
            sheet.dismiss();
        });
        favouriteLayout.setOnClickListener(v -> {
            setAsFavorite(url, url);
            sheet.dismiss();
        });
        if (shouldAllowDownload) {
            downloadLayout.setOnClickListener(v -> {
                downloadFileAsk(url, null, null);
                sheet.dismiss();
            });
            downloadLayout.setVisibility(View.VISIBLE);
        }
        sheet.setContentView(view);
        sheet.show();
    }

    private void requestStoragePermission() {
        String[] permissionArray = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(permissionArray, STORAGE_PERM_REQ);
    }

    private boolean hasStoragePermission() {
        int result = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermission() {
        String[] permissionArray = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        requestPermissions(permissionArray, LOCATION_PERM_REQ);
    }

    public boolean hasLocationPermission() {
        int result = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public void onThemeColorSet(int color) {
        if (mHasThemeColorSupport) {
            applyThemeColor(color);
        }
    }

    public void onFaviconLoaded(Bitmap favicon) {
        if (favicon == null || favicon.isRecycled()) {
            return;
        }

        mUrlIcon = favicon.copy(favicon.getConfig(), true);
        if (!mHasThemeColorSupport) {
            applyThemeColor(UiUtils.getColor(favicon, mWebView.isIncognito()));
        }

        if (!favicon.isRecycled()) {
            favicon.recycle();
        }
    }

    private void applyThemeColor(int color) {
        boolean hasValidColor = color != Color.TRANSPARENT;
        mThemeColor = color;
        color = getThemeColorWithFallback();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            ColorDrawable newDrawable = new ColorDrawable(color);
            if (mLastActionBarDrawable != null) {
                final Drawable[] layers = new Drawable[] { mLastActionBarDrawable, newDrawable };
                final TransitionDrawable transition = new TransitionDrawable(layers);
                transition.setCrossFadeEnabled(true);
                transition.startTransition(200);
                actionBar.setBackgroundDrawable(transition);
            } else {
                actionBar.setBackgroundDrawable(newDrawable);
            }
            mLastActionBarDrawable = newDrawable;
        }

        int progressColor = hasValidColor
                ? UiUtils.isColorLight(color) ? Color.BLACK : Color.WHITE
                : ContextCompat.getColor(this, R.color.colorAccent);
        mLoadingProgress.setProgressTintList(ColorStateList.valueOf(progressColor));
        mLoadingProgress.postInvalidate();

        boolean isReachMode = UiUtils.isReachModeEnabled(this);
        if (isReachMode) {
            getWindow().setNavigationBarColor(color);
        } else {
            getWindow().setStatusBarColor(color);
        }

        int flags = getWindow().getDecorView().getSystemUiVisibility();
        if (UiUtils.isColorLight(color)) {
            flags |= isReachMode ?
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR :
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            flags &= isReachMode ?
                    ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR :
                    ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);

        setTaskDescription(new ActivityManager.TaskDescription(mWebView.getTitle(),
                mUrlIcon, color));
    }

    private void resetSystemUIColor() {
        int flags = getWindow().getDecorView().getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
    }

    private int getThemeColorWithFallback() {
        if (mThemeColor != Color.TRANSPARENT) {
            return mThemeColor;
        }
        return ContextCompat.getColor(this,
                mWebView.isIncognito() ? R.color.colorIncognito : R.color.colorPrimary);
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mCustomView = view;
        mFullScreenCallback = callback;
        setImmersiveMode(true);
        mCustomView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        addContentView(mCustomView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mAppBar.setVisibility(View.GONE);
        mWebViewContainer.setVisibility(View.GONE);
    }

    @Override
    public void onHideCustomView() {
        if (mCustomView == null) {
            return;
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setImmersiveMode(false);
        mAppBar.setVisibility(View.VISIBLE);
        mWebViewContainer.setVisibility(View.VISIBLE);
        ViewGroup viewGroup = (ViewGroup) mCustomView.getParent();
        viewGroup.removeView(mCustomView);
        mFullScreenCallback.onCustomViewHidden();
        mFullScreenCallback = null;
        mCustomView = null;
    }

    private void addShortcut() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setData(Uri.parse(mWebView.getUrl()));
        intent.setAction(Intent.ACTION_MAIN);

        Icon launcherIcon;

        if (mUrlIcon != null) {
            launcherIcon = Icon.createWithBitmap(
                    UiUtils.getShortcutIcon(mUrlIcon, getThemeColorWithFallback()));
        } else {
            launcherIcon = Icon.createWithResource(this, R.mipmap.ic_launcher);
        }

        String title = mWebView.getTitle();
        ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, title)
                .setShortLabel(title)
                .setIcon(launcherIcon)
                .setIntent(intent)
                .build();

        getSystemService(ShortcutManager.class).requestPinShortcut(shortcutInfo, null);
    }

    private void setImmersiveMode(boolean enable) {
        int flags = getWindow().getDecorView().getSystemUiVisibility();
        int immersiveModeFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if (enable) {
            flags |= immersiveModeFlags;
        } else {
            flags &= ~immersiveModeFlags;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setImmersiveMode(hasFocus && mCustomView != null);
    }

    private void registerLocalBroadcastListeners() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);

        if (!UiUtils.isTablet(this)) {
            manager.registerReceiver(mUiModeChangeReceiver, new IntentFilter(IntentUtils.EVENT_CHANGE_UI_MODE));
        }
    }

    private void unregisterLocalBroadcastsListeners() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);

        if (!UiUtils.isTablet(this)) {
            manager.unregisterReceiver(mUiModeChangeReceiver);
        }
    }

    private void setUiMode() {
        // Now you don't see it
        mCoordinator.setAlpha(0f);
        // Magic happens
        changeUiMode(UiUtils.isReachModeEnabled(this));
        // Now you see it
        mCoordinator.setAlpha(1f);
    }

    private void changeUiMode(boolean isReachMode) {
        CoordinatorLayout.LayoutParams appBarParams =
                (CoordinatorLayout.LayoutParams) mAppBar.getLayoutParams();
        CoordinatorLayout.LayoutParams containerParams =
                (CoordinatorLayout.LayoutParams) mWebViewContainer.getLayoutParams();
        RelativeLayout.LayoutParams progressParams =
                (RelativeLayout.LayoutParams) mLoadingProgress.getLayoutParams();
        RelativeLayout.LayoutParams searchBarParams =
                (RelativeLayout.LayoutParams) mToolbarSearchBar.getLayoutParams();

        int margin = (int) UiUtils.getDimenAttr(this, R.style.AppTheme,
                android.R.attr.actionBarSize);

        if (isReachMode) {
            appBarParams.gravity = Gravity.BOTTOM;
            containerParams.setMargins(0, 0, 0, margin);
            progressParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            progressParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            searchBarParams.removeRule(RelativeLayout.ABOVE);
            searchBarParams.addRule(RelativeLayout.BELOW, R.id.load_progress);
        } else {
            appBarParams.gravity = Gravity.TOP;
            containerParams.setMargins(0, margin, 0, 0);
            progressParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            progressParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            searchBarParams.removeRule(RelativeLayout.BELOW);
            searchBarParams.addRule(RelativeLayout.ABOVE, R.id.load_progress);
        }

        mAppBar.setLayoutParams(appBarParams);
        mAppBar.invalidate();
        mWebViewContainer.setLayoutParams(containerParams);
        mWebViewContainer.invalidate();
        mLoadingProgress.setLayoutParams(progressParams);
        mLoadingProgress.invalidate();
        mToolbarSearchBar.setLayoutParams(searchBarParams);
        mToolbarSearchBar.invalidate();

        resetSystemUIColor();

        if (mThemeColor != 0) {
            applyThemeColor(mThemeColor);
        }
    }

    private static class SetAsFavoriteTask extends AsyncTask<Void, Void, Boolean> {
        private ContentResolver contentResolver;
        private final String title;
        private final String url;
        private final int color;
        private final WeakReference<View> parentView;

        SetAsFavoriteTask(ContentResolver contentResolver, String title, String url,
                          int color, View parentView) {
            this.contentResolver = contentResolver;
            this.title = title;
            this.url = url;
            this.color = color;
            this.parentView = new WeakReference<>(parentView);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            FavoriteProvider.addOrUpdateItem(contentResolver, title, url, color);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            View view = parentView.get();
            if (view != null) {
                Snackbar.make(view, view.getContext().getString(R.string.favorite_added),
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
