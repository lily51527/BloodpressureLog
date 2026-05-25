package idv.wennyli.bloodpressurelog.ui.view.login

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import com.google.firebase.auth.FirebaseUser
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import idv.wennyli.bloodpressurelog.utils.ResourceProvider
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockAuthRepository = mockk<AuthRepository>()
    private val mockResourceProvider = mockk<ResourceProvider>()
    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun setUp() {
        viewModel = LoginViewModel(mockAuthRepository, mockResourceProvider)
    }

    /** 初始狀態下，email 和 password 應為空且不處於載入中或有錯誤。 */
    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState.value
        assertThat(state.email).isEqualTo("")
        assertThat(state.password).isEqualTo("")
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    /** 使用者修改 email 輸入時，應更新 email 值並清除先前的錯誤訊息。 */
    @Test
    fun `onEmailChange updates email and clears errorMessage`() {
        viewModel.onEmailChange("user@example.com")
        assertThat(viewModel.uiState.value.email).isEqualTo("user@example.com")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /** 使用者修改密碼輸入時，應更新 password 值並清除先前的錯誤訊息。 */
    @Test
    fun `onPasswordChange updates password and clears errorMessage`() {
        viewModel.onPasswordChange("secret123")
        assertThat(viewModel.uiState.value.password).isEqualTo("secret123")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /** Email 已驗證的使用者登入成功後，應導向主畫面。 */
    @Test
    fun `signInWithEmail emits navigateToMain when email is verified`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.isEmailVerified } returns true
        every { mockAuthRepository.currentUser } returns mockUser
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } just Runs

        viewModel.navigateToMain.test {
            viewModel.signInWithEmail()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Email 尚未驗證的使用者登入後，應導向 Email 驗證畫面並清除 isLoading。 */
    @Test
    fun `signInWithEmail emits navigateToEmailVerification and clears isLoading when email is not verified`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.isEmailVerified } returns false
            every { mockAuthRepository.currentUser } returns mockUser
            coEvery { mockAuthRepository.signInWithEmail(any(), any()) } just Runs

            viewModel.navigateToEmailVerification.test {
                viewModel.signInWithEmail()
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(viewModel.uiState.value.isLoading).isFalse()
        }

    /** Email 登入失敗時，應顯示錯誤訊息並清除載入狀態。 */
    @Test
    fun `signInWithEmail sets errorMessage and clears isLoading on failure`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } throws
            RuntimeException("Bad credentials")

        viewModel.signInWithEmail()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Bad credentials")
    }

    /** 匿名登入成功後，應發射導向主畫面的導航事件。 */
    @Test
    fun `signInAnonymously emits navigateToMain on success`() = runTest {
        coEvery { mockAuthRepository.signInAnonymously() } just Runs

        viewModel.navigateToMain.test {
            viewModel.signInAnonymously()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** 匿名登入失敗時，應顯示錯誤訊息並清除載入狀態。 */
    @Test
    fun `signInAnonymously sets errorMessage and clears isLoading on failure`() = runTest {
        coEvery { mockAuthRepository.signInAnonymously() } throws RuntimeException("Failed")

        viewModel.signInAnonymously()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Failed")
    }

    /** 呼叫 clearError 後，errorMessage 應被清除為 null。 */
    @Test
    fun `clearError resets errorMessage`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } throws RuntimeException("err")
        viewModel.signInWithEmail()

        viewModel.clearError()

        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    // ── Existence：currentUser 為 null ────────────────────────────────────────

    /**
     * 登入後 currentUser 回傳 null（sign-in 成功卻無 user，屬於異常狀態），
     * 不應導向任何頁面，應顯示錯誤訊息並清除 isLoading。
     */
    @Test
    fun `signInWithEmail sets errorMessage and clears isLoading when currentUser is null after sign in`() =
        runTest {
            every {
                mockResourceProvider.getString(R.string.login_error_sign_in_failed)
            } returns "登入失敗，請稍後再試"
            every { mockAuthRepository.currentUser } returns null
            coEvery { mockAuthRepository.signInWithEmail(any(), any()) } just Runs

            viewModel.signInWithEmail()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessage).isEqualTo("登入失敗，請稍後再試")
        }

    // ── Boundary：例外訊息為 null ─────────────────────────────────────────────

    /**
     * signInWithEmail 拋出 message 為 null 的 Exception 時，
     * errorMessage 應 fallback 為空字串（`e.message ?: ""`）。
     */
    @Test
    fun `signInWithEmail sets empty string errorMessage when exception message is null`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } throws RuntimeException()

        viewModel.signInWithEmail()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("")
    }

    /**
     * signInAnonymously 拋出 message 為 null 的 Exception 時，
     * errorMessage 應 fallback 為空字串（`e.message ?: ""`）。
     */
    @Test
    fun `signInAnonymously sets empty string errorMessage when exception message is null`() =
        runTest {
            coEvery { mockAuthRepository.signInAnonymously() } throws RuntimeException()

            viewModel.signInAnonymously()

            assertThat(viewModel.uiState.value.errorMessage).isEqualTo("")
        }

    // ── Inverse：輸入變更清除既有錯誤 ─────────────────────────────────────────

    /**
     * 先觸發登入失敗讓 errorMessage 有值，
     * 再呼叫 onEmailChange，確認 errorMessage 真的被清除。
     */
    @Test
    fun `onEmailChange clears pre-existing errorMessage`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } throws RuntimeException("err")
        viewModel.signInWithEmail()

        viewModel.onEmailChange("new@example.com")

        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /**
     * 先觸發登入失敗讓 errorMessage 有值，
     * 再呼叫 onPasswordChange，確認 errorMessage 真的被清除。
     */
    @Test
    fun `onPasswordChange clears pre-existing errorMessage`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } throws RuntimeException("err")
        viewModel.signInWithEmail()

        viewModel.onPasswordChange("newpassword")

        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

}
