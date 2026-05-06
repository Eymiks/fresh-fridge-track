package com.freshtrack.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
    const val LOADING = "loading"
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

    // Multi-scan : barcode → date → formulaire → "Ajouter & scanner le suivant"
    const val BARCODE_SCANNER_MULTI = "barcode_scanner_multi"
    const val DATE_SCANNER_MULTI = "date_scanner_multi/{barcode}"
    const val ADD_PRODUCT_MULTI = "add_product_multi/{barcode}/{date}"

    fun productDetail(id: String) = "product/$id"
    fun dateScanner(barcode: String = "") = "date_scanner/$barcode"
    fun addProduct(barcode: String = "") =
        if (barcode.isBlank()) ADD_PRODUCT_MANUAL else "add_product/$barcode"
    fun editProduct(id: String) = "edit_product/$id"
    fun dateScannerMulti(barcode: String) = "date_scanner_multi/$barcode"
    fun addProductMulti(barcode: String, date: String) =
        "add_product_multi/${barcode.ifBlank { "-" }}/${date.replace("/", "_").ifBlank { "-" }}"
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val targetRoot = when (val state = authState) {
        is AuthState.Loading -> null
        is AuthState.NotAuthenticated -> Routes.AUTH
        is AuthState.Guest -> Routes.MAIN
        is AuthState.Authenticated -> if (state.household == null) Routes.HOUSEHOLD_SETUP else Routes.MAIN
    }

    LaunchedEffect(targetRoot, currentRoute) {
        val route = targetRoot ?: return@LaunchedEffect
        val current = currentRoute
        val shouldNavigate = when (route) {
            Routes.AUTH -> current != Routes.AUTH
            Routes.HOUSEHOLD_SETUP -> current != Routes.HOUSEHOLD_SETUP
            Routes.MAIN -> current == null || current == Routes.LOADING ||
                current == Routes.AUTH || current == Routes.HOUSEHOLD_SETUP
            else -> current != route
        }

        if (shouldNavigate) {
            navController.navigate(route) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOADING) {
        composable(Routes.LOADING) { LoadingScreen() }
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
        composable(Routes.BARCODE_SCANNER_MULTI) {
            BarcodeScannerScreen(navController, isMultiScan = true)
        }
        composable(Routes.DATE_SCANNER_MULTI) { back ->
            DateScannerScreen(
                navController,
                barcode = back.arguments?.getString("barcode") ?: "",
                isMultiScan = true
            )
        }
        composable(Routes.ADD_PRODUCT_MULTI) { back ->
            val rawDate = back.arguments?.getString("date") ?: "-"
            AddProductScreen(
                navController,
                barcode = (back.arguments?.getString("barcode") ?: "-").let { if (it == "-") "" else it },
                isMultiMode = true,
                initialDate = if (rawDate == "-") "" else rawDate.replace("_", "/")
            )
        }
        composable(Routes.HOUSEHOLD_SETTINGS) { HouseholdSettingsScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.CREDITS) { CreditsScreen(navController) }
    }

    GuestImportPrompt()
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MainScreen(rootNavController: androidx.navigation.NavController) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
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
                    onMultiScanClick = { rootNavController.navigate(Routes.BARCODE_SCANNER_MULTI) },
                    onSettingsClick = { rootNavController.navigate(Routes.SETTINGS) },
                    onEditProduct = { id -> rootNavController.navigate(Routes.editProduct(id)) }
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
