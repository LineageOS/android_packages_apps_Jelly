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
import org.lineageos.jelly.favorite.FavoriteProvider
import org.lineageos.jelly.model.Favorite

class FavoriteFlow(private val context: Context) : QueryFlow<Favorite> {
    override fun flowCursor() = context.contentResolver.queryFlow(
        FavoriteProvider.Columns.CONTENT_URI,
        null,
        bundleOf(ContentResolver.QUERY_ARG_SQL_SORT_ORDER to "${BaseColumns._ID} DESC")
    )

    override fun flowData() = flowCursor().mapEachRow(
        arrayOf(
            BaseColumns._ID,
            FavoriteProvider.Columns.TITLE,
            FavoriteProvider.Columns.URL,
            FavoriteProvider.Columns.COLOR
        )
    ) { it, indexCache ->
        var i = 0
        Favorite(
            it.getLong(indexCache[i++]),
            it.getString(indexCache[i++]),
            it.getString(indexCache[i++]),
            it.getInt(indexCache[i++])
        )
    }
}
