package idv.wennyli.bloodpressurelog.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FirestorePathsTest {

    @Test
    fun `bloodPressures returns correct path`() {
        val path = FirestorePaths.bloodPressures("bloodpressurelog_debug", "user123")
        assertEquals("artifacts/bloodpressurelog_debug/users/user123/bloodPressures", path)
    }

    @Test
    fun `bloodPressures with release appId returns correct path`() {
        val path = FirestorePaths.bloodPressures("bloodpressurelog", "abc-xyz")
        assertEquals("artifacts/bloodpressurelog/users/abc-xyz/bloodPressures", path)
    }

    @Test
    fun `bloodPressures isolates debug and release data`() {
        val debugPath = FirestorePaths.bloodPressures("bloodpressurelog_debug", "uid1")
        val releasePath = FirestorePaths.bloodPressures("bloodpressurelog", "uid1")
        assert(debugPath != releasePath)
    }
}
