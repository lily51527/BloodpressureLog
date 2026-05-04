package idv.wennyli.bloodpressurelog.ui.view.login

import app.cash.turbine.test
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: EmailVerificationViewModel

    @Before
    fun setUp() {
        every { mockAuthRepository.currentUser } returns null
        viewModel = EmailVerificationViewModel(mockAuthRepository)
    }

    @Test
    fun `initial state has email from currentUser`() {
        val mockUser = mockk<com.google.firebase.auth.FirebaseUser>()
        every { mockUser.email } returns "user@example.com"
        every { mockAuthRepository.currentUser } returns mockUser
        val vm = EmailVerificationViewModel(mockAuthRepository)

        assertEquals("user@example.com", vm.uiState.value.email)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(0, vm.uiState.value.resendCooldownSeconds)
    }

    @Test
    fun `resendVerificationEmail starts cooldown on success`() = runTest {
        coEvery { mockAuthRepository.sendEmailVerification() } returns DataState.Success(Unit)

        viewModel.resendVerificationEmail()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS,
            viewModel.uiState.value.resendCooldownSeconds,
        )
    }

    @Test
    fun `resendVerificationEmail cooldown counts down to zero`() = runTest {
        coEvery { mockAuthRepository.sendEmailVerification() } returns DataState.Success(Unit)

        viewModel.resendVerificationEmail()
        advanceTimeBy(EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 100)

        assertEquals(0, viewModel.uiState.value.resendCooldownSeconds)
    }

    @Test
    fun `resendVerificationEmail sets errorMessage on failure`() = runTest {
        coEvery { mockAuthRepository.sendEmailVerification() } returns
            DataState.Error(RuntimeException("Too many requests"), "Too many requests")

        viewModel.resendVerificationEmail()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Too many requests", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `resendVerificationEmail is ignored when cooldown is active`() = runTest {
        coEvery { mockAuthRepository.sendEmailVerification() } returns DataState.Success(Unit)

        viewModel.resendVerificationEmail()
        viewModel.resendVerificationEmail()

        coVerify(exactly = 1) { mockAuthRepository.sendEmailVerification() }
    }

    @Test
    fun `backToLogin calls signOut and emits navigateToLogin`() = runTest {
        coEvery { mockAuthRepository.signOut() } returns Unit

        viewModel.navigateToLogin.test {
            viewModel.backToLogin()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { mockAuthRepository.signOut() }
    }
}
