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
package org.lineageos.jelly.favorite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class FavoriteDatabaseHandler extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "FavoriteDatabase";
    private static final String DB_TABLE_FAVORITES = "favorites";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_URL = "url";
    private static final String KEY_COLOR = "color";


    public FavoriteDatabaseHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DB_TABLE_FAVORITES + " (" +
                KEY_ID + " INTEGER PRIMARY KEY, " +
                KEY_TITLE + " TEXT, " +
                KEY_URL + " TEXT, " +
                KEY_COLOR + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Update this when db table will be changed
    }

    public void addItem(Favorite item) {
        if (item.getId() == -1) {
            item.setId(System.currentTimeMillis());
        }

        ContentValues values = new ContentValues();
        values.put(KEY_ID, item.getId());
        values.put(KEY_TITLE, item.getTitle());
        values.put(KEY_URL, item.getUrl());
        values.put(KEY_COLOR, item.getColor());

        SQLiteDatabase db = getWritableDatabase();
        db.insert(DB_TABLE_FAVORITES, null, values);
        db.close();
    }

    void updateItem(Favorite item) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, item.getTitle());
        values.put(KEY_URL, item.getUrl());
        values.put(KEY_COLOR, item.getColor());

        db.update(DB_TABLE_FAVORITES, values, KEY_ID + "=?",
                new String[]{String.valueOf(item.getId())});
    }

    void deleteItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(DB_TABLE_FAVORITES, KEY_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    List<Favorite> getAllItems() {
        List<Favorite> list = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DB_TABLE_FAVORITES, null);

        if (cursor.moveToFirst()) {
            do {
                list.add(new Favorite(Long.parseLong(cursor.getString(0)),
                        cursor.getString(1), cursor.getString(2),
                        Integer.parseInt(cursor.getString(3))));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}
