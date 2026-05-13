package idv.wennyli.bloodpressurelog.data.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
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
        assertThat(recordAt(0).timeSlot).isEqualTo(TimeSlot.NIGHT)
    }

    @Test
    fun `hour 5 is NIGHT`() {
        assertThat(recordAt(5, 59).timeSlot).isEqualTo(TimeSlot.NIGHT)
    }

    @Test
    fun `hour 6 is MORNING`() {
        assertThat(recordAt(6).timeSlot).isEqualTo(TimeSlot.MORNING)
    }

    @Test
    fun `hour 11 is MORNING`() {
        assertThat(recordAt(11, 59).timeSlot).isEqualTo(TimeSlot.MORNING)
    }

    @Test
    fun `hour 12 is AFTERNOON`() {
        assertThat(recordAt(12).timeSlot).isEqualTo(TimeSlot.AFTERNOON)
    }

    @Test
    fun `hour 17 is AFTERNOON`() {
        assertThat(recordAt(17, 59).timeSlot).isEqualTo(TimeSlot.AFTERNOON)
    }

    @Test
    fun `hour 18 is EVENING`() {
        assertThat(recordAt(18).timeSlot).isEqualTo(TimeSlot.EVENING)
    }

    @Test
    fun `hour 23 is EVENING`() {
        assertThat(recordAt(23, 59).timeSlot).isEqualTo(TimeSlot.EVENING)
    }
}
