package idv.wennyli.bloodpressurelog.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import kotlin.test.Test
import java.time.Instant
import java.time.LocalDate
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

    /**
     * toUtcMidnightMillis 回傳的毫秒值，以 UTC 時區解析後，小時與分鐘應為 0，
     * 且日期應與輸入本地時間的日期一致。
     */
    @Test
    fun `toUtcMidnightMillis returns UTC midnight of the local date`() {
        val epoch = epochOf(2024, 3, 15, 22, 45)

        val utcMidnight = DateUtils.toUtcMidnightMillis(epoch)

        val zoned = Instant.ofEpochMilli(utcMidnight).atZone(ZoneOffset.UTC)
        assertThat(zoned.hour).isEqualTo(0)
        assertThat(zoned.minute).isEqualTo(0)
        assertThat(zoned.second).isEqualTo(0)
        assertThat(zoned.year).isEqualTo(2024)
        assertThat(zoned.monthValue).isEqualTo(3)
        assertThat(zoned.dayOfMonth).isEqualTo(15)
    }

    /** combineDateAndTime 在邊界時間（00:00 與 23:59）時應正確格式化，不發生日期偏移。 */
    @Test
    fun `combineDateAndTime with boundary hours returns correct time`() {
        val utcMidnight = LocalDateTime.of(2024, 3, 15, 0, 0)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        val midnight = DateUtils.combineDateAndTime(utcMidnight, hour = 0, minute = 0)
        val endOfDay = DateUtils.combineDateAndTime(utcMidnight, hour = 23, minute = 59)

        assertThat(DateUtils.formatDate(midnight)).isEqualTo("2024/03/15")
        assertThat(DateUtils.formatTime(midnight)).isEqualTo("00:00")
        assertThat(DateUtils.formatDate(endOfDay)).isEqualTo("2024/03/15")
        assertThat(DateUtils.formatTime(endOfDay)).isEqualTo("23:59")
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

    // ── startOfDay / endOfDay ───────────────────────────────────────────────────

    /** startOfDay(0) 應回傳今天 00:00:00.000 的 timestamp。 */
    @Test
    fun `startOfDay with daysAgo 0 returns start of today`() {
        val result = DateUtils.startOfDay(daysAgo = 0)
        val zoned = Instant.ofEpochMilli(result).atZone(ZoneId.systemDefault())

        assertThat(zoned.hour).isEqualTo(0)
        assertThat(zoned.minute).isEqualTo(0)
        assertThat(zoned.second).isEqualTo(0)
        assertThat(zoned.nano).isEqualTo(0)
        assertThat(zoned.toLocalDate()).isEqualTo(LocalDate.now(ZoneId.systemDefault()))
    }

    /** startOfDay(29) 應回傳 29 天前當天 00:00:00.000 的 timestamp。 */
    @Test
    fun `startOfDay with daysAgo 29 returns start of day 29 days ago`() {
        val result = DateUtils.startOfDay(daysAgo = 29)
        val zoned = Instant.ofEpochMilli(result).atZone(ZoneId.systemDefault())
        val expected = LocalDate.now(ZoneId.systemDefault()).minusDays(29)

        assertThat(zoned.hour).isEqualTo(0)
        assertThat(zoned.minute).isEqualTo(0)
        assertThat(zoned.second).isEqualTo(0)
        assertThat(zoned.toLocalDate()).isEqualTo(expected)
    }

    /** endOfDay(0) 應回傳今天 23:59:59.999 的 timestamp。 */
    @Test
    fun `endOfDay with daysAgo 0 returns end of today`() {
        val result = DateUtils.endOfDay(daysAgo = 0)
        val zoned = Instant.ofEpochMilli(result).atZone(ZoneId.systemDefault())

        assertThat(zoned.hour).isEqualTo(23)
        assertThat(zoned.minute).isEqualTo(59)
        assertThat(zoned.second).isEqualTo(59)
        assertThat(zoned.toLocalDate()).isEqualTo(LocalDate.now(ZoneId.systemDefault()))
    }

    /** startOfDay 的結果必須嚴格小於 endOfDay 同一天，確保篩選範圍正確。 */
    @Test
    fun `startOfDay is strictly less than endOfDay for same day`() {
        val start = DateUtils.startOfDay(daysAgo = 0)
        val end = DateUtils.endOfDay(daysAgo = 0)

        assertThat(start).isLessThan(end)
    }

    /** startOfDay(29) 篩選起點應嚴格小於 endOfDay(0) 篩選終點（預設 30 天範圍有效）。 */
    @Test
    fun `default 30 day range start is less than end`() {
        val start = DateUtils.startOfDay(daysAgo = 29)
        val end = DateUtils.endOfDay(daysAgo = 0)

        assertThat(start).isLessThan(end)
    }

    // ── utcMidnightToStartOfDay / utcMidnightToEndOfDay ────────────────────────

    /**
     * utcMidnightToStartOfDay 回傳的 timestamp 在本地時區解析後，
     * 小時應為 0，且日期與 UTC 午夜所代表的日期相同。
     */
    @Test
    fun `utcMidnightToStartOfDay returns local start of that date`() {
        val utcMidnight = LocalDate.of(2024, 5, 20)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val result = DateUtils.utcMidnightToStartOfDay(utcMidnight)
        val zoned = Instant.ofEpochMilli(result).atZone(ZoneId.systemDefault())

        assertThat(zoned.hour).isEqualTo(0)
        assertThat(zoned.minute).isEqualTo(0)
        assertThat(zoned.second).isEqualTo(0)
        assertThat(zoned.toLocalDate()).isEqualTo(LocalDate.of(2024, 5, 20))
    }

    /**
     * utcMidnightToEndOfDay 回傳的 timestamp 在本地時區解析後，
     * 時間應為 23:59:59，且日期與 UTC 午夜所代表的日期相同。
     */
    @Test
    fun `utcMidnightToEndOfDay returns local end of that date`() {
        val utcMidnight = LocalDate.of(2024, 5, 20)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val result = DateUtils.utcMidnightToEndOfDay(utcMidnight)
        val zoned = Instant.ofEpochMilli(result).atZone(ZoneId.systemDefault())

        assertThat(zoned.hour).isEqualTo(23)
        assertThat(zoned.minute).isEqualTo(59)
        assertThat(zoned.second).isEqualTo(59)
        assertThat(zoned.toLocalDate()).isEqualTo(LocalDate.of(2024, 5, 20))
    }

    /** utcMidnightToStartOfDay 的結果必須嚴格小於 utcMidnightToEndOfDay 同一天。 */
    @Test
    fun `utcMidnightToStartOfDay is less than utcMidnightToEndOfDay for same date`() {
        val utcMidnight = LocalDate.of(2024, 5, 20)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val start = DateUtils.utcMidnightToStartOfDay(utcMidnight)
        val end = DateUtils.utcMidnightToEndOfDay(utcMidnight)

        assertThat(start).isLessThan(end)
        assertThat(start).isGreaterThan(0L)
    }
}
