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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: EmailVerificationViewModel

    @BeforeTest
    fun setUp() {
        every { mockAuthRepository.currentUser } returns null
        viewModel = EmailVerificationViewModel(mockAuthRepository)
    }

    @Test
    fun `initial state starts with cooldown active`() {
        assertThat(viewModel.uiState.value.resendCooldownSeconds).isEqualTo(
            EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS
        )
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `initial state has email from currentUser`() {
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.email } returns "user@example.com"
        every { mockAuthRepository.currentUser } returns mockUser
        val vm = EmailVerificationViewModel(mockAuthRepository)
        assertThat(vm.uiState.value.email).isEqualTo("user@example.com")
    }

    @Test
    fun `resendVerificationEmail is ignored during initial cooldown`() = runTest(testDispatcher) {
        viewModel.resendVerificationEmail()
        coVerify(exactly = 0) { mockAuthRepository.sendEmailVerification() }
    }

    @Test
    fun `resendVerificationEmail succeeds after initial cooldown expires`() =
        runTest(testDispatcher) {
            coEvery { mockAuthRepository.sendEmailVerification() } just Runs

            advanceTimeBy(EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 100)
            viewModel.resendVerificationEmail()

            assertThat(viewModel.uiState.value.isLoading).isFalse()
            assertThat(viewModel.uiState.value.resendCooldownSeconds).isEqualTo(
                EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS
            )
        }

    @Test
    fun `resendVerificationEmail cooldown counts down to zero after resend`() =
        runTest(testDispatcher) {
            coEvery { mockAuthRepository.sendEmailVerification() } just Runs

            advanceTimeBy(EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 100)
            viewModel.resendVerificationEmail()
            advanceTimeBy(EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 100)

            assertThat(viewModel.uiState.value.resendCooldownSeconds).isEqualTo(0)
        }

    @Test
    fun `resendVerificationEmail sets errorMessage on failure`() = runTest(testDispatcher) {
        coEvery { mockAuthRepository.sendEmailVerification() } throws
            RuntimeException("Too many requests")

        advanceTimeBy(EmailVerificationViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 100)
        viewModel.resendVerificationEmail()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Too many requests")
    }

    @Test
    fun `backToLogin calls signOut and emits navigateToLogin`() = runTest(testDispatcher) {
        coEvery { mockAuthRepository.signOut() } returns Unit

        viewModel.navigateToLogin.test {
            viewModel.backToLogin()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { mockAuthRepository.signOut() }
    }
}
