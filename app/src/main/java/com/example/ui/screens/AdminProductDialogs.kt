package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.InvestmentPack
import com.example.data.model.ProductProject
import com.example.ui.components.MarketplaceImage
import com.example.ui.components.encodePickedImageToDataUri
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Navy950
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.launch

@Composable
fun ProjectEditorDialog(
    existingProject: ProductProject?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        category: String,
        description: String,
        imageKey: String,
        imageGallery: List<String>,
        purchaseCost: Double,
        expectedSellingPrice: Double,
        totalUnits: Long,
        published: Boolean,
        packName: String,
        packPrice: Double,
        unitsPerPack: Long
    ) -> Unit,
    onUpdate: (
        project: ProductProject,
        title: String,
        category: String,
        description: String,
        imageKey: String,
        imageGallery: List<String>,
        purchaseCost: Double,
        expectedSellingPrice: Double,
        totalUnits: Long,
        availableUnits: Long,
        published: Boolean,
        status: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEdit = existingProject != null

    var title by remember(existingProject?.id) { mutableStateOf(existingProject?.title.orEmpty()) }
    var category by remember(existingProject?.id) { mutableStateOf(existingProject?.category ?: "Household FMCG") }
    var description by remember(existingProject?.id) { mutableStateOf(existingProject?.description.orEmpty()) }
    var imageKey by remember(existingProject?.id) { mutableStateOf(existingProject?.imageKey ?: "detergent") }
    var imageGallery by remember(existingProject?.id) {
        mutableStateOf(
            existingProject?.imageGallery?.filter { it.isNotBlank() }.orEmpty().ifEmpty {
                val existingKey = existingProject?.imageKey.orEmpty()
                if (existingKey.startsWith("data:") || existingKey.startsWith("http")) listOf(existingKey) else emptyList()
            }
        )
    }
    var customUrlInput by remember { mutableStateOf("") }
    var isProcessingImages by remember { mutableStateOf(false) }

    var costStr by remember(existingProject?.id) {
        mutableStateOf(existingProject?.purchaseCost?.toLong()?.toString() ?: "100000")
    }
    var expSaleStr by remember(existingProject?.id) {
        mutableStateOf(existingProject?.expectedSellingPrice?.toLong()?.toString() ?: "120000")
    }
    var totalUnitsStr by remember(existingProject?.id) {
        mutableStateOf(existingProject?.totalUnits?.toString() ?: "1000")
    }
    var availableUnitsStr by remember(existingProject?.id) {
        mutableStateOf(existingProject?.availableUnits?.toString() ?: "1000")
    }
    var published by remember(existingProject?.id) { mutableStateOf(existingProject?.published ?: true) }
    var status by remember(existingProject?.id) { mutableStateOf(existingProject?.status ?: "OPEN") }

    // Initial pack fields (Create mode only)
    var packName by remember { mutableStateOf("Pack A — Starter") }
    var packPriceStr by remember { mutableStateOf("140") }
    var unitsPerPackStr by remember { mutableStateOf("1") }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                scope.launch {
                    isProcessingImages = true
                    val encoded = uris.mapNotNull { uri ->
                        encodePickedImageToDataUri(context, uri, maxDim = 640, quality = 68)
                    }
                    imageGallery = (imageGallery + encoded).distinct().take(6)
                    isProcessingImages = false
                }
            }
        }
    )

    val parsedCost = costStr.toDoubleOrNull() ?: 0.0
    val parsedSale = expSaleStr.toDoubleOrNull() ?: 0.0
    val parsedUnits = totalUnitsStr.toLongOrNull() ?: 0L
    val canSubmit = title.trim().length >= 2 &&
        category.trim().length >= 2 &&
        parsedCost > 0.0 &&
        parsedSale > 0.0 &&
        parsedUnits > 0L &&
        !isProcessingImages &&
        !isSubmitting

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Product, Pics & Pricing" else "Add New Product Project",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text("Product Pictures (Up to 6)", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            multiPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessingImages && imageGallery.size < 6,
                        modifier = Modifier.fillMaxWidth().testTag("admin_upload_product_pics_button")
                    ) {
                        if (isProcessingImages) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Optimizing Photos...")
                        } else {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload Product Pictures")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Pictures from Gallery (${imageGallery.size}/6)")
                        }
                    }
                }

                if (imageGallery.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(imageGallery) { idx, src ->
                                Box(modifier = Modifier.size(84.dp)) {
                                    MarketplaceImage(
                                        source = src,
                                        fallbackKey = imageKey,
                                        contentDescription = "Uploaded Product Photo ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    IconButton(
                                        onClick = { imageGallery = imageGallery.filterIndexed { i, _ -> i != idx } },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Navy950.copy(alpha = 0.85f))
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove Photo",
                                            tint = PureWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            label = { Text("Or Paste Image URL (https://)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val url = customUrlInput.trim()
                                if (url.startsWith("http", ignoreCase = true) && imageGallery.size < 6) {
                                    imageGallery = (imageGallery + url).distinct().take(6)
                                    customUrlInput = ""
                                }
                            },
                            enabled = customUrlInput.trim().startsWith("http", ignoreCase = true) && imageGallery.size < 6
                        ) {
                            Text("Add")
                        }
                    }
                }

                item {
                    Text("Fallback Preset Icon", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("detergent", "solar", "rice", "hero")) { preset ->
                            FilterChip(
                                selected = imageKey == preset,
                                onClick = { imageKey = preset },
                                label = { Text(preset) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Product Title *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_project_title_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Household FMCG", "Clean Energy", "Agri-Commodities", "Electronics", "Apparel")) { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Product Description & Batch Details") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = costStr,
                            onValueChange = { costStr = it },
                            label = { Text("Purchase Cost (Rs.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = expSaleStr,
                            onValueChange = { expSaleStr = it },
                            label = { Text("Target Sale (Rs.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = totalUnitsStr,
                            onValueChange = { totalUnitsStr = it },
                            label = { Text("Total Stock Units") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (isEdit) {
                            OutlinedTextField(
                                value = availableUnitsStr,
                                onValueChange = { availableUnitsStr = it },
                                label = { Text("Available Units") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (isEdit) {
                    item {
                        Text("Batch Status", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("OPEN", "SOLD_OUT", "CLOSED")) { st ->
                                FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st) })
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Visible to Buyers (Published)")
                        Switch(checked = published, onCheckedChange = { published = it })
                    }
                }

                if (!isEdit) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Initial Pack Pricing", style = MaterialTheme.typography.labelLarge, color = Emerald500)
                                OutlinedTextField(
                                    value = packName,
                                    onValueChange = { packName = it },
                                    label = { Text("Pack Name (e.g. Pack A)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = packPriceStr,
                                        onValueChange = { packPriceStr = it },
                                        label = { Text("Price (Rs.)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = unitsPerPackStr,
                                        onValueChange = { unitsPerPackStr = it },
                                        label = { Text("Units/Pack") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeDesc = description.trim().ifBlank { title.trim() }
                    if (existingProject == null) {
                        onCreate(
                            title.trim(),
                            category.trim(),
                            safeDesc,
                            imageKey,
                            imageGallery,
                            parsedCost,
                            parsedSale,
                            parsedUnits,
                            published,
                            packName.trim().ifBlank { "Pack A" },
                            packPriceStr.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 140.0,
                            unitsPerPackStr.toLongOrNull()?.takeIf { it > 0L } ?: 1L
                        )
                    } else {
                        onUpdate(
                            existingProject,
                            title.trim(),
                            category.trim(),
                            safeDesc,
                            imageKey,
                            imageGallery,
                            parsedCost,
                            parsedSale,
                            parsedUnits,
                            availableUnitsStr.toLongOrNull() ?: existingProject.availableUnits,
                            published,
                            status
                        )
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                modifier = Modifier.testTag("admin_save_project_button")
            ) {
                Text(if (isEdit) "Save Changes" else "Publish Product", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PackEditorDialog(
    projectTitle: String,
    existingPack: InvestmentPack?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        packName: String,
        price: Double,
        unitsPerPack: Long,
        totalPacks: Long,
        availablePacks: Long,
        minQty: Long
    ) -> Unit
) {
    val isEdit = existingPack != null
    var packName by remember(existingPack?.id) { mutableStateOf(existingPack?.packName.orEmpty()) }
    var priceStr by remember(existingPack?.id) { mutableStateOf(existingPack?.price?.toLong()?.toString() ?: "500") }
    var unitsPerPackStr by remember(existingPack?.id) { mutableStateOf(existingPack?.unitsPerPack?.toString() ?: "1") }
    var totalPacksStr by remember(existingPack?.id) { mutableStateOf(existingPack?.totalPacks?.toString() ?: "100") }
    var availablePacksStr by remember(existingPack?.id) { mutableStateOf(existingPack?.availablePacks?.toString() ?: "100") }
    var minQtyStr by remember(existingPack?.id) { mutableStateOf(existingPack?.minQty?.toString() ?: "1") }

    val parsedPrice = priceStr.toDoubleOrNull() ?: 0.0
    val parsedTotal = totalPacksStr.toLongOrNull() ?: 0L
    val parsedUnits = unitsPerPackStr.toLongOrNull() ?: 1L
    val parsedMin = minQtyStr.toLongOrNull() ?: 1L
    val parsedAvail = availablePacksStr.toLongOrNull() ?: parsedTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Pack Pricing" else "Add Pack to $projectTitle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = packName,
                    onValueChange = { packName = it },
                    label = { Text("Pack Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Pack Price (Rs.) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitsPerPackStr,
                        onValueChange = { unitsPerPackStr = it },
                        label = { Text("Units / Pack") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalPacksStr,
                        onValueChange = { totalPacksStr = it },
                        label = { Text("Total Packs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (isEdit) {
                        OutlinedTextField(
                            value = availablePacksStr,
                            onValueChange = { availablePacksStr = it },
                            label = { Text("Available Packs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        OutlinedTextField(
                            value = minQtyStr,
                            onValueChange = { minQtyStr = it },
                            label = { Text("Min Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (isEdit) {
                    OutlinedTextField(
                        value = minQtyStr,
                        onValueChange = { minQtyStr = it },
                        label = { Text("Minimum Purchase Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        packName.trim(),
                        parsedPrice,
                        parsedUnits.coerceAtLeast(1L),
                        parsedTotal.coerceAtLeast(1L),
                        if (isEdit) parsedAvail else parsedTotal,
                        parsedMin.coerceAtLeast(1L)
                    )
                },
                enabled = packName.trim().isNotBlank() && parsedPrice > 0.0 && parsedTotal > 0L && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950)
            ) {
                Text(if (isEdit) "Save Pack" else "Add Pack", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
