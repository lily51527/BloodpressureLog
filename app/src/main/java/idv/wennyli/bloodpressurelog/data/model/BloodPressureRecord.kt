package idv.wennyli.bloodpressurelog.data.model

import java.time.Instant
import java.time.ZoneId

data class BloodPressureRecord(
    val id: String = "",
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val note: String = "",
    val recordedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

val BloodPressureRecord.timeSlot: TimeSlot
    get() {
        val hour = Instant.ofEpochMilli(recordedAt)
            .atZone(ZoneId.systemDefault())
            .hour
        return when (hour) {
            in 6..11 -> TimeSlot.MORNING
            in 12..17 -> TimeSlot.AFTERNOON
            in 18..23 -> TimeSlot.EVENING
            else -> TimeSlot.NIGHT
        }
    }
