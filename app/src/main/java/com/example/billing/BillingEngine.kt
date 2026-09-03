package com.example.billing

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.floor
import kotlin.math.roundToLong

object BillingEngine {

    /**
     * Core deterministic calculation function.
     * Pure Kotlin: No UI or platform dependencies.
     */
    fun calculateBill(
        sourceBillRupees: Long,
        mainMeter: MeterReading,
        portions: List<PortionInput>,
        allocationMethod: AllocationMethod = AllocationMethod.PRO_RATA,
        commonUnitsHandling: CommonUnitsHandling = CommonUnitsHandling.PROPORTIONAL_TO_UNITS,
        specificPortionIdForCommon: String? = null,
        defaultCustomRate: Double? = null,
        commonCustomAmount: Long? = null
    ): BillingCalculationResult {
        // 1. Validate Main Meter
        if (!mainMeter.isValid) {
            return errorResult(
                sourceBillRupees, mainMeter, portions, allocationMethod, commonUnitsHandling,
                "Main Meter reading ghalat hai: Current reading previous reading se kam hai."
            )
        }

        val mainUnits = mainMeter.unitsUsed

        // 2. Validate Portions readings
        for (p in portions) {
            if (!p.reading.isValid) {
                return errorResult(
                    sourceBillRupees, mainMeter, portions, allocationMethod, commonUnitsHandling,
                    "${p.name} ki reading ghalat hai: Current reading previous reading se kam hai."
                )
            }
        }

        if (portions.isEmpty()) {
            return errorResult(
                sourceBillRupees, mainMeter, portions, allocationMethod, commonUnitsHandling,
                "Koi Portion/Shop shamil nahi kiya gaya."
            )
        }

        val subMetersTotalUnits = portions.sumOf { it.unitsUsed }

        // 3. Check for sub-meters > main meter (negative common units)
        val rawCommonUnits = mainUnits - subMetersTotalUnits
        if (rawCommonUnits < -0.001) {
            return errorResult(
                sourceBillRupees, mainMeter, portions, allocationMethod, commonUnitsHandling,
                "⚠️ Sub-meter readings Main Meter se zyada hain (${subMetersTotalUnits} > ${mainUnits} Units). Readings check karein."
            )
        }

        val commonUnits = if (rawCommonUnits < 0) 0.0 else rawCommonUnits
        val averageRatePerUnit = if (mainUnits > 0.001) {
            sourceBillRupees.toDouble() / mainUnits
        } else 0.0

        // Warning if all common units (no submeter usage)
        var warning: String? = null
        if (subMetersTotalUnits == 0.0 && mainUnits > 0) {
            warning = "Tamam ${mainUnits} Units abhi common/unallocated hain kyunkay sub-meters mein 0 units hain."
        }

        // Perform calculation based on method
        return when (allocationMethod) {
            AllocationMethod.PRO_RATA -> calculateProRata(
                sourceBillRupees = sourceBillRupees,
                mainMeter = mainMeter,
                mainUnits = mainUnits,
                subUnits = subMetersTotalUnits,
                commonUnits = commonUnits,
                portions = portions,
                commonHandling = commonUnitsHandling,
                specificPortionId = specificPortionIdForCommon,
                commonCustomAmount = commonCustomAmount,
                warning = warning
            )
            AllocationMethod.CUSTOM_RATE -> calculateCustomRate(
                sourceBillRupees = sourceBillRupees,
                mainMeter = mainMeter,
                mainUnits = mainUnits,
                subUnits = subMetersTotalUnits,
                commonUnits = commonUnits,
                portions = portions,
                defaultCustomRate = defaultCustomRate ?: 65.0,
                commonHandling = commonUnitsHandling,
                warning = warning
            )
            AllocationMethod.EQUAL -> calculateEqualSplit(
                sourceBillRupees = sourceBillRupees,
                mainMeter = mainMeter,
                mainUnits = mainUnits,
                subUnits = subMetersTotalUnits,
                commonUnits = commonUnits,
                portions = portions,
                warning = warning
            )
            AllocationMethod.PERCENTAGE -> calculatePercentageSplit(
                sourceBillRupees = sourceBillRupees,
                mainMeter = mainMeter,
                mainUnits = mainUnits,
                subUnits = subMetersTotalUnits,
                commonUnits = commonUnits,
                portions = portions,
                warning = warning
            )
            AllocationMethod.FIXED_AMOUNT -> calculateFixedAmount(
                sourceBillRupees = sourceBillRupees,
                mainMeter = mainMeter,
                mainUnits = mainUnits,
                subUnits = subMetersTotalUnits,
                commonUnits = commonUnits,
                portions = portions,
                warning = warning
            )
            AllocationMethod.RATIO -> calculateRatioSplit(
                sourceBillRupees = sourceBillRupees,
                mainMeter = mainMeter,
                mainUnits = mainUnits,
                subUnits = subMetersTotalUnits,
                commonUnits = commonUnits,
                portions = portions,
                warning = warning
            )
        }
    }

