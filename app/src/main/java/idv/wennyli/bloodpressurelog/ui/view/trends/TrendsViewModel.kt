package idv.wennyli.bloodpressurelog.ui.view.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TrendsUiState(
    val chartPoints: List<Pair<Float, Float>> = emptyList(),
    val xLabels: List<String> = emptyList(),
    val selectedRange: TrendRange = TrendRange.DAYS_7,
    val selectedMetric: TrendMetric = TrendMetric.SYSTOLIC,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null,
)

enum class TrendRange(val days: Int, val label: String) {
    DAYS_7(7, "7天"),
    DAYS_14(14, "14天"),
    DAYS_30(30, "30天"),
}

enum class TrendMetric(val label: String) {
    SYSTOLIC("收縮壓"),
    DIASTOLIC("舒張壓"),
    PULSE("脈搏"),
}

@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val repository: BloodPressureRepository,
) : ViewModel() {

    private val _recordsState =
        MutableStateFlow<DataState<List<BloodPressureRecord>>>(DataState.Loading)
    private val _selectedRange = MutableStateFlow(TrendRange.DAYS_7)
    private val _selectedMetric = MutableStateFlow(TrendMetric.SYSTOLIC)

    val uiState: StateFlow<TrendsUiState> = combine(
        _recordsState,
        _selectedRange,
        _selectedMetric,
    ) { state, range, metric ->
        when (state) {
            is DataState.Loading -> TrendsUiState(
                isLoading = true,
                selectedRange = range,
                selectedMetric = metric
            )

            is DataState.Error -> TrendsUiState(
                isLoading = false,
                selectedRange = range,
                selectedMetric = metric,
                errorMessage = state.message
            )

            is DataState.Success -> {
                val (points, labels) = buildChartData(state.data, range, metric)
                TrendsUiState(
                    chartPoints = points,
                    xLabels = labels,
                    selectedRange = range,
                    selectedMetric = metric,
                    isLoading = false,
                    isEmpty = points.isEmpty(),
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TrendsUiState(),
    )

    init {
        viewModelScope.launch {
            repository.observeRecords().collect { _recordsState.value = it }
        }
    }

    fun onRangeChange(range: TrendRange) {
        _selectedRange.value = range
    }

    fun onMetricChange(metric: TrendMetric) {
        _selectedMetric.value = metric
    }

    internal fun buildChartData(
        records: List<BloodPressureRecord>,
        range: TrendRange,
        metric: TrendMetric,
    ): Pair<List<Pair<Float, Float>>, List<String>> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startDate = today.minusDays((range.days - 1).toLong())
        val formatter = DateTimeFormatter.ofPattern("M/d")

        val xLabels = (0 until range.days).map { i ->
            startDate.plusDays(i.toLong()).format(formatter)
        }

        val byDay = records
            .groupBy { Instant.ofEpochMilli(it.recordedAt).atZone(zone).toLocalDate() }
            .filterKeys { date -> !date.isBefore(startDate) && !date.isAfter(today) }

        val points = byDay.entries
            .sortedBy { it.key }
            .mapNotNull { (date, dayRecords) ->
                val dayIndex = (date.toEpochDay() - startDate.toEpochDay()).toFloat()
                val avg = when (metric) {
                    TrendMetric.SYSTOLIC -> dayRecords.map { it.systolic }.average()
                    TrendMetric.DIASTOLIC -> dayRecords.map { it.diastolic }.average()
                    TrendMetric.PULSE -> dayRecords.map { it.pulse }.average()
                }
                if (avg.isNaN()) null else dayIndex to avg.toFloat()
            }

        return points to xLabels
    }
}
