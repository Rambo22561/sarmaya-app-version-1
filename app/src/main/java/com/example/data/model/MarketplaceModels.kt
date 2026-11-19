package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AuthFlowMode {
    LOGIN,
    REGISTER
}

data class RegistrationDraft(
    val fullName: String,
    val phone: String,
    val referralCode: String = ""
)

sealed interface SessionRegistrationState {
    data object Checking : SessionRegistrationState
    data object Ready : SessionRegistrationState
    data class RegistrationRequired(
        val prefillName: String,
        val email: String,
        val prefillPhone: String = "",
        val prefillReferral: String = "",
        val notice: String? = null
    ) : SessionRegistrationState
    data class Error(val message: String) : SessionRegistrationState
}

object RegistrationValidator {
    fun validate(fullName: String, phone: String, referralCode: String): String? {
        val cleanName = fullName.trim()
        val cleanPhone = phone.trim()
        val cleanRef = referralCode.trim()
        val digitCount = cleanPhone.count { it.isDigit() }
        return when {
            cleanName.length !in 2..90 -> "Please enter your full name (2–90 characters)."
            digitCount < 7 || cleanPhone.length > 30 -> "Please enter a valid mobile or WhatsApp number (at least 7 digits)."
            cleanRef.isNotEmpty() && !cleanRef.matches(Regex("^[a-zA-Z0-9_\\-]{4,24}$")) ->
                "Referral code must be 4–24 letters or digits."
            else -> null
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "INVESTOR",
    val eligibleMember: Boolean = false,
    val referralCode: String = "",
    val referredByCode: String? = null,
    val referredByUserId: String? = null,
    val availableBalance: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val investedCapital: Double = 0.0,
    val realizedProfit: Double = 0.0,
    val buybackProceeds: Double = 0.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralRecord(
    @DocumentId val id: String = "",
    val referrerUserId: String = "",
    val referrerCode: String = "",
    val referredUserId: String = "",
    val referredName: String = "",
    val activeEligible: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProductProject(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val imageKey: String = "detergent",
    val imageGallery: List<String> = emptyList(),
    val purchaseCost: Double = 0.0,
    val expectedSellingPrice: Double = 0.0,
    val totalUnits: Long = 0L,
    val availableUnits: Long = 0L,
    val published: Boolean = true,
    val status: String = "OPEN",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InvestmentPack(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val packName: String = "",
    val price: Double = 0.0,
    val unitsPerPack: Long = 1L,
    val totalPacks: Long = 0L,
    val availablePacks: Long = 0L,
    val minQty: Long = 1L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentChannel(
    @DocumentId val id: String = "",
    val methodType: String = "EASYPAISA",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val bankName: String = "",
    val instructions: String = "",
    val adminWhatsapp: String = "",
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val packId: String = "",
    val packName: String = "",
    val quantity: Long = 1L,
    val unitPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "EASYPAISA",
    val transactionRef: String = "",
    val proofSummary: String = "",
    val proofUri: String? = null,
    val status: String = "PENDING",
    val adminNote: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OwnershipRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val packId: String = "",
    val packName: String = "",
    val paymentRequestId: String = "",
    val quantity: Long = 1L,
    val unitsOwned: Long = 1L,
    val purchasePrice: Double = 0.0,
    val ownershipPercent: Double = 0.0,
    val status: String = "ACTIVE",
    val realizedCapitalReturned: Double = 0.0,
    val realizedProfitEarned: Double = 0.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProductSale(
    @DocumentId val id: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val unitsSold: Long = 0L,
    val actualRevenue: Double = 0.0,
    val allocatedCost: Double = 0.0,
    val eligibleExpenses: Double = 0.0,
    val grossProfit: Double = 0.0,
    val netDistributableProfit: Double = 0.0,
    val platformCommission: Double = 0.0,
    val investorProfitPool: Double = 0.0,
    val recordedBy: String = "",
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BuybackRequest(
    @DocumentId val id: String = "",
    val ownershipId: String = "",
    val userId: String = "",
    val userName: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val originalValue: Double = 0.0,
    val offeredPrice: Double = 0.0,
    val deductionAmount: Double = 0.0,
    val netPayout: Double = 0.0,
    val status: String = "REQUESTED",
    val adminNote: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CompanyExpense(
    @DocumentId val id: String = "",
    val billingMonth: String = "",
    val category: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubscriptionCycle(
    @DocumentId val id: String = "",
    val billingMonth: String = "",
    val totalExpenses: Double = 0.0,
    val carryForwardAdjustment: Double = 0.0,
    val eligibleMemberCount: Long = 1L,
    val roundingRule: String = "ROUND_NEAREST",
    val promoEnabled: Boolean = false,
    val promoAmount: Double = 1.0,
    val promoEligibilityNote: String = "",
    val calculatedSharePerMember: Double = 0.0,
    val status: String = "OPEN",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubscriptionPayment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val billingMonth: String = "",
    val cycleId: String = "",
    val amountPaid: Double = 0.0,
    val paymentMethod: String = "WALLET",
    val status: String = "PAID",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val amount: Double = 0.0,
    val method: String = "EASYPAISA",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val status: String = "PENDING",
    val adminNote: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LedgerEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val entryType: String = "CREDIT",
    val category: String = "",
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val referenceId: String = "",
    val description: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetRole: String = "USER",
    val title: String = "",
    val body: String = "",
    val eventType: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetId: String = "",
    val details: String = "",
    val createdAt: Timestamp? = null
)
