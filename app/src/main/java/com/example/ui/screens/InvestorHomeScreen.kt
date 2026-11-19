package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LedgerEntry
import com.example.data.model.OwnershipRecord
import com.example.data.model.SubscriptionCycle
import com.example.data.model.SubscriptionPayment
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.PortfolioBreakdownChart
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Rose500
import com.example.ui.theme.Sky500

@Composable
fun InvestorHomeScreen(
    userState: UiState<UserAccount?>,
    ownershipsState: UiState<List<OwnershipRecord>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    cyclesState: UiState<List<SubscriptionCycle>>,
    subPaymentsState: UiState<List<SubscriptionPayment>>,
    ledgerState: UiState<List<LedgerEntry>>,
    onNavigateToBrowse: () -> Unit,
    onNavigateToMyProducts: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToReferrals: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onCopyFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    val user = (userState as? UiState.Success)?.data
    val ownerships = (ownershipsState as? UiState.Success)?.data.orEmpty()
    val withdrawals = (withdrawalsState as? UiState.Success)?.data.orEmpty()
    val cycles = (cyclesState as? UiState.Success)?.data.orEmpty()
    val subPayments = (subPaymentsState as? UiState.Success)?.data.orEmpty()
    val ledger = (ledgerState as? UiState.Success)?.data.orEmpty()

    if (userState is UiState.Loading && user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val totalPurchased = ownerships.sumOf { it.purchasePrice }
    val activeOwnerships = ownerships.filter {
        it.status == "ACTIVE" || it.status == "PARTIALLY_SOLD" || it.status == "BUYBACK_REQUESTED"
    }
    val soldOwnerships = ownerships.filter { it.status == "SOLD" || it.status == "BOUGHT_BACK" }
    val activeOwnershipValue = activeOwnerships.sumOf {
        (it.purchasePrice - it.realizedCapitalReturned).coerceAtLeast(0.0)
    }
    val pendingWithdrawalsTotal = withdrawals.filter { it.status == "PENDING" }.sumOf { it.amount }
    val currentCycle = cycles.firstOrNull { it.status == "OPEN" } ?: cycles.firstOrNull()
    val isCurrentMonthPaid = currentCycle != null &&
        subPayments.any { it.billingMonth == currentCycle.billingMonth && it.status == "PAID" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Greeting + Admin Switch
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome, ${user?.displayName ?: "Investor"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (user?.eligibleMember == true) "Eligible Active Member" else "Standard Member (Purchase a pack to activate)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (user?.eligibleMember == true) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Primary Wallet Balance Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Navy950, Navy900, Navy800)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Withdrawal Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite.copy(alpha = 0.78f)
                        )
                        StatusBadge(
                            status = if (isCurrentMonthPaid) "SUB PAID" else "SUB DUE"
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatRs(user?.availableBalance ?: 0.0),
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pending Withdrawals: ${formatRs(pendingWithdrawalsTotal)}  •  Realized Profit: ${formatRs(user?.realizedProfit ?: 0.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Emerald500
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToBrowse,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald500,
                                contentColor = Navy950
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_browse_packs_button")
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buy Packs", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onNavigateToWallet,
                            border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_wallet_button")
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Withdraw", color = PureWhite, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // KPI Grid (2 columns x 3 rows covering all dashboard metrics)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "Total Purchased",
                        value = formatRs(totalPurchased),
                        icon = Icons.Default.Payments,
                        accentColor = Sky500,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Ownership Value",
                        value = formatRs(activeOwnershipValue),
                        icon = Icons.Default.Inventory,
                        accentColor = Emerald500,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "Active Products",
                        value = "${activeOwnerships.size}",
                        subtitle = "${activeOwnerships.sumOf { it.unitsOwned }} units",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Emerald500,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Products Sold",
                        value = "${soldOwnerships.size}",
                        subtitle = "Settled batches",
                        icon = Icons.Default.Sell,
                        accentColor = Sky500,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "Realized Profit",
                        value = formatRs(user?.realizedProfit ?: 0.0),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accentColor = Amber500,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Pending Payouts",
                        value = formatRs(pendingWithdrawalsTotal),
                        icon = Icons.Default.HourglassTop,
                        accentColor = Amber500,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Monthly Subscription Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSubscription)
                    .testTag("home_subscription_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monthly Expense Share (${currentCycle?.billingMonth ?: "2026-11"})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentCycle != null) {
                                "Calculated share: ${formatRs(currentCycle.calculatedSharePerMember)} across ${currentCycle.eligibleMemberCount} members"
                            } else {
                                "View transparent monthly expense breakdown"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    StatusBadge(status = if (isCurrentMonthPaid) "PAID" else "DUE")
                }
            }
        }

        // Referral Code Quick Card
        item {
            val code = user?.referralCode.orEmpty()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToReferrals)
                    .testTag("home_referral_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = "Referral Code",
                        tint = Emerald500,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Member Referral Code",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = code.ifBlank { "Generating..." },
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lowers monthly shared expenses • Zero cash commissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            if (code.isNotBlank()) {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Referral Code", code))
                                onCopyFeedback("Referral code $code copied")
                            }
                        },
                        modifier = Modifier.testTag("copy_referral_code_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Referral Code")
                    }
                }
            }
        }

        // Chart
        item {
            PortfolioBreakdownChart(
                investedCapital = user?.investedCapital ?: 0.0,
                availableBalance = user?.availableBalance ?: 0.0,
                realizedProfit = user?.realizedProfit ?: 0.0,
                buybackProceeds = user?.buybackProceeds ?: 0.0
            )
        }

        // Quick Links Row (My Products, Payment History, Support)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToMyProducts,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("My Shares", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onNavigateToSupport,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Support", maxLines = 1)
                }
            }
        }

        // Recent Transaction History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Ledger Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onNavigateToWallet)
                        .padding(6.dp)
                )
            }
        }

        if (ledger.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No Ledger Transactions Yet",
                    subtitle = "Once admin verifies your pack purchase or distributes sales profits, immutable entries appear here."
                )
            }
        } else {
            items(ledger.take(5), key = { it.id }) { entry ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.category.replace("_", " "),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = entry.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTimestamp(entry.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        val isCredit = entry.entryType == "CREDIT"
                        Text(
                            text = "${if (isCredit) "+" else "-"}${formatRs(entry.amount)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            color = if (isCredit) Emerald500 else Rose500
                        )
                    }
                }
            }
        }

        item {
            DisclaimerBanner(text = stringResource(id = R.string.marketplace_disclaimer))
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
