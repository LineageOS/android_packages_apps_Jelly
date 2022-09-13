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

import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import org.lineageos.jelly.utils.requireContextExt

class HistoryProvider : ContentProvider() {
    companion object {
        private const val MATCH_ALL = 0
        private const val MATCH_ID = 1
        private val sURIMatcher = UriMatcher(UriMatcher.NO_MATCH)
        fun addOrUpdateItem(resolver: ContentResolver, title: String?, url: String) {
            var existingId: Long = -1
            val cursor = resolver.query(
                Columns.CONTENT_URI, arrayOf(BaseColumns._ID),
                Columns.URL + "=?", arrayOf(url), null
            )
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    existingId = cursor.getLong(0)
                }
                cursor.close()
            }
            val values = ContentValues()
            values.put(Columns.TITLE, title)
            if (existingId >= 0) {
                resolver.update(
                    ContentUris.withAppendedId(Columns.CONTENT_URI, existingId),
                    values, null, null
                )
            } else {
                values.put(Columns.TIMESTAMP, System.currentTimeMillis())
                values.put(Columns.URL, url)
                resolver.insert(Columns.CONTENT_URI, values)
            }
        }

        init {
            sURIMatcher.addURI(Columns.AUTHORITY, "history", MATCH_ALL)
            sURIMatcher.addURI(Columns.AUTHORITY, "history/#", MATCH_ID)
        }
    }

    private lateinit var mDbHelper: HistoryDbHelper
    override fun onCreate(): Boolean {
        mDbHelper = HistoryDbHelper(context)
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?,
        selection: String?, selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val qb = SQLiteQueryBuilder()
        val match = sURIMatcher.match(uri)
        qb.tables = HistoryDbHelper.DB_TABLE_HISTORY
        when (match) {
            MATCH_ALL -> {
            }
            MATCH_ID -> qb.appendWhere(BaseColumns._ID + " = " + uri.lastPathSegment)
            else -> return null
        }
        val db = mDbHelper.readableDatabase
        val ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder)
        ret.setNotificationUri(requireContextExt().contentResolver, uri)
        return ret
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (sURIMatcher.match(uri) != MATCH_ALL) {
            return null
        }
        if (values != null && !values.containsKey(Columns.TIMESTAMP)) {
            values.put(Columns.TIMESTAMP, System.currentTimeMillis() / 1000)
        }
        val db = mDbHelper.writableDatabase
        val rowID = db.insert(HistoryDbHelper.DB_TABLE_HISTORY, null, values)
        if (rowID <= 0) {
            return null
        }
        requireContextExt().contentResolver.notifyChange(Columns.CONTENT_URI, null)
        return ContentUris.withAppendedId(Columns.CONTENT_URI, rowID)
    }

    override fun update(
        uri: Uri, values: ContentValues?,
        selection: String?, selectionArgs: Array<String>?
    ): Int {
        val count: Int
        val match = sURIMatcher.match(uri)
        val db = mDbHelper.writableDatabase
        count = when (match) {
            MATCH_ALL -> db.update(
                HistoryDbHelper.DB_TABLE_HISTORY,
                values, selection, selectionArgs
            )
            MATCH_ID -> {
                if (selection != null || selectionArgs != null) {
                    throw UnsupportedOperationException(
                        "Cannot update URI $uri with a where clause"
                    )
                }
                db.update(
                    HistoryDbHelper.DB_TABLE_HISTORY,
                    values, BaseColumns._ID + " = ?", arrayOf(uri.lastPathSegment)
                )
            }
            else -> throw UnsupportedOperationException("Cannot update that URI: $uri")
        }
        if (count > 0) {
            requireContextExt().contentResolver.notifyChange(Columns.CONTENT_URI, null)
        }
        return count
    }

    override fun delete(
        uri: Uri, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        var localSelection = selection
        var localSelectionArgs = selectionArgs
        val match = sURIMatcher.match(uri)
        val db = mDbHelper.writableDatabase
        when (match) {
            MATCH_ALL -> {
            }
            MATCH_ID -> {
                if (localSelection != null || localSelectionArgs != null) {
                    throw UnsupportedOperationException(
                        "Cannot delete URI $uri with a where clause"
                    )
                }
                localSelection = BaseColumns._ID + " = ?"
                uri.lastPathSegment?.let {
                    localSelectionArgs = arrayOf(it)
                }
            }
            else -> throw UnsupportedOperationException("Cannot delete the URI $uri")
        }
        val count = db.delete(
            HistoryDbHelper.DB_TABLE_HISTORY,
            localSelection, localSelectionArgs
        )
        if (count > 0) {
            requireContextExt().contentResolver.notifyChange(Columns.CONTENT_URI, null)
        }
        return count
    }

    interface Columns : BaseColumns {
        companion object {
            const val AUTHORITY = "org.lineageos.jelly.history"
            val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/history")
            const val TITLE = "title"
            const val URL = "url"
            const val TIMESTAMP = "timestamp"
        }
    }

    private class HistoryDbHelper(context: Context?) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE " + DB_TABLE_HISTORY + " (" +
                        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        Columns.TIMESTAMP + " INTEGER NOT NULL, " +
                        Columns.TITLE + " TEXT, " +
                        Columns.URL + " TEXT)"
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                // Recreate table with now auto-incrementing id column,
                // renaming the old id column to timestamp
                db.execSQL(
                    "CREATE TABLE " + DB_TABLE_HISTORY + "_new (" +
                            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            Columns.TIMESTAMP + " INTEGER NOT NULL, " +
                            Columns.TITLE + " TEXT, " +
                            Columns.URL + " TEXT)"
                )
                db.execSQL(
                    "INSERT INTO " + DB_TABLE_HISTORY + "_new("
                            + Columns.TITLE + ", " + Columns.URL + ", " + Columns.TIMESTAMP
                            + ") SELECT " + Columns.TITLE + ", " + Columns.URL + ", id"
                            + " FROM " + DB_TABLE_HISTORY
                )
                db.execSQL("DROP TABLE $DB_TABLE_HISTORY")
                db.execSQL(
                    "ALTER TABLE " + DB_TABLE_HISTORY
                            + "_new RENAME TO " + DB_TABLE_HISTORY
                )
            }
        }

        companion object {
            private const val DB_VERSION = 2
            private const val DB_NAME = "HistoryDatabase"
            const val DB_TABLE_HISTORY = "history"
        }
    }
}