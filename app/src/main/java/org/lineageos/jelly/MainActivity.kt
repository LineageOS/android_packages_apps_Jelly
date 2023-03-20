/*
 * Copyright (C) 2020-2023 The LineageOS Project
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
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.net.http.HttpResponseCache
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.TextUtils
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebChromeClient.CustomViewCallback
import android.widget.AdapterView.OnItemClickListener
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
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
    // Views
    private val autoCompleteTextView by lazy { findViewById<AutoCompleteTextView>(R.id.url_bar) }
    private val coordinator by lazy { findViewById<CoordinatorLayout>(R.id.coordinatorLayout) }
    private val homeImageButton by lazy { findViewById<ImageButton>(R.id.homeImageButton) }
    private val incognitoIcon by lazy { findViewById<ImageView>(R.id.incognitoImageView) }
    private val loadingProgress by lazy { findViewById<ProgressBar>(R.id.loadingProgress) }
    private val searchMenuImageButton by lazy { findViewById<ImageButton>(R.id.searchMenuImageButton) }
    private val secureImageButton by lazy { findViewById<ImageButton>(R.id.secureImageButton) }
    private val titleTextView by lazy { findViewById<TextView>(R.id.titleTextView) }
    private val toolbar by lazy { findViewById<MaterialToolbar>(R.id.toolbar) }
    private val toolbarSearchBar by lazy { findViewById<ConstraintLayout>(R.id.toolbarSearchBar) }
    private val toolbarSearchPage by lazy { findViewById<ConstraintLayout>(R.id.toolbarSearchPage) }
    private val webViewContainer by lazy { findViewById<FrameLayout>(R.id.webViewContainer) }
    private val webView by lazy { findViewById<WebViewExt>(R.id.webView) }

    private val urlResolvedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!intent.hasExtra(Intent.EXTRA_INTENT) ||
                !intent.hasExtra(Intent.EXTRA_RESULT_RECEIVER)
            ) {
                return
            }
            val resolvedIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
            } else {
                @Suppress("Deprecation")
                intent.getParcelableExtra(Intent.EXTRA_INTENT)!!
            }
            if (TextUtils.equals(packageName, resolvedIntent.getPackage())) {
                val url: String = intent.getStringExtra(IntentUtils.EXTRA_URL)!!
                webView.loadUrl(url)
            } else {
                startActivity(resolvedIntent)
            }
            val receiver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    Intent.EXTRA_RESULT_RECEIVER,
                    ResultReceiver::class.java
                )!!
            } else {
                @Suppress("Deprecation")
                intent.getParcelableExtra(Intent.EXTRA_RESULT_RECEIVER)!!
            }
            receiver.send(Activity.RESULT_CANCELED, Bundle())
        }
    }

    private lateinit var searchController: SearchBarController
    private var urlIcon: Bitmap? = null
    private var incognito = false
    private var customView: View? = null
    private var fullScreenCallback: CustomViewCallback? = null
    private var searchActive = false
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        autoCompleteTextView.setAdapter(SuggestionsAdapter(this))
        autoCompleteTextView.setOnEditorActionListener { _, actionId: Int, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                UiUtils.hideKeyboard(autoCompleteTextView)
                webView.loadUrl(autoCompleteTextView.text.toString())
                autoCompleteTextView.clearFocus()
                return@setOnEditorActionListener true
            }
            false
        }
        autoCompleteTextView.setOnKeyListener { _, keyCode: Int, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                UiUtils.hideKeyboard(autoCompleteTextView)
                webView.loadUrl(autoCompleteTextView.text.toString())
                autoCompleteTextView.clearFocus()
                return@setOnKeyListener true
            }
            false
        }
        autoCompleteTextView.onItemClickListener = OnItemClickListener { _, _, _, _ ->
            val searchString = titleTextView.text
            val url = searchString.toString()
            UiUtils.hideKeyboard(autoCompleteTextView)
            autoCompleteTextView.clearFocus()
            webView.loadUrl(url)
        }
        val intent = intent
        var url = intent.dataString
        incognito = intent.getBooleanExtra(IntentUtils.EXTRA_INCOGNITO, false)
        var desktopMode = false

        // Restore from previous instance
        savedInstanceState?.let {
            incognito = it.getBoolean(IntentUtils.EXTRA_INCOGNITO, incognito)
            url = url?.takeIf { url ->
                url.isNotEmpty()
            } ?: it.getString(IntentUtils.EXTRA_URL, null)
            desktopMode = it.getBoolean(IntentUtils.EXTRA_DESKTOP_MODE, false)
        }
        if (incognito) {
            autoCompleteTextView.imeOptions = autoCompleteTextView.imeOptions or
                    EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        }

        // Make sure prefs are set before loading them
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceManager.registerOnSharedPreferenceChangeListener(this)

        incognitoIcon.visibility = if (incognito) View.VISIBLE else View.GONE
        setupMenu()
        val urlBarController = UrlBarController(autoCompleteTextView, secureImageButton)

        webView.init(this, urlBarController, loadingProgress, incognito)
        webView.isDesktopMode = desktopMode
        webView.loadUrl(url ?: PrefsUtils.getHomePage(this))
        searchController = SearchBarController(
            webView,
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

        // Set home button callback
        homeImageButton.setOnClickListener {
            webView.loadUrl(PrefsUtils.getHomePage(this))
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(urlResolvedReceiver, IntentFilter(IntentUtils.EVENT_URL_RESOLVED))
    }

    override fun onStop() {
        CookieManager.getInstance().flush()
        unregisterReceiver(urlResolvedReceiver)
        HttpResponseCache.getInstalled().flush()
        super.onStop()
    }

    public override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        CookieManager.getInstance()
            .setAcceptCookie(!webView.isIncognito && PrefsUtils.getCookie(this))
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
            searchActive -> {
                searchController.onCancel()
            }
            customView != null -> {
                onHideCustomView()
            }
            webView.canGoBack() -> {
                webView.goBack()
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
                webView.reload()
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Preserve webView status
        outState.putString(IntentUtils.EXTRA_URL, webView.url)
        outState.putBoolean(IntentUtils.EXTRA_INCOGNITO, webView.isIncognito)
        outState.putBoolean(IntentUtils.EXTRA_DESKTOP_MODE, webView.isDesktopMode)
    }

    private fun setupMenu() {
        searchMenuImageButton.setOnClickListener {
            val isDesktop = webView.isDesktopMode
            val wrapper = ContextThemeWrapper(
                this,
                R.style.Theme_Jelly_PopupMenuOverlapAnchor
            )
            val popupMenu = PopupMenu(
                wrapper, searchMenuImageButton, Gravity.NO_GRAVITY,
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
                    R.id.menu_reload -> webView.reload()
                    R.id.menu_add_favorite -> uiScope.launch {
                        setAsFavorite(webView.title, webView.url)
                    }
                    R.id.menu_share ->
                        // Delay a bit to allow popup menu hide animation to play
                        Handler(Looper.getMainLooper()).postDelayed({
                            webView.url?.let { url -> shareUrl(url) }
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
                        val printAdapter = webView.createPrintDocumentAdapter(documentName)
                        printManager.print(
                            documentName, printAdapter,
                            PrintAttributes.Builder().build()
                        )
                    }
                    R.id.desktop_mode -> {
                        webView.isDesktopMode = !isDesktop
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
                    R.id.menu_settings -> {
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                }
                true
            }

            popupMenu.setForceShowIcon(true)
            popupMenu.show()
        }
    }

    private fun showSearch() {
        toolbarSearchBar.visibility = View.GONE
        toolbarSearchPage.visibility = View.VISIBLE
        searchController.onShow()
        searchActive = true
    }

    override fun onCancelSearch() {
        toolbarSearchPage.visibility = View.GONE
        toolbarSearchBar.visibility = View.VISIBLE
        searchActive = false
    }

    private fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, url)
        if (PrefsUtils.getAdvancedShare(this) && url == webView.url) {
            val file = File(cacheDir, System.currentTimeMillis().toString() + ".png")
            try {
                FileOutputStream(file).use { out ->
                    val bm = webView.snap
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
        val color = urlIcon?.takeUnless { it.isRecycled }?.let {
            UiUtils.getColor(it, false)
        } ?: Color.TRANSPARENT
        withContext(Dispatchers.Default) {
            FavoriteProvider.addOrUpdateItem(contentResolver, title, url, color)
            withContext(Dispatchers.Main) {
                Snackbar.make(
                    coordinator, getString(R.string.favorite_added),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun downloadFileAsk(url: String?, contentDisposition: String?, mimeType: String?) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
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
            openInNewTab(this, url, incognito)
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

    override fun requestLocationPermission() {
        val permissionArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        requestPermissions(permissionArray, LOCATION_PERM_REQ)
    }

    override fun hasLocationPermission(): Boolean {
        val result = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED
    }


    override fun onFaviconLoaded(favicon: Bitmap?) {
        favicon?.takeUnless { it.isRecycled }?.let {
            urlIcon = it.copy(it.config, true)
            applyThemeColor()
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyThemeColor() {
        setTaskDescription(
            TaskDescription(
                webView.title,
                urlIcon, Color.BLACK
            )
        )
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback) {
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        customView = view
        fullScreenCallback = callback
        setImmersiveMode(true)
        customView!!.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))
        addContentView(
            customView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        toolbar.visibility = View.GONE
        webViewContainer.visibility = View.GONE
    }

    override fun onHideCustomView() {
        if (customView == null) {
            return
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setImmersiveMode(false)
        toolbar.visibility = View.VISIBLE
        webViewContainer.visibility = View.VISIBLE
        val viewGroup = customView!!.parent as ViewGroup
        viewGroup.removeView(customView)
        fullScreenCallback!!.onCustomViewHidden()
        fullScreenCallback = null
        customView = null
    }

    private fun addShortcut() {
        val intent = Intent(this, MainActivity::class.java)
        intent.data = Uri.parse(webView.url)
        intent.action = Intent.ACTION_MAIN
        val launcherIcon: Icon = if (urlIcon != null) {
            Icon.createWithBitmap(
                UiUtils.getShortcutIcon(urlIcon!!, Color.WHITE)
            )
        } else {
            Icon.createWithResource(this, R.mipmap.ic_launcher)
        }
        val title = webView.title.toString()
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
        setImmersiveMode(hasFocus && customView != null)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "key_reach_mode" -> setUiMode()
        }
    }

    private fun setUiMode() {
        // Now you don't see it
        coordinator.alpha = 0f
        // Magic happens
        changeUiMode(UiUtils.isReachModeEnabled(this))
        // Now you see it
        coordinator.alpha = 1f
    }

    private fun changeUiMode(isReachMode: Boolean) {
        val appBarParams = toolbar.layoutParams as CoordinatorLayout.LayoutParams
        val containerParams = webViewContainer.layoutParams as CoordinatorLayout.LayoutParams
        val progressParams = loadingProgress.layoutParams as RelativeLayout.LayoutParams
        val toolbarParams = toolbarSearchBar.layoutParams as RelativeLayout.LayoutParams
        val margin = UiUtils.getDimenAttr(
            this, R.style.Theme_Jelly,
            android.R.attr.actionBarSize
        ).toInt()
        if (isReachMode) {
            appBarParams.gravity = Gravity.BOTTOM
            containerParams.setMargins(0, 0, 0, margin)
            progressParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            progressParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
            toolbarParams.removeRule(RelativeLayout.ABOVE)
            toolbarParams.addRule(RelativeLayout.BELOW, R.id.loadingProgress)
        } else {
            appBarParams.gravity = Gravity.TOP
            containerParams.setMargins(0, margin, 0, 0)
            progressParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
            progressParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            toolbarParams.removeRule(RelativeLayout.BELOW)
            toolbarParams.addRule(RelativeLayout.ABOVE, R.id.loadingProgress)
        }
        toolbar.layoutParams = appBarParams
        toolbar.invalidate()
        webViewContainer.layoutParams = containerParams
        webViewContainer.invalidate()
        loadingProgress.layoutParams = progressParams
        loadingProgress.invalidate()
        toolbarSearchBar.layoutParams = toolbarParams
        toolbarSearchBar.invalidate()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PROVIDER = "org.lineageos.jelly.fileprovider"
        private const val LOCATION_PERM_REQ = 424
    }
}
