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

package org.lineageos.jelly.history

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.jelly.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private val context: Context) : RecyclerView.Adapter<HistoryHolder>() {
    private val historyDateFormat: DateFormat
    private var cursor: Cursor? = null
    private var idColumnIndex = 0
    private var titleColumnIndex = 0
    private var urlColumnIndex = 0
    private var timestampColumnIndex = 0

    init {
        historyDateFormat = SimpleDateFormat(
            context.getString(R.string.history_date_format),
            Locale.getDefault()
        )
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
            titleColumnIndex = it.getColumnIndexOrThrow(HistoryProvider.Columns.TITLE)
            urlColumnIndex = it.getColumnIndexOrThrow(HistoryProvider.Columns.URL)
            timestampColumnIndex = it.getColumnIndexOrThrow(HistoryProvider.Columns.TIMESTAMP)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): HistoryHolder {
        return HistoryHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
        )
    }

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        val cursor = cursor ?: return
        if (!cursor.moveToPosition(position)) {
            return
        }
        val timestamp = cursor.getLong(timestampColumnIndex)
        val summary = historyDateFormat.format(Date(timestamp))
        val title = cursor.getString(titleColumnIndex)
        val url = cursor.getString(urlColumnIndex)
        holder.bind(context, title, url, summary, timestamp)
    }

    override fun getItemCount() = cursor?.count ?: 0

    override fun getItemId(position: Int): Long {
        val cursor = cursor ?: return -1
        return if (cursor.moveToPosition(position)) cursor.getLong(idColumnIndex) else -1
    }
}
