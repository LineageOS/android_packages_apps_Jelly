/*
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly

import android.Manifest
import android.app.Activity
import android.app.ActivityManager.TaskDescription
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
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
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebChromeClient.CustomViewCallback
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.AppBarLayout
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
import org.lineageos.jelly.ui.UrlBarLayout
import org.lineageos.jelly.utils.IntentUtils
import org.lineageos.jelly.utils.PrefsUtils
import org.lineageos.jelly.utils.TabUtils.openInNewTab
import org.lineageos.jelly.utils.UiUtils
import org.lineageos.jelly.webview.WebViewExt
import org.lineageos.jelly.webview.WebViewExtActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : WebViewExtActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    // Views
    private val appBarLayout by lazy { findViewById<AppBarLayout>(R.id.appBarLayout) }
    private val coordinatorLayout by lazy { findViewById<CoordinatorLayout>(R.id.coordinatorLayout) }
    private val toolbar by lazy { findViewById<MaterialToolbar>(R.id.toolbar) }
    private val urlBarLayout by lazy { findViewById<UrlBarLayout>(R.id.urlBarLayout) }
    private val webView by lazy { findViewById<WebViewExt>(R.id.webView) }
    private val webViewContainerLayout by lazy { findViewById<FrameLayout>(R.id.webViewContainerLayout) }

    private val urlResolvedReceiver = object : BroadcastReceiver() {
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
    private var urlIcon: Bitmap? = null
    private var incognito = false
    private var customView: View? = null
    private var fullScreenCallback: CustomViewCallback? = null
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val intent = intent
        var url = intent.dataString
        incognito = intent.getBooleanExtra(IntentUtils.EXTRA_INCOGNITO, false)
        var desktopMode = false

        // Restore from previous instance
        savedInstanceState?.let {
            incognito = it.getBoolean(IntentUtils.EXTRA_INCOGNITO, incognito)
            url = url?.takeIf {
                    url -> url.isNotEmpty()
            } ?: it.getString(IntentUtils.EXTRA_URL, null)
            desktopMode = it.getBoolean(IntentUtils.EXTRA_DESKTOP_MODE, false)
        }

        // Make sure prefs are set before loading them
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceManager.registerOnSharedPreferenceChangeListener(this)

        urlBarLayout.isIncognito = incognito
        setupMenu()
        webView.init(this, urlBarLayout, incognito)
        webView.isDesktopMode = desktopMode
        webView.loadUrl(url ?: PrefsUtils.getHomePage(this))
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
            urlBarLayout.currentMode == UrlBarLayout.UrlBarMode.SEARCH -> {
                urlBarLayout.currentMode = UrlBarLayout.UrlBarMode.URL
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
        urlBarLayout.onMoreButtonClickCallback = {
            val isDesktop = webView.isDesktopMode
            val wrapper = ContextThemeWrapper(
                this,
                R.style.Theme_Jelly_PopupMenu
            )
            val popupMenu = PopupMenu(
                wrapper, urlBarLayout, Gravity.END,
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
                        webView.title?.let { title ->
                            webView.url?.let { url ->
                                setAsFavorite(title, url)
                            }
                        }
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
                    R.id.menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                }
                true
            }

            popupMenu.setForceShowIcon(true)
            popupMenu.show()
        }
    }

    private fun showSearch() {
        urlBarLayout.currentMode = UrlBarLayout.UrlBarMode.SEARCH
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


    private suspend fun setAsFavorite(title: String, url: String) {
        val hasValidIcon = urlIcon?.isRecycled == false
        var color = if (hasValidIcon) UiUtils.getColor(urlIcon, false) else Color.TRANSPARENT
        if (color == Color.TRANSPARENT) {
            color = ContextCompat.getColor(this, R.color.material_dynamic_primary50)
        }
        withContext(Dispatchers.Default) {
            FavoriteProvider.addOrUpdateItem(contentResolver, title, url, color)
            withContext(Dispatchers.Main) {
                Snackbar.make(
                    coordinatorLayout, getString(R.string.favorite_added),
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
        val request = try {
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
        val tabLayout = view.findViewById<View>(R.id.sheetNewTabLayout)
        val shareLayout = view.findViewById<View>(R.id.sheetShareLayout)
        val favouriteLayout = view.findViewById<View>(R.id.sheetFavouriteLayout)
        val downloadLayout = view.findViewById<View>(R.id.sheetDownloadLayout)
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
        favicon?.let {
            if (it.isRecycled) {
                return
            }
            urlIcon = it.copy(it.config, true)
            updateTaskDescription()
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }

    private fun updateTaskDescription() {
        setTaskDescription(
            TaskDescription(
                webView.title,
                urlIcon, Color.WHITE
            )
        )
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback) {
        customView?.let {
            callback.onCustomViewHidden()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        customView = view
        fullScreenCallback = callback
        setImmersiveMode(true)
        customView?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))
        addContentView(
            customView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        appBarLayout.visibility = View.GONE
        webViewContainerLayout.visibility = View.GONE
    }

    override fun onHideCustomView() {
        val customView = customView ?: return
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setImmersiveMode(false)
        appBarLayout.visibility = View.VISIBLE
        webViewContainerLayout.visibility = View.VISIBLE
        val viewGroup = customView.parent as ViewGroup
        viewGroup.removeView(customView)
        fullScreenCallback?.onCustomViewHidden()
        fullScreenCallback = null
        this.customView = null
    }

    private fun addShortcut() {
        val intent = Intent(this, MainActivity::class.java).apply {
            data = Uri.parse(webView.url)
            action = Intent.ACTION_MAIN
        }
        val launcherIcon = urlIcon?.let {
            Icon.createWithBitmap(UiUtils.getShortcutIcon(it, Color.WHITE))
        } ?: Icon.createWithResource(this, R.mipmap.ic_launcher)
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
        coordinatorLayout.alpha = 0f
        // Magic happens
        changeUiMode(UiUtils.isReachModeEnabled(this))
        // Now you see it
        coordinatorLayout.alpha = 1f
    }

    private fun changeUiMode(isReachMode: Boolean) {
        val appBarParams = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        val containerParams = webViewContainerLayout.layoutParams as CoordinatorLayout.LayoutParams
        val margin = resources.getDimension(R.dimen.m3_appbar_size_compact).toInt()
        if (isReachMode) {
            appBarParams.gravity = Gravity.BOTTOM
            containerParams.setMargins(0, 0, 0, margin)
        } else {
            appBarParams.gravity = Gravity.TOP
            containerParams.setMargins(0, margin, 0, 0)
        }
        appBarLayout.layoutParams = appBarParams
        appBarLayout.invalidate()
        webViewContainerLayout.layoutParams = containerParams
        webViewContainerLayout.invalidate()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PROVIDER = "org.lineageos.jelly.fileprovider"
        private const val LOCATION_PERM_REQ = 424
    }
}
