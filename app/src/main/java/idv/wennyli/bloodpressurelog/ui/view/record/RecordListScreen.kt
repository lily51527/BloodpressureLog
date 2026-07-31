package idv.wennyli.bloodpressurelog.ui.view.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.model.BloodPressureLevel
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.TimeSlot
import idv.wennyli.bloodpressurelog.data.model.bloodPressureLevel
import idv.wennyli.bloodpressurelog.data.model.timeSlot
import idv.wennyli.bloodpressurelog.ui.common.DateRangeFilterBar
import idv.wennyli.bloodpressurelog.ui.common.color
import idv.wennyli.bloodpressurelog.ui.theme.BloodPressureLogTheme
import idv.wennyli.bloodpressurelog.utils.DateUtils

@Composable
fun RecordListScreen(
    onNavigateToEdit: (recordId: String) -> Unit,
    viewModel: RecordListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToEdit.collect { recordId ->
            onNavigateToEdit(recordId)
        }
    }

    var recordIdToDelete by remember { mutableStateOf<String?>(null) }

    if (recordIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordIdToDelete = null },
            title = { Text(stringResource(R.string.record_list_dialog_delete_title)) },
            text = { Text(stringResource(R.string.record_list_dialog_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(id = recordIdToDelete!!)
                        recordIdToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.record_list_button_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordIdToDelete = null }) { Text(stringResource(R.string.common_button_cancel)) }
            },
        )
    }

    RecordListContent(
        uiState = uiState,
        onSignOut = viewModel::signOut,
        onEditRecord = viewModel::onEditRecord,
        onDeleteRecord = { recordIdToDelete = it },
        onDateRangeConfirmed = viewModel::onDateRangeConfirmed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordListContent(
    uiState: RecordListUiState,
    onSignOut: () -> Unit,
    onEditRecord: (String) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onDateRangeConfirmed: (startMs: Long, endMs: Long) -> Unit = { _, _ -> },
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.record_list_screen_title)) },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = stringResource(R.string.record_list_cd_sign_out))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            DateRangeFilterBar(
                startMs = uiState.filter.startMs,
                endMs = uiState.filter.endMs,
                onDateRangeConfirmed = onDateRangeConfirmed,
            )
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.errorMessage != null -> {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                        )
                    }
                    uiState.records.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.record_list_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(uiState.records, key = { it.id }) { record ->
                                RecordCard(
                                    record = record,
                                    onEdit = { onEditRecord(record.id) },
                                    onDelete = { onDeleteRecord(record.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCard(
    record: BloodPressureRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val level = bloodPressureLevel(record.systolic, record.diastolic)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(level.color(), CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.systolic}/${record.diastolic} mmHg  ·  ${record.pulse} bpm",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = DateUtils.formatDateTime(record.recordedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = record.timeSlot.displayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (record.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.record_list_cd_edit),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.record_list_cd_delete),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun TimeSlot.displayName(): String = when (this) {
    TimeSlot.MORNING -> stringResource(R.string.record_list_time_slot_morning)
    TimeSlot.AFTERNOON -> stringResource(R.string.record_list_time_slot_afternoon)
    TimeSlot.EVENING -> stringResource(R.string.record_list_time_slot_evening)
    TimeSlot.NIGHT -> stringResource(R.string.record_list_time_slot_night)
}

@Preview(showBackground = true, name = "RecordList - With Records")
@Composable
private fun RecordListWithRecordsPreview() {
    BloodPressureLogTheme {
        RecordListContent(
            uiState = RecordListUiState(
                isLoading = false,
                records = listOf(
                    BloodPressureRecord(
                        id = "1",
                        systolic = 115,
                        diastolic = 75,
                        pulse = 68,
                        note = "",
                        recordedAt = System.currentTimeMillis(),
                    ),
                    BloodPressureRecord(
                        id = "2",
                        systolic = 132,
                        diastolic = 84,
                        pulse = 72,
                        note = "運動後量測",
                        recordedAt = System.currentTimeMillis() - 3_600_000,
                    ),
                    BloodPressureRecord(
                        id = "3",
                        systolic = 145,
                        diastolic = 92,
                        pulse = 80,
                        note = "",
                        recordedAt = System.currentTimeMillis() - 86_400_000,
                    ),
                ),
            ),
            onSignOut = {},
            onEditRecord = {},
            onDeleteRecord = {},
        )
    }
}

@Preview(showBackground = true, name = "RecordList - Empty")
@Composable
private fun RecordListEmptyPreview() {
    BloodPressureLogTheme {
        RecordListContent(
            uiState = RecordListUiState(isLoading = false, records = emptyList()),
            onSignOut = {},
            onEditRecord = {},
            onDeleteRecord = {},
        )
    }
}

@Preview(showBackground = true, name = "RecordList - Loading")
@Composable
private fun RecordListLoadingPreview() {
    BloodPressureLogTheme {
        RecordListContent(
            uiState = RecordListUiState(isLoading = true),
            onSignOut = {},
            onEditRecord = {},
            onDeleteRecord = {},
        )
    }
}
