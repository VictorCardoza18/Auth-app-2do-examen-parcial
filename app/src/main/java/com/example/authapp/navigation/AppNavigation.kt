package com.example.authapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.authapp.ui.screens.*
import com.example.authapp.viewmodel.AuthViewModel
import com.example.authapp.viewmodel.NotificationViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object UserHome : Screen("userHome")
    object AdminHome : Screen("adminHome")
    object Notifications : Screen("notifications")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    notificationViewModel: NotificationViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(authState, currentUser) {
        when (authState) {
            is AuthViewModel.AuthState.Authenticated -> {
                // Verificamos explícitamente el valor de admin
                if (currentUser?.admin == true) {
                    navController.navigate(Screen.AdminHome.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.UserHome.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
            is AuthViewModel.AuthState.Unauthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
            else -> {
                // No hacemos nada en otros estados
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                viewModel = authViewModel
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }
        composable(Screen.UserHome.route) {
            UserHomeScreen(
                onSignOut = {
                    authViewModel.signOut()
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                viewModel = authViewModel
            )
        }
        composable(Screen.AdminHome.route) {
            AdminHomeScreen(
                onSignOut = {
                    authViewModel.signOut()
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                viewModel = authViewModel,
                notificationViewModel = notificationViewModel
            )
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = notificationViewModel
            )
        }
    }
}