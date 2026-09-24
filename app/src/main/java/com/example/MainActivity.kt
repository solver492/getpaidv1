package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DataVaultScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GetPaiTheme
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.PlatformViewModel

enum class NavDestination(val id: String, val title: String, val icon: ImageVector) {
    CHAT("chat", "Chat", Icons.AutoMirrored.Filled.Chat),
    WALLET("wallet", "Wallet", Icons.Default.AccountBalanceWallet),
    VAULT("vault", "Registre", Icons.Default.Storage),
    LEADERBOARD("ranks", "Classement", Icons.Default.EmojiEvents),
    ADMIN("admin", "Admin", Icons.Default.AdminPanelSettings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: PlatformViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GetPaiTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: PlatformViewModel) {
    var currentTab by remember { mutableStateOf(NavDestination.CHAT) }
    val snackbarHostState = remember { SnackbarHostState() }
    val feedbackMessage by viewModel.userFeedback.collectAsState()

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    if (isWideScreen) {
        // Tablet / Large Screen Layout with Navigation Rail
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            NavigationRail(
                containerColor = SurfaceDark,
                contentColor = TextPrimary,
                modifier = Modifier.testTag("nav_rail")
            ) {
                NavDestination.entries.forEach { dest ->
                    NavigationRailItem(
                        selected = currentTab == dest,
                        onClick = { currentTab = dest },
                        icon = { Icon(dest.icon, contentDescription = dest.title) },
                        label = {
                            Text(
                                text = dest.title,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldGlow,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_${dest.id}")
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                ScreenContent(tab = currentTab, viewModel = viewModel)
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    } else {
        // Standard Mobile Phone Layout with Bottom Navigation Bar
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = BackgroundDark,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    NavDestination.entries.forEach { dest ->
                        val isSelected = currentTab == dest
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = dest },
                            icon = {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = dest.title
                                )
                            },
                            label = {
                                Text(
                                    text = dest.title,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                indicatorColor = EmeraldGlow,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_${dest.id}")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ScreenContent(tab = currentTab, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ScreenContent(
    tab: NavDestination,
    viewModel: PlatformViewModel,
    modifier: Modifier = Modifier
) {
    when (tab) {
        NavDestination.CHAT -> ChatScreen(viewModel = viewModel, modifier = modifier)
        NavDestination.WALLET -> WalletScreen(viewModel = viewModel, modifier = modifier)
        NavDestination.VAULT -> DataVaultScreen(viewModel = viewModel, modifier = modifier)
        NavDestination.LEADERBOARD -> LeaderboardScreen(viewModel = viewModel, modifier = modifier)
        NavDestination.ADMIN -> AdminScreen(viewModel = viewModel, modifier = modifier)
    }
}
