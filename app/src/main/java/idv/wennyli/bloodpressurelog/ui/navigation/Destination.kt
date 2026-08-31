package idv.wennyli.bloodpressurelog.ui.navigation

import kotlinx.serialization.Serializable

/**
 * App 內所有導航目的地的 type-safe 定義。
 *
 * 以 `@Serializable` 型別取代字串 route，navigate 呼叫與參數存取皆為編譯期檢查。
 * 分組用 sealed interface（官方 type-safe navigation 指南 best practice）。
 */
sealed interface Destination {

    /** 標記需登入才能存取的目的地，供 [AppNavigator] 攔截未登入的導航。 */
    sealed interface RequiresAuth : Destination

    @Serializable
    data object Login : Destination

    @Serializable
    data object Register : Destination

    @Serializable
    data object ForgotPassword : Destination

    @Serializable
    data object EmailVerification : Destination

    /** bottom nav「新增」分頁：連續新增模式。 */
    @Serializable
    data object AddRecord : RequiresAuth

    @Serializable
    data object RecordList : RequiresAuth

    @Serializable
    data object Trends : RequiresAuth

    /** 編輯既有血壓紀錄（由清單進入）。 */
    @Serializable
    data class AddEditRecord(val recordId: String) : RequiresAuth
}
