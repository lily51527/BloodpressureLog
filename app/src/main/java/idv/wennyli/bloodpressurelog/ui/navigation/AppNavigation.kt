package idv.wennyli.bloodpressurelog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import idv.wennyli.bloodpressurelog.ui.view.login.LoginScreen
import idv.wennyli.bloodpressurelog.ui.view.main.MainViewModel
import idv.wennyli.bloodpressurelog.ui.view.record.AddEditRecordScreen
import idv.wennyli.bloodpressurelog.ui.view.record.RecordListScreen

private const val ROUTE_LOGIN = "login"
private const val ROUTE_RECORD_LIST = "record_list"
private const val ROUTE_ADD_EDIT_RECORD = "add_edit_record"
private const val ARG_RECORD_ID = "recordId"

@Composable
fun AppNavigation(startLoggedIn: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (startLoggedIn) ROUTE_RECORD_LIST else ROUTE_LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(ROUTE_RECORD_LIST) {
                        popUpTo(ROUTE_LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(ROUTE_RECORD_LIST) {
            val mainViewModel: MainViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                mainViewModel.signedOut.collect {
                    navController.navigate(ROUTE_LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            RecordListScreen(
                onSignOut = mainViewModel::signOut,
                onNavigateToAddEdit = { recordId ->
                    val route = if (recordId != null) {
                        "$ROUTE_ADD_EDIT_RECORD?$ARG_RECORD_ID=$recordId"
                    } else {
                        ROUTE_ADD_EDIT_RECORD
                    }
                    navController.navigate(route)
                },
            )
        }

        composable(
            route = "$ROUTE_ADD_EDIT_RECORD?$ARG_RECORD_ID={$ARG_RECORD_ID}",
            arguments = listOf(
                navArgument(ARG_RECORD_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            AddEditRecordScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
