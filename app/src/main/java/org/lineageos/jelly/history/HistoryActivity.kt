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
package org.lineageos.jelly.history

import android.app.LoaderManager
import android.app.ProgressDialog
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_history.*
import org.lineageos.jelly.R
import org.lineageos.jelly.utils.UiUtils

class HistoryActivity : AppCompatActivity() {
    private val mHistoryAdapter by lazy {
        HistoryAdapter(this)
    }

    private val mAdapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            updateHistoryView(mHistoryAdapter.itemCount == 0)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            updateHistoryView(mHistoryAdapter.itemCount == 0)
        }
    }

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        setContentView(R.layout.activity_history)

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { finish() }

        loaderManager.initLoader(0, null, object : LoaderManager.LoaderCallbacks<Cursor> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
                return CursorLoader(this@HistoryActivity, HistoryProvider.Columns.CONTENT_URI, null, null, null, HistoryProvider.Columns.TIMESTAMP + " DESC")
            }

            override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
                mHistoryAdapter.swapCursor(cursor)
            }

            override fun onLoaderReset(loader: Loader<Cursor>) {
                mHistoryAdapter.swapCursor(null)
            }
        })

        history_list.layoutManager = LinearLayoutManager(this)
        history_list.addItemDecoration(HistoryAnimationDecorator(this))
        history_list.itemAnimator = DefaultItemAnimator()
        history_list.adapter = mHistoryAdapter

        mHistoryAdapter.registerAdapterDataObserver(mAdapterDataObserver)

        ItemTouchHelper(HistoryCallBack(this)).attachToRecyclerView(history_list)

        val listTop = history_list.top
        history_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val elevate = recyclerView.getChildAt(0) != null && recyclerView.getChildAt(0).top < listTop
                toolbar.elevation = if (elevate)
                    UiUtils.dpToPx(resources,
                            resources.getDimension(R.dimen.toolbar_elevation))
                else
                    0.toFloat()
            }
        })
    }

    public override fun onDestroy() {
        mHistoryAdapter.unregisterAdapterDataObserver(mAdapterDataObserver)
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
                .setPositiveButton(R.string.history_delete_positive
                ) { _, _ -> deleteAll() }
                .setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }
                .show()
        return true
    }

    private fun updateHistoryView(empty: Boolean) {
        history_empty_layout.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun deleteAll() {
        val dialog = ProgressDialog(this)
        dialog.setTitle(getString(R.string.history_delete_title))
        dialog.setMessage(getString(R.string.history_deleting_message))
        dialog.setCancelable(false)
        dialog.isIndeterminate = true
        dialog.show()

        object : AsyncTask<Void, Void, Boolean>() {
            override fun doInBackground(vararg params: Void): Boolean? {
                contentResolver.delete(HistoryProvider.Columns.CONTENT_URI, null, null)
                return true
            }

            override fun onPostExecute(param: Boolean?) {
                Handler().postDelayed({ dialog.dismiss() }, 1000)
            }
        }.execute()
    }
}
