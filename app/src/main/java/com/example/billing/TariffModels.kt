package com.example.billing

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

enum class TariffPricingMode {
    SLAB_BASED,
    PEAK_OFF_PEAK,
    FLAT_RATE
}

data class TariffSlab(
    val id: String = UUID.randomUUID().toString(),
    val fromUnits: Double,
    val toUnits: Double, // Use Double.MAX_VALUE for "Above X"
    val ratePerUnit: Double,
    val label: String
) {
    val isUncapped: Boolean
        get() = toUnits >= 99999.0 || toUnits == Double.MAX_VALUE

    val slabDisplayRange: String
        get() = if (isUncapped) {
            "Above ${fromUnits.toInt()}"
        } else {
            "${fromUnits.toInt()} – ${toUnits.toInt()}"
        }
}

data class TariffConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val discoName: String,
    val pricingMode: TariffPricingMode = TariffPricingMode.SLAB_BASED,
    val slabs: List<TariffSlab> = emptyList(),
    val isSlabProgressive: Boolean = true, // Progressive tier calculation (Standard NEPRA)
    val peakRate: Double = 44.50,
    val offPeakRate: Double = 35.20,
    val peakHoursDescription: String = "5:00 PM – 11:00 PM",
    val flatRate: Double = 45.00,
    val fixedCharges: Double = 500.0,
    val gstPercentage: Double = 18.0,
    val fpaPerUnit: Double = 2.50, // Fuel Price Adjustment / unit
    val electricityDutyPercentage: Double = 1.5,
    val tvFee: Double = 35.0,
    val isProtectedConsumer: Boolean = false,
    val notes: String = ""
)

data class SlabCalculationItem(
    val slab: TariffSlab,
    val unitsConsumedInSlab: Double,
    val rate: Double,
    val costRupees: Double
)

data class TariffCalculationResult(
    val tariffName: String,
    val pricingMode: TariffPricingMode,
    val totalUnits: Double,
    val peakUnits: Double = 0.0,
    val offPeakUnits: Double = 0.0,
    val slabItems: List<SlabCalculationItem> = emptyList(),
    val energyCostRupees: Double,
    val peakCostRupees: Double = 0.0,
    val offPeakCostRupees: Double = 0.0,
    val fixedChargesRupees: Double,
    val fpaRupees: Double,
    val electricityDutyRupees: Double,
    val gstRupees: Double,
    val tvFeeRupees: Double,
    val totalTaxesAndSurcharges: Double,
    val estimatedTotalRupees: Long,
    val effectiveRatePerUnit: Double,
    val calculationSummaryEn: String,
    val calculationSummaryUr: String
)

object TariffPresets {

    // 1. NEPRA Standard Domestic Unprotected Slabs (2024-2026 Realistic Base Schedule)
    val NEPRA_UNPROTECTED_DOMESTIC = TariffConfig(
        id = "nepra_unprotected",
        name = "NEPRA Domestic (Unprotected Slabs)",
        discoName = "National / DISCOs Standard",
        pricingMode = TariffPricingMode.SLAB_BASED,
        isSlabProgressive = true,
        slabs = listOf(
            TariffSlab(id = "s1", fromUnits = 1.0, toUnits = 100.0, ratePerUnit = 16.48, label = "1 – 100 Units"),
            TariffSlab(id = "s2", fromUnits = 101.0, toUnits = 200.0, ratePerUnit = 22.95, label = "101 – 200 Units"),
            TariffSlab(id = "s3", fromUnits = 201.0, toUnits = 300.0, ratePerUnit = 27.14, label = "201 – 300 Units"),
            TariffSlab(id = "s4", fromUnits = 301.0, toUnits = 400.0, ratePerUnit = 32.03, label = "301 – 400 Units"),
            TariffSlab(id = "s5", fromUnits = 401.0, toUnits = 500.0, ratePerUnit = 35.24, label = "401 – 500 Units"),
            TariffSlab(id = "s6", fromUnits = 501.0, toUnits = 600.0, ratePerUnit = 36.66, label = "501 – 600 Units"),
            TariffSlab(id = "s7", fromUnits = 601.0, toUnits = 700.0, ratePerUnit = 37.80, label = "601 – 700 Units"),
            TariffSlab(id = "s8", fromUnits = 701.0, toUnits = 99999.0, ratePerUnit = 42.72, label = "Above 700 Units")
        ),
        peakRate = 44.50,
        offPeakRate = 35.20,
        flatRate = 45.0,
        fixedCharges = 500.0,
        gstPercentage = 18.0,
        fpaPerUnit = 2.50,
        electricityDutyPercentage = 1.5,
        tvFee = 35.0,
        isProtectedConsumer = false,
        notes = "Standard progressive domestic tariff applicable to LESCO, IESCO, FESCO, MEPCO, GEPCO, PESCO, HESCO."
    )

