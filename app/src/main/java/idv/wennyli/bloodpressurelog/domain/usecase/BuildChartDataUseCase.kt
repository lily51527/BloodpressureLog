package idv.wennyli.bloodpressurelog.domain.usecase

import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.ui.view.trends.TrendMetric
import idv.wennyli.bloodpressurelog.ui.view.trends.TrendRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 將血壓紀錄轉換為趨勢圖表所需資料的 UseCase。
 *
 * 負責以下業務邏輯：
 * - 依指定時間範圍（[TrendRange]）過濾紀錄
 * - 將同一天的多筆紀錄計算日平均值
 * - 將日期轉換為相對於起始日的 x 軸索引（dayIndex）
 * - 產生對應範圍天數的 x 軸日期標籤（格式：M/d）
 *
 * @return [Pair]，first 為圖表資料點清單（x = dayIndex, y = 指標平均值），
 *         second 為 x 軸標籤清單（長度等於 [TrendRange.days]）
 */
class BuildChartDataUseCase @Inject constructor() {

    /**
     * 執行圖表資料轉換。
     *
     * @param records 從 Repository 取得的血壓紀錄清單
     * @param range 要顯示的時間範圍（7 / 14 / 30 天）
     * @param metric 要顯示的指標（收縮壓 / 舒張壓 / 脈搏）
     * @return 圖表資料點與 x 軸標籤的配對
     */
    operator fun invoke(
        records: List<BloodPressureRecord>,
        range: TrendRange,
        metric: TrendMetric,
    ): Pair<List<Pair<Float, Float>>, List<String>> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startDate = today.minusDays((range.days - 1).toLong())
        val formatter = DateTimeFormatter.ofPattern("M/d")

        val xLabels = (0 until range.days).map { i ->
            startDate.plusDays(i.toLong()).format(formatter)
        }

        val byDay = records
            .groupBy { Instant.ofEpochMilli(it.recordedAt).atZone(zone).toLocalDate() }
            .filterKeys { date -> !date.isBefore(startDate) && !date.isAfter(today) }

        val points = byDay.entries
            .sortedBy { it.key }
            .mapNotNull { (date, dayRecords) ->
                val dayIndex = (date.toEpochDay() - startDate.toEpochDay()).toFloat()
                val avg = when (metric) {
                    TrendMetric.SYSTOLIC -> dayRecords.map { it.systolic }.average()
                    TrendMetric.DIASTOLIC -> dayRecords.map { it.diastolic }.average()
                    TrendMetric.PULSE -> dayRecords.map { it.pulse }.average()
                }
                if (avg.isNaN()) null else dayIndex to avg.toFloat()
            }

        return points to xLabels
    }
}
