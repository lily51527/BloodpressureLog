package idv.wennyli.bloodpressurelog.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import idv.wennyli.bloodpressurelog.R
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test

class FirebaseAuthErrorMapperTest {

    /** ERROR_INVALID_CREDENTIAL 錯誤碼應對應到憑證無效的字串資源。 */
    @Test
    fun `maps ERROR_INVALID_CREDENTIAL to invalid credential string res`() {
        val e = mockFirebaseAuthException("ERROR_INVALID_CREDENTIAL")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_invalid_credential)
    }

    /** ERROR_USER_NOT_FOUND 錯誤碼應對應到憑證無效的字串資源（避免揭露帳號是否存在）。 */
    @Test
    fun `maps ERROR_USER_NOT_FOUND to invalid credential string res`() {
        val e = mockFirebaseAuthException("ERROR_USER_NOT_FOUND")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_invalid_credential)
    }

    /** ERROR_WRONG_PASSWORD 錯誤碼應對應到憑證無效的字串資源（避免揭露密碼錯誤細節）。 */
    @Test
    fun `maps ERROR_WRONG_PASSWORD to invalid credential string res`() {
        val e = mockFirebaseAuthException("ERROR_WRONG_PASSWORD")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_invalid_credential)
    }

    /** ERROR_INVALID_EMAIL 錯誤碼應對應到 email 格式無效的字串資源。 */
    @Test
    fun `maps ERROR_INVALID_EMAIL to invalid email string res`() {
        val e = mockFirebaseAuthException("ERROR_INVALID_EMAIL")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_invalid_email)
    }

    /** ERROR_EMAIL_ALREADY_IN_USE 錯誤碼應對應到 email 已被使用的字串資源。 */
    @Test
    fun `maps ERROR_EMAIL_ALREADY_IN_USE to email already in use string res`() {
        val e = mockFirebaseAuthException("ERROR_EMAIL_ALREADY_IN_USE")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_email_already_in_use)
    }

    /** ERROR_WEAK_PASSWORD 錯誤碼應對應到密碼強度不足的字串資源。 */
    @Test
    fun `maps ERROR_WEAK_PASSWORD to weak password string res`() {
        val e = mockFirebaseAuthException("ERROR_WEAK_PASSWORD")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_weak_password)
    }

    /** ERROR_USER_DISABLED 錯誤碼應對應到帳號已停用的字串資源。 */
    @Test
    fun `maps ERROR_USER_DISABLED to user disabled string res`() {
        val e = mockFirebaseAuthException("ERROR_USER_DISABLED")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_user_disabled)
    }

    /** ERROR_TOO_MANY_REQUESTS 錯誤碼應對應到請求過於頻繁的字串資源。 */
    @Test
    fun `maps ERROR_TOO_MANY_REQUESTS to too many requests string res`() {
        val e = mockFirebaseAuthException("ERROR_TOO_MANY_REQUESTS")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_too_many_requests)
    }

    /** ERROR_OPERATION_NOT_ALLOWED 錯誤碼應對應到操作不允許的字串資源。 */
    @Test
    fun `maps ERROR_OPERATION_NOT_ALLOWED to operation not allowed string res`() {
        val e = mockFirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_operation_not_allowed)
    }

    /** ERROR_REQUIRES_RECENT_LOGIN 錯誤碼應對應到需要重新登入的字串資源。 */
    @Test
    fun `maps ERROR_REQUIRES_RECENT_LOGIN to requires recent login string res`() {
        val e = mockFirebaseAuthException("ERROR_REQUIRES_RECENT_LOGIN")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_requires_recent_login)
    }

    /** 未知的 Firebase 錯誤碼應對應到通用錯誤的字串資源。 */
    @Test
    fun `maps unknown Firebase error code to generic error string res`() {
        val e = mockFirebaseAuthException("ERROR_SOME_UNKNOWN_CODE")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_generic)
    }

    /** FirebaseNetworkException 應對應到網路錯誤的字串資源。 */
    @Test
    fun `maps FirebaseNetworkException to network error string res`() {
        val e = mockk<FirebaseNetworkException>()
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_network)
    }

    /** 一般 RuntimeException 應對應到通用錯誤的字串資源。 */
    @Test
    fun `maps generic RuntimeException to generic error string res`() {
        val e = RuntimeException("Some English error")
        assertThat(e.toAuthErrorStringRes()).isEqualTo(R.string.error_auth_generic)
    }

    /** errorCode 為空字串的 FirebaseAuthException 應落到 else 分支，回傳通用錯誤字串資源。 */
    @Test
    fun `maps FirebaseAuthException with empty errorCode to generic error string res`() {
        val e = mockFirebaseAuthException("")

        val result = e.toAuthErrorStringRes()

        assertThat(result).isEqualTo(R.string.error_auth_generic)
    }

    /**
     * errorCode 為小寫的 FirebaseAuthException 應落到 else 分支，驗證 when 比對為大小寫敏感，
     * 回傳通用錯誤字串資源。
     */
    @Test
    fun `maps FirebaseAuthException with lowercase errorCode to generic error string res`() {
        val e = mockFirebaseAuthException("error_invalid_credential")

        val result = e.toAuthErrorStringRes()

        assertThat(result).isEqualTo(R.string.error_auth_generic)
    }

    /**
     * errorCode 含前後空白的 FirebaseAuthException 應落到 else 分支（不做 trim），
     * 回傳通用錯誤字串資源。
     */
    @Test
    fun `maps FirebaseAuthException with whitespace-padded errorCode to generic error string res`() {
        val e = mockFirebaseAuthException(" ERROR_INVALID_CREDENTIAL ")

        val result = e.toAuthErrorStringRes()

        assertThat(result).isEqualTo(R.string.error_auth_generic)
    }

    private fun mockFirebaseAuthException(errorCode: String): FirebaseAuthException =
        mockk<FirebaseAuthException>().also { every { it.errorCode } returns errorCode }
}
