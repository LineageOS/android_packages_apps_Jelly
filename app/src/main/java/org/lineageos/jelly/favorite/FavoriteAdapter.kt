/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.favorite

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.jelly.R

class FavoriteAdapter(private val mContext: Context) : RecyclerView.Adapter<FavoriteHolder>() {
    private var mCursor: Cursor? = null
    private var mIdColumnIndex = 0
    private var mTitleColumnIndex = 0
    private var mUrlColumnIndex = 0
    private var mColorColumnIndex = 0

    init {
        setHasStableIds(true)
    }

    fun swapCursor(cursor: Cursor?) {
        if (cursor === mCursor) {
            return
        }
        mCursor?.close()
        mCursor = cursor
        mCursor?.let {
            mIdColumnIndex = it.getColumnIndexOrThrow(BaseColumns._ID)
            mTitleColumnIndex = it.getColumnIndexOrThrow(FavoriteProvider.Columns.TITLE)
            mUrlColumnIndex = it.getColumnIndexOrThrow(FavoriteProvider.Columns.URL)
            mColorColumnIndex = it.getColumnIndexOrThrow(FavoriteProvider.Columns.COLOR)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): FavoriteHolder {
        return FavoriteHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite, parent, false)
        )
    }

    override fun onBindViewHolder(holder: FavoriteHolder, position: Int) {
        val cursor = mCursor ?: return
        val id = cursor.getLong(mIdColumnIndex)
        val title = cursor.getString(mTitleColumnIndex)
        val url = cursor.getString(mUrlColumnIndex)
        val color = cursor.getInt(mColorColumnIndex)
        holder.bind(mContext, id, title, url, color)
    }

    override fun getItemCount() = mCursor?.count ?: 0

    override fun getItemId(position: Int): Long {
        val cursor = mCursor ?: return -1
        return if (cursor.moveToPosition(position)) cursor.getLong(mIdColumnIndex) else -1
    }
}
