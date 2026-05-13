package idv.wennyli.bloodpressurelog.domain.usecase

import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.ui.view.trends.TrendMetric
import idv.wennyli.bloodpressurelog.ui.view.trends.TrendRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class BuildChartDataUseCase @Inject constructor() {

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