    // 2. NEPRA Protected Domestic / LifeLine Category
    val NEPRA_PROTECTED_DOMESTIC = TariffConfig(
        id = "nepra_protected",
        name = "NEPRA Domestic (Protected / LifeLine)",
        discoName = "Protected Domestic",
        pricingMode = TariffPricingMode.SLAB_BASED,
        isSlabProgressive = true,
        slabs = listOf(
            TariffSlab(id = "p1", fromUnits = 1.0, toUnits = 50.0, ratePerUnit = 3.95, label = "1 – 50 Units (LifeLine)"),
            TariffSlab(id = "p2", fromUnits = 51.0, toUnits = 100.0, ratePerUnit = 7.74, label = "51 – 100 Units"),
            TariffSlab(id = "p3", fromUnits = 101.0, toUnits = 200.0, ratePerUnit = 14.16, label = "101 – 200 Units")
        ),
        peakRate = 35.0,
        offPeakRate = 25.0,
        flatRate = 12.0,
        fixedCharges = 0.0,
        gstPercentage = 0.0,
        fpaPerUnit = 1.25,
        electricityDutyPercentage = 1.5,
        tvFee = 35.0,
        isProtectedConsumer = true,
        notes = "Subsidized protected category for consumers who consistently use under 200 units for 6 months."
    )

    // 3. Time-of-Use (TOU) Peak / Off-Peak (3-Phase Domestic Meters)
    val DOMESTIC_TOU_PEAK_OFFPEAK = TariffConfig(
        id = "domestic_tou",
        name = "Domestic Time-of-Use (Peak / Off-Peak)",
        discoName = "3-Phase Domestic TOU",
        pricingMode = TariffPricingMode.PEAK_OFF_PEAK,
        slabs = emptyList(),
        peakRate = 44.50,
        offPeakRate = 35.20,
        peakHoursDescription = "5:00 PM – 11:00 PM (Summer) / 6:00 PM – 10:00 PM (Winter)",
        flatRate = 38.0,
        fixedCharges = 500.0,
        gstPercentage = 18.0,
        fpaPerUnit = 2.50,
        electricityDutyPercentage = 1.5,
        tvFee = 35.0,
        isProtectedConsumer = false,
        notes = "For 3-phase domestic meters with separate peak and off-peak register counts."
    )

    // 4. Commercial (A-2) Tariff
    val COMMERCIAL_A2 = TariffConfig(
        id = "commercial_a2",
        name = "Commercial A-2 (Shops & Plazas)",
        discoName = "Commercial",
        pricingMode = TariffPricingMode.FLAT_RATE,
        slabs = listOf(
            TariffSlab(id = "c1", fromUnits = 1.0, toUnits = 99999.0, ratePerUnit = 54.50, label = "Commercial Flat Rate")
        ),
        peakRate = 58.00,
        offPeakRate = 46.50,
        peakHoursDescription = "5:00 PM – 11:00 PM",
        flatRate = 54.50,
        fixedCharges = 1250.0,
        gstPercentage = 18.0,
        fpaPerUnit = 3.50,
        electricityDutyPercentage = 2.0,
        tvFee = 35.0,
        isProtectedConsumer = false,
        notes = "For commercial shops, offices, and plazas."
    )

    // 5. K-Electric Karachi Residential
    val KELECTRIC_RESIDENTIAL = TariffConfig(
        id = "kelectric_res",
        name = "K-Electric Karachi Residential Slabs",
        discoName = "K-Electric",
        pricingMode = TariffPricingMode.SLAB_BASED,
        isSlabProgressive = true,
        slabs = listOf(
            TariffSlab(id = "k1", fromUnits = 1.0, toUnits = 100.0, ratePerUnit = 17.20, label = "1 – 100 Units"),
            TariffSlab(id = "k2", fromUnits = 101.0, toUnits = 200.0, ratePerUnit = 23.80, label = "101 – 200 Units"),
            TariffSlab(id = "k3", fromUnits = 201.0, toUnits = 300.0, ratePerUnit = 28.50, label = "201 – 300 Units"),
            TariffSlab(id = "k4", fromUnits = 301.0, toUnits = 400.0, ratePerUnit = 33.10, label = "301 – 400 Units"),
            TariffSlab(id = "k5", fromUnits = 401.0, toUnits = 500.0, ratePerUnit = 36.40, label = "401 – 500 Units"),
            TariffSlab(id = "k6", fromUnits = 501.0, toUnits = 99999.0, ratePerUnit = 43.50, label = "Above 500 Units")
        ),
        peakRate = 46.00,
        offPeakRate = 36.80,
        flatRate = 46.0,
        fixedCharges = 600.0,
        gstPercentage = 18.0,
        fpaPerUnit = 3.20,
        electricityDutyPercentage = 1.5,
        tvFee = 35.0,
        isProtectedConsumer = false,
        notes = "Tariff structure for K-Electric Karachi domestic consumers."
    )