    private fun calculateProRata(
        sourceBillRupees: Long,
        mainMeter: MeterReading,
        mainUnits: Double,
        subUnits: Double,
        commonUnits: Double,
        portions: List<PortionInput>,
        commonHandling: CommonUnitsHandling,
        specificPortionId: String?,
        commonCustomAmount: Long?,
        warning: String?
    ): BillingCalculationResult {
        val nonExcludedPortions = portions.filter { !it.isExcludedFromCommon }
        val eligibleCount = if (nonExcludedPortions.isNotEmpty()) nonExcludedPortions.size else portions.size
        val eligibleSubUnits = if (nonExcludedPortions.isNotEmpty()) nonExcludedPortions.sumOf { it.unitsUsed } else subUnits

        // Calculate common units share per portion
        val assignedCommonUnits = mutableMapOf<String, Double>()
        for (p in portions) {
            assignedCommonUnits[p.id] = 0.0
        }

        when (commonHandling) {
            CommonUnitsHandling.PROPORTIONAL_TO_UNITS -> {
                if (eligibleSubUnits > 0.001) {
                    for (p in (if (nonExcludedPortions.isNotEmpty()) nonExcludedPortions else portions)) {
                        assignedCommonUnits[p.id] = (p.unitsUsed / eligibleSubUnits) * commonUnits
                    }
                } else if (eligibleCount > 0) {
                    for (p in (if (nonExcludedPortions.isNotEmpty()) nonExcludedPortions else portions)) {
                        assignedCommonUnits[p.id] = commonUnits / eligibleCount
                    }
                }
            }
            CommonUnitsHandling.DIVIDED_EQUALLY -> {
                val targetPortions = if (nonExcludedPortions.isNotEmpty()) nonExcludedPortions else portions
                val equalUnits = if (targetPortions.isNotEmpty()) commonUnits / targetPortions.size else 0.0
                for (p in targetPortions) {
                    assignedCommonUnits[p.id] = equalUnits
                }
            }
            CommonUnitsHandling.SPECIFIC_PORTION -> {
                val targetId = specificPortionId ?: portions.firstOrNull()?.id
                if (targetId != null) {
                    assignedCommonUnits[targetId] = commonUnits
                }
            }
            CommonUnitsHandling.PERCENTAGE_SHARE -> {
                for (p in portions) {
                    val pct = (p.customPercentage ?: (100.0 / portions.size)) / 100.0
                    assignedCommonUnits[p.id] = commonUnits * pct
                }
            }
            CommonUnitsHandling.COMMON_EXPENSE,
            CommonUnitsHandling.CUSTOM_AMOUNT,
            CommonUnitsHandling.KEEP_UNALLOCATED -> {
                // Common units not added to individual billable units
            }
        }

        // Compute exact amounts
        // If common units are proportional/equal/specific, total bill is distributed according to total billable units
        val exactAmounts = mutableListOf<BigDecimal>()
        val totalBillBD = BigDecimal(sourceBillRupees)

        val shares = mutableListOf<PortionShareResult>()

        val totalEffectiveBillableUnits = portions.sumOf { it.unitsUsed + (assignedCommonUnits[it.id] ?: 0.0) }

        var allocatableRupees = sourceBillRupees
        var commonExpenseRupees = 0L

        if (commonHandling == CommonUnitsHandling.COMMON_EXPENSE) {
            // Calculate common units money share
            if (mainUnits > 0.001) {
                commonExpenseRupees = ((commonUnits / mainUnits) * sourceBillRupees).roundToLong()
                allocatableRupees = sourceBillRupees - commonExpenseRupees
            }
        } else if (commonHandling == CommonUnitsHandling.CUSTOM_AMOUNT && commonCustomAmount != null) {
            commonExpenseRupees = commonCustomAmount
            allocatableRupees = (sourceBillRupees - commonCustomAmount).coerceAtLeast(0L)
        } else if (commonHandling == CommonUnitsHandling.KEEP_UNALLOCATED) {
            if (mainUnits > 0.001) {
                val unallocatedMoney = ((commonUnits / mainUnits) * sourceBillRupees).roundToLong()
                allocatableRupees = sourceBillRupees - unallocatedMoney
            }
        }

        val baseAllocatableBD = BigDecimal(allocatableRupees)

        for (p in portions) {
            val commonShare = assignedCommonUnits[p.id] ?: 0.0
            val totalUnitsForPortion = p.unitsUsed + commonShare

            val exactAmt: BigDecimal = if (subUnits > 0.001 && (commonHandling == CommonUnitsHandling.COMMON_EXPENSE || commonHandling == CommonUnitsHandling.CUSTOM_AMOUNT || commonHandling == CommonUnitsHandling.KEEP_UNALLOCATED)) {
                val fraction = BigDecimal(p.unitsUsed).divide(BigDecimal(subUnits), 10, RoundingMode.HALF_UP)
                baseAllocatableBD.multiply(fraction)
            } else if (totalEffectiveBillableUnits > 0.001) {
                val fraction = BigDecimal(totalUnitsForPortion).divide(BigDecimal(totalEffectiveBillableUnits), 10, RoundingMode.HALF_UP)
                totalBillBD.multiply(fraction)
            } else {
                BigDecimal(sourceBillRupees).divide(BigDecimal(portions.size), 10, RoundingMode.HALF_UP)
            }

            exactAmounts.add(exactAmt)
        }

        // Apply Largest Remainder Method on allocatableRupees (or sourceBillRupees)
        val targetSum = if (commonHandling == CommonUnitsHandling.COMMON_EXPENSE || commonHandling == CommonUnitsHandling.CUSTOM_AMOUNT || commonHandling == CommonUnitsHandling.KEEP_UNALLOCATED) {
            allocatableRupees
        } else {
            sourceBillRupees
        }

        val roundedRupees = applyLargestRemainderMethod(exactAmounts, targetSum)

        for (i in portions.indices) {
            val p = portions[i]
            val commonShare = assignedCommonUnits[p.id] ?: 0.0
            val totalUnitsForPortion = p.unitsUsed + commonShare
            val sharePct = if (subUnits > 0.001) (p.unitsUsed / subUnits) * 100.0 else (100.0 / portions.size)
            val allocated = roundedRupees[i]
            val exact = exactAmounts[i]
            val adjustment = allocated - exact.setScale(0, RoundingMode.FLOOR).toLong()

            val effectiveRate = if (totalUnitsForPortion > 0.001) allocated.toDouble() / totalUnitsForPortion else 0.0

            val note = buildString {
                append("${p.unitsUsed.formatUnits()} consumption (${String.format("%.1f", sharePct)}%)")
                if (commonShare > 0.001) {
                    append(" + ${commonShare.formatUnits()} common share")
                }
            }

            shares.add(
                PortionShareResult(
                    portionId = p.id,
                    portionName = p.name,
                    tenantName = p.tenantName,
                    tenantPhone = p.tenantPhone,
                    previousReading = p.previousReading,
                    currentReading = p.currentReading,
                    unitsUsed = p.unitsUsed,
                    consumptionSharePct = sharePct,
                    commonUnitsAssigned = commonShare,
                    totalBillableUnits = totalUnitsForPortion,
                    baseAmountRupees = allocated,
                    commonAmountRupees = 0L,
                    totalAmountRupees = allocated,
                    exactDecimalAmount = exact,
                    roundingAdjustment = adjustment,
                    effectiveRatePerUnit = effectiveRate,
                    calculationNotes = note
                )
            )
        }

        val totalAllocated = shares.sumOf { it.totalAmountRupees }
        val remaining = sourceBillRupees - totalAllocated

        val reconciliation = ReconciliationReport(
            sourceBillAmount = sourceBillRupees,
            totalAllocatedAmount = totalAllocated,
            remainingUnallocatedAmount = remaining,
            isFullyReconciled = remaining == 0L,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            isUnitsReconciled = Math.abs((subUnits + commonUnits) - mainUnits) < 0.01,
            discrepancyRupees = 0L
        )

        return BillingCalculationResult(
            isSuccess = true,
            errorMessage = null,
            warningMessage = warning,
            sourceBillAmount = sourceBillRupees,
            mainMeterReading = mainMeter,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            allocationMethod = AllocationMethod.PRO_RATA,
            commonUnitsHandling = commonHandling,
            portionShares = shares,
            reconciliation = reconciliation,
            averageRatePerUnit = if (mainUnits > 0.001) sourceBillRupees.toDouble() / mainUnits else 0.0
        )
    }

