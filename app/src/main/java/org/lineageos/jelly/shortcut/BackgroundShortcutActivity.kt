/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.shortcut

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.lineageos.jelly.R
import org.lineageos.jelly.utils.IntentUtils
import org.lineageos.jelly.utils.SharedPreferencesExt
import org.lineageos.jelly.utils.TabUtils
import kotlin.reflect.cast

class BackgroundShortcutActivity : AppCompatActivity(R.layout.activity_background_shortcut) {
    // View models
    private val model: BackgroundShortcutViewModel by viewModels()

    // Views
    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
    private val listView by lazy { findViewById<RecyclerView>(R.id.shortcutListView) }

    private val sharedPreferencesExt by lazy { SharedPreferencesExt(this) }
    private val adapter by lazy { BackgroundShortcutAdapter() }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { save() }

    private lateinit var selected: MutableSet<String>
    private lateinit var backgroundShortcuts: List<BackgroundShortcut>

    private var shortcutId: String? = null
    private var backgroundShortcutService: BackgroundShortcutService? = null
    private var serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = BackgroundShortcutService.ServiceBinder::class.cast(service)
            backgroundShortcutService = binder.getService()
            refresh()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            backgroundShortcutService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        shortcutId = intent.getStringExtra(IntentUtils.EXTRA_SHORTCUT_ID)

        adapter.getSelected = { id -> selected.contains(id) }
        adapter.onLayoutClick = { id ->
            val contains = selected.contains(id)
            if (contains) selected.remove(id) else selected.add(id)
            !contains
        }
        adapter.onSelectedChange = { id, value ->
            val contains = selected.contains(id)
            if (contains && !value) selected.remove(id)
            if (!contains && value) selected.add(id)
        }
        adapter.onDestroyClick = { id ->
            backgroundShortcutService?.destroyWebView(id)
            if (shortcutId == id) setResult(RESULT_OK)
            refresh()
        }
        adapter.onOpenClick = { id -> TabUtils.openInNewTab(this, shortcutId = id) }

        listView.layoutManager = LinearLayoutManager(this)
        listView.adapter = adapter

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.shortcuts.collect {
                    backgroundShortcuts = it
                    val savedSelected = getSavedSelected()
                    val validSelected = getValidSelected(savedSelected)
                    if (savedSelected != validSelected) setSaveSelected(validSelected)
                    if (!::selected.isInitialized) {
                        selected = validSelected.toMutableSet()
                    }
                    adapter.submitList(it)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, BackgroundShortcutService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (backgroundShortcutService != null) unbindService(serviceConnection)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_background_shortcut, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.shortcutSave -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                save()
            }
            true
        }
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun refresh() {
        backgroundShortcutService?.let { service ->
            val running = service.getRunning()
            model.next(running)
        }
    }

    private fun save() {
        val validSelected = getValidSelected(selected)
        setSaveSelected(validSelected)
        finish()
    }

    private fun getSavedSelected(): Set<String> = sharedPreferencesExt.backgroundShortcuts

    private fun setSaveSelected(selected: Set<String>) {
        sharedPreferencesExt.backgroundShortcuts = selected
    }

    private fun getValidSelected(selected: Set<String>): Set<String> {
        val list = backgroundShortcuts.map { it.id }
        val validSelected = mutableSetOf<String>()
        selected.forEach { if (list.contains(it)) validSelected.add(it) }
        return validSelected.toSet()
    }
}
