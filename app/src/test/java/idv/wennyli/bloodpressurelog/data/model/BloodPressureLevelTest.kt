package idv.wennyli.bloodpressurelog.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BloodPressureLevelTest {

    @Test
    fun `normal when systolic below 120 and diastolic below 80`() {
        assertEquals(BloodPressureLevel.NORMAL, bloodPressureLevel(119, 79))
        assertEquals(BloodPressureLevel.NORMAL, bloodPressureLevel(100, 60))
        assertEquals(BloodPressureLevel.NORMAL, bloodPressureLevel(115, 75))
    }

    @Test
    fun `elevated when systolic 120 to 129 and diastolic below 80`() {
        assertEquals(BloodPressureLevel.ELEVATED, bloodPressureLevel(120, 79))
        assertEquals(BloodPressureLevel.ELEVATED, bloodPressureLevel(125, 70))
        assertEquals(BloodPressureLevel.ELEVATED, bloodPressureLevel(129, 79))
    }

    @Test
    fun `not elevated when diastolic is 80 or above even if systolic is 120 to 129`() {
        assertEquals(BloodPressureLevel.HIGH_STAGE_1, bloodPressureLevel(125, 80))
    }

    @Test
    fun `high stage 1 when systolic 130 to 139 regardless of diastolic`() {
        assertEquals(BloodPressureLevel.HIGH_STAGE_1, bloodPressureLevel(130, 70))
        assertEquals(BloodPressureLevel.HIGH_STAGE_1, bloodPressureLevel(139, 79))
    }

    @Test
    fun `high stage 1 when diastolic 80 to 89 regardless of systolic`() {
        assertEquals(BloodPressureLevel.HIGH_STAGE_1, bloodPressureLevel(110, 80))
        assertEquals(BloodPressureLevel.HIGH_STAGE_1, bloodPressureLevel(115, 89))
    }

    @Test
    fun `high stage 2 when systolic 140 or above`() {
        assertEquals(BloodPressureLevel.HIGH_STAGE_2, bloodPressureLevel(140, 70))
        assertEquals(BloodPressureLevel.HIGH_STAGE_2, bloodPressureLevel(180, 90))
    }

    @Test
    fun `high stage 2 when diastolic 90 or above`() {
        assertEquals(BloodPressureLevel.HIGH_STAGE_2, bloodPressureLevel(110, 90))
        assertEquals(BloodPressureLevel.HIGH_STAGE_2, bloodPressureLevel(120, 100))
    }

    @Test
    fun `high stage 2 takes precedence over stage 1 boundary`() {
        assertEquals(BloodPressureLevel.HIGH_STAGE_2, bloodPressureLevel(140, 90))
    }
}