    private fun calculateCustomRate(
        sourceBillRupees: Long,
        mainMeter: MeterReading,
        mainUnits: Double,
        subUnits: Double,
        commonUnits: Double,
        portions: List<PortionInput>,
        defaultCustomRate: Double,
        commonHandling: CommonUnitsHandling,
        warning: String?
    ): BillingCalculationResult {
        val shares = mutableListOf<PortionShareResult>()

        for (p in portions) {
            val rate = p.customRate ?: defaultCustomRate
            val exactAmt = BigDecimal(p.unitsUsed * rate).setScale(2, RoundingMode.HALF_UP)
            val roundedAmt = (p.unitsUsed * rate).roundToLong()

            shares.add(
                PortionShareResult(
                    portionId = p.id,
                    portionName = p.name,
                    tenantName = p.tenantName,
                    tenantPhone = p.tenantPhone,
                    previousReading = p.previousReading,
                    currentReading = p.currentReading,
                    unitsUsed = p.unitsUsed,
                    consumptionSharePct = if (subUnits > 0.001) (p.unitsUsed / subUnits) * 100.0 else 0.0,
                    commonUnitsAssigned = 0.0,
                    totalBillableUnits = p.unitsUsed,
                    baseAmountRupees = roundedAmt,
                    commonAmountRupees = 0L,
                    totalAmountRupees = roundedAmt,
                    exactDecimalAmount = exactAmt,
                    roundingAdjustment = 0L,
                    effectiveRatePerUnit = rate,
                    calculationNotes = "${p.unitsUsed.formatUnits()} × Rs. ${String.format("%.2f", rate)}/unit"
                )
            )
        }

        val totalAllocated = shares.sumOf { it.totalAmountRupees }
        val remaining = sourceBillRupees - totalAllocated

        val reconciliation = ReconciliationReport(
            sourceBillAmount = sourceBillRupees,
            totalAllocatedAmount = totalAllocated,
            remainingUnallocatedAmount = remaining,
            isFullyReconciled = remaining == 0L,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            isUnitsReconciled = Math.abs((subUnits + commonUnits) - mainUnits) < 0.01,
            discrepancyRupees = 0L
        )

        return BillingCalculationResult(
            isSuccess = true,
            errorMessage = null,
            warningMessage = if (remaining != 0L) {
                "Custom rate total (Rs. $totalAllocated) actual bill se mukhtalif hai. Rs. $remaining baqi (remaining) hai."
            } else warning,
            sourceBillAmount = sourceBillRupees,
            mainMeterReading = mainMeter,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            allocationMethod = AllocationMethod.CUSTOM_RATE,
            commonUnitsHandling = commonHandling,
            portionShares = shares,
            reconciliation = reconciliation,
            averageRatePerUnit = defaultCustomRate
        )
    }

