package com.classapp.schedule.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exam ORDER BY examDate ASC, examTimeRange ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exams: List<ExamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: ExamEntity)

    @Query("DELETE FROM exam")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM exam")
    suspend fun count(): Int
}
