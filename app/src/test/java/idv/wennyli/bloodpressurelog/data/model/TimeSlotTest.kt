package idv.wennyli.bloodpressurelog.data.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import java.time.Instant
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

    // ── 凌晨時段補全（hour 1~4 補足 NIGHT 完整覆蓋） ──────────────────────────

    /** 凌晨 1 時的紀錄，時段應判斷為夜間。 */
    @Test
    fun `hour 1 is NIGHT`() {
        assertThat(recordAt(1).timeSlot).isEqualTo(TimeSlot.NIGHT)
    }

    /** 凌晨 2 時的紀錄，時段應判斷為夜間。 */
    @Test
    fun `hour 2 is NIGHT`() {
        assertThat(recordAt(2).timeSlot).isEqualTo(TimeSlot.NIGHT)
    }

    /** 凌晨 3 時的紀錄，時段應判斷為夜間。 */
    @Test
    fun `hour 3 is NIGHT`() {
        assertThat(recordAt(3).timeSlot).isEqualTo(TimeSlot.NIGHT)
    }

    /** 凌晨 4 時的紀錄，時段應判斷為夜間。 */
    @Test
    fun `hour 4 is NIGHT`() {
        assertThat(recordAt(4).timeSlot).isEqualTo(TimeSlot.NIGHT)
    }

    // ── 異常值：記錄極端 epoch 毫秒的實際行為 ─────────────────────────────────

    /**
     * recordedAt = 0L（Unix epoch 起點）。
     * 以系統預設時區轉換後取得 hour，動態計算預期值以確保任何時區下測試均可通過。
     * 驗證目的：確認 timeSlot 不因極小值而拋出例外，且行為與直接用 Instant API 計算一致。
     */
    @Test
    fun `recordedAt 0L does not throw and matches expected time slot`() {
        val epochZero = 0L

        val hour = Instant.ofEpochMilli(epochZero)
            .atZone(ZoneId.systemDefault())
            .hour
        val expected = when (hour) {
            in 6..11 -> TimeSlot.MORNING
            in 12..17 -> TimeSlot.AFTERNOON
            in 18..23 -> TimeSlot.EVENING
            else -> TimeSlot.NIGHT
        }

        val record = BloodPressureRecord(
            systolic = 120, diastolic = 80, pulse = 70,
            recordedAt = epochZero,
        )
        assertThat(record.timeSlot).isEqualTo(expected)
    }

    /**
     * recordedAt = Long.MAX_VALUE（毫秒最大值，約西元 292278994 年）。
     * 驗證目的：確認 timeSlot 不因極大值而拋出例外，且行為與直接用 Instant API 計算一致。
     */
    @Test
    fun `recordedAt Long MAX_VALUE does not throw and matches expected time slot`() {
        val maxMillis = Long.MAX_VALUE

        val hour = Instant.ofEpochMilli(maxMillis)
            .atZone(ZoneId.systemDefault())
            .hour
        val expected = when (hour) {
            in 6..11 -> TimeSlot.MORNING
            in 12..17 -> TimeSlot.AFTERNOON
            in 18..23 -> TimeSlot.EVENING
            else -> TimeSlot.NIGHT
        }

        val record = BloodPressureRecord(
            systolic = 120, diastolic = 80, pulse = 70,
            recordedAt = maxMillis,
        )
        assertThat(record.timeSlot).isEqualTo(expected)
    }
}
