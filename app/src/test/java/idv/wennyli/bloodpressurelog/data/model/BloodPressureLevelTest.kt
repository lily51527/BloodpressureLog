package idv.wennyli.bloodpressurelog.data.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class BloodPressureLevelTest {

    /** 收縮壓未達 120 且舒張壓未達 80 時，血壓等級應為正常。 */
    @Test
    fun `normal when systolic below 120 and diastolic below 80`() {
        assertThat(bloodPressureLevel(119, 79)).isEqualTo(BloodPressureLevel.NORMAL)
        assertThat(bloodPressureLevel(100, 60)).isEqualTo(BloodPressureLevel.NORMAL)
        assertThat(bloodPressureLevel(115, 75)).isEqualTo(BloodPressureLevel.NORMAL)
    }

    /** 收縮壓 120–129 且舒張壓未達 80 時，血壓等級應為正常偏高。 */
    @Test
    fun `elevated when systolic 120 to 129 and diastolic below 80`() {
        assertThat(bloodPressureLevel(120, 79)).isEqualTo(BloodPressureLevel.ELEVATED)
        assertThat(bloodPressureLevel(125, 70)).isEqualTo(BloodPressureLevel.ELEVATED)
        assertThat(bloodPressureLevel(129, 79)).isEqualTo(BloodPressureLevel.ELEVATED)
    }

    /** 收縮壓在正常偏高範圍但舒張壓達 80 時，等級應升至第一期高血壓而非正常偏高。 */
    @Test
    fun `not elevated when diastolic is 80 or above even if systolic is 120 to 129`() {
        assertThat(bloodPressureLevel(125, 80)).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
    }

    /** 收縮壓 130–139 時，無論舒張壓為何，血壓等級應為第一期高血壓。 */
    @Test
    fun `high stage 1 when systolic 130 to 139 regardless of diastolic`() {
        assertThat(bloodPressureLevel(130, 70)).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
        assertThat(bloodPressureLevel(139, 79)).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
    }

    /** 舒張壓 80–89 時，無論收縮壓為何，血壓等級應為第一期高血壓。 */
    @Test
    fun `high stage 1 when diastolic 80 to 89 regardless of systolic`() {
        assertThat(bloodPressureLevel(110, 80)).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
        assertThat(bloodPressureLevel(115, 89)).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
    }

    /** 收縮壓達 140 或以上時，血壓等級應為第二期高血壓。 */
    @Test
    fun `high stage 2 when systolic 140 or above`() {
        assertThat(bloodPressureLevel(140, 70)).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
        assertThat(bloodPressureLevel(180, 90)).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
    }

    /** 舒張壓達 90 或以上時，血壓等級應為第二期高血壓。 */
    @Test
    fun `high stage 2 when diastolic 90 or above`() {
        assertThat(bloodPressureLevel(110, 90)).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
        assertThat(bloodPressureLevel(120, 100)).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
    }

    /** 同時符合第一期與第二期條件時，應以第二期高血壓為最終等級。 */
    @Test
    fun `high stage 2 takes precedence over stage 1 boundary`() {
        assertThat(bloodPressureLevel(140, 90)).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
    }
}
