package idv.wennyli.bloodpressurelog.ui.view.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import idv.wennyli.bloodpressurelog.domain.usecase.BuildChartDataUseCase
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
    val selectedRange: TrendRange = TrendRange.DAYS_7,
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
                val (points, labels) = buildChartDataUseCase(state.data, range, metric)
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
}
