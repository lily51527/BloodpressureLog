package idv.wennyli.bloodpressurelog.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test

class FirestorePathsTest {

    @Test
    fun `bloodPressures returns correct path`() {
        val path = FirestorePaths.bloodPressures("bloodpressurelog_debug", "user123")
        assertThat(path).isEqualTo("artifacts/bloodpressurelog_debug/users/user123/bloodPressures")
    }

    @Test
    fun `bloodPressures with release appId returns correct path`() {
        val path = FirestorePaths.bloodPressures("bloodpressurelog", "abc-xyz")
        assertThat(path).isEqualTo("artifacts/bloodpressurelog/users/abc-xyz/bloodPressures")
    }

    @Test
    fun `bloodPressures isolates debug and release data`() {
        val debugPath = FirestorePaths.bloodPressures("bloodpressurelog_debug", "uid1")
        val releasePath = FirestorePaths.bloodPressures("bloodpressurelog", "uid1")
        assertThat(debugPath).isNotEqualTo(releasePath)
    }
}
