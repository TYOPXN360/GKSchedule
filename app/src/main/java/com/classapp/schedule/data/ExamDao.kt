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

    @Query("DELETE FROM exam WHERE isLocal = 0")
    suspend fun deleteRemoteExams()

    @Query("DELETE FROM exam WHERE id = :id")
    suspend fun deleteExamById(id: Long)

    @Query("SELECT COUNT(*) FROM exam")
    suspend fun count(): Int
}
