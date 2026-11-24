package com.example.melody.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.melody.feature.auth.AuthViewModel
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
            ProfileScreen(onRegisterClick = { navController.navigate(AuthRoute.Register) })
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
    const val Register = "register"
    const val VerifyEmailBase = "verify-email"
    const val EmailArgument = "email"
    const val VerifyEmail = "$VerifyEmailBase/{$EmailArgument}"
}