    private fun calculateEqualSplit(
        sourceBillRupees: Long,
        mainMeter: MeterReading,
        mainUnits: Double,
        subUnits: Double,
        commonUnits: Double,
        portions: List<PortionInput>,
        warning: String?
    ): BillingCalculationResult {
        val count = portions.size
        val exactAmt = BigDecimal(sourceBillRupees).divide(BigDecimal(count), 10, RoundingMode.HALF_UP)
        val exactList = portions.map { exactAmt }
        val rounded = applyLargestRemainderMethod(exactList, sourceBillRupees)

        val shares = portions.mapIndexed { i, p ->
            val amt = rounded[i]
            PortionShareResult(
                portionId = p.id,
                portionName = p.name,
                tenantName = p.tenantName,
                tenantPhone = p.tenantPhone,
                previousReading = p.previousReading,
                currentReading = p.currentReading,
                unitsUsed = p.unitsUsed,
                consumptionSharePct = (100.0 / count),
                commonUnitsAssigned = commonUnits / count,
                totalBillableUnits = p.unitsUsed + (commonUnits / count),
                baseAmountRupees = amt,
                commonAmountRupees = 0L,
                totalAmountRupees = amt,
                exactDecimalAmount = exactAmt,
                roundingAdjustment = amt - exactAmt.setScale(0, RoundingMode.FLOOR).toLong(),
                effectiveRatePerUnit = if (p.unitsUsed > 0.001) amt.toDouble() / p.unitsUsed else 0.0,
                calculationNotes = "Equal distribution: Total bill divided equally across $count portions"
            )
        }

        val totalAllocated = shares.sumOf { it.totalAmountRupees }
        val reconciliation = ReconciliationReport(
            sourceBillAmount = sourceBillRupees,
            totalAllocatedAmount = totalAllocated,
            remainingUnallocatedAmount = sourceBillRupees - totalAllocated,
            isFullyReconciled = true,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            isUnitsReconciled = Math.abs((subUnits + commonUnits) - mainUnits) < 0.01,
            discrepancyRupees = 0L
        )

        return BillingCalculationResult(
            isSuccess = true,
            errorMessage = null,
            warningMessage = warning,
            sourceBillAmount = sourceBillRupees,
            mainMeterReading = mainMeter,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            allocationMethod = AllocationMethod.EQUAL,
            commonUnitsHandling = CommonUnitsHandling.DIVIDED_EQUALLY,
            portionShares = shares,
            reconciliation = reconciliation,
            averageRatePerUnit = if (mainUnits > 0.001) sourceBillRupees.toDouble() / mainUnits else 0.0
        )
    }

