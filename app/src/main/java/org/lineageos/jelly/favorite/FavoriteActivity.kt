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

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_favorites.*
import kotlinx.android.synthetic.main.dialog_favorite_edit.view.*
import org.lineageos.jelly.R
import org.lineageos.jelly.utils.UiUtils
import java.util.*

class FavoriteActivity : AppCompatActivity() {
    private val mFavoriteDatabaseHandler by lazy {
        FavoriteDatabaseHandler(this)
    }

    private val mFavoriteAdapter by lazy {
        FavoriteAdapter(this)
    }

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        setContentView(R.layout.activity_favorites)

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { finish() }

        favorite_list.layoutManager = GridLayoutManager(this, 2)
        favorite_list.itemAnimator = DefaultItemAnimator()
        favorite_list.adapter = mFavoriteAdapter

        val listTop = favorite_list.top
        favorite_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                toolbar.elevation = if (recyclerView!!.getChildAt(0).top < listTop)
                    UiUtils.dpToPx(resources,
                            resources.getDimension(R.dimen.toolbar_elevation))
                else
                    0.toFloat()

            }
        })
    }

    public override fun onResume() {
        super.onResume()
        refresh()
    }

    internal fun refresh() {
        val items = mFavoriteDatabaseHandler.allItems
        // Reverse database list order
        Collections.reverse(items)
        mFavoriteAdapter.updateList(items)

        if (items.isEmpty()) {
            favorite_list.visibility = View.GONE
            favorite_empty_layout.visibility = View.VISIBLE
        }
    }

    internal fun editItem(item: Favorite) {
        val view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_favorite_edit, LinearLayout(this))
        val favorite_edit_title = view.rootView.favorite_edit_title
        val favorite_edit_url = view.rootView.favorite_edit_url

        favorite_edit_title.setText(item.title)
        favorite_edit_url.setText(item.url)

        val error = getString(R.string.favorite_edit_error)
        favorite_edit_url.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!s.toString().contains("http")) {
                    favorite_edit_url.error = error
                }
            }

            override fun afterTextChanged(s: Editable) {
                if (!s.toString().contains("http")) {
                    favorite_edit_url.error = error
                }
            }
        })

        AlertDialog.Builder(this)
                .setTitle(R.string.favorite_edit_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.favorite_edit_positive
                ) { dialog, _ ->
                    val url = favorite_edit_url.text.toString()
                    val title = favorite_edit_title.text.toString()
                    if (url.isEmpty()) {
                        favorite_edit_url.error = error
                        favorite_edit_url.requestFocus()
                    }
                    item.title = title
                    item.url = url
                    mFavoriteDatabaseHandler.updateItem(item)
                    refresh()
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.favorite_edit_delete
                ) { dialog, _ ->
                    mFavoriteDatabaseHandler.deleteItem(item.id)
                    mFavoriteAdapter.removeItem(item.id)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel
                ) { dialog, _ -> dialog.dismiss() }
                .show()
    }

}
