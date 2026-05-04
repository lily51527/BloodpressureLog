package idv.wennyli.bloodpressurelog.ui.view.login

import app.cash.turbine.test
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import com.google.firebase.auth.FirebaseUser
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.every
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
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        viewModel = LoginViewModel(mockAuthRepository)
    }

    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
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
    fun `signInWithEmail emits navigateToMain when email is verified`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.isEmailVerified } returns true
        every { mockAuthRepository.currentUser } returns mockUser
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } returns DataState.Success(Unit)

        viewModel.navigateToMain.test {
            viewModel.signInWithEmail()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signInWithEmail emits navigateToEmailVerification when email is not verified`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.isEmailVerified } returns false
        every { mockAuthRepository.currentUser } returns mockUser
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } returns DataState.Success(Unit)

        viewModel.navigateToEmailVerification.test {
            viewModel.signInWithEmail()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signInWithEmail sets errorMessage and clears isLoading on failure`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } returns
            DataState.Error(RuntimeException("Bad credentials"), "Bad credentials")

        viewModel.signInWithEmail()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Bad credentials", state.errorMessage)
    }

    @Test
    fun `signInAnonymously emits navigateToMain on success`() = runTest {
        coEvery { mockAuthRepository.signInAnonymously() } returns DataState.Success(Unit)

        viewModel.navigateToMain.test {
            viewModel.signInAnonymously()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signInAnonymously sets errorMessage and clears isLoading on failure`() = runTest {
        coEvery { mockAuthRepository.signInAnonymously() } returns
            DataState.Error(RuntimeException("Failed"), "Failed")

        viewModel.signInAnonymously()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Failed", state.errorMessage)
    }

    @Test
    fun `clearError resets errorMessage`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } returns
            DataState.Error(RuntimeException("err"), "err")
        viewModel.signInWithEmail()

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
