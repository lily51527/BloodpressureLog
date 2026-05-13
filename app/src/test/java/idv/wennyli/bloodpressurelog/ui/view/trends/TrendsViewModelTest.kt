package idv.wennyli.bloodpressurelog.ui.view.trends

import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import idv.wennyli.bloodpressurelog.domain.usecase.BuildChartDataUseCase
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
    private val mockBuildChartDataUseCase = mockk<BuildChartDataUseCase>()

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

    private fun createViewModel() = TrendsViewModel(mockRepository, mockBuildChartDataUseCase)

    @Test
    fun `initial state is loading`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `success with no chart points sets isEmpty true`() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(emptyList()))
        every { mockBuildChartDataUseCase(any(), any(), any()) } returns (emptyList<Pair<Float, Float>>() to emptyList())

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertNull(state.errorMessage)
    }

    @Test
    fun `error state sets errorMessage`() {
        every { mockRepository.observeRecords() } returns
            flowOf(DataState.Error(RuntimeException("error"), "載入失敗"))

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("載入失敗", state.errorMessage)
    }

    @Test
    fun `success with records produces chart points from use case`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(130, 85, 72, today.minusDays(1)),
        )
        val fakePoints = listOf(5f to 120f, 6f to 130f)
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(records))
        every { mockBuildChartDataUseCase(any(), any(), any()) } returns (fakePoints to emptyList())

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isEmpty)
        assertEquals(2, state.chartPoints.size)
    }

    @Test
    fun `onRangeChange updates selectedRange and calls use case with new range`() {
        val today = LocalDate.now()
        val records = listOf(record(120, 80, 70, today))
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(records))
        every { mockBuildChartDataUseCase(any(), TrendRange.DAYS_7, any()) } returns
            (listOf(6f to 120f) to List(7) { "" })
        every { mockBuildChartDataUseCase(any(), TrendRange.DAYS_14, any()) } returns
            (listOf(6f to 120f) to List(14) { "" })

        val viewModel = createViewModel()
        viewModel.onRangeChange(TrendRange.DAYS_14)

        val state = viewModel.uiState.value
        assertEquals(TrendRange.DAYS_14, state.selectedRange)
        assertEquals(14, state.xLabels.size)
    }

    @Test
    fun `onMetricChange updates selectedMetric and calls use case with new metric`() = runTest {
        val today = LocalDate.now()
        val records = listOf(record(120, 80, 70, today))
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(records))
        every { mockBuildChartDataUseCase(any(), any(), TrendMetric.SYSTOLIC) } returns
            (listOf(6f to 120f) to emptyList())
        every { mockBuildChartDataUseCase(any(), any(), TrendMetric.DIASTOLIC) } returns
            (listOf(6f to 80f) to emptyList())

        val viewModel = createViewModel()
        viewModel.onMetricChange(TrendMetric.DIASTOLIC)

        val state = viewModel.uiState.value
        assertEquals(TrendMetric.DIASTOLIC, state.selectedMetric)
        assertEquals(80f, state.chartPoints[0].second)
    }
}
