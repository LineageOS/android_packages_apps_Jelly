/*
 * SPDX-FileCopyrightText: 2020-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.favorite

import android.content.ContentResolver
import android.content.ContentUris
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.jelly.MainActivity
import org.lineageos.jelly.R
import org.lineageos.jelly.utils.UiUtils
import org.lineageos.jelly.viewmodels.FavoriteViewModel

class FavoriteActivity : AppCompatActivity(R.layout.activity_favorites) {
    // View models
    private val model: FavoriteViewModel by viewModels()

    // Views
    private val favoriteEmptyLayout by lazy { findViewById<View>(R.id.favoriteEmptyLayout) }
    private val favoriteListView by lazy { findViewById<RecyclerView>(R.id.favoriteListView) }
    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }

    private val adapter by lazy { FavoriteAdapter() }

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        favoriteListView.layoutManager = GridLayoutManager(this, 2)
        favoriteListView.itemAnimator = DefaultItemAnimator()
        favoriteListView.adapter = adapter
        val listTop = favoriteListView.top
        favoriteListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                toolbar.elevation = if (recyclerView.getChildAt(0).top < listTop) {
                    UiUtils.dpToPx(resources, resources.getDimension(R.dimen.toolbar_elevation))
                } else {
                    0f
                }
            }
        })

        adapter.onCardClick = { favorite ->
            val intent = Intent(this, MainActivity::class.java).apply {
                data = favorite.url.toUri()
            }
            startActivity(intent)
        }

        adapter.onCardLongClick = { favorite ->
            editItem(favorite.id, favorite.title, favorite.url)
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.favorites.collectLatest {
                    adapter.submitList(it)
                    val empty = it.isEmpty()
                    favoriteListView.isVisible = !empty
                    favoriteEmptyLayout.isVisible = empty
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun editItem(id: Long, title: String?, url: String) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_favorite_edit, LinearLayout(this))
        val titleEdit = view.findViewById<EditText>(R.id.favoriteTitleEditText)
        val urlEdit = view.findViewById<EditText>(R.id.favoriteUrlEditText)
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
            .setPositiveButton(
                R.string.favorite_edit_positive
            ) { dialog: DialogInterface, _: Int ->
                val updatedUrl = urlEdit.text.toString()
                val updatedTitle = titleEdit.text.toString()
                if (url.isEmpty()) {
                    urlEdit.error = error
                    urlEdit.requestFocus()
                }
                lifecycleScope.launch {
                    updateFavorite(
                        contentResolver, id, updatedTitle,
                        updatedUrl
                    )
                }
                dialog.dismiss()
            }
            .setNeutralButton(
                R.string.favorite_edit_delete
            ) { dialog: DialogInterface, _: Int ->
                lifecycleScope.launch {
                    deleteFavorite(contentResolver, id)
                }
                dialog.dismiss()
            }
            .setNegativeButton(
                android.R.string.cancel
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
    }

    private suspend fun updateFavorite(
        contentResolver: ContentResolver, id: Long,
        title: String, url: String
    ) {
        withContext(Dispatchers.IO) {
            FavoriteProvider.updateItem(contentResolver, id, title, url)
        }
    }

    private suspend fun deleteFavorite(contentResolver: ContentResolver, id: Long) {
        withContext(Dispatchers.IO) {
            val uri = ContentUris.withAppendedId(FavoriteProvider.Columns.CONTENT_URI, id)
            contentResolver.delete(uri, null, null)
        }
    }
}
