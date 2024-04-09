/*
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly

import android.app.Activity
import android.app.ActivityManager.TaskDescription
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
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
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.MimeTypeMap
import android.webkit.WebChromeClient.CustomViewCallback
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.updateLayoutParams
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
import org.lineageos.jelly.history.HistoryProvider
import org.lineageos.jelly.ui.MenuDialog
import org.lineageos.jelly.ui.UrlBarLayout
import org.lineageos.jelly.utils.IntentUtils
import org.lineageos.jelly.utils.PermissionsUtils
import org.lineageos.jelly.utils.SharedPreferencesExt
import org.lineageos.jelly.utils.TabUtils.openInNewTab
import org.lineageos.jelly.utils.UiUtils
import org.lineageos.jelly.utils.UrlUtils
import org.lineageos.jelly.viewmodels.WebViewModel
import org.lineageos.jelly.webview.WebViewExt
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    // View models
    private val model: WebViewModel by viewModels()

    // Views
    private val appBarLayout by lazy { findViewById<AppBarLayout>(R.id.appBarLayout) }
    private val coordinatorLayout by lazy { findViewById<CoordinatorLayout>(R.id.coordinatorLayout) }
    private val toolbar by lazy { findViewById<MaterialToolbar>(R.id.toolbar) }
    private val urlBarLayout by lazy { findViewById<UrlBarLayout>(R.id.urlBarLayout) }
    private val webView by lazy { findViewById<WebViewExt>(R.id.webView) }

    // System services
    private val downloadManager by lazy { getSystemService(DownloadManager::class.java) }
    private val printManager by lazy { getSystemService(PrintManager::class.java) }
    private val shortcutManager by lazy { getSystemService(ShortcutManager::class.java) }

    // File chooser
    private val fileRequest =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            fileRequestCallback.invoke(it)
        }
    private lateinit var fileRequestCallback: ((data: List<Uri>?) -> Unit)

    private val urlResolvedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!intent.hasExtra(Intent.EXTRA_INTENT) ||
                !intent.hasExtra(Intent.EXTRA_RESULT_RECEIVER)
            ) {
                return
            }
            @Suppress("UnsafeIntentLaunch")
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
    private var customView: View? = null
    private var fullScreenCallback: CustomViewCallback? = null
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private lateinit var menuDialog: MenuDialog

    private val sharedPreferencesExt by lazy { SharedPreferencesExt(this) }

    private val permissionsUtils by lazy { PermissionsUtils(this) }

    private lateinit var locationDialogCallback: (() -> Unit)
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.isNotEmpty()) {
            if (permissionsUtils.locationPermissionsGranted()) {
                // Precise or approximate location access granted.
                locationDialogCallback()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val intent = intent

        model.setIncognitoMode(
            intent.getBooleanExtra(IntentUtils.EXTRA_INCOGNITO, false)
        )

        // Set the initial URL
        intent.dataString?.takeIf {
            it.isNotEmpty()
        }?.let {
            model.setInitialUrl(it)
        }

        // Make sure prefs are set before loading them
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceManager.registerOnSharedPreferenceChangeListener(this)

        // Register app shortcuts
        registerShortcuts()

        menuDialog = MenuDialog(this) { option: MenuDialog.Option ->
            when (option) {
                MenuDialog.Option.BACK -> webView.goBack()
                MenuDialog.Option.FORWARD -> webView.goForward()
                MenuDialog.Option.NEW_TAB -> openInNewTab(this, null, false)
                MenuDialog.Option.NEW_PRIVATE_TAB -> openInNewTab(this, null, true)
                MenuDialog.Option.REFRESH -> webView.reload()
                MenuDialog.Option.ADD_TO_FAVORITE -> uiScope.launch {
                    webView.title?.let { title ->
                        webView.url?.let { url ->
                            setAsFavorite(title, url)
                        }
                    }
                }

                MenuDialog.Option.SHARE -> {
                    // Delay a bit to allow popup menu hide animation to play
                    Handler(Looper.getMainLooper()).postDelayed({
                        webView.url?.let { url -> shareUrl(url) }
                    }, 300)
                }

                MenuDialog.Option.FIND_IN_PAGE -> {
                    // Run the search setup
                    showSearch()
                }

                MenuDialog.Option.FAVORITES -> startActivity(
                    Intent(
                        this,
                        FavoriteActivity::class.java
                    )
                )

                MenuDialog.Option.HISTORY -> startActivity(
                    Intent(
                        this,
                        HistoryActivity::class.java
                    )
                )

                MenuDialog.Option.DOWNLOADS -> startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                MenuDialog.Option.ADD_TO_HOME_SCREEN -> addShortcut()
                MenuDialog.Option.PRINT -> {
                    val documentName = "Jelly document"
                    val printAdapter = webView.createPrintDocumentAdapter(documentName)
                    printManager.print(
                        documentName, printAdapter,
                        PrintAttributes.Builder().build()
                    )
                }

                MenuDialog.Option.DESKTOP_VIEW -> {
                    model.setDesktopMode(model.desktopMode.value != true)
                }

                MenuDialog.Option.SETTINGS -> startActivity(
                    Intent(
                        this,
                        SettingsActivity::class.java
                    )
                )
            }
            menuDialog.dismiss()
        }
        urlBarLayout.onMoreButtonClickCallback = {
            UiUtils.hideKeyboard(window, urlBarLayout)
            menuDialog.showAsDropdownMenu(urlBarLayout, sharedPreferencesExt.reachModeEnabled)
        }
        urlBarLayout.onLoadUrlCallback = { webView.loadUrl(it) }
        urlBarLayout.onStartSearchCallback = { webView.findAllAsync(it) }
        urlBarLayout.onClearSearchCallback = { webView.clearMatches() }
        urlBarLayout.onSearchPositionChangeCallback = { webView.findNext(it) }

        webView.init()

        // Load the initial URL
        webView.loadUrl(model.url.value ?: sharedPreferencesExt.homePage)

        setUiMode()

        try {
            val httpCacheDir = File(cacheDir, "suggestion_responses")
            val httpCacheSize = 1024 * 1024.toLong() // 1 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
        } catch (e: IOException) {
            Log.i(TAG, "HTTP response cache installation failed:$e")
        }

        // Observe favicon
        model.favicon.observe(this) { favicon ->
            updateTaskDescription(favicon)
        }

        // Observe title
        model.title.observe(this) { title ->
            webView.url?.let { url ->
                if (model.isIncognito.value != true) {
                    HistoryProvider.addOrUpdateItem(contentResolver, title, url)
                }
            }
        }

        // Observe show sheet menu requests
        model.onShowSheetMenu.observe(this) { onShowSheetMenu ->
            onShowSheetMenu.handle {
                showSheetMenu(it.url, it.shouldAllowDownload)
            }
        }

        // Observe download requests
        model.onDownloadStart.observe(this) { onDownloadStart ->
            onDownloadStart.handle {
                downloadFileAsk(
                    it.url, it.userAgent, it.contentDisposition, it.mimeType
                )
            }
        }

        // Observe file chooser requests
        model.onShowFileChooser.observe(this) { onShowFileChooser ->
            onShowFileChooser.handle {
                fileRequestCallback = { uris: List<Uri>? ->
                    it.path.onReceiveValue(uris?.toTypedArray())
                }

                try {
                    fileRequest.launch(
                        it.params.acceptTypes.mapNotNull { type ->
                            MimeTypeMap.getSingleton().getMimeTypeFromExtension(type)
                        }.toTypedArray().takeIf { mimeType ->
                            mimeType.isNotEmpty()
                        } ?: arrayOf("*/*")
                    )
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(
                        this, R.string.error_no_activity_found, Toast.LENGTH_LONG
                    ).show()

                    it.path.onReceiveValue(null)
                }
            }
        }

        // Observe geolocation permissions requests
        model.onGeolocationPermissionsShowPrompt.observe(
            this
        ) { onGeolocationPermissionsShowPrompt ->
            onGeolocationPermissionsShowPrompt.handle {
                showLocationDialog(it.origin, it.callback)
            }
        }

        // Observe show custom view requests
        model.onShowCustomView.observe(this) { onShowCustomView ->
            onShowCustomView.handle {
                onShowCustomView(it.view, it.callback)
            }
        }

        // Observe hide custom view requests
        model.onHideCustomView.observe(this) { onHideCustomView ->
            onHideCustomView.handle {
                onHideCustomView()
            }
        }

        // Observe open in new window requests
        model.onCreateWindow.observe(this) { onCreateWindow ->
            onCreateWindow.handle {
                val result = it.view.hitTestResult

                openInNewTab(this, result.extra, model.isIncognito.value == true)
            }
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
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
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                urlResolvedReceiver, IntentFilter(IntentUtils.EVENT_URL_RESOLVED),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(urlResolvedReceiver, IntentFilter(IntentUtils.EVENT_URL_RESOLVED))
        }
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
            .setAcceptCookie(model.isIncognito.value != true && sharedPreferencesExt.cookiesEnabled)
        if (sharedPreferencesExt.lookLockEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun registerShortcuts() {
        shortcutManager.dynamicShortcuts = listOf(
            ShortcutInfo.Builder(this, "new_incognito_tab_shortcut")
                .setShortLabel(getString(R.string.shortcut_new_incognito_tab))
                .setLongLabel(getString(R.string.shortcut_new_incognito_tab))
                .setIcon(Icon.createWithResource(this, R.drawable.shortcut_incognito))
                .setIntent(
                    Intent(this, MainActivity::class.java)
                        .putExtra("extra_incognito", true)
                        .setAction(Intent.ACTION_VIEW)
                )
                .build()
        )
    }

    private fun showSearch() {
        urlBarLayout.currentMode = UrlBarLayout.UrlBarMode.SEARCH
    }

    private fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, url)
        if (sharedPreferencesExt.advancedShareEnabled && url == webView.url) {
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
        val color = model.favicon.value?.takeUnless { it.isRecycled }?.let { bitmap ->
            UiUtils.getColor(bitmap, false).takeUnless { it == Color.TRANSPARENT }
        } ?: ContextCompat.getColor(
            this, com.google.android.material.R.color.material_dynamic_primary50
        )
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

    private fun downloadFileAsk(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        val fileName = UrlUtils.guessFileName(url, contentDisposition, mimeType)
        AlertDialog.Builder(this)
            .setTitle(R.string.download_title)
            .setMessage(getString(R.string.download_message, fileName))
            .setPositiveButton(
                getString(R.string.download_positive)
            ) { _: DialogInterface?, _: Int -> fetchFile(url, userAgent, fileName) }
            .setNegativeButton(
                getString(R.string.dismiss)
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
    }

    private fun fetchFile(url: String?, userAgent: String?, fileName: String) {
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
        userAgent?.let {
            request.addRequestHeader("User-Agent", it)
        }
        CookieManager.getInstance().getCookie(url)?.takeUnless { it.isEmpty() }?.let {
            request.addRequestHeader("Cookie", it)
        }
        downloadManager.enqueue(request)
    }

    private fun showSheetMenu(url: String, shouldAllowDownload: Boolean) {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_actions, LinearLayout(this))
        val tabLayout = view.findViewById<View>(R.id.sheetNewTabLayout)
        val shareLayout = view.findViewById<View>(R.id.sheetShareLayout)
        val favouriteLayout = view.findViewById<View>(R.id.sheetFavouriteLayout)
        val downloadLayout = view.findViewById<View>(R.id.sheetDownloadLayout)
        tabLayout.setOnClickListener {
            openInNewTab(this, url, model.isIncognito.value == true)
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
                downloadFileAsk(url, webView.settings.userAgentString, null, null)
                sheet.dismiss()
            }
            downloadLayout.visibility = View.VISIBLE
        }
        sheet.setContentView(view)
        sheet.show()
    }

    /*
     * This is called only if GeolocationPermissions doesn't have an explicit entry for @origin
     */
    private fun showLocationDialog(origin: String, callback: GeolocationPermissions.Callback) {
        locationDialogCallback = {
            AlertDialog.Builder(this)
                .setTitle(R.string.location_dialog_title)
                .setMessage(getString(R.string.location_dialog_message, origin))
                .setPositiveButton(R.string.location_dialog_allow) { _, _ ->
                    callback(origin, true, true)
                }
                .setNegativeButton(R.string.location_dialog_block) { _, _ ->
                    callback(origin, false, true)
                }
                .setNeutralButton(R.string.location_dialog_cancel) { _, _ ->
                    callback(origin, false, false)
                }
                .setOnCancelListener {
                    callback(origin, false, false)
                }
                .create()
                .show()
        }

        if (!permissionsUtils.locationPermissionsGranted()) {
            locationPermissionRequest.launch(PermissionsUtils.locationPermissions)
        } else {
            locationDialogCallback()
        }
    }

    private fun updateTaskDescription(favicon: Bitmap?) {
        setTaskDescription(
            TaskDescription(
                webView.title,
                favicon, Color.WHITE
            )
        )
    }

    private fun onShowCustomView(view: View?, callback: CustomViewCallback) {
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
        webView.visibility = View.GONE
    }

    private fun onHideCustomView() {
        val customView = customView ?: return
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setImmersiveMode(false)
        appBarLayout.visibility = View.VISIBLE
        webView.visibility = View.VISIBLE
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
        val launcherIcon = model.favicon.value?.let {
            Icon.createWithBitmap(UiUtils.getShortcutIcon(it, Color.WHITE))
        } ?: Icon.createWithResource(this, R.mipmap.ic_launcher)
        val title = webView.title.toString()
        val shortcutInfo = ShortcutInfo.Builder(this, title)
            .setShortLabel(title)
            .setIcon(launcherIcon)
            .setIntent(intent)
            .build()
        shortcutManager.requestPinShortcut(shortcutInfo, null)
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
        changeUiMode(sharedPreferencesExt.reachModeEnabled)
        // Now you see it
        coordinatorLayout.alpha = 1f
    }

    private fun changeUiMode(isReachMode: Boolean) {
        appBarLayout.updateLayoutParams<RelativeLayout.LayoutParams> {
            removeRule(
                when (isReachMode) {
                    true -> RelativeLayout.ALIGN_PARENT_TOP
                    false -> RelativeLayout.ALIGN_PARENT_BOTTOM
                }
            )
            addRule(
                when (isReachMode) {
                    true -> RelativeLayout.ALIGN_PARENT_BOTTOM
                    false -> RelativeLayout.ALIGN_PARENT_TOP
                }
            )
        }

        webView.updateLayoutParams<RelativeLayout.LayoutParams> {
            removeRule(
                when (isReachMode) {
                    true -> RelativeLayout.BELOW
                    false -> RelativeLayout.ABOVE
                }
            )
            addRule(
                when (isReachMode) {
                    true -> RelativeLayout.ABOVE
                    false -> RelativeLayout.BELOW
                },
                R.id.appBarLayout
            )
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PROVIDER = "${BuildConfig.APPLICATION_ID}.fileprovider"
    }
}
