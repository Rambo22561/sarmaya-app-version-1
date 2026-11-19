package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.InvestmentPack
import com.example.data.model.PaymentChannel
import com.example.data.model.ProductProject
import com.example.data.model.ProductSale
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MarketplaceImage
import com.example.ui.components.StatusBadge
import com.example.ui.components.encodePickedImageToDataUri
import com.example.ui.components.formatRs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.Emerald500
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Navy950
import com.example.ui.viewmodel.MarketplaceViewModel
import kotlinx.coroutines.launch

@Composable
fun BrowseProductsScreen(
    projectsState: UiState<List<ProductProject>>,
    salesState: UiState<List<ProductSale>>,
    channelsState: UiState<List<PaymentChannel>>,
    userAccount: UserAccount?,
    viewModel: MarketplaceViewModel,
    isSubmitting: Boolean,
    onFeedback: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedProject by remember { mutableStateOf<ProductProject?>(null) }

    if (selectedProject != null) {
        BackHandler { selectedProject = null }
        ProjectDetailScreen(
            project = selectedProject!!,
            sales = (salesState as? UiState.Success)?.data.orEmpty().filter { it.projectId == selectedProject!!.id },
            channels = (channelsState as? UiState.Success)?.data.orEmpty(),
            userAccount = userAccount,
            viewModel = viewModel,
            isSubmitting = isSubmitting,
            onBack = { selectedProject = null },
            onFeedback = onFeedback
        )
        return
    }

    val projects = (projectsState as? UiState.Success)?.data.orEmpty()
    val categories = remember(projects) {
        listOf("All") + projects.map { it.category }.distinct()
    }
    val filtered = remember(projects, searchQuery, selectedCategory) {
        projects.filter { proj ->
            val matchesCat = selectedCategory == "All" || proj.category == selectedCategory
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                proj.title.contains(q, ignoreCase = true) ||
                proj.description.contains(q, ignoreCase = true) ||
                proj.category.contains(q, ignoreCase = true)
            matchesCat && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Physical Product Projects",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select a verified wholesale inventory project and purchase one or more fixed-price packs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search washing powder, solar, rice...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("browse_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        when (projectsState) {
            is UiState.Loading -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> item {
                EmptyStateCard(
                    icon = Icons.Default.Inventory2,
                    title = "Unable to Load Products",
                    subtitle = projectsState.message
                )
            }
            is UiState.Success -> {
                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Inventory2,
                            title = "No Matching Projects",
                            subtitle = "Adjust your search or category filter to view available physical product batches."
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { project ->
                        ProductProjectCard(
                            project = project,
                            onSelect = { selectedProject = project }
                        )
                    }
                }
            }
        }

        item {
            DisclaimerBanner(text = stringResource(id = R.string.marketplace_disclaimer))
        }
    }
}

