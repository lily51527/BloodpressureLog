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

    // ── 邊界精確值 ────────────────────────────────────────────────────────────

    /** systolic=139 剛好不到第二期門檻（140），舒張壓也未達 80，應為第一期高血壓。 */
    @Test
    fun `high stage 1 when systolic is 139 and diastolic is 79`() {
        val result = bloodPressureLevel(139, 79)

        assertThat(result).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
    }

    /** systolic=140 剛好跨過第二期收縮壓門檻，應為第二期高血壓。 */
    @Test
    fun `high stage 2 when systolic is exactly 140 and diastolic is 79`() {
        val result = bloodPressureLevel(140, 79)

        assertThat(result).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
    }

    /** diastolic=89 剛好不到第二期門檻（90），收縮壓也未達 120，應為第一期高血壓。 */
    @Test
    fun `high stage 1 when diastolic is 89 and systolic is 119`() {
        val result = bloodPressureLevel(119, 89)

        assertThat(result).isEqualTo(BloodPressureLevel.HIGH_STAGE_1)
    }

    /** diastolic=90 剛好跨過第二期舒張壓門檻，應為第二期高血壓。 */
    @Test
    fun `high stage 2 when diastolic is exactly 90 and systolic is 119`() {
        val result = bloodPressureLevel(119, 90)

        assertThat(result).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
    }

    // ── 只有一側條件達標（優先級組合） ─────────────────────────────────────────

    /** 舒張壓達 90 但收縮壓僅 139（未達第二期），OR 邏輯應以舒張壓達標為準，回傳第二期高血壓。 */
    @Test
    fun `high stage 2 when only diastolic meets stage 2 threshold`() {
        val result = bloodPressureLevel(139, 90)

        assertThat(result).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
    }

    // ── 無效／極端輸入（記錄實際行為） ──────────────────────────────────────────

    /** systolic=0、diastolic=0 不滿足任何高血壓條件，實際回傳 NORMAL。 */
    @Test
    fun `returns normal when both systolic and diastolic are zero`() {
        val result = bloodPressureLevel(0, 0)

        assertThat(result).isEqualTo(BloodPressureLevel.NORMAL)
    }

    /** systolic=-1、diastolic=-1 不滿足任何高血壓條件，實際回傳 NORMAL。 */
    @Test
    fun `returns normal when both systolic and diastolic are negative`() {
        val result = bloodPressureLevel(-1, -1)

        assertThat(result).isEqualTo(BloodPressureLevel.NORMAL)
    }

    /** systolic=999、diastolic=999 超過所有門檻，應落入最高等級第二期高血壓。 */
    @Test
    fun `high stage 2 when both values are extremely large`() {
        val result = bloodPressureLevel(999, 999)

        assertThat(result).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
    }

    /** systolic=Int.MAX_VALUE、diastolic=Int.MAX_VALUE 為整數最大值，應落入第二期高血壓。 */
    @Test
    fun `high stage 2 when both values are Int MAX_VALUE`() {
        val result = bloodPressureLevel(Int.MAX_VALUE, Int.MAX_VALUE)

        assertThat(result).isEqualTo(BloodPressureLevel.HIGH_STAGE_2)
    }
}
