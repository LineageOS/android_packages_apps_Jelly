/*
 * Copyright (C) 2020 The LineageOS Project
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
import android.app.ActivityManager.TaskDescription
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.net.http.HttpResponseCache
import android.os.*
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebChromeClient.CustomViewCallback
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.jelly.favorite.FavoriteActivity
import org.lineageos.jelly.favorite.FavoriteProvider
import org.lineageos.jelly.history.HistoryActivity
import org.lineageos.jelly.suggestions.SuggestionsAdapter
import org.lineageos.jelly.ui.SearchBarController
import org.lineageos.jelly.ui.UrlBarController
import org.lineageos.jelly.utils.IntentUtils
import org.lineageos.jelly.utils.PrefsUtils
import org.lineageos.jelly.utils.TabUtils.openInNewTab
import org.lineageos.jelly.utils.UiUtils
import org.lineageos.jelly.webview.WebViewExt
import org.lineageos.jelly.webview.WebViewExtActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : WebViewExtActivity(), SearchBarController.OnCancelListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var mCoordinator: CoordinatorLayout
    private lateinit var mAppBar: AppBarLayout
    private lateinit var mWebViewContainer: FrameLayout
    private lateinit var mWebView: WebViewExt
    private val mUrlResolvedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!intent.hasExtra(Intent.EXTRA_INTENT) ||
                !intent.hasExtra(Intent.EXTRA_RESULT_RECEIVER)
            ) {
                return
            }
            val resolvedIntent: Intent =
                if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)!!
                }
            if (TextUtils.equals(packageName, resolvedIntent.getPackage())) {
                val url: String = intent.getStringExtra(IntentUtils.EXTRA_URL)!!
                mWebView.loadUrl(url)
            } else {
                startActivity(resolvedIntent)
            }
            val receiver: ResultReceiver = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(
                    Intent.EXTRA_RESULT_RECEIVER,
                    ResultReceiver::class.java
                )!!
            } else {
                intent.getParcelableExtra(Intent.EXTRA_RESULT_RECEIVER)!!
            }
            receiver.send(Activity.RESULT_CANCELED, Bundle())
        }
    }
    private lateinit var mLoadingProgress: ProgressBar
    private lateinit var mSearchController: SearchBarController
    private lateinit var mToolbarSearchBar: RelativeLayout
    private var mLastActionBarDrawable: Drawable? = null
    private var mThemeColor = 0
    private var mWaitingDownloadUrl: String? = null
    private var mUrlIcon: Bitmap? = null
    private var mIncognito = false
    private var mCustomView: View? = null
    private var mFullScreenCallback: CustomViewCallback? = null
    private var mSearchActive = false
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        mCoordinator = findViewById(R.id.coordinator_layout)
        mAppBar = findViewById(R.id.app_bar_layout)
        mWebViewContainer = findViewById(R.id.web_view_container)
        mLoadingProgress = findViewById(R.id.load_progress)
        mToolbarSearchBar = findViewById(R.id.toolbar_search_bar)
        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.url_bar)
        autoCompleteTextView.setAdapter(SuggestionsAdapter(this))
        autoCompleteTextView.setOnEditorActionListener { _, actionId: Int, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                UiUtils.hideKeyboard(autoCompleteTextView)
                mWebView.loadUrl(autoCompleteTextView.text.toString())
                autoCompleteTextView.clearFocus()
                return@setOnEditorActionListener true
            }
            false
        }
        autoCompleteTextView.setOnKeyListener { _, keyCode: Int, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                UiUtils.hideKeyboard(autoCompleteTextView)
                mWebView.loadUrl(autoCompleteTextView.text.toString())
                autoCompleteTextView.clearFocus()
                return@setOnKeyListener true
            }
            false
        }
        autoCompleteTextView.onItemClickListener = OnItemClickListener { _, view: View, _, _ ->
            val searchString = (view.findViewById<View>(R.id.title) as TextView).text
            val url = searchString.toString()
            UiUtils.hideKeyboard(autoCompleteTextView)
            autoCompleteTextView.clearFocus()
            mWebView.loadUrl(url)
        }
        val intent = intent
        var url = intent.dataString
        mIncognito = intent.getBooleanExtra(IntentUtils.EXTRA_INCOGNITO, false)
        var desktopMode = false

        // Restore from previous instance
        if (savedInstanceState != null) {
            mIncognito = savedInstanceState.getBoolean(IntentUtils.EXTRA_INCOGNITO, mIncognito)
            if (url == null || url.isEmpty()) {
                url = savedInstanceState.getString(IntentUtils.EXTRA_URL, null)
            }
            desktopMode = savedInstanceState.getBoolean(IntentUtils.EXTRA_DESKTOP_MODE, false)
            mThemeColor = savedInstanceState.getInt(STATE_KEY_THEME_COLOR, 0)
        }
        if (mIncognito) {
            autoCompleteTextView.imeOptions = autoCompleteTextView.imeOptions or
                    EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        }

        // Make sure prefs are set before loading them
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceManager.registerOnSharedPreferenceChangeListener(this)

        val incognitoIcon = findViewById<ImageView>(R.id.incognito)
        incognitoIcon.visibility = if (mIncognito) View.VISIBLE else View.GONE
        setupMenu()
        val urlBarController = UrlBarController(
            autoCompleteTextView,
            findViewById(R.id.secure)
        )
        mWebView = findViewById(R.id.web_view)
        mWebView.init(this, urlBarController, mLoadingProgress, mIncognito)
        mWebView.isDesktopMode = desktopMode
        mWebView.loadUrl(url ?: PrefsUtils.getHomePage(this))
        mSearchController = SearchBarController(
            mWebView,
            findViewById(R.id.search_menu_edit),
            findViewById(R.id.search_status),
            findViewById(R.id.search_menu_prev),
            findViewById(R.id.search_menu_next),
            findViewById(R.id.search_menu_cancel),
            this
        )
        setUiMode()
        try {
            val httpCacheDir = File(cacheDir, "suggestion_responses")
            val httpCacheSize = 1024 * 1024.toLong() // 1 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
        } catch (e: IOException) {
            Log.i(TAG, "HTTP response cache installation failed:$e")
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(mUrlResolvedReceiver, IntentFilter(IntentUtils.EVENT_URL_RESOLVED))
    }

    override fun onStop() {
        CookieManager.getInstance().flush()
        unregisterReceiver(mUrlResolvedReceiver)
        HttpResponseCache.getInstalled().flush()
        super.onStop()
    }

    public override fun onPause() {
        mWebView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mWebView.onResume()
        CookieManager.getInstance()
            .setAcceptCookie(!mWebView.isIncognito && PrefsUtils.getCookie(this))
        if (PrefsUtils.getLookLock(this)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onBackPressed() {
        when {
            mSearchActive -> {
                mSearchController.onCancel()
            }
            mCustomView != null -> {
                onHideCustomView()
            }
            mWebView.canGoBack() -> {
                mWebView.goBack()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        when (requestCode) {
            LOCATION_PERM_REQ -> if (hasLocationPermission()) {
                mWebView.reload()
            }
            STORAGE_PERM_REQ -> if (hasStoragePermission() && mWaitingDownloadUrl != null) {
                downloadFileAsk(mWaitingDownloadUrl, null, null)
            } else {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.permission_error_title)
                        .setMessage(R.string.permission_error_storage)
                        .setCancelable(false)
                        .setPositiveButton(
                            getString(R.string.permission_error_ask_again)
                        ) { _: DialogInterface?, _: Int -> requestStoragePermission() }
                        .setNegativeButton(
                            getString(R.string.dismiss)
                        ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                        .show()
                } else {
                    Snackbar.make(
                        mCoordinator, getString(R.string.permission_error_forever),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Preserve webView status
        outState.putString(IntentUtils.EXTRA_URL, mWebView.url)
        outState.putBoolean(IntentUtils.EXTRA_INCOGNITO, mWebView.isIncognito)
        outState.putBoolean(IntentUtils.EXTRA_DESKTOP_MODE, mWebView.isDesktopMode)
        outState.putInt(STATE_KEY_THEME_COLOR, mThemeColor)
    }

    private fun setupMenu() {
        val menu = findViewById<ImageButton>(R.id.search_menu)
        menu.setOnClickListener {
            val isDesktop = mWebView.isDesktopMode
            val wrapper = ContextThemeWrapper(
                this,
                R.style.AppTheme_PopupMenuOverlapAnchor
            )
            val popupMenu = PopupMenu(
                wrapper, menu, Gravity.NO_GRAVITY,
                R.attr.actionOverflowMenuStyle, 0
            )
            popupMenu.inflate(R.menu.menu_main)
            val desktopMode = popupMenu.menu.findItem(R.id.desktop_mode)
            desktopMode.title = getString(
                if (isDesktop) {
                    R.string.menu_mobile_mode
                } else {
                    R.string.menu_desktop_mode
                }
            )
            desktopMode.icon = ContextCompat.getDrawable(
                this, if (isDesktop) {
                    R.drawable.ic_mobile
                } else {
                    R.drawable.ic_desktop
                }
            )
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_new -> openInNewTab(this, null, false)
                    R.id.menu_incognito -> openInNewTab(this, null, true)
                    R.id.menu_reload -> mWebView.reload()
                    R.id.menu_add_favorite -> uiScope.launch {
                        setAsFavorite(mWebView.title, mWebView.url)
                    }
                    R.id.menu_share ->
                        // Delay a bit to allow popup menu hide animation to play
                        Handler(Looper.getMainLooper()).postDelayed({
                            shareUrl(mWebView.url)
                        }, 300)
                    R.id.menu_search ->
                        // Run the search setup
                        showSearch()
                    R.id.menu_favorite -> startActivity(Intent(this, FavoriteActivity::class.java))
                    R.id.menu_history -> startActivity(Intent(this, HistoryActivity::class.java))
                    R.id.menu_shortcut -> addShortcut()
                    R.id.menu_print -> {
                        val printManager = getSystemService(PrintManager::class.java)
                        val documentName = "Jelly document"
                        val printAdapter = mWebView.createPrintDocumentAdapter(documentName)
                        printManager.print(
                            documentName, printAdapter,
                            PrintAttributes.Builder().build()
                        )
                    }
                    R.id.desktop_mode -> {
                        mWebView.isDesktopMode = !isDesktop
                        desktopMode.title = getString(
                            if (isDesktop) {
                                R.string.menu_desktop_mode
                            } else {
                                R.string.menu_mobile_mode
                            }
                        )
                        desktopMode.icon = ContextCompat.getDrawable(
                            this, if (isDesktop) {
                                R.drawable.ic_desktop
                            } else {
                                R.drawable.ic_mobile
                            }
                        )
                    }
                    R.id.menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                }
                true
            }

            val helper = MenuPopupHelper(
                wrapper,
                (popupMenu.menu as MenuBuilder), menu
            )
            helper.setForceShowIcon(true)
            helper.show()
        }
    }

    private fun showSearch() {
        mToolbarSearchBar.visibility = View.GONE
        findViewById<View>(R.id.toolbar_search_page).visibility = View.VISIBLE
        mSearchController.onShow()
        mSearchActive = true
    }

    override fun onCancelSearch() {
        findViewById<View>(R.id.toolbar_search_page).visibility = View.GONE
        mToolbarSearchBar.visibility = View.VISIBLE
        mSearchActive = false
    }

    private fun shareUrl(url: String?) {
        if (url == null) {
            return
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, url)
        if (PrefsUtils.getAdvancedShare(this) && url == mWebView.url) {
            val file = File(cacheDir, System.currentTimeMillis().toString() + ".png")
            try {
                FileOutputStream(file).use { out ->
                    val bm = mWebView.snap
                    bm.compress(Bitmap.CompressFormat.PNG, 70, out)
                    out.flush()
                    out.close()
                    intent.putExtra(
                        Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(this, PROVIDER, file)
                    )
                    intent.type = "image/png"
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (e: IOException) {
                Log.e(TAG, "${e.message}", e)
            }
        } else {
            intent.type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_title)))
    }


    private suspend fun setAsFavorite(title: String?, url: String?) {
        if (title == null || url == null) {
            return
        }
        val hasValidIcon = mUrlIcon != null && !mUrlIcon!!.isRecycled
        var color = if (hasValidIcon) UiUtils.getColor(mUrlIcon, false) else Color.TRANSPARENT
        if (color == Color.TRANSPARENT) {
            color = ContextCompat.getColor(this, R.color.colorAccent)
        }
        withContext(Dispatchers.Default) {
            FavoriteProvider.addOrUpdateItem(contentResolver, title, url, color)
            withContext(Dispatchers.Main) {
                Snackbar.make(
                    mCoordinator, getString(R.string.favorite_added),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun downloadFileAsk(url: String?, contentDisposition: String?, mimeType: String?) {
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
            .setPositiveButton(
                getString(R.string.download_positive)
            ) { _: DialogInterface?, _: Int -> fetchFile(url, fileName) }
            .setNegativeButton(
                getString(R.string.dismiss)
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
    }

    private fun fetchFile(url: String?, fileName: String) {
        val request: DownloadManager.Request = try {
            DownloadManager.Request(Uri.parse(url))
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Cannot download non http or https scheme")
            return
        }

        // Let this downloaded file be scanned by MediaScanner - so that it can
        // show up in Gallery app, for example.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            request.allowScanningByMediaScanner()
        }
        request.setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setMimeType(
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(url)
            )
        )
        getSystemService(DownloadManager::class.java).enqueue(request)
    }

    override fun showSheetMenu(url: String, shouldAllowDownload: Boolean) {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_actions, LinearLayout(this))
        val tabLayout = view.findViewById<View>(R.id.sheet_new_tab)
        val shareLayout = view.findViewById<View>(R.id.sheet_share)
        val favouriteLayout = view.findViewById<View>(R.id.sheet_favourite)
        val downloadLayout = view.findViewById<View>(R.id.sheet_download)
        tabLayout.setOnClickListener {
            openInNewTab(this, url, mIncognito)
            sheet.dismiss()
        }
        shareLayout.setOnClickListener {
            shareUrl(url)
            sheet.dismiss()
        }
        favouriteLayout.setOnClickListener {
            uiScope.launch {
                setAsFavorite(url, url)
            }
            sheet.dismiss()
        }
        if (shouldAllowDownload) {
            downloadLayout.setOnClickListener {
                downloadFileAsk(url, null, null)
                sheet.dismiss()
            }
            downloadLayout.visibility = View.VISIBLE
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


    override fun onFaviconLoaded(favicon: Bitmap?) {
        if (favicon == null || favicon.isRecycled) {
            return
        }
        mUrlIcon = favicon.copy(favicon.config, true)
        applyThemeColor(UiUtils.getColor(favicon, mWebView.isIncognito))
        if (!favicon.isRecycled) {
            favicon.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun applyThemeColor(color: Int) {
        var localColor = color
        val hasValidColor = localColor != Color.TRANSPARENT
        mThemeColor = localColor
        localColor = themeColorWithFallback
        val actionBar = supportActionBar
        if (actionBar != null) {
            val newDrawable = ColorDrawable(localColor)
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
        val progressColor = if (hasValidColor) {
            if (UiUtils.isColorLight(localColor)) Color.BLACK else Color.WHITE
        } else {
            ContextCompat.getColor(this, R.color.colorAccent)
        }
        mLoadingProgress.progressTintList = ColorStateList.valueOf(progressColor)
        mLoadingProgress.postInvalidate()
        val isReachMode = UiUtils.isReachModeEnabled(this)
        if (isReachMode) {
            window.navigationBarColor = localColor
        } else {
            window.statusBarColor = localColor
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                if (UiUtils.isColorLight(localColor)) {
                    if (isReachMode) {
                        it.setSystemBarsAppearance(
                            APPEARANCE_LIGHT_NAVIGATION_BARS,
                            APPEARANCE_LIGHT_NAVIGATION_BARS
                        )
                    } else {
                        it.setSystemBarsAppearance(
                            APPEARANCE_LIGHT_STATUS_BARS,
                            APPEARANCE_LIGHT_STATUS_BARS
                        )
                    }
                } else {
                    if (isReachMode) {
                        it.setSystemBarsAppearance(0, APPEARANCE_LIGHT_NAVIGATION_BARS)
                    } else {
                        it.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS)
                    }
                }
            }
        } else {
            var flags = window.decorView.systemUiVisibility
            flags = if (UiUtils.isColorLight(localColor)) {
                flags or if (isReachMode) {
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            } else {
                flags and if (isReachMode) {
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                } else {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
            window.decorView.systemUiVisibility = flags
        }
        setTaskDescription(
            TaskDescription(
                mWebView.title,
                mUrlIcon, localColor
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun resetSystemUIColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.setSystemBarsAppearance(0, APPEARANCE_LIGHT_NAVIGATION_BARS)
                it.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS)
            }
        } else {
            var flags = window.decorView.systemUiVisibility
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            window.decorView.systemUiVisibility = flags
        }
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
    }

    private val themeColorWithFallback: Int
        get() = if (mThemeColor != Color.TRANSPARENT) {
            mThemeColor
        } else ContextCompat.getColor(
            this,
            if (mWebView.isIncognito) R.color.colorIncognito else R.color.colorPrimary
        )

    override fun onShowCustomView(view: View?, callback: CustomViewCallback) {
        if (mCustomView != null) {
            callback.onCustomViewHidden()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mCustomView = view
        mFullScreenCallback = callback
        setImmersiveMode(true)
        mCustomView!!.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))
        addContentView(
            mCustomView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        mAppBar.visibility = View.GONE
        mWebViewContainer.visibility = View.GONE
    }

    override fun onHideCustomView() {
        if (mCustomView == null) {
            return
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setImmersiveMode(false)
        mAppBar.visibility = View.VISIBLE
        mWebViewContainer.visibility = View.VISIBLE
        val viewGroup = mCustomView!!.parent as ViewGroup
        viewGroup.removeView(mCustomView)
        mFullScreenCallback!!.onCustomViewHidden()
        mFullScreenCallback = null
        mCustomView = null
    }

    private fun addShortcut() {
        val intent = Intent(this, MainActivity::class.java)
        intent.data = Uri.parse(mWebView.url)
        intent.action = Intent.ACTION_MAIN
        val launcherIcon: Icon = if (mUrlIcon != null) {
            Icon.createWithBitmap(
                UiUtils.getShortcutIcon(mUrlIcon!!, themeColorWithFallback)
            )
        } else {
            Icon.createWithResource(this, R.mipmap.ic_launcher)
        }
        val title = mWebView.title.toString()
        val shortcutInfo = ShortcutInfo.Builder(this, title)
            .setShortLabel(title)
            .setIcon(launcherIcon)
            .setIntent(intent)
            .build()
        getSystemService(ShortcutManager::class.java).requestPinShortcut(shortcutInfo, null)
    }

    @Suppress("DEPRECATION")
    private fun setImmersiveMode(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(!enable)
            window.insetsController?.let {
                val flags = WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                val behavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (enable) {
                    it.hide(flags)
                    it.systemBarsBehavior = behavior
                } else {
                    it.show(flags)
                    it.systemBarsBehavior = behavior.inv()
                }
            }
        } else {
            var flags = window.decorView.systemUiVisibility
            val immersiveModeFlags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            flags = if (enable) {
                flags or immersiveModeFlags
            } else {
                flags and immersiveModeFlags.inv()
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        setImmersiveMode(hasFocus && mCustomView != null)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "key_reach_mode" -> setUiMode()
        }
    }

    private fun setUiMode() {
        // Now you don't see it
        mCoordinator.alpha = 0f
        // Magic happens
        changeUiMode(UiUtils.isReachModeEnabled(this))
        // Now you see it
        mCoordinator.alpha = 1f
    }

    private fun changeUiMode(isReachMode: Boolean) {
        val appBarParams = mAppBar.layoutParams as CoordinatorLayout.LayoutParams
        val containerParams = mWebViewContainer.layoutParams as CoordinatorLayout.LayoutParams
        val progressParams = mLoadingProgress.layoutParams as RelativeLayout.LayoutParams
        val searchBarParams = mToolbarSearchBar.layoutParams as RelativeLayout.LayoutParams
        val margin = UiUtils.getDimenAttr(
            this, R.style.AppTheme,
            android.R.attr.actionBarSize
        ).toInt()
        if (isReachMode) {
            appBarParams.gravity = Gravity.BOTTOM
            containerParams.setMargins(0, 0, 0, margin)
            progressParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            progressParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
            searchBarParams.removeRule(RelativeLayout.ABOVE)
            searchBarParams.addRule(RelativeLayout.BELOW, R.id.load_progress)
        } else {
            appBarParams.gravity = Gravity.TOP
            containerParams.setMargins(0, margin, 0, 0)
            progressParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
            progressParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            searchBarParams.removeRule(RelativeLayout.BELOW)
            searchBarParams.addRule(RelativeLayout.ABOVE, R.id.load_progress)
        }
        mAppBar.layoutParams = appBarParams
        mAppBar.invalidate()
        mWebViewContainer.layoutParams = containerParams
        mWebViewContainer.invalidate()
        mLoadingProgress.layoutParams = progressParams
        mLoadingProgress.invalidate()
        mToolbarSearchBar.layoutParams = searchBarParams
        mToolbarSearchBar.invalidate()
        resetSystemUIColor()
        if (mThemeColor != 0) {
            applyThemeColor(mThemeColor)
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PROVIDER = "org.lineageos.jelly.fileprovider"
        private const val STATE_KEY_THEME_COLOR = "theme_color"
        private const val STORAGE_PERM_REQ = 423
        private const val LOCATION_PERM_REQ = 424
    }
}