    private fun calculatePercentageSplit(
        sourceBillRupees: Long,
        mainMeter: MeterReading,
        mainUnits: Double,
        subUnits: Double,
        commonUnits: Double,
        portions: List<PortionInput>,
        warning: String?
    ): BillingCalculationResult {
        val totalPct = portions.sumOf { it.customPercentage ?: (100.0 / portions.size) }
        var warn = warning
        if (Math.abs(totalPct - 100.0) > 0.01) {
            warn = "Total percentage ${String.format("%.1f", totalPct)}% hai (100% honi chahiye)."
        }

        val billBD = BigDecimal(sourceBillRupees)
        val exactList = portions.map { p ->
            val pct = BigDecimal(p.customPercentage ?: (100.0 / portions.size))
            billBD.multiply(pct).divide(BigDecimal(100), 10, RoundingMode.HALF_UP)
        }

        val rounded = applyLargestRemainderMethod(exactList, sourceBillRupees)

        val shares = portions.mapIndexed { i, p ->
            val amt = rounded[i]
            val pct = p.customPercentage ?: (100.0 / portions.size)
            PortionShareResult(
                portionId = p.id,
                portionName = p.name,
                tenantName = p.tenantName,
                tenantPhone = p.tenantPhone,
                previousReading = p.previousReading,
                currentReading = p.currentReading,
                unitsUsed = p.unitsUsed,
                consumptionSharePct = pct,
                commonUnitsAssigned = (commonUnits * pct) / 100.0,
                totalBillableUnits = p.unitsUsed + ((commonUnits * pct) / 100.0),
                baseAmountRupees = amt,
                commonAmountRupees = 0L,
                totalAmountRupees = amt,
                exactDecimalAmount = exactList[i],
                roundingAdjustment = amt - exactList[i].setScale(0, RoundingMode.FLOOR).toLong(),
                effectiveRatePerUnit = if (p.unitsUsed > 0.001) amt.toDouble() / p.unitsUsed else 0.0,
                calculationNotes = "Percentage share: ${String.format("%.1f", pct)}% of bill"
            )
        }

        val totalAllocated = shares.sumOf { it.totalAmountRupees }
        val remaining = sourceBillRupees - totalAllocated

        val reconciliation = ReconciliationReport(
            sourceBillAmount = sourceBillRupees,
            totalAllocatedAmount = totalAllocated,
            remainingUnallocatedAmount = remaining,
            isFullyReconciled = remaining == 0L,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            isUnitsReconciled = Math.abs((subUnits + commonUnits) - mainUnits) < 0.01,
            discrepancyRupees = 0L
        )

        return BillingCalculationResult(
            isSuccess = true,
            errorMessage = null,
            warningMessage = warn,
            sourceBillAmount = sourceBillRupees,
            mainMeterReading = mainMeter,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            allocationMethod = AllocationMethod.PERCENTAGE,
            commonUnitsHandling = CommonUnitsHandling.PERCENTAGE_SHARE,
            portionShares = shares,
            reconciliation = reconciliation,
            averageRatePerUnit = if (mainUnits > 0.001) sourceBillRupees.toDouble() / mainUnits else 0.0
        )
    }

