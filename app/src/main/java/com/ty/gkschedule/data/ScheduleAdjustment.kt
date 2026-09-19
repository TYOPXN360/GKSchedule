package com.ty.gkschedule.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ScheduleAdjustment(
    val sourceDate: String,
    val targetDate: String,
    val enabled: Boolean = true
)

object ScheduleAdjustmentCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(value: String): List<ScheduleAdjustment> = runCatching {
        if (value.isBlank()) emptyList() else json.decodeFromString<List<ScheduleAdjustment>>(value)
    }.getOrDefault(emptyList())

    fun encode(value: List<ScheduleAdjustment>): String = json.encodeToString(value.distinct())
}
