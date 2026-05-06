package idv.wennyli.bloodpressurelog.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException

fun Exception.toChineseAuthErrorMessage(): String {
    if (this is FirebaseNetworkException) return "網路連線失敗，請檢查網路設定"
    if (this is FirebaseAuthException) {
        return when (errorCode) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD" -> "帳號或密碼錯誤，請重新輸入"
            "ERROR_INVALID_EMAIL" -> "Email 格式不正確"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "此 Email 已被其他帳號使用"
            "ERROR_WEAK_PASSWORD" -> "密碼強度不足，請使用至少 6 位字元"
            "ERROR_USER_DISABLED" -> "此帳號已被停用"
            "ERROR_TOO_MANY_REQUESTS" -> "嘗試次數過多，請稍後再試"
            "ERROR_OPERATION_NOT_ALLOWED" -> "此登入方式目前不開放"
            "ERROR_REQUIRES_RECENT_LOGIN" -> "為保障安全，請重新登入後再試"
            else -> "操作失敗，請稍後再試"
        }
    }
    return "操作失敗，請稍後再試"
}
