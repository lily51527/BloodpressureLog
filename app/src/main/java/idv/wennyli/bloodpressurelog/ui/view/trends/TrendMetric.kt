package idv.wennyli.bloodpressurelog.ui.view.trends

import androidx.annotation.StringRes
import idv.wennyli.bloodpressurelog.R

enum class TrendMetric(@StringRes val labelRes: Int) {
    SYSTOLIC(R.string.trend_metric_systolic),
    DIASTOLIC(R.string.trend_metric_diastolic),
    PULSE(R.string.trend_metric_pulse),
}
