package idv.wennyli.bloodpressurelog.ui.view.main

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.google.firebase.auth.FirebaseUser
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import idv.wennyli.bloodpressurelog.utils.SnackbarController
import io.mockk.every
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockAuthRepository = mockk<AuthRepository>()
    private val mockSnackbarController = mockk<SnackbarController>()
    private val mockUser = mockk<FirebaseUser>()

    @BeforeTest
    fun setUp() {
        every { mockSnackbarController.messages } returns MutableSharedFlow()
    }

    private fun viewModel() = MainViewModel(
        authRepository = mockAuthRepository,
        snackbarController = mockSnackbarController,
    )

    /** authState 初始值應為 authRepository.currentUser。 */
    @Test
    fun `authState initial value reflects authRepository currentUser`() {
        every { mockAuthRepository.currentUser } returns null
        every { mockAuthRepository.authStateChanges } returns flowOf()

        val viewModel = viewModel()

        assertThat(viewModel.authState.value).isNull()
    }

    /** authStateChanges 發出新使用者時，authState 應同步更新。 */
    @Test
    fun `authState reflects authRepository authStateChanges emissions`() = runTest {
        every { mockAuthRepository.currentUser } returns null
        every { mockAuthRepository.authStateChanges } returns flowOf(mockUser)

        val viewModel = viewModel()

        assertThat(viewModel.authState.value).isEqualTo(mockUser)
    }

    /** snackbarMessages 應原樣轉發 snackbarController.messages 發出的訊息。 */
    @Test
    fun `snackbarMessages forwards snackbarController messages`() = runTest {
        every { mockAuthRepository.currentUser } returns null
        every { mockAuthRepository.authStateChanges } returns flowOf()
        val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
        every { mockSnackbarController.messages } returns messages

        val viewModel = viewModel()

        viewModel.snackbarMessages.test {
            messages.emit("儲存完成")

            assertThat(awaitItem()).isEqualTo("儲存完成")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
