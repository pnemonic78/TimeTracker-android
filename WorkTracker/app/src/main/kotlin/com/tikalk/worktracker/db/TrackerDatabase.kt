package com.tikalk.worktracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.ProjectTaskKey

/**
 * Work Tracker database.
 */
@Database(entities = [Project::class, ProjectTask::class, ProjectTaskKey::class], version = 1, exportSchema = false)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): ProjectTaskDao
    abstract fun projectTaskKeyDao(): ProjectTaskKeyDao

    companion object {
        @Volatile
        private var instance: TrackerDatabase? = null

        fun getDatabase(context: Context): TrackerDatabase {
            if (instance == null) {
                synchronized(TrackerDatabase::class.java) {
                    if (instance == null) {
                        // Create database here
                        instance = Room.databaseBuilder(context.applicationContext, TrackerDatabase::class.java, "tracker.db").build()
                    }
                }
            }
            return instance!!
        }
    }
}