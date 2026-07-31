package idv.wennyli.bloodpressurelog.ui.common

import androidx.compose.ui.graphics.Color
import idv.wennyli.bloodpressurelog.data.model.BloodPressureLevel

fun BloodPressureLevel.color(): Color = when (this) {
    BloodPressureLevel.NORMAL -> Color(0xFF4CAF50)
    BloodPressureLevel.ELEVATED -> Color(0xFFFFEB3B)
    BloodPressureLevel.HIGH_STAGE_1 -> Color(0xFFFF9800)
    BloodPressureLevel.HIGH_STAGE_2 -> Color(0xFFF44336)
}
