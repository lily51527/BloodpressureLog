package idv.wennyli.bloodpressurelog.ui.view.trends

import androidx.annotation.StringRes
import idv.wennyli.bloodpressurelog.R

enum class TrendRange(val days: Int, @StringRes val labelRes: Int) {
    DAYS_7(7, R.string.trends_range_7_days),
    DAYS_14(14, R.string.trends_range_14_days),
    DAYS_30(30, R.string.trends_range_30_days),
}
