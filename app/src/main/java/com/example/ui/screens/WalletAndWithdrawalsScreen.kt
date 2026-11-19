package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.LedgerEntry
import com.example.data.model.PaymentRequest
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Navy950
import com.example.ui.theme.Rose500
import com.example.ui.theme.Sky500

@Composable
fun WalletAndWithdrawalsScreen(
    userAccount: UserAccount?,
    ledgerState: UiState<List<LedgerEntry>>,
    paymentsState: UiState<List<PaymentRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    isSubmitting: Boolean,
    onSubmitWithdrawal: (amount: Double, method: String, accountTitle: String, accountNumber: String, pendingTotal: Double) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val ledger = (ledgerState as? UiState.Success)?.data.orEmpty()
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()
    val withdrawals = (withdrawalsState as? UiState.Success)?.data.orEmpty()
    val pendingTotal = withdrawals.filter { it.status == "PENDING" }.sumOf { it.amount }
    val eligibleBalance = ((userAccount?.availableBalance ?: 0.0) - pendingTotal).coerceAtLeast(0.0)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Wallet & Ledger") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Payments") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Withdrawals") })
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            KpiMetricCard(
                                title = "Available Balance",
                                value = formatRs(userAccount?.availableBalance ?: 0.0),
                                icon = Icons.Default.AccountBalanceWallet,
                                accentColor = Emerald500,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Pending Withdrawals",
                                value = formatRs(pendingTotal),
                                icon = Icons.Default.HourglassTop,
                                accentColor = Amber500,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            KpiMetricCard(
                                title = "Invested Capital",
                                value = formatRs(userAccount?.investedCapital ?: 0.0),
                                icon = Icons.Default.Payments,
                                accentColor = Sky500,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Realized Profit",
                                value = formatRs(userAccount?.realizedProfit ?: 0.0),
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                accentColor = Emerald500,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        KpiMetricCard(
                            title = "Buyback Proceeds",
                            value = formatRs(userAccount?.buybackProceeds ?: 0.0),
                            icon = Icons.Default.AccountBalance,
                            accentColor = Sky500,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text(
                            text = "Double-Entry Financial Ledger (Immutable)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (ledger.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                title = "No Ledger Entries",
                                subtitle = "Every approved investment, profit distribution, buyback, subscription, and withdrawal records an immutable entry."
                            )
                        }
                    } else {
                        items(ledger, key = { it.id }) { entry ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.category.replace("_", " "), fontWeight = FontWeight.Bold)
                                        Text(entry.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${formatTimestamp(entry.createdAt)} • Bal: ${formatRs(entry.balanceAfter)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    val isCredit = entry.entryType == "CREDIT"
                                    Text(
                                        text = "${if (isCredit) "+" else "-"}${formatRs(entry.amount)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = if (isCredit) Emerald500 else Rose500,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Manual Payment History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    if (payments.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Payments,
                                title = "No Payment Submissions",
                                subtitle = "Purchase a product pack from Browse Products to submit manual payment proof."
                            )
                        }
                    } else {
                        items(payments, key = { it.id }) { pay ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(pay.projectTitle, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        StatusBadge(status = pay.status)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("${pay.packName} x${pay.quantity} • ${formatRs(pay.totalAmount)} via ${pay.paymentMethod}")
                                    Text("TID: ${pay.transactionRef} • ${formatTimestamp(pay.createdAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (pay.adminNote.isNotBlank()) {
                                        Text("Admin Note: ${pay.adminNote}", style = MaterialTheme.typography.bodySmall, color = Emerald500)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                var amountStr by remember { mutableStateOf("") }
                var method by remember { mutableStateOf("EASYPAISA") }
                var accountTitle by remember { mutableStateOf(userAccount?.displayName.orEmpty()) }
                var accountNumber by remember { mutableStateOf(userAccount?.phone.orEmpty()) }
                val amountVal = amountStr.toDoubleOrNull() ?: 0.0
                val canSubmit = !isSubmitting && amountVal > 0.0 && amountVal <= eligibleBalance &&
                    accountTitle.trim().length >= 2 && accountNumber.trim().length >= 4

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Request Withdrawal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Eligible for Withdrawal: ${formatRs(eligibleBalance)} (Only completed withdrawals reduce wallet balance)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Emerald500
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(listOf("EASYPAISA", "JAZZCASH", "BANK_TRANSFER")) { m ->
                                        FilterChip(
                                            selected = method == m,
                                            onClick = { method = m },
                                            label = { Text(m.replace("_", " ")) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = amountStr,
                                    onValueChange = { amountStr = it },
                                    label = { Text("Amount (Rs.)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("withdrawal_amount_input")
                                )
                                OutlinedTextField(
                                    value = accountTitle,
                                    onValueChange = { accountTitle = it },
                                    label = { Text("Account Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("withdrawal_title_input")
                                )
                                OutlinedTextField(
                                    value = accountNumber,
                                    onValueChange = { accountNumber = it },
                                    label = { Text("Account Number / IBAN") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("withdrawal_number_input")
                                )
                                Button(
                                    onClick = {
                                        onSubmitWithdrawal(amountVal, method, accountTitle, accountNumber, pendingTotal)
                                        amountStr = ""
                                    },
                                    enabled = canSubmit,
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_withdrawal_button")
                                ) {
                                    Text("Submit Withdrawal Request", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text("Withdrawal History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (withdrawals.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.AccountBalanceWallet,
                                title = "No Withdrawals Yet",
                                subtitle = "Submit a request above when you have available balance from product sales or buybacks."
                            )
                        }
                    } else {
                        items(withdrawals, key = { it.id }) { wd ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${formatRs(wd.amount)} via ${wd.method.replace("_", " ")}", fontWeight = FontWeight.Bold)
                                        Text("${wd.accountTitle} (${wd.accountNumber})", style = MaterialTheme.typography.bodySmall)
                                        Text(formatTimestamp(wd.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusBadge(status = wd.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
