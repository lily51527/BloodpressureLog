package idv.wennyli.bloodpressurelog.ui.view.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val _allRecords = MutableStateFlow<List<BloodPressureRecord>>(emptyList())
    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeRecords().collect { dataState ->
                when (dataState) {
                    is DataState.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is DataState.Success -> {
                        _allRecords.value = dataState.data
                        val current = _uiState.value
                        val (points, labels) = buildChartData(
                            dataState.data,
                            current.selectedRange,
                            current.selectedMetric
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                chartPoints = points,
                                xLabels = labels,
                                isEmpty = points.isEmpty(),
                                errorMessage = null
                            )
                        }
                    }

                    is DataState.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = dataState.message)
                    }
                }
            }
        }
    }

    fun onRangeChange(range: TrendRange) {
        val (points, labels) = buildChartData(
            _allRecords.value,
            range,
            _uiState.value.selectedMetric
        )
        _uiState.update {
            it.copy(
                selectedRange = range,
                chartPoints = points,
                xLabels = labels,
                isEmpty = points.isEmpty()
            )
        }
    }

    fun onMetricChange(metric: TrendMetric) {
        val (points, labels) = buildChartData(
            _allRecords.value,
            _uiState.value.selectedRange,
            metric
        )
        _uiState.update {
            it.copy(
                selectedMetric = metric,
                chartPoints = points,
                xLabels = labels,
                isEmpty = points.isEmpty()
            )
        }
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
