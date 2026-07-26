package idv.wennyli.bloodpressurelog.ui.common

import idv.wennyli.bloodpressurelog.utils.DateUtils

data class DateRangeFilter(
    val startMs: Long,
    val endMs: Long,
) {
    companion object {
        fun default(): DateRangeFilter = DateRangeFilter(
            startMs = DateUtils.startOfDay(daysAgo = 29),
            endMs = DateUtils.endOfDay(daysAgo = 0),
        )
    }
}
