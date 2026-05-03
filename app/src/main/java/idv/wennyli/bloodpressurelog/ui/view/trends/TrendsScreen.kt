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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
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
            TopAppBar(title = { Text("趨勢") })
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
                        label = { Text(range.label) },
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
                        label = { Text(metric.label) },
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
                            text = "此區間無紀錄",
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

@Composable
private fun TrendChart(
    chartPoints: List<Pair<Float, Float>>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

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

    val valueFormatter = CartesianValueFormatter { _, value, _ ->
        xLabels.getOrElse(value.toInt()) { " " }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = valueFormatter),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}

@Composable
private fun SystolicThresholdLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "收縮壓警戒標準（WHO）",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        listOf(
            Triple("< 120", "正常", Color(0xFF4CAF50)),
            Triple("120–129", "正常偏高", Color(0xFFFFEB3B)),
            Triple("130–139", "第一期高血壓", Color(0xFFFF9800)),
            Triple("≥ 140", "第二期高血壓", Color(0xFFF44336)),
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
