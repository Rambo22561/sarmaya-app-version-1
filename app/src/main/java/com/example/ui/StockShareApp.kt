package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.firebase.MarketplaceRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOutUser
import com.example.data.model.AuthFlowMode
import com.example.data.model.RegistrationDraft
import com.example.data.model.SessionRegistrationState
import com.example.data.model.UiState
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BrowseProductsScreen
import com.example.ui.screens.CompleteRegistrationScreen
import com.example.ui.screens.InvestorHomeScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OwnershipsAndBuybackScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SessionCheckingScreen
import com.example.ui.screens.SessionErrorScreen
import com.example.ui.screens.SubscriptionAndReferralScreen
import com.example.ui.screens.SupportScreen
import com.example.ui.screens.WalletAndWithdrawalsScreen
import com.example.ui.theme.Emerald500
import com.example.ui.theme.StockShareTheme
import com.example.ui.viewmodel.MarketplaceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val INVESTOR_TABS = listOf(
    NavDestination("home", "Home", Icons.Default.Home),
    NavDestination("browse", "Browse", Icons.Default.Explore),
    NavDestination("shares", "Shares", Icons.Default.Inventory),
    NavDestination("wallet", "Wallet", Icons.Default.AccountBalanceWallet),
    NavDestination("community", "Expenses", Icons.Default.GroupAdd),
    NavDestination("profile", "Profile", Icons.Default.Person)
)

private val ADMIN_TABS = listOf(
    NavDestination("admin_overview", "Overview", Icons.Default.Dashboard),
    NavDestination("admin_products", "Products", Icons.Default.Inventory2),
    NavDestination("admin_payments", "Payments", Icons.Default.Payments),
    NavDestination("admin_payouts", "Payouts", Icons.Default.Receipt),
    NavDestination("admin_config", "Config", Icons.Default.Tune)
)

