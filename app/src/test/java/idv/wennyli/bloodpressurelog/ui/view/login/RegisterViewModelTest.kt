package idv.wennyli.bloodpressurelog.ui.view.login

import app.cash.turbine.test
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        viewModel = RegisterViewModel(mockAuthRepository)
    }

    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onEmailChange updates email and clears errorMessage`() {
        viewModel.onEmailChange("user@example.com")
        assertEquals("user@example.com", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onPasswordChange updates password and clears errorMessage`() {
        viewModel.onPasswordChange("secret123")
        assertEquals("secret123", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onConfirmPasswordChange updates confirmPassword and clears errorMessage`() {
        viewModel.onConfirmPasswordChange("secret123")
        assertEquals("secret123", viewModel.uiState.value.confirmPassword)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `register sets errorMessage when passwords do not match`() {
        viewModel.onPasswordChange("abc123")
        viewModel.onConfirmPasswordChange("xyz456")

        viewModel.register("兩次輸入的密碼不一致")

        assertEquals("兩次輸入的密碼不一致", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
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
        assertFalse(state.isLoading)
        assertEquals("Email already in use", state.errorMessage)
    }
}
