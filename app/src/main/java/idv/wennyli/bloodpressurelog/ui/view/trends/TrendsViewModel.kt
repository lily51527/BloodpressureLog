package idv.wennyli.bloodpressurelog.ui.view.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import idv.wennyli.bloodpressurelog.domain.usecase.BuildChartDataUseCase
import idv.wennyli.bloodpressurelog.ui.common.DateRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrendsUiState(
    val chartPoints: List<Pair<Float, Float>> = emptyList(),
    val xLabels: List<String> = emptyList(),
    val dateFilter: DateRangeFilter = DateRangeFilter.default(),
    val selectedMetric: TrendMetric = TrendMetric.SYSTOLIC,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val repository: BloodPressureRepository,
    private val buildChartDataUseCase: BuildChartDataUseCase,
) : ViewModel() {

    private val _recordsState =
        MutableStateFlow<DataState<List<BloodPressureRecord>>>(DataState.Loading)
    private val _dateFilter = MutableStateFlow(DateRangeFilter.default())
    private val _selectedMetric = MutableStateFlow(TrendMetric.SYSTOLIC)

    val uiState: StateFlow<TrendsUiState> = combine(
        _recordsState,
        _dateFilter,
        _selectedMetric,
    ) { state, dateFilter, metric ->
        when (state) {
            is DataState.Loading -> TrendsUiState(
                isLoading = true,
                dateFilter = dateFilter,
                selectedMetric = metric
            )

            is DataState.Error -> TrendsUiState(
                isLoading = false,
                dateFilter = dateFilter,
                selectedMetric = metric,
                errorMessage = state.message
            )

            is DataState.Success -> {
                val (points, labels) = buildChartDataUseCase(
                    state.data,
                    dateFilter.startMs,
                    dateFilter.endMs,
                    metric,
                )
                TrendsUiState(
                    chartPoints = points,
                    xLabels = labels,
                    dateFilter = dateFilter,
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

    fun onDateRangeConfirmed(startMs: Long, endMs: Long) {
        _dateFilter.value = DateRangeFilter(startMs = startMs, endMs = endMs)
    }

    fun onMetricChange(metric: TrendMetric) {
        _selectedMetric.value = metric
    }
}
