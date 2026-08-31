package idv.wennyli.bloodpressurelog.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import idv.wennyli.bloodpressurelog.ui.view.record.AddEditRecordScreen
import idv.wennyli.bloodpressurelog.ui.view.record.RecordListScreen
import idv.wennyli.bloodpressurelog.ui.view.trends.TrendsScreen

/**
 * 已登入區的導航子圖：新增（bottom nav 分頁）、紀錄列表、趨勢圖表、編輯既有紀錄。
 *
 * 新增與編輯共用 [AddEditRecordScreen]；`recordId == null` 即新增模式。
 */
fun NavGraphBuilder.recordGraph(navController: NavController) {

    composable<Destination.AddRecord> {
        AddEditRecordScreen(
            recordId = null,
            onNavigateBack = { navController.popBackStack() },
        )
    }

    composable<Destination.RecordList> {
        RecordListScreen(
            onNavigateToEdit = { recordId ->
                navController.navigate(Destination.AddEditRecord(recordId))
            },
        )
    }

    composable<Destination.Trends> {
        TrendsScreen()
    }

    composable<Destination.AddEditRecord> { backStackEntry ->
        val route = backStackEntry.toRoute<Destination.AddEditRecord>()
        AddEditRecordScreen(
            recordId = route.recordId,
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
