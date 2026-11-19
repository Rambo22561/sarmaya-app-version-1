package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.PaymentChannel
import com.example.data.model.UserAccount
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Navy950
import com.example.ui.theme.Rose500

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Notifications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        if (notifications.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Notifications,
                    title = "You're All Caught Up",
                    subtitle = "Payment approvals, product sales, profit distributions, buybacks, and subscription notices appear here."
                )
            }
        } else {
            items(notifications, key = { it.id }) { notif ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (!notif.read) onMarkRead(notif.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (notif.read) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Emerald500
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(notif.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            StatusBadge(status = if (notif.read) "READ" else "NEW")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(notif.body, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatTimestamp(notif.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    userAccount: UserAccount?,
    darkTheme: Boolean,
    isSubmitting: Boolean,
    onToggleTheme: () -> Unit,
    onSaveProfile: (displayName: String, phone: String) -> Unit,
    onOpenSupport: () -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by remember(userAccount?.displayName) { mutableStateOf(userAccount?.displayName.orEmpty()) }
    var phone by remember(userAccount?.phone) { mutableStateOf(userAccount?.phone.orEmpty()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userAccount?.displayName ?: "Member", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(userAccount?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(status = userAccount?.role ?: "INVESTOR")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = "Verified", tint = Emerald500, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google Verified • ${if (userAccount?.eligibleMember == true) "Eligible Active Member" else "Pending First Pack Verification"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald500
                        )
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
                    Text("Edit Profile Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile / WhatsApp Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_phone_input")
                    )
                    Button(
                        onClick = { onSaveProfile(displayName, phone) },
                        enabled = !isSubmitting && displayName.trim().length >= 2,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button")
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
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
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onOpenSupport,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Support, Rules & Legal Disclosures")
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSignOut,
                border = BorderStroke(1.dp, Rose500),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Rose500)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secure Logout", color = Rose500, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SupportScreen(
    channels: List<PaymentChannel>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val whatsapp = channels.firstOrNull { it.adminWhatsapp.isNotBlank() }?.adminWhatsapp ?: "923458812090"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Support & Marketplace Rules", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }

        item {
            DisclaimerBanner(text = stringResource(id = R.string.marketplace_disclaimer))
        }
        item {
            DisclaimerBanner(text = stringResource(id = R.string.buyback_disclaimer))
        }
        item {
            DisclaimerBanner(text = stringResource(id = R.string.referral_disclaimer))
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("How Profit Calculation Works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("1. Admin procures physical inventory & defines fixed-price packs.\n2. Upon manual payment verification, your unique ownership share is recorded.\n3. When stock is sold: Net Distributable Profit = Actual Revenue - Allocated Product Cost - Eligible Expenses.\n4. Platform Commission = 10% of positive net profit.\n5. Remaining 90% profit + original capital return to owners proportionally.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Button(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/${whatsapp.filter { it.isDigit() }}?text=${Uri.encode("Hello StockShare Support, I need assistance with my account.")}")
                    )
                    runCatching { context.startActivity(intent) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp Support")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contact Admin Support on WhatsApp", fontWeight = FontWeight.Bold)
            }
        }
    }
}