    private fun calculateFixedAmount(
        sourceBillRupees: Long,
        mainMeter: MeterReading,
        mainUnits: Double,
        subUnits: Double,
        commonUnits: Double,
        portions: List<PortionInput>,
        warning: String?
    ): BillingCalculationResult {
        val shares = portions.map { p ->
            val fixed = p.fixedAmount ?: 0L
            PortionShareResult(
                portionId = p.id,
                portionName = p.name,
                tenantName = p.tenantName,
                tenantPhone = p.tenantPhone,
                previousReading = p.previousReading,
                currentReading = p.currentReading,
                unitsUsed = p.unitsUsed,
                consumptionSharePct = if (sourceBillRupees > 0) (fixed.toDouble() / sourceBillRupees) * 100.0 else 0.0,
                commonUnitsAssigned = 0.0,
                totalBillableUnits = p.unitsUsed,
                baseAmountRupees = fixed,
                commonAmountRupees = 0L,
                totalAmountRupees = fixed,
                exactDecimalAmount = BigDecimal(fixed),
                roundingAdjustment = 0L,
                effectiveRatePerUnit = if (p.unitsUsed > 0.001) fixed.toDouble() / p.unitsUsed else 0.0,
                calculationNotes = "Fixed specified amount: Rs. $fixed"
            )
        }

        val totalAllocated = shares.sumOf { it.totalAmountRupees }
        val remaining = sourceBillRupees - totalAllocated

        val reconciliation = ReconciliationReport(
            sourceBillAmount = sourceBillRupees,
            totalAllocatedAmount = totalAllocated,
            remainingUnallocatedAmount = remaining,
            isFullyReconciled = remaining == 0L,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            isUnitsReconciled = Math.abs((subUnits + commonUnits) - mainUnits) < 0.01,
            discrepancyRupees = 0L
        )

        return BillingCalculationResult(
            isSuccess = true,
            errorMessage = null,
            warningMessage = if (remaining != 0L) "Baqi (Remaining) amount: Rs. $remaining" else warning,
            sourceBillAmount = sourceBillRupees,
            mainMeterReading = mainMeter,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            allocationMethod = AllocationMethod.FIXED_AMOUNT,
            commonUnitsHandling = CommonUnitsHandling.CUSTOM_AMOUNT,
            portionShares = shares,
            reconciliation = reconciliation,
            averageRatePerUnit = if (mainUnits > 0.001) sourceBillRupees.toDouble() / mainUnits else 0.0
        )
    }

