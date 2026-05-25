package idv.wennyli.bloodpressurelog.ui.view.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import idv.wennyli.bloodpressurelog.utils.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class RecordListUiState(
    val records: List<BloodPressureRecord> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val filter: RecordFilter = RecordFilter.default(),
    val isDateRangeDialogVisible: Boolean = false,
)

@HiltViewModel
class RecordListViewModel @Inject constructor(
    private val repository: BloodPressureRepository,
    private val authRepository: AuthRepository,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordListUiState())
    val uiState: StateFlow<RecordListUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(RecordFilter.default())

    private val _navigateToEdit = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateToEdit: SharedFlow<String> = _navigateToEdit.asSharedFlow()

    init {
        observeRecords()
    }

    private fun observeRecords() {
        viewModelScope.launch {
            _filter.flatMapLatest { filter ->
                repository.observeRecords(filter.startMs, filter.endMs).map { it to filter }
            }.collect { (state, filter) ->
                when (state) {
                    DataState.Loading -> _uiState.update { it.copy(isLoading = true, filter = filter) }
                    is DataState.Success -> _uiState.update {
                        it.copy(
                            records = state.data.sortedByDescending { it.recordedAt },
                            isLoading = false,
                            errorMessage = null,
                            filter = filter,
                        )
                    }
                    is DataState.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = state.message, filter = filter)
                    }
                }
            }
        }
    }

    fun onDateRangeConfirmed(startMs: Long, endMs: Long) {
        _filter.value = RecordFilter(startMs = startMs, endMs = endMs)
    }

    fun onShowDateRangeDialog() {
        _uiState.update { it.copy(isDateRangeDialogVisible = true) }
    }

    fun onDismissDateRangeDialog() {
        _uiState.update { it.copy(isDateRangeDialogVisible = false) }
    }

    fun onEditRecord(id: String) {
        viewModelScope.launch { _navigateToEdit.emit(id) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecord(id)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "[RecordListViewModel] deleteRecord failed")
                _uiState.update {
                    it.copy(errorMessage = resourceProvider.getString(R.string.record_list_error_delete_failed))
                }
            }
        }
    }
}
