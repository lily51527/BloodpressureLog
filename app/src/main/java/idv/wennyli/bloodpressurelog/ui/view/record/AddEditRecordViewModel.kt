package idv.wennyli.bloodpressurelog.ui.view.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditRecordUiState(
    val systolic: String = "",
    val diastolic: String = "",
    val pulse: String = "",
    val note: String = "",
    val recordedAt: Long = System.currentTimeMillis(),
    val originalCreatedAt: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false,
)

@HiltViewModel
class AddEditRecordViewModel @Inject constructor(
    private val repository: BloodPressureRepository,
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
            when (val result = repository.getRecord(id)) {
                is DataState.Success -> {
                    val record = result.data
                    if (record != null) {
                        _uiState.update {
                            it.copy(
                                systolic = record.systolic.toString(),
                                diastolic = record.diastolic.toString(),
                                pulse = record.pulse.toString(),
                                note = record.note,
                                recordedAt = record.recordedAt,
                                originalCreatedAt = record.createdAt,
                                isLoading = false,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "找不到紀錄") }
                    }
                }
                is DataState.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                DataState.Loading -> {}
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
            _uiState.update { it.copy(errorMessage = "請輸入有效的正整數數值") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (recordId != null) {
                repository.updateRecord(
                    BloodPressureRecord(
                        id = recordId,
                        systolic = systolic,
                        diastolic = diastolic,
                        pulse = pulse,
                        note = state.note,
                        recordedAt = state.recordedAt,
                        createdAt = state.originalCreatedAt,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            } else {
                repository.addRecord(
                    BloodPressureRecord(
                        systolic = systolic,
                        diastolic = diastolic,
                        pulse = pulse,
                        note = state.note,
                        recordedAt = state.recordedAt,
                    ),
                )
            }

            when (result) {
                is DataState.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _savedSuccessfully.emit(Unit)
                }
                is DataState.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                DataState.Loading -> {}
            }
        }
    }
}
