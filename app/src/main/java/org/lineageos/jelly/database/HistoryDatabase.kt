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
import org.lineageos.jelly.dao.HistoryDao
import org.lineageos.jelly.model.History

@Database(entities = [History::class], version = 3)
abstract class HistoryDatabase : RoomDatabase() {
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate table with now auto-incrementing id column,
                // renaming the old id column to timestamp
                db.execSQL("CREATE TABLE history_new (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp INTEGER NOT NULL, title TEXT, url TEXT)")
                // Copy data
                db.execSQL("INSERT INTO history_new (title, url, timestamp) SELECT title, url, id FROM history")
                // Remove old table
                db.execSQL("DROP TABLE history")
                // Rename new table
                db.execSQL("ALTER TABLE history_new RENAME TO history")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Make _id, title, url and timestamp columns not nullable
                db.execSQL("CREATE TABLE history_new (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, url TEXT NOT NULL, timestamp INTEGER NOT NULL)")
                // Copy data
                db.execSQL("INSERT INTO history_new (_id, title, url, timestamp) SELECT _id, title, url, timestamp FROM history")
                // Remove old table
                db.execSQL("DROP TABLE history")
                // Rename new table
                db.execSQL("ALTER TABLE history_new RENAME TO history")
            }
        }

        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        fun getDatabase(context: Context): HistoryDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, HistoryDatabase::class.java, "HistoryDatabase"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

    abstract fun historyDao(): HistoryDao
}
