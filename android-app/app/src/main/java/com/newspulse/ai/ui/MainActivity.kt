package com.newspulse.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.newspulse.ai.ui.screens.AlertsScreen
import com.newspulse.ai.ui.screens.FilingsScreen
import com.newspulse.ai.ui.screens.OnboardingSetupScreen
import com.newspulse.ai.ui.screens.PortfolioScreen
import com.newspulse.ai.ui.screens.SettingsScreen
import com.newspulse.ai.ui.screens.WatchlistScreen
import com.newspulse.ai.ui.theme.NewsPulseAITheme
import com.newspulse.ai.ui.theme.OnSurfaceDisabled
import com.newspulse.ai.ui.theme.OnSurfacePrimary
import com.newspulse.ai.ui.theme.OutlineDivider
import com.newspulse.ai.ui.theme.SurfaceBase
import com.newspulse.ai.ui.theme.TwitterBlue
import com.newspulse.ai.ui.theme.TwitterBlueContainer
import com.newspulse.ai.ui.theme.Typography

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Alerts : Screen("alerts", "Feed", Icons.Default.Notifications)
    object Portfolio : Screen("portfolio", "Broker", Icons.Default.AccountBalanceWallet)
    object Watchlist : Screen("watchlist", "Watchlist", Icons.Default.Visibility)
    object Filings : Screen("filings", "Filings", Icons.AutoMirrored.Filled.ShowChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            NewsPulseAITheme {
                val groqKey by viewModel.groqApiKey.collectAsState()
                var hasCompletedOnboarding by remember(groqKey) {
                    mutableStateOf(groqKey.isNotBlank())
                }

                if (!hasCompletedOnboarding) {
                    OnboardingSetupScreen(
                        viewModel = viewModel,
                        onSetupComplete = { hasCompletedOnboarding = true }
                    )
                } else {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        Screen.Alerts,
        Screen.Portfolio,
        Screen.Watchlist,
        Screen.Filings,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.background(SurfaceBase)) {
                HorizontalDivider(color = OutlineDivider, thickness = 1.dp)
                NavigationBar(
                    containerColor = SurfaceBase,
                    contentColor = OnSurfacePrimary
                ) {
                    items.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, style = Typography.labelSmall) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TwitterBlue,
                                selectedTextColor = TwitterBlue,
                                indicatorColor = TwitterBlueContainer,
                                unselectedIconColor = OnSurfaceDisabled,
                                unselectedTextColor = OnSurfaceDisabled
                            ),
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
            }
        },
        containerColor = SurfaceBase
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Alerts.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Alerts.route) { AlertsScreen(viewModel = viewModel) }
            composable(Screen.Portfolio.route) { PortfolioScreen(viewModel = viewModel) }
            composable(Screen.Watchlist.route) { WatchlistScreen(viewModel = viewModel) }
            composable(Screen.Filings.route) { FilingsScreen(viewModel = viewModel) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel = viewModel) }
        }
    }
}
