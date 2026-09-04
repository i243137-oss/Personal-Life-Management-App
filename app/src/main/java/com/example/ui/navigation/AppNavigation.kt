package com.example.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dictionary.DictionaryScreen
import com.example.ui.screens.money.MoneyScreen
import com.example.ui.screens.loans.LoansScreen
import com.example.ui.screens.placeholder.PhasePlaceholderScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.DictionaryViewModel
import com.example.ui.viewmodel.MoneyViewModel
import com.example.ui.viewmodel.LoanViewModel
import kotlinx.coroutines.launch

enum class BottomNavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    MONEY("Money", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_money"),
    LOANS("Loans", Icons.Filled.Paid, Icons.Outlined.Paid, "nav_loans"),
    DICTIONARY("Dictionary", Icons.Filled.Book, Icons.Outlined.Book, "nav_dictionary"),
    MORE("More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz, "nav_more")
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    moneyViewModel: MoneyViewModel,
    loanViewModel: LoanViewModel,
    dictionaryViewModel: DictionaryViewModel,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    var isRegisterMode by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(BottomNavDestination.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    AnimatedContent(
        targetState = isLoggedIn,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "auth_flow_transition"
    ) { loggedIn ->
        if (!loggedIn) {
            if (isRegisterMode) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { isRegisterMode = false },
                    modifier = modifier
                )
            } else {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { isRegisterMode = true },
                    modifier = modifier
                )
            }
        } else {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("main_bottom_nav"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        BottomNavDestination.values().forEach { destination ->
                            val selected = currentTab == destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.title
                                    )
                                },
                                label = { Text(destination.title) },
                                modifier = Modifier.testTag(destination.tag)
                            )
                        }
                    }
                }
            ) { paddingValues ->
                BoxContent(
                    currentTab = currentTab,
                    authViewModel = authViewModel,
                    dashboardViewModel = dashboardViewModel,
                    moneyViewModel = moneyViewModel,
                    loanViewModel = loanViewModel,
                    dictionaryViewModel = dictionaryViewModel,
                    currentUser = currentUser,
                    onQuickActionClick = { actionKey ->
                        when (actionKey) {
                            "add_money", "add_expense" -> currentTab = BottomNavDestination.MONEY
                            "add_loan" -> currentTab = BottomNavDestination.LOANS
                            "dictionary" -> currentTab = BottomNavDestination.DICTIONARY
                            "add_note", "luggage" -> {
                                currentTab = BottomNavDestination.MORE
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Feature scheduled for upcoming phase (Luggage: Phase 5, Notes: Phase 6)"
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun BoxContent(
    currentTab: BottomNavDestination,
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    moneyViewModel: MoneyViewModel,
    loanViewModel: LoanViewModel,
    dictionaryViewModel: DictionaryViewModel,
    currentUser: com.example.data.model.UserDto?,
    onQuickActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (currentTab) {
        BottomNavDestination.HOME -> {
            DashboardScreen(
                dashboardViewModel = dashboardViewModel,
                currentUser = currentUser,
                onQuickActionClick = onQuickActionClick,
                modifier = modifier
            )
        }
        BottomNavDestination.MONEY -> {
            MoneyScreen(
                moneyViewModel = moneyViewModel,
                modifier = modifier
            )
        }
        BottomNavDestination.LOANS -> {
            LoansScreen(
                loanViewModel = loanViewModel,
                modifier = modifier
            )
        }
        BottomNavDestination.DICTIONARY -> {
            DictionaryScreen(
                dictionaryViewModel = dictionaryViewModel,
                modifier = modifier
            )
        }
        BottomNavDestination.MORE -> {
            SettingsScreen(
                authViewModel = authViewModel,
                currentUser = currentUser,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun rememberSnackbarHostState(): SnackbarHostState {
    return remember { SnackbarHostState() }
}
