package idv.wennyli.bloodpressurelog.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.ui.theme.BloodPressureLogTheme
import idv.wennyli.bloodpressurelog.utils.DateUtils

private enum class DatePickerTarget { NONE, START, END }

/**
 * 共用的日期區間篩選列，供 RecordListScreen 與 TrendsScreen 共用，確保 UI 與驗證邏輯一致。
 *
 * 開始日期不得晚於結束日期、結束日期不得早於開始日期，透過 [SelectableDates] 在選擇當下即擋掉不合法組合。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilterBar(
    startMs: Long,
    endMs: Long,
    onDateRangeConfirmed: (startMs: Long, endMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingStartUtcMs by remember(startMs, endMs) {
        mutableLongStateOf(DateUtils.toUtcMidnightMillis(startMs))
    }
    var pendingEndUtcMs by remember(startMs, endMs) {
        mutableLongStateOf(DateUtils.toUtcMidnightMillis(endMs))
    }
    var showPicker by remember { mutableStateOf(DatePickerTarget.NONE) }

    when (showPicker) {
        DatePickerTarget.START -> {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = pendingStartUtcMs,
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        utcTimeMillis <= pendingEndUtcMs
                },
            )
            DatePickerDialog(
                onDismissRequest = { showPicker = DatePickerTarget.NONE },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { pendingStartUtcMs = it }
                        showPicker = DatePickerTarget.NONE
                    }) { Text(stringResource(R.string.common_button_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showPicker = DatePickerTarget.NONE }) {
                        Text(stringResource(R.string.common_button_cancel))
                    }
                },
            ) { DatePicker(state = state) }
        }

        DatePickerTarget.END -> {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = pendingEndUtcMs,
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        utcTimeMillis >= pendingStartUtcMs
                },
            )
            DatePickerDialog(
                onDismissRequest = { showPicker = DatePickerTarget.NONE },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { pendingEndUtcMs = it }
                        showPicker = DatePickerTarget.NONE
                    }) { Text(stringResource(R.string.common_button_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showPicker = DatePickerTarget.NONE }) {
                        Text(stringResource(R.string.common_button_cancel))
                    }
                },
            ) { DatePicker(state = state) }
        }

        DatePickerTarget.NONE -> Unit
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = { showPicker = DatePickerTarget.START },
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = DateUtils.formatDate(DateUtils.utcMidnightToStartOfDay(pendingStartUtcMs)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        OutlinedButton(
            onClick = { showPicker = DatePickerTarget.END },
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = DateUtils.formatDate(DateUtils.utcMidnightToStartOfDay(pendingEndUtcMs)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        IconButton(onClick = {
            onDateRangeConfirmed(
                DateUtils.utcMidnightToStartOfDay(pendingStartUtcMs),
                DateUtils.utcMidnightToEndOfDay(pendingEndUtcMs),
            )
        }) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.record_list_cd_search),
            )
        }
    }
}

@Preview(showBackground = true, name = "DateRangeFilterBar - Last 30 Days")
@Composable
private fun DateRangeFilterBarPreview() {
    BloodPressureLogTheme {
        DateRangeFilterBar(
            startMs = DateUtils.startOfDay(daysAgo = 29),
            endMs = DateUtils.endOfDay(daysAgo = 0),
            onDateRangeConfirmed = { _, _ -> },
        )
    }
}
