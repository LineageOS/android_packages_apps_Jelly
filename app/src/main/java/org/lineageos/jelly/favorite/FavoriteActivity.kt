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
package org.lineageos.jelly.favorite

import android.app.LoaderManager
import android.content.*
import android.database.Cursor
import android.os.AsyncTask
import android.os.Bundle
import android.provider.BaseColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.jelly.R
import org.lineageos.jelly.favorite.FavoriteActivity
import org.lineageos.jelly.utils.UiUtils

class FavoriteActivity : AppCompatActivity() {
    private lateinit var mList: RecyclerView
    private lateinit var mEmptyView: View
    private lateinit var mAdapter: FavoriteAdapter
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.activity_favorites)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { finish() }
        mList = findViewById(R.id.favorite_list)
        mEmptyView = findViewById(R.id.favorite_empty_layout)
        mAdapter = FavoriteAdapter(this)
        loaderManager.initLoader(0, null, object : LoaderManager.LoaderCallbacks<Cursor> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
                return CursorLoader(this@FavoriteActivity, FavoriteProvider.Columns.CONTENT_URI,
                        null, null, null, BaseColumns._ID + " DESC")
            }

            override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
                mAdapter.swapCursor(cursor)
                if (cursor.count == 0) {
                    mList.visibility = View.GONE
                    mEmptyView.visibility = View.VISIBLE
                }
            }

            override fun onLoaderReset(loader: Loader<Cursor>) {
                mAdapter.swapCursor(null)
            }
        })
        mList.layoutManager = GridLayoutManager(this, 2)
        mList.itemAnimator = DefaultItemAnimator()
        mList.adapter = mAdapter
        val listTop = mList.top
        mList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                toolbar.elevation = if (recyclerView.getChildAt(0).top < listTop) UiUtils.dpToPx(resources,
                        resources.getDimension(R.dimen.toolbar_elevation)) else 0f
            }
        })
    }

    fun editItem(id: Long, title: String?, url: String) {
        val view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_favorite_edit, LinearLayout(this))
        val titleEdit = view.findViewById<EditText>(R.id.favorite_edit_title)
        val urlEdit = view.findViewById<EditText>(R.id.favorite_edit_url)
        titleEdit.setText(title)
        urlEdit.setText(url)
        val error = getString(R.string.favorite_edit_error)
        urlEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!s.toString().contains("http")) {
                    urlEdit.error = error
                }
            }

            override fun afterTextChanged(s: Editable) {
                if (!s.toString().contains("http")) {
                    urlEdit.error = error
                }
            }
        })
        AlertDialog.Builder(this)
                .setTitle(R.string.favorite_edit_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.favorite_edit_positive
                ) { dialog: DialogInterface, _: Int ->
                    val updatedUrl = urlEdit.text.toString()
                    val updatedTitle = titleEdit.text.toString()
                    if (url.isEmpty()) {
                        urlEdit.error = error
                        urlEdit.requestFocus()
                    }
                    UpdateFavoriteTask(contentResolver, id, updatedTitle,
                            updatedUrl).execute()
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.favorite_edit_delete
                ) { dialog: DialogInterface, _: Int ->
                    DeleteFavoriteTask(contentResolver).execute(id)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel
                ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .show()
    }

    private class UpdateFavoriteTask internal constructor(private val contentResolver: ContentResolver, private val id: Long, private val title: String, private val url: String) : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            FavoriteProvider.updateItem(contentResolver, id, title, url)
            return null
        }

    }

    private class DeleteFavoriteTask internal constructor(private val contentResolver: ContentResolver) : AsyncTask<Long?, Void?, Void?>() {
        override fun doInBackground(vararg ids: Long?): Void? {
            for (id in ids) {
                val uri = ContentUris.withAppendedId(FavoriteProvider.Columns.CONTENT_URI, id!!)
                contentResolver.delete(uri, null, null)
            }
            return null
        }
    }
}