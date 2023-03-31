/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.history

import android.content.ContentResolver
import android.content.ContentValues
import android.content.DialogInterface
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.jelly.R
import org.lineageos.jelly.history.HistoryCallBack.OnDeleteListener
import org.lineageos.jelly.utils.UiUtils

class HistoryActivity : AppCompatActivity() {
    // Views
    private val emptyView by lazy { findViewById<View>(R.id.history_empty_layout) }
    private val list by lazy { findViewById<RecyclerView>(R.id.history_list) }
    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }

    private val uiScope = CoroutineScope(Dispatchers.Main)
    private lateinit var adapter: HistoryAdapter
    private val adapterDataObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            updateHistoryView(adapter.itemCount == 0)
        }
    }

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.activity_history)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { finish() }
        adapter = HistoryAdapter(this)
        val loader = LoaderManager.getInstance(this)
        loader.initLoader(0, null, object : LoaderManager.LoaderCallbacks<Cursor> {
            override fun onCreateLoader(id: Int, args: Bundle?) = CursorLoader(
                this@HistoryActivity, HistoryProvider.Columns.CONTENT_URI,
                null, null, null, HistoryProvider.Columns.TIMESTAMP + " DESC"
            )

            override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
                adapter.swapCursor(data)
            }

            override fun onLoaderReset(loader: Loader<Cursor>) {
                adapter.swapCursor(null)
            }
        })
        list.layoutManager = LinearLayoutManager(this)
        list.addItemDecoration(HistoryAnimationDecorator(this))
        list.itemAnimator = DefaultItemAnimator()
        list.adapter = adapter
        adapter.registerAdapterDataObserver(adapterDataObserver)
        val helper = ItemTouchHelper(HistoryCallBack(this, object : OnDeleteListener {
            override fun onItemDeleted(data: ContentValues?) {
                Snackbar.make(
                    findViewById(R.id.coordinator_layout),
                    R.string.history_snackbar_item_deleted, Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.history_snackbar_item_deleted_message) {
                        contentResolver.insert(HistoryProvider.Columns.CONTENT_URI, data)
                    }
                    .show()
            }
        }))
        helper.attachToRecyclerView(list)
        val listTop = list.top
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
    }

    public override fun onDestroy() {
        adapter.unregisterAdapterDataObserver(adapterDataObserver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.menu_history_delete) {
            return super.onOptionsItemSelected(item)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.history_delete_title)
            .setMessage(R.string.history_delete_message)
            .setPositiveButton(R.string.history_delete_positive) { _, _ -> deleteAll() }
            .setNegativeButton(android.R.string.cancel) { d: DialogInterface, _ -> d.dismiss() }
            .show()
        return true
    }

    private fun updateHistoryView(empty: Boolean) {
        emptyView.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun deleteAll() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.history_delete_title)
            .setView(R.layout.history_deleting_dialog)
            .setCancelable(false)
            .create()
        dialog.show()
        uiScope.launch {
            deleteAllHistory(contentResolver, dialog)
        }
    }

    private suspend fun deleteAllHistory(contentResolver: ContentResolver, dialog: AlertDialog) {
        withContext(Dispatchers.Default) {
            contentResolver.delete(HistoryProvider.Columns.CONTENT_URI, null, null)
            withContext(Dispatchers.Main) {
                Handler(Looper.getMainLooper()).postDelayed({ dialog.dismiss() }, 1000)
            }
        }
    }
}
