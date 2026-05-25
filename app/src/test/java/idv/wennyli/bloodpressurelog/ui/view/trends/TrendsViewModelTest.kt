package idv.wennyli.bloodpressurelog.ui.view.trends

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import idv.wennyli.bloodpressurelog.domain.usecase.BuildChartDataUseCase
import io.mockk.every
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.time.LocalDate
import java.time.ZoneId

class TrendsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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

    @BeforeTest
    fun setUp() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Loading)
    }

    private fun createViewModel() = TrendsViewModel(mockRepository, mockBuildChartDataUseCase)

    /** Repository 尚未回傳資料（預設為 [DataState.Loading]）時，初始 uiState 應為 loading。 */
    @Test
    fun `initial state is loading`() {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value.isLoading).isTrue()
    }

    /** Loading 狀態下呼叫 [TrendsViewModel.onRangeChange]，[TrendsUiState.selectedRange] 應立即更新，且維持 loading 中。 */
    @Test
    fun `onRangeChange while loading updates selectedRange`() {
        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.isLoading).isTrue()

        viewModel.onRangeChange(TrendRange.DAYS_30)

        val state = viewModel.uiState.value
        assertThat(state.selectedRange).isEqualTo(TrendRange.DAYS_30)
        assertThat(state.isLoading).isTrue()
    }

    /** UseCase 回傳空圖表資料點時，[TrendsUiState.isEmpty] 應為 true，且無 loading 與 errorMessage。 */
    @Test
    fun `success with no chart points sets isEmpty true`() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(emptyList()))
        every {
            mockBuildChartDataUseCase(
                any(),
                any(),
                any()
            )
        } returns (emptyList<Pair<Float, Float>>() to emptyList())

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.isEmpty).isTrue()
        assertThat(state.errorMessage).isNull()
    }

    /** Repository 回傳 [DataState.Error] 時，[TrendsUiState.errorMessage] 應等於 error 中的 message。 */
    @Test
    fun `error state sets errorMessage`() {
        every { mockRepository.observeRecords() } returns
                flowOf(DataState.Error(RuntimeException("error"), "載入失敗"))

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("載入失敗")
    }

    /** [DataState.Error] 狀態下呼叫 [TrendsViewModel.onRangeChange]，errorMessage 應保留，且 selectedRange 應正確更新。 */
    @Test
    fun `error state preserves errorMessage when selectedRange changes`() {
        every { mockRepository.observeRecords() } returns
                flowOf(DataState.Error(RuntimeException("error"), "載入失敗"))

        val viewModel = createViewModel()
        viewModel.onRangeChange(TrendRange.DAYS_14)

        val state = viewModel.uiState.value
        assertThat(state.errorMessage).isEqualTo("載入失敗")
        assertThat(state.selectedRange).isEqualTo(TrendRange.DAYS_14)
        assertThat(state.isLoading).isFalse()
    }

    /**
     * Repository 有資料時，圖表資料點應完全來自 UseCase 的回傳值，
     * ViewModel 不應自行計算或修改，數量應與 UseCase 回傳的資料點數相符。
     */
    @Test
    fun `success with records produces chart points from use case`() {
        val today = LocalDate.now()
        val records = listOf(
            record(120, 80, 70, today),
            record(130, 85, 72, today.minusDays(1)),
        )
        val fakePoints = listOf(5f to 120f, 6f to 130f)
        val fakeLabels = listOf("1/1", "1/2")
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(records))
        every { mockBuildChartDataUseCase(any(), any(), any()) } returns (fakePoints to fakeLabels)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertThat(state.isEmpty).isFalse()
        assertThat(state.chartPoints).hasSize(2)
        assertThat(state.xLabels).hasSize(2)
    }

    /**
     * 呼叫 [TrendsViewModel.onRangeChange] 後：
     * - [TrendsUiState.selectedRange] 應更新為新範圍
     * - UseCase 應以新的 range 重新呼叫，[TrendsUiState.xLabels] 數量應對應新範圍天數
     */
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
        assertThat(state.selectedRange).isEqualTo(TrendRange.DAYS_14)
        assertThat(state.xLabels).hasSize(14)
    }

    /**
     * Repository Flow 從 Loading 轉為 Success 時，[TrendsUiState] 應從 loading 狀態正確轉換為顯示圖表資料。
     * (ViewModel 的 init 能不能持續監聽 Flow 的變化)
     */
    @Test
    fun `records flow transitions from loading to success`() = runTest {
        val recordsFlow = MutableStateFlow<DataState<List<BloodPressureRecord>>>(DataState.Loading)
        every { mockRepository.observeRecords() } returns recordsFlow
        every {
            mockBuildChartDataUseCase(
                any(),
                any(),
                any()
            )
        } returns (listOf(0f to 120f) to emptyList())

        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value.isLoading).isTrue()

        recordsFlow.value = DataState.Success(listOf(record(120, 80, 70, LocalDate.now())))

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.chartPoints).hasSize(1)
        assertThat(state.isEmpty).isFalse()
    }

    /**
     * 呼叫 [TrendsViewModel.onMetricChange] 後：
     * - [TrendsUiState.selectedMetric] 應更新為新指標
     * - UseCase 應以新的 metric 重新呼叫，圖表資料點應反映新指標的數值
     */
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
        assertThat(state.selectedMetric).isEqualTo(TrendMetric.DIASTOLIC)
        assertThat(state.chartPoints[0].second).isEqualTo(80f)
    }
}
