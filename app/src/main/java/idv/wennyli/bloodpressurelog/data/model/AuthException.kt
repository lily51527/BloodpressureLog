package idv.wennyli.bloodpressurelog.data.model

/**
 * AuthRepositoryImpl 將 Firebase 例外轉換為本地化錯誤訊息後，
 * 以此例外拋出，讓呼叫方（ViewModel）能直接取得可顯示的 message。
 */
class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)
