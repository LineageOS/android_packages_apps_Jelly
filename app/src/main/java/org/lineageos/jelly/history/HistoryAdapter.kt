/*
 * SPDX-FileCopyrightText: 2020-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.history

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.jelly.R
import org.lineageos.jelly.model.History
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(context: Context) :
    ListAdapter<History, HistoryAdapter.HistoryHolder>(diffCallback) {
    var onRowClick: ((History) -> Unit) = { }

    private val historyDateFormat = SimpleDateFormat(
        context.getString(R.string.history_date_format),
        Locale.getDefault()
    )

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = HistoryHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
    )

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<LinearLayout>(R.id.rowHistoryLayout)
        private val summary = view.findViewById<TextView>(R.id.rowHistorySummaryTextView)
        private val title = view.findViewById<TextView>(R.id.rowHistoryTitleTextView)

        fun bind(history: History) {
            title.text = history.title.ifEmpty {
                history.url.split("/").toTypedArray()[2]
            }
            summary.text = historyDateFormat.format(Date(history.timestamp))
            layout.setOnClickListener {
                onRowClick(history)
            }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<History>() {
            override fun areItemsTheSame(oldItem: History, newItem: History) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: History, newItem: History) = oldItem == newItem
        }
    }
}
