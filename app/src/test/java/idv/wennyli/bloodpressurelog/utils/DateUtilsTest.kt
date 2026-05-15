package idv.wennyli.bloodpressurelog.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class DateUtilsTest {

    private fun epochOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /** formatDate 應將毫秒時間戳記格式化為 yyyy/MM/dd 的日期字串。 */
    @Test
    fun `formatDate returns yyyy slash MM slash dd`() {
        val epoch = epochOf(2024, 3, 15, 10, 30)
        assertThat(DateUtils.formatDate(epoch)).isEqualTo("2024/03/15")
    }

    /** formatTime 應將毫秒時間戳記格式化為 HH:mm 的時間字串。 */
    @Test
    fun `formatTime returns HH colon mm`() {
        val epoch = epochOf(2024, 3, 15, 9, 5)
        assertThat(DateUtils.formatTime(epoch)).isEqualTo("09:05")
    }

    /** formatDateTime 應將毫秒時間戳記格式化為日期與時間合併的字串。 */
    @Test
    fun `formatDateTime returns date and time combined`() {
        val epoch = epochOf(2024, 3, 15, 9, 5)
        assertThat(DateUtils.formatDateTime(epoch)).isEqualTo("2024/03/15 09:05")
    }

    /** combineDateAndTime 應將 UTC 午夜日期與本地時間組合成正確的日期時間。 */
    @Test
    fun `combineDateAndTime combines UTC midnight date with local hour and minute`() {
        val utcMidnight = LocalDateTime.of(2024, 3, 15, 0, 0)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        val result = DateUtils.combineDateAndTime(utcMidnight, hour = 14, minute = 30)

        assertThat(DateUtils.formatDate(result)).isEqualTo("2024/03/15")
        assertThat(DateUtils.formatTime(result)).isEqualTo("14:30")
    }

    /** toUtcMidnightMillis 與 combineDateAndTime 往返轉換後，日期與時間應與原始值一致。 */
    @Test
    fun `toUtcMidnightMillis and combineDateAndTime round-trip preserves date`() {
        val original = epochOf(2024, 6, 20, 22, 45)
        val utcMidnight = DateUtils.toUtcMidnightMillis(original)
        val roundTripped = DateUtils.combineDateAndTime(utcMidnight, 22, 45)
        assertThat(DateUtils.formatDate(roundTripped)).isEqualTo(DateUtils.formatDate(original))
        assertThat(DateUtils.formatTime(roundTripped)).isEqualTo(DateUtils.formatTime(original))
    }
}
