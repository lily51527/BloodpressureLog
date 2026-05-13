package idv.wennyli.bloodpressurelog.ui.view.login

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
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
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: RegisterViewModel

    @BeforeTest
    fun setUp() {
        viewModel = RegisterViewModel(mockAuthRepository)
    }

    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState.value
        assertThat(state.email).isEqualTo("")
        assertThat(state.password).isEqualTo("")
        assertThat(state.confirmPassword).isEqualTo("")
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `onEmailChange updates email and clears errorMessage`() {
        viewModel.onEmailChange("user@example.com")
        assertThat(viewModel.uiState.value.email).isEqualTo("user@example.com")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `onPasswordChange updates password and clears errorMessage`() {
        viewModel.onPasswordChange("secret123")
        assertThat(viewModel.uiState.value.password).isEqualTo("secret123")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `onConfirmPasswordChange updates confirmPassword and clears errorMessage`() {
        viewModel.onConfirmPasswordChange("secret123")
        assertThat(viewModel.uiState.value.confirmPassword).isEqualTo("secret123")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `register sets errorMessage when passwords do not match`() {
        viewModel.onPasswordChange("abc123")
        viewModel.onConfirmPasswordChange("xyz456")

        viewModel.register("兩次輸入的密碼不一致")

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("兩次輸入的密碼不一致")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `register emits navigateToEmailVerification on success`() = runTest {
        viewModel.onPasswordChange("secret123")
        viewModel.onConfirmPasswordChange("secret123")
        coEvery { mockAuthRepository.registerWithEmail(any(), any()) } just Runs

        viewModel.navigateToEmailVerification.test {
            viewModel.register("mismatch")
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `register sets errorMessage and clears isLoading on failure`() = runTest {
        viewModel.onPasswordChange("secret123")
        viewModel.onConfirmPasswordChange("secret123")
        coEvery { mockAuthRepository.registerWithEmail(any(), any()) } throws
            RuntimeException("Email already in use")

        viewModel.register("mismatch")

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Email already in use")
    }
}
