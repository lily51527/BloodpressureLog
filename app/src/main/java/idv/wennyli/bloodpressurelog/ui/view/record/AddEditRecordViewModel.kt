package idv.wennyli.bloodpressurelog.ui.view.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import idv.wennyli.bloodpressurelog.domain.usecase.SaveBloodPressureRecordUseCase
import idv.wennyli.bloodpressurelog.domain.usecase.SaveRecordResult
import idv.wennyli.bloodpressurelog.utils.ResourceProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AddEditRecordUiState(
    val systolic: String = "",
    val diastolic: String = "",
    val pulse: String = "",
    val note: String = "",
    val recordedAt: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false,
)

@HiltViewModel
class AddEditRecordViewModel @Inject constructor(
    private val repository: BloodPressureRepository,
    private val saveRecordUseCase: SaveBloodPressureRecordUseCase,
    private val resourceProvider: ResourceProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val recordId: String? = savedStateHandle["recordId"]

    private val _uiState = MutableStateFlow(AddEditRecordUiState(isEditMode = recordId != null))
    val uiState: StateFlow<AddEditRecordUiState> = _uiState.asStateFlow()

    private val _savedSuccessfully = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val savedSuccessfully: SharedFlow<Unit> = _savedSuccessfully.asSharedFlow()

    init {
        if (recordId != null) {
            loadRecord(recordId)
        }
    }

    private fun loadRecord(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val record = repository.getRecord(id)
                if (record != null) {
                    _uiState.update {
                        it.copy(
                            systolic = record.systolic.toString(),
                            diastolic = record.diastolic.toString(),
                            pulse = record.pulse.toString(),
                            note = record.note,
                            recordedAt = record.recordedAt,
                            isLoading = false,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = resourceProvider.getString(R.string.error_record_not_found))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "loadRecord failed")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: resourceProvider.getString(R.string.error_record_load_failed))
                }
            }
        }
    }

    fun onSystolicChange(value: String) {
        _uiState.update { it.copy(systolic = value, errorMessage = null) }
    }

    fun onDiastolicChange(value: String) {
        _uiState.update { it.copy(diastolic = value, errorMessage = null) }
    }

    fun onPulseChange(value: String) {
        _uiState.update { it.copy(pulse = value, errorMessage = null) }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun onRecordedAtChange(value: Long) {
        _uiState.update { it.copy(recordedAt = value) }
    }

    fun save() {
        val state = _uiState.value
        val systolic = state.systolic.toIntOrNull()
        val diastolic = state.diastolic.toIntOrNull()
        val pulse = state.pulse.toIntOrNull()

        if (systolic == null || diastolic == null || pulse == null ||
            systolic <= 0 || diastolic <= 0 || pulse <= 0
        ) {
            _uiState.update { it.copy(errorMessage = resourceProvider.getString(R.string.error_record_invalid_input)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = saveRecordUseCase(
                recordId = recordId,
                systolic = systolic,
                diastolic = diastolic,
                pulse = pulse,
                note = state.note,
                recordedAt = state.recordedAt,
            )) {
                is SaveRecordResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _savedSuccessfully.emit(Unit)
                }
                is SaveRecordResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message ?: resourceProvider.getString(R.string.error_record_save_failed),
                        )
                    }
                }
            }
        }
    }
}
