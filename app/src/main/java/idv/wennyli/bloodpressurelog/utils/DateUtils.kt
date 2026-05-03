package idv.wennyli.bloodpressurelog.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

    fun formatDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateFormatter)

    fun formatTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

    fun formatDateTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

    // DatePickerDialog uses UTC midnight. This converts local epoch millis to UTC midnight millis.
    fun toUtcMidnightMillis(epochMillis: Long): Long {
        val localDate = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    // Combines a UTC midnight millis (from DatePicker) with hour/minute in the local timezone.
    fun combineDateAndTime(utcMidnightMillis: Long, hour: Int, minute: Int): Long {
        val localDate: LocalDate = Instant.ofEpochMilli(utcMidnightMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return localDate.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
