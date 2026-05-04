package idv.wennyli.bloodpressurelog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import idv.wennyli.bloodpressurelog.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import idv.wennyli.bloodpressurelog.ui.view.login.EmailVerificationScreen
import idv.wennyli.bloodpressurelog.ui.view.login.ForgotPasswordScreen
import idv.wennyli.bloodpressurelog.ui.view.login.LoginScreen
import idv.wennyli.bloodpressurelog.ui.view.login.RegisterScreen
import idv.wennyli.bloodpressurelog.ui.view.main.MainViewModel
import idv.wennyli.bloodpressurelog.ui.view.record.AddEditRecordScreen
import idv.wennyli.bloodpressurelog.ui.view.record.RecordListScreen
import idv.wennyli.bloodpressurelog.ui.view.trends.TrendsScreen

private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"
private const val ROUTE_EMAIL_VERIFICATION = "email_verification"
private const val ROUTE_FORGOT_PASSWORD = "forgot_password"
private const val ROUTE_RECORD_LIST = "record_list"
private const val ROUTE_TRENDS = "trends"
private const val ROUTE_ADD_EDIT_RECORD = "add_edit_record"
private const val ARG_RECORD_ID = "recordId"

private val BOTTOM_NAV_ROUTES = setOf(ROUTE_RECORD_LIST, ROUTE_TRENDS)

@Composable
fun AppNavigation(startLoggedIn: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (startLoggedIn) ROUTE_RECORD_LIST else ROUTE_LOGIN
    val mainViewModel: MainViewModel = hiltViewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        mainViewModel.authState.collect { user ->
            if (user == null && navController.currentDestination?.route != ROUTE_LOGIN) {
                navController.navigate(ROUTE_LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in BOTTOM_NAV_ROUTES) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_RECORD_LIST,
                        onClick = {
                            navController.navigate(ROUTE_RECORD_LIST) {
                                popUpTo(ROUTE_RECORD_LIST) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.List, contentDescription = stringResource(R.string.nav_label_records)) },
                        label = { Text(stringResource(R.string.nav_label_records)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_TRENDS,
                        onClick = {
                            navController.navigate(ROUTE_TRENDS) {
                                popUpTo(ROUTE_RECORD_LIST) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.nav_label_trends)) },
                        label = { Text(stringResource(R.string.nav_label_trends)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ROUTE_LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(ROUTE_RECORD_LIST) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(ROUTE_REGISTER)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(ROUTE_FORGOT_PASSWORD)
                    },
                    onNavigateToEmailVerification = {
                        navController.navigate(ROUTE_EMAIL_VERIFICATION) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    },
                )
            }

            composable(ROUTE_REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(ROUTE_EMAIL_VERIFICATION) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                )
            }

            composable(ROUTE_FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable(ROUTE_EMAIL_VERIFICATION) {
                EmailVerificationScreen(
                    onNavigateToLogin = {
                        navController.navigate(ROUTE_LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            composable(ROUTE_RECORD_LIST) {
                RecordListScreen(
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

            composable(ROUTE_TRENDS) {
                TrendsScreen()
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
}
