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
package org.lineageos.jelly.favorite

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class FavoriteDatabaseHandler(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE " + DB_TABLE_FAVORITES + " (" +
                KEY_ID + " INTEGER PRIMARY KEY, " +
                KEY_TITLE + " TEXT, " +
                KEY_URL + " TEXT, " +
                KEY_COLOR + " INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Update this when db table will be changed
    }

    fun addItem(item: Favorite) {
        if (item.id == -1.toLong()) {
            item.id = System.currentTimeMillis()
        }

        val values = ContentValues()
        values.put(KEY_ID, item.id)
        values.put(KEY_TITLE, item.title)
        values.put(KEY_URL, item.url)
        values.put(KEY_COLOR, item.color)

        val db = writableDatabase
        db.insert(DB_TABLE_FAVORITES, null, values)
        db.close()
    }

    internal fun updateItem(item: Favorite) {
        val db = writableDatabase

        val values = ContentValues()
        values.put(KEY_TITLE, item.title)
        values.put(KEY_URL, item.url)
        values.put(KEY_COLOR, item.color)

        db.update(DB_TABLE_FAVORITES, values, KEY_ID + "=?",
                arrayOf(item.id.toString()))
    }

    internal fun deleteItem(id: Long) {
        val db = writableDatabase
        db.delete(DB_TABLE_FAVORITES, KEY_ID + "=?", arrayOf(id.toString()))
        db.close()
    }

    internal val allItems: List<Favorite>
        get() {
            val list = ArrayList<Favorite>()
            val db = writableDatabase
            val cursor = db.rawQuery("SELECT * FROM " + DB_TABLE_FAVORITES, null)

            if (cursor.moveToFirst()) {
                do {
                    list.add(Favorite(java.lang.Long.parseLong(cursor.getString(0)),
                            cursor.getString(1), cursor.getString(2),
                            Integer.parseInt(cursor.getString(3))))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            return list
        }

    companion object {
        private val DB_VERSION = 1
        private val DB_NAME = "FavoriteDatabase"
        private val DB_TABLE_FAVORITES = "favorites"
        private val KEY_ID = "id"
        private val KEY_TITLE = "title"
        private val KEY_URL = "url"
        private val KEY_COLOR = "color"
    }
}
