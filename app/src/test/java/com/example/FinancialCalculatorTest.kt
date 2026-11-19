package com.example

import com.example.domain.FinancialCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculatorTest {

    @Test
    fun saleDistribution_matchesPromptExample() {
        val result = FinancialCalculator.calculateSaleDistribution(
            unitsSold = 1000L,
            projectTotalUnits = 1000L,
            projectPurchaseCost = 100000.0,
            actualRevenue = 120000.0,
            eligibleExpenses = 0.0
        )
        assertEquals(100000.0, result.allocatedCost, 0.01)
        assertEquals(20000.0, result.grossProfit, 0.01)
        assertEquals(20000.0, result.netDistributableProfit, 0.01)
        assertEquals(2000.0, result.platformCommission, 0.01)
        assertEquals(18000.0, result.investorProfitPool, 0.01)
    }

    @Test
    fun saleDistribution_zeroCommissionOnLoss() {
        val result = FinancialCalculator.calculateSaleDistribution(
            unitsSold = 500L,
            projectTotalUnits = 1000L,
            projectPurchaseCost = 100000.0,
            actualRevenue = 45000.0,
            eligibleExpenses = 2000.0
        )
        assertEquals(50000.0, result.allocatedCost, 0.01)
        assertEquals(-7000.0, result.netDistributableProfit, 0.01)
        assertEquals(0.0, result.platformCommission, 0.01)
        assertEquals(0.0, result.investorProfitPool, 0.01)
    }

    @Test
    fun buybackCalculation_deducts10Percent() {
        val result = FinancialCalculator.calculateBuybackPayout(140.0)
        assertEquals(140.0, result.offeredPrice, 0.01)
        assertEquals(14.0, result.deductionAmount, 0.01)
        assertEquals(126.0, result.netPayout, 0.01)
    }

    @Test
    fun monthlyExpenseSharing_reducesWithMoreMembersAndSupportsPromo() {
        val base = FinancialCalculator.calculateMonthlyShare(100000.0, 0.0, 1000L, "EXACT", false, 1.0)
        assertEquals(100.0, base, 0.01)

        val expanded = FinancialCalculator.calculateMonthlyShare(100000.0, 0.0, 1001L, "EXACT", false, 1.0)
        assertEquals(99.90, expanded, 0.01)

        val promo = FinancialCalculator.calculateMonthlyShare(100000.0, 0.0, 1000L, "ROUND_NEAREST", true, 1.0)
        assertEquals(1.0, promo, 0.01)
    }
}
