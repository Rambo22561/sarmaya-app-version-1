package com.example.domain

import kotlin.math.ceil
import kotlin.math.roundToLong

data class SaleDistributionCalculation(
    val allocatedCost: Double,
    val grossProfit: Double,
    val netDistributableProfit: Double,
    val platformCommission: Double,
    val investorProfitPool: Double
)

data class BuybackCalculation(
    val offeredPrice: Double,
    val deductionAmount: Double,
    val netPayout: Double
)

object FinancialCalculator {

    fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    /**
     * Product Sales and Profit Distribution:
     * - Allocated cost = proportional original product cost for units sold.
     * - Net distributable profit = actual revenue - allocated cost - eligible expenses.
     * - Platform commission = 10% of positive distributable profit only (0 if <= 0).
     * - Investor profit pool = remaining 90% of positive distributable profit.
     */
    fun calculateSaleDistribution(
        unitsSold: Long,
        projectTotalUnits: Long,
        projectPurchaseCost: Double,
        actualRevenue: Double,
        eligibleExpenses: Double
    ): SaleDistributionCalculation {
        val safeTotalUnits = projectTotalUnits.coerceAtLeast(1L)
        val allocatedCost = round2((projectPurchaseCost / safeTotalUnits.toDouble()) * unitsSold.toDouble())
        val grossProfit = round2(actualRevenue - allocatedCost)
        val netDistributableProfit = round2(actualRevenue - allocatedCost - eligibleExpenses)
        val platformCommission = if (netDistributableProfit > 0.0) {
            round2(netDistributableProfit * 0.10)
        } else {
            0.0
        }
        val investorProfitPool = if (netDistributableProfit > 0.0) {
            round2(netDistributableProfit - platformCommission)
        } else {
            0.0
        }
        return SaleDistributionCalculation(
            allocatedCost = allocatedCost,
            grossProfit = grossProfit,
            netDistributableProfit = netDistributableProfit,
            platformCommission = platformCommission,
            investorProfitPool = investorProfitPool
        )
    }

    /**
     * Buyback calculation:
     * - 10% admin deduction on agreed buyback price.
     */
    fun calculateBuybackPayout(offeredPrice: Double): BuybackCalculation {
        val safePrice = offeredPrice.coerceAtLeast(0.0)
        val deduction = round2(safePrice * 0.10)
        val net = round2(safePrice - deduction)
        return BuybackCalculation(
            offeredPrice = round2(safePrice),
            deductionAmount = deduction,
            netPayout = net
        )
    }

    /**
     * Monthly Company Expense Sharing Formula:
     * Monthly subscription = (Total eligible expenses + carryForwardAdjustment) / eligibleMembers
     * Or promotional subscription amount (e.g., Rs. 1) when promo is enabled.
     */
    fun calculateMonthlyShare(
        totalExpenses: Double,
        carryForwardAdjustment: Double,
        eligibleMemberCount: Long,
        roundingRule: String,
        promoEnabled: Boolean,
        promoAmount: Double
    ): Double {
        if (promoEnabled) return round2(promoAmount.coerceAtLeast(0.0))
        val safeMembers = eligibleMemberCount.coerceAtLeast(1L).toDouble()
        val raw = ((totalExpenses + carryForwardAdjustment).coerceAtLeast(0.0)) / safeMembers
        return when (roundingRule) {
            "CEIL" -> ceil(raw)
            "ROUND_NEAREST" -> raw.roundToLong().toDouble()
            else -> round2(raw)
        }
    }
}
