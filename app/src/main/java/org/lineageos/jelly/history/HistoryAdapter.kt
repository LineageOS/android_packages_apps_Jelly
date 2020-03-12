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

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.jelly.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

internal class HistoryAdapter(private val mContext: Context) : RecyclerView.Adapter<HistoryHolder>() {
    private val mHistoryDateFormat: DateFormat
    private var mCursor: Cursor? = null
    private var mIdColumnIndex = 0
    private var mTitleColumnIndex = 0
    private var mUrlColumnIndex = 0
    private var mTimestampColumnIndex = 0
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
            mTitleColumnIndex = cursor.getColumnIndexOrThrow(HistoryProvider.Columns.TITLE)
            mUrlColumnIndex = cursor.getColumnIndexOrThrow(HistoryProvider.Columns.URL)
            mTimestampColumnIndex = cursor.getColumnIndexOrThrow(HistoryProvider.Columns.TIMESTAMP)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): HistoryHolder {
        return HistoryHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false))
    }

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        if (!mCursor!!.moveToPosition(position)) {
            return
        }
        val timestamp = mCursor!!.getLong(mTimestampColumnIndex)
        val summary = mHistoryDateFormat.format(Date(timestamp))
        val title = mCursor!!.getString(mTitleColumnIndex)
        val url = mCursor!!.getString(mUrlColumnIndex)
        holder.bind(mContext, title, url, summary, timestamp)
    }

    override fun getItemCount(): Int {
        return if (mCursor != null) mCursor!!.count else 0
    }

    override fun getItemId(position: Int): Long {
        return if (mCursor!!.moveToPosition(position)) mCursor!!.getLong(mIdColumnIndex) else -1
    }

    init {
        mHistoryDateFormat = SimpleDateFormat(mContext.getString(R.string.history_date_format),
                Locale.getDefault())
        setHasStableIds(true)
    }
}