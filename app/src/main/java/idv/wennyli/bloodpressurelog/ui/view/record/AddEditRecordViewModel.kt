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
import idv.wennyli.bloodpressurelog.utils.SnackbarController
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
    private val snackbarController: SnackbarController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val recordId: String? = savedStateHandle["recordId"]

    private val _uiState = MutableStateFlow(AddEditRecordUiState(isEditMode = recordId != null))
    val uiState: StateFlow<AddEditRecordUiState> = _uiState.asStateFlow()

    /**
     * 儲存成功事件，供 Screen 決定後續行為（連續新增模式下 resetForm，否則導航返回上一頁）。
     * 不再與 Snackbar 顯示綁定——儲存成功的提示訊息改由 [snackbarController] 統一發送，
     * 由 App 殼層監聽顯示，生命週期不受此畫面是否離開 Composition 影響。
     */
    private val _savedSuccessfully = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val savedSuccessfully: SharedFlow<Unit> = _savedSuccessfully.asSharedFlow()

    init {
        if (recordId != null) {
            loadRecord(recordId)
        }
    }

    /**
     * 依 [id] 從 Repository 載入既有血壓紀錄，並將各欄位填入 [uiState]。
     * 找不到紀錄時顯示「找不到紀錄」錯誤；發生例外時顯示例外訊息或預設錯誤訊息。
     */
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
                        it.copy(isLoading = false, errorMessage = resourceProvider.getString(R.string.add_edit_record_error_not_found))
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "[AddEditRecordViewModel] loadRecord failed")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: resourceProvider.getString(R.string.add_edit_record_error_load_failed))
                }
            }
        }
    }

    /** 更新收縮壓（systolic）輸入值，並清除既有的錯誤訊息。 */
    fun onSystolicChange(value: String) {
        _uiState.update { it.copy(systolic = value, errorMessage = null) }
    }

    /** 更新舒張壓（diastolic）輸入值，並清除既有的錯誤訊息。 */
    fun onDiastolicChange(value: String) {
        _uiState.update { it.copy(diastolic = value, errorMessage = null) }
    }

    /** 更新脈搏（pulse）輸入值，並清除既有的錯誤訊息。 */
    fun onPulseChange(value: String) {
        _uiState.update { it.copy(pulse = value, errorMessage = null) }
    }

    /** 更新備註（note）輸入值。 */
    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    /** 更新紀錄時間（[value] 為 epoch milliseconds）。 */
    fun onRecordedAtChange(value: Long) {
        _uiState.update { it.copy(recordedAt = value) }
    }

    /**
     * 重設表單至初始狀態：清空所有輸入欄位（收縮壓、舒張壓、脈搏、備註）、
     * 清除錯誤訊息、解除載入狀態，並將記錄時間重設為當前時間。
     * 新增模式下儲存成功後呼叫此方法，以便快速連續輸入下一筆。
     */
    fun resetForm() {
        _uiState.update {
            it.copy(
                systolic = "",
                diastolic = "",
                pulse = "",
                note = "",
                recordedAt = System.currentTimeMillis(),
                isLoading = false,
                errorMessage = null,
            )
        }
    }

    /**
     * 驗證輸入欄位後，透過 [SaveBloodPressureRecordUseCase] 儲存血壓紀錄。
     *
     * - 輸入不合法（非正整數）時：更新 [uiState] 的 [AddEditRecordUiState.errorMessage]，直接返回。
     * - 儲存成功時：發射 [savedSuccessfully] 事件，由 Screen 決定後續導航或重設表單。
     * - 儲存失敗時：將錯誤訊息寫入 [AddEditRecordUiState.errorMessage]。
     */
    fun save() {
        val state = _uiState.value
        val systolic = state.systolic.toIntOrNull()
        val diastolic = state.diastolic.toIntOrNull()
        val pulse = state.pulse.toIntOrNull()

        if (systolic == null || diastolic == null || pulse == null ||
            systolic <= 0 || diastolic <= 0 || pulse <= 0
        ) {
            _uiState.update { it.copy(errorMessage = resourceProvider.getString(R.string.add_edit_record_error_invalid_input)) }
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
                    snackbarController.sendMessage(resourceProvider.getString(R.string.add_edit_record_message_save_success))
                    _savedSuccessfully.emit(Unit)
                }
                is SaveRecordResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message ?: resourceProvider.getString(R.string.add_edit_record_error_save_failed),
                        )
                    }
                }
            }
        }
    }
}
