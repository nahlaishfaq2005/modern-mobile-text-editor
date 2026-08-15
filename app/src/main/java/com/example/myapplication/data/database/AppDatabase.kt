package com.example.myapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.database.dao.DeltaDao
import com.example.myapplication.data.database.dao.FileDao
import com.example.myapplication.data.database.dao.VersionDao
import com.example.myapplication.data.database.entity.DeltaEntity
import com.example.myapplication.data.database.entity.FileEntity
import com.example.myapplication.data.database.entity.VersionEntity

/**
 * Main database singleton for the application.
 * Manages Room entities for files, versions, and deltas.
 */
@Database(
    entities = [
        FileEntity::class,
        VersionEntity::class,
        DeltaEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * DAO for file metadata operations.
     */
    abstract fun fileDao(): FileDao

    /**
     * DAO for version metadata operations.
     */
    abstract fun versionDao(): VersionDao

    /**
     * DAO for delta/patch operations.
     */
    abstract fun deltaDao(): DeltaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton instance of the database.
         * 
         * @param context Application context
         * @return The AppDatabase instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                // Using destructive migration for now as we are in initial setup phase.
                // This allows changing the schema without providing migrations.
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
