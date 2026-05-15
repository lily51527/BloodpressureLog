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

    /** 凌晨 0 時的紀錄，時段應判斷為夜間。 */
    @Test
    fun `hour 0 is NIGHT`() {
        assertThat(recordAt(0).timeSlot).isEqualTo(TimeSlot.NIGHT)
    }

    /** 凌晨 5:59 的紀錄，時段應仍屬夜間，不進入早上區間。 */
    @Test
    fun `hour 5 is NIGHT`() {
        assertThat(recordAt(5, 59).timeSlot).isEqualTo(TimeSlot.NIGHT)
    }

    /** 早上 6 時的紀錄，時段應判斷為早上。 */
    @Test
    fun `hour 6 is MORNING`() {
        assertThat(recordAt(6).timeSlot).isEqualTo(TimeSlot.MORNING)
    }

    /** 早上 11:59 的紀錄，時段應仍屬早上，不進入下午區間。 */
    @Test
    fun `hour 11 is MORNING`() {
        assertThat(recordAt(11, 59).timeSlot).isEqualTo(TimeSlot.MORNING)
    }

    /** 中午 12 時的紀錄，時段應判斷為下午。 */
    @Test
    fun `hour 12 is AFTERNOON`() {
        assertThat(recordAt(12).timeSlot).isEqualTo(TimeSlot.AFTERNOON)
    }

    /** 下午 17:59 的紀錄，時段應仍屬下午，不進入晚上區間。 */
    @Test
    fun `hour 17 is AFTERNOON`() {
        assertThat(recordAt(17, 59).timeSlot).isEqualTo(TimeSlot.AFTERNOON)
    }

    /** 晚上 18 時的紀錄，時段應判斷為晚上。 */
    @Test
    fun `hour 18 is EVENING`() {
        assertThat(recordAt(18).timeSlot).isEqualTo(TimeSlot.EVENING)
    }

    /** 晚上 23:59 的紀錄，時段應仍屬晚上，不進入夜間區間。 */
    @Test
    fun `hour 23 is EVENING`() {
        assertThat(recordAt(23, 59).timeSlot).isEqualTo(TimeSlot.EVENING)
    }
}
