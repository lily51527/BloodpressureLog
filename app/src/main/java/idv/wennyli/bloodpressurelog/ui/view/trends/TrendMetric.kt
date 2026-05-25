package idv.wennyli.bloodpressurelog.ui.view.trends

import androidx.annotation.StringRes
import idv.wennyli.bloodpressurelog.R

enum class TrendMetric(@StringRes val labelRes: Int) {
    SYSTOLIC(R.string.trends_metric_systolic),
    DIASTOLIC(R.string.trends_metric_diastolic),
    PULSE(R.string.trends_metric_pulse),
}
