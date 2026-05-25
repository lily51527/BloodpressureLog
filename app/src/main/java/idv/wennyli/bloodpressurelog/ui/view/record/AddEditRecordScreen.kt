package idv.wennyli.bloodpressurelog.ui.view.record

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun AddEditRecordScreen(
    onNavigateBack: () -> Unit,
    stayOnScreen: Boolean = false,
    viewModel: AddEditRecordViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.savedSuccessfully.collect {
            Toast.makeText(context, context.getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show()
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

    val currentZdt = remember(uiState.recordedAt) {
        Instant.ofEpochMilli(uiState.recordedAt).atZone(ZoneId.systemDefault())
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.toUtcMidnightMillis(uiState.recordedAt),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            val newEpoch = DateUtils.combineDateAndTime(
                                selectedDate,
                                currentZdt.hour,
                                currentZdt.minute,
                            )
                            onRecordedAtChange(newEpoch)
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.button_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.button_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = currentZdt.hour,
            initialMinute = currentZdt.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.dialog_title_time_picker)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newEpoch = DateUtils.combineDateAndTime(
                            DateUtils.toUtcMidnightMillis(uiState.recordedAt),
                            timePickerState.hour,
                            timePickerState.minute,
                        )
                        onRecordedAtChange(newEpoch)
                        showTimePicker = false
                    },
                ) { Text(stringResource(R.string.button_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.button_cancel)) }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) stringResource(R.string.screen_title_edit_record) else stringResource(R.string.screen_title_add_record)) },
                navigationIcon = {
                    if (showNavigateBack) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_description_navigate_back),
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

            OutlinedTextField(
                value = uiState.systolic,
                onValueChange = onSystolicChange,
                label = { Text(stringResource(R.string.label_systolic)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )

            OutlinedTextField(
                value = uiState.diastolic,
                onValueChange = onDiastolicChange,
                label = { Text(stringResource(R.string.label_diastolic)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )

            OutlinedTextField(
                value = uiState.pulse,
                onValueChange = onPulseChange,
                label = { Text(stringResource(R.string.label_pulse)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )

            OutlinedTextField(
                value = uiState.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.label_note)) },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )

            Text(
                text = stringResource(R.string.section_title_recorded_at),
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
                    Text(stringResource(R.string.button_save))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
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
