/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.flow

import android.content.ContentResolver
import android.content.Context
import android.provider.BaseColumns
import androidx.core.os.bundleOf
import org.lineageos.jelly.ext.mapEachRow
import org.lineageos.jelly.ext.queryFlow
import org.lineageos.jelly.history.HistoryProvider
import org.lineageos.jelly.model.History

class HistoryFlow(private val context: Context) : QueryFlow<History> {
    override fun flowCursor() = context.contentResolver.queryFlow(
        HistoryProvider.Columns.CONTENT_URI,
        null,
        bundleOf(
            ContentResolver.QUERY_ARG_SQL_SORT_ORDER
                    to "${HistoryProvider.Columns.TIMESTAMP} DESC"
        )
    )

    override fun flowData() = flowCursor().mapEachRow(
        arrayOf(
            BaseColumns._ID,
            HistoryProvider.Columns.TITLE,
            HistoryProvider.Columns.URL,
            HistoryProvider.Columns.TIMESTAMP
        )
    ) { it, indexCache ->
        var i = 0
        History(
            it.getLong(indexCache[i++]),
            it.getString(indexCache[i++]),
            it.getString(indexCache[i++]),
            it.getLong(indexCache[i++])
        )
    }
}
