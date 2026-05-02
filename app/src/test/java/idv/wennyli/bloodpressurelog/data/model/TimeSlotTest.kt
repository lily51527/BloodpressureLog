package idv.wennyli.bloodpressurelog.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TimeSlotTest {

    private fun epochMillisAt(hour: Int, minute: Int = 0): Long {
        return LocalDateTime.now()
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun recordAt(hour: Int, minute: Int = 0) = BloodPressureRecord(
        systolic = 120,
        diastolic = 80,
        pulse = 70,
        recordedAt = epochMillisAt(hour, minute),
    )

    @Test
    fun `hour 0 is NIGHT`() {
        assertEquals(TimeSlot.NIGHT, recordAt(0).timeSlot)
    }

    @Test
    fun `hour 5 is NIGHT`() {
        assertEquals(TimeSlot.NIGHT, recordAt(5, 59).timeSlot)
    }

    @Test
    fun `hour 6 is MORNING`() {
        assertEquals(TimeSlot.MORNING, recordAt(6).timeSlot)
    }

    @Test
    fun `hour 11 is MORNING`() {
        assertEquals(TimeSlot.MORNING, recordAt(11, 59).timeSlot)
    }

    @Test
    fun `hour 12 is AFTERNOON`() {
        assertEquals(TimeSlot.AFTERNOON, recordAt(12).timeSlot)
    }

    @Test
    fun `hour 17 is AFTERNOON`() {
        assertEquals(TimeSlot.AFTERNOON, recordAt(17, 59).timeSlot)
    }

    @Test
    fun `hour 18 is EVENING`() {
        assertEquals(TimeSlot.EVENING, recordAt(18).timeSlot)
    }

    @Test
    fun `hour 23 is EVENING`() {
        assertEquals(TimeSlot.EVENING, recordAt(23, 59).timeSlot)
    }
}
