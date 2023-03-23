/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
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

class HistoryAdapter(private val mContext: Context) : RecyclerView.Adapter<HistoryHolder>() {
    private val mHistoryDateFormat: DateFormat
    private var mCursor: Cursor? = null
    private var mIdColumnIndex = 0
    private var mTitleColumnIndex = 0
    private var mUrlColumnIndex = 0
    private var mTimestampColumnIndex = 0

    init {
        mHistoryDateFormat = SimpleDateFormat(mContext.getString(R.string.history_date_format),
                Locale.getDefault())
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
            mTitleColumnIndex = it.getColumnIndexOrThrow(HistoryProvider.Columns.TITLE)
            mUrlColumnIndex = it.getColumnIndexOrThrow(HistoryProvider.Columns.URL)
            mTimestampColumnIndex = it.getColumnIndexOrThrow(HistoryProvider.Columns.TIMESTAMP)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): HistoryHolder {
        return HistoryHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false))
    }

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        val cursor = mCursor ?: return
        if (!cursor.moveToPosition(position)) {
            return
        }
        val timestamp = cursor.getLong(mTimestampColumnIndex)
        val summary = mHistoryDateFormat.format(Date(timestamp))
        val title = cursor.getString(mTitleColumnIndex)
        val url = cursor.getString(mUrlColumnIndex)
        holder.bind(mContext, title, url, summary, timestamp)
    }

    override fun getItemCount() = mCursor?.count ?: 0

    override fun getItemId(position: Int): Long {
        val cursor = mCursor ?: return -1
        return if (cursor.moveToPosition(position)) cursor.getLong(mIdColumnIndex) else -1
    }
}
