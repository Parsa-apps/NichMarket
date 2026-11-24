package com.parsaapps.nichmarket.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.parsaapps.nichmarket.ui.screens.about.AboutScreen
import com.parsaapps.nichmarket.ui.screens.bookmarks.BookmarksScreen
import com.parsaapps.nichmarket.ui.screens.category.CategoryScreen
import com.parsaapps.nichmarket.ui.screens.exam.ExamScreen
import com.parsaapps.nichmarket.ui.screens.home.HomeScreen
import com.parsaapps.nichmarket.ui.screens.result.ResultScreen
import com.parsaapps.nichmarket.ui.screens.results.ResultsScreen

object Routes {
    const val HOME = "home"
    const val BOOKMARKS = "bookmarks"
    const val RESULTS = "results"
    const val ABOUT = "about"
    const val CATEGORY = "category/{catId}"
    const val EXAM = "exam/{packId}"
    const val RESULT = "result/{resultId}"

    fun category(catId: String) = "category/$catId"
    fun exam(packId: String) = "exam/$packId"
    fun result(resultId: String) = "result/$resultId"
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppRoot() {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomItems = listOf(
        BottomItem(Routes.HOME, "خانه", Icons.Filled.Home),
        BottomItem(Routes.BOOKMARKS, "علاقه‌مندی‌ها", Icons.Filled.Favorite),
        BottomItem(Routes.RESULTS, "کارنامه", Icons.Filled.List),
        BottomItem(Routes.ABOUT, "درباره", Icons.Filled.Info)
    )
    val showBottomBar = currentRoute in bottomItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onCategoryClick = { catId -> navController.navigate(Routes.category(catId)) }
                )
            }
            composable(Routes.CATEGORY) { entry ->
                val catId = entry.arguments?.getString("catId").orEmpty()
                CategoryScreen(
                    catId = catId,
                    onBack = { navController.popBackStack() },
                    onStartExam = { packId -> navController.navigate(Routes.exam(packId)) }
                )
            }
            composable(Routes.EXAM) { entry ->
                val packId = entry.arguments?.getString("packId").orEmpty()
                ExamScreen(
                    packId = packId,
                    onExit = { navController.popBackStack() },
                    onFinished = { resultId -> navController.navigate(Routes.result(resultId)) }
                )
            }
            composable(Routes.RESULT) { entry ->
                val resultId = entry.arguments?.getString("resultId").orEmpty()
                ResultScreen(
                    resultId = resultId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.RESULTS) {
                ResultsScreen(
                    onOpenResult = { resultId -> navController.navigate(Routes.result(resultId)) }
                )
            }
            composable(Routes.BOOKMARKS) {
                BookmarksScreen()
            }
            composable(Routes.ABOUT) {
                AboutScreen()
            }
        }
    }
}
