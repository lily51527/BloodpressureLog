package idv.wennyli.bloodpressurelog.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test

class FirestorePathsTest {

    /** bloodPressures 應依 appId 與 userId 產生正確的 Firestore 集合路徑。 */
    @Test
    fun `bloodPressures returns correct path`() {
        val path = FirestorePaths.bloodPressures("bloodpressurelog_debug", "user123")
        assertThat(path).isEqualTo("artifacts/bloodpressurelog_debug/users/user123/bloodPressures")
    }

    /** 使用 release appId 時，bloodPressures 應產生對應的正式環境集合路徑。 */
    @Test
    fun `bloodPressures with release appId returns correct path`() {
        val path = FirestorePaths.bloodPressures("bloodpressurelog", "abc-xyz")
        assertThat(path).isEqualTo("artifacts/bloodpressurelog/users/abc-xyz/bloodPressures")
    }

    /** debug 與 release 的 appId 應產生不同路徑，確保兩個環境的資料互相隔離。 */
    @Test
    fun `bloodPressures isolates debug and release data`() {
        val debugPath = FirestorePaths.bloodPressures("bloodpressurelog_debug", "uid1")
        val releasePath = FirestorePaths.bloodPressures("bloodpressurelog", "uid1")
        assertThat(debugPath).isNotEqualTo(releasePath)
    }
}
