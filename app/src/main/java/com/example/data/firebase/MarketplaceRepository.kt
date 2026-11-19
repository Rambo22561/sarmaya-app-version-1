package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.BuybackRequest
import com.example.data.model.CompanyExpense
import com.example.data.model.InvestmentPack
import com.example.data.model.LedgerEntry
import com.example.data.model.OwnershipRecord
import com.example.data.model.PaymentChannel
import com.example.data.model.PaymentRequest
import com.example.data.model.ProductProject
import com.example.data.model.ProductSale
import com.example.data.model.ReferralRecord
import com.example.data.model.RegistrationDraft
import com.example.data.model.RegistrationValidator
import com.example.data.model.SubscriptionCycle
import com.example.data.model.SubscriptionPayment
import com.example.data.model.UserAccount
import com.example.data.model.WithdrawalRequest
import com.example.domain.FinancialCalculator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "khan561salman@gmail.com"
        const val DEFAULT_BILLING_MONTH = "2026-11"

        fun isAdminEmail(email: String?): Boolean =
            email?.trim()?.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) == true
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun newId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(16)}"

    private fun <T> Query.observeList(
        path: String,
        clazz: Class<T>
    ): Flow<List<T>> = snapshots()
        .map { snap -> snap.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser != null) {
                handleFirestoreError(toLog, OperationType.LIST, path)
            }
            throw error
        }

    // --- REALTIME STREAMS ---

    fun observeUserProfile(uid: String): Flow<UserAccount?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, "users/$uid")
                }
                throw error
            }

    fun observeAdminStatus(uid: String, email: String): Flow<Boolean> =
        flowOf(isAdminEmail(email))

    fun observePublishedProjects(): Flow<List<ProductProject>> =
        db.collection("projects")
            .whereEqualTo("published", true)
            .observeList("projects", ProductProject::class.java)

    fun observeAllProjectsAdmin(): Flow<List<ProductProject>> =
        db.collection("projects")
            .observeList("projects", ProductProject::class.java)

    fun observePacks(projectId: String): Flow<List<InvestmentPack>> =
        db.collection("projects").document(projectId).collection("packs")
            .observeList("projects/$projectId/packs", InvestmentPack::class.java)

    fun observeActivePaymentChannels(): Flow<List<PaymentChannel>> =
        db.collection("payment_channels")
            .whereEqualTo("active", true)
            .observeList("payment_channels", PaymentChannel::class.java)

    fun observeAllPaymentChannelsAdmin(): Flow<List<PaymentChannel>> =
        db.collection("payment_channels")
            .observeList("payment_channels", PaymentChannel::class.java)

    fun observeUserPaymentRequests(uid: String): Flow<List<PaymentRequest>> =
        db.collection("payment_requests")
            .whereEqualTo("userId", uid)
            .observeList("payment_requests", PaymentRequest::class.java)

    fun observeAllPaymentRequestsAdmin(): Flow<List<PaymentRequest>> =
        db.collection("payment_requests")
            .observeList("payment_requests", PaymentRequest::class.java)

    fun observeUserOwnerships(uid: String): Flow<List<OwnershipRecord>> =
        db.collection("ownerships")
            .whereEqualTo("userId", uid)
            .observeList("ownerships", OwnershipRecord::class.java)

    fun observeAllOwnershipsAdmin(): Flow<List<OwnershipRecord>> =
        db.collection("ownerships")
            .observeList("ownerships", OwnershipRecord::class.java)

    fun observeProductSales(): Flow<List<ProductSale>> =
        db.collection("product_sales")
            .observeList("product_sales", ProductSale::class.java)

    fun observeUserBuybacks(uid: String): Flow<List<BuybackRequest>> =
        db.collection("buyback_requests")
            .whereEqualTo("userId", uid)
            .observeList("buyback_requests", BuybackRequest::class.java)

    fun observeAllBuybacksAdmin(): Flow<List<BuybackRequest>> =
        db.collection("buyback_requests")
            .observeList("buyback_requests", BuybackRequest::class.java)

    fun observeCompanyExpenses(): Flow<List<CompanyExpense>> =
        db.collection("company_expenses")
            .observeList("company_expenses", CompanyExpense::class.java)

    fun observeSubscriptionCycles(): Flow<List<SubscriptionCycle>> =
        db.collection("subscription_cycles")
            .observeList("subscription_cycles", SubscriptionCycle::class.java)

    fun observeUserSubscriptionPayments(uid: String): Flow<List<SubscriptionPayment>> =
        db.collection("subscription_payments")
            .whereEqualTo("userId", uid)
            .observeList("subscription_payments", SubscriptionPayment::class.java)

    fun observeUserWithdrawals(uid: String): Flow<List<WithdrawalRequest>> =
        db.collection("withdrawals")
            .whereEqualTo("userId", uid)
            .observeList("withdrawals", WithdrawalRequest::class.java)

    fun observeAllWithdrawalsAdmin(): Flow<List<WithdrawalRequest>> =
        db.collection("withdrawals")
            .observeList("withdrawals", WithdrawalRequest::class.java)

    fun observeUserLedger(uid: String): Flow<List<LedgerEntry>> =
        db.collection("ledger_entries")
            .whereEqualTo("userId", uid)
            .observeList("ledger_entries", LedgerEntry::class.java)

    fun observeUserReferrals(uid: String): Flow<List<ReferralRecord>> =
        db.collection("referrals")
            .whereEqualTo("referrerUserId", uid)
            .observeList("referrals", ReferralRecord::class.java)

    fun observeUserNotifications(uid: String): Flow<List<AppNotification>> =
        db.collection("notifications")
            .whereEqualTo("userId", uid)
            .observeList("notifications", AppNotification::class.java)

    fun observeAdminNotifications(): Flow<List<AppNotification>> =
        db.collection("notifications")
            .whereEqualTo("targetRole", "ADMIN")
            .observeList("notifications", AppNotification::class.java)

    fun observeAllUsersAdmin(): Flow<List<UserAccount>> =
        db.collection("users")
            .observeList("users", UserAccount::class.java)

    fun observeAuditLogsAdmin(): Flow<List<AuditLog>> =
        db.collection("audit_logs")
            .observeList("audit_logs", AuditLog::class.java)

    // --- ONBOARDING & DEMO BOOTSTRAP ---

    sealed interface SessionBootstrapOutcome {
        data class Ready(val alreadyRegistered: Boolean) : SessionBootstrapOutcome
        data class NeedsRegistration(val correctionError: String? = null) : SessionBootstrapOutcome
    }

    suspend fun checkAndBootstrapSession(
        uid: String,
        displayName: String,
        email: String,
        pendingDraft: RegistrationDraft? = null
    ): Result<SessionBootstrapOutcome> = try {
        val activeUid = requireUserId()
        check(activeUid == uid)
        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        val referralCode = "SS${uid.take(6).uppercase()}"
        val safeName = displayName.ifBlank { email.substringBefore("@").ifBlank { "Member" } }.take(90)
        val isBootstrapAdmin = isAdminEmail(email)

        if (isBootstrapAdmin) {
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email.take(150),
                    "phone" to "",
                    "role" to "ADMIN",
                    "eligibleMember" to true,
                    "referralCode" to referralCode,
                    "availableBalance" to 0.0,
                    "pendingBalance" to 0.0,
                    "investedCapital" to 0.0,
                    "realizedProfit" to 0.0,
                    "buybackProceeds" to 0.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(payload).await()
            } else if ((snap.getString("role") ?: "INVESTOR") != "ADMIN") {
                userRef.update(
                    mapOf(
                        "role" to "ADMIN",
                        "eligibleMember" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val codeRef = db.collection("referral_codes").document(referralCode)
            if (!codeRef.get().await().exists()) {
                codeRef.set(
                    mapOf(
                        "userId" to uid,
                        "code" to referralCode,
                        "ownerFirstName" to safeName.substringBefore(" ").ifBlank { "Admin" }.take(50),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val adminDoc = db.collection("admins").document(uid).get().await()
            if (!adminDoc.exists()) {
                db.collection("admins").document(uid).set(
                    mapOf(
                        "uid" to uid,
                        "email" to email,
                        "displayName" to safeName,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            seedMarketplaceDemoDataIfEmpty(uid)
            Result.success(SessionBootstrapOutcome.Ready(alreadyRegistered = snap.exists()))
        } else if (snap.exists()) {
            // Existing registered investor — repair referral_codes doc if missing, never downgrade/overwrite balances
            val existingCode = snap.getString("referralCode")?.takeIf { it.isNotBlank() } ?: referralCode
            val codeRef = db.collection("referral_codes").document(existingCode)
            if (!codeRef.get().await().exists()) {
                val existingName = snap.getString("displayName").orEmpty().ifBlank { safeName }
                codeRef.set(
                    mapOf(
                        "userId" to uid,
                        "code" to existingCode,
                        "ownerFirstName" to existingName.substringBefore(" ").ifBlank { "Member" }.take(50),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            if ((snap.getString("role") ?: "INVESTOR") != "INVESTOR") {
                userRef.update(
                    mapOf(
                        "role" to "INVESTOR",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(SessionBootstrapOutcome.Ready(alreadyRegistered = true))
        } else {
            // Account has not registered yet!
            if (pendingDraft == null) {
                Result.success(SessionBootstrapOutcome.NeedsRegistration())
            } else {
                val regResult = completeUserRegistration(
                    uid = uid,
                    email = email,
                    fullName = pendingDraft.fullName,
                    phone = pendingDraft.phone,
                    rawReferralCode = pendingDraft.referralCode
                )
                regResult.fold(
                    onSuccess = { Result.success(SessionBootstrapOutcome.Ready(alreadyRegistered = false)) },
                    onFailure = { err ->
                        if (err is IllegalArgumentException) {
                            Result.success(SessionBootstrapOutcome.NeedsRegistration(err.message))
                        } else {
                            Result.failure(err)
                        }
                    }
                )
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$uid")
        Result.failure(e)
    }

    suspend fun completeUserRegistration(
        uid: String,
        email: String,
        fullName: String,
        phone: String,
        rawReferralCode: String = ""
    ): Result<Unit> = try {
        val activeUid = requireUserId()
        check(activeUid == uid)

        val validationError = RegistrationValidator.validate(fullName, phone, rawReferralCode)
        require(validationError == null) { validationError ?: "Invalid registration details." }

        val cleanName = fullName.trim().take(90)
        val cleanPhone = phone.trim().take(30)
        val cleanRefCode = rawReferralCode.trim().uppercase()

        val userRef = db.collection("users").document(uid)
        val existingSnap = userRef.get().await()
        if (existingSnap.exists()) {
            return Result.success(Unit)
        }

        var referrerUid: String? = null
        if (cleanRefCode.isNotEmpty()) {
            val lookupSnap = db.collection("referral_codes").document(cleanRefCode).get().await()
            require(lookupSnap.exists()) { "Referral code '$cleanRefCode' was not found. Clear it or enter a valid code." }
            val ownerId = lookupSnap.getString("userId").orEmpty()
            require(ownerId.isNotBlank() && ownerId != uid) { "Self-referral is not allowed." }
            referrerUid = ownerId
        }

        val myReferralCode = "SS${uid.take(6).uppercase()}"
        val batch = db.batch()

        val userPayload = mutableMapOf<String, Any>(
            "userId" to uid,
            "displayName" to cleanName,
            "email" to email.take(150),
            "phone" to cleanPhone,
            "role" to "INVESTOR",
            "eligibleMember" to false,
            "referralCode" to myReferralCode,
            "availableBalance" to 0.0,
            "pendingBalance" to 0.0,
            "investedCapital" to 0.0,
            "realizedProfit" to 0.0,
            "buybackProceeds" to 0.0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (referrerUid != null) {
            userPayload["referredByCode"] = cleanRefCode
            userPayload["referredByUserId"] = referrerUid
        }
        batch.set(userRef, userPayload)

        batch.set(
            db.collection("referral_codes").document(myReferralCode),
            mapOf(
                "userId" to uid,
                "code" to myReferralCode,
                "ownerFirstName" to cleanName.substringBefore(" ").ifBlank { "Member" }.take(50),
                "createdAt" to FieldValue.serverTimestamp()
            )
        )

        if (referrerUid != null) {
            batch.set(
                db.collection("referrals").document("ref_$uid"),
                mapOf(
                    "referrerUserId" to referrerUid,
                    "referrerCode" to cleanRefCode,
                    "referredUserId" to uid,
                    "referredName" to cleanName,
                    "activeEligible" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$uid")
        Result.failure(e)
    }

    private suspend fun seedMarketplaceDemoDataIfEmpty(adminUid: String) {
        val existingProjects = db.collection("projects").limit(1).get().await()
        if (existingProjects.isEmpty) {
            val batch = db.batch()
            val proj1Id = "proj_washing_powder"
            batch.set(
                db.collection("projects").document(proj1Id),
                mapOf(
                    "title" to "EcoWash Detergent Bulk Batch",
                    "category" to "Household FMCG",
                    "description" to "Wholesale procurement of 1,000 retail packs of high-demand laundry washing powder distributed across regional grocery partners.",
                    "imageKey" to "detergent",
                    "purchaseCost" to 100000.0,
                    "expectedSellingPrice" to 120000.0,
                    "totalUnits" to 1000L,
                    "availableUnits" to 1000L,
                    "published" to true,
                    "status" to "OPEN",
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val washingPacks = listOf(
                Triple("pack_a", "Pack A — Starter Unit", 140.0 to 1L),
                Triple("pack_b", "Pack B — Retail Bundle", 500.0 to 5L),
                Triple("pack_c", "Pack C — Store Lot", 1000.0 to 10L),
                Triple("pack_d", "Pack D — Distributor Lot", 5000.0 to 50L)
            )
            washingPacks.forEach { (packId, name, priceUnits) ->
                val (price, units) = priceUnits
                val packCount = (1000L / units).coerceAtLeast(10L)
                batch.set(
                    db.collection("projects").document(proj1Id).collection("packs").document(packId),
                    mapOf(
                        "projectId" to proj1Id,
                        "packName" to name,
                        "price" to price,
                        "unitsPerPack" to units,
                        "totalPacks" to packCount,
                        "availablePacks" to packCount,
                        "minQty" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val proj2Id = "proj_solar_kits"
            batch.set(
                db.collection("projects").document(proj2Id),
                mapOf(
                    "title" to "Solar Inverter Mini-Grid Kits",
                    "category" to "Clean Energy",
                    "description" to "Direct factory import of portable 600W solar inverter & panel kits for rural shops and residential backup.",
                    "imageKey" to "solar",
                    "purchaseCost" to 250000.0,
                    "expectedSellingPrice" to 310000.0,
                    "totalUnits" to 500L,
                    "availableUnits" to 500L,
                    "published" to true,
                    "status" to "OPEN",
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            listOf(
                Triple("pack_s1", "Single Share Pack", 500.0 to 1L),
                Triple("pack_s2", "Standard 5-Share Pack", 2500.0 to 5L),
                Triple("pack_s3", "Dealer 20-Share Lot", 10000.0 to 20L)
            ).forEach { (packId, name, priceUnits) ->
                val (price, units) = priceUnits
                val packCount = (500L / units).coerceAtLeast(10L)
                batch.set(
                    db.collection("projects").document(proj2Id).collection("packs").document(packId),
                    mapOf(
                        "projectId" to proj2Id,
                        "packName" to name,
                        "price" to price,
                        "unitsPerPack" to units,
                        "totalPacks" to packCount,
                        "availablePacks" to packCount,
                        "minQty" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val proj3Id = "proj_basmati_rice"
            batch.set(
                db.collection("projects").document(proj3Id),
                mapOf(
                    "title" to "Organic Super Kernel Basmati Harvest",
                    "category" to "Agri-Commodities",
                    "description" to "Mill-direct sealed 5kg & 20kg export-grade basmati rice sacks warehoused for urban wholesale fulfillment.",
                    "imageKey" to "rice",
                    "purchaseCost" to 150000.0,
                    "expectedSellingPrice" to 182000.0,
                    "totalUnits" to 600L,
                    "availableUnits" to 600L,
                    "published" to true,
                    "status" to "OPEN",
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            listOf(
                Triple("pack_r1", "Grain Share A", 250.0 to 1L),
                Triple("pack_r2", "Pantry Lot B", 1000.0 to 4L),
                Triple("pack_r3", "Wholesale Sack Lot", 5000.0 to 20L)
            ).forEach { (packId, name, priceUnits) ->
                val (price, units) = priceUnits
                val packCount = (600L / units).coerceAtLeast(10L)
                batch.set(
                    db.collection("projects").document(proj3Id).collection("packs").document(packId),
                    mapOf(
                        "projectId" to proj3Id,
                        "packName" to name,
                        "price" to price,
                        "unitsPerPack" to units,
                        "totalPacks" to packCount,
                        "availablePacks" to packCount,
                        "minQty" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // Default Payment Channels
            batch.set(
                db.collection("payment_channels").document("ch_easypaisa"),
                mapOf(
                    "methodType" to "EASYPAISA",
                    "accountTitle" to "StockShare Traders",
                    "accountNumber" to "0345-8812090",
                    "bankName" to "Easypaisa Mobile Wallet",
                    "instructions" to "Transfer exact pack total, capture screenshot, and enter your TID reference.",
                    "adminWhatsapp" to "923458812090",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("payment_channels").document("ch_jazzcash"),
                mapOf(
                    "methodType" to "JAZZCASH",
                    "accountTitle" to "StockShare Traders",
                    "accountNumber" to "0301-4421980",
                    "bankName" to "JazzCash Mobile Account",
                    "instructions" to "Send via JazzCash app and attach receipt screenshot before submitting.",
                    "adminWhatsapp" to "923458812090",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("payment_channels").document("ch_bank"),
                mapOf(
                    "methodType" to "BANK_TRANSFER",
                    "accountTitle" to "StockShare Commerce Pvt",
                    "accountNumber" to "PK36MEZN0002110104558812",
                    "bankName" to "Meezan Bank Ltd",
                    "instructions" to "Use IBFT/Raast transfer and upload bank confirmation slip.",
                    "adminWhatsapp" to "923458812090",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Default Monthly Company Expenses & Open Cycle (Rs. 100,000 / 1,000 example baseline)
            val demoExpenses = listOf(
                Triple("exp_rent", "Office & Warehouse Rent", 38000.0),
                Triple("exp_elec", "Electricity & Utilities", 14500.0),
                Triple("exp_petrol", "Petrol & Delivery Fleet", 12500.0),
                Triple("exp_salaries", "Operations Staff Salaries", 25000.0),
                Triple("exp_pack", "Packaging & Logistics", 10000.0)
            )
            demoExpenses.forEach { (expId, desc, amt) ->
                val cat = desc.substringBefore(" ")
                batch.set(
                    db.collection("company_expenses").document(expId),
                    mapOf(
                        "billingMonth" to DEFAULT_BILLING_MONTH,
                        "category" to cat,
                        "description" to desc,
                        "amount" to amt,
                        "createdBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                db.collection("subscription_cycles").document("cycle_$DEFAULT_BILLING_MONTH"),
                mapOf(
                    "billingMonth" to DEFAULT_BILLING_MONTH,
                    "totalExpenses" to 100000.0,
                    "carryForwardAdjustment" to 0.0,
                    "eligibleMemberCount" to 1000L,
                    "roundingRule" to "ROUND_NEAREST",
                    "promoEnabled" to false,
                    "promoAmount" to 1.0,
                    "promoEligibilityNote" to "Standard expense share across active eligible members.",
                    "calculatedSharePerMember" to 100.0,
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
        }
    }

    // --- USER ACTIONS ---

    suspend fun updateUserProfile(uid: String, displayName: String, phone: String): Result<Unit> = try {
        val activeUid = requireUserId()
        check(activeUid == uid)
        require(displayName.trim().length in 2..100) { "Display name must be 2-100 characters." }
        require(phone.trim().length <= 30) { "Phone number is too long." }
        db.collection("users").document(uid).update(
            mapOf(
                "displayName" to displayName.trim(),
                "phone" to phone.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        Result.failure(e)
    }

    suspend fun applyReferralCode(uid: String, userName: String, rawCode: String): Result<Unit> = try {
        val activeUid = requireUserId()
        check(activeUid == uid)
        val code = rawCode.trim().uppercase()
        require(code.length in 4..24) { "Invalid referral code format." }

        val userSnap = db.collection("users").document(uid).get().await()
        val existingCode = userSnap.getString("referredByCode")
        require(existingCode.isNullOrBlank()) { "A referral code has already been applied to this account." }

        val lookupSnap = db.collection("referral_codes").document(code).get().await()
        require(lookupSnap.exists()) { "Referral code '$code' was not found." }
        val referrerUid = lookupSnap.getString("userId").orEmpty()
        require(referrerUid.isNotBlank() && referrerUid != uid) { "Self-referral is not allowed." }

        val batch = db.batch()
        batch.update(
            db.collection("users").document(uid),
            mapOf(
                "referredByCode" to code,
                "referredByUserId" to referrerUid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("referrals").document("ref_$uid"),
            mapOf(
                "referrerUserId" to referrerUid,
                "referrerCode" to code,
                "referredUserId" to uid,
                "referredName" to userName.ifBlank { "Member" }.take(90),
                "activeEligible" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "referrals/ref_$uid")
        Result.failure(e)
    }

    suspend fun submitManualPayment(
        user: UserAccount,
        project: ProductProject,
        pack: InvestmentPack,
        quantity: Long,
        paymentMethod: String,
        transactionRef: String,
        proofSummary: String,
        proofUri: String?
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == user.userId)
        require(quantity >= pack.minQty && quantity <= pack.availablePacks) {
            "Quantity must be between ${pack.minQty} and ${pack.availablePacks} available packs."
        }
        require(transactionRef.trim().length in 2..80) { "Enter a valid transaction reference ID." }

        val reqId = newId("pay")
        val total = FinancialCalculator.round2(pack.price * quantity.toDouble())
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("userName", user.displayName.ifBlank { user.email })
            put("projectId", project.id)
            put("projectTitle", project.title)
            put("packId", pack.id)
            put("packName", pack.packName)
            put("quantity", quantity)
            put("unitPrice", pack.price)
            put("totalAmount", total)
            put("paymentMethod", paymentMethod)
            put("transactionRef", transactionRef.trim())
            put("proofSummary", proofSummary.ifBlank { "Manual transfer receipt submitted" }.take(280))
            proofUri?.let { put("proofUri", it.take(245000)) }
            put("status", "PENDING")
            put("adminNote", "")
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }

        val batch = db.batch()
        batch.set(db.collection("payment_requests").document(reqId), payload)
        batch.set(
            db.collection("notifications").document(newId("notif")),
            mapOf(
                "userId" to uid,
                "targetRole" to "ADMIN",
                "title" to "New Investment Payment Request",
                "body" to "${user.displayName} submitted Rs. ${total.toLong()} via $paymentMethod for ${project.title} (${pack.packName} x$quantity).",
                "eventType" to "NEW_INVESTMENT_REQUEST",
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "payment_requests")
        Result.failure(e)
    }

    suspend fun requestBuyback(ownership: OwnershipRecord): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == ownership.userId)
        require(ownership.status == "ACTIVE") { "Only active ownership shares can request buyback." }

        val buybackId = newId("bb")
        val batch = db.batch()
        batch.update(
            db.collection("ownerships").document(ownership.id),
            mapOf(
                "status" to "BUYBACK_REQUESTED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("buyback_requests").document(buybackId),
            mapOf(
                "ownershipId" to ownership.id,
                "userId" to uid,
                "userName" to ownership.userName,
                "projectId" to ownership.projectId,
                "projectTitle" to ownership.projectTitle,
                "originalValue" to ownership.purchasePrice,
                "offeredPrice" to 0.0,
                "deductionAmount" to 0.0,
                "netPayout" to 0.0,
                "status" to "REQUESTED",
                "adminNote" to "Awaiting admin review & fund availability confirmation.",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("notifications").document(newId("notif")),
            mapOf(
                "userId" to uid,
                "targetRole" to "ADMIN",
                "title" to "Buyback Request Submitted",
                "body" to "${ownership.userName} requested buyback for ${ownership.projectTitle} (Rs. ${ownership.purchasePrice.toLong()}).",
                "eventType" to "BUYBACK_REQUEST",
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "buyback_requests")
        Result.failure(e)
    }

    suspend fun respondToBuybackOffer(buyback: BuybackRequest, accept: Boolean): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == buyback.userId)
        require(buyback.status == "OFFERED") { "Buyback is not awaiting your response." }
        val nextStatus = if (accept) "ACCEPTED_BY_USER" else "REJECTED"
        db.collection("buyback_requests").document(buyback.id).update(
            mapOf(
                "status" to nextStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "buyback_requests/${buyback.id}")
        Result.failure(e)
    }

    suspend fun payMonthlySubscriptionFromWallet(
        user: UserAccount,
        cycle: SubscriptionCycle
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == user.userId)
        val share = cycle.calculatedSharePerMember
        val docId = "${uid}_${cycle.billingMonth}"
        val subPayRef = db.collection("subscription_payments").document(docId)
        val userRef = db.collection("users").document(uid)

        db.runTransaction { tx ->
            val existingSub = tx.get(subPayRef)
            if (existingSub.exists()) {
                throw IllegalStateException("Subscription for ${cycle.billingMonth} is already paid.")
            }
            val userSnap = tx.get(userRef)
            val currentBal = userSnap.getDouble("availableBalance") ?: 0.0
            if (currentBal < share) {
                throw IllegalStateException("Insufficient available wallet balance (need Rs. $share).")
            }
            val newBal = FinancialCalculator.round2(currentBal - share)
            tx.update(
                userRef,
                mapOf(
                    "availableBalance" to newBal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                subPayRef,
                mapOf(
                    "userId" to uid,
                    "userName" to user.displayName,
                    "billingMonth" to cycle.billingMonth,
                    "cycleId" to cycle.id,
                    "amountPaid" to share,
                    "paymentMethod" to "WALLET",
                    "status" to "PAID",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                db.collection("ledger_entries").document(newId("led")),
                mapOf(
                    "userId" to uid,
                    "entryType" to "DEBIT",
                    "category" to "SUBSCRIPTION_PAID",
                    "amount" to share,
                    "balanceAfter" to newBal,
                    "referenceId" to docId,
                    "description" to "Monthly company expense share (${cycle.billingMonth})",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "subscription_payments")
        Result.failure(e)
    }

    suspend fun submitWithdrawalRequest(
        user: UserAccount,
        amount: Double,
        method: String,
        accountTitle: String,
        accountNumber: String,
        pendingWithdrawalsTotal: Double
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == user.userId)
        val eligibleAvailable = FinancialCalculator.round2(user.availableBalance - pendingWithdrawalsTotal)
        require(amount > 0.0 && amount <= eligibleAvailable) {
            "Amount must be between Rs. 1 and your eligible available balance (Rs. $eligibleAvailable)."
        }
        require(accountTitle.trim().length in 2..100) { "Enter valid account title." }
        require(accountNumber.trim().length in 4..60) { "Enter valid account number / IBAN." }

        val wId = newId("wd")
        val batch = db.batch()
        batch.set(
            db.collection("withdrawals").document(wId),
            mapOf(
                "userId" to uid,
                "userName" to user.displayName.ifBlank { user.email },
                "amount" to FinancialCalculator.round2(amount),
                "method" to method,
                "accountTitle" to accountTitle.trim(),
                "accountNumber" to accountNumber.trim(),
                "status" to "PENDING",
                "adminNote" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("notifications").document(newId("notif")),
            mapOf(
                "userId" to uid,
                "targetRole" to "ADMIN",
                "title" to "Withdrawal Request",
                "body" to "${user.displayName} requested Rs. ${amount.toLong()} withdrawal via $method.",
                "eventType" to "WITHDRAWAL_REQUEST",
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "withdrawals")
        Result.failure(e)
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = try {
        requireUserId()
        db.collection("notifications").document(notificationId).update(
            mapOf(
                "read" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
        Result.failure(e)
    }

    // --- ADMIN ACTIONS ---

    suspend fun approvePaymentRequest(
        request: PaymentRequest,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val reqRef = db.collection("payment_requests").document(request.id)
        val projRef = db.collection("projects").document(request.projectId)
        val packRef = projRef.collection("packs").document(request.packId)
        val userRef = db.collection("users").document(request.userId)
        val ownershipId = newId("own")

        db.runTransaction { tx ->
            val reqSnap = tx.get(reqRef)
            val status = reqSnap.getString("status")
            if (status != "PENDING") {
                throw IllegalStateException("Payment request was already processed ($status).")
            }
            val packSnap = tx.get(packRef)
            val projSnap = tx.get(projRef)
            val userSnap = tx.get(userRef)

            val availablePacks = packSnap.getLong("availablePacks") ?: 0L
            val unitsPerPack = packSnap.getLong("unitsPerPack") ?: 1L
            val unitsPurchased = request.quantity * unitsPerPack
            if (availablePacks < request.quantity) {
                throw IllegalStateException("Insufficient pack inventory remaining ($availablePacks left).")
            }
            val projTotalUnits = (projSnap.getLong("totalUnits") ?: 1L).coerceAtLeast(1L)
            val projAvailUnits = (projSnap.getLong("availableUnits") ?: 0L)
            val nextAvailUnits = (projAvailUnits - unitsPurchased).coerceAtLeast(0L)
            val ownershipPct = FinancialCalculator.round2((unitsPurchased.toDouble() / projTotalUnits.toDouble()) * 100.0)
                .coerceIn(0.01, 100.0)

            val prevInvested = userSnap.getDouble("investedCapital") ?: 0.0
            val availBalance = userSnap.getDouble("availableBalance") ?: 0.0

            tx.update(
                reqRef,
                mapOf(
                    "status" to "APPROVED",
                    "adminNote" to "Payment verified & product share allocated",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(
                packRef,
                mapOf(
                    "availablePacks" to (availablePacks - request.quantity),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(
                projRef,
                mapOf(
                    "availableUnits" to nextAvailUnits,
                    "status" to if (nextAvailUnits == 0L) "SOLD_OUT" else "OPEN",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                db.collection("ownerships").document(ownershipId),
                mapOf(
                    "userId" to request.userId,
                    "userName" to request.userName,
                    "projectId" to request.projectId,
                    "projectTitle" to request.projectTitle,
                    "packId" to request.packId,
                    "packName" to request.packName,
                    "paymentRequestId" to request.id,
                    "quantity" to request.quantity,
                    "unitsOwned" to unitsPurchased,
                    "purchasePrice" to request.totalAmount,
                    "ownershipPercent" to ownershipPct,
                    "status" to "ACTIVE",
                    "realizedCapitalReturned" to 0.0,
                    "realizedProfitEarned" to 0.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(
                userRef,
                mapOf(
                    "investedCapital" to FinancialCalculator.round2(prevInvested + request.totalAmount),
                    "eligibleMember" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                db.collection("ledger_entries").document(newId("led")),
                mapOf(
                    "userId" to request.userId,
                    "entryType" to "DEBIT",
                    "category" to "CAPITAL_INVESTED",
                    "amount" to request.totalAmount,
                    "balanceAfter" to availBalance,
                    "referenceId" to ownershipId,
                    "description" to "Verified ${request.packName} x${request.quantity} in ${request.projectTitle}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                db.collection("notifications").document(newId("notif")),
                mapOf(
                    "userId" to request.userId,
                    "targetRole" to "USER",
                    "title" to "Payment Approved & Share Allocated",
                    "body" to "Your Rs. ${request.totalAmount.toLong()} payment for ${request.projectTitle} is verified. Ownership ($ownershipPct%) is now active.",
                    "eventType" to "PAYMENT_APPROVED",
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                db.collection("audit_logs").document(newId("aud")),
                mapOf(
                    "adminUid" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to "APPROVE_PAYMENT",
                    "targetId" to request.id,
                    "details" to "Approved Rs. ${request.totalAmount} for ${request.userName} -> Ownership $ownershipId",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()

        // Activate referral eligibility if user was referred
        val refDoc = db.collection("referrals").document("ref_${request.userId}")
        if (refDoc.get().await().exists()) {
            refDoc.update(
                mapOf(
                    "activeEligible" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "payment_requests/${request.id}")
        Result.failure(e)
    }

    suspend fun rejectPaymentRequest(
        request: PaymentRequest,
        reason: String,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val reqRef = db.collection("payment_requests").document(request.id)
        db.runTransaction { tx ->
            val snap = tx.get(reqRef)
            if (snap.getString("status") != "PENDING") {
                throw IllegalStateException("Payment request already processed.")
            }
            tx.update(
                reqRef,
                mapOf(
                    "status" to "REJECTED",
                    "adminNote" to reason.ifBlank { "Payment could not be verified" }.take(280),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                db.collection("notifications").document(newId("notif")),
                mapOf(
                    "userId" to request.userId,
                    "targetRole" to "USER",
                    "title" to "Payment Verification Rejected",
                    "body" to "Payment ${request.transactionRef} for ${request.projectTitle} was rejected: ${reason.ifBlank { "Unverified receipt" }}",
                    "eventType" to "PAYMENT_REJECTED",
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                db.collection("audit_logs").document(newId("aud")),
                mapOf(
                    "adminUid" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to "REJECT_PAYMENT",
                    "targetId" to request.id,
                    "details" to "Rejected Rs. ${request.totalAmount} (${request.transactionRef}): $reason",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "payment_requests/${request.id}")
        Result.failure(e)
    }

    suspend fun recordSaleAndDistributeProfit(
        project: ProductProject,
        unitsSold: Long,
        actualRevenue: Double,
        eligibleExpenses: Double,
        notes: String,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        require(unitsSold > 0 && unitsSold <= project.totalUnits) {
            "Units sold must be between 1 and ${project.totalUnits}."
        }
        val calc = FinancialCalculator.calculateSaleDistribution(
            unitsSold = unitsSold,
            projectTotalUnits = project.totalUnits,
            projectPurchaseCost = project.purchaseCost,
            actualRevenue = actualRevenue,
            eligibleExpenses = eligibleExpenses
        )

        val ownershipSnap = db.collection("ownerships")
            .whereEqualTo("projectId", project.id)
            .get().await()
        val activeOwnerships = ownershipSnap.toObjects(
            OwnershipRecord::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).filter { it.status == "ACTIVE" || it.status == "PARTIALLY_SOLD" }

        val totalOwnedUnits = activeOwnerships.sumOf { it.unitsOwned }.coerceAtLeast(1L)
        val soldRatio = (unitsSold.toDouble() / project.totalUnits.coerceAtLeast(1L).toDouble()).coerceIn(0.0, 1.0)
        val saleId = newId("sale")

        val batch = db.batch()
        batch.set(
            db.collection("product_sales").document(saleId),
            mapOf(
                "projectId" to project.id,
                "projectTitle" to project.title,
                "unitsSold" to unitsSold,
                "actualRevenue" to FinancialCalculator.round2(actualRevenue),
                "allocatedCost" to calc.allocatedCost,
                "eligibleExpenses" to FinancialCalculator.round2(eligibleExpenses),
                "grossProfit" to calc.grossProfit,
                "netDistributableProfit" to calc.netDistributableProfit,
                "platformCommission" to calc.platformCommission,
                "investorProfitPool" to calc.investorProfitPool,
                "recordedBy" to adminUid,
                "notes" to notes.ifBlank { "Physical batch sale settled" }.take(380),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val nextStatus = if (soldRatio >= 0.999) "SOLD" else "PARTIALLY_SOLD"
        val userAdditions = mutableMapOf<String, Pair<Double, Double>>() // uid -> (capitalReturn, profitShare)
        activeOwnerships.forEach { own ->
            val shareFraction = own.unitsOwned.toDouble() / totalOwnedUnits.toDouble()
            val capitalReturn = FinancialCalculator.round2(own.purchasePrice * soldRatio)
                .coerceAtMost((own.purchasePrice - own.realizedCapitalReturned).coerceAtLeast(0.0))
            val profitShare = FinancialCalculator.round2(calc.investorProfitPool * shareFraction)

            val prev = userAdditions[own.userId] ?: (0.0 to 0.0)
            userAdditions[own.userId] = (prev.first + capitalReturn) to (prev.second + profitShare)

            batch.update(
                db.collection("ownerships").document(own.id),
                mapOf(
                    "status" to nextStatus,
                    "realizedCapitalReturned" to FinancialCalculator.round2(own.realizedCapitalReturned + capitalReturn),
                    "realizedProfitEarned" to FinancialCalculator.round2(own.realizedProfitEarned + profitShare),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        // Credit each investor's wallet & record immutable ledger entries
        userAdditions.forEach { (investorUid, amounts) ->
            val (capitalReturn, profitShare) = amounts
            val uRef = db.collection("users").document(investorUid)
            val uSnap = uRef.get().await()
            val curAvail = uSnap.getDouble("availableBalance") ?: 0.0
            val curProfit = uSnap.getDouble("realizedProfit") ?: 0.0
            val curInvested = uSnap.getDouble("investedCapital") ?: 0.0

            val afterCap = FinancialCalculator.round2(curAvail + capitalReturn)
            val afterProfit = FinancialCalculator.round2(afterCap + profitShare)

            batch.update(
                uRef,
                mapOf(
                    "availableBalance" to afterProfit,
                    "realizedProfit" to FinancialCalculator.round2(curProfit + profitShare),
                    "investedCapital" to (curInvested - capitalReturn).coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (capitalReturn > 0.0) {
                batch.set(
                    db.collection("ledger_entries").document(newId("led")),
                    mapOf(
                        "userId" to investorUid,
                        "entryType" to "CREDIT",
                        "category" to "CAPITAL_RETURNED",
                        "amount" to capitalReturn,
                        "balanceAfter" to afterCap,
                        "referenceId" to saleId,
                        "description" to "Original capital returned from ${project.title} sale",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (profitShare > 0.0) {
                batch.set(
                    db.collection("ledger_entries").document(newId("led")),
                    mapOf(
                        "userId" to investorUid,
                        "entryType" to "CREDIT",
                        "category" to "PROFIT_DISTRIBUTED",
                        "amount" to profitShare,
                        "balanceAfter" to afterProfit,
                        "referenceId" to saleId,
                        "description" to "Net profit share (after 10% platform commission) from ${project.title}",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                db.collection("notifications").document(newId("notif")),
                mapOf(
                    "userId" to investorUid,
                    "targetRole" to "USER",
                    "title" to "Product Sale & Profit Distributed",
                    "body" to "${project.title} sale settled: Rs. ${capitalReturn.toLong()} capital + Rs. ${profitShare.toLong()} net profit credited to your wallet.",
                    "eventType" to "PROFIT_DISTRIBUTION",
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "RECORD_SALE_AND_DISTRIBUTE",
                "targetId" to saleId,
                "details" to "${project.title}: Sold $unitsSold units, Rev Rs. $actualRevenue, Commission Rs. ${calc.platformCommission}, Investor Pool Rs. ${calc.investorProfitPool}",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "product_sales")
        Result.failure(e)
    }

    suspend fun offerBuyback(
        buyback: BuybackRequest,
        offeredPrice: Double,
        adminNote: String,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val calc = FinancialCalculator.calculateBuybackPayout(offeredPrice)
        val batch = db.batch()
        batch.update(
            db.collection("buyback_requests").document(buyback.id),
            mapOf(
                "offeredPrice" to calc.offeredPrice,
                "deductionAmount" to calc.deductionAmount,
                "netPayout" to calc.netPayout,
                "status" to "OFFERED",
                "adminNote" to adminNote.ifBlank { "Offer ready: 10% deduction applies." }.take(280),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("notifications").document(newId("notif")),
            mapOf(
                "userId" to buyback.userId,
                "targetRole" to "USER",
                "title" to "Buyback Offer Received",
                "body" to "Admin offered Rs. ${calc.offeredPrice.toLong()} (Net Rs. ${calc.netPayout.toLong()} after 10% deduction) for ${buyback.projectTitle}.",
                "eventType" to "BUYBACK_OFFERED",
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "OFFER_BUYBACK",
                "targetId" to buyback.id,
                "details" to "Offered Rs. ${calc.offeredPrice} (Net Rs. ${calc.netPayout}) to ${buyback.userName}",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "buyback_requests/${buyback.id}")
        Result.failure(e)
    }

    suspend fun completeBuyback(
        buyback: BuybackRequest,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val bbRef = db.collection("buyback_requests").document(buyback.id)
        val ownRef = db.collection("ownerships").document(buyback.ownershipId)
        val userRef = db.collection("users").document(buyback.userId)

        db.runTransaction { tx ->
            val bbSnap = tx.get(bbRef)
            val status = bbSnap.getString("status")
            if (status != "ACCEPTED_BY_USER" && status != "REQUESTED") {
                throw IllegalStateException("Buyback cannot be finalized in state $status.")
            }
            val offered = if ((bbSnap.getDouble("offeredPrice") ?: 0.0) > 0.0) {
                bbSnap.getDouble("offeredPrice") ?: buyback.originalValue
            } else {
                buyback.originalValue
            }
            val calc = FinancialCalculator.calculateBuybackPayout(offered)
            val userSnap = tx.get(userRef)
            val avail = userSnap.getDouble("availableBalance") ?: 0.0
            val bbProceeds = userSnap.getDouble("buybackProceeds") ?: 0.0
            val invested = userSnap.getDouble("investedCapital") ?: 0.0
            val newAvail = FinancialCalculator.round2(avail + calc.netPayout)

            tx.update(
                bbRef,
                mapOf(
                    "offeredPrice" to calc.offeredPrice,
                    "deductionAmount" to calc.deductionAmount,
                    "netPayout" to calc.netPayout,
                    "status" to "COMPLETED",
                    "adminNote" to "Funds confirmed & ownership transferred.",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(
                ownRef,
                mapOf(
                    "status" to "BOUGHT_BACK",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(
                userRef,
                mapOf(
                    "availableBalance" to newAvail,
                    "buybackProceeds" to FinancialCalculator.round2(bbProceeds + calc.netPayout),
                    "investedCapital" to (invested - buyback.originalValue).coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                db.collection("ledger_entries").document(newId("led")),
                mapOf(
                    "userId" to buyback.userId,
                    "entryType" to "CREDIT",
                    "category" to "BUYBACK_PROCEEDS",
                    "amount" to calc.netPayout,
                    "balanceAfter" to newAvail,
                    "referenceId" to buyback.id,
                    "description" to "Buyback net payout for ${buyback.projectTitle} (Gross Rs. ${calc.offeredPrice} - 10% Rs. ${calc.deductionAmount})",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                db.collection("notifications").document(newId("notif")),
                mapOf(
                    "userId" to buyback.userId,
                    "targetRole" to "USER",
                    "title" to "Buyback Completed",
                    "body" to "Rs. ${calc.netPayout.toLong()} has been credited to your available balance for ${buyback.projectTitle}.",
                    "eventType" to "BUYBACK_APPROVED",
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                db.collection("audit_logs").document(newId("aud")),
                mapOf(
                    "adminUid" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to "COMPLETE_BUYBACK",
                    "targetId" to buyback.id,
                    "details" to "Completed buyback for ${buyback.userName}: Net Rs. ${calc.netPayout} credited",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "buyback_requests/${buyback.id}")
        Result.failure(e)
    }

    suspend fun processWithdrawal(
        withdrawal: WithdrawalRequest,
        approve: Boolean,
        adminNote: String,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val wRef = db.collection("withdrawals").document(withdrawal.id)
        val uRef = db.collection("users").document(withdrawal.userId)

        db.runTransaction { tx ->
            val wSnap = tx.get(wRef)
            if (wSnap.getString("status") != "PENDING") {
                throw IllegalStateException("Withdrawal request already processed.")
            }
            val uSnap = tx.get(uRef)
            val avail = uSnap.getDouble("availableBalance") ?: 0.0
            if (approve) {
                if (avail < withdrawal.amount) {
                    throw IllegalStateException("User available balance (Rs. $avail) is below withdrawal amount.")
                }
                val newAvail = FinancialCalculator.round2(avail - withdrawal.amount)
                tx.update(
                    uRef,
                    mapOf(
                        "availableBalance" to newAvail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    wRef,
                    mapOf(
                        "status" to "APPROVED",
                        "adminNote" to adminNote.ifBlank { "Paid via ${withdrawal.method}" }.take(280),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("ledger_entries").document(newId("led")),
                    mapOf(
                        "userId" to withdrawal.userId,
                        "entryType" to "DEBIT",
                        "category" to "WITHDRAWAL_COMPLETED",
                        "amount" to withdrawal.amount,
                        "balanceAfter" to newAvail,
                        "referenceId" to withdrawal.id,
                        "description" to "Verified withdrawal via ${withdrawal.method} (${withdrawal.accountNumber})",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("notifications").document(newId("notif")),
                    mapOf(
                        "userId" to withdrawal.userId,
                        "targetRole" to "USER",
                        "title" to "Withdrawal Approved",
                        "body" to "Your Rs. ${withdrawal.amount.toLong()} withdrawal to ${withdrawal.method} has been completed.",
                        "eventType" to "WITHDRAWAL_APPROVED",
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                tx.update(
                    wRef,
                    mapOf(
                        "status" to "REJECTED",
                        "adminNote" to adminNote.ifBlank { "Withdrawal declined by admin" }.take(280),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("notifications").document(newId("notif")),
                    mapOf(
                        "userId" to withdrawal.userId,
                        "targetRole" to "USER",
                        "title" to "Withdrawal Rejected",
                        "body" to "Your Rs. ${withdrawal.amount.toLong()} withdrawal was rejected: ${adminNote.ifBlank { "Check account details" }}",
                        "eventType" to "WITHDRAWAL_REJECTED",
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            tx.set(
                db.collection("audit_logs").document(newId("aud")),
                mapOf(
                    "adminUid" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to if (approve) "APPROVE_WITHDRAWAL" else "REJECT_WITHDRAWAL",
                    "targetId" to withdrawal.id,
                    "details" to "${if (approve) "Approved" else "Rejected"} Rs. ${withdrawal.amount} for ${withdrawal.userName}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "withdrawals/${withdrawal.id}")
        Result.failure(e)
    }

    suspend fun createProjectWithPack(
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
        unitsPerPack: Long,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val projId = newId("proj")
        val packId = newId("pack")
        val totalPacks = (totalUnits / unitsPerPack.coerceAtLeast(1L)).coerceAtLeast(1L)
        val cleanGallery = imageGallery.map { it.trim() }.filter { it.isNotBlank() }.take(6)
        val primaryImage = cleanGallery.firstOrNull() ?: imageKey.ifBlank { "detergent" }
        val batch = db.batch()
        batch.set(
            db.collection("projects").document(projId),
            mapOf(
                "title" to title.trim(),
                "category" to category.trim(),
                "description" to description.trim(),
                "imageKey" to primaryImage.take(195000),
                "imageGallery" to cleanGallery,
                "purchaseCost" to FinancialCalculator.round2(purchaseCost),
                "expectedSellingPrice" to FinancialCalculator.round2(expectedSellingPrice),
                "totalUnits" to totalUnits,
                "availableUnits" to totalUnits,
                "published" to published,
                "status" to "OPEN",
                "createdBy" to adminUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("projects").document(projId).collection("packs").document(packId),
            mapOf(
                "projectId" to projId,
                "packName" to packName.trim(),
                "price" to FinancialCalculator.round2(packPrice),
                "unitsPerPack" to unitsPerPack,
                "totalPacks" to totalPacks,
                "availablePacks" to totalPacks,
                "minQty" to 1L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "CREATE_PROJECT",
                "targetId" to projId,
                "details" to "Created project '$title' with ${cleanGallery.size} photo(s) and pack '$packName' (Rs. $packPrice)",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "projects")
        Result.failure(e)
    }

    suspend fun updateProject(
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
        status: String,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val safeTotal = totalUnits.coerceAtLeast(1L)
        val safeAvail = availableUnits.coerceIn(0L, safeTotal)
        val cleanGallery = imageGallery.map { it.trim() }.filter { it.isNotBlank() }.take(6)
        val primaryImage = cleanGallery.firstOrNull() ?: imageKey.ifBlank { "detergent" }
        val batch = db.batch()
        batch.update(
            db.collection("projects").document(project.id),
            mapOf(
                "title" to title.trim(),
                "category" to category.trim(),
                "description" to description.trim(),
                "imageKey" to primaryImage.take(195000),
                "imageGallery" to cleanGallery,
                "purchaseCost" to FinancialCalculator.round2(purchaseCost),
                "expectedSellingPrice" to FinancialCalculator.round2(expectedSellingPrice),
                "totalUnits" to safeTotal,
                "availableUnits" to safeAvail,
                "published" to published,
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "UPDATE_PROJECT",
                "targetId" to project.id,
                "details" to "Updated project '${title.trim()}' (Cost Rs. $purchaseCost, Target Rs. $expectedSellingPrice, Photos: ${cleanGallery.size})",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "projects/${project.id}")
        Result.failure(e)
    }

    suspend fun addPackToProject(
        project: ProductProject,
        packName: String,
        price: Double,
        unitsPerPack: Long,
        totalPacks: Long,
        minQty: Long,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val packId = newId("pack")
        val batch = db.batch()
        batch.set(
            db.collection("projects").document(project.id).collection("packs").document(packId),
            mapOf(
                "projectId" to project.id,
                "packName" to packName.trim(),
                "price" to FinancialCalculator.round2(price),
                "unitsPerPack" to unitsPerPack.coerceAtLeast(1L),
                "totalPacks" to totalPacks.coerceAtLeast(1L),
                "availablePacks" to totalPacks.coerceAtLeast(1L),
                "minQty" to minQty.coerceAtLeast(1L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "ADD_PACK",
                "targetId" to project.id,
                "details" to "Added pack '$packName' (Rs. $price, $totalPacks packs) to ${project.title}",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "projects/${project.id}/packs")
        Result.failure(e)
    }

    suspend fun updatePackInProject(
        projectId: String,
        pack: InvestmentPack,
        packName: String,
        price: Double,
        unitsPerPack: Long,
        totalPacks: Long,
        availablePacks: Long,
        minQty: Long,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val safeTotal = totalPacks.coerceAtLeast(1L)
        val safeAvail = availablePacks.coerceIn(0L, safeTotal)
        val batch = db.batch()
        batch.update(
            db.collection("projects").document(projectId).collection("packs").document(pack.id),
            mapOf(
                "packName" to packName.trim(),
                "price" to FinancialCalculator.round2(price),
                "unitsPerPack" to unitsPerPack.coerceAtLeast(1L),
                "totalPacks" to safeTotal,
                "availablePacks" to safeAvail,
                "minQty" to minQty.coerceAtLeast(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "UPDATE_PACK",
                "targetId" to pack.id,
                "details" to "Updated pack '${packName.trim()}' to Rs. $price ($safeAvail/$safeTotal packs)",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "projects/$projectId/packs/${pack.id}")
        Result.failure(e)
    }

    suspend fun deletePackFromProject(
        projectId: String,
        pack: InvestmentPack,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val batch = db.batch()
        batch.delete(db.collection("projects").document(projectId).collection("packs").document(pack.id))
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "DELETE_PACK",
                "targetId" to pack.id,
                "details" to "Deleted pack '${pack.packName}' from project $projectId",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "projects/$projectId/packs/${pack.id}")
        Result.failure(e)
    }

    suspend fun toggleProjectPublish(
        project: ProductProject,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val nextPublished = !project.published
        val batch = db.batch()
        batch.update(
            db.collection("projects").document(project.id),
            mapOf(
                "published" to nextPublished,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to if (nextPublished) "PUBLISH_PROJECT" else "UNPUBLISH_PROJECT",
                "targetId" to project.id,
                "details" to "${if (nextPublished) "Published" else "Unpublished"} ${project.title}",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "projects/${project.id}")
        Result.failure(e)
    }

    suspend fun deleteProject(
        project: ProductProject,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val packsSnap = db.collection("projects").document(project.id).collection("packs").get().await()
        val batch = db.batch()
        packsSnap.documents.forEach { batch.delete(it.reference) }
        batch.delete(db.collection("projects").document(project.id))
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "DELETE_PROJECT",
                "targetId" to project.id,
                "details" to "Deleted project ${project.title}",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "projects/${project.id}")
        Result.failure(e)
    }

    suspend fun addCompanyExpense(
        billingMonth: String,
        category: String,
        description: String,
        amount: Double,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val expId = newId("exp")
        val batch = db.batch()
        batch.set(
            db.collection("company_expenses").document(expId),
            mapOf(
                "billingMonth" to billingMonth,
                "category" to category,
                "description" to description.trim(),
                "amount" to FinancialCalculator.round2(amount),
                "createdBy" to adminUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "ADD_COMPANY_EXPENSE",
                "targetId" to expId,
                "details" to "Added $category expense Rs. $amount for $billingMonth ($description)",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "company_expenses")
        Result.failure(e)
    }

    suspend fun publishSubscriptionCycle(
        billingMonth: String,
        totalExpenses: Double,
        carryForwardAdjustment: Double,
        eligibleMembers: Long,
        roundingRule: String,
        promoEnabled: Boolean,
        promoAmount: Double,
        promoNote: String,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val safeMembers = eligibleMembers.coerceAtLeast(1L)
        val share = FinancialCalculator.calculateMonthlyShare(
            totalExpenses = totalExpenses,
            carryForwardAdjustment = carryForwardAdjustment,
            eligibleMemberCount = safeMembers,
            roundingRule = roundingRule,
            promoEnabled = promoEnabled,
            promoAmount = promoAmount
        )
        val cycleId = "cycle_$billingMonth"
        val ref = db.collection("subscription_cycles").document(cycleId)
        val existing = ref.get().await()
        val payload = mapOf(
            "billingMonth" to billingMonth,
            "totalExpenses" to FinancialCalculator.round2(totalExpenses),
            "carryForwardAdjustment" to FinancialCalculator.round2(carryForwardAdjustment),
            "eligibleMemberCount" to safeMembers,
            "roundingRule" to roundingRule,
            "promoEnabled" to promoEnabled,
            "promoAmount" to FinancialCalculator.round2(promoAmount),
            "promoEligibilityNote" to promoNote.ifBlank { "Standard active member expense sharing" }.take(280),
            "calculatedSharePerMember" to share,
            "status" to "OPEN",
            "createdAt" to (existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val batch = db.batch()
        batch.set(ref, payload)
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "PUBLISH_SUBSCRIPTION_CYCLE",
                "targetId" to cycleId,
                "details" to "Published $billingMonth cycle: Rs. $share/member ($safeMembers eligible, promo=$promoEnabled)",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "subscription_cycles/cycle_$billingMonth")
        Result.failure(e)
    }

    suspend fun savePaymentChannel(
        channelId: String?,
        methodType: String,
        accountTitle: String,
        accountNumber: String,
        bankName: String,
        instructions: String,
        adminWhatsapp: String,
        active: Boolean,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val id = channelId ?: newId("ch")
        val ref = db.collection("payment_channels").document(id)
        val existing = ref.get().await()
        val batch = db.batch()
        batch.set(
            ref,
            mapOf(
                "methodType" to methodType,
                "accountTitle" to accountTitle.trim(),
                "accountNumber" to accountNumber.trim(),
                "bankName" to bankName.trim(),
                "instructions" to instructions.trim(),
                "adminWhatsapp" to adminWhatsapp.trim(),
                "active" to active,
                "createdAt" to (existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "UPDATE_PAYMENT_CHANNEL",
                "targetId" to id,
                "details" to "Saved $methodType channel ($accountNumber)",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "payment_channels")
        Result.failure(e)
    }

    suspend fun adminUpdateUserStatus(
        targetUser: UserAccount,
        eligibleMember: Boolean,
        adminUid: String,
        adminEmail: String
    ): Result<Unit> = try {
        requireUserId()
        val role = if (isAdminEmail(targetUser.email)) "ADMIN" else "INVESTOR"
        val batch = db.batch()
        batch.update(
            db.collection("users").document(targetUser.userId),
            mapOf(
                "eligibleMember" to eligibleMember,
                "role" to role,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("audit_logs").document(newId("aud")),
            mapOf(
                "adminUid" to adminUid,
                "adminEmail" to adminEmail,
                "action" to "UPDATE_USER_ELIGIBILITY",
                "targetId" to targetUser.userId,
                "details" to "Set ${targetUser.email} eligible=$eligibleMember (role=$role)",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${targetUser.userId}")
        Result.failure(e)
    }
}
