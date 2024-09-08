/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.lineageos.jelly.dao.FavoriteDao
import org.lineageos.jelly.model.Favorite

@Database(entities = [Favorite::class], version = 2)
abstract class FavoriteDatabase : RoomDatabase() {
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate table with auto incrementing id column.
                db.execSQL("CREATE TABLE favorite_new (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, url TEXT, color INTEGER)")
                // Copy data
                db.execSQL("INSERT INTO favorite_new (title, url, color) SELECT title, url, color FROM favorite")
                // Remove old table
                db.execSQL("DROP TABLE favorite")
                // Rename new table
                db.execSQL("ALTER TABLE favorite_new RENAME TO favorite")
            }
        }

        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: FavoriteDatabase? = null

        fun getDatabase(context: Context): FavoriteDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, FavoriteDatabase::class.java, "FavoriteDatabase"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

    abstract fun favoriteDao(): FavoriteDao
}
