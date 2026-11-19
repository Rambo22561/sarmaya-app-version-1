package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.model.AuthFlowMode
import com.example.data.model.RegistrationDraft
import com.example.data.model.RegistrationValidator
import com.example.ui.components.DisclaimerBanner
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.PureWhite

@Composable
fun AuthScreen(
    onStartAuth: (AuthFlowMode, RegistrationDraft?) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedMode by rememberSaveable { mutableStateOf(AuthFlowMode.LOGIN) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var referralCode by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                onStartAuth(AuthFlowMode.LOGIN, null)
                onAuthSuccess()
            },
            onUnauthenticated = { /* Ready for interactive login or registration */ },
            scope = scope
        )
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedBorderColor = Emerald500,
        unfocusedBorderColor = PureWhite.copy(alpha = 0.28f),
        focusedLabelColor = Emerald500,
        unfocusedLabelColor = PureWhite.copy(alpha = 0.7f),
        cursorColor = Emerald500
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Navy950, Navy900, Navy800)
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 560.dp)) {
                // Hero Artwork Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_marketplace),
                        contentDescription = "StockShare Marketplace Hero",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Navy950.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Emerald500.copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = "PHYSICAL PRODUCT OWNERSHIP",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald500,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.displaySmall,
                            color = PureWhite
                        )
                        Text(
                            text = stringResource(id = R.string.marketplace_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Login vs Sign Up Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy900.copy(alpha = 0.94f)),
                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Mode Switcher Tabs: Login / Register
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Navy950.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AuthModeTabButton(
                                    label = "Login",
                                    icon = Icons.AutoMirrored.Filled.Login,
                                    selected = selectedMode == AuthFlowMode.LOGIN,
                                    onClick = {
                                        selectedMode = AuthFlowMode.LOGIN
                                        errorMessage = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auth_tab_login")
                                )
                                AuthModeTabButton(
                                    label = "Register",
                                    icon = Icons.Default.PersonAdd,
                                    selected = selectedMode == AuthFlowMode.REGISTER,
                                    onClick = {
                                        selectedMode = AuthFlowMode.REGISTER
                                        errorMessage = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auth_tab_register")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedMode == AuthFlowMode.LOGIN) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Registered Member Login",
                                    tint = Emerald500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Registered Member Login",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Already registered? Log in directly with your Google account to access your products, profit ledger, and wallet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureWhite.copy(alpha = 0.78f)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "New Member Registration",
                                    tint = Emerald500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Create Member Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "New to StockShare? Fill in your member details below, then verify with Google Sign-In to register and log in.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureWhite.copy(alpha = 0.78f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = {
                                    fullName = it
                                    errorMessage = null
                                },
                                label = { Text("Full Name *") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_name_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = phone,
                                onValueChange = {
                                    phone = it
                                    errorMessage = null
                                },
                                label = { Text("Mobile / WhatsApp Number *") },
                                placeholder = { Text("e.g. 03001234567", color = PureWhite.copy(alpha = 0.45f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_phone_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = referralCode,
                                onValueChange = {
                                    referralCode = it.uppercase()
                                    errorMessage = null
                                },
                                label = { Text("Referral Code (Optional)") },
                                placeholder = { Text("e.g. SS12AB34", color = PureWhite.copy(alpha = 0.45f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_referral_input")
                            )
                        }

                        if (!errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedMode == AuthFlowMode.LOGIN) {
                            Button(
                                onClick = {
                                    errorMessage = null
                                    isLoading = true
                                    onStartAuth(AuthFlowMode.LOGIN, null)
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onAuthError = { msg ->
                                            isLoading = false
                                            errorMessage = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = {
                                            isLoading = false
                                        }
                                    )
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald500,
                                    contentColor = Navy950
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("google_sign_in_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Navy950,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Logging in...", fontWeight = FontWeight.Bold)
                                } else {
                                    Text(
                                        text = stringResource(id = R.string.sign_in_with_google),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    selectedMode = AuthFlowMode.REGISTER
                                    errorMessage = null
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .testTag("switch_to_register_button")
                            ) {
                                Text(
                                    text = "Not registered yet? Create an account",
                                    color = Emerald500,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    val validationError = RegistrationValidator.validate(fullName, phone, referralCode)
                                    if (validationError != null) {
                                        errorMessage = validationError
                                        return@Button
                                    }
                                    errorMessage = null
                                    isLoading = true
                                    onStartAuth(
                                        AuthFlowMode.REGISTER,
                                        RegistrationDraft(
                                            fullName = fullName.trim(),
                                            phone = phone.trim(),
                                            referralCode = referralCode.trim().uppercase()
                                        )
                                    )
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onAuthError = { msg ->
                                            isLoading = false
                                            errorMessage = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = {
                                            isLoading = false
                                        }
                                    )
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald500,
                                    contentColor = Navy950
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("google_register_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Navy950,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Registering...", fontWeight = FontWeight.Bold)
                                } else {
                                    Text(
                                        text = stringResource(id = R.string.register_with_google),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    selectedMode = AuthFlowMode.LOGIN
                                    errorMessage = null
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .testTag("switch_to_login_button")
                            ) {
                                Text(
                                    text = "Already registered? Login here",
                                    color = Emerald500,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Marketplace Features Overview
                AuthFeatureRow(
                    icon = Icons.Default.Inventory2,
                    title = "Own Real Physical Product Packs",
                    subtitle = "Purchase fixed-price inventory packs from Rs. 140 with transparent share tracking."
                )
                Spacer(modifier = Modifier.height(10.dp))
                AuthFeatureRow(
                    icon = Icons.Default.VerifiedUser,
                    title = "Verified Manual Payments & 90/10 Profit Split",
                    subtitle = "Easypaisa, JazzCash & Bank transfers verified by admin. 90% net profit to owners, 10% platform commission."
                )
                Spacer(modifier = Modifier.height(10.dp))
                AuthFeatureRow(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Shared Monthly Expenses & Double-Entry Ledger",
                    subtitle = "Actual monthly company expenses split fairly across eligible members."
                )

                Spacer(modifier = Modifier.height(16.dp))

                DisclaimerBanner(
                    text = stringResource(id = R.string.marketplace_disclaimer)
                )
            }
        }
    }
}

@Composable
fun CompleteRegistrationScreen(
    email: String,
    prefillName: String,
    prefillPhone: String,
    prefillReferral: String,
    initialNotice: String?,
    feedbackMessage: String?,
    isSubmitting: Boolean,
    onSubmitRegistration: (fullName: String, phone: String, referralCode: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    BackHandler { onBackToLogin() }

    var fullName by rememberSaveable(email, prefillName) { mutableStateOf(prefillName) }
    var phone by rememberSaveable(email, prefillPhone) { mutableStateOf(prefillPhone) }
    var referralCode by rememberSaveable(email, prefillReferral) { mutableStateOf(prefillReferral) }
    var localError by remember(initialNotice, feedbackMessage) {
        mutableStateOf(feedbackMessage ?: initialNotice)
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedBorderColor = Emerald500,
        unfocusedBorderColor = PureWhite.copy(alpha = 0.28f),
        focusedLabelColor = Emerald500,
        unfocusedLabelColor = PureWhite.copy(alpha = 0.7f),
        cursorColor = Emerald500
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Navy950, Navy900, Navy800)
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.widthIn(max = 540.dp).fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900.copy(alpha = 0.96f)),
                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Emerald500.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "REGISTRATION REQUIRED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Emerald500,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Complete Member Registration",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No StockShare account was found for $email. Register your member details once to finish logging in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureWhite.copy(alpha = 0.78f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            localError = null
                        },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("complete_register_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            localError = null
                        },
                        label = { Text("Mobile / WhatsApp Number *") },
                        placeholder = { Text("e.g. 03001234567", color = PureWhite.copy(alpha = 0.45f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Emerald500
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("complete_register_phone_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = {
                            referralCode = it.uppercase()
                            localError = null
                        },
                        label = { Text("Referral Code (Optional)") },
                        placeholder = { Text("e.g. SS12AB34", color = PureWhite.copy(alpha = 0.45f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("complete_register_referral_input")
                    )

                    if (!localError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = localError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val validationError = RegistrationValidator.validate(fullName, phone, referralCode)
                            if (validationError != null) {
                                localError = validationError
                                return@Button
                            }
                            localError = null
                            onSubmitRegistration(fullName.trim(), phone.trim(), referralCode.trim().uppercase())
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald500,
                            contentColor = Navy950
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("complete_register_submit_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Navy950,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Creating Account...", fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                text = "Complete Registration & Login",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onBackToLogin,
                        enabled = !isSubmitting,
                        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("complete_register_back_button")
                    ) {
                        Text("Back to Login / Switch Account")
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCheckingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Navy950, Navy900, Navy800)))
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Emerald500)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Checking member account...",
                style = MaterialTheme.typography.bodyMedium,
                color = PureWhite
            )
        }
    }
}

@Composable
fun SessionErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onBackToLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Navy950, Navy900, Navy800)))
            .safeDrawingPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Navy900),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Unable to Verify Account",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PureWhite.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Navy950),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onBackToLogin,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Back to Login")
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthModeTabButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Emerald500 else Color.Transparent,
            contentColor = if (selected) Navy950 else PureWhite.copy(alpha = 0.78f)
        ),
        elevation = null,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = modifier.height(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy800.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Emerald500.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Emerald500,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = PureWhite
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PureWhite.copy(alpha = 0.72f)
                )
            }
        }
    }
}
