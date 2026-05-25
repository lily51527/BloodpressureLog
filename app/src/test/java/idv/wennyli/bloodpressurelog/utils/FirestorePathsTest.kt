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

    // -------------------------------------------------------------------------
    // 路徑結構交叉驗證
    // -------------------------------------------------------------------------

    /**
     * 用 split("/") 逐層確認路徑節點的順序與名稱。
     * 防止常數（COLLECTION_ARTIFACTS / COLLECTION_USERS / COLLECTION_BLOOD_PRESSURES）
     * 被改動後，只靠字串比對的測試無法察覺。
     */
    @Test
    fun `bloodPressures path segments are in correct order`() {
        val appId = "bloodpressurelog_debug"
        val userId = "user123"

        val path = FirestorePaths.bloodPressures(appId, userId)
        val segments = path.split("/")

        assertThat(segments[0]).isEqualTo("artifacts")
        assertThat(segments[1]).isEqualTo(appId)
        assertThat(segments[2]).isEqualTo("users")
        assertThat(segments[3]).isEqualTo(userId)
        assertThat(segments[4]).isEqualTo("bloodPressures")
    }

    /** 路徑應恰好由 5 個節點組成，不多不少。 */
    @Test
    fun `bloodPressures path has exactly 5 segments`() {
        val path = FirestorePaths.bloodPressures("myApp", "myUser")

        val segments = path.split("/")

        assertThat(segments.size).isEqualTo(5)
    }

    // -------------------------------------------------------------------------
    // 空字串輸入
    // -------------------------------------------------------------------------

    /**
     * 記錄 appId 為空字串時的實際行為：
     * 函式不拋出例外，路徑中對應位置直接為空字串。
     */
    @Test
    fun `bloodPressures with empty appId produces path with empty segment`() {
        val appId = ""
        val userId = "user123"

        val path = FirestorePaths.bloodPressures(appId, userId)

        assertThat(path).isEqualTo("artifacts//users/user123/bloodPressures")
    }

    /**
     * 記錄 userId 為空字串時的實際行為：
     * 函式不拋出例外，路徑中對應位置直接為空字串。
     */
    @Test
    fun `bloodPressures with empty userId produces path with empty segment`() {
        val appId = "bloodpressurelog_debug"
        val userId = ""

        val path = FirestorePaths.bloodPressures(appId, userId)

        assertThat(path).isEqualTo("artifacts/bloodpressurelog_debug/users//bloodPressures")
    }

    /**
     * 記錄 appId 與 userId 同時為空字串時的實際行為。
     */
    @Test
    fun `bloodPressures with both empty appId and userId produces path with empty segments`() {
        val appId = ""
        val userId = ""

        val path = FirestorePaths.bloodPressures(appId, userId)

        assertThat(path).isEqualTo("artifacts//users//bloodPressures")
    }

    // -------------------------------------------------------------------------
    // 含 "/" 的特殊字元
    // -------------------------------------------------------------------------

    /**
     * 記錄 appId 含有 "/" 時的實際行為：
     * 函式不做任何跳脫處理，"/" 直接內嵌於路徑字串中，
     * 導致 split("/") 後的節點數超過 5 個。
     */
    @Test
    fun `bloodPressures with slash in appId embeds slash as-is`() {
        val appId = "app/extra"
        val userId = "user123"

        val path = FirestorePaths.bloodPressures(appId, userId)

        assertThat(path).isEqualTo("artifacts/app/extra/users/user123/bloodPressures")
    }

    /**
     * 記錄 userId 含有 "/" 時的實際行為：
     * 函式不做任何跳脫處理，"/" 直接內嵌於路徑字串中，
     * 導致 split("/") 後的節點數超過 5 個。
     */
    @Test
    fun `bloodPressures with slash in userId embeds slash as-is`() {
        val appId = "bloodpressurelog_debug"
        val userId = "user/123"

        val path = FirestorePaths.bloodPressures(appId, userId)

        assertThat(path).isEqualTo("artifacts/bloodpressurelog_debug/users/user/123/bloodPressures")
    }
}
