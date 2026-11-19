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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuditLog
import com.example.data.model.BuybackRequest
import com.example.data.model.CompanyExpense
import com.example.data.model.InvestmentPack
import com.example.data.model.OwnershipRecord
import com.example.data.model.PaymentChannel
import com.example.data.model.PaymentRequest
import com.example.data.model.ProductProject
import com.example.data.model.ProductSale
import com.example.data.model.SubscriptionCycle
import com.example.data.model.UserAccount
import com.example.data.model.WithdrawalRequest
import com.example.domain.FinancialCalculator
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.MarketplaceImage
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Navy950
import com.example.ui.theme.Rose500
import com.example.ui.theme.Sky500
import com.example.ui.viewmodel.MarketplaceViewModel

@Composable
fun AdminDashboardScreen(
    selectedTab: Int,
    adminEmail: String,
    darkTheme: Boolean,
    viewModel: MarketplaceViewModel,
    projects: List<ProductProject>,
    paymentRequests: List<PaymentRequest>,
    ownerships: List<OwnershipRecord>,
    sales: List<ProductSale>,
    buybacks: List<BuybackRequest>,
    withdrawals: List<WithdrawalRequest>,
    expenses: List<CompanyExpense>,
    cycles: List<SubscriptionCycle>,
    channels: List<PaymentChannel>,
    users: List<UserAccount>,
    auditLogs: List<AuditLog>,
    isSubmitting: Boolean,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit
) {
    when (selectedTab) {
        0 -> AdminOverviewTab(adminEmail, projects, paymentRequests, ownerships, sales, withdrawals, auditLogs)
        1 -> AdminProjectsAndSalesTab(viewModel, projects, isSubmitting)
        2 -> AdminPaymentsTab(viewModel, paymentRequests, isSubmitting)
        3 -> AdminBuybacksAndWithdrawalsTab(viewModel, buybacks, withdrawals, isSubmitting)
        else -> AdminExpensesChannelsUsersTab(
            adminEmail = adminEmail,
            darkTheme = darkTheme,
            viewModel = viewModel,
            expenses = expenses,
            cycles = cycles,
            channels = channels,
            users = users,
            isSubmitting = isSubmitting,
            onToggleTheme = onToggleTheme,
            onSignOut = onSignOut
        )
    }
}

