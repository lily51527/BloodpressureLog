package idv.wennyli.bloodpressurelog.ui.view.login

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: ForgotPasswordViewModel

    @BeforeTest
    fun setUp() {
        viewModel = ForgotPasswordViewModel(mockAuthRepository)
    }

    /** 初始狀態下，email 應為空且不處於載入中、無錯誤、未寄出信件。 */
    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState.value
        assertThat(state.email).isEqualTo("")
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
        assertThat(state.isEmailSent).isFalse()
    }

    /** 使用者修改 email 輸入時，應更新 email 值並清除先前的錯誤訊息。 */
    @Test
    fun `onEmailChange updates email and clears errorMessage`() {
        coEvery { mockAuthRepository.sendPasswordResetEmail(any()) } throws RuntimeException("err")
        viewModel.sendResetEmail()

        viewModel.onEmailChange("user@example.com")

        assertThat(viewModel.uiState.value.email).isEqualTo("user@example.com")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /** 寄出重設密碼信成功後，應將 isEmailSent 設為 true 表示已寄出。 */
    @Test
    fun `sendResetEmail sets isEmailSent on success`() = runTest {
        coEvery { mockAuthRepository.sendPasswordResetEmail(any()) } just Runs

        viewModel.sendResetEmail()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.isEmailSent).isTrue()
    }

    /** 寄出重設密碼信失敗時，應顯示錯誤訊息並清除載入狀態。 */
    @Test
    fun `sendResetEmail sets errorMessage and clears isLoading on failure`() = runTest {
        coEvery { mockAuthRepository.sendPasswordResetEmail(any()) } throws
            RuntimeException("User not found")

        viewModel.sendResetEmail()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.isEmailSent).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("User not found")
    }
}
