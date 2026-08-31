package idv.wennyli.bloodpressurelog.ui.navigation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotInstanceOf
import kotlin.test.Test

/**
 * 驗證 type-safe 導航目的地的 [Destination.RequiresAuth] 標記與參數相等性，
 * 避免新增畫面時漏標記、或誤改 route 參數導致 NavController 無法辨識目的地。
 */
class DestinationTest {

    /** 相同 recordId 的 AddEditRecord 相等，不同則不相等（NavController 以相等性辨識目的地）。 */
    @Test
    fun `AddEditRecord equality is based on recordId`() {
        assertThat(Destination.AddEditRecord("a")).isEqualTo(Destination.AddEditRecord("a"))
        assertThat(Destination.AddEditRecord("a")).isNotEqualTo(Destination.AddEditRecord("b"))
    }

    /** 已登入區的目的地都應標記為 RequiresAuth。 */
    @Test
    fun `authenticated destinations are marked RequiresAuth`() {
        val authed: List<Destination> = listOf(
            Destination.AddRecord,
            Destination.RecordList,
            Destination.Trends,
            Destination.AddEditRecord("id"),
        )

        authed.forEach { assertThat(it).isInstanceOf(Destination.RequiresAuth::class) }
    }

    /** 未登入區（登入流程）的目的地都不應標記為 RequiresAuth。 */
    @Test
    fun `auth-flow destinations are not marked RequiresAuth`() {
        val public: List<Destination> = listOf(
            Destination.Login,
            Destination.Register,
            Destination.ForgotPassword,
            Destination.EmailVerification,
        )

        public.forEach { assertThat(it).isNotInstanceOf(Destination.RequiresAuth::class) }
    }
}
