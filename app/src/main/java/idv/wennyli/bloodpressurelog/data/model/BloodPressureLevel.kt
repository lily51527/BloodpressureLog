package idv.wennyli.bloodpressurelog.data.model

enum class BloodPressureLevel {
    NORMAL,       // systolic < 120 AND diastolic < 80
    ELEVATED,     // systolic 120–129 AND diastolic < 80
    HIGH_STAGE_1, // systolic 130–139 OR diastolic 80–89
    HIGH_STAGE_2, // systolic ≥ 140 OR diastolic ≥ 90
}

fun bloodPressureLevel(systolic: Int, diastolic: Int): BloodPressureLevel = when {
    systolic >= 140 || diastolic >= 90 -> BloodPressureLevel.HIGH_STAGE_2
    systolic >= 130 || diastolic >= 80 -> BloodPressureLevel.HIGH_STAGE_1
    systolic >= 120 && diastolic < 80 -> BloodPressureLevel.ELEVATED
    else -> BloodPressureLevel.NORMAL
}
