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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

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
            mockResourceProvider.getString(R.string.error_passwords_do_not_match)
        } returns "兩次輸入的密碼不一致"
        viewModel.onPasswordChange("abc123")
        viewModel.onConfirmPasswordChange("xyz456")

        viewModel.register()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("兩次輸入的密碼不一致")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    /** 註冊成功後，應發射導向 Email 驗證畫面的導航事件。 */
    @Test
    fun `register emits navigateToEmailVerification on success`() = runTest {
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
        viewModel.onPasswordChange("secret123")
        viewModel.onConfirmPasswordChange("secret123")
        coEvery { mockAuthRepository.registerWithEmail(any(), any()) } throws
            RuntimeException("Email already in use")

        viewModel.register()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Email already in use")
    }
}
