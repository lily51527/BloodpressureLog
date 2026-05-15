package idv.wennyli.bloodpressurelog.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.assertIs
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.model.AuthException
import idv.wennyli.bloodpressurelog.utils.ResourceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class AuthRepositoryImplTest {

    private val mockAuth = mockk<FirebaseAuth>()
    private val mockUser = mockk<FirebaseUser>()
    private val mockResourceProvider = mockk<ResourceProvider>()
    private lateinit var repository: AuthRepositoryImpl

    @BeforeTest
    fun setUp() {
        every { mockResourceProvider.getString(R.string.error_auth_generic) } returns "操作失敗，請稍後再試"
        repository = AuthRepositoryImpl(mockAuth, mockResourceProvider)
    }

    /** currentUser 應返回 FirebaseAuth 目前登入的使用者物件。 */
    @Test
    fun `currentUser returns auth currentUser`() {
        every { mockAuth.currentUser } returns mockUser
        assertThat(repository.currentUser).isEqualTo(mockUser)
    }

    /** 未登入時，currentUser 應返回 null。 */
    @Test
    fun `currentUser returns null when not authenticated`() {
        every { mockAuth.currentUser } returns null
        assertThat(repository.currentUser).isNull()
    }

    /** Firebase 登入成功時，signInWithEmail 應正常完成而不拋出例外。 */
    @Test
    fun `signInWithEmail completes on Firebase success`() = runTest {
        val task = buildSuccessTask<AuthResult>(null)
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns task

        repository.signInWithEmail("test@test.com", "password")
    }

    /** Firebase 登入失敗時，signInWithEmail 應拋出含本地化訊息的 AuthException。 */
    @Test
    fun `signInWithEmail throws AuthException with localized message on Firebase failure`() = runTest {
        val exception = RuntimeException("Invalid credentials")
        val task = buildFailureTask<AuthResult>(exception)
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns task

        val thrown = runCatching { repository.signInWithEmail("test@test.com", "wrong") }
            .exceptionOrNull()

        assertIs<AuthException>(thrown)
        assertThat(thrown.message).isEqualTo("操作失敗，請稍後再試")
    }

    /** Firebase 匿名登入成功時，signInAnonymously 應正常完成而不拋出例外。 */
    @Test
    fun `signInAnonymously completes on Firebase success`() = runTest {
        val task = buildSuccessTask<AuthResult>(null)
        every { mockAuth.signInAnonymously() } returns task

        repository.signInAnonymously()
    }

    /** Firebase 匿名登入失敗時，signInAnonymously 應拋出含本地化訊息的 AuthException。 */
    @Test
    fun `signInAnonymously throws AuthException with localized message on Firebase failure`() = runTest {
        val exception = RuntimeException("Anonymous sign in failed")
        val task = buildFailureTask<AuthResult>(exception)
        every { mockAuth.signInAnonymously() } returns task

        val thrown = runCatching { repository.signInAnonymously() }.exceptionOrNull()

        assertIs<AuthException>(thrown)
        assertThat(thrown.message).isEqualTo("操作失敗，請稍後再試")
    }

    /** signOut 應呼叫 FirebaseAuth 的 signOut，確保使用者登出。 */
    @Test
    fun `signOut calls auth signOut`() = runTest {
        every { mockAuth.signOut() } returns Unit

        repository.signOut()

        verify { mockAuth.signOut() }
    }

    /** Firebase 註冊成功時，registerWithEmail 應正常完成，並對新使用者觸發寄送驗證信。 */
    @Test
    fun `registerWithEmail completes and sends verification email on Firebase success`() = runTest {
        val task = buildSuccessTask<AuthResult>(null)
        every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns task
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.sendEmailVerification() } returns buildSuccessTask<Void>(null)

        repository.registerWithEmail("new@test.com", "password123")

        verify { mockUser.sendEmailVerification() }
    }

    /** Firebase 註冊失敗時，registerWithEmail 應拋出含本地化訊息的 AuthException。 */
    @Test
    fun `registerWithEmail throws AuthException with localized message on Firebase failure`() = runTest {
        val exception = RuntimeException("Email already in use")
        val task = buildFailureTask<AuthResult>(exception)
        every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns task

        val thrown = runCatching { repository.registerWithEmail("taken@test.com", "password123") }
            .exceptionOrNull()

        assertIs<AuthException>(thrown)
        assertThat(thrown.message).isEqualTo("操作失敗，請稍後再試")
    }

    /** 已登入時，sendEmailVerification 應正常完成而不拋出例外。 */
    @Test
    fun `sendEmailVerification completes on Firebase success`() = runTest {
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.sendEmailVerification() } returns buildSuccessTask<Void>(null)

        repository.sendEmailVerification()
    }

    /** 未登入時，sendEmailVerification 應立即拋出 AuthException，不呼叫 Firebase。 */
    @Test
    fun `sendEmailVerification throws AuthException when user is null`() = runTest {
        every { mockAuth.currentUser } returns null

        val thrown = runCatching { repository.sendEmailVerification() }.exceptionOrNull()

        assertIs<AuthException>(thrown)
        assertThat(thrown.message).isEqualTo("操作失敗，請稍後再試")
    }

    /** Firebase 驗證信寄送失敗時，sendEmailVerification 應拋出含本地化訊息的 AuthException。 */
    @Test
    fun `sendEmailVerification throws AuthException with localized message on Firebase failure`() = runTest {
        val exception = RuntimeException("Send email failed")
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.sendEmailVerification() } returns buildFailureTask<Void>(exception)

        val thrown = runCatching { repository.sendEmailVerification() }.exceptionOrNull()

        assertIs<AuthException>(thrown)
        assertThat(thrown.message).isEqualTo("操作失敗，請稍後再試")
    }

    /** Firebase 重設密碼信寄送成功時，sendPasswordResetEmail 應正常完成而不拋出例外。 */
    @Test
    fun `sendPasswordResetEmail completes on Firebase success`() = runTest {
        val task = buildSuccessTask<Void>(null)
        every { mockAuth.sendPasswordResetEmail(any()) } returns task

        repository.sendPasswordResetEmail("user@test.com")
    }

    /** Firebase 重設密碼信寄送失敗時，sendPasswordResetEmail 應拋出含本地化訊息的 AuthException。 */
    @Test
    fun `sendPasswordResetEmail throws AuthException with localized message on Firebase failure`() = runTest {
        val exception = RuntimeException("User not found")
        val task = buildFailureTask<Void>(exception)
        every { mockAuth.sendPasswordResetEmail(any()) } returns task

        val thrown = runCatching { repository.sendPasswordResetEmail("nobody@test.com") }
            .exceptionOrNull()

        assertIs<AuthException>(thrown)
        assertThat(thrown.message).isEqualTo("操作失敗，請稍後再試")
    }

    // region helpers

    @Suppress("UNCHECKED_CAST")
    private fun <T> buildSuccessTask(value: Any?): Task<T> = mockk<Task<T>>().apply {
        every { addOnSuccessListener(any()) } answers {
            (firstArg() as OnSuccessListener<Any?>).onSuccess(value)
            this@apply
        }
        every { addOnFailureListener(any()) } returns this@apply
    }

    private fun <T> buildFailureTask(exception: Exception): Task<T> = mockk<Task<T>>().apply {
        every { addOnSuccessListener(any()) } returns this@apply
        every { addOnFailureListener(any()) } answers {
            firstArg<OnFailureListener>().onFailure(exception)
            this@apply
        }
    }

    // endregion
}
