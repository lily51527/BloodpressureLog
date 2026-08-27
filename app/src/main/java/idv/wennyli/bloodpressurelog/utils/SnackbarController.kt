package idv.wennyli.bloodpressurelog.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * App 層級的 Snackbar 訊息匯流排。
 *
 * 讓各畫面的 ViewModel 不需依賴 Compose 的 [androidx.compose.material3.SnackbarHostState]，
 * 只需呼叫 [sendMessage] 送出提示文字；實際顯示由 App 殼層（[idv.wennyli.bloodpressurelog.ui.navigation.AppNavigation]）
 * 統一監聽 [messages] 並呼叫 showSnackbar，生命週期不受單一畫面銷毀影響。
 */
interface SnackbarController {
    val messages: SharedFlow<String>
    suspend fun sendMessage(message: String)
}

class SnackbarControllerImpl @Inject constructor() : SnackbarController {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val messages: SharedFlow<String> = _messages.asSharedFlow()

    override suspend fun sendMessage(message: String) {
        _messages.emit(message)
    }
}
