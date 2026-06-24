package com.classapp.schedule.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startPeriod")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE dayOfWeek = :day ORDER BY startPeriod")
    fun getCoursesByDay(day: Int): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): Course?

    @Query("SELECT * FROM courses WHERE id = :id")
    fun getCourseByIdFlow(id: Long): Flow<Course?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteCourseById(id: Long)

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun getCourseCount(): Int

    @Query("SELECT * FROM courses WHERE name = :name ORDER BY dayOfWeek, startPeriod")
    fun getCoursesByName(name: String): Flow<List<Course>>

    @Query("SELECT DISTINCT name FROM courses ORDER BY name")
    fun getAllCourseNames(): Flow<List<String>>
}
