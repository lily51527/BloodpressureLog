package idv.wennyli.bloodpressurelog.ui.view.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.ui.theme.BloodPressureLogTheme
import idv.wennyli.bloodpressurelog.utils.DateUtils
import java.time.Instant
import java.time.ZoneId

/**
 * 血壓新增／編輯畫面的入口 Composable。
 *
 * 負責：
 * - 收集 [AddEditRecordViewModel] 的 [AddEditRecordUiState]
 * - 監聽 [AddEditRecordViewModel.savedSuccessfully] 儲存成功事件
 * - 依 [stayOnScreen] 決定儲存後行為：`true` 時重設表單（連續新增模式），`false` 時返回上一頁
 *
 * @param onNavigateBack 返回上一頁的回呼
 * @param stayOnScreen 儲存成功後是否留在畫面（連續新增模式）；預設 `false`（儲存後離開）
 * @param viewModel 由 Hilt 提供的 ViewModel 實例
 */
@Composable
fun AddEditRecordScreen(
    onNavigateBack: () -> Unit,
    stayOnScreen: Boolean = false,
    viewModel: AddEditRecordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.savedSuccessfully.collect {
            if (stayOnScreen) {
                viewModel.resetForm()
            } else {
                onNavigateBack()
            }
        }
    }

    AddEditRecordContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        showNavigateBack = !stayOnScreen,
        onSystolicChange = viewModel::onSystolicChange,
        onDiastolicChange = viewModel::onDiastolicChange,
        onPulseChange = viewModel::onPulseChange,
        onNoteChange = viewModel::onNoteChange,
        onRecordedAtChange = viewModel::onRecordedAtChange,
        onSave = viewModel::save,
    )
}

