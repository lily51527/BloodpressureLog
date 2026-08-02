package idv.wennyli.bloodpressurelog.ui.view.trends

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import idv.wennyli.bloodpressurelog.ui.common.DateRangeFilter
import idv.wennyli.bloodpressurelog.utils.DateUtils
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrendsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingState_hidesEmptyErrorAndLegendText() {
        composeTestRule.setContent {
            TrendsContent(
                uiState = TrendsUiState(isLoading = true),
                onDateRangeConfirmed = { _, _ -> },
                onMetricChange = {},
            )
        }

        composeTestRule.onNodeWithText("此區間無紀錄").assertDoesNotExist()
        composeTestRule.onNodeWithText("收縮壓警戒標準（WHO）").assertDoesNotExist()
    }

    @Test
    fun errorState_showsErrorMessage() {
        // 已知邊界情況：errorMessage != null 但 isEmpty 預設為 false 時，
        // 圖例的顯示條件 `!isEmpty && !isLoading` 仍成立，圖例會跟著錯誤訊息一起顯示。
        // 這裡如實鎖定現況，是否修正為「Error 時一律隱藏圖例」留待日後另行決定。
        composeTestRule.setContent {
            TrendsContent(
                uiState = TrendsUiState(isLoading = false, errorMessage = "載入失敗"),
                onDateRangeConfirmed = { _, _ -> },
                onMetricChange = {},
            )
        }

        composeTestRule.onNodeWithText("載入失敗").assertIsDisplayed()
        composeTestRule.onNodeWithText("收縮壓警戒標準（WHO）").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsEmptyMessageAndNoLegend() {
        composeTestRule.setContent {
            TrendsContent(
                uiState = TrendsUiState(isLoading = false, isEmpty = true),
                onDateRangeConfirmed = { _, _ -> },
                onMetricChange = {},
            )
        }

        composeTestRule.onNodeWithText("此區間無紀錄").assertIsDisplayed()
        composeTestRule.onNodeWithText("收縮壓警戒標準（WHO）").assertDoesNotExist()
    }

    @Test
    fun successSystolic_showsSystolicLegendWithAllFourRows() {
        composeTestRule.setContent {
            TrendsContent(
                uiState = TrendsUiState(
                    isLoading = false,
                    isEmpty = false,
                    selectedMetric = TrendMetric.SYSTOLIC,
                    chartPoints = listOf(0f to 120f, 1f to 130f),
                    xLabels = listOf("1/1", "1/2"),
                ),
                onDateRangeConfirmed = { _, _ -> },
                onMetricChange = {},
            )
        }

        composeTestRule.onNodeWithText("收縮壓警戒標準（WHO）").assertIsDisplayed()
        composeTestRule.onNodeWithText("< 120 mmHg　正常").assertIsDisplayed()
        composeTestRule.onNodeWithText("120–129 mmHg　正常偏高").assertIsDisplayed()
        composeTestRule.onNodeWithText("130–139 mmHg　第一期高血壓").assertIsDisplayed()
        composeTestRule.onNodeWithText("≥ 140 mmHg　第二期高血壓").assertIsDisplayed()
    }

    @Test
    fun successDiastolic_showsDiastolicLegendWithThreeRows() {
        composeTestRule.setContent {
            TrendsContent(
                uiState = TrendsUiState(
                    isLoading = false,
                    isEmpty = false,
                    selectedMetric = TrendMetric.DIASTOLIC,
                    chartPoints = listOf(0f to 78f, 1f to 85f),
                    xLabels = listOf("1/1", "1/2"),
                ),
                onDateRangeConfirmed = { _, _ -> },
                onMetricChange = {},
            )
        }

        composeTestRule.onNodeWithText("舒張壓警戒標準（WHO）").assertIsDisplayed()
        composeTestRule.onNodeWithText("< 80 mmHg　正常").assertIsDisplayed()
        composeTestRule.onNodeWithText("80–89 mmHg　第一期高血壓").assertIsDisplayed()
        composeTestRule.onNodeWithText("≥ 90 mmHg　第二期高血壓").assertIsDisplayed()
    }

    @Test
    fun successPulse_showsNoLegend() {
        composeTestRule.setContent {
            TrendsContent(
                uiState = TrendsUiState(
                    isLoading = false,
                    isEmpty = false,
                    selectedMetric = TrendMetric.PULSE,
                    chartPoints = listOf(0f to 70f, 1f to 72f),
                    xLabels = listOf("1/1", "1/2"),
                ),
                onDateRangeConfirmed = { _, _ -> },
                onMetricChange = {},
            )
        }

        composeTestRule.onNodeWithText("收縮壓警戒標準（WHO）").assertDoesNotExist()
        composeTestRule.onNodeWithText("舒張壓警戒標準（WHO）").assertDoesNotExist()
    }

    @Test
    fun metricChips_renderWithCorrectLabels() {
        composeTestRule.setContent {
            TrendsContent(
                uiState = TrendsUiState(isLoading = true),
                onDateRangeConfirmed = { _, _ -> },
                onMetricChange = {},
            )
        }

        composeTestRule.onNodeWithText("收縮壓").assertIsDisplayed()
        composeTestRule.onNodeWithText("舒張壓").assertIsDisplayed()
        composeTestRule.onNodeWithText("脈搏").assertIsDisplayed()
    }

    @Test
    fun clickingSystolicChip_invokesOnMetricChangeWithSystolic() {
        var changedMetric: TrendMetric? = null

        composeTestRule.setContent {
            TrendsContent(
                uiState = TrendsUiState(isLoading = true, selectedMetric = TrendMetric.DIASTOLIC),
                onDateRangeConfirmed = { _, _ -> },
                onMetricChange = { changedMetric = it },
            )
        }

        composeTestRule.onNodeWithText("收縮壓").performClick()

        assert(changedMetric == TrendMetric.SYSTOLIC)
    }

    @Test
    fun clickingDiastolicChip_invokesOnMetricChangeWithDiastolic() {
        var changedMetric: TrendMetric? = null

        composeTestRule.setContent {
            TrendsContent(
                uiState = TrendsUiState(isLoading = true, selectedMetric = TrendMetric.SYSTOLIC),
                onDateRangeConfirmed = { _, _ -> },
                onMetricChange = { changedMetric = it },
            )
        }

        composeTestRule.onNodeWithText("舒張壓").performClick()

        assert(changedMetric == TrendMetric.DIASTOLIC)
    }

    @Test
    fun clickingPulseChip_invokesOnMetricChangeWithPulse() {
        var changedMetric: TrendMetric? = null

        composeTestRule.setContent {
            TrendsContent(
                uiState = TrendsUiState(isLoading = true, selectedMetric = TrendMetric.SYSTOLIC),
                onDateRangeConfirmed = { _, _ -> },
                onMetricChange = { changedMetric = it },
            )
        }

        composeTestRule.onNodeWithText("脈搏").performClick()

        assert(changedMetric == TrendMetric.PULSE)
    }

    @Test
    fun dateRangeButtons_showFormattedDatesFromUiState() {
        val startMs = 1_700_000_000_000L
        val endMs = 1_700_500_000_000L

        composeTestRule.setContent {
            TrendsContent(
                uiState = TrendsUiState(
                    isLoading = true,
                    dateFilter = DateRangeFilter(startMs = startMs, endMs = endMs),
                ),
                onDateRangeConfirmed = { _, _ -> },
                onMetricChange = {},
            )
        }

        composeTestRule.onNodeWithText(DateUtils.formatDate(startMs)).assertIsDisplayed()
        composeTestRule.onNodeWithText(DateUtils.formatDate(endMs)).assertIsDisplayed()
    }
}
