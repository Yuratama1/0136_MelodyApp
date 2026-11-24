package com.example.melody.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.melody.feature.home.HomeScreen
import com.example.melody.feature.library.LibraryScreen
import com.example.melody.feature.profile.ProfileScreen
import com.example.melody.feature.search.SearchScreen

@Composable
fun MelodyNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = MelodyRoute.Home.route,
        modifier = modifier,
    ) {
        composable(MelodyRoute.Home.route) { HomeScreen() }
        composable(MelodyRoute.Search.route) { SearchScreen() }
        composable(MelodyRoute.Library.route) { LibraryScreen() }
        composable(MelodyRoute.Profile.route) { ProfileScreen() }
    }
}
