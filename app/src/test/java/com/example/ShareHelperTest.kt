package com.example

import com.example.billing.PortionShareResult
import com.example.billing.ReconciliationReport
import com.example.ui.ShareHelper
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ShareHelperTest {

    @Test
    fun testFormatAllPortionsSummaryText() {
        val portions = listOf(
            PortionShareResult(
                portionId = "1",
                portionName = "Ground Floor",
                tenantName = "Ahmed",
                tenantPhone = "03001234567",
                previousReading = 100.0,
                currentReading = 200.0,
                unitsUsed = 100.0,
                consumptionSharePct = 50.0,
                commonUnitsAssigned = 10.0,
                totalBillableUnits = 110.0,
                baseAmountRupees = 4000L,
                commonAmountRupees = 400L,
                totalAmountRupees = 4400L,
                exactDecimalAmount = BigDecimal(4400),
                roundingAdjustment = 0L,
                effectiveRatePerUnit = 40.0,
                calculationNotes = ""
            )
        )

        val reconciliation = ReconciliationReport(
            sourceBillAmount = 8800L,
            totalAllocatedAmount = 8800L,
            remainingUnallocatedAmount = 0L,
            isFullyReconciled = true,
            mainMeterUnits = 220.0,
            subMetersTotalUnits = 200.0,
            commonUnits = 20.0,
            isUnitsReconciled = true
        )

        val summary = ShareHelper.formatAllPortionsSummaryText(
            portions = portions,
            reconciliation = reconciliation,
            monthYear = "September 2026"
        )

        assertTrue(summary.contains("BIJLIHISAB"))
        assertTrue(summary.contains("Ground Floor"))
        assertTrue(summary.contains("Rs. 8,800"))
        assertTrue(summary.contains("Rs. 4,400"))
    }

    @Test
    fun testFormatEstimatedBillSummaryText() {
        val summary = ShareHelper.formatEstimatedBillSummaryText(
            units = 250.0,
            ratePerUnit = 45.0,
            fixedCharges = 500.0,
            gstPercentage = 18.0,
            energyCost = 11250.0,
            taxes = 2115.0,
            estimatedTotal = 13865L
        )

        assertTrue(summary.contains("ESTIMATED BILL CALCULATION"))
        assertTrue(summary.contains("250 Units"))
        assertTrue(summary.contains("Rs. 13,865"))
    }
}
