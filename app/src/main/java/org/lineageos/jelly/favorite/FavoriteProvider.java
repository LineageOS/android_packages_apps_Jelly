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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FavoriteProvider extends ContentProvider {
    public interface Columns extends BaseColumns {
        String AUTHORITY = "org.lineageos.jelly.favorite";
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/favorite");

        String TITLE = "title";
        String URL = "url";
        String COLOR = "color";
    }

    private static final int MATCH_ALL = 0;
    private static final int MATCH_ID = 1;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(Columns.AUTHORITY, "favorite", MATCH_ALL);
        sURIMatcher.addURI(Columns.AUTHORITY, "favorite/#", MATCH_ID);
    }

    private FavoriteDbHelper mDbHelper;

    public static void addOrUpdateItem(ContentResolver resolver, String title, String url,
                                       int color) {
        long existingId = -1;
        Cursor cursor = resolver.query(Columns.CONTENT_URI, new String[] { Columns._ID },
                Columns.URL + "=?", new String[] { url }, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                existingId = cursor.getLong(0);
            }
            cursor.close();
        }

        ContentValues values = new ContentValues();
        values.put(Columns.TITLE, title);
        values.put(Columns.COLOR, color);

        if (existingId >= 0) {
            resolver.update(ContentUris.withAppendedId(Columns.CONTENT_URI, existingId),
                    values, null, null);
        } else {
            values.put(Columns.URL, url);
            resolver.insert(Columns.CONTENT_URI, values);
        }
    }

    public static void updateItem(ContentResolver resolver, long id, String title, String url) {
        ContentValues values = new ContentValues();
        values.put(Columns.TITLE, title);
        values.put(Columns.URL, url);

        resolver.update(ContentUris.withAppendedId(Columns.CONTENT_URI, id), values, null, null);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new FavoriteDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        int match = sURIMatcher.match(uri);

        qb.setTables(FavoriteDbHelper.DB_TABLE_FAVORITES);

        switch (match) {
            case MATCH_ALL:
                break;
            case MATCH_ID:
                qb.appendWhere(Columns._ID + " = " + uri.getLastPathSegment());
                break;
            default:
                return null;
        }

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        ret.setNotificationUri(getContext().getContentResolver(), uri);

        return ret;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (sURIMatcher.match(uri) != MATCH_ALL) {
            return null;
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long rowID = db.insert(FavoriteDbHelper.DB_TABLE_FAVORITES, null, values);
        if (rowID <= 0) {
            return null;
        }

        getContext().getContentResolver().notifyChange(Columns.CONTENT_URI, null);

        return ContentUris.withAppendedId(Columns.CONTENT_URI, rowID);
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        int count;
        int match = sURIMatcher.match(uri);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        switch (match) {
            case MATCH_ALL:
                count = db.update(FavoriteDbHelper.DB_TABLE_FAVORITES,
                        values, selection, selectionArgs);
                break;
            case MATCH_ID:
                if (selection != null || selectionArgs != null) {
                    throw new UnsupportedOperationException(
                            "Cannot update URI " + uri + " with a where clause");
                }
                count = db.update(FavoriteDbHelper.DB_TABLE_FAVORITES, values, Columns._ID + " = ?",
                        new String[] { uri.getLastPathSegment() });
                break;
            default:
                throw new UnsupportedOperationException("Cannot update that URI: " + uri);
        }

        if (count > 0) {
            getContext().getContentResolver().notifyChange(Columns.CONTENT_URI, null);
        }

        return count;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        int match = sURIMatcher.match(uri);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        switch (match) {
            case MATCH_ALL:
                break;
            case MATCH_ID:
                if (selection != null || selectionArgs != null) {
                    throw new UnsupportedOperationException(
                            "Cannot delete URI " + uri + " with a where clause");
                }
                selection = Columns._ID + " = ?";
                selectionArgs = new String[] { uri.getLastPathSegment() };
                break;
            default:
                throw new UnsupportedOperationException("Cannot delete the URI " + uri);
        }

        int count = db.delete(FavoriteDbHelper.DB_TABLE_FAVORITES, selection, selectionArgs);

        if (count > 0) {
            getContext().getContentResolver().notifyChange(Columns.CONTENT_URI, null);
        }

        return count;
    }

    private static class FavoriteDbHelper extends SQLiteOpenHelper {
        private static final int DB_VERSION = 2;
        private static final String DB_NAME = "FavoriteDatabase";
        private static final String DB_TABLE_FAVORITES = "favorites";

        FavoriteDbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + DB_TABLE_FAVORITES + " (" +
                    Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Columns.TITLE + " TEXT, " +
                    Columns.URL + " TEXT, " +
                    Columns.COLOR + " INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                // Recreate table with auto incrementing id column.
                db.execSQL("CREATE TABLE " + DB_TABLE_FAVORITES + "_new (" +
                        Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        Columns.TITLE + " TEXT, " +
                        Columns.URL + " TEXT, " +
                        Columns.COLOR + " INTEGER)");
                db.execSQL("INSERT INTO " + DB_TABLE_FAVORITES + "_new("
                        + Columns.TITLE + ", " + Columns.URL + ", " + Columns.COLOR
                        + ") SELECT " + Columns.TITLE + ", " + Columns.URL + ", " + Columns.COLOR
                        + " FROM " + DB_TABLE_FAVORITES);
                db.execSQL("DROP TABLE " + DB_TABLE_FAVORITES);
                db.execSQL("ALTER TABLE " + DB_TABLE_FAVORITES
                        + "_new RENAME TO " + DB_TABLE_FAVORITES);
            }
        }
    }
}
