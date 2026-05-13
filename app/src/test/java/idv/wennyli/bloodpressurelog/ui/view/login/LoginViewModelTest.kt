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
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun setUp() {
        viewModel = LoginViewModel(mockAuthRepository)
    }

    @Test
    fun `initial state is empty and not loading`() {
        val state = viewModel.uiState.value
        assertThat(state.email).isEqualTo("")
        assertThat(state.password).isEqualTo("")
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

    @Test
    fun `signInWithEmail emits navigateToEmailVerification when email is not verified`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.isEmailVerified } returns false
        every { mockAuthRepository.currentUser } returns mockUser
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } just Runs

        viewModel.navigateToEmailVerification.test {
            viewModel.signInWithEmail()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signInWithEmail sets errorMessage and clears isLoading on failure`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } throws
            RuntimeException("Bad credentials")

        viewModel.signInWithEmail()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Bad credentials")
    }

    @Test
    fun `signInAnonymously emits navigateToMain on success`() = runTest {
        coEvery { mockAuthRepository.signInAnonymously() } just Runs

        viewModel.navigateToMain.test {
            viewModel.signInAnonymously()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signInAnonymously sets errorMessage and clears isLoading on failure`() = runTest {
        coEvery { mockAuthRepository.signInAnonymously() } throws RuntimeException("Failed")

        viewModel.signInAnonymously()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Failed")
    }

    @Test
    fun `clearError resets errorMessage`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } throws RuntimeException("err")
        viewModel.signInWithEmail()

        viewModel.clearError()

        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }
}
