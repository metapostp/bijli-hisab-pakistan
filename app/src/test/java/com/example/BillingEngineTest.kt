package com.example

import com.example.billing.AllocationMethod
import com.example.billing.BillingEngine
import com.example.billing.CommonUnitsHandling
import com.example.billing.MeterReading
import com.example.billing.PortionInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingEngineTest {

    @Test
    fun testCanonicalExample_ProRata() {
        // Spec Section 1 & Section 100:
        // Actual Bill = Rs. 8,000
        // Main meter: 1,000 -> 1,235 = 235 Units
        // Flat 1: 500 -> 585 = 85 Units
        // Flat 2: 700 -> 735 = 35 Units
        // Flat 3: 900 -> 980 = 80 Units
        // Total sub-meters = 200 Units
        // Common Units = 35 Units

        val mainMeter = MeterReading(previousReading = 1000.0, currentReading = 1235.0)
        assertEquals(235.0, mainMeter.unitsUsed, 0.001)

        val portions = listOf(
            PortionInput(id = "1", name = "Flat 1", previousReading = 500.0, currentReading = 585.0),
            PortionInput(id = "2", name = "Flat 2", previousReading = 700.0, currentReading = 735.0),
            PortionInput(id = "3", name = "Flat 3", previousReading = 900.0, currentReading = 980.0)
        )

        val result = BillingEngine.calculateBill(
            sourceBillRupees = 8000L,
            mainMeter = mainMeter,
            portions = portions,
            allocationMethod = AllocationMethod.PRO_RATA,
            commonUnitsHandling = CommonUnitsHandling.PROPORTIONAL_TO_UNITS
        )

        assertTrue(result.isSuccess)
        assertEquals(235.0, result.mainMeterUnits, 0.001)
        assertEquals(200.0, result.subMetersTotalUnits, 0.001)
        assertEquals(35.0, result.commonUnits, 0.001)

        // Flat 1: 85/200 * 8000 = 3400
        // Flat 2: 35/200 * 8000 = 1400
        // Flat 3: 80/200 * 8000 = 3200
        assertEquals(3400L, result.portionShares[0].totalAmountRupees)
        assertEquals(1400L, result.portionShares[1].totalAmountRupees)
        assertEquals(3200L, result.portionShares[2].totalAmountRupees)

        // Reconciles exactly to Rs. 8,000
        assertEquals(8000L, result.reconciliation.totalAllocatedAmount)
        assertEquals(0L, result.reconciliation.remainingUnallocatedAmount)
        assertTrue(result.reconciliation.isFullyReconciled)
        assertTrue(result.reconciliation.isUnitsReconciled)
    }

    @Test
    fun testRoundingLargestRemainder_ZeroDiscrepancy() {
        // Bill with non-even rupee splits
        val mainMeter = MeterReading(previousReading = 0.0, currentReading = 300.0)
        val portions = listOf(
            PortionInput(id = "1", name = "Portion A", previousReading = 0.0, currentReading = 97.0),
            PortionInput(id = "2", name = "Portion B", previousReading = 0.0, currentReading = 101.0),
            PortionInput(id = "3", name = "Portion C", previousReading = 0.0, currentReading = 102.0)
        )

        val result = BillingEngine.calculateBill(
            sourceBillRupees = 10000L,
            mainMeter = mainMeter,
            portions = portions,
            allocationMethod = AllocationMethod.PRO_RATA
        )

        assertTrue(result.isSuccess)
        val totalAllocated = result.portionShares.sumOf { it.totalAmountRupees }
        assertEquals(10000L, totalAllocated)
        assertEquals(0L, result.reconciliation.remainingUnallocatedAmount)
        assertTrue(result.reconciliation.isFullyReconciled)
    }

    @Test
    fun testSubMeterGreaterThanMain_Rejection() {
        // Submeters total 300 > Main 200 -> Must reject!
        val mainMeter = MeterReading(previousReading = 100.0, currentReading = 300.0) // 200 units
        val portions = listOf(
            PortionInput(id = "1", name = "Portion A", previousReading = 0.0, currentReading = 250.0),
            PortionInput(id = "2", name = "Portion B", previousReading = 0.0, currentReading = 100.0)
        )

        val result = BillingEngine.calculateBill(
            sourceBillRupees = 5000L,
            mainMeter = mainMeter,
            portions = portions
        )

        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("Sub-meter readings Main Meter se zyada hain") == true)
    }

    @Test
    fun testCustomRate_ShowsRemaining() {
        val mainMeter = MeterReading(previousReading = 0.0, currentReading = 200.0)
        val portions = listOf(
            PortionInput(id = "1", name = "Shop 1", previousReading = 0.0, currentReading = 100.0, customRate = 60.0),
            PortionInput(id = "2", name = "Shop 2", previousReading = 0.0, currentReading = 50.0, customRate = 60.0)
        )

        // 100*60 = 6000, 50*60 = 3000 -> Total 9,000 against bill 15,000 -> Remaining Rs. 6,000
        val result = BillingEngine.calculateBill(
            sourceBillRupees = 15000L,
            mainMeter = mainMeter,
            portions = portions,
            allocationMethod = AllocationMethod.CUSTOM_RATE
        )

        assertTrue(result.isSuccess)
        assertEquals(9000L, result.reconciliation.totalAllocatedAmount)
        assertEquals(6000L, result.reconciliation.remainingUnallocatedAmount)
    }
}