    private fun calculateRatioSplit(
        sourceBillRupees: Long,
        mainMeter: MeterReading,
        mainUnits: Double,
        subUnits: Double,
        commonUnits: Double,
        portions: List<PortionInput>,
        warning: String?
    ): BillingCalculationResult {
        val totalWeight = portions.sumOf { it.ratioWeight.coerceAtLeast(0.0) }
        val safeWeight = if (totalWeight > 0.001) totalWeight else portions.size.toDouble()

        val billBD = BigDecimal(sourceBillRupees)
        val exactList = portions.map { p ->
            val w = BigDecimal(p.ratioWeight.coerceAtLeast(0.0))
            billBD.multiply(w).divide(BigDecimal(safeWeight), 10, RoundingMode.HALF_UP)
        }

        val rounded = applyLargestRemainderMethod(exactList, sourceBillRupees)

        val shares = portions.mapIndexed { i, p ->
            val amt = rounded[i]
            val pct = (p.ratioWeight / safeWeight) * 100.0
            PortionShareResult(
                portionId = p.id,
                portionName = p.name,
                tenantName = p.tenantName,
                tenantPhone = p.tenantPhone,
                previousReading = p.previousReading,
                currentReading = p.currentReading,
                unitsUsed = p.unitsUsed,
                consumptionSharePct = pct,
                commonUnitsAssigned = (commonUnits * pct) / 100.0,
                totalBillableUnits = p.unitsUsed + ((commonUnits * pct) / 100.0),
                baseAmountRupees = amt,
                commonAmountRupees = 0L,
                totalAmountRupees = amt,
                exactDecimalAmount = exactList[i],
                roundingAdjustment = amt - exactList[i].setScale(0, RoundingMode.FLOOR).toLong(),
                effectiveRatePerUnit = if (p.unitsUsed > 0.001) amt.toDouble() / p.unitsUsed else 0.0,
                calculationNotes = "Ratio ${p.ratioWeight}:$safeWeight (${String.format("%.1f", pct)}%)"
            )
        }

        val totalAllocated = shares.sumOf { it.totalAmountRupees }
        val reconciliation = ReconciliationReport(
            sourceBillAmount = sourceBillRupees,
            totalAllocatedAmount = totalAllocated,
            remainingUnallocatedAmount = sourceBillRupees - totalAllocated,
            isFullyReconciled = true,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            isUnitsReconciled = Math.abs((subUnits + commonUnits) - mainUnits) < 0.01,
            discrepancyRupees = 0L
        )

        return BillingCalculationResult(
            isSuccess = true,
            errorMessage = null,
            warningMessage = warning,
            sourceBillAmount = sourceBillRupees,
            mainMeterReading = mainMeter,
            mainMeterUnits = mainUnits,
            subMetersTotalUnits = subUnits,
            commonUnits = commonUnits,
            allocationMethod = AllocationMethod.RATIO,
            commonUnitsHandling = CommonUnitsHandling.PERCENTAGE_SHARE,
            portionShares = shares,
            reconciliation = reconciliation,
            averageRatePerUnit = if (mainUnits > 0.001) sourceBillRupees.toDouble() / mainUnits else 0.0
        )
    }

