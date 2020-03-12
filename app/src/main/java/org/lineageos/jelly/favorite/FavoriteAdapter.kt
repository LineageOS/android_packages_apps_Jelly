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

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.jelly.R

internal class FavoriteAdapter(private val mContext: Context) : RecyclerView.Adapter<FavoriteHolder>() {
    private var mCursor: Cursor? = null
    private var mIdColumnIndex = 0
    private var mTitleColumnIndex = 0
    private var mUrlColumnIndex = 0
    private var mColorColumnIndex = 0
    fun swapCursor(cursor: Cursor?) {
        if (cursor === mCursor) {
            return
        }
        if (mCursor != null) {
            mCursor!!.close()
        }
        mCursor = cursor
        if (mCursor != null) {
            mIdColumnIndex = cursor!!.getColumnIndexOrThrow(BaseColumns._ID)
            mTitleColumnIndex = cursor.getColumnIndexOrThrow(FavoriteProvider.Columns.TITLE)
            mUrlColumnIndex = cursor.getColumnIndexOrThrow(FavoriteProvider.Columns.URL)
            mColorColumnIndex = cursor.getColumnIndexOrThrow(FavoriteProvider.Columns.COLOR)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): FavoriteHolder {
        return FavoriteHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite, parent, false))
    }

    override fun onBindViewHolder(holder: FavoriteHolder, position: Int) {
        if (!mCursor!!.moveToPosition(position)) {
            return
        }
        val id = mCursor!!.getLong(mIdColumnIndex)
        val title = mCursor!!.getString(mTitleColumnIndex)
        val url = mCursor!!.getString(mUrlColumnIndex)
        val color = mCursor!!.getInt(mColorColumnIndex)
        holder.bind(mContext, id, title, url, color)
    }

    override fun getItemCount(): Int {
        return if (mCursor != null) mCursor!!.count else 0
    }

    override fun getItemId(position: Int): Long {
        return if (mCursor!!.moveToPosition(position)) mCursor!!.getLong(mIdColumnIndex) else -1
    }

    init {
        setHasStableIds(true)
    }
}