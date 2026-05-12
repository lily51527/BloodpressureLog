package idv.wennyli.bloodpressurelog.data.repository

import android.content.Context
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.model.AuthException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private val mockAuth = mockk<FirebaseAuth>()
    private val mockUser = mockk<FirebaseUser>()
    private val mockContext = mockk<Context>()
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        every { mockContext.getString(R.string.error_auth_generic) } returns "操作失敗，請稍後再試"
        repository = AuthRepositoryImpl(mockAuth, mockContext)
    }

    @Test
    fun `currentUser returns auth currentUser`() {
        every { mockAuth.currentUser } returns mockUser
        assertEquals(mockUser, repository.currentUser)
    }

    @Test
    fun `currentUser returns null when not authenticated`() {
        every { mockAuth.currentUser } returns null
        assertNull(repository.currentUser)
    }

    @Test
    fun `signInWithEmail completes on Firebase success`() = runTest {
        val task = buildSuccessTask<AuthResult>(null)
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns task

        repository.signInWithEmail("test@test.com", "password")
    }

    @Test
    fun `signInWithEmail throws AuthException with localized message on Firebase failure`() = runTest {
        val exception = RuntimeException("Invalid credentials")
        val task = buildFailureTask<AuthResult>(exception)
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns task

        val thrown = runCatching { repository.signInWithEmail("test@test.com", "wrong") }
            .exceptionOrNull()

        assertTrue(thrown is AuthException)
        assertEquals("操作失敗，請稍後再試", thrown?.message)
    }

    @Test
    fun `signInAnonymously completes on Firebase success`() = runTest {
        val task = buildSuccessTask<AuthResult>(null)
        every { mockAuth.signInAnonymously() } returns task

        repository.signInAnonymously()
    }

    @Test
    fun `signInAnonymously throws AuthException with localized message on Firebase failure`() = runTest {
        val exception = RuntimeException("Anonymous sign in failed")
        val task = buildFailureTask<AuthResult>(exception)
        every { mockAuth.signInAnonymously() } returns task

        val thrown = runCatching { repository.signInAnonymously() }.exceptionOrNull()

        assertTrue(thrown is AuthException)
        assertEquals("操作失敗，請稍後再試", thrown?.message)
    }

    @Test
    fun `signOut calls auth signOut`() = runTest {
        every { mockAuth.signOut() } returns Unit

        repository.signOut()

        verify { mockAuth.signOut() }
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
