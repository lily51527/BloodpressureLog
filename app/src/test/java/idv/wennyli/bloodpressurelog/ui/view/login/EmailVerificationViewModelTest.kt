package idv.wennyli.bloodpressurelog.ui.view.login

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import com.google.firebase.auth.FirebaseUser
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: EmailVerificationViewModel

    @BeforeTest
    fun setUp() {
        every { mockAuthRepository.currentUser } returns null
        viewModel = EmailVerificationViewModel(mockAuthRepository)
    }

    /** 初始狀態下，冷卻倒數應已啟動且不應處於載入中或有錯誤訊息。 */
    @Test
    fun `initial state starts with cooldown active`() {
        assertThat(viewModel.uiState.value.resendCooldownSeconds).isEqualTo(
            EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS
        )
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /** 初始狀態的 email 欄位應顯示目前登入使用者的 email 位址。 */
    @Test
    fun `initial state has email from currentUser`() {
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.email } returns "user@example.com"
        every { mockAuthRepository.currentUser } returns mockUser
        val vm = EmailVerificationViewModel(mockAuthRepository)
        assertThat(vm.uiState.value.email).isEqualTo("user@example.com")
    }

    /** 冷卻期間呼叫重新發送，應不觸發實際寄信以防止濫用。 */
    @Test
    fun `resendVerificationEmail is ignored during initial cooldown`() = runTest(testDispatcher) {
        viewModel.resendVerificationEmail()
        coVerify(exactly = 0) { mockAuthRepository.sendEmailVerification() }
    }

    /** 冷卻時間結束後，重新發送驗證信應成功並重置冷卻倒數。 */
    @Test
    fun `resendVerificationEmail succeeds after initial cooldown expires`() =
        runTest(testDispatcher) {
            coEvery { mockAuthRepository.sendEmailVerification() } just Runs

            advanceTimeBy(EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 100)
            viewModel.resendVerificationEmail()

            assertThat(viewModel.uiState.value.isLoading).isFalse()
            assertThat(viewModel.uiState.value.resendCooldownSeconds).isEqualTo(
                EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS
            )
        }

    /** 重新發送後，冷卻倒數應在時間到期後歸零。 */
    @Test
    fun `resendVerificationEmail cooldown counts down to zero after resend`() =
        runTest(testDispatcher) {
            coEvery { mockAuthRepository.sendEmailVerification() } just Runs

            advanceTimeBy(EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 100)
            viewModel.resendVerificationEmail()
            advanceTimeBy(EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 100)

            assertThat(viewModel.uiState.value.resendCooldownSeconds).isEqualTo(0)
        }

    /** 重新發送驗證信失敗時，應顯示錯誤訊息且不再處於載入中狀態。 */
    @Test
    fun `resendVerificationEmail sets errorMessage on failure`() = runTest(testDispatcher) {
        coEvery { mockAuthRepository.sendEmailVerification() } throws
            RuntimeException("Too many requests")

        advanceTimeBy(EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 100)
        viewModel.resendVerificationEmail()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Too many requests")
    }

    /** 返回登入頁時，應先登出並發射導航事件。 */
    @Test
    fun `backToLogin calls signOut and emits navigateToLogin`() = runTest(testDispatcher) {
        coEvery { mockAuthRepository.signOut() } returns Unit

        viewModel.navigateToLogin.test {
            viewModel.backToLogin()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { mockAuthRepository.signOut() }
    }

    // ── Existence：currentUser 為 null ────────────────────────────────────────

    /** currentUser 為 null 時（未登入狀態），初始 email 欄位應為空字串。 */
    @Test
    fun `initial state has empty email when currentUser is null`() {
        // setUp 已設定 currentUser = null
        assertThat(viewModel.uiState.value.email).isEqualTo("")
    }

    // ── Boundary：例外訊息為 null ─────────────────────────────────────────────

    /**
     * resendVerificationEmail 拋出 message 為 null 的 Exception 時，
     * errorMessage 應 fallback 為空字串（`e.message ?: ""`）。
     */
    @Test
    fun `resendVerificationEmail sets empty string errorMessage when exception message is null`() =
        runTest(testDispatcher) {
            coEvery { mockAuthRepository.sendEmailVerification() } throws RuntimeException()

            advanceTimeBy(EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 100)
            viewModel.resendVerificationEmail()

            assertThat(viewModel.uiState.value.errorMessage).isEqualTo("")
        }

    // ── Inverse：失敗後冷卻不重置 ─────────────────────────────────────────────

    /**
     * resend 失敗後，冷卻倒數不應重置（仍為 0），
     * 確保使用者可以立即重試，不被鎖定。
     */
    @Test
    fun `resendVerificationEmail does not restart cooldown on failure`() =
        runTest(testDispatcher) {
            coEvery { mockAuthRepository.sendEmailVerification() } throws RuntimeException("error")

            advanceTimeBy(EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 100)
            viewModel.resendVerificationEmail()

            assertThat(viewModel.uiState.value.resendCooldownSeconds).isEqualTo(0)
        }
}
