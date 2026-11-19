package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Rose500
import com.example.ui.theme.Sky500
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToLong

fun formatRs(amount: Double): String {
    val rounded = (amount * 100.0).roundToLong() / 100.0
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return if (rounded % 1.0 == 0.0) {
        formatter.maximumFractionDigits = 0
        "Rs. ${formatter.format(rounded.toLong())}"
    } else {
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        "Rs. ${formatter.format(rounded)}"
    }
}

fun formatTimestamp(ts: Timestamp?): String {
    val date = ts?.toDate() ?: return "Just now"
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(date)
}

@DrawableRes
fun projectDrawableRes(imageKey: String): Int = when (imageKey.lowercase()) {
    "solar" -> R.drawable.img_product_solar
    "rice" -> R.drawable.img_product_rice
    "hero" -> R.drawable.img_hero_marketplace
    else -> R.drawable.img_product_detergent
}

@Composable
fun MarketplaceImage(
    source: String?,
    fallbackKey: String = "detergent",
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val clean = source?.trim().orEmpty()
    if (clean.startsWith("data:image/", ignoreCase = true) && clean.contains("base64,")) {
        val decodedBitmap: ImageBitmap? = remember(clean) {
            runCatching {
                val base64Part = clean.substringAfter("base64,")
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
            return
        }
    }
    if (clean.startsWith("http://", ignoreCase = true) ||
        clean.startsWith("https://", ignoreCase = true) ||
        clean.startsWith("content://", ignoreCase = true) ||
        clean.startsWith("file://", ignoreCase = true)
    ) {
        AsyncImage(
            model = clean,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
        return
    }
    val key = clean.ifBlank { fallbackKey }
    Image(
        painter = painterResource(id = projectDrawableRes(key)),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

suspend fun encodePickedImageToDataUri(
    context: Context,
    uri: Uri,
    maxDim: Int = 640,
    quality: Int = 68
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext null
        val original = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: return@withContext null
        val scale = minOf(
            maxDim.toFloat() / original.width.coerceAtLeast(1),
            maxDim.toFloat() / original.height.coerceAtLeast(1),
            1f
        )
        val targetW = (original.width * scale).toInt().coerceAtLeast(1)
        val targetH = (original.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, targetW, targetH, true)
        } else {
            original
        }
        var currentQuality = quality
        var jpegBytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, currentQuality, out)
            jpegBytes = out.toByteArray()
            currentQuality -= 10
        } while (jpegBytes.size > 110_000 && currentQuality >= 35)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        "data:image/jpeg;base64,$b64"
    }.getOrNull()
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "ACTIVE", "APPROVED", "PAID", "OPEN", "COMPLETED" ->
            Emerald500.copy(alpha = 0.16f) to Emerald500
        "PENDING", "REQUESTED", "OFFERED", "PARTIALLY_SOLD", "BUYBACK_REQUESTED" ->
            Amber500.copy(alpha = 0.18f) to Amber500
        "SOLD", "ACCEPTED_BY_USER", "BOUGHT_BACK" ->
            Sky500.copy(alpha = 0.18f) to Sky500
        "REJECTED", "SOLD_OUT", "CLOSED" ->
            Rose500.copy(alpha = 0.16f) to Rose500
        else ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bgColor
    ) {
        Text(
            text = status.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PortfolioBreakdownChart(
    investedCapital: Double,
    availableBalance: Double,
    realizedProfit: Double,
    buybackProceeds: Double,
    modifier: Modifier = Modifier
) {
    val entries = listOf(
        Triple("Invested", investedCapital, Sky500),
        Triple("Available", availableBalance, Emerald500),
        Triple("Profit", realizedProfit, Amber500),
        Triple("Buybacks", buybackProceeds, Color(0xFF8B5CF6))
    )
    val maxVal = entries.maxOfOrNull { it.second }?.coerceAtLeast(100.0) ?: 100.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Capital & Return Mix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Live ledger allocation across active inventory and settled payouts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val barCount = entries.size
                val slotWidth = size.width / barCount
                val barWidth = (slotWidth * 0.48f).coerceAtMost(56.dp.toPx())
                val maxBarHeight = size.height - 12.dp.toPx()

                entries.forEachIndexed { idx, (_, amount, color) ->
                    val ratio = (amount / maxVal).toFloat().coerceIn(0.06f, 1f)
                    val barHeight = maxBarHeight * ratio
                    val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val y = size.height - barHeight

                    drawRoundRect(
                        color = color.copy(alpha = 0.14f),
                        topLeft = Offset(x, 6.dp.toPx()),
                        size = Size(barWidth, maxBarHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                entries.forEach { (label, amount, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatRs(amount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DisclaimerBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Disclosure",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
