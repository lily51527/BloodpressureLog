package idv.wennyli.bloodpressurelog.ui.view.trends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import idv.wennyli.bloodpressurelog.R
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import idv.wennyli.bloodpressurelog.ui.theme.BloodPressureLogTheme

@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    TrendsContent(
        uiState = uiState,
        onRangeChange = viewModel::onRangeChange,
        onMetricChange = viewModel::onMetricChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TrendsContent(
    uiState: TrendsUiState,
    onRangeChange: (TrendRange) -> Unit,
    onMetricChange: (TrendMetric) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.trends_screen_title)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrendRange.entries.forEach { range ->
                    FilterChip(
                        selected = uiState.selectedRange == range,
                        onClick = { onRangeChange(range) },
                        label = { Text(range.displayLabel()) },
                    )
                }
            }

            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrendMetric.entries.forEach { metric ->
                    FilterChip(
                        selected = uiState.selectedMetric == metric,
                        onClick = { onMetricChange(metric) },
                        label = { Text(metric.displayLabel()) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                uiState.isEmpty -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.trends_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    TrendChart(
                        chartPoints = uiState.chartPoints,
                        xLabels = uiState.xLabels,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(horizontal = 8.dp),
                    )
                }
            }

            if (uiState.selectedMetric == TrendMetric.SYSTOLIC && !uiState.isEmpty && !uiState.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                SystolicThresholdLegend(modifier = Modifier.padding(horizontal = 16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 趨勢折線圖元件，使用 Vico 圖表庫繪製。
 *
 * @param chartPoints 圖表資料點清單，由 [BuildChartDataUseCase] 產生。
 *                    first 為 x 軸值（相對於起始日的 dayIndex），second 為 y 軸值（指標平均值）。
 * @param xLabels x 軸標籤清單（格式：M/d），索引對應 dayIndex，長度等於所選時間範圍的天數。
 */
@Composable
private fun TrendChart(
    chartPoints: List<Pair<Float, Float>>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    // Vico 的資料橋接器，負責將資料傳遞給圖表層。
    // 使用 remember 確保在 recomposition 之間保持同一個實例，避免重複建立。
    val modelProducer = remember { CartesianChartModelProducer() }

    // 當 chartPoints 變更時（切換時間範圍或指標），以 transaction 方式更新圖表資料。
    // runTransaction 為非同步操作，Vico 會在資料準備就緒後自動觸發重繪。
    LaunchedEffect(chartPoints) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = chartPoints.map { it.first.toDouble() },
                    y = chartPoints.map { it.second.toDouble() },
                )
            }
        }
    }

    // 將 Vico 傳入的 x 軸數值（dayIndex）對應回 xLabels 中的日期字串。
    // 使用 remember(xLabels) 讓 formatter 在 xLabels 更新時同步重建。
    // 注意：Vico 要求回傳值不可為空字串，fallback 使用 index.toString() 而非空白。
    val valueFormatter = remember(xLabels) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt()
            xLabels.getOrElse(index) { index.toString() }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),                // 折線圖層
            startAxis = VerticalAxis.rememberStart(),    // 左側 y 軸
            bottomAxis = HorizontalAxis.rememberBottom(  // 底部 x 軸，套用日期標籤
                valueFormatter = valueFormatter,
            ),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}

@Composable
private fun SystolicThresholdLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.trends_systolic_legend_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        listOf(
            Triple("< 120", stringResource(R.string.trends_bp_level_normal), Color(0xFF4CAF50)),
            Triple("120–129", stringResource(R.string.trends_bp_level_elevated), Color(0xFFFFEB3B)),
            Triple("130–139", stringResource(R.string.trends_bp_level_high_stage_1), Color(0xFFFF9800)),
            Triple("≥ 140", stringResource(R.string.trends_bp_level_high_stage_2), Color(0xFFF44336)),
        ).forEach { (range, label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .padding(0.dp),
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(color = color)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$range mmHg　$label",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Trends - With Data")
@Composable
private fun TrendsWithDataPreview() {
    BloodPressureLogTheme {
        TrendsContent(
            uiState = TrendsUiState(
                isLoading = false,
                isEmpty = false,
                chartPoints = listOf(0f to 125f, 1f to 130f, 3f to 118f, 5f to 135f, 6f to 122f),
                xLabels = listOf("4/27", "4/28", "4/29", "4/30", "5/1", "5/2", "5/3"),
                selectedRange = TrendRange.DAYS_7,
                selectedMetric = TrendMetric.SYSTOLIC,
            ),
            onRangeChange = {},
            onMetricChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Trends - Empty")
@Composable
private fun TrendsEmptyPreview() {
    BloodPressureLogTheme {
        TrendsContent(
            uiState = TrendsUiState(isLoading = false, isEmpty = true),
            onRangeChange = {},
            onMetricChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Trends - Loading")
@Composable
private fun TrendsLoadingPreview() {
    BloodPressureLogTheme {
        TrendsContent(
            uiState = TrendsUiState(isLoading = true),
            onRangeChange = {},
            onMetricChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Trends - Error")
@Composable
private fun TrendsErrorPreview() {
    BloodPressureLogTheme {
        TrendsContent(
            uiState = TrendsUiState(isLoading = false, errorMessage = "載入失敗"),
            onRangeChange = {},
            onMetricChange = {},
        )
    }
}

@Composable
private fun TrendRange.displayLabel(): String = stringResource(this.labelRes)

@Composable
private fun TrendMetric.displayLabel(): String = stringResource(this.labelRes)
