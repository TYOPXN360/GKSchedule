package com.ty.gkschedule.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Course::class, ExamEntity::class], version = 104, exportSchema = false)
abstract class CourseDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun examDao(): ExamDao

    companion object {
        @Volatile
        private var INSTANCE: CourseDatabase? = null

        // ponytail: fallbackToDestructiveMigration会清库，加列必须给迁移保住用户课表
        private val MIGRATION_103_104 = object : Migration(103, 104) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN chaoxingClassId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE courses ADD COLUMN chaoxingCourseId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE courses ADD COLUMN chaoxingFid INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): CourseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CourseDatabase::class.java,
                    "course_database"
                ).addMigrations(MIGRATION_103_104)
                 .fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