@Composable
fun StockShareRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    var authFlowMode by remember { mutableStateOf(AuthFlowMode.LOGIN) }
    var pendingDraft by remember { mutableStateOf<RegistrationDraft?>(null) }

    val resetAuthFlow = {
        authFlowMode = AuthFlowMode.LOGIN
        pendingDraft = null
    }

    if (user == null) {
        StockShareTheme(darkTheme = true) {
            AuthScreen(
                onStartAuth = { mode, draft ->
                    authFlowMode = mode
                    pendingDraft = draft
                },
                onAuthSuccess = {}
            )
        }
    } else {
        key(user.uid) {
            val capturedMode = remember(user.uid) { authFlowMode }
            val capturedDraft = remember(user.uid) { pendingDraft }

            val viewModel: MarketplaceViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        MarketplaceViewModel(
                            repository = MarketplaceRepository(db, auth),
                            currentUserId = user.uid,
                            currentUserEmail = user.email.orEmpty(),
                            currentUserName = user.displayName.orEmpty().ifBlank { user.email?.substringBefore("@") ?: "Member" },
                            initialAuthMode = capturedMode,
                            initialRegistrationDraft = capturedDraft
                        )
                    }
                }
            )
            val isOwnerAdmin = MarketplaceRepository.isAdminEmail(user.email)
            if (isOwnerAdmin) {
                AdminPortalShell(viewModel = viewModel, onResetAuthFlow = resetAuthFlow)
            } else {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val credentialManager = remember { CredentialManager.create(context) }
                val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
                val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
                val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

                when (val state = sessionState) {
                    SessionRegistrationState.Checking -> {
                        StockShareTheme(darkTheme = true) {
                            SessionCheckingScreen()
                        }
                    }
                    is SessionRegistrationState.RegistrationRequired -> {
                        StockShareTheme(darkTheme = true) {
                            CompleteRegistrationScreen(
                                email = state.email,
                                prefillName = state.prefillName,
                                prefillPhone = state.prefillPhone,
                                prefillReferral = state.prefillReferral,
                                initialNotice = state.notice,
                                feedbackMessage = feedback,
                                isSubmitting = isSubmitting,
                                onSubmitRegistration = viewModel::completeRegistration,
                                onBackToLogin = {
                                    resetAuthFlow()
                                    signOutUser(credentialManager, scope)
                                }
                            )
                        }
                    }
                    is SessionRegistrationState.Error -> {
                        StockShareTheme(darkTheme = true) {
                            SessionErrorScreen(
                                message = state.message,
                                onRetry = viewModel::retrySessionCheck,
                                onBackToLogin = {
                                    resetAuthFlow()
                                    signOutUser(credentialManager, scope)
                                }
                            )
                        }
                    }
                    SessionRegistrationState.Ready -> {
                        InvestorPortalShell(viewModel = viewModel, onResetAuthFlow = resetAuthFlow)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPortalShell(
    viewModel: MarketplaceViewModel,
    onResetAuthFlow: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val salesState by viewModel.productSales.collectAsStateWithLifecycle()
    val expensesState by viewModel.companyExpenses.collectAsStateWithLifecycle()
    val cyclesState by viewModel.subscriptionCycles.collectAsStateWithLifecycle()
    val userNotifsState by viewModel.userNotifications.collectAsStateWithLifecycle()

    val adminProjects by viewModel.adminProjects.collectAsStateWithLifecycle()
    val adminPayments by viewModel.adminPaymentRequests.collectAsStateWithLifecycle()
    val adminOwnerships by viewModel.adminOwnerships.collectAsStateWithLifecycle()
    val adminBuybacks by viewModel.adminBuybacks.collectAsStateWithLifecycle()
    val adminWithdrawals by viewModel.adminWithdrawals.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminChannels by viewModel.adminChannels.collectAsStateWithLifecycle()
    val adminAuditLogs by viewModel.adminAuditLogs.collectAsStateWithLifecycle()
    val adminNotifications by viewModel.adminNotifications.collectAsStateWithLifecycle()

    val combinedNotifications = remember(userNotifsState, adminNotifications) {
        val personal = (userNotifsState as? UiState.Success)?.data.orEmpty()
        (adminNotifications + personal).distinctBy { it.id }
    }
    val unreadCount = combinedNotifications.count { !it.read }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showNotifications by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        if (!feedback.isNullOrBlank()) {
            snackbarHostState.showSnackbar(feedback!!)
            viewModel.clearFeedback()
        }
    }

    StockShareTheme(darkTheme = darkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Portal",
                                    tint = Emerald500
                                )
                                Text(
                                    text = "  StockShare Admin",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showNotifications = !showNotifications },
                                modifier = Modifier.testTag("admin_notifications_bell")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge { Text("$unreadCount") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Admin Notifications")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (!useRail && !showNotifications) {
                        NavigationBar {
                            ADMIN_TABS.forEachIndexed { idx, dest ->
                                NavigationBarItem(
                                    selected = selectedTabIndex == idx,
                                    onClick = { selectedTabIndex = idx },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_tab_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && !showNotifications) {
                        NavigationRail {
                            ADMIN_TABS.forEachIndexed { idx, dest ->
                                NavigationRailItem(
                                    selected = selectedTabIndex == idx,
                                    onClick = { selectedTabIndex = idx },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) }
                                )
                            }
                        }
                    }

                    if (showNotifications) {
                        NotificationsScreen(
                            notifications = combinedNotifications,
                            onMarkRead = viewModel::markNotificationRead,
                            onBack = { showNotifications = false }
                        )
                    } else {
                        AdminDashboardScreen(
                            selectedTab = selectedTabIndex,
                            adminEmail = viewModel.currentUserEmail,
                            darkTheme = darkTheme,
                            viewModel = viewModel,
                            projects = adminProjects,
                            paymentRequests = adminPayments,
                            ownerships = adminOwnerships,
                            sales = (salesState as? UiState.Success)?.data.orEmpty(),
                            buybacks = adminBuybacks,
                            withdrawals = adminWithdrawals,
                            expenses = (expensesState as? UiState.Success)?.data.orEmpty(),
                            cycles = (cyclesState as? UiState.Success)?.data.orEmpty(),
                            channels = adminChannels,
                            users = adminUsers,
                            auditLogs = adminAuditLogs,
                            isSubmitting = isSubmitting,
                            onToggleTheme = viewModel::toggleTheme,
                            onSignOut = {
                                onResetAuthFlow()
                                signOutUser(credentialManager, scope)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvestorPortalShell(
    viewModel: MarketplaceViewModel,
    onResetAuthFlow: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val userState by viewModel.userProfile.collectAsStateWithLifecycle()
    val projectsState by viewModel.publishedProjects.collectAsStateWithLifecycle()
    val channelsState by viewModel.paymentChannels.collectAsStateWithLifecycle()
    val paymentsState by viewModel.userPaymentRequests.collectAsStateWithLifecycle()
    val ownershipsState by viewModel.userOwnerships.collectAsStateWithLifecycle()
    val salesState by viewModel.productSales.collectAsStateWithLifecycle()
    val buybacksState by viewModel.userBuybacks.collectAsStateWithLifecycle()
    val expensesState by viewModel.companyExpenses.collectAsStateWithLifecycle()
    val cyclesState by viewModel.subscriptionCycles.collectAsStateWithLifecycle()
    val subPaymentsState by viewModel.userSubscriptionPayments.collectAsStateWithLifecycle()
    val withdrawalsState by viewModel.userWithdrawals.collectAsStateWithLifecycle()
    val ledgerState by viewModel.userLedger.collectAsStateWithLifecycle()
    val referralsState by viewModel.userReferrals.collectAsStateWithLifecycle()
    val notificationsState by viewModel.userNotifications.collectAsStateWithLifecycle()

    val userAccount = (userState as? UiState.Success)?.data
    val userNotifications = (notificationsState as? UiState.Success)?.data.orEmpty()
    val unreadCount = userNotifications.count { !it.read }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var communityInitialTab by remember { mutableIntStateOf(0) }
    var overlayScreen by remember { mutableStateOf<String?>(null) } // "notifications", "support"
    var localSnack by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback, localSnack) {
        val msg = feedback ?: localSnack
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
            localSnack = null
        }
    }

    StockShareTheme(darkTheme = darkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "StockShare",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { overlayScreen = "notifications" },
                                modifier = Modifier.testTag("notifications_bell_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge { Text("$unreadCount") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (!useRail && overlayScreen == null) {
                        NavigationBar {
                            INVESTOR_TABS.forEachIndexed { idx, dest ->
                                NavigationBarItem(
                                    selected = selectedTabIndex == idx,
                                    onClick = { selectedTabIndex = idx },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_tab_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && overlayScreen == null) {
                        NavigationRail {
                            INVESTOR_TABS.forEachIndexed { idx, dest ->
                                NavigationRailItem(
                                    selected = selectedTabIndex == idx,
                                    onClick = { selectedTabIndex = idx },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) }
                                )
                            }
                        }
                    }

                    when {
                        overlayScreen == "notifications" -> NotificationsScreen(
                            notifications = userNotifications,
                            onMarkRead = viewModel::markNotificationRead,
                            onBack = { overlayScreen = null }
                        )
                        overlayScreen == "support" -> SupportScreen(
                            channels = (channelsState as? UiState.Success)?.data.orEmpty(),
                            onBack = { overlayScreen = null }
                        )
                        else -> when (selectedTabIndex) {
                            0 -> InvestorHomeScreen(
                                userState = userState,
                                ownershipsState = ownershipsState,
                                withdrawalsState = withdrawalsState,
                                cyclesState = cyclesState,
                                subPaymentsState = subPaymentsState,
                                ledgerState = ledgerState,
                                onNavigateToBrowse = { selectedTabIndex = 1 },
                                onNavigateToMyProducts = { selectedTabIndex = 2 },
                                onNavigateToWallet = { selectedTabIndex = 3 },
                                onNavigateToSubscription = { communityInitialTab = 0; selectedTabIndex = 4 },
                                onNavigateToReferrals = { communityInitialTab = 1; selectedTabIndex = 4 },
                                onNavigateToSupport = { overlayScreen = "support" },
                                onCopyFeedback = { localSnack = it }
                            )
                            1 -> BrowseProductsScreen(
                                projectsState = projectsState,
                                salesState = salesState,
                                channelsState = channelsState,
                                userAccount = userAccount,
                                viewModel = viewModel,
                                isSubmitting = isSubmitting,
                                onFeedback = { localSnack = it }
                            )
                            2 -> OwnershipsAndBuybackScreen(
                                ownershipsState = ownershipsState,
                                buybacksState = buybacksState,
                                isSubmitting = isSubmitting,
                                onRequestBuyback = viewModel::requestBuyback,
                                onRespondToOffer = viewModel::respondToBuybackOffer
                            )
                            3 -> WalletAndWithdrawalsScreen(
                                userAccount = userAccount,
                                ledgerState = ledgerState,
                                paymentsState = paymentsState,
                                withdrawalsState = withdrawalsState,
                                isSubmitting = isSubmitting,
                                onSubmitWithdrawal = { amount, method, title, number, pending ->
                                    userAccount?.let {
                                        viewModel.submitWithdrawal(it, amount, method, title, number, pending)
                                    }
                                }
                            )
                            4 -> SubscriptionAndReferralScreen(
                                initialTab = communityInitialTab,
                                userAccount = userAccount,
                                expensesState = expensesState,
                                cyclesState = cyclesState,
                                subPaymentsState = subPaymentsState,
                                referralsState = referralsState,
                                isSubmitting = isSubmitting,
                                onPaySubscriptionFromWallet = viewModel::paySubscriptionFromWallet,
                                onApplyReferralCode = viewModel::applyReferralCode,
                                onFeedback = { localSnack = it }
                            )
                            else -> ProfileScreen(
                                userAccount = userAccount,
                                darkTheme = darkTheme,
                                isSubmitting = isSubmitting,
                                onToggleTheme = viewModel::toggleTheme,
                                onSaveProfile = viewModel::updateProfile,
                                onOpenSupport = { overlayScreen = "support" },
                                onSignOut = {
                                    onResetAuthFlow()
                                    signOutUser(credentialManager, scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
