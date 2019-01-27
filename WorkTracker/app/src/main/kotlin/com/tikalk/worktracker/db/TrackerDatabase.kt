package com.tikalk.worktracker.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask

/**
 * Work Tracker database.
 */
@Database(entities = [Project::class, ProjectTask::class], version = 1)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun projectsDao(): ProjectDao
    abstract fun tasksDao(): ProjectTaskDao
}