package com.example.melody.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.melody.feature.auth.AuthViewModel
import com.example.melody.feature.auth.login.LoginScreen
import com.example.melody.feature.auth.recovery.ForgotPasswordScreen
import com.example.melody.feature.auth.recovery.ResetPasswordScreen
import com.example.melody.feature.home.HomeScreen
import com.example.melody.feature.library.LibraryScreen
import com.example.melody.feature.profile.ProfileScreen
import com.example.melody.feature.search.SearchScreen
import com.example.melody.feature.auth.register.RegisterScreen
import com.example.melody.feature.auth.verification.VerifyEmailScreen

@Composable
fun MelodyNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = MelodyRoute.Home.route,
        modifier = modifier,
    ) {
        composable(MelodyRoute.Home.route) { HomeScreen() }
        composable(MelodyRoute.Search.route) { SearchScreen() }
        composable(MelodyRoute.Library.route) { LibraryScreen() }
        composable(MelodyRoute.Profile.route) {
            ProfileScreen(
                onLoginClick = { navController.navigate(AuthRoute.Login) },
                onRegisterClick = { navController.navigate(AuthRoute.Register) },
            )
        }
        composable(AuthRoute.Login) {
            LoginScreen(
                viewModel = authViewModel,
                onRegisterClick = { navController.navigate(AuthRoute.Register) },
                onForgotPasswordClick = { navController.navigate(AuthRoute.ForgotPassword) },
                onLoginSuccess = {
                    navController.navigate(MelodyRoute.Home.route) {
                        popUpTo(MelodyRoute.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(AuthRoute.ForgotPassword) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onBackToLogin = {
                    authViewModel.clearRecoveryState()
                    navController.popBackStack()
                },
                onContinueToReset = { email ->
                    navController.navigate("${AuthRoute.ResetPasswordBase}/${android.net.Uri.encode(email)}")
                },
            )
        }
        composable(
            route = AuthRoute.ResetPassword,
            arguments = listOf(navArgument(AuthRoute.EmailArgument) { type = NavType.StringType }),
        ) { entry ->
            val email = entry.arguments?.getString(AuthRoute.EmailArgument).orEmpty()
            ResetPasswordScreen(
                email = email,
                viewModel = authViewModel,
                onBack = {
                    authViewModel.clearPasswordResetState()
                    navController.popBackStack()
                },
                onResetSuccess = {
                    navController.navigate(AuthRoute.Login) {
                        popUpTo(AuthRoute.Login) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(AuthRoute.Register) {
            RegisterScreen(
                viewModel = authViewModel,
                onVerificationRequired = { email ->
                    navController.navigate("${AuthRoute.VerifyEmailBase}/${android.net.Uri.encode(email)}")
                },
            )
        }
        composable(
            route = AuthRoute.VerifyEmail,
            arguments = listOf(navArgument(AuthRoute.EmailArgument) { type = NavType.StringType }),
        ) { entry ->
            val email = entry.arguments?.getString(AuthRoute.EmailArgument).orEmpty()
            VerifyEmailScreen(email = email, viewModel = authViewModel)
        }
    }
}

object AuthRoute {
    const val Login = "login"
    const val ForgotPassword = "forgot-password"
    const val ResetPasswordBase = "reset-password"
    const val EmailArgument = "email"
    const val ResetPassword = "$ResetPasswordBase/{$EmailArgument}"
    const val Register = "register"
    const val VerifyEmailBase = "verify-email"
    const val VerifyEmail = "$VerifyEmailBase/{$EmailArgument}"
}
