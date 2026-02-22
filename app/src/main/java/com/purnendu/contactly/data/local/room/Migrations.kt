package com.purnendu.contactly.data.local.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All Room database migrations in one place.
 */
object Migrations {

    // Migration 1 → 2: Add image columns
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE schedules ADD COLUMN temporaryImage TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE schedules ADD COLUMN originalImage TEXT DEFAULT NULL")
        }
    }

    // Migration 2 → 3: Make time/day fields nullable for INSTANT schedule support
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // SQLite doesn't support ALTER COLUMN to remove NOT NULL
            // Recreate table with nullable time/day fields
            db.execSQL("""
                CREATE TABLE schedules_new (
                    scheduleId INTEGER NOT NULL PRIMARY KEY,
                    contactId INTEGER NOT NULL,
                    contactLookupKey TEXT,
                    originalName TEXT NOT NULL,
                    temporaryName TEXT NOT NULL,
                    temporaryImage TEXT DEFAULT NULL,
                    originalImage TEXT DEFAULT NULL,
                    startAtMillis INTEGER,
                    endAtMillis INTEGER,
                    selectedDays INTEGER,
                    scheduledAlarmsMetadata TEXT DEFAULT NULL,
                    scheduleType INTEGER NOT NULL,
                    instantSwitchStatus INTEGER DEFAULT NULL
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO schedules_new (
                    scheduleId, contactId, contactLookupKey, originalName, temporaryName,
                    temporaryImage, originalImage, startAtMillis, endAtMillis, selectedDays,
                    scheduledAlarmsMetadata, scheduleType, instantSwitchStatus
                )
                SELECT
                    scheduleId, contactId, contactLookupKey, originalName, temporaryName,
                    temporaryImage, originalImage, startAtMillis, endAtMillis, selectedDays,
                    scheduledAlarmsMetadata, scheduleType, NULL
                FROM schedules
            """.trimIndent())
            db.execSQL("DROP TABLE schedules")
            db.execSQL("ALTER TABLE schedules_new RENAME TO schedules")
        }
    }
}