/**
 * 血壓新增／編輯畫面的 UI 內容層（無狀態 Composable）。
 *
 * 包含：收縮壓／舒張壓／脈搏／備註輸入欄位、日期與時間選擇器按鈕、
 * 錯誤訊息顯示，以及儲存按鈕（儲存中顯示進度指示器）。
 *
 * @param uiState 當前 UI 狀態
 * @param onNavigateBack 返回上一頁的回呼
 * @param showNavigateBack 是否顯示 TopAppBar 的返回按鈕；預設 `true`
 * @param onSystolicChange 收縮壓輸入變更回呼
 * @param onDiastolicChange 舒張壓輸入變更回呼
 * @param onPulseChange 脈搏輸入變更回呼
 * @param onNoteChange 備註輸入變更回呼
 * @param onRecordedAtChange 記錄時間變更回呼（epoch milliseconds）
 * @param onSave 儲存按鈕點擊回呼
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditRecordContent(
    uiState: AddEditRecordUiState,
    onNavigateBack: () -> Unit,
    showNavigateBack: Boolean = true,
    onSystolicChange: (String) -> Unit,
    onDiastolicChange: (String) -> Unit,
    onPulseChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onRecordedAtChange: (Long) -> Unit,
    onSave: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        RecordDatePickerDialog(
            recordedAt = uiState.recordedAt,
            onConfirm = { newRecordedAt ->
                onRecordedAtChange(newRecordedAt)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showTimePicker) {
        RecordTimePickerDialog(
            recordedAt = uiState.recordedAt,
            onConfirm = { newRecordedAt ->
                onRecordedAtChange(newRecordedAt)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) stringResource(R.string.add_edit_record_screen_title_edit) else stringResource(
                            R.string.add_edit_record_screen_title_add
                        )
                    )
                },
                navigationIcon = {
                    if (showNavigateBack) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.add_edit_record_cd_navigate_back),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            RecordFormField(
                value = uiState.systolic,
                onValueChange = onSystolicChange,
                label = stringResource(R.string.add_edit_record_label_systolic),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                enabled = !uiState.isLoading,
            )

            RecordFormField(
                value = uiState.diastolic,
                onValueChange = onDiastolicChange,
                label = stringResource(R.string.add_edit_record_label_diastolic),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                enabled = !uiState.isLoading,
            )

            RecordFormField(
                value = uiState.pulse,
                onValueChange = onPulseChange,
                label = stringResource(R.string.add_edit_record_label_pulse),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                enabled = !uiState.isLoading,
            )

            RecordFormField(
                value = uiState.note,
                onValueChange = onNoteChange,
                label = stringResource(R.string.add_edit_record_label_note),
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                enabled = !uiState.isLoading,
            )

            Text(
                text = stringResource(R.string.add_edit_record_section_recorded_at),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    enabled = !uiState.isLoading,
                ) {
                    Text(DateUtils.formatDate(uiState.recordedAt))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    enabled = !uiState.isLoading,
                ) {
                    Text(DateUtils.formatTime(uiState.recordedAt))
                }
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.add_edit_record_button_save))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 日期選擇對話框。
 *
 * 使用者選取新日期後，保留 [recordedAt] 原有的時、分，合併成新的 epoch milliseconds 回傳。
 *
 * @param recordedAt 目前的記錄時間（epoch milliseconds），用來初始化選取日期與保留時間部分
 * @param onConfirm 使用者確認選取後的回呼，傳入合併後的新時間（epoch milliseconds）
 * @param onDismiss 使用者取消或關閉對話框的回呼
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordDatePickerDialog(
    recordedAt: Long,
    onConfirm: (newRecordedAt: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentZdt = remember(recordedAt) {
        Instant.ofEpochMilli(recordedAt).atZone(ZoneId.systemDefault())
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.toUtcMidnightMillis(recordedAt),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedDate = datePickerState.selectedDateMillis
                    if (selectedDate != null) {
                        onConfirm(
                            DateUtils.combineDateAndTime(
                                selectedDate,
                                currentZdt.hour,
                                currentZdt.minute,
                            )
                        )
                    } else {
                        onDismiss()
                    }
                },
            ) { Text(stringResource(R.string.common_button_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_button_cancel)) }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * 時間選擇對話框。
 *
 * 使用者選取新時間後，保留 [recordedAt] 原有的日期部分，合併成新的 epoch milliseconds 回傳。
 *
 * @param recordedAt 目前的記錄時間（epoch milliseconds），用來初始化選取時間與保留日期部分
 * @param onConfirm 使用者確認選取後的回呼，傳入合併後的新時間（epoch milliseconds）
 * @param onDismiss 使用者取消或關閉對話框的回呼
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordTimePickerDialog(
    recordedAt: Long,
    onConfirm: (newRecordedAt: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentZdt = remember(recordedAt) {
        Instant.ofEpochMilli(recordedAt).atZone(ZoneId.systemDefault())
    }
    val timePickerState = rememberTimePickerState(
        initialHour = currentZdt.hour,
        initialMinute = currentZdt.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_edit_record_dialog_time_title)) },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        DateUtils.combineDateAndTime(
                            DateUtils.toUtcMidnightMillis(recordedAt),
                            timePickerState.hour,
                            timePickerState.minute,
                        )
                    )
                },
            ) { Text(stringResource(R.string.common_button_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_button_cancel)) }
        },
    )
}

/**
 * 通用的單行／多行文字輸入欄位（[OutlinedTextField] 的封裝）。
 *
 * @param value 當前輸入值
 * @param onValueChange 輸入內容變更回呼
 * @param label 欄位標籤文字
 * @param keyboardActions 鍵盤動作回呼（如 Next、Done）
 * @param modifier 外部傳入的 Modifier，預設填滿寬度
 * @param enabled 是否允許輸入；儲存中時應設為 `false`
 * @param keyboardType 鍵盤類型；預設 [KeyboardType.Number]（數字鍵盤）
 * @param imeAction IME 動作按鈕類型；預設 [ImeAction.Next]
 * @param singleLine 是否單行模式；預設 `true`
 * @param minLines 最小顯示行數；預設 `1`
 * @param maxLines 最大顯示行數；預設 `1`
 */
@Composable
private fun RecordFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Number,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
    )
}

@Preview(showBackground = true, name = "AddRecord")
@Composable
private fun AddRecordPreview() {
    BloodPressureLogTheme {
        AddEditRecordContent(
            uiState = AddEditRecordUiState(isEditMode = false),
            onNavigateBack = {},
            onSystolicChange = {},
            onDiastolicChange = {},
            onPulseChange = {},
            onNoteChange = {},
            onRecordedAtChange = {},
            onSave = {},
        )
    }
}

@Preview(showBackground = true, name = "EditRecord")
@Composable
private fun EditRecordPreview() {
    BloodPressureLogTheme {
        AddEditRecordContent(
            uiState = AddEditRecordUiState(
                systolic = "125",
                diastolic = "80",
                pulse = "72",
                note = "運動後量測",
                isEditMode = true,
            ),
            onNavigateBack = {},
            onSystolicChange = {},
            onDiastolicChange = {},
            onPulseChange = {},
            onNoteChange = {},
            onRecordedAtChange = {},
            onSave = {},
        )
    }
}

@Preview(showBackground = true, name = "AddRecord - Error")
@Composable
private fun AddRecordErrorPreview() {
    BloodPressureLogTheme {
        AddEditRecordContent(
            uiState = AddEditRecordUiState(
                systolic = "abc",
                errorMessage = "請輸入有效的正整數數值",
            ),
            onNavigateBack = {},
            onSystolicChange = {},
            onDiastolicChange = {},
            onPulseChange = {},
            onNoteChange = {},
            onRecordedAtChange = {},
            onSave = {},
        )
    }
}
