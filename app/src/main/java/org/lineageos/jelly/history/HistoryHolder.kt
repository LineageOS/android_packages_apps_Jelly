/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.history

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.jelly.MainActivity
import org.lineageos.jelly.R
import org.lineageos.jelly.utils.UiUtils

class HistoryHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val rootLayout = view.findViewById<LinearLayout>(R.id.row_history_layout)
    private val title = view.findViewById<TextView>(R.id.row_history_title)
    private val summary = view.findViewById<TextView>(R.id.row_history_summary)
    fun bind(context: Context, title: String, url: String, summary: String?, timestamp: Long) {
        val historyTitle = title.ifEmpty {
            url.split("/").toTypedArray()[2]
        }
        this.title.text = historyTitle
        this.summary.text = summary
        rootLayout.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        }
        val background = when (UiUtils.getPositionInTime(timestamp)) {
            0 -> R.color.history_last_hour
            1 -> R.color.history_today
            2 -> R.color.history_this_week
            3 -> R.color.history_this_month
            else -> R.color.history_earlier
        }
        rootLayout.background = ColorDrawable(ContextCompat.getColor(context, background))
    }
}
