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
package org.lineageos.jelly

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.net.http.HttpResponseCache
import android.os.*
import android.preference.PreferenceManager
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AlertDialog
import android.support.v7.view.menu.MenuBuilder
import android.support.v7.view.menu.MenuPopupHelper
import android.support.v7.widget.PopupMenu
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_suggestion.view.*
import kotlinx.android.synthetic.main.search_bar.*
import kotlinx.android.synthetic.main.sheet_actions.*
import org.lineageos.jelly.favorite.Favorite
import org.lineageos.jelly.favorite.FavoriteActivity
import org.lineageos.jelly.favorite.FavoriteDatabaseHandler
import org.lineageos.jelly.history.HistoryActivity
import org.lineageos.jelly.suggestions.SuggestionsAdapter
import org.lineageos.jelly.utils.PrefsUtils
import org.lineageos.jelly.utils.UiUtils
import org.lineageos.jelly.webview.WebViewCompat
import org.lineageos.jelly.webview.WebViewExtActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : WebViewExtActivity(), View.OnTouchListener, View.OnScrollChangeListener {
    private val mUrlResolvedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val resolvedIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            if (TextUtils.equals(packageName, resolvedIntent.`package`)) {
                val url = intent.getStringExtra(EXTRA_URL)
                web_view.loadUrl(url)
            } else {
                startActivity(resolvedIntent)
            }
            val receiver = intent.getParcelableExtra<ResultReceiver>(Intent.EXTRA_RESULT_RECEIVER)
            receiver.send(Activity.RESULT_CANCELED, Bundle())
        }
    }
    private var mHasThemeColorSupport: Boolean = false
    private var mLastActionBarDrawable: Drawable? = null
    private var mThemeColor: Int = 0

    private var mWaitingDownloadUrl: String? = null

    private var mUrlIcon: Bitmap? = null

    private var mGestureDetector: GestureDetectorCompat? = null
    private var mFingerReleased = false
    private var mGestureOngoing = false
    private var mIncognito: Boolean = false

    private val shortcutManager by lazy {
        getSystemService(ShortcutManager::class.java) as ShortcutManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        swipe_refresh.setOnRefreshListener {
            web_view.reload()
            Handler().postDelayed({ swipe_refresh.isRefreshing = false }, 1000)
        }
        url_bar.setAdapter(SuggestionsAdapter(this))
        url_bar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                UiUtils.hideKeyboard(url_bar)

                web_view.loadUrl(url_bar.text.toString())
                url_bar.clearFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        url_bar.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                UiUtils.hideKeyboard(url_bar)

                web_view.loadUrl(url_bar.text.toString())
                url_bar.clearFocus()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        url_bar.setOnItemClickListener { _, view, _, _ ->
            val searchString = view.title.text
            val url = searchString.toString()

            UiUtils.hideKeyboard(url_bar)

            url_bar.clearFocus()
            web_view.loadUrl(url)
        }

        var url: String? = null
        mIncognito = intent.getBooleanExtra(EXTRA_INCOGNITO, false)
        var desktopMode = false

        // Restore from previous instance
        savedInstanceState?.let {
            mIncognito = it.getBoolean(EXTRA_INCOGNITO, mIncognito)
            if (intent.dataString.isNullOrEmpty()) {
                url = it.getString(EXTRA_URL, null)
            }
            desktopMode = it.getBoolean(EXTRA_DESKTOP_MODE, false)
            mThemeColor = it.getInt(STATE_KEY_THEME_COLOR, 0)
        }

        // Make sure prefs are set before loading them
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)

        incognito.visibility = if (mIncognito) View.VISIBLE else View.GONE

        setupMenu()

        web_view.init(this, mIncognito, url_bar, load_progress)
        web_view.isDesktopMode = desktopMode
        web_view.loadUrl(if (!url.isNullOrBlank()) url!! else PrefsUtils.getHomePage(this))

        mHasThemeColorSupport = WebViewCompat.isThemeColorSupported(web_view)

        mGestureDetector = GestureDetectorCompat(this,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                        mGestureOngoing = true
                        return false
                    }
                })
        web_view.setOnTouchListener(this)
        web_view.setOnScrollChangeListener(this)

        applyThemeColor(mThemeColor)

        try {
            val httpCacheDir = File(cacheDir, "suggestion_responses")
            val httpCacheSize = (1024 * 1024).toLong() // 1 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
        } catch (e: IOException) {
            Log.i(TAG, "HTTP response cache installation failed:" + e)
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(mUrlResolvedReceiver, IntentFilter(ACTION_URL_RESOLVED))
    }

    override fun onStop() {
        CookieManager.getInstance().flush()
        unregisterReceiver(mUrlResolvedReceiver)
        HttpResponseCache.getInstalled()?.flush()
        super.onStop()
    }

    public override fun onPause() {
        web_view.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        web_view.onResume()
        CookieManager.getInstance()
                .setAcceptCookie(!web_view.isIncognito && PrefsUtils.getCookie(this))
        if (PrefsUtils.getLookLock(this)) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onBackPressed() {
        if (web_view.canGoBack()) {
            web_view.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
            results: IntArray) {
        when (requestCode) {
            LOCATION_PERM_REQ -> if (hasLocationPermission()) {
                web_view.reload()
            }
            STORAGE_PERM_REQ -> if (hasStoragePermission() && mWaitingDownloadUrl != null) {
                downloadFileAsk(mWaitingDownloadUrl!!, null, null)
            } else {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder(this)
                            .setTitle(R.string.permission_error_title)
                            .setMessage(R.string.permission_error_storage)
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.permission_error_ask_again)
                            ) { _, _ -> requestStoragePermission() }
                            .setNegativeButton(getString(R.string.dismiss)
                            ) { dialog, _ -> dialog.dismiss() }
                            .show()
                } else {
                    Snackbar.make(coordinator_layout, getString(R.string.permission_error_forever),
                            Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Preserve webView status
        outState.putString(EXTRA_URL, web_view.url)
        outState.putBoolean(EXTRA_INCOGNITO, web_view.isIncognito)
        outState.putBoolean(EXTRA_DESKTOP_MODE, web_view.isDesktopMode)
        outState.putInt(STATE_KEY_THEME_COLOR, mThemeColor)
    }

    private fun setupMenu() {
        search_menu.setOnClickListener {
            val isDesktop = web_view.isDesktopMode
            val wrapper = ContextThemeWrapper(this,
                    R.style.AppTheme_PopupMenuOverlapAnchor)

            val popupMenu = PopupMenu(wrapper, search_menu, Gravity.NO_GRAVITY,
                    R.attr.actionOverflowMenuStyle, 0)
            popupMenu.inflate(R.menu.menu_main)

            val desktopMode = popupMenu.menu.findItem(R.id.desktop_mode)
            desktopMode.title = getString(if (isDesktop)
                R.string.menu_mobile_mode
            else
                R.string.menu_desktop_mode)
            desktopMode.icon = ContextCompat.getDrawable(this, if (isDesktop)
                R.drawable.ic_mobile
            else
                R.drawable.ic_desktop)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_new -> openInNewTab(null, false)
                    R.id.menu_incognito -> openInNewTab(null, true)
                    R.id.menu_reload -> web_view.reload()
                    R.id.menu_add_favorite -> setAsFavorite(web_view.title, web_view.url)
                    R.id.menu_share ->
                        // Delay a bit to allow popup menu hide animation to play
                        Handler().postDelayed({ shareUrl(web_view.url) }, 300)
                    R.id.menu_favorite -> startActivity(Intent(this, FavoriteActivity::class.java))
                    R.id.menu_history -> startActivity(Intent(this, HistoryActivity::class.java))
                    R.id.menu_shortcut -> addShortcut()
                    R.id.menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                    R.id.desktop_mode -> {
                        web_view.isDesktopMode = !isDesktop
                        desktopMode.title = getString(if (isDesktop)
                            R.string.menu_desktop_mode
                        else
                            R.string.menu_mobile_mode)
                        desktopMode.icon = ContextCompat.getDrawable(this, if (isDesktop)
                            R.drawable.ic_desktop
                        else
                            R.drawable.ic_mobile)
                    }
                }
                true
            }

            // Fuck you, lint
            val helper = MenuPopupHelper(wrapper,
                    popupMenu.menu as MenuBuilder, search_menu)
            helper.setForceShowIcon(true)
            helper.show()
        }
    }

    private fun openInNewTab(url: String?, incognito: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        if (url != null && !url.isEmpty()) {
            intent.data = Uri.parse(url)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        intent.putExtra(EXTRA_INCOGNITO, incognito)
        startActivity(intent)
    }

    private fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, url)

        if (PrefsUtils.getAdvancedShare(this) && url == web_view.url) {
            try {
                val file = File(cacheDir,
                        System.currentTimeMillis().toString() + ".png")
                val out = FileOutputStream(file)
                val bm = web_view.snap
                bm.compress(Bitmap.CompressFormat.PNG, 70, out)
                out.flush()
                out.close()
                intent.putExtra(Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(this, PROVIDER, file))
                intent.type = "image/png"
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: IOException) {
                Log.e(TAG, e.message)
            }

        } else {
            intent.type = "text/plain"
        }

        startActivity(Intent.createChooser(intent, getString(R.string.share_title)))
    }

    private fun setAsFavorite(title: String, url: String) {
        val handler = FavoriteDatabaseHandler(this)
        val hasValidIcon = mUrlIcon != null && !mUrlIcon!!.isRecycled
        var color = if (hasValidIcon) UiUtils.getColor(mUrlIcon!!, false) else Color.TRANSPARENT
        if (color == Color.TRANSPARENT) {
            color = ContextCompat.getColor(this, R.color.colorAccent)
        }
        handler.addItem(Favorite(title, url, color))
        Snackbar.make(coordinator_layout, getString(R.string.favorite_added),
                Snackbar.LENGTH_LONG).show()
    }

    override fun downloadFileAsk(url: String, contentDisposition: String?, mimeType: String?) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)

        if (!hasStoragePermission()) {
            mWaitingDownloadUrl = url
            requestStoragePermission()
            return
        }
        mWaitingDownloadUrl = null

        AlertDialog.Builder(this)
                .setTitle(R.string.download_title)
                .setMessage(getString(R.string.download_message, fileName))
                .setPositiveButton(getString(R.string.download_positive)
                ) { _, _ -> fetchFile(url, fileName) }
                .setNegativeButton(getString(R.string.dismiss)
                ) { dialog, _ -> dialog.dismiss() }
                .show()
    }

    private fun fetchFile(url: String, fileName: String) {
        val request = DownloadManager.Request(Uri.parse(url))

        // Let this downloaded file be scanned by MediaScanner - so that it can
        // show up in Gallery app, for example.
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }

    override fun showSheetMenu(url: String, shouldAllowDownload: Boolean) {
        val sheet = BottomSheetDialog(this)

        val view = layoutInflater.inflate(R.layout.sheet_actions, LinearLayout(this))

        sheet_new_tab.setOnClickListener { openInNewTab(url, mIncognito) }
        sheet_share.setOnClickListener { shareUrl(url) }
        sheet_favourite.setOnClickListener { setAsFavorite(url, url) }
        if (shouldAllowDownload) {
            sheet_download.setOnClickListener { downloadFileAsk(url, null, null) }
            sheet_download.visibility = View.VISIBLE
        }
        sheet.setContentView(view)
        sheet.show()
    }

    private fun requestStoragePermission() {
        val permissionArray = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        requestPermissions(permissionArray, STORAGE_PERM_REQ)
    }

    private fun hasStoragePermission(): Boolean {
        val result = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    override fun requestLocationPermission() {
        val permissionArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        requestPermissions(permissionArray, LOCATION_PERM_REQ)
    }

    override fun hasLocationPermission(): Boolean {
        val result = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED
    }

    override fun onThemeColorSet(color: Int) {
        if (mHasThemeColorSupport) {
            applyThemeColor(color)
        }
    }

    override fun onFaviconLoaded(favicon: Bitmap?) {
        if (favicon == null || favicon.isRecycled) {
            return
        }

        mUrlIcon = favicon.copy(favicon.config, true)
        if (!mHasThemeColorSupport) {
            applyThemeColor(UiUtils.getColor(favicon, web_view.isIncognito))
        }

        if (!favicon.isRecycled) {
            favicon.recycle()
        }
    }

    private fun applyThemeColor(color: Int) {
        var new_color = color
        val hasValidColor = new_color != Color.TRANSPARENT
        mThemeColor = new_color
        new_color = themeColorWithFallback

        val actionBar = supportActionBar
        if (actionBar != null) {
            val newDrawable = ColorDrawable(new_color)
            if (mLastActionBarDrawable != null) {
                val layers = arrayOf(mLastActionBarDrawable!!, newDrawable)
                val transition = TransitionDrawable(layers)
                transition.isCrossFadeEnabled = true
                transition.startTransition(200)
                actionBar.setBackgroundDrawable(transition)
            } else {
                actionBar.setBackgroundDrawable(newDrawable)
            }
            mLastActionBarDrawable = newDrawable
        }

        val progressColor = if (hasValidColor)
            if (UiUtils.isColorLight(new_color)) Color.BLACK else Color.WHITE
        else
            ContextCompat.getColor(this, R.color.colorAccent)
        load_progress.progressTintList = ColorStateList.valueOf(progressColor)
        load_progress.postInvalidate()

        window.statusBarColor = new_color

        var flags = window.decorView.systemUiVisibility
        if (UiUtils.isColorLight(new_color)) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        window.decorView.systemUiVisibility = flags

        setTaskDescription(ActivityManager.TaskDescription(web_view.title,
                mUrlIcon, new_color))
    }

    private val themeColorWithFallback: Int
        get() {
            if (mThemeColor != Color.TRANSPARENT) {
                return mThemeColor
            }
            return ContextCompat.getColor(this,
                    if (web_view.isIncognito) R.color.colorIncognito else R.color.colorPrimary)
        }

    private fun addShortcut() {
        val intent = Intent(this, MainActivity::class.java)
        intent.data = Uri.parse(web_view.url)
        intent.action = Intent.ACTION_MAIN

        val icon = if (mUrlIcon == null)
            BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        else
            mUrlIcon
        val launcherIcon = UiUtils.getShortcutIcon(icon!!, themeColorWithFallback)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (shortcutManager.isRequestPinShortcutSupported) {
                val pinShortcutInfo = ShortcutInfo.Builder(this, "")
                        .setIcon(Icon.createWithBitmap(launcherIcon))
                        .setShortLabel(web_view.title)
                        .setIntent(intent)
                        .build()

                shortcutManager.requestPinShortcut(pinShortcutInfo, null)
            }
        } else {
            val addIntent = Intent()
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, web_view.title)
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, launcherIcon)
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
            addIntent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
            sendBroadcast(addIntent)
        }
        launcherIcon.recycle()
        Snackbar.make(coordinator_layout, getString(R.string.shortcut_added),
                Snackbar.LENGTH_LONG).show()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        mGestureDetector!!.onTouchEvent(event)
        mFingerReleased = event.action == MotionEvent.ACTION_UP

        if (mGestureOngoing && mFingerReleased && web_view.scrollY == 0) {
            // We are ending a gesture and we are at the top
            swipe_refresh.isEnabled = true
        } else if (mGestureOngoing || event.pointerCount > 1) {
            // A gesture is ongoing or starting
            swipe_refresh.isEnabled = false
        } else if (event.action != MotionEvent.ACTION_MOVE) {
            // We are either initiating or ending a movement
            swipe_refresh.isEnabled = web_view.scrollY == 0
        }
        // Reset the flag, the gesture detector will set it to true if the
        // gesture is still ongoing
        mGestureOngoing = false

        v.performClick()
        return super.onTouchEvent(event)
    }

    override fun onScrollChange(v: View, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        // In case we reach the top without touching the screen (e.g. fling gesture)
        if (mFingerReleased && scrollY == 0) {
            swipe_refresh.isEnabled = true
        }
    }

    companion object {
        val EXTRA_URL = "extra_url"
        val ACTION_URL_RESOLVED = "org.lineageos.jelly.action.URL_RESOLVED"
        private val TAG = MainActivity::class.java.simpleName
        private val PROVIDER = "org.lineageos.jelly.studio.fileprovider"
        private val EXTRA_INCOGNITO = "extra_incognito"
        private val EXTRA_DESKTOP_MODE = "extra_desktop_mode"
        private val STATE_KEY_THEME_COLOR = "theme_color"
        private val STORAGE_PERM_REQ = 423
        private val LOCATION_PERM_REQ = 424
    }
}
