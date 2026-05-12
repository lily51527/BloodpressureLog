package idv.wennyli.bloodpressurelog.ui.view.login

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: ForgotPasswordViewModel

    @Before
    fun setUp() {
        viewModel = ForgotPasswordViewModel(mockAuthRepository)
    }

    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.isEmailSent)
    }

    @Test
    fun `onEmailChange updates email and clears errorMessage`() {
        coEvery { mockAuthRepository.sendPasswordResetEmail(any()) } throws RuntimeException("err")
        viewModel.sendResetEmail()

        viewModel.onEmailChange("user@example.com")

        assertEquals("user@example.com", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `sendResetEmail sets isEmailSent on success`() = runTest {
        coEvery { mockAuthRepository.sendPasswordResetEmail(any()) } just Runs

        viewModel.sendResetEmail()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isEmailSent)
    }

    @Test
    fun `sendResetEmail sets errorMessage and clears isLoading on failure`() = runTest {
        coEvery { mockAuthRepository.sendPasswordResetEmail(any()) } throws
            RuntimeException("User not found")

        viewModel.sendResetEmail()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isEmailSent)
        assertEquals("User not found", viewModel.uiState.value.errorMessage)
    }
}
