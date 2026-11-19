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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BuybackRequest
import com.example.data.model.OwnershipRecord
import com.example.data.model.UiState
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.Emerald500
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Navy950
import com.example.ui.theme.Rose500

@Composable
fun OwnershipsAndBuybackScreen(
    ownershipsState: UiState<List<OwnershipRecord>>,
    buybacksState: UiState<List<BuybackRequest>>,
    isSubmitting: Boolean,
    onRequestBuyback: (OwnershipRecord) -> Unit,
    onRespondToOffer: (BuybackRequest, Boolean) -> Unit
) {
    val ownerships = (ownershipsState as? UiState.Success)?.data.orEmpty()
    val buybacks = (buybacksState as? UiState.Success)?.data.orEmpty()
    var filter by remember { mutableStateOf("All") }
    var confirmBuybackTarget by remember { mutableStateOf<OwnershipRecord?>(null) }

    val filtered = remember(ownerships, filter) {
        when (filter) {
            "Active" -> ownerships.filter { it.status == "ACTIVE" || it.status == "PARTIALLY_SOLD" }
            "Sold" -> ownerships.filter { it.status == "SOLD" || it.status == "BOUGHT_BACK" }
            "Buybacks" -> ownerships.filter { it.status == "BUYBACK_REQUESTED" || it.status == "BOUGHT_BACK" }
            else -> ownerships
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "My Product Ownerships",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Each verified pack purchase creates an immutable product share record.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Active", "Sold", "Buybacks")) { tab ->
                    FilterChip(
                        selected = filter == tab,
                        onClick = { filter = tab },
                        label = { Text(tab) }
                    )
                }
            }
        }

        // Pending Buyback Offers requiring user acceptance
        val offeredBuybacks = buybacks.filter { it.status == "OFFERED" }
        if (offeredBuybacks.isNotEmpty()) {
            item {
                Text(
                    text = "Action Required: Admin Buyback Offers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Emerald500
                )
            }
            items(offeredBuybacks, key = { it.id }) { bb ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, Emerald500)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(bb.projectTitle, fontWeight = FontWeight.Bold)
                            StatusBadge(status = bb.status)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Offered Price: ${formatRs(bb.offeredPrice)}  •  10% Admin Deduction: ${formatRs(bb.deductionAmount)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Net Payout to Wallet: ${formatRs(bb.netPayout)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = Emerald500,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = bb.adminNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onRespondToOffer(bb, true) },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                                modifier = Modifier.weight(1f).testTag("accept_buyback_${bb.id}")
                            ) {
                                Text("Accept Offer", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onRespondToOffer(bb, false) },
                                enabled = !isSubmitting,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Decline", color = Rose500)
                            }
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Inventory,
                    title = "No Product Ownerships Found",
                    subtitle = "Browse physical product projects and submit a manual payment to own product shares."
                )
            }
        } else {
            items(filtered, key = { it.id }) { own ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(own.projectTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${own.packName} x${own.quantity}  •  ${own.unitsOwned} units",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(status = own.status)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Purchase Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatRs(own.purchasePrice),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Share %", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${own.ownershipPercent}%",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Profit Earned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatRs(own.realizedProfitEarned),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = Emerald500,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Purchased: ${formatTimestamp(own.createdAt)} • Capital Returned: ${formatRs(own.realizedCapitalReturned)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (own.status == "ACTIVE") {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { confirmBuybackTarget = own },
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("request_buyback_${own.id}")
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Request Buyback")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Request Admin Buyback (10% deduction)")
                            }
                        }
                    }
                }
            }
        }

        if (buybacks.isNotEmpty()) {
            item {
                Text(
                    text = "Buyback Request History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(buybacks, key = { it.id }) { bb ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bb.projectTitle, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (bb.netPayout > 0) "Net Payout: ${formatRs(bb.netPayout)}" else "Original Value: ${formatRs(bb.originalValue)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        StatusBadge(status = bb.status)
                    }
                }
            }
        }

        item {
            DisclaimerBanner(text = stringResource(id = R.string.buyback_disclaimer))
        }
    }

    if (confirmBuybackTarget != null) {
        val target = confirmBuybackTarget!!
        val estDeduction = target.purchasePrice * 0.10
        val estNet = target.purchasePrice - estDeduction
        AlertDialog(
            onDismissRequest = { confirmBuybackTarget = null },
            title = { Text("Request Share Buyback?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Buyback is NOT guaranteed and depends on admin inspection and available funds.\n\n" +
                        "Original Purchase Value: ${formatRs(target.purchasePrice)}\n" +
                        "Example at Par (10% deduction = ${formatRs(estDeduction)}): ${formatRs(estNet)} net payout."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRequestBuyback(target)
                        confirmBuybackTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950)
                ) {
                    Text("Submit Request", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBuybackTarget = null }) { Text("Cancel") }
            }
        )
    }
}
