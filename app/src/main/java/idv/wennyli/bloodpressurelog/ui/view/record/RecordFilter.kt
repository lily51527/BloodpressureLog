package idv.wennyli.bloodpressurelog.ui.view.record

import idv.wennyli.bloodpressurelog.utils.DateUtils

data class RecordFilter(
    val startMs: Long,
    val endMs: Long,
) {
    companion object {
        fun default(): RecordFilter = RecordFilter(
            startMs = DateUtils.startOfDay(daysAgo = 29),
            endMs = DateUtils.endOfDay(daysAgo = 0),
        )
    }
}
