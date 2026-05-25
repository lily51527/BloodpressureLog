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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class RecordListUiState(
    val records: List<BloodPressureRecord> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class RecordListViewModel @Inject constructor(
    private val repository: BloodPressureRepository,
    private val authRepository: AuthRepository,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordListUiState())
    val uiState: StateFlow<RecordListUiState> = _uiState.asStateFlow()

    private val _navigateToEdit = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateToEdit: SharedFlow<String> = _navigateToEdit.asSharedFlow()

    init {
        observeRecords()
    }

    private fun observeRecords() {
        viewModelScope.launch {
            repository.observeRecords().collect { state ->
                when (state) {
                    DataState.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is DataState.Success -> _uiState.update {
                        it.copy(
                            records = state.data.sortedByDescending { it.recordedAt },
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                    is DataState.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = state.message)
                    }
                }
            }
        }
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
