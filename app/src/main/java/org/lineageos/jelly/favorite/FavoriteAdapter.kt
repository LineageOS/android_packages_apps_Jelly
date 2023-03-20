/*
 * Copyright (C) 2020 The LineageOS Project
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

class FavoriteAdapter(private val mContext: Context) : RecyclerView.Adapter<FavoriteHolder>() {
    private var cursor: Cursor? = null
    private var idColumnIndex = 0
    private var titleColumnIndex = 0
    private var urlColumnIndex = 0
    private var colorColumnIndex = 0

    init {
        setHasStableIds(true)
    }

    fun swapCursor(cursor: Cursor?) {
        if (cursor === this.cursor) {
            return
        }
        this.cursor?.close()
        this.cursor = cursor
        this.cursor?.let {
            idColumnIndex = it.getColumnIndexOrThrow(BaseColumns._ID)
            titleColumnIndex = it.getColumnIndexOrThrow(FavoriteProvider.Columns.TITLE)
            urlColumnIndex = it.getColumnIndexOrThrow(FavoriteProvider.Columns.URL)
            colorColumnIndex = it.getColumnIndexOrThrow(FavoriteProvider.Columns.COLOR)
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
        val cursor = cursor ?: return
        val id = cursor.getLong(idColumnIndex)
        val title = cursor.getString(titleColumnIndex)
        val url = cursor.getString(urlColumnIndex)
        val color = cursor.getInt(colorColumnIndex)
        holder.bind(mContext, id, title, url, color)
    }

    override fun getItemCount() = cursor?.count ?: 0

    override fun getItemId(position: Int): Long {
        val cursor = cursor ?: return -1
        return if (cursor.moveToPosition(position)) cursor.getLong(idColumnIndex) else -1
    }
}
