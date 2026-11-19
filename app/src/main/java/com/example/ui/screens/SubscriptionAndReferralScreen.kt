package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CompanyExpense
import com.example.data.model.ReferralRecord
import com.example.data.model.SubscriptionCycle
import com.example.data.model.SubscriptionPayment
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.Emerald500
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Navy950

@Composable
fun SubscriptionAndReferralScreen(
    initialTab: Int = 0,
    userAccount: UserAccount?,
    expensesState: UiState<List<CompanyExpense>>,
    cyclesState: UiState<List<SubscriptionCycle>>,
    subPaymentsState: UiState<List<SubscriptionPayment>>,
    referralsState: UiState<List<ReferralRecord>>,
    isSubmitting: Boolean,
    onPaySubscriptionFromWallet: (UserAccount, SubscriptionCycle) -> Unit,
    onApplyReferralCode: (String, String) -> Unit,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val expenses = (expensesState as? UiState.Success)?.data.orEmpty()
    val cycles = (cyclesState as? UiState.Success)?.data.orEmpty()
    val subPayments = (subPaymentsState as? UiState.Success)?.data.orEmpty()
    val referrals = (referralsState as? UiState.Success)?.data.orEmpty()

    val currentCycle = cycles.firstOrNull { it.status == "OPEN" } ?: cycles.firstOrNull()
    val cycleExpenses = remember(expenses, currentCycle) {
        if (currentCycle == null) expenses else expenses.filter { it.billingMonth == currentCycle.billingMonth }
    }
    val totalMonthExpenses = cycleExpenses.sumOf { it.amount }.coerceAtLeast(currentCycle?.totalExpenses ?: 0.0)
    val alreadyPaid = currentCycle != null && subPayments.any { it.billingMonth == currentCycle.billingMonth && it.status == "PAID" }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Expense Share") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Refer a Member") })
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Billing Period: ${currentCycle?.billingMonth ?: "2026-11"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusBadge(status = if (alreadyPaid) "PAID" else "DUE")
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = formatRs(currentCycle?.calculatedSharePerMember ?: 0.0),
                                style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = Emerald500,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentCycle?.promoEnabled == true) {
                                    "Promotional Period Active: ${currentCycle.promoEligibilityNote}"
                                } else {
                                    "Formula: (${formatRs(currentCycle?.totalExpenses ?: totalMonthExpenses)} + Carry-Fwd ${formatRs(currentCycle?.carryForwardAdjustment ?: 0.0)}) / ${currentCycle?.eligibleMemberCount ?: 1} eligible members"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "More eligible active members reduce everyone's monthly share.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Emerald500
                            )
                            if (currentCycle != null && userAccount != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { onPaySubscriptionFromWallet(userAccount, currentCycle) },
                                    enabled = !alreadyPaid && !isSubmitting && userAccount.availableBalance >= currentCycle.calculatedSharePerMember,
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pay_subscription_button")
                                ) {
                                    Text(
                                        text = when {
                                            alreadyPaid -> "Paid for ${currentCycle.billingMonth}"
                                            userAccount.availableBalance < currentCycle.calculatedSharePerMember ->
                                                "Need ${formatRs(currentCycle.calculatedSharePerMember)} in Wallet"
                                            else -> "Pay ${formatRs(currentCycle.calculatedSharePerMember)} from Wallet"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Documented Company Expenses Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (cycleExpenses.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Receipt,
                            title = "No Expenses Logged Yet",
                            subtitle = "Admin enters actual rent, electricity, petrol, transport, staff salaries, and packaging expenses here."
                        )
                    }
                } else {
                    items(cycleExpenses, key = { it.id }) { exp ->
                        val ratio = if (totalMonthExpenses > 0) (exp.amount / totalMonthExpenses).toFloat().coerceIn(0f, 1f) else 0f
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${exp.category}: ${exp.description}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text(
                                        formatRs(exp.amount),
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                                    color = Emerald500
                                )
                            }
                        }
                    }
                }

                if (subPayments.isNotEmpty()) {
                    item {
                        Text("Your Subscription Payment History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(subPayments, key = { it.id }) { sp ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${sp.billingMonth} (${sp.paymentMethod})", fontWeight = FontWeight.SemiBold)
                                    Text(formatTimestamp(sp.createdAt), style = MaterialTheme.typography.labelSmall)
                                }
                                Text(formatRs(sp.amountPaid), color = Emerald500, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            val myCode = userAccount?.referralCode.orEmpty()
            val shareLink = "https://stockshare.app/join?ref=$myCode"
            var inputCode by remember { mutableStateOf("") }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DisclaimerBanner(text = stringResource(id = R.string.referral_disclaimer))
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Your Unique Referral Code", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = myCode.ifBlank { "..." },
                                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                                    color = Emerald500,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Referral Link", shareLink))
                                        onFeedback("Referral link copied")
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link")
                                }
                            }
                            Text(shareLink, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join StockShare physical product ownership marketplace with my member referral code $myCode: $shareLink"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Referral Link"))
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Invite")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share Invite Link")
                            }
                        }
                    }
                }

                if (userAccount?.referredByCode.isNullOrBlank()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Were You Invited by a Member?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = inputCode,
                                    onValueChange = { inputCode = it.uppercase() },
                                    label = { Text("Enter Referral Code") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("apply_referral_input")
                                )
                                Button(
                                    onClick = {
                                        onApplyReferralCode(userAccount?.displayName ?: "Member", inputCode)
                                        inputCode = ""
                                    },
                                    enabled = !isSubmitting && inputCode.trim().length >= 4,
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("apply_referral_button")
                                ) {
                                    Text("Link Referral Code", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Linked to member code: ${userAccount.referredByCode}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth().padding(12.dp)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Referred Members (${referrals.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (referrals.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.GroupAdd,
                            title = "No Referrals Yet",
                            subtitle = "Share your code above. Eligible referred members help reduce monthly shared expenses for everyone."
                        )
                    }
                } else {
                    items(referrals, key = { it.id }) { ref ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(ref.referredName, fontWeight = FontWeight.SemiBold)
                                    Text(formatTimestamp(ref.createdAt), style = MaterialTheme.typography.labelSmall)
                                }
                                StatusBadge(status = if (ref.activeEligible) "ELIGIBLE" else "PENDING")
                            }
                        }
                    }
                }
            }
        }
    }
}
