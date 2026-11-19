package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.MarketplaceRepository
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.AuthFlowMode
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
import com.example.data.model.SessionRegistrationState
import com.example.data.model.SubscriptionCycle
import com.example.data.model.SubscriptionPayment
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.WithdrawalRequest
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceViewModel(
    private val repository: MarketplaceRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserName: String,
    private val initialAuthMode: AuthFlowMode = AuthFlowMode.LOGIN,
    private val initialRegistrationDraft: RegistrationDraft? = null
) : ViewModel() {

    val isAdmin: StateFlow<Boolean> = MutableStateFlow(
        MarketplaceRepository.isAdminEmail(currentUserEmail)
    ).asStateFlow()

    private val _sessionState = MutableStateFlow<SessionRegistrationState>(
        SessionRegistrationState.Checking
    )
    val sessionState: StateFlow<SessionRegistrationState> = _sessionState.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    init {
        checkSessionRegistration(initialRegistrationDraft)
    }

    fun retrySessionCheck() {
        checkSessionRegistration(initialRegistrationDraft)
    }

    private fun checkSessionRegistration(draft: RegistrationDraft?) {
        viewModelScope.launch {
            _sessionState.value = SessionRegistrationState.Checking
            val result = repository.checkAndBootstrapSession(
                uid = currentUserId,
                displayName = currentUserName,
                email = currentUserEmail,
                pendingDraft = draft
            )
            result.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is MarketplaceRepository.SessionBootstrapOutcome.Ready -> {
                            _sessionState.value = SessionRegistrationState.Ready
                            if (!MarketplaceRepository.isAdminEmail(currentUserEmail)) {
                                if (!outcome.alreadyRegistered) {
                                    _feedbackMessage.value = "Registration complete! Welcome to StockShare."
                                } else if (initialAuthMode == AuthFlowMode.REGISTER) {
                                    _feedbackMessage.value = "Account already registered — logged in!"
                                }
                            }
                        }
                        is MarketplaceRepository.SessionBootstrapOutcome.NeedsRegistration -> {
                            _sessionState.value = SessionRegistrationState.RegistrationRequired(
                                prefillName = draft?.fullName?.takeIf { it.isNotBlank() } ?: currentUserName,
                                email = currentUserEmail,
                                prefillPhone = draft?.phone.orEmpty(),
                                prefillReferral = draft?.referralCode.orEmpty(),
                                notice = outcome.correctionError
                            )
                        }
                    }
                },
                onFailure = { err ->
                    val message = err.localizedMessage ?: "Profile initialization failed"
                    _sessionState.value = SessionRegistrationState.Error(message)
                    _feedbackMessage.value = message
                }
            )
        }
    }

    fun completeRegistration(fullName: String, phone: String, referralCode: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.completeUserRegistration(
                uid = currentUserId,
                email = currentUserEmail,
                fullName = fullName,
                phone = phone,
                rawReferralCode = referralCode
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _sessionState.value = SessionRegistrationState.Ready
                    _feedbackMessage.value = "Registration complete! Welcome to StockShare."
                },
                onFailure = { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Could not complete registration"
                }
            )
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w("MarketplaceVM", "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val userProfile: StateFlow<UiState<UserAccount?>> =
        repository.observeUserProfile(currentUserId).toUiState("user profile")

    val publishedProjects: StateFlow<UiState<List<ProductProject>>> =
        repository.observePublishedProjects()
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toUiState("projects")

    val paymentChannels: StateFlow<UiState<List<PaymentChannel>>> =
        repository.observeActivePaymentChannels().toUiState("payment channels")

    val userPaymentRequests: StateFlow<UiState<List<PaymentRequest>>> =
        repository.observeUserPaymentRequests(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toUiState("payment requests")

    val userOwnerships: StateFlow<UiState<List<OwnershipRecord>>> =
        repository.observeUserOwnerships(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toUiState("ownerships")

    val productSales: StateFlow<UiState<List<ProductSale>>> =
        repository.observeProductSales()
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toUiState("product sales")

    val userBuybacks: StateFlow<UiState<List<BuybackRequest>>> =
        repository.observeUserBuybacks(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toUiState("buybacks")

    val companyExpenses: StateFlow<UiState<List<CompanyExpense>>> =
        repository.observeCompanyExpenses()
            .map { list -> list.sortedByDescending { it.amount } }
            .toUiState("company expenses")

    val subscriptionCycles: StateFlow<UiState<List<SubscriptionCycle>>> =
        repository.observeSubscriptionCycles()
            .map { list -> list.sortedByDescending { it.billingMonth } }
            .toUiState("subscription cycles")

    val userSubscriptionPayments: StateFlow<UiState<List<SubscriptionPayment>>> =
        repository.observeUserSubscriptionPayments(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toUiState("subscription payments")

    val userWithdrawals: StateFlow<UiState<List<WithdrawalRequest>>> =
        repository.observeUserWithdrawals(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toUiState("withdrawals")

    val userLedger: StateFlow<UiState<List<LedgerEntry>>> =
        repository.observeUserLedger(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toUiState("ledger")

    val userReferrals: StateFlow<UiState<List<ReferralRecord>>> =
        repository.observeUserReferrals(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toUiState("referrals")

    val userNotifications: StateFlow<UiState<List<AppNotification>>> =
        repository.observeUserNotifications(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .toUiState("notifications")

    // Admin-scoped reactive streams (only active when isAdmin == true!)
    val adminProjects: StateFlow<List<ProductProject>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllProjectsAdmin() else flowOf(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminPaymentRequests: StateFlow<List<PaymentRequest>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllPaymentRequestsAdmin() else flowOf(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminOwnerships: StateFlow<List<OwnershipRecord>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllOwnershipsAdmin() else flowOf(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminBuybacks: StateFlow<List<BuybackRequest>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllBuybacksAdmin() else flowOf(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminWithdrawals: StateFlow<List<WithdrawalRequest>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllWithdrawalsAdmin() else flowOf(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminUsers: StateFlow<List<UserAccount>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllUsersAdmin() else flowOf(emptyList())
        }
        .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminChannels: StateFlow<List<PaymentChannel>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllPaymentChannelsAdmin() else flowOf(emptyList())
        }
        .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminAuditLogs: StateFlow<List<AuditLog>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAuditLogsAdmin() else flowOf(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminNotifications: StateFlow<List<AppNotification>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAdminNotifications() else flowOf(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .catch { if (it is CancellationException) throw it else emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun observePacksForProject(projectId: String): Flow<List<InvestmentPack>> =
        repository.observePacks(projectId)
            .map { list -> list.sortedBy { it.price } }
            .catch { if (it is CancellationException) throw it else emit(emptyList()) }

    fun toggleTheme() {
        _darkTheme.value = !_darkTheme.value
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    private fun runMutation(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = block()
            _isSubmitting.value = false
            _feedbackMessage.value = result.fold(
                onSuccess = { successMessage },
                onFailure = { it.localizedMessage ?: "Operation failed" }
            )
        }
    }

    fun updateProfile(displayName: String, phone: String) =
        runMutation("Profile updated") {
            repository.updateUserProfile(currentUserId, displayName, phone)
        }

    fun applyReferralCode(userName: String, code: String) =
        runMutation("Referral code linked! Eligibility activates after your first verified pack purchase.") {
            repository.applyReferralCode(currentUserId, userName, code)
        }

    fun submitManualPayment(
        user: UserAccount,
        project: ProductProject,
        pack: InvestmentPack,
        quantity: Long,
        paymentMethod: String,
        transactionRef: String,
        proofSummary: String,
        proofUri: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.submitManualPayment(
                user, project, pack, quantity, paymentMethod, transactionRef, proofSummary, proofUri
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = {
                    _feedbackMessage.value = "Payment request submitted (PENDING admin verification)."
                    onDone()
                },
                onFailure = {
                    _feedbackMessage.value = it.localizedMessage ?: "Could not submit payment"
                }
            )
        }
    }

    fun requestBuyback(ownership: OwnershipRecord) =
        runMutation("Buyback request submitted for admin inspection.") {
            repository.requestBuyback(ownership)
        }

    fun respondToBuybackOffer(buyback: BuybackRequest, accept: Boolean) =
        runMutation(if (accept) "Buyback offer accepted; awaiting fund confirmation." else "Buyback offer declined.") {
            repository.respondToBuybackOffer(buyback, accept)
        }

    fun paySubscriptionFromWallet(user: UserAccount, cycle: SubscriptionCycle) =
        runMutation("Monthly expense share (${cycle.billingMonth}) paid from wallet.") {
            repository.payMonthlySubscriptionFromWallet(user, cycle)
        }

    fun submitWithdrawal(
        user: UserAccount,
        amount: Double,
        method: String,
        accountTitle: String,
        accountNumber: String,
        pendingTotal: Double
    ) = runMutation("Withdrawal request submitted for admin approval.") {
        repository.submitWithdrawalRequest(user, amount, method, accountTitle, accountNumber, pendingTotal)
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    // Admin actions
    fun approvePayment(request: PaymentRequest) =
        runMutation("Payment approved & ownership record created.") {
            repository.approvePaymentRequest(request, currentUserId, currentUserEmail)
        }

    fun rejectPayment(request: PaymentRequest, reason: String) =
        runMutation("Payment rejected.") {
            repository.rejectPaymentRequest(request, reason, currentUserId, currentUserEmail)
        }

    fun recordSale(
        project: ProductProject,
        unitsSold: Long,
        actualRevenue: Double,
        eligibleExpenses: Double,
        notes: String
    ) = runMutation("Sale recorded & net profits (90% investor pool / 10% commission) distributed.") {
        repository.recordSaleAndDistributeProfit(
            project, unitsSold, actualRevenue, eligibleExpenses, notes, currentUserId, currentUserEmail
        )
    }

    fun offerBuyback(buyback: BuybackRequest, offeredPrice: Double, note: String) =
        runMutation("Buyback offer sent to investor.") {
            repository.offerBuyback(buyback, offeredPrice, note, currentUserId, currentUserEmail)
        }

    fun completeBuyback(buyback: BuybackRequest) =
        runMutation("Buyback completed & net proceeds credited to investor.") {
            repository.completeBuyback(buyback, currentUserId, currentUserEmail)
        }

    fun processWithdrawal(withdrawal: WithdrawalRequest, approve: Boolean, note: String) =
        runMutation(if (approve) "Withdrawal approved & wallet deducted." else "Withdrawal rejected.") {
            repository.processWithdrawal(withdrawal, approve, note, currentUserId, currentUserEmail)
        }

    fun createProjectWithPack(
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
    ) = runMutation("Product project & initial pack created.") {
        repository.createProjectWithPack(
            title, category, description, imageKey, imageGallery, purchaseCost, expectedSellingPrice,
            totalUnits, published, packName, packPrice, unitsPerPack, currentUserId, currentUserEmail
        )
    }

    fun updateProject(
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
    ) = runMutation("Product project updated.") {
        repository.updateProject(
            project, title, category, description, imageKey, imageGallery, purchaseCost,
            expectedSellingPrice, totalUnits, availableUnits, published, status, currentUserId, currentUserEmail
        )
    }

    fun addPack(
        project: ProductProject,
        packName: String,
        price: Double,
        unitsPerPack: Long,
        totalPacks: Long,
        minQty: Long = 1L
    ) = runMutation("Investment pack added to ${project.title}.") {
        repository.addPackToProject(project, packName, price, unitsPerPack, totalPacks, minQty, currentUserId, currentUserEmail)
    }

    fun updatePack(
        projectId: String,
        pack: InvestmentPack,
        packName: String,
        price: Double,
        unitsPerPack: Long,
        totalPacks: Long,
        availablePacks: Long,
        minQty: Long
    ) = runMutation("Pack '${packName.trim()}' updated.") {
        repository.updatePackInProject(
            projectId, pack, packName, price, unitsPerPack, totalPacks, availablePacks, minQty, currentUserId, currentUserEmail
        )
    }

    fun deletePack(projectId: String, pack: InvestmentPack) =
        runMutation("Pack '${pack.packName}' deleted.") {
            repository.deletePackFromProject(projectId, pack, currentUserId, currentUserEmail)
        }

    fun togglePublish(project: ProductProject) =
        runMutation(if (project.published) "Project unpublished." else "Project published.") {
            repository.toggleProjectPublish(project, currentUserId, currentUserEmail)
        }

    fun deleteProject(project: ProductProject) =
        runMutation("Project deleted.") {
            repository.deleteProject(project, currentUserId, currentUserEmail)
        }

    fun addCompanyExpense(billingMonth: String, category: String, description: String, amount: Double) =
        runMutation("Monthly expense logged.") {
            repository.addCompanyExpense(billingMonth, category, description, amount, currentUserId, currentUserEmail)
        }

    fun publishSubscriptionCycle(
        billingMonth: String,
        totalExpenses: Double,
        carryForwardAdjustment: Double,
        eligibleMembers: Long,
        roundingRule: String,
        promoEnabled: Boolean,
        promoAmount: Double,
        promoNote: String
    ) = runMutation("Subscription cycle published for $billingMonth.") {
        repository.publishSubscriptionCycle(
            billingMonth, totalExpenses, carryForwardAdjustment, eligibleMembers,
            roundingRule, promoEnabled, promoAmount, promoNote, currentUserId, currentUserEmail
        )
    }

    fun savePaymentChannel(
        channelId: String?,
        methodType: String,
        accountTitle: String,
        accountNumber: String,
        bankName: String,
        instructions: String,
        adminWhatsapp: String,
        active: Boolean
    ) = runMutation("Payment channel saved.") {
        repository.savePaymentChannel(
            channelId, methodType, accountTitle, accountNumber, bankName,
            instructions, adminWhatsapp, active, currentUserId, currentUserEmail
        )
    }

    fun adminUpdateUser(target: UserAccount, eligibleMember: Boolean) =
        runMutation("Updated member eligibility for ${target.displayName}.") {
            repository.adminUpdateUserStatus(target, eligibleMember, currentUserId, currentUserEmail)
        }
}
