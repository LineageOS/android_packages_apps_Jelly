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

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_history.view.*
import org.lineageos.jelly.MainActivity
import org.lineageos.jelly.R
import org.lineageos.jelly.utils.UiUtils
import java.text.SimpleDateFormat
import java.util.*

internal class HistoryHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(context: Context, id: Long, title: String?, url: String, timestamp: Long) {
        var new_title = title
        if (new_title == null || new_title.isEmpty()) {
            new_title = url.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2]
        }
        itemView.row_history_title.text = new_title
        itemView.row_history_summary.text = SimpleDateFormat(context.getString(R.string.history_date_format),
                Locale.getDefault()).format(Date(timestamp))

        itemView.row_history_layout.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        }

        itemView.row_history_layout.setOnLongClickListener {
            val uri = ContentUris.withAppendedId(HistoryProvider.Columns.CONTENT_URI, id)
            context.contentResolver.delete(uri, null, null)
            true
        }

        val background: Int
        when (UiUtils.getPositionInTime(timestamp)) {
            0 -> background = R.color.history_last_hour
            1 -> background = R.color.history_today
            2 -> background = R.color.history_this_week
            3 -> background = R.color.history_this_month
            else -> background = R.color.history_earlier
        }
        itemView.row_history_layout.background = ColorDrawable(ContextCompat.getColor(context, background))
    }

}
