package com.example.safefood.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.safefood.ui.SafeFoodViewModel
import com.example.safefood.ui.screens.categories.CategoriesScreen
import com.example.safefood.ui.screens.export.ExportScreen
import com.example.safefood.ui.screens.menuplanner.MenuPlannerScreen
import com.example.safefood.ui.screens.nutrients.NutrientsScreen
import com.example.safefood.ui.screens.safefood.SafeFoodScreen
import com.example.safefood.ui.screens.shopping.ShoppingListScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Categories : Screen("categories", "Categories", Icons.Default.Category)
    object SafeFood : Screen("safe_food", "SafeFood", Icons.Default.Verified)
    object Nutrients : Screen("nutrients", "Nutrients", Icons.Default.Spa)
    object AiMenu : Screen("ai_menu", "AI Menu", Icons.Default.AutoAwesome)
    object Shopping : Screen("shopping", "Shopping", Icons.Default.ShoppingCart)
    object Export : Screen("export", "Export CSV", Icons.Default.FileDownload)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SafeFoodViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val bottomNavItems = listOf(
        Screen.Categories,
        Screen.SafeFood,
        Screen.Nutrients,
        Screen.AiMenu,
        Screen.Shopping
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SafeFood",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (currentRoute != Screen.Export.route) {
                                navController.navigate(Screen.Export.route)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Categories.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Categories.route) {
                CategoriesScreen(viewModel = viewModel)
            }
            composable(Screen.SafeFood.route) {
                SafeFoodScreen(viewModel = viewModel)
            }
            composable(Screen.Nutrients.route) {
                NutrientsScreen(viewModel = viewModel)
            }
            composable(Screen.AiMenu.route) {
                MenuPlannerScreen(viewModel = viewModel)
            }
            composable(Screen.Shopping.route) {
                ShoppingListScreen(viewModel = viewModel)
            }
            composable(Screen.Export.route) {
                ExportScreen(viewModel = viewModel)
            }
        }
    }
}
