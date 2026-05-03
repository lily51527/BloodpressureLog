package idv.wennyli.bloodpressurelog.ui.view.trends

import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TrendsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val mockRepository = mockk<BloodPressureRepository>()

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
        every { mockRepository.observeRecords() } returns flowOf(DataState.Loading)
    }

    @Test
    fun `initial state is loading`() {
        val viewModel = TrendsViewModel(mockRepository)
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `success with no records sets isEmpty true`() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(emptyList()))
        val viewModel = TrendsViewModel(mockRepository)
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertNull(state.errorMessage)
    }

    @Test
    fun `error state sets errorMessage`() {
        every { mockRepository.observeRecords() } returns
            flowOf(DataState.Error(RuntimeException("error"), "載入失敗"))
        val viewModel = TrendsViewModel(mockRepository)
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("載入失敗", state.errorMessage)
    }

    @Test
    fun `records within range produce chart points`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(130, 85, 72, today.minusDays(1)),
        )
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(records))
        val viewModel = TrendsViewModel(mockRepository)
        val state = viewModel.uiState.value
        assertFalse(state.isEmpty)
        assertEquals(2, state.chartPoints.size)
    }

    @Test
    fun `records outside range are excluded`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(130, 85, 72, today.minusDays(10)),
        )
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(records))
        val viewModel = TrendsViewModel(mockRepository)
        assertEquals(1, viewModel.uiState.value.chartPoints.size)
    }

    @Test
    fun `onRangeChange updates selectedRange and recomputes data`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(130, 85, 72, today.minusDays(10)),
        )
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(records))
        val viewModel = TrendsViewModel(mockRepository)

        viewModel.onRangeChange(TrendRange.DAYS_14)

        val state = viewModel.uiState.value
        assertEquals(TrendRange.DAYS_14, state.selectedRange)
        assertEquals(2, state.chartPoints.size)
        assertEquals(14, state.xLabels.size)
    }

    @Test
    fun `onMetricChange updates selectedMetric and recomputes diastolic data`() = runTest {
        val today = LocalDate.now()
        val records = listOf(record(120, 80, 70, today))
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(records))
        val viewModel = TrendsViewModel(mockRepository)

        viewModel.onMetricChange(TrendMetric.DIASTOLIC)

        val state = viewModel.uiState.value
        assertEquals(TrendMetric.DIASTOLIC, state.selectedMetric)
        assertEquals(1, state.chartPoints.size)
        assertEquals(80f, state.chartPoints[0].second)
    }

    @Test
    fun `buildChartData daily average is correct for multiple records on same day`() {
        val viewModel = TrendsViewModel(mockRepository)
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(140, 90, 80, today),
        )
        val (points, _) = viewModel.buildChartData(records, TrendRange.DAYS_7, TrendMetric.SYSTOLIC)
        assertEquals(1, points.size)
        assertEquals(130f, points[0].second)
    }

    @Test
    fun `buildChartData xLabels count matches range days`() {
        val viewModel = TrendsViewModel(mockRepository)
        val (_, labels7) = viewModel.buildChartData(emptyList(), TrendRange.DAYS_7, TrendMetric.SYSTOLIC)
        val (_, labels30) = viewModel.buildChartData(emptyList(), TrendRange.DAYS_30, TrendMetric.SYSTOLIC)
        assertEquals(7, labels7.size)
        assertEquals(30, labels30.size)
    }
}