    val ALL_PRESETS = listOf(
        NEPRA_UNPROTECTED_DOMESTIC,
        NEPRA_PROTECTED_DOMESTIC,
        DOMESTIC_TOU_PEAK_OFFPEAK,
        COMMERCIAL_A2,
        KELECTRIC_RESIDENTIAL
    )
}

object TariffCalculator {

    fun calculate(
        config: TariffConfig,
        totalUnits: Double,
        peakUnits: Double = 0.0,
        offPeakUnits: Double = 0.0
    ): TariffCalculationResult {
        val safeUnits = totalUnits.coerceAtLeast(0.0)

        when (config.pricingMode) {
            TariffPricingMode.SLAB_BASED -> {
                val slabItems = mutableListOf<SlabCalculationItem>()
                var remainingUnits = safeUnits
                var energyCost = 0.0

                if (config.isSlabProgressive) {
                    // Progressive tiers (NEPRA Standard):
                    // Each slab gets up to (toUnits - fromUnits + 1) units
                    for (slab in config.slabs.sortedBy { it.fromUnits }) {
                        if (remainingUnits <= 0.0) break

                        val slabCapacity = if (slab.isUncapped) {
                            remainingUnits
                        } else {
                            (slab.toUnits - slab.fromUnits + 1.0).coerceAtLeast(0.0)
                        }

                        val unitsInThisSlab = remainingUnits.coerceAtMost(slabCapacity)
                        val cost = unitsInThisSlab * slab.ratePerUnit
                        slabItems.add(
                            SlabCalculationItem(
                                slab = slab,
                                unitsConsumedInSlab = unitsInThisSlab,
                                rate = slab.ratePerUnit,
                                costRupees = cost
                            )
                        )
                        energyCost += cost
                        remainingUnits -= unitsInThisSlab
                    }
                } else {
                    // Single bracket (all units charged at the matching slab rate)
                    val matchingSlab = config.slabs.firstOrNull {
                        safeUnits >= it.fromUnits && (safeUnits <= it.toUnits || it.isUncapped)
                    } ?: config.slabs.lastOrNull()

                    val rate = matchingSlab?.ratePerUnit ?: config.flatRate
                    energyCost = safeUnits * rate
                    if (matchingSlab != null) {
                        slabItems.add(
                            SlabCalculationItem(
                                slab = matchingSlab,
                                unitsConsumedInSlab = safeUnits,
                                rate = rate,
                                costRupees = energyCost
                            )
                        )
                    }
                }

                return buildResult(
                    config = config,
                    totalUnits = safeUnits,
                    peakUnits = 0.0,
                    offPeakUnits = 0.0,
                    slabItems = slabItems,
                    energyCost = energyCost,
                    peakCost = 0.0,
                    offPeakCost = 0.0
                )
            }

            TariffPricingMode.PEAK_OFF_PEAK -> {
                val safePeak = peakUnits.coerceAtLeast(0.0)
                val safeOffPeak = offPeakUnits.coerceAtLeast(0.0)
                val actualTotal = if (safePeak + safeOffPeak > 0.0) safePeak + safeOffPeak else safeUnits
                val effectivePeak = if (safePeak + safeOffPeak > 0.0) safePeak else (actualTotal * 0.25)
                val effectiveOffPeak = if (safePeak + safeOffPeak > 0.0) safeOffPeak else (actualTotal * 0.75)

                val peakCost = effectivePeak * config.peakRate
                val offPeakCost = effectiveOffPeak * config.offPeakRate
                val energyCost = peakCost + offPeakCost

                return buildResult(
                    config = config,
                    totalUnits = actualTotal,
                    peakUnits = effectivePeak,
                    offPeakUnits = effectiveOffPeak,
                    slabItems = emptyList(),
                    energyCost = energyCost,
                    peakCost = peakCost,
                    offPeakCost = offPeakCost
                )
            }

            TariffPricingMode.FLAT_RATE -> {
                val energyCost = safeUnits * config.flatRate
                return buildResult(
                    config = config,
                    totalUnits = safeUnits,
                    peakUnits = 0.0,
                    offPeakUnits = 0.0,
                    slabItems = emptyList(),
                    energyCost = energyCost,
                    peakCost = 0.0,
                    offPeakCost = 0.0
                )
            }
        }
    }

