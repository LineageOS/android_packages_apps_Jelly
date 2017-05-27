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
package org.lineageos.jelly.history;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryDatabaseHandler extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "HistoryDatabase";
    private static final String DB_TABLE_HISTORY = "history";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_URL = "url";


    public HistoryDatabaseHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DB_TABLE_HISTORY + " (" +
                KEY_ID + " INTEGER PRIMARY KEY, " +
                KEY_TITLE + " TEXT, " +
                KEY_URL + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Update this when db table will be changed
    }

    public void addItem(HistoryItem item) {
        if (item.getId() == -1) {
            item.setId(System.currentTimeMillis());
        }

        ContentValues values = new ContentValues();
        values.put(KEY_ID, item.getId());
        values.put(KEY_TITLE, item.getTitle());
        values.put(KEY_URL, item.getUrl());

        SQLiteDatabase db = getWritableDatabase();
        db.insert(DB_TABLE_HISTORY, null, values);
        db.close();
    }

    void deleteItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(DB_TABLE_HISTORY, KEY_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    List<HistoryItem> getAllItems() {
        List<HistoryItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {KEY_ID, KEY_TITLE, KEY_URL};
        String sortOrder = KEY_ID + " DESC";
        Cursor cursor = db.query(DB_TABLE_HISTORY, projection, null, null, null, null, sortOrder);

        if (cursor.moveToFirst()) {
            do {
                list.add(new HistoryItem(Long.parseLong(cursor.getString(0)),
                        cursor.getString(1), cursor.getString(2)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    void deleteAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(DB_TABLE_HISTORY, KEY_ID + ">=?", new String[]{"0"});
        db.close();
    }
}
