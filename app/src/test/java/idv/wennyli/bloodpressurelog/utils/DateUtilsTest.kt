package idv.wennyli.bloodpressurelog.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class DateUtilsTest {

    private fun epochOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    @Test
    fun `formatDate returns yyyy slash MM slash dd`() {
        val epoch = epochOf(2024, 3, 15, 10, 30)
        assertEquals("2024/03/15", DateUtils.formatDate(epoch))
    }

    @Test
    fun `formatTime returns HH colon mm`() {
        val epoch = epochOf(2024, 3, 15, 9, 5)
        assertEquals("09:05", DateUtils.formatTime(epoch))
    }

    @Test
    fun `formatDateTime returns date and time combined`() {
        val epoch = epochOf(2024, 3, 15, 9, 5)
        assertEquals("2024/03/15 09:05", DateUtils.formatDateTime(epoch))
    }

    @Test
    fun `combineDateAndTime combines UTC midnight date with local hour and minute`() {
        val utcMidnight = LocalDateTime.of(2024, 3, 15, 0, 0)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        val result = DateUtils.combineDateAndTime(utcMidnight, hour = 14, minute = 30)

        assertEquals("2024/03/15", DateUtils.formatDate(result))
        assertEquals("14:30", DateUtils.formatTime(result))
    }

    @Test
    fun `toUtcMidnightMillis and combineDateAndTime round-trip preserves date`() {
        val original = epochOf(2024, 6, 20, 22, 45)
        val utcMidnight = DateUtils.toUtcMidnightMillis(original)
        val roundTripped = DateUtils.combineDateAndTime(utcMidnight, 22, 45)
        assertEquals(DateUtils.formatDate(original), DateUtils.formatDate(roundTripped))
        assertEquals(DateUtils.formatTime(original), DateUtils.formatTime(roundTripped))
    }
}