    private fun buildResult(
        config: TariffConfig,
        totalUnits: Double,
        peakUnits: Double,
        offPeakUnits: Double,
        slabItems: List<SlabCalculationItem>,
        energyCost: Double,
        peakCost: Double,
        offPeakCost: Double
    ): TariffCalculationResult {
        val fixedCharges = config.fixedCharges
        val fpa = totalUnits * config.fpaPerUnit
        val electricityDuty = (energyCost + fixedCharges) * (config.electricityDutyPercentage / 100.0)
        val taxableSubtotal = energyCost + fixedCharges + fpa + electricityDuty
        val gst = taxableSubtotal * (config.gstPercentage / 100.0)
        val tvFee = if (totalUnits > 0.0) config.tvFee else 0.0

        val totalTaxesAndSurcharges = fixedCharges + fpa + electricityDuty + gst + tvFee
        val grandTotalExact = energyCost + totalTaxesAndSurcharges
        val estimatedTotal = grandTotalExact.toLong()

        val effectiveRate = if (totalUnits > 0.001) {
            grandTotalExact / totalUnits
        } else 0.0

        val summaryEn = buildString {
            appendLine("Tariff: ${config.name}")
            appendLine("Pricing Mode: ${config.pricingMode.name}")
            appendLine("Units: ${String.format("%.1f", totalUnits)} kWh")
            if (slabItems.isNotEmpty()) {
                appendLine("Slab Breakdown:")
                slabItems.forEach {
                    appendLine("  • ${it.slab.label}: ${String.format("%.1f", it.unitsConsumedInSlab)} units × Rs. ${it.rate} = Rs. ${String.format("%.0f", it.costRupees)}")
                }
            } else if (config.pricingMode == TariffPricingMode.PEAK_OFF_PEAK) {
                appendLine("  • Peak: ${String.format("%.1f", peakUnits)} units × Rs. ${config.peakRate} = Rs. ${String.format("%.0f", peakCost)}")
                appendLine("  • Off-Peak: ${String.format("%.1f", offPeakUnits)} units × Rs. ${config.offPeakRate} = Rs. ${String.format("%.0f", offPeakCost)}")
            }
            appendLine("Energy Cost: Rs. ${String.format("%.0f", energyCost)}")
            appendLine("Fixed + FPA + Taxes: Rs. ${String.format("%.0f", totalTaxesAndSurcharges)}")
            appendLine("Estimated Bill: Rs. $estimatedTotal (Effective: Rs. ${String.format("%.2f", effectiveRate)}/unit)")
        }

        val summaryUr = buildString {
            appendLine("ٹیرف شیڈول: ${config.name}")
            appendLine("کل استعمال شدہ یونٹس: ${String.format("%.1f", totalUnits)}")
            appendLine("بجلی قیمت: Rs. ${String.format("%.0f", energyCost)}")
            appendLine("فکسڈ چارجز و ٹیکسز: Rs. ${String.format("%.0f", totalTaxesAndSurcharges)}")
            appendLine("کل متوقع بل: Rs. $estimatedTotal (اوسط ریٹ: Rs. ${String.format("%.2f", effectiveRate)} فی یونٹ)")
        }

        return TariffCalculationResult(
            tariffName = config.name,
            pricingMode = config.pricingMode,
            totalUnits = totalUnits,
            peakUnits = peakUnits,
            offPeakUnits = offPeakUnits,
            slabItems = slabItems,
            energyCostRupees = energyCost,
            peakCostRupees = peakCost,
            offPeakCostRupees = offPeakCost,
            fixedChargesRupees = fixedCharges,
            fpaRupees = fpa,
            electricityDutyRupees = electricityDuty,
            gstRupees = gst,
            tvFeeRupees = tvFee,
            totalTaxesAndSurcharges = totalTaxesAndSurcharges,
            estimatedTotalRupees = estimatedTotal,
            effectiveRatePerUnit = effectiveRate,
            calculationSummaryEn = summaryEn,
            calculationSummaryUr = summaryUr
        )
    }
}
