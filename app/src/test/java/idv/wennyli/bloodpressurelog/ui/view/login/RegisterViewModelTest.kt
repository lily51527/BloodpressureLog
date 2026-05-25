package idv.wennyli.bloodpressurelog.ui.view.login

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
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

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockAuthRepository = mockk<AuthRepository>()
    private val mockResourceProvider = mockk<ResourceProvider>()
    private lateinit var viewModel: RegisterViewModel

    @BeforeTest
    fun setUp() {
        viewModel = RegisterViewModel(mockAuthRepository, mockResourceProvider)
    }

    /** 初始狀態下，所有輸入欄位應為空且不處於載入中或有錯誤。 */
    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState.value
        assertThat(state.email).isEqualTo("")
        assertThat(state.password).isEqualTo("")
        assertThat(state.confirmPassword).isEqualTo("")
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

    /** 使用者修改確認密碼輸入時，應更新 confirmPassword 值並清除先前的錯誤訊息。 */
    @Test
    fun `onConfirmPasswordChange updates confirmPassword and clears errorMessage`() {
        viewModel.onConfirmPasswordChange("secret123")
        assertThat(viewModel.uiState.value.confirmPassword).isEqualTo("secret123")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /** 兩次輸入的密碼不一致時，註冊應顯示錯誤訊息而不進行實際註冊。 */
    @Test
    fun `register sets errorMessage when passwords do not match`() {
        every {
            mockResourceProvider.getString(R.string.register_error_passwords_mismatch)
        } returns "兩次輸入的密碼不一致"
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("abc123")
        viewModel.onConfirmPasswordChange("xyz456")

        viewModel.register()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("兩次輸入的密碼不一致")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    /** 註冊成功後，應發射導向 Email 驗證畫面的導航事件。 */
    @Test
    fun `register emits navigateToEmailVerification on success`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("secret123")
        viewModel.onConfirmPasswordChange("secret123")
        coEvery { mockAuthRepository.registerWithEmail(any(), any()) } just Runs

        viewModel.navigateToEmailVerification.test {
            viewModel.register()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** 註冊失敗時，應顯示錯誤訊息並清除載入狀態。 */
    @Test
    fun `register sets errorMessage and clears isLoading on failure`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("secret123")
        viewModel.onConfirmPasswordChange("secret123")
        coEvery { mockAuthRepository.registerWithEmail(any(), any()) } throws
            RuntimeException("Email already in use")

        viewModel.register()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Email already in use")
    }

    // ── Inverse：輸入變更清除既有錯誤 ─────────────────────────────────────────

    /**
     * 先觸發 email 空白讓 errorMessage 有值，
     * 再呼叫 onEmailChange，確認 errorMessage 真的被清除。
     */
    @Test
    fun `onEmailChange clears pre-existing errorMessage`() {
        every {
            mockResourceProvider.getString(R.string.register_error_email_required)
        } returns "請輸入 Email"
        viewModel.register()

        viewModel.onEmailChange("new@example.com")

        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /**
     * 先觸發 email 空白讓 errorMessage 有值，
     * 再呼叫 onPasswordChange，確認 errorMessage 真的被清除。
     */
    @Test
    fun `onPasswordChange clears pre-existing errorMessage`() {
        every {
            mockResourceProvider.getString(R.string.register_error_email_required)
        } returns "請輸入 Email"
        viewModel.register()

        viewModel.onPasswordChange("newpassword")

        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /**
     * 先觸發 email 空白讓 errorMessage 有值，
     * 再呼叫 onConfirmPasswordChange，確認 errorMessage 真的被清除。
     */
    @Test
    fun `onConfirmPasswordChange clears pre-existing errorMessage`() {
        every {
            mockResourceProvider.getString(R.string.register_error_email_required)
        } returns "請輸入 Email"
        viewModel.register()

        viewModel.onConfirmPasswordChange("abc123")

        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    // ── Boundary：例外訊息為 null ─────────────────────────────────────────────

    /**
     * register 拋出 message 為 null 的 Exception 時，
     * errorMessage 應 fallback 為空字串（`e.message ?: ""`）。
     */
    @Test
    fun `register sets empty string errorMessage when exception message is null`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("secret123")
        viewModel.onConfirmPasswordChange("secret123")
        coEvery { mockAuthRepository.registerWithEmail(any(), any()) } throws RuntimeException()

        viewModel.register()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("")
    }

    // ── 輸入驗證 ──────────────────────────────────────────────────────────────

    /** Email 為空白時，應顯示「請輸入 Email」錯誤並不進行實際註冊。 */
    @Test
    fun `register sets errorMessage when email is blank`() {
        every {
            mockResourceProvider.getString(R.string.register_error_email_required)
        } returns "請輸入 Email"
        // email 保持初始值 ""，password 設為合法值
        viewModel.onPasswordChange("secret123")
        viewModel.onConfirmPasswordChange("secret123")

        viewModel.register()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入 Email")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    /** 密碼長度不足 6 個字元時，應顯示密碼強度不足錯誤並不進行實際註冊。 */
    @Test
    fun `register sets errorMessage when password is shorter than 6 characters`() {
        every {
            mockResourceProvider.getString(R.string.error_auth_weak_password)
        } returns "密碼強度不足，請使用至少 6 位字元"
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("abc12")       // 5 個字元
        viewModel.onConfirmPasswordChange("abc12")

        viewModel.register()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("密碼強度不足，請使用至少 6 位字元")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

}
