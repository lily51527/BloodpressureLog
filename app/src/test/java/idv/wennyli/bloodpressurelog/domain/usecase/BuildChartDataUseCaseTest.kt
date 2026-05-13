package idv.wennyli.bloodpressurelog.domain.usecase

import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.ui.view.trends.TrendMetric
import idv.wennyli.bloodpressurelog.ui.view.trends.TrendRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
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

    @Before
    fun setUp() {
        useCase = BuildChartDataUseCase()
    }

    @Test
    fun `xLabels count matches range days`() {
        val (_, labels7) = useCase(emptyList(), TrendRange.DAYS_7, TrendMetric.SYSTOLIC)
        val (_, labels14) = useCase(emptyList(), TrendRange.DAYS_14, TrendMetric.SYSTOLIC)
        val (_, labels30) = useCase(emptyList(), TrendRange.DAYS_30, TrendMetric.SYSTOLIC)

        assertEquals(7, labels7.size)
        assertEquals(14, labels14.size)
        assertEquals(30, labels30.size)
    }

    @Test
    fun `empty records produce empty points`() {
        val (points, _) = useCase(emptyList(), TrendRange.DAYS_7, TrendMetric.SYSTOLIC)

        assertTrue(points.isEmpty())
    }

    @Test
    fun `records outside range are excluded`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(130, 85, 72, today.minusDays(10)),
        )

        val (points, _) = useCase(records, TrendRange.DAYS_7, TrendMetric.SYSTOLIC)

        assertEquals(1, points.size)
    }

    @Test
    fun `daily average is correct for multiple records on same day`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(140, 90, 80, today),
        )

        val (points, _) = useCase(records, TrendRange.DAYS_7, TrendMetric.SYSTOLIC)

        assertEquals(1, points.size)
        assertEquals(130f, points[0].second)
    }

    @Test
    fun `metric DIASTOLIC returns correct average`() {
        val today = LocalDate.now()
        val records = listOf(record(120, 80, 70, today))

        val (points, _) = useCase(records, TrendRange.DAYS_7, TrendMetric.DIASTOLIC)

        assertEquals(1, points.size)
        assertEquals(80f, points[0].second)
    }

    @Test
    fun `metric PULSE returns correct average`() {
        val today = LocalDate.now()
        val records = listOf(record(120, 80, 72, today))

        val (points, _) = useCase(records, TrendRange.DAYS_7, TrendMetric.PULSE)

        assertEquals(1, points.size)
        assertEquals(72f, points[0].second)
    }

    @Test
    fun `dayIndex is relative to startDate`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(130, 85, 72, today.minusDays(3)),
        )

        val (points, _) = useCase(records, TrendRange.DAYS_7, TrendMetric.SYSTOLIC)

        val sortedPoints = points.sortedBy { it.first }
        assertEquals(2, sortedPoints.size)
        assertEquals(3f, sortedPoints[0].first)   // today.minusDays(3) → dayIndex 3
        assertEquals(6f, sortedPoints[1].first)   // today → dayIndex 6 (range.days - 1)
    }

    @Test
    fun `records within 14-day range are included`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(130, 85, 72, today.minusDays(10)),
            record(140, 90, 74, today.minusDays(15)),
        )

        val (points, _) = useCase(records, TrendRange.DAYS_14, TrendMetric.SYSTOLIC)

        assertEquals(2, points.size)
    }
}
