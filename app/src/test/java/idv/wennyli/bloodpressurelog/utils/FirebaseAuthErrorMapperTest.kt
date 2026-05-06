package idv.wennyli.bloodpressurelog.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseAuthErrorMapperTest {

    @Test
    fun `maps ERROR_INVALID_CREDENTIAL to Chinese message`() {
        val e = mockFirebaseAuthException("ERROR_INVALID_CREDENTIAL")
        assertEquals("帳號或密碼錯誤，請重新輸入", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps ERROR_USER_NOT_FOUND to Chinese message`() {
        val e = mockFirebaseAuthException("ERROR_USER_NOT_FOUND")
        assertEquals("帳號或密碼錯誤，請重新輸入", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps ERROR_WRONG_PASSWORD to Chinese message`() {
        val e = mockFirebaseAuthException("ERROR_WRONG_PASSWORD")
        assertEquals("帳號或密碼錯誤，請重新輸入", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps ERROR_INVALID_EMAIL to Chinese message`() {
        val e = mockFirebaseAuthException("ERROR_INVALID_EMAIL")
        assertEquals("Email 格式不正確", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps ERROR_EMAIL_ALREADY_IN_USE to Chinese message`() {
        val e = mockFirebaseAuthException("ERROR_EMAIL_ALREADY_IN_USE")
        assertEquals("此 Email 已被其他帳號使用", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps ERROR_WEAK_PASSWORD to Chinese message`() {
        val e = mockFirebaseAuthException("ERROR_WEAK_PASSWORD")
        assertEquals("密碼強度不足，請使用至少 6 位字元", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps ERROR_USER_DISABLED to Chinese message`() {
        val e = mockFirebaseAuthException("ERROR_USER_DISABLED")
        assertEquals("此帳號已被停用", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps ERROR_TOO_MANY_REQUESTS to Chinese message`() {
        val e = mockFirebaseAuthException("ERROR_TOO_MANY_REQUESTS")
        assertEquals("嘗試次數過多，請稍後再試", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps ERROR_OPERATION_NOT_ALLOWED to Chinese message`() {
        val e = mockFirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED")
        assertEquals("此登入方式目前不開放", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps ERROR_REQUIRES_RECENT_LOGIN to Chinese message`() {
        val e = mockFirebaseAuthException("ERROR_REQUIRES_RECENT_LOGIN")
        assertEquals("為保障安全，請重新登入後再試", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps unknown Firebase error code to fallback message`() {
        val e = mockFirebaseAuthException("ERROR_SOME_UNKNOWN_CODE")
        assertEquals("操作失敗，請稍後再試", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps FirebaseNetworkException to network error message`() {
        val e = mockk<FirebaseNetworkException>()
        assertEquals("網路連線失敗，請檢查網路設定", e.toChineseAuthErrorMessage())
    }

    @Test
    fun `maps generic RuntimeException to fallback message`() {
        val e = RuntimeException("Some English error")
        assertEquals("操作失敗，請稍後再試", e.toChineseAuthErrorMessage())
    }

    private fun mockFirebaseAuthException(errorCode: String): FirebaseAuthException =
        mockk<FirebaseAuthException>().also { every { it.errorCode } returns errorCode }
}