@Composable
private fun ProductProjectCard(
    project: ProductProject,
    onSelect: () -> Unit
) {
    val soldUnits = (project.totalUnits - project.availableUnits).coerceAtLeast(0L)
    val progress = if (project.totalUnits > 0) soldUnits.toFloat() / project.totalUnits.toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("project_card_${project.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                MarketplaceImage(
                    source = project.imageGallery.firstOrNull() ?: project.imageKey,
                    fallbackKey = project.imageKey,
                    contentDescription = project.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Navy950.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = project.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Emerald500,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                StatusBadge(
                    status = project.status,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Purchase Cost",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatRs(project.purchaseCost),
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Projected Sale Value",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatRs(project.expectedSellingPrice),
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            color = Emerald500
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = Emerald500
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${project.availableUnits} / ${project.totalUnits} units available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "View Packs →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectDetailScreen(
    project: ProductProject,
    sales: List<ProductSale>,
    channels: List<PaymentChannel>,
    userAccount: UserAccount?,
    viewModel: MarketplaceViewModel,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onFeedback: (String) -> Unit
) {
    val packs by viewModel.observePacksForProject(project.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var checkoutPack by remember { mutableStateOf<InvestmentPack?>(null) }
    var checkoutQuantity by remember { mutableLongStateOf(1L) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("project_detail_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            val photos = remember(project) {
                project.imageGallery.filter { it.isNotBlank() }.ifEmpty { listOf(project.imageKey) }
            }
            var activePhoto by remember(project.id) { mutableStateOf(photos.first()) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MarketplaceImage(
                    source = activePhoto,
                    fallbackKey = project.imageKey,
                    contentDescription = project.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                if (photos.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photos) { photo ->
                            MarketplaceImage(
                                source = photo,
                                fallbackKey = project.imageKey,
                                contentDescription = "Product Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { activePhoto = photo }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(project.category, style = MaterialTheme.typography.labelLarge, color = Emerald500)
                        StatusBadge(status = project.status)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(project.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Batch Cost: ${formatRs(project.purchaseCost)}", style = MaterialTheme.typography.bodySmall)
                        Text("Available: ${project.availableUnits} / ${project.totalUnits} units", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Expected Sales Revenue Projection: ${formatRs(project.expectedSellingPrice)} (non-guaranteed)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "Available Investment Packs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Minimum purchase is 1 available pack. Select quantity and pay manually with proof.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (packs.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Inventory2,
                    title = "No Packs Configured",
                    subtitle = "Admin has not added packs to this project yet."
                )
            }
        } else {
            items(packs, key = { it.id }) { pack ->
                PackPurchaseCard(
                    pack = pack,
                    onBuy = { qty ->
                        checkoutPack = pack
                        checkoutQuantity = qty
                    }
                )
            }
        }

        if (sales.isNotEmpty()) {
            item {
                Text(
                    text = "Recorded Batch Sales & Profit Distributions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(sales, key = { it.id }) { sale ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sold ${sale.unitsSold} units", fontWeight = FontWeight.Bold)
                            Text(formatTimestamp(sale.createdAt), style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Revenue: ${formatRs(sale.actualRevenue)} • Cost: ${formatRs(sale.allocatedCost)} • Expenses: ${formatRs(sale.eligibleExpenses)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "10% Commission: ${formatRs(sale.platformCommission)} • Investor Pool: ${formatRs(sale.investorProfitPool)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            DisclaimerBanner(text = stringResource(id = R.string.marketplace_disclaimer))
        }
    }

    if (checkoutPack != null && userAccount != null) {
        ManualPaymentCheckoutDialog(
            user = userAccount,
            project = project,
            pack = checkoutPack!!,
            quantity = checkoutQuantity,
            channels = channels,
            isSubmitting = isSubmitting,
            onDismiss = { checkoutPack = null },
            onCopyFeedback = onFeedback,
            onSubmit = { method, tid, proofSummary, proofUri ->
                viewModel.submitManualPayment(
                    user = userAccount,
                    project = project,
                    pack = checkoutPack!!,
                    quantity = checkoutQuantity,
                    paymentMethod = method,
                    transactionRef = tid,
                    proofSummary = proofSummary,
                    proofUri = proofUri,
                    onDone = { checkoutPack = null }
                )
            }
        )
    }
}

@Composable
private fun PackPurchaseCard(
    pack: InvestmentPack,
    onBuy: (Long) -> Unit
) {
    var qty by remember(pack.id) { mutableLongStateOf(pack.minQty.coerceAtLeast(1L)) }
    val maxQty = pack.availablePacks.coerceAtLeast(0L)
    val total = pack.price * qty.toDouble()

    Card(
        shape = RoundedCornerShape(16.dp),
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
                    Text(pack.packName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${pack.unitsPerPack} product unit(s) / pack  •  $maxQty packs left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatRs(pack.price),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                    color = Emerald500,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (qty > pack.minQty) qty-- },
                        enabled = qty > pack.minQty && maxQty > 0,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease Quantity")
                    }
                    Text(
                        text = "$qty",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = { if (qty < maxQty) qty++ },
                        enabled = qty < maxQty,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase Quantity")
                    }
                }

                Button(
                    onClick = { onBuy(qty) },
                    enabled = maxQty >= pack.minQty,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald500,
                        contentColor = Navy950
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("buy_pack_${pack.id}")
                ) {
                    Text(
                        text = if (maxQty > 0) "Pay ${formatRs(total)}" else "Sold Out",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualPaymentCheckoutDialog(
    user: UserAccount,
    project: ProductProject,
    pack: InvestmentPack,
    quantity: Long,
    channels: List<PaymentChannel>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onCopyFeedback: (String) -> Unit,
    onSubmit: (method: String, tid: String, proofSummary: String, proofUri: String?) -> Unit
) {
    val context = LocalContext.current
    val availableMethods = remember(channels) {
        if (channels.isNotEmpty()) channels else listOf(
            PaymentChannel(
                methodType = "EASYPAISA",
                accountTitle = "StockShare Traders",
                accountNumber = "0345-8812090",
                bankName = "Easypaisa",
                instructions = "Transfer exact amount and attach screenshot.",
                adminWhatsapp = "923458812090"
            )
        )
    }
    val scope = rememberCoroutineScope()
    var selectedChannel by remember { mutableStateOf(availableMethods.first()) }
    var transactionRef by remember { mutableStateOf("") }
    var proofSummary by remember { mutableStateOf("") }
    var selectedProofDataUri by remember { mutableStateOf<String?>(null) }
    var isEncodingProof by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    isEncodingProof = true
                    selectedProofDataUri = encodePickedImageToDataUri(
                        context = context,
                        uri = uri,
                        maxDim = 540,
                        quality = 62
                    ) ?: uri.toString()
                    isEncodingProof = false
                }
            }
        }
    )

    val totalAmount = pack.price * quantity.toDouble()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Payment Checkout", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        text = "${project.title}\n${pack.packName} x$quantity = ${formatRs(totalAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Emerald500,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Text("1. Choose Payment Method", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableMethods) { ch ->
                            FilterChip(
                                selected = selectedChannel.methodType == ch.methodType,
                                onClick = { selectedChannel = ch },
                                label = { Text(ch.methodType.replace("_", " ")) }
                            )
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Account Title: ${selectedChannel.accountTitle}", fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${selectedChannel.bankName}: ${selectedChannel.accountNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Account", selectedChannel.accountNumber))
                                        onCopyFeedback("Copied account number")
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Account", modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                text = selectedChannel.instructions,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text("2. Attach Payment Screenshot (Photo Picker)", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !isEncodingProof,
                        modifier = Modifier.fillMaxWidth().testTag("pick_payment_proof_button")
                    ) {
                        Icon(
                            imageVector = if (selectedProofDataUri != null) Icons.Default.Check else Icons.Default.Image,
                            contentDescription = "Attach Screenshot"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when {
                                isEncodingProof -> "Processing Screenshot..."
                                selectedProofDataUri != null -> "Screenshot Attached (Change)"
                                else -> "Select Payment Screenshot"
                            }
                        )
                    }
                    if (selectedProofDataUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MarketplaceImage(
                            source = selectedProofDataUri,
                            fallbackKey = "hero",
                            contentDescription = "Payment Proof Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = transactionRef,
                        onValueChange = { transactionRef = it },
                        label = { Text("Transaction ID / Reference *") },
                        placeholder = { Text("e.g. TID-9928174") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_tid_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = proofSummary,
                        onValueChange = { proofSummary = it },
                        label = { Text("Sender Name / Note (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedChannel.adminWhatsapp.isNotBlank()) {
                    item {
                        OutlinedButton(
                            onClick = {
                                val msg = Uri.encode(
                                    "StockShare Payment Notice: ${user.displayName} sent Rs. ${totalAmount.toLong()} via ${selectedChannel.methodType} for ${project.title} (${pack.packName} x$quantity). TID: ${transactionRef.ifBlank { "Pending" }}"
                                )
                                val waIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://wa.me/${selectedChannel.adminWhatsapp.filter { it.isDigit() }}?text=$msg")
                                )
                                runCatching { context.startActivity(waIntent) }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("notify_whatsapp_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp Admin", tint = Emerald500)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notify Admin on WhatsApp")
                        }
                        Text(
                            text = "Note: WhatsApp notification only alerts admin and never auto-approves payments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        selectedChannel.methodType,
                        transactionRef,
                        proofSummary,
                        selectedProofDataUri
                    )
                },
                enabled = !isSubmitting && !isEncodingProof && transactionRef.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                modifier = Modifier.testTag("submit_payment_request_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Submit Payment", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
