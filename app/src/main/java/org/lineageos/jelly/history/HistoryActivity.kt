/*
 * SPDX-FileCopyrightText: 2020-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.history

import android.content.ContentResolver
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.jelly.MainActivity
import org.lineageos.jelly.R
import org.lineageos.jelly.history.HistoryCallBack.OnDeleteListener
import org.lineageos.jelly.utils.UiUtils
import org.lineageos.jelly.viewmodels.HistoryViewModel

class HistoryActivity : AppCompatActivity(R.layout.activity_history) {
    // View models
    private val model: HistoryViewModel by viewModels()

    // Views
    private val historyEmptyLayout by lazy { findViewById<View>(R.id.historyEmptyLayout) }
    private val historyListView by lazy { findViewById<RecyclerView>(R.id.historyListView) }
    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }

    private val adapter by lazy { HistoryAdapter(this) }

    private val adapterDataObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            updateHistoryView(adapter.itemCount == 0)
        }
    }

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        historyListView.layoutManager = LinearLayoutManager(this)
        historyListView.addItemDecoration(HistoryAnimationDecorator(this))
        historyListView.itemAnimator = DefaultItemAnimator()
        historyListView.adapter = adapter
        val helper = ItemTouchHelper(HistoryCallBack(this, object : OnDeleteListener {
            override fun onItemDeleted(data: ContentValues?) {
                Snackbar.make(
                    findViewById(R.id.coordinatorLayout),
                    R.string.history_snackbar_item_deleted, Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.history_snackbar_item_deleted_message) {
                        contentResolver.insert(HistoryProvider.Columns.CONTENT_URI, data)
                    }
                    .show()
            }
        }))
        helper.attachToRecyclerView(historyListView)
        val listTop = historyListView.top
        historyListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val elevate = recyclerView.getChildAt(0) != null &&
                        recyclerView.getChildAt(0).top < listTop
                toolbar.elevation = if (elevate) UiUtils.dpToPx(
                    resources,
                    resources.getDimension(R.dimen.toolbar_elevation)
                ) else 0f
            }
        })

        adapter.registerAdapterDataObserver(adapterDataObserver)
        adapter.onRowClick = {
            val intent = Intent(this, MainActivity::class.java).apply {
                data = it.url.toUri()
            }
            startActivity(intent)
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.history.collectLatest {
                    adapter.submitList(it)
                    val empty = it.isEmpty()
                    historyListView.isVisible = !empty
                    historyEmptyLayout.isVisible = empty
                }
            }
        }
    }

    public override fun onDestroy() {
        adapter.unregisterAdapterDataObserver(adapterDataObserver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        R.id.menu_history_delete -> {
            AlertDialog.Builder(this)
                .setTitle(R.string.history_delete_title)
                .setMessage(R.string.history_delete_message)
                .setPositiveButton(R.string.history_delete_positive) { _, _ -> deleteAll() }
                .setNegativeButton(android.R.string.cancel) { d: DialogInterface, _ -> d.dismiss() }
                .show()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun updateHistoryView(empty: Boolean) {
        historyEmptyLayout.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun deleteAll() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.history_delete_title)
            .setView(R.layout.history_deleting_dialog)
            .setCancelable(false)
            .create()
        dialog.show()
        lifecycleScope.launch {
            deleteAllHistory(contentResolver, dialog)
        }
    }

    private suspend fun deleteAllHistory(contentResolver: ContentResolver, dialog: AlertDialog) {
        withContext(Dispatchers.IO) {
            contentResolver.delete(HistoryProvider.Columns.CONTENT_URI, null, null)
            withContext(Dispatchers.Main) {
                delay(200)
                dialog.dismiss()
            }
        }
    }
}