@Composable
private fun AdminOverviewTab(
    adminEmail: String,
    projects: List<ProductProject>,
    paymentRequests: List<PaymentRequest>,
    ownerships: List<OwnershipRecord>,
    sales: List<ProductSale>,
    withdrawals: List<WithdrawalRequest>,
    auditLogs: List<AuditLog>
) {
    val pendingPayCount = paymentRequests.count { it.status == "PENDING" }
    val pendingWdCount = withdrawals.count { it.status == "PENDING" }
    val totalCommission = sales.sumOf { it.platformCommission }
    val totalOwnershipCap = ownerships.sumOf { it.purchasePrice }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Owner Admin Console",
                        tint = Emerald500,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Owner Admin Portal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Locked to $adminEmail • ${projects.size} projects",
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald500
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiMetricCard("Pending Payments", "$pendingPayCount", Icons.Default.Payments, Modifier.weight(1f), accentColor = Amber500)
                KpiMetricCard("Pending Payouts", "$pendingWdCount", Icons.Default.Receipt, Modifier.weight(1f), accentColor = Amber500)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiMetricCard("Verified Capital", formatRs(totalOwnershipCap), Icons.Default.Inventory2, Modifier.weight(1f), accentColor = Sky500)
                KpiMetricCard("10% Commission", formatRs(totalCommission), Icons.Default.Sell, Modifier.weight(1f), accentColor = Emerald500)
            }
        }

        item {
            Text("Recent Product Sales & 10% Commission (${sales.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(sales.take(5), key = { it.id }) { sale ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("${sale.projectTitle} • Sold ${sale.unitsSold} units", fontWeight = FontWeight.Bold)
                    Text("Rev: ${formatRs(sale.actualRevenue)} | Cost: ${formatRs(sale.allocatedCost)} | Exp: ${formatRs(sale.eligibleExpenses)}", style = MaterialTheme.typography.bodySmall)
                    Text("Commission: ${formatRs(sale.platformCommission)} | Investor Pool: ${formatRs(sale.investorProfitPool)}", style = MaterialTheme.typography.bodySmall, color = Emerald500)
                }
            }
        }

        item {
            Text("Immutable Admin Audit Logs (${auditLogs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (auditLogs.isEmpty()) {
            item {
                EmptyStateCard(Icons.Default.CheckCircle, "No Audit Logs Yet", "Admin approvals, sales, and updates are logged immutably here.")
            }
        } else {
            items(auditLogs.take(15), key = { it.id }) { log ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(log.action, style = MaterialTheme.typography.labelLarge, color = Emerald500)
                        Text(log.details, style = MaterialTheme.typography.bodySmall)
                        Text("${log.adminEmail} • ${formatTimestamp(log.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminProjectsAndSalesTab(
    viewModel: MarketplaceViewModel,
    projects: List<ProductProject>,
    isSubmitting: Boolean
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editProjectTarget by remember { mutableStateOf<ProductProject?>(null) }
    var addPackProject by remember { mutableStateOf<ProductProject?>(null) }
    var editPackPair by remember { mutableStateOf<Pair<ProductProject, InvestmentPack>?>(null) }
    var recordSaleTarget by remember { mutableStateOf<ProductProject?>(null) }
    var deleteConfirmProject by remember { mutableStateOf<ProductProject?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_create_project_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Product Project (Pics, Details & Pricing)", fontWeight = FontWeight.Bold)
            }
        }

        if (projects.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Inventory2,
                    title = "No Product Projects Yet",
                    subtitle = "Tap above to upload product pictures, set details, pricing, and investment packs."
                )
            }
        } else {
            items(projects, key = { it.id }) { proj ->
                AdminProjectCard(
                    project = proj,
                    viewModel = viewModel,
                    isSubmitting = isSubmitting,
                    onEditProject = { editProjectTarget = proj },
                    onAddPack = { addPackProject = proj },
                    onEditPack = { pack -> editPackPair = proj to pack },
                    onDeletePack = { pack -> viewModel.deletePack(proj.id, pack) },
                    onRecordSale = { recordSaleTarget = proj },
                    onTogglePublish = { viewModel.togglePublish(proj) },
                    onDeleteProject = { deleteConfirmProject = proj }
                )
            }
        }
    }

    if (showCreateDialog || editProjectTarget != null) {
        ProjectEditorDialog(
            existingProject = editProjectTarget,
            isSubmitting = isSubmitting,
            onDismiss = {
                showCreateDialog = false
                editProjectTarget = null
            },
            onCreate = { title, cat, desc, imgKey, gallery, cost, expSale, units, pub, pName, pPrice, uPerPack ->
                viewModel.createProjectWithPack(
                    title = title,
                    category = cat,
                    description = desc,
                    imageKey = imgKey,
                    imageGallery = gallery,
                    purchaseCost = cost,
                    expectedSellingPrice = expSale,
                    totalUnits = units,
                    published = pub,
                    packName = pName,
                    packPrice = pPrice,
                    unitsPerPack = uPerPack
                )
                showCreateDialog = false
            },
            onUpdate = { proj, title, cat, desc, imgKey, gallery, cost, expSale, totalU, availU, pub, st ->
                viewModel.updateProject(
                    project = proj,
                    title = title,
                    category = cat,
                    description = desc,
                    imageKey = imgKey,
                    imageGallery = gallery,
                    purchaseCost = cost,
                    expectedSellingPrice = expSale,
                    totalUnits = totalU,
                    availableUnits = availU,
                    published = pub,
                    status = st
                )
                editProjectTarget = null
            }
        )
    }

    if (addPackProject != null) {
        val proj = addPackProject!!
        PackEditorDialog(
            projectTitle = proj.title,
            existingPack = null,
            isSubmitting = isSubmitting,
            onDismiss = { addPackProject = null },
            onSave = { name, price, uPerPack, totalP, _, minQ ->
                viewModel.addPack(proj, name, price, uPerPack, totalP, minQ)
                addPackProject = null
            }
        )
    }

    if (editPackPair != null) {
        val (proj, pack) = editPackPair!!
        PackEditorDialog(
            projectTitle = proj.title,
            existingPack = pack,
            isSubmitting = isSubmitting,
            onDismiss = { editPackPair = null },
            onSave = { name, price, uPerPack, totalP, availP, minQ ->
                viewModel.updatePack(proj.id, pack, name, price, uPerPack, totalP, availP, minQ)
                editPackPair = null
            }
        )
    }

    if (recordSaleTarget != null) {
        val proj = recordSaleTarget!!
        var unitsSoldStr by remember { mutableStateOf(proj.totalUnits.toString()) }
        var revenueStr by remember { mutableStateOf(proj.expectedSellingPrice.toLong().toString()) }
        var expensesStr by remember { mutableStateOf("0") }
        var notes by remember { mutableStateOf("") }

        val uSold = unitsSoldStr.toLongOrNull() ?: 0L
        val rev = revenueStr.toDoubleOrNull() ?: 0.0
        val exp = expensesStr.toDoubleOrNull() ?: 0.0
        val preview = FinancialCalculator.calculateSaleDistribution(
            unitsSold = uSold.coerceAtLeast(1L),
            projectTotalUnits = proj.totalUnits,
            projectPurchaseCost = proj.purchaseCost,
            actualRevenue = rev,
            eligibleExpenses = exp
        )

        AlertDialog(
            onDismissRequest = { recordSaleTarget = null },
            title = { Text("Record Sale & Distribute Profit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(proj.title, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = unitsSoldStr, onValueChange = { unitsSoldStr = it }, label = { Text("Units Sold (max ${proj.totalUnits})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = revenueStr, onValueChange = { revenueStr = it }, label = { Text("Actual Selling Revenue (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = expensesStr, onValueChange = { expensesStr = it }, label = { Text("Eligible Business Expenses (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Settlement Note") })
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Allocated Cost: ${formatRs(preview.allocatedCost)}", style = MaterialTheme.typography.bodySmall)
                            Text("Net Distributable Profit: ${formatRs(preview.netDistributableProfit)}", style = MaterialTheme.typography.bodySmall)
                            Text("10% Platform Commission: ${formatRs(preview.platformCommission)}", style = MaterialTheme.typography.bodySmall, color = Amber500)
                            Text("90% Investor Pool: ${formatRs(preview.investorProfitPool)}", style = MaterialTheme.typography.bodySmall, color = Emerald500, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recordSale(proj, uSold, rev, exp, notes)
                        recordSaleTarget = null
                    },
                    enabled = uSold in 1..proj.totalUnits && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950)
                ) { Text("Confirm & Distribute", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { recordSaleTarget = null }) { Text("Cancel") } }
        )
    }

    if (deleteConfirmProject != null) {
        val proj = deleteConfirmProject!!
        AlertDialog(
            onDismissRequest = { deleteConfirmProject = null },
            title = { Text("Delete Product Project?") },
            text = { Text("This deletes '${proj.title}' and all of its pricing packs from the marketplace.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(proj)
                        deleteConfirmProject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteConfirmProject = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminProjectCard(
    project: ProductProject,
    viewModel: MarketplaceViewModel,
    isSubmitting: Boolean,
    onEditProject: () -> Unit,
    onAddPack: () -> Unit,
    onEditPack: (InvestmentPack) -> Unit,
    onDeletePack: (InvestmentPack) -> Unit,
    onRecordSale: () -> Unit,
    onTogglePublish: () -> Unit,
    onDeleteProject: () -> Unit
) {
    val packs by viewModel.observePacksForProject(project.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var showPacks by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MarketplaceImage(
                    source = project.imageGallery.firstOrNull() ?: project.imageKey,
                    fallbackKey = project.imageKey,
                    contentDescription = project.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        StatusBadge(status = if (project.published) "PUBLISHED" else "HIDDEN")
                    }
                    Text("${project.category} • ${project.status}", style = MaterialTheme.typography.bodySmall, color = Emerald500)
                    Text(
                        text = "${project.availableUnits}/${project.totalUnits} units • ${project.imageGallery.size.coerceAtLeast(1)} photo(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Cost: ${formatRs(project.purchaseCost)}  •  Target Sale: ${formatRs(project.expectedSellingPrice)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onEditProject,
                    modifier = Modifier.weight(1f).testTag("admin_edit_project_${project.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Pics/Info")
                }
                OutlinedButton(onClick = onAddPack, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Pack")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onRecordSale,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                    modifier = Modifier.weight(1f).testTag("admin_record_sale_${project.id}")
                ) {
                    Text("Record Sale", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { showPacks = !showPacks },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (showPacks) "Hide Packs (${packs.size})" else "Pricing Packs (${packs.size})")
                }
            }

            if (showPacks) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (packs.isEmpty()) {
                            Text("No packs configured yet.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            packs.forEachIndexed { idx, pack ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${pack.packName} • ${formatRs(pack.price)}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                        Text("${pack.unitsPerPack} unit/pack • ${pack.availablePacks}/${pack.totalPacks} left • min ${pack.minQty}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { onEditPack(pack) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Pack", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDeletePack(pack) }, enabled = !isSubmitting) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Pack", tint = Rose500, modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (idx < packs.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onTogglePublish, enabled = !isSubmitting) {
                    Text(if (project.published) "Unpublish from Buyers" else "Publish to Buyers")
                }
                IconButton(onClick = onDeleteProject, enabled = !isSubmitting) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = Rose500)
                }
            }
        }
    }
}

@Composable
private fun AdminPaymentsTab(
    viewModel: MarketplaceViewModel,
    paymentRequests: List<PaymentRequest>,
    isSubmitting: Boolean
) {
    var filter by remember { mutableStateOf("ALL") }
    var rejectTarget by remember { mutableStateOf<PaymentRequest?>(null) }

    val filtered = remember(paymentRequests, filter) {
        if (filter == "ALL") paymentRequests else paymentRequests.filter { it.status == filter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("ALL", "PENDING", "APPROVED", "REJECTED")) { status ->
                    FilterChip(
                        selected = filter == status,
                        onClick = { filter = status },
                        label = { Text(status) }
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(Icons.Default.Payments, "No Payment Requests", "Manual payment requests submitted by buyers appear here.")
            }
        } else {
            items(filtered, key = { it.id }) { req ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${req.userName} • ${formatRs(req.totalAmount)}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            StatusBadge(status = req.status)
                        }
                        Text("${req.projectTitle} (${req.packName} x${req.quantity})", style = MaterialTheme.typography.bodySmall)
                        Text("Method: ${req.paymentMethod} • TID: ${req.transactionRef}", style = MaterialTheme.typography.bodySmall, color = Emerald500)
                        if (!req.proofUri.isNullOrBlank()) {
                            MarketplaceImage(
                                source = req.proofUri,
                                fallbackKey = "hero",
                                contentDescription = "Payment Receipt",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp))
                            )
                        }
                        if (req.status == "PENDING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { viewModel.approvePayment(req) },
                                    enabled = !isSubmitting,
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                                    modifier = Modifier.weight(1f).testTag("admin_approve_pay_${req.id}")
                                ) { Text("Approve", fontWeight = FontWeight.Bold) }
                                OutlinedButton(
                                    onClick = { rejectTarget = req },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reject", color = Rose500) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (rejectTarget != null) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject Payment") },
            text = {
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Rejection Reason") })
            },
            confirmButton = {
                Button(onClick = { viewModel.rejectPayment(rejectTarget!!, reason); rejectTarget = null }) {
                    Text("Confirm Reject")
                }
            },
            dismissButton = { TextButton(onClick = { rejectTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminBuybacksAndWithdrawalsTab(
    viewModel: MarketplaceViewModel,
    buybacks: List<BuybackRequest>,
    withdrawals: List<WithdrawalRequest>,
    isSubmitting: Boolean
) {
    var offerTarget by remember { mutableStateOf<BuybackRequest?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Buyback Requests (${buybacks.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(buybacks, key = { it.id }) { bb ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${bb.userName} • ${bb.projectTitle}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        StatusBadge(status = bb.status)
                    }
                    Text("Original: ${formatRs(bb.originalValue)} • Offered: ${formatRs(bb.offeredPrice)} • Net: ${formatRs(bb.netPayout)}", style = MaterialTheme.typography.bodySmall)
                    if (bb.status == "REQUESTED") {
                        Button(onClick = { offerTarget = bb }, enabled = !isSubmitting) {
                            Text("Send Buyback Offer (10% deduction)")
                        }
                    } else if (bb.status == "ACCEPTED_BY_USER") {
                        Button(
                            onClick = { viewModel.completeBuyback(bb) },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950)
                        ) { Text("Confirm Funds & Complete Transfer", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        item {
            Text("Withdrawal Requests (${withdrawals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(withdrawals, key = { it.id }) { wd ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${wd.userName} • ${formatRs(wd.amount)}", fontWeight = FontWeight.Bold)
                        StatusBadge(status = wd.status)
                    }
                    Text("${wd.method} • ${wd.accountTitle} (${wd.accountNumber})", style = MaterialTheme.typography.bodySmall)
                    if (wd.status == "PENDING") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.processWithdrawal(wd, true, "Paid") },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                                modifier = Modifier.weight(1f)
                            ) { Text("Approve & Deduct", fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = { viewModel.processWithdrawal(wd, false, "Declined") },
                                enabled = !isSubmitting,
                                modifier = Modifier.weight(1f)
                            ) { Text("Reject", color = Rose500) }
                        }
                    }
                }
            }
        }
    }

    if (offerTarget != null) {
        val bb = offerTarget!!
        var priceStr by remember { mutableStateOf(bb.originalValue.toLong().toString()) }
        var note by remember { mutableStateOf("") }
        val calc = FinancialCalculator.calculateBuybackPayout(priceStr.toDoubleOrNull() ?: 0.0)
        AlertDialog(
            onDismissRequest = { offerTarget = null },
            title = { Text("Offer Buyback Price") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("Agreed Buyback Price (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Inspection Note") })
                    Text("10% Deduction: ${formatRs(calc.deductionAmount)} • User Net: ${formatRs(calc.netPayout)}", color = Emerald500, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.offerBuyback(bb, calc.offeredPrice, note); offerTarget = null }) {
                    Text("Send Offer")
                }
            },
            dismissButton = { TextButton(onClick = { offerTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminExpensesChannelsUsersTab(
    adminEmail: String,
    darkTheme: Boolean,
    viewModel: MarketplaceViewModel,
    expenses: List<CompanyExpense>,
    cycles: List<SubscriptionCycle>,
    channels: List<PaymentChannel>,
    users: List<UserAccount>,
    isSubmitting: Boolean,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit
) {
    var category by remember { mutableStateOf("Rent") }
    var expDesc by remember { mutableStateOf("") }
    var expAmountStr by remember { mutableStateOf("") }
    var showAddChannel by remember { mutableStateOf(false) }

    val latestCycle = cycles.firstOrNull()
    var eligibleMembersStr by remember { mutableStateOf((latestCycle?.eligibleMemberCount ?: 1000L).toString()) }
    var carryFwdStr by remember { mutableStateOf((latestCycle?.carryForwardAdjustment ?: 0.0).toString()) }
    var promoEnabled by remember { mutableStateOf(latestCycle?.promoEnabled ?: false) }
    var promoAmountStr by remember { mutableStateOf((latestCycle?.promoAmount ?: 1.0).toString()) }
    var promoNote by remember { mutableStateOf(latestCycle?.promoEligibilityNote ?: "Rs. 1 promotional period") }

    val totalMonthExp = expenses.filter { it.billingMonth == "2026-11" }.sumOf { it.amount }
    val previewShare = FinancialCalculator.calculateMonthlyShare(
        totalExpenses = totalMonthExp,
        carryForwardAdjustment = carryFwdStr.toDoubleOrNull() ?: 0.0,
        eligibleMemberCount = eligibleMembersStr.toLongOrNull() ?: 1L,
        roundingRule = "ROUND_NEAREST",
        promoEnabled = promoEnabled,
        promoAmount = promoAmountStr.toDoubleOrNull() ?: 1.0
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Monthly Company Expense (2026-11)", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Rent", "Electricity", "Petrol", "Transport", "Salaries", "Packaging", "Other")) { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(value = expDesc, onValueChange = { expDesc = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = expAmountStr, onValueChange = { expAmountStr = it }, label = { Text("Amount (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            viewModel.addCompanyExpense("2026-11", category, expDesc, expAmountStr.toDoubleOrNull() ?: 0.0)
                            expDesc = ""; expAmountStr = ""
                        },
                        enabled = !isSubmitting && expDesc.isNotBlank() && (expAmountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Log Company Expense") }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Publish Monthly Subscription Cycle", fontWeight = FontWeight.Bold)
                    Text("Total Logged 2026-11 Expenses: ${formatRs(totalMonthExp)}", color = Emerald500, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = eligibleMembersStr, onValueChange = { eligibleMembersStr = it }, label = { Text("Eligible Active Members") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = carryFwdStr, onValueChange = { carryFwdStr = it }, label = { Text("Surplus/Shortfall Carry Forward (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Promotional Subscription (e.g. Rs. 1)")
                        Switch(checked = promoEnabled, onCheckedChange = { promoEnabled = it })
                    }
                    if (promoEnabled) {
                        OutlinedTextField(value = promoAmountStr, onValueChange = { promoAmountStr = it }, label = { Text("Promo Amount (Rs.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = promoNote, onValueChange = { promoNote = it }, label = { Text("Eligibility & Dates Note") }, modifier = Modifier.fillMaxWidth())
                    }
                    Text("Calculated Per-Member Share: ${formatRs(previewShare)}", color = Emerald500, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            viewModel.publishSubscriptionCycle(
                                billingMonth = "2026-11",
                                totalExpenses = totalMonthExp,
                                carryForwardAdjustment = carryFwdStr.toDoubleOrNull() ?: 0.0,
                                eligibleMembers = eligibleMembersStr.toLongOrNull() ?: 1L,
                                roundingRule = "ROUND_NEAREST",
                                promoEnabled = promoEnabled,
                                promoAmount = promoAmountStr.toDoubleOrNull() ?: 1.0,
                                promoNote = promoNote
                            )
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Publish Subscription Cycle", fontWeight = FontWeight.Bold) }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Payment Channels (${channels.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAddChannel = true }) { Text("+ Add Channel") }
            }
        }
        items(channels, key = { it.id }) { ch ->
            var editing by remember { mutableStateOf(false) }
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${ch.methodType} • ${ch.accountTitle}", fontWeight = FontWeight.Bold)
                        TextButton(onClick = { editing = !editing }) { Text(if (editing) "Close" else "Edit") }
                    }
                    Text("${ch.bankName}: ${ch.accountNumber} • WhatsApp: ${ch.adminWhatsapp}", style = MaterialTheme.typography.bodySmall)
                    if (editing) {
                        var title by remember { mutableStateOf(ch.accountTitle) }
                        var num by remember { mutableStateOf(ch.accountNumber) }
                        var bank by remember { mutableStateOf(ch.bankName) }
                        var instr by remember { mutableStateOf(ch.instructions) }
                        var wa by remember { mutableStateOf(ch.adminWhatsapp) }
                        var active by remember { mutableStateOf(ch.active) }
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Account Title") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = num, onValueChange = { num = it }, label = { Text("Account Number") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = bank, onValueChange = { bank = it }, label = { Text("Bank / Service Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = instr, onValueChange = { instr = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = wa, onValueChange = { wa = it }, label = { Text("Admin WhatsApp") }, modifier = Modifier.fillMaxWidth())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Channel Active")
                            Switch(checked = active, onCheckedChange = { active = it })
                        }
                        Button(onClick = {
                            viewModel.savePaymentChannel(ch.id, ch.methodType, title, num, bank, instr, wa, active)
                            editing = false
                        }) { Text("Save Channel") }
                    }
                }
            }
        }

        item {
            Text("Buyer Eligibility Management (${users.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(users, key = { it.id }) { member ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(member.displayName, fontWeight = FontWeight.Bold)
                        Text("${member.email} • ${member.role}", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (member.eligibleMember) "Eligible" else "Inactive", style = MaterialTheme.typography.labelSmall, color = if (member.eligibleMember) Emerald500 else Amber500)
                        TextButton(onClick = {
                            viewModel.adminUpdateUser(member, !member.eligibleMember)
                        }) {
                            Text(if (member.eligibleMember) "Deactivate" else "Activate")
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Admin Session ($adminEmail)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = "Theme")
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Dark Navy Fintech Theme")
                        }
                        Switch(checked = darkTheme, onCheckedChange = { onToggleTheme() })
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        border = BorderStroke(1.dp, Rose500),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Rose500)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Secure Admin Logout", color = Rose500, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddChannel) {
        var methodType by remember { mutableStateOf("BANK_TRANSFER") }
        var title by remember { mutableStateOf("") }
        var number by remember { mutableStateOf("") }
        var bank by remember { mutableStateOf("") }
        var instr by remember { mutableStateOf("Transfer exact amount and attach screenshot.") }
        var wa by remember { mutableStateOf("923458812090") }

        AlertDialog(
            onDismissRequest = { showAddChannel = false },
            title = { Text("Add Payment Channel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("EASYPAISA", "JAZZCASH", "BANK_TRANSFER")) { m ->
                            FilterChip(selected = methodType == m, onClick = { methodType = m }, label = { Text(m) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Account Title *") })
                    OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Account / IBAN *") })
                    OutlinedTextField(value = bank, onValueChange = { bank = it }, label = { Text("Bank / Wallet Name") })
                    OutlinedTextField(value = instr, onValueChange = { instr = it }, label = { Text("Instructions") })
                    OutlinedTextField(value = wa, onValueChange = { wa = it }, label = { Text("Admin WhatsApp") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.savePaymentChannel(null, methodType, title, number, bank, instr, wa, true)
                        showAddChannel = false
                    },
                    enabled = title.trim().length >= 2 && number.trim().length >= 4 && !isSubmitting
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddChannel = false }) { Text("Cancel") } }
        )
    }
}
