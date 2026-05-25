package idv.wennyli.bloodpressurelog.domain.usecase

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.ui.view.trends.TrendMetric
import idv.wennyli.bloodpressurelog.ui.view.trends.TrendRange
import kotlin.test.BeforeTest
import kotlin.test.Test
import java.time.LocalDate
import java.time.ZoneId

class BuildChartDataUseCaseTest {

    private lateinit var useCase: BuildChartDataUseCase

    private fun epochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun record(systolic: Int, diastolic: Int, pulse: Int, date: LocalDate) =
        BloodPressureRecord(
            id = "${systolic}_${date}",
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
            recordedAt = epochMillis(date),
        )

    @BeforeTest
    fun setUp() {
        useCase = BuildChartDataUseCase()
    }

    /** 各時間範圍（7 / 14 / 30 天）產生的 xLabels 數量應與 [TrendRange.days] 相符。 */
    @Test
    fun `xLabels count matches range days`() {
        val (_, labels7) = useCase(emptyList(), TrendRange.DAYS_7, TrendMetric.SYSTOLIC)
        val (_, labels14) = useCase(emptyList(), TrendRange.DAYS_14, TrendMetric.SYSTOLIC)
        val (_, labels30) = useCase(emptyList(), TrendRange.DAYS_30, TrendMetric.SYSTOLIC)

        assertThat(labels7).hasSize(7)
        assertThat(labels14).hasSize(14)
        assertThat(labels30).hasSize(30)
    }

    /** 傳入空紀錄清單時，圖表資料點應為空，不應拋出例外。 */
    @Test
    fun `empty records produce empty points`() {
        val (points, _) = useCase(emptyList(), TrendRange.DAYS_7, TrendMetric.SYSTOLIC)

        assertThat(points).isEmpty()
    }

    /** 超出時間範圍的紀錄（本例為 10 天前，超出 7 天範圍）應被過濾，不出現在圖表資料點中。 */
    @Test
    fun `records outside range are excluded`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(130, 85, 72, today.minusDays(10)),
        )

        val (points, _) = useCase(records, TrendRange.DAYS_7, TrendMetric.SYSTOLIC)

        assertThat(points).hasSize(1)
    }

    /** 同一天有多筆紀錄時，應合併為單一資料點並取平均值（120 + 140) / 2 = 130。 */
    @Test
    fun `daily average is correct for multiple records on same day`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(140, 90, 80, today),
        )

        val (points, _) = useCase(records, TrendRange.DAYS_7, TrendMetric.SYSTOLIC)

        assertThat(points).hasSize(1)
        assertThat(points[0].second).isEqualTo(130f)
    }

    /** 指標為 [TrendMetric.DIASTOLIC] 時，y 軸值應取舒張壓平均，而非收縮壓。 */
    @Test
    fun `metric DIASTOLIC returns correct average`() {
        val today = LocalDate.now()
        val records = listOf(record(120, 80, 70, today))

        val (points, _) = useCase(records, TrendRange.DAYS_7, TrendMetric.DIASTOLIC)

        assertThat(points).hasSize(1)
        assertThat(points[0].second).isEqualTo(80f)
    }

    /** 指標為 [TrendMetric.PULSE] 時，y 軸值應取脈搏平均，而非收縮壓或舒張壓。 */
    @Test
    fun `metric PULSE returns correct average`() {
        val today = LocalDate.now()
        val records = listOf(record(120, 80, 72, today))

        val (points, _) = useCase(records, TrendRange.DAYS_7, TrendMetric.PULSE)

        assertThat(points).hasSize(1)
        assertThat(points[0].second).isEqualTo(72f)
    }

    /**
     * dayIndex 應以 startDate 為基準（dayIndex = 0）往後計算：
     * - 7 天範圍中，today.minusDays(3) → dayIndex 3
     * - today（最後一天）→ dayIndex 6（= range.days - 1）
     */
    @Test
    fun `dayIndex is relative to startDate`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(130, 85, 72, today.minusDays(3)),
        )

        val (points, _) = useCase(records, TrendRange.DAYS_7, TrendMetric.SYSTOLIC)

        val sortedPoints = points.sortedBy { it.first }
        assertThat(sortedPoints).hasSize(2)
        assertThat(sortedPoints[0].first).isEqualTo(3f)   // today.minusDays(3) → dayIndex 3
        assertThat(sortedPoints[1].first).isEqualTo(6f)   // today → dayIndex 6 (range.days - 1)
    }

    /** 14 天範圍應包含今天與 10 天前的紀錄，排除 15 天前（超出範圍）的紀錄。 */
    @Test
    fun `records within 14-day range are included`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(130, 85, 72, today.minusDays(10)),
            record(140, 90, 74, today.minusDays(15)),
        )

        val (points, _) = useCase(records, TrendRange.DAYS_14, TrendMetric.SYSTOLIC)

        assertThat(points).hasSize(2)
    }

    /**
     * startDate 當天（邊界首日）的紀錄應被納入；startDate 前一天的紀錄應被排除。
     * 以 DAYS_7 為例：startDate = today.minusDays(6)，minusDays(7) 超出範圍。
     */
    @Test
    fun `record on startDate is included and record before startDate is excluded`() {
        val today = LocalDate.now()
        val startDate = today.minusDays((TrendRange.DAYS_7.days - 1).toLong())
        val records = listOf(
            record(120, 80, 70, startDate),
            record(130, 85, 72, startDate.minusDays(1)),
        )

        val (points, _) = useCase(records, TrendRange.DAYS_7, TrendMetric.SYSTOLIC)

        assertThat(points).hasSize(1)
        assertThat(points[0].second).isEqualTo(120f)
    }

    /**
     * xLabels 的每個元素格式應符合 "M/d"（例如 "5/18"），且不得為空白。
     * Vico 的 CartesianValueFormatter 若回傳空白字串會拋出 IllegalStateException。
     */
    @Test
    fun `xLabels have correct format and are not blank`() {
        val today = LocalDate.now()
        val startDate = today.minusDays((TrendRange.DAYS_7.days - 1).toLong())

        val (_, labels) = useCase(emptyList(), TrendRange.DAYS_7, TrendMetric.SYSTOLIC)

        assertThat(labels.first()).isEqualTo("${startDate.monthValue}/${startDate.dayOfMonth}")
        assertThat(labels.last()).isEqualTo("${today.monthValue}/${today.dayOfMonth}")
        assertThat(labels.none { it.isBlank() }).isEqualTo(true)
    }
}
