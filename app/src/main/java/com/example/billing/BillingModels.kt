package com.example.billing

import java.math.BigDecimal
import java.math.RoundingMode

enum class AllocationMethod {
    PRO_RATA,           // Consumption share: User Units / Total Units * Bill Amount
    CUSTOM_RATE,        // User defined Rs per unit (e.g. Rs 65/unit)
    EQUAL,              // Equal split of bill among portions
    PERCENTAGE,         // User assigned % per portion
    FIXED_AMOUNT,       // User assigned exact Rs per portion
    RATIO               // Custom ratio e.g. 1:2:1
}

enum class CommonUnitsHandling {
    PROPORTIONAL_TO_UNITS, // Sab mein units ke hisaab se baant dein
    DIVIDED_EQUALLY,       // Sab mein barabar baant dein
    COMMON_EXPENSE,        // Common Expense alag line item rakhein
    SPECIFIC_PORTION,      // Kisi specific portion/shop ko dein
    PERCENTAGE_SHARE,      // Percentage ke hisaab se
    CUSTOM_AMOUNT,         // Amount khud set karein
    KEEP_UNALLOCATED       // Isay abhi unallocated rakhein
}

data class MeterReading(
    val previousReading: Double,
    val currentReading: Double,
    val rolloverMax: Double? = null // if meter rolled over
) {
    val unitsUsed: Double
        get() {
            return if (currentReading >= previousReading) {
                currentReading - previousReading
            } else if (rolloverMax != null && rolloverMax > previousReading) {
                (rolloverMax - previousReading) + currentReading
            } else {
                -1.0 // Invalid reading
            }
        }

    val isValid: Boolean
        get() = unitsUsed >= 0.0
}

data class PortionInput(
    val id: String,
    val name: String,
    val tenantName: String = "",
    val tenantPhone: String = "",
    val previousReading: Double = 0.0,
    val currentReading: Double = 0.0,
    val customRate: Double? = null,
    val customPercentage: Double? = null,
    val fixedAmount: Long? = null,
    val ratioWeight: Double = 1.0,
    val isExcludedFromCommon: Boolean = false,
    val format: MeterReadingFormat = MeterReadingFormat.STANDARD_DIGITAL,
    val formatMultiplier: Double = 1.0,
    val rolloverCapacity: Double = 100000.0,
    val redDigitDecimal: Boolean = true
) {
    val reading: MeterReading
        get() = MeterReading(previousReading, currentReading)

    val unitsUsed: Double
        get() = MeterFormatConverter.convert(
            previousReading = previousReading,
            currentReading = currentReading,
            format = format,
            multiplier = formatMultiplier,
            rolloverCapacity = rolloverCapacity,
            redDigitAsInteger = redDigitDecimal
        ).convertedUnitsKwh
}

data class PortionShareResult(
    val portionId: String,
    val portionName: String,
    val tenantName: String,
    val tenantPhone: String,
    val previousReading: Double,
    val currentReading: Double,
    val unitsUsed: Double,
    val consumptionSharePct: Double,
    val commonUnitsAssigned: Double,
    val totalBillableUnits: Double,
    val baseAmountRupees: Long,
    val commonAmountRupees: Long,
    val totalAmountRupees: Long,
    val exactDecimalAmount: BigDecimal,
    val roundingAdjustment: Long, // +1 or 0 or -1 from Largest Remainder Method
    val effectiveRatePerUnit: Double,
    val calculationNotes: String
)

data class ReconciliationReport(
    val sourceBillAmount: Long,
    val totalAllocatedAmount: Long,
    val remainingUnallocatedAmount: Long,
    val isFullyReconciled: Boolean,
    val mainMeterUnits: Double,
    val subMetersTotalUnits: Double,
    val commonUnits: Double,
    val isUnitsReconciled: Boolean,
    val discrepancyRupees: Long = 0L
)

data class BillingCalculationResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val warningMessage: String? = null,
    val sourceBillAmount: Long,
    val mainMeterReading: MeterReading,
    val mainMeterUnits: Double,
    val subMetersTotalUnits: Double,
    val commonUnits: Double,
    val allocationMethod: AllocationMethod,
    val commonUnitsHandling: CommonUnitsHandling,
    val portionShares: List<PortionShareResult>,
    val reconciliation: ReconciliationReport,
    val averageRatePerUnit: Double
)
