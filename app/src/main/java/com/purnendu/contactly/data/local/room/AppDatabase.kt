package com.purnendu.contactly.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScheduleEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2: Add temporaryImageUri column
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN temporaryImage TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE schedules ADD COLUMN originalImage TEXT DEFAULT NULL")
            }
        }
        fun getDataBase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(this)
                {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "contactly.db"
                    )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                }
            }
            return INSTANCE!!
        }
    }
}
