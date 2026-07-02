package com.classapp.schedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseName: String = "",
    val examDate: String = "",
    val examTimeRange: String = "",
    val classroom: String = "",
    val campus: String = "",
    val examMethod: String = "",
    val courseCode: String = "",
    val credits: String = "",
    val yearName: String = "",
    val semesterName: String = "",
    val teacherInfo: String = "",
    val isLocal: Boolean = false,
    val customStartTime: String = "",
    val customEndTime: String = "",
    val customRemark: String = ""
)
