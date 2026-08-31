package idv.wennyli.bloodpressurelog.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import idv.wennyli.bloodpressurelog.ui.view.login.EmailVerificationScreen
import idv.wennyli.bloodpressurelog.ui.view.login.ForgotPasswordScreen
import idv.wennyli.bloodpressurelog.ui.view.login.LoginScreen
import idv.wennyli.bloodpressurelog.ui.view.login.RegisterScreen

/**
 * 未登入區的導航子圖：登入、註冊、忘記密碼、Email 驗證。
 */
fun NavGraphBuilder.authGraph(navController: NavController) {

    composable<Destination.Login> {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(Destination.AddRecord) {
                    popUpTo<Destination.Login> { inclusive = true }
                }
            },
            onNavigateToRegister = { navController.navigate(Destination.Register) },
            onNavigateToForgotPassword = { navController.navigate(Destination.ForgotPassword) },
            onNavigateToEmailVerification = {
                navController.navigate(Destination.EmailVerification) {
                    popUpTo<Destination.Login> { inclusive = true }
                }
            },
        )
    }

    composable<Destination.Register> {
        RegisterScreen(
            onRegisterSuccess = {
                navController.navigate(Destination.EmailVerification) {
                    popUpTo<Destination.Login> { inclusive = true }
                }
            },
            onNavigateToLogin = { navController.popBackStack() },
        )
    }

    composable<Destination.ForgotPassword> {
        ForgotPasswordScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }

    composable<Destination.EmailVerification> {
        EmailVerificationScreen(
            onNavigateToLogin = {
                navController.navigate(Destination.Login) {
                    popUpTo(0) { inclusive = true }
                }
            },
        )
    }
}
