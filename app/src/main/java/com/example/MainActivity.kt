package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.GameStatusBar
import com.example.ui.dialogs.NewGameDialog
import com.example.ui.dialogs.RandomEventDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

enum class NavigationTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Штаб", Icons.Default.Shield),
    CRIMES("Дела", Icons.Default.Gavel),
    DISTRICTS("Районы", Icons.Default.Place),
    ASSETS("Имущество", Icons.Default.DirectionsCar),
    CREW("Братва", Icons.Default.Groups)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CriminalRussiaApp()
            }
        }
    }
}

@Composable
fun CriminalRussiaApp(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Display alert message if present
    LaunchedEffect(uiState.alertMessage) {
        val msg = uiState.alertMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissAlert()
        }
    }

    // Back handler: return to dashboard if on another tab
    BackHandler(enabled = currentTab != NavigationTab.DASHBOARD && !uiState.isJailed && uiState.activeShowdown == null) {
        currentTab = NavigationTab.DASHBOARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CrimeBgDark,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            ) { data ->
                Snackbar(
                    containerColor = CrimeSurfaceCard,
                    contentColor = CrimeTextPrimary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(data.visuals.message, fontSize = 13.sp)
                }
            }
        },
        topBar = {
            if (!uiState.isJailed && uiState.activeShowdown == null) {
                GameStatusBar(
                    uiState = uiState,
                    onNextDayClick = { viewModel.passDay() },
                    onHealClick = { viewModel.healHospital() },
                    onBribePoliceClick = { viewModel.bribePolice() }
                )
            }
        },
        bottomBar = {
            if (!uiState.isJailed && uiState.activeShowdown == null) {
                NavigationBar(
                    containerColor = CrimeSurfaceDark,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    NavigationTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) CrimeGoldLight else CrimeTextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    color = if (isSelected) CrimeGoldLight else CrimeTextMuted
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = CrimeSurfaceHighlight,
                                selectedIconColor = CrimeGoldLight,
                                unselectedIconColor = CrimeTextMuted,
                                selectedTextColor = CrimeGoldLight,
                                unselectedTextColor = CrimeTextMuted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CrimeBgDark)
        ) {
            when {
                // 1. Jail screen has top priority when player is jailed
                uiState.isJailed -> {
                    JailScreen(
                        uiState = uiState,
                        onAnswerRiddle = { viewModel.answerPrisonRiddle(it) },
                        onBribeWarden = { viewModel.bribePrisonWarden() },
                        onHireLawyer = { viewModel.hireLawyerInJail() },
                        onPassDay = { viewModel.passDay() }
                    )
                }

                // 2. Active Showdown confrontation
                uiState.activeShowdown != null -> {
                    ShowdownScreen(
                        showdown = uiState.activeShowdown!!,
                        onAction = { viewModel.performShowdownAction(it) },
                        onClose = { viewModel.closeShowdown() }
                    )
                }

                // 3. Regular Navigation tabs
                else -> {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab_transition"
                    ) { targetTab ->
                        when (targetTab) {
                            NavigationTab.DASHBOARD -> {
                                DashboardScreen(
                                    uiState = uiState,
                                    onNavigateToCrimes = { currentTab = NavigationTab.CRIMES },
                                    onNavigateToAssets = { currentTab = NavigationTab.ASSETS },
                                    onNavigateToCrew = { currentTab = NavigationTab.CREW },
                                    onNavigateToDistricts = { currentTab = NavigationTab.DISTRICTS },
                                    onNewGameClick = { viewModel.openNewGameDialog() }
                                )
                            }
                            NavigationTab.CRIMES -> {
                                CrimesScreen(
                                    uiState = uiState,
                                    onExecuteCrime = { viewModel.executeCrime(it) }
                                )
                            }
                            NavigationTab.DISTRICTS -> {
                                MapScreen(
                                    uiState = uiState,
                                    onExpandDistrict = { viewModel.expandDistrictControl(it) }
                                )
                            }
                            NavigationTab.ASSETS -> {
                                AssetsScreen(
                                    uiState = uiState,
                                    onBuyAsset = { viewModel.buyAsset(it) },
                                    onToggleEquip = { viewModel.toggleEquipAsset(it) }
                                )
                            }
                            NavigationTab.CREW -> {
                                CrewScreen(
                                    uiState = uiState,
                                    onHireCrew = { viewModel.hireCrew(it) },
                                    onFireCrew = { viewModel.fireCrew(it) }
                                )
                            }
                        }
                    }
                }
            }

            // Dialogs overlay
            if (uiState.activeEvent != null) {
                RandomEventDialog(
                    event = uiState.activeEvent!!,
                    onSelectOption = { viewModel.resolveEvent(it) },
                    onDismiss = { viewModel.dismissEvent() }
                )
            }

            if (uiState.showNewGameDialog) {
                NewGameDialog(
                    onConfirm = { nickname, perk ->
                        viewModel.startNewGame(nickname, perk)
                    },
                    onDismiss = { viewModel.dismissNewGameDialog() }
                )
            }
        }
    }
}
