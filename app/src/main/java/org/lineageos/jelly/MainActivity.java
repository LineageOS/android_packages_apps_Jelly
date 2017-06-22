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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import org.lineageos.jelly.favorite.Favorite;
import org.lineageos.jelly.favorite.FavoriteActivity;
import org.lineageos.jelly.favorite.FavoriteDatabaseHandler;
import org.lineageos.jelly.history.HistoryActivity;
import org.lineageos.jelly.ui.EditTextExt;
import org.lineageos.jelly.utils.PrefsUtils;
import org.lineageos.jelly.utils.UiUtils;
import org.lineageos.jelly.webview.WebViewCompat;
import org.lineageos.jelly.webview.WebViewExt;
import org.lineageos.jelly.webview.WebViewExtActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends WebViewExtActivity implements View.OnTouchListener,
        View.OnScrollChangeListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PROVIDER = "org.lineageos.jelly.fileprovider";
    private static final String EXTRA_INCOGNITO = "extra_incognito";
    private static final String EXTRA_DESKTOP_MODE = "extra_desktop_mode";
    private static final String EXTRA_URL = "extra_url";
    private static final String STATE_KEY_THEME_COLOR = "theme_color";
    private static final int STORAGE_PERM_REQ = 423;
    private static final int LOCATION_PERM_REQ = 424;

    private CoordinatorLayout mCoordinator;
    private WebViewExt mWebView;
    private ProgressBar mLoadingProgress;
    private boolean mHasThemeColorSupport;
    private Drawable mLastActionBarDrawable;
    private int mThemeColor;

    private String mWaitingDownloadUrl;
    private String mWaitingDownloadName;

    private Bitmap mUrlIcon;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private GestureDetectorCompat mGestureDetector;
    private boolean mFingerReleased = false;
    private boolean mGestureOngoing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mCoordinator = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mWebView.reload();
            new Handler().postDelayed(() -> mSwipeRefreshLayout.setRefreshing(false), 1000);
        });
        mLoadingProgress = (ProgressBar) findViewById(R.id.load_progress);
        EditTextExt editText = (EditTextExt) findViewById(R.id.url_bar);
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager manager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(editText.getApplicationWindowToken(), 0);

                mWebView.loadUrl(editText.getText().toString());
                editText.clearFocus();
                return true;
            }
            return false;
        });

        Intent intent = getIntent();
        String url = intent.getDataString();
        boolean incognito = intent.getBooleanExtra(EXTRA_INCOGNITO, false);
        boolean desktopMode = false;

        // Restore from previous instance
        if (savedInstanceState != null) {
            incognito = savedInstanceState.getBoolean(EXTRA_INCOGNITO, incognito);
            if (url == null || url.isEmpty()) {
                url = savedInstanceState.getString(EXTRA_URL, null);
            }
            desktopMode = savedInstanceState.getBoolean(EXTRA_DESKTOP_MODE, false);
            mThemeColor = savedInstanceState.getInt(STATE_KEY_THEME_COLOR, 0);
        }

        // Make sure prefs are set before loading them
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        ImageView incognitoIcon = (ImageView) findViewById(R.id.incognito);
        incognitoIcon.setVisibility(incognito ? View.VISIBLE : View.GONE);

        setupMenu();
        mWebView = (WebViewExt) findViewById(R.id.web_view);
        mWebView.init(this, editText, mLoadingProgress, incognito);
        mWebView.setDesktopMode(desktopMode);
        mWebView.loadUrl(url == null ? PrefsUtils.getHomePage(this) : url);

        mHasThemeColorSupport = WebViewCompat.isThemeColorSupported(mWebView);

        mGestureDetector = new GestureDetectorCompat(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTapEvent(MotionEvent e) {
                        mGestureOngoing = true;
                        return false;
                    }
                });
        mWebView.setOnTouchListener(this);
        mWebView.setOnScrollChangeListener(this);

        applyThemeColor(mThemeColor);
    }

    @Override
    protected void onStop() {
        CookieManager.getInstance().flush();
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
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
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
                    downloadFileAsk(mWaitingDownloadUrl, mWaitingDownloadName);
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
        outState.putString(EXTRA_URL, mWebView.getUrl());
        outState.putBoolean(EXTRA_INCOGNITO, mWebView.isIncognito());
        outState.putBoolean(EXTRA_DESKTOP_MODE, mWebView.isDesktopMode());
        outState.putInt(STATE_KEY_THEME_COLOR, mThemeColor);
    }

    private void setupMenu() {
        ImageButton menu = (ImageButton) findViewById(R.id.search_menu);
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
                        openInNewTab(null);
                        break;
                    case R.id.menu_incognito:
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra(EXTRA_INCOGNITO, true);
                        startActivity(intent);
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

    private void openInNewTab(String url) {
        Intent intent = new Intent(this, MainActivity.class);
        if (url != null && !url.isEmpty()) {
            intent.setData(Uri.parse(url));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
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
        FavoriteDatabaseHandler handler = new FavoriteDatabaseHandler(this);
        boolean hasValidIcon = mUrlIcon != null && !mUrlIcon.isRecycled();
        int color = hasValidIcon ? UiUtils.getColor(mUrlIcon, false) : Color.TRANSPARENT;
        if (color == Color.TRANSPARENT) {
            color = ContextCompat.getColor(this, R.color.colorAccent);
        }
        handler.addItem(new Favorite(title, url, color));
        Snackbar.make(mCoordinator, getString(R.string.favorite_added),
                Snackbar.LENGTH_LONG).show();
    }

    public void downloadFileAsk(String url, String fileName) {
        if (!hasStoragePermission()) {
            mWaitingDownloadUrl = url;
            mWaitingDownloadName = fileName;
            requestStoragePermission();
            return;
        }
        mWaitingDownloadUrl = null;
        mWaitingDownloadName = null;

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
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }

    public void showSheetMenu(String url, boolean shouldAllowDownload) {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);

        View view = getLayoutInflater().inflate(R.layout.sheet_actions, new LinearLayout(this));
        View tabLayout = view.findViewById(R.id.sheet_new_tab);
        View shareLayout = view.findViewById(R.id.sheet_share);
        View favouriteLayout = view.findViewById(R.id.sheet_favourite);
        View downloadLayout = view.findViewById(R.id.sheet_download);

        tabLayout.setOnClickListener(v -> openInNewTab(url));
        shareLayout.setOnClickListener(v -> shareUrl(url));
        favouriteLayout.setOnClickListener(v -> setAsFavorite(url, url));
        if (shouldAllowDownload) {
            downloadLayout.setOnClickListener(v -> downloadFileAsk(url, ""));
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

        getWindow().setStatusBarColor(color);

        int flags = getWindow().getDecorView().getSystemUiVisibility();
        if (UiUtils.isColorLight(color)) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);

        setTaskDescription(new ActivityManager.TaskDescription(mWebView.getTitle(),
                mUrlIcon, color));
    }

    private int getThemeColorWithFallback() {
        if (mThemeColor != Color.TRANSPARENT) {
            return mThemeColor;
        }
        return ContextCompat.getColor(this,
                mWebView.isIncognito() ? R.color.colorIncognito : R.color.colorPrimary);
    }

    private void addShortcut() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setData(Uri.parse(mWebView.getUrl()));
        intent.setAction(Intent.ACTION_MAIN);

        Bitmap icon = mUrlIcon == null ?
                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher) : mUrlIcon;
        Bitmap launcherIcon = UiUtils.getShortcutIcon(this, icon, getThemeColorWithFallback());

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mWebView.getTitle());
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, launcherIcon);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        sendBroadcast(addIntent);
        launcherIcon.recycle();
        Snackbar.make(mCoordinator, getString(R.string.shortcut_added),
                Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        mFingerReleased = event.getAction() == MotionEvent.ACTION_UP;

        if (mGestureOngoing && mFingerReleased && mWebView.getScrollY() == 0) {
            // We are ending a gesture and we are at the top
            mSwipeRefreshLayout.setEnabled(true);
        } else if (mGestureOngoing || event.getPointerCount() > 1) {
            // A gesture is ongoing or starting
            mSwipeRefreshLayout.setEnabled(false);
        } else if (event.getAction() != MotionEvent.ACTION_MOVE) {
            // We are either initiating or ending a movement
            if (mWebView.getScrollY() == 0) {
                mSwipeRefreshLayout.setEnabled(true);
            } else {
                mSwipeRefreshLayout.setEnabled(false);
            }
        }
        // Reset the flag, the gesture detector will set it to true if the
        // gesture is still ongoing
        mGestureOngoing = false;

        return super.onTouchEvent(event);
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        // In case we reach the top without touching the screen (e.g. fling gesture)
        if (mFingerReleased && scrollY == 0) {
            mSwipeRefreshLayout.setEnabled(true);
        }
    }
}
