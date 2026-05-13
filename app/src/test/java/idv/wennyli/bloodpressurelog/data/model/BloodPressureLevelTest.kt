package idv.wennyli.bloodpressurelog.data.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class BloodPressureLevelTest {

    @Test
    fun `normal when systolic below 120 and diastolic below 80`() {
        assertThat(bloodPressureLevel(119, 79)).isEqualTo(BloodPressureLevel.NORMAL)
        assertThat(bloodPressureLevel(100, 60)).isEqualTo(BloodPressureLevel.NORMAL)
        assertThat(bloodPressureLevel(115, 75)).isEqualTo(BloodPressureLevel.NORMAL)
    }

    @Test
    fun `elevated when systolic 120 to 129 and diastolic below 80`() {
        assertThat(bloodPressureLevel(120, 79)).isEqualTo(BloodPressureLevel.ELEVATED)
        assertThat(bloodPressureLevel(125, 70)).isEqualTo(BloodPressureLevel.ELEVATED)
        assertThat(bloodPressureLevel(129, 79)).isEqualTo(BloodPressureLevel.ELEVATED)
    }

    @Test
    fun `not elevated when diastolic is 80 or above even if systolic is 120 to 129`() {
        assertThat(bloodPressureLevel(125, 80)).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
    }

    @Test
    fun `high stage 1 when systolic 130 to 139 regardless of diastolic`() {
        assertThat(bloodPressureLevel(130, 70)).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
        assertThat(bloodPressureLevel(139, 79)).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
    }

    @Test
    fun `high stage 1 when diastolic 80 to 89 regardless of systolic`() {
        assertThat(bloodPressureLevel(110, 80)).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
        assertThat(bloodPressureLevel(115, 89)).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
    }

    @Test
    fun `high stage 2 when systolic 140 or above`() {
        assertThat(bloodPressureLevel(140, 70)).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
        assertThat(bloodPressureLevel(180, 90)).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
    }

    @Test
    fun `high stage 2 when diastolic 90 or above`() {
        assertThat(bloodPressureLevel(110, 90)).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
        assertThat(bloodPressureLevel(120, 100)).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
    }

    @Test
    fun `high stage 2 takes precedence over stage 1 boundary`() {
        assertThat(bloodPressureLevel(140, 90)).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
    }
}
