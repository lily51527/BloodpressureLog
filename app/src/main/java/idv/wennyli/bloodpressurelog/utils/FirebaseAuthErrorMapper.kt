package idv.wennyli.bloodpressurelog.utils

import androidx.annotation.StringRes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import idv.wennyli.bloodpressurelog.R

@StringRes
fun Exception.toAuthErrorStringRes(): Int {
    if (this is FirebaseNetworkException) return R.string.error_auth_network
    if (this is FirebaseAuthException) {
        return when (errorCode) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD" -> R.string.error_auth_invalid_credential
            "ERROR_INVALID_EMAIL" -> R.string.error_auth_invalid_email
            "ERROR_EMAIL_ALREADY_IN_USE" -> R.string.error_auth_email_already_in_use
            "ERROR_WEAK_PASSWORD" -> R.string.error_auth_weak_password
            "ERROR_USER_DISABLED" -> R.string.error_auth_user_disabled
            "ERROR_TOO_MANY_REQUESTS" -> R.string.error_auth_too_many_requests
            "ERROR_OPERATION_NOT_ALLOWED" -> R.string.error_auth_operation_not_allowed
            "ERROR_REQUIRES_RECENT_LOGIN" -> R.string.error_auth_requires_recent_login
            else -> R.string.error_auth_generic
        }
    }
    return R.string.error_auth_generic
}
