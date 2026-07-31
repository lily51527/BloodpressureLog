package idv.wennyli.bloodpressurelog.domain.usecase

import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.ui.view.trends.TrendMetric
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * 將血壓紀錄轉換為趨勢圖表所需資料的 UseCase。
 *
 * 負責以下業務邏輯：
 * - 依指定日期區間（startMs ~ endMs）過濾紀錄
 * - 將同一天的多筆紀錄計算日平均值
 * - 將日期轉換為相對於起始日的 x 軸索引（dayIndex）
 * - 產生對應區間天數的 x 軸日期標籤（格式：M/d）
 *
 * @return [Pair]，first 為圖表資料點清單（x = dayIndex, y = 指標平均值），
 *         second 為 x 軸標籤清單（長度等於區間天數）
 */
class BuildChartDataUseCase @Inject constructor() {

    /**
     * 執行圖表資料轉換。
     *
     * @param records 從 Repository 取得的血壓紀錄清單
     * @param startMs 篩選區間的開始時間（epoch millis）
     * @param endMs 篩選區間的結束時間（epoch millis）
     * @param metric 要顯示的指標（收縮壓 / 舒張壓 / 脈搏）
     * @return 圖表資料點與 x 軸標籤的配對；若 endMs 早於 startMs 則回傳空結果
     */
    operator fun invoke(
        records: List<BloodPressureRecord>,
        startMs: Long,
        endMs: Long,
        metric: TrendMetric,
    ): Pair<List<Pair<Float, Float>>, List<String>> {
        val zone = ZoneId.systemDefault() //時區
        val startDate = Instant.ofEpochMilli(startMs).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochMilli(endMs).atZone(zone).toLocalDate()

        if (endDate.isBefore(startDate)) return emptyList<Pair<Float, Float>>() to emptyList()

        val formatter = DateTimeFormatter.ofPattern("M/d")
        val days = (ChronoUnit.DAYS.between(startDate, endDate) + 1).toInt()

        val xLabels = (0 until days).map { i ->
            startDate.plusDays(i.toLong()).format(formatter)
        }

        val byDay = records
            .groupBy { Instant.ofEpochMilli(it.recordedAt).atZone(zone).toLocalDate() } // 把 records 這個 List 按「同一天」分組，結果是一個 Map<LocalDate, List<BloodPressureRecord>>
            .filterKeys { date -> !date.isBefore(startDate) && !date.isAfter(endDate) }

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
