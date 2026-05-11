package idv.wennyli.bloodpressurelog.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import idv.wennyli.bloodpressurelog.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseAuthErrorMapperTest {

    @Test
    fun `maps ERROR_INVALID_CREDENTIAL to invalid credential string res`() {
        val e = mockFirebaseAuthException("ERROR_INVALID_CREDENTIAL")
        assertEquals(R.string.error_auth_invalid_credential, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps ERROR_USER_NOT_FOUND to invalid credential string res`() {
        val e = mockFirebaseAuthException("ERROR_USER_NOT_FOUND")
        assertEquals(R.string.error_auth_invalid_credential, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps ERROR_WRONG_PASSWORD to invalid credential string res`() {
        val e = mockFirebaseAuthException("ERROR_WRONG_PASSWORD")
        assertEquals(R.string.error_auth_invalid_credential, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps ERROR_INVALID_EMAIL to invalid email string res`() {
        val e = mockFirebaseAuthException("ERROR_INVALID_EMAIL")
        assertEquals(R.string.error_auth_invalid_email, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps ERROR_EMAIL_ALREADY_IN_USE to email already in use string res`() {
        val e = mockFirebaseAuthException("ERROR_EMAIL_ALREADY_IN_USE")
        assertEquals(R.string.error_auth_email_already_in_use, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps ERROR_WEAK_PASSWORD to weak password string res`() {
        val e = mockFirebaseAuthException("ERROR_WEAK_PASSWORD")
        assertEquals(R.string.error_auth_weak_password, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps ERROR_USER_DISABLED to user disabled string res`() {
        val e = mockFirebaseAuthException("ERROR_USER_DISABLED")
        assertEquals(R.string.error_auth_user_disabled, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps ERROR_TOO_MANY_REQUESTS to too many requests string res`() {
        val e = mockFirebaseAuthException("ERROR_TOO_MANY_REQUESTS")
        assertEquals(R.string.error_auth_too_many_requests, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps ERROR_OPERATION_NOT_ALLOWED to operation not allowed string res`() {
        val e = mockFirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED")
        assertEquals(R.string.error_auth_operation_not_allowed, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps ERROR_REQUIRES_RECENT_LOGIN to requires recent login string res`() {
        val e = mockFirebaseAuthException("ERROR_REQUIRES_RECENT_LOGIN")
        assertEquals(R.string.error_auth_requires_recent_login, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps unknown Firebase error code to generic error string res`() {
        val e = mockFirebaseAuthException("ERROR_SOME_UNKNOWN_CODE")
        assertEquals(R.string.error_auth_generic, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps FirebaseNetworkException to network error string res`() {
        val e = mockk<FirebaseNetworkException>()
        assertEquals(R.string.error_auth_network, e.toAuthErrorStringRes())
    }

    @Test
    fun `maps generic RuntimeException to generic error string res`() {
        val e = RuntimeException("Some English error")
        assertEquals(R.string.error_auth_generic, e.toAuthErrorStringRes())
    }

    private fun mockFirebaseAuthException(errorCode: String): FirebaseAuthException =
        mockk<FirebaseAuthException>().also { every { it.errorCode } returns errorCode }
}
