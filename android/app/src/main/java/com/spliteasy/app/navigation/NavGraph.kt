package com.spliteasy.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.spliteasy.app.presentation.auth.LoginScreen
import com.spliteasy.app.presentation.auth.RegisterScreen
import com.spliteasy.app.presentation.dashboard.DashboardScreen
import com.spliteasy.app.presentation.group.CreateGroupScreen
import com.spliteasy.app.presentation.group.GroupDetailScreen
import com.spliteasy.app.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object Settings : Screen("settings")
    object CreateGroup : Screen("create_group")
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: Int) = "group_detail/$groupId"
    }
    object AddExpense : Screen("add_expense/{groupId}") {
        fun createRoute(groupId: Int) = "add_expense/$groupId"
    }
    object AddMembers : Screen("add_members/{groupId}") {
        fun createRoute(groupId: Int) = "add_members/$groupId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onCreateGroup = {
                    navController.navigate(Screen.CreateGroup.route)
                },
                onGroupClick = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onGroupCreated = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument("groupId") { type = NavType.IntType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: return@composable
            GroupDetailScreen(
                groupId = groupId,
                onBackClick = {
                    navController.popBackStack()
                },
                onAddExpense = {
                    navController.navigate(Screen.AddExpense.createRoute(groupId))
                },
                onAddMembers = {
                    navController.navigate(Screen.AddMembers.createRoute(groupId))
                }
            )
        }
    }
}
