package com.example.safefood.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import com.example.safefood.ui.components.FoodPhoto
import com.example.safefood.ui.components.StarRatingBar
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.safefood.ui.SafeFoodViewModel
import com.example.safefood.ui.screens.categories.CategoriesScreen
import com.example.safefood.ui.screens.export.ExportScreen
import com.example.safefood.ui.screens.menuplanner.MenuPlannerScreen
import com.example.safefood.ui.screens.safefood.SafeFoodScreen
import com.example.safefood.ui.screens.shopping.ShoppingListScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Categories : Screen("categories", "Categories", Icons.Default.Category)
    object SafeFood : Screen("safe_food", "SafeFood", Icons.Default.Verified)
    object AiMenu : Screen("ai_menu", "Menu", Icons.Default.AutoAwesome)
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
                            if (currentRoute == Screen.Export.route) {
                                navController.popBackStack()
                            } else {
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
        // Startup Rate Prompt Dialog
        val showStartupRatePrompt by viewModel.showStartupRatePrompt.collectAsState()
        if (showStartupRatePrompt) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissStartupRatePrompt() },
                title = { Text("Food preference check-in") },
                text = { Text("Some foods you rated are older than 5 days. Want to quickly re-rate a few so menus stay accurate?") },
                confirmButton = {
                    Button(onClick = { viewModel.startRandomRating() }) {
                        Text("Let's Rate!")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissStartupRatePrompt() }) {
                        Text("Decline")
                    }
                }
            )
        }

        // Active Random Rating Dialog
        val activeRandomFood by viewModel.activeRandomFood.collectAsState()
        activeRandomFood?.let { food ->
            val categories by viewModel.categories.collectAsState()
            val categoryName = categories.find { it.id == food.categoryId }?.name ?: "Food Group"

            AlertDialog(
                onDismissRequest = { viewModel.stopRandomRating() },
                title = { Text("Quick rate") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Group: $categoryName", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(food.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        FoodPhoto(
                            food = food,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            cornerRadius = 12.dp
                        )
                        if (food.notes.isNotBlank()) {
                            Text(food.notes, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Do you like this food?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        StarRatingBar(
                            rating = food.rating,
                            onRatingSelected = { newRating ->
                                viewModel.rateRandomFoodAndNext(food.id, newRating)
                            },
                            showLabel = true,
                            starSize = 28.dp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.startRandomRating()
                    }) {
                        Text("Skip")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.stopRandomRating() }) {
                        Text("Stop Rating")
                    }
                }
            )
        }

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