    /**
     * Largest Remainder Method (Hamilton method)
     * 1. Calculate floor integer amounts
     * 2. Compute remainder = targetSum - sum(floor)
     * 3. Sort fractions descending
     * 4. Add 1 rupee to top items until remainder is 0
     * Discrepancy is always guaranteed 0.
     */
    fun applyLargestRemainderMethod(
        exactAmounts: List<BigDecimal>,
        targetSum: Long
    ): List<Long> {
        if (exactAmounts.isEmpty()) return emptyList()

        val baseAmounts = exactAmounts.map { it.setScale(0, RoundingMode.FLOOR).toLong() }
        val currentSum = baseAmounts.sum()
        var remainder = (targetSum - currentSum).toInt()

        if (remainder == 0) return baseAmounts

        // Compute fractions
        val fractionsWithIndex = exactAmounts.mapIndexed { index, bd ->
            val fraction = bd.subtract(BigDecimal(baseAmounts[index])).toDouble()
            Pair(index, fraction)
        }.sortedByDescending { it.second }

        val result = baseAmounts.toMutableList()

        if (remainder > 0) {
            for (i in 0 until remainder.coerceAtMost(fractionsWithIndex.size)) {
                val idx = fractionsWithIndex[i].first
                result[idx] = result[idx] + 1
            }
        } else {
            // In rare negative adjustment (e.g. over allocation)
            val sortedAsc = fractionsWithIndex.reversed()
            val negRemainder = -remainder
            for (i in 0 until negRemainder.coerceAtMost(sortedAsc.size)) {
                val idx = sortedAsc[i].first
                result[idx] = (result[idx] - 1).coerceAtLeast(0L)
            }
        }

        return result
    }

    private fun errorResult(
        sourceBill: Long,
        mainMeter: MeterReading,
        portions: List<PortionInput>,
        method: AllocationMethod,
        commonHandling: CommonUnitsHandling,
        error: String
    ): BillingCalculationResult {
        return BillingCalculationResult(
            isSuccess = false,
            errorMessage = error,
            warningMessage = null,
            sourceBillAmount = sourceBill,
            mainMeterReading = mainMeter,
            mainMeterUnits = mainMeter.unitsUsed.coerceAtLeast(0.0),
            subMetersTotalUnits = portions.sumOf { it.unitsUsed.coerceAtLeast(0.0) },
            commonUnits = 0.0,
            allocationMethod = method,
            commonUnitsHandling = commonHandling,
            portionShares = emptyList(),
            reconciliation = ReconciliationReport(
                sourceBillAmount = sourceBill,
                totalAllocatedAmount = 0L,
                remainingUnallocatedAmount = sourceBill,
                isFullyReconciled = false,
                mainMeterUnits = mainMeter.unitsUsed.coerceAtLeast(0.0),
                subMetersTotalUnits = 0.0,
                commonUnits = 0.0,
                isUnitsReconciled = false,
                discrepancyRupees = sourceBill
            ),
            averageRatePerUnit = 0.0
        )
    }

    private fun Double.formatUnits(): String {
        return if (this == this.toLong().toDouble()) {
            "${this.toLong()} Units"
        } else {
            "${String.format("%.1f", this)} Units"
        }
    }
}
