package com.freshtrack.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.freshtrack.data.auth.AuthState
import com.freshtrack.ui.components.HamburgerMenuDrawer
import com.freshtrack.ui.components.OfflineBanner
import com.freshtrack.ui.screens.HamburgerMenuViewModel
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
import com.freshtrack.ui.theme.AppearanceViewModel
import com.freshtrack.ui.theme.ThemeMode

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
    const val DATE_SCANNER_ADD = "date_scanner_add/{barcode}"
    const val ADD_PRODUCT_MANUAL = "add_product"
    const val ADD_PRODUCT = "add_product/{barcode}"
    const val ADD_PRODUCT_SCANNED_DATE = "add_product/{barcode}/{date}"
    const val EDIT_PRODUCT = "edit_product/{productId}"

    // Multi-scan : barcode → date → formulaire → "Ajouter & scanner le suivant"
    const val BARCODE_SCANNER_MULTI = "barcode_scanner_multi"
    const val DATE_SCANNER_MULTI = "date_scanner_multi/{barcode}"
    const val ADD_PRODUCT_MULTI = "add_product_multi/{barcode}/{date}"

    fun productDetail(id: String) = "product/$id"
    fun dateScanner(barcode: String = "") = "date_scanner/$barcode"
    fun dateScannerAdd(barcode: String = "") = "date_scanner_add/${barcode.ifBlank { "-" }}"
    fun addProduct(barcode: String = "") =
        if (barcode.isBlank()) ADD_PRODUCT_MANUAL else "add_product/$barcode"
    fun addProductWithDate(barcode: String, date: String) =
        "add_product/${barcode.ifBlank { "-" }}/${date.replace("/", "_").ifBlank { "-" }}"
    fun editProduct(id: String) = "edit_product/$id"
    fun dateScannerMulti(barcode: String) = "date_scanner_multi/$barcode"
    fun addProductMulti(barcode: String, date: String) =
        "add_product_multi/${barcode.ifBlank { "-" }}/${date.replace("/", "_").ifBlank { "-" }}"
}

data class BottomTab(val route: String, val label: String, val icon: @Composable () -> Unit)

val bottomTabs = listOf(
    BottomTab(Routes.INDEX, "Frigo") { Icon(Icons.Default.Home, null, modifier = Modifier.size(26.dp)) },
    BottomTab(Routes.STATS, "Stats") { Icon(Icons.Default.BarChart, null, modifier = Modifier.size(24.dp)) },
    BottomTab(Routes.HISTORY, "Historique") { Icon(Icons.Default.History, null, modifier = Modifier.size(24.dp)) },
    BottomTab(Routes.NOTIFICATIONS, "Alertes") { Icon(Icons.Default.Notifications, null, modifier = Modifier.size(24.dp)) }
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
        composable(Routes.DATE_SCANNER_ADD) { back ->
            DateScannerScreen(
                navController,
                barcode = (back.arguments?.getString("barcode") ?: "-").let { if (it == "-") "" else it },
                isAddFlow = true
            )
        }
        composable(Routes.ADD_PRODUCT_MANUAL) {
            AddProductScreen(navController, "")
        }
        composable(Routes.ADD_PRODUCT) { back ->
            AddProductScreen(navController, back.arguments?.getString("barcode") ?: "")
        }
        composable(Routes.ADD_PRODUCT_SCANNED_DATE) { back ->
            val rawDate = back.arguments?.getString("date") ?: "-"
            AddProductScreen(
                navController,
                barcode = (back.arguments?.getString("barcode") ?: "-").let { if (it == "-") "" else it },
                initialDate = if (rawDate == "-") "" else rawDate.replace("_", "/")
            )
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
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MainScreen(rootNavController: androidx.navigation.NavController) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val menuVm: HamburgerMenuViewModel = hiltViewModel()
    val menuState by menuVm.uiState.collectAsState()
    val appearanceVm: AppearanceViewModel = hiltViewModel()
    val appearance by appearanceVm.appearance.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDarkMode = when (appearance.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val context = LocalContext.current

    var showMenu by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 0.dp
                ) {
                    NavigationBar(
                        modifier = Modifier.height(84.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
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
                                label = {
                                    Text(
                                        tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(Modifier.fillMaxSize().padding(innerPadding)) {
                OfflineBanner()
                NavHost(
                    navController = tabNavController,
                    startDestination = Routes.INDEX,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.INDEX) {
                        IndexScreen(
                            onProductClick = { id -> rootNavController.navigate(Routes.productDetail(id)) },
                            onScanClick = { rootNavController.navigate(Routes.BARCODE_SCANNER) },
                            onMultiScanClick = { rootNavController.navigate(Routes.BARCODE_SCANNER_MULTI) },
                            onSettingsClick = { rootNavController.navigate(Routes.SETTINGS) },
                            onEditProduct = { id -> rootNavController.navigate(Routes.editProduct(id)) },
                            onMenuOpen = { showMenu = true }
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

        // Hamburger menu drawer (slide depuis la droite)
        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            HamburgerMenuDrawer(
                state = menuState,
                currentRoute = currentRoute,
                isDarkMode = isDarkMode,
                onClose = { showMenu = false },
                onNavigate = { route ->
                    when (route) {
                        Routes.INDEX, Routes.STATS, Routes.HISTORY, Routes.NOTIFICATIONS -> {
                            tabNavController.navigate(route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        else -> rootNavController.navigate(route)
                    }
                },
                onProfileClick = {
                    showMenu = false
                    rootNavController.navigate(Routes.SETTINGS)
                },
                onHouseholdClick = {
                    showMenu = false
                    rootNavController.navigate(Routes.SETTINGS)
                },
                onThemeToggle = {
                    val next = when (appearance.themeMode) {
                        ThemeMode.LIGHT -> ThemeMode.DARK
                        ThemeMode.DARK -> ThemeMode.LIGHT
                        ThemeMode.SYSTEM -> ThemeMode.DARK
                    }
                    appearanceVm.setThemeMode(next)
                },
                onInviteShare = { code ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Rejoins mon frigo sur FreshTrack ! Code d'invitation : $code")
                    }
                    context.startActivity(Intent.createChooser(intent, "Inviter via…"))
                },
                onLogin = {
                    menuVm.signOut()
                },
                onSignOut = {
                    showMenu = false
                    menuVm.signOut()
                }
            )
        }
    }
}
