/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.favorite

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import org.lineageos.jelly.BuildConfig
import org.lineageos.jelly.ext.requireContextExt

class FavoriteProvider : ContentProvider() {
    companion object {
        private const val MATCH_ALL = 0
        private const val MATCH_ID = 1
        private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(Columns.AUTHORITY, "favorite", MATCH_ALL)
            addURI(Columns.AUTHORITY, "favorite/#", MATCH_ID)
        }
        fun addOrUpdateItem(
            resolver: ContentResolver, title: String?, url: String,
            color: Int
        ) {
            var existingId: Long = -1
            val cursor = resolver.query(
                Columns.CONTENT_URI, arrayOf(BaseColumns._ID),
                Columns.URL + "=?", arrayOf(url), null
            )
            cursor?.let {
                if (it.moveToFirst()) {
                    existingId = it.getLong(0)
                }
                it.close()
            }
            val values = ContentValues()
            values.put(Columns.TITLE, title)
            values.put(Columns.COLOR, color)
            if (existingId >= 0) {
                resolver.update(
                    ContentUris.withAppendedId(Columns.CONTENT_URI, existingId),
                    values, null, null
                )
            } else {
                values.put(Columns.URL, url)
                resolver.insert(Columns.CONTENT_URI, values)
            }
        }

        fun updateItem(resolver: ContentResolver, id: Long, title: String?, url: String?) {
            val values = ContentValues()
            values.put(Columns.TITLE, title)
            values.put(Columns.URL, url)
            resolver.update(ContentUris.withAppendedId(Columns.CONTENT_URI, id), values, null, null)
        }
    }

    private lateinit var dbHelper: FavoriteDbHelper
    override fun onCreate(): Boolean {
        dbHelper = FavoriteDbHelper(context)
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?,
        selection: String?, selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val qb = SQLiteQueryBuilder()
        val match = URI_MATCHER.match(uri)
        qb.tables = FavoriteDbHelper.DB_TABLE_FAVORITES
        when (match) {
            MATCH_ALL -> {
            }
            MATCH_ID -> qb.appendWhere(BaseColumns._ID + " = " + uri.lastPathSegment)
            else -> return null
        }
        val db = dbHelper.readableDatabase
        val ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder)
        ret.setNotificationUri(requireContextExt().contentResolver, uri)
        return ret
    }

    override fun getType(uri: Uri) = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (URI_MATCHER.match(uri) != MATCH_ALL) {
            return null
        }
        val db = dbHelper.writableDatabase
        val rowID = db.insert(FavoriteDbHelper.DB_TABLE_FAVORITES, null, values)
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
        val match = URI_MATCHER.match(uri)
        val db = dbHelper.writableDatabase
        val count = when (match) {
            MATCH_ALL -> db.update(
                FavoriteDbHelper.DB_TABLE_FAVORITES,
                values, selection, selectionArgs
            )
            MATCH_ID -> {
                if (selection != null || selectionArgs != null) {
                    throw UnsupportedOperationException(
                        "Cannot update URI $uri with a where clause"
                    )
                }
                db.update(
                    FavoriteDbHelper.DB_TABLE_FAVORITES,
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
        val match = URI_MATCHER.match(uri)
        val db = dbHelper.writableDatabase
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
            FavoriteDbHelper.DB_TABLE_FAVORITES,
            localSelection, localSelectionArgs
        )
        if (count > 0) {
            requireContextExt().contentResolver.notifyChange(Columns.CONTENT_URI, null)
        }
        return count
    }

    interface Columns : BaseColumns {
        companion object {
            const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.favorite"
            val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/favorite")
            const val TITLE = "title"
            const val URL = "url"
            const val COLOR = "color"
        }
    }

    private class FavoriteDbHelper(context: Context?) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE " + DB_TABLE_FAVORITES + " (" +
                        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        Columns.TITLE + " TEXT, " +
                        Columns.URL + " TEXT, " +
                        Columns.COLOR + " INTEGER)"
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                // Recreate table with auto incrementing id column.
                db.execSQL(
                    "CREATE TABLE " + DB_TABLE_FAVORITES + "_new (" +
                            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            Columns.TITLE + " TEXT, " +
                            Columns.URL + " TEXT, " +
                            Columns.COLOR + " INTEGER)"
                )
                db.execSQL(
                    "INSERT INTO " + DB_TABLE_FAVORITES + "_new("
                            + Columns.TITLE + ", " + Columns.URL + ", " + Columns.COLOR
                            + ") SELECT " + Columns.TITLE + ", " + Columns.URL + ", " + Columns.COLOR
                            + " FROM " + DB_TABLE_FAVORITES
                )
                db.execSQL("DROP TABLE $DB_TABLE_FAVORITES")
                db.execSQL(
                    "ALTER TABLE " + DB_TABLE_FAVORITES
                            + "_new RENAME TO " + DB_TABLE_FAVORITES
                )
            }
        }

        companion object {
            private const val DB_VERSION = 2
            private const val DB_NAME = "FavoriteDatabase"
            const val DB_TABLE_FAVORITES = "favorites"
        }
    }
}
