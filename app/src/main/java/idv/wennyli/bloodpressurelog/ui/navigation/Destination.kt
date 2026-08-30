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

    @Serializable
    data object RecordList : RequiresAuth

    @Serializable
    data object Trends : RequiresAuth

    /**
     * 血壓紀錄新增／編輯畫面。
     *
     * - [recordId] 為 `null`：新增模式（亦作為 bottom nav「新增」top-level 目的地）
     * - [recordId] 有值：編輯既有紀錄
     */
    @Serializable
    data class AddEditRecord(val recordId: String? = null) : RequiresAuth
}

/** bottom navigation 的三個 top-level 目的地，順序即顯示順序。 */
val TOP_LEVEL_DESTINATIONS: List<Destination> = listOf(
    Destination.AddEditRecord(),
    Destination.RecordList,
    Destination.Trends,
)
