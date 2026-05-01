package com.freshtrack.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.freshtrack.data.auth.AuthState
import com.freshtrack.ui.screens.auth.AuthScreen
import com.freshtrack.ui.screens.auth.AuthViewModel
import com.freshtrack.ui.screens.credits.CreditsScreen
import com.freshtrack.ui.screens.detail.ProductDetailScreen
import com.freshtrack.ui.screens.history.HistoryScreen
import com.freshtrack.ui.screens.household.HouseholdSettingsScreen
import com.freshtrack.ui.screens.household.HouseholdSetupScreen
import com.freshtrack.ui.screens.index.IndexScreen
import com.freshtrack.ui.screens.notifications.NotificationsScreen
import com.freshtrack.ui.screens.scanner.BarcodeScannerScreen
import com.freshtrack.ui.screens.scanner.DateScannerScreen
import com.freshtrack.ui.screens.settings.SettingsScreen
import com.freshtrack.ui.screens.add.AddProductScreen
import com.freshtrack.ui.screens.stats.StatsScreen

object Routes {
    const val AUTH = "auth"
    const val HOUSEHOLD_SETUP = "household_setup"
    const val MAIN = "main"
    const val INDEX = "index"
    const val STATS = "stats"
    const val HISTORY = "history"
    const val NOTIFICATIONS = "notifications"
    const val HOUSEHOLD_SETTINGS = "household_settings"
    const val SETTINGS = "settings"
    const val CREDITS = "credits"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val BARCODE_SCANNER = "barcode_scanner"
    const val DATE_SCANNER = "date_scanner/{barcode}"
    const val ADD_PRODUCT_MANUAL = "add_product"
    const val ADD_PRODUCT = "add_product/{barcode}"
    const val EDIT_PRODUCT = "edit_product/{productId}"

    fun productDetail(id: String) = "product/$id"
    fun dateScanner(barcode: String = "") = "date_scanner/$barcode"
    fun addProduct(barcode: String = "") =
        if (barcode.isBlank()) ADD_PRODUCT_MANUAL else "add_product/$barcode"
    fun editProduct(id: String) = "edit_product/$id"
}

data class BottomTab(val route: String, val label: String, val icon: @Composable () -> Unit)

val bottomTabs = listOf(
    BottomTab(Routes.INDEX, "Frigo") { Icon(Icons.Default.Home, null) },
    BottomTab(Routes.STATS, "Stats") { Icon(Icons.Default.BarChart, null) },
    BottomTab(Routes.HISTORY, "Historique") { Icon(Icons.Default.History, null) },
    BottomTab(Routes.NOTIFICATIONS, "Alertes") { Icon(Icons.Default.Notifications, null) }
)

@Composable
fun AppNavigation() {
    val authVm: AuthViewModel = hiltViewModel()
    val authState by authVm.authState.collectAsState()

    val navController = rememberNavController()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Loading -> {}
            is AuthState.NotAuthenticated -> navController.navigate(Routes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
            is AuthState.Guest -> navController.navigate(Routes.MAIN) {
                popUpTo(0) { inclusive = true }
            }
            is AuthState.Authenticated -> {
                val dest = if (state.household == null) Routes.HOUSEHOLD_SETUP else Routes.MAIN
                navController.navigate(dest) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) { AuthScreen(navController) }
        composable(Routes.HOUSEHOLD_SETUP) { HouseholdSetupScreen(navController) }
        composable(Routes.MAIN) { MainScreen(navController) }
        composable(Routes.PRODUCT_DETAIL) { back ->
            ProductDetailScreen(navController, back.arguments?.getString("productId") ?: "")
        }
        composable(Routes.BARCODE_SCANNER) { BarcodeScannerScreen(navController) }
        composable(Routes.DATE_SCANNER) { back ->
            DateScannerScreen(navController, back.arguments?.getString("barcode") ?: "")
        }
        composable(Routes.ADD_PRODUCT_MANUAL) {
            AddProductScreen(navController, "")
        }
        composable(Routes.ADD_PRODUCT) { back ->
            AddProductScreen(navController, back.arguments?.getString("barcode") ?: "")
        }
        composable(Routes.EDIT_PRODUCT) {
            AddProductScreen(navController, "")
        }
        composable(Routes.HOUSEHOLD_SETTINGS) { HouseholdSettingsScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.CREDITS) { CreditsScreen(navController) }
    }

    GuestImportPrompt()
}

@Composable
private fun MainScreen(rootNavController: androidx.navigation.NavController) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = tab.icon,
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = Routes.INDEX,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.INDEX) {
                IndexScreen(
                    onProductClick = { id -> rootNavController.navigate(Routes.productDetail(id)) },
                    onScanClick = { rootNavController.navigate(Routes.BARCODE_SCANNER) },
                    onSettingsClick = { rootNavController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.HISTORY) {
                HistoryScreen(onProductClick = { id -> rootNavController.navigate(Routes.productDetail(id)) })
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onProductClick = { id -> rootNavController.navigate(Routes.productDetail(id)) })
            }
        }
    }
}
