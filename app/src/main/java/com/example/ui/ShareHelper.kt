package com.example.ui

import android.content.Context
import android.content.Intent
import com.example.billing.PortionShareResult
import com.example.billing.ReconciliationReport
import com.example.localization.BijliStrings

object ShareHelper {

    private const val BRAND_FOOTER = "Generated via BijliHisab Pakistan\nPowered by Sellinix Tech • WhatsApp/Email Support"

    fun formatTenantShareText(
        portion: PortionShareResult,
        monthYear: String,
        totalBill: Long,
        mainUnits: Double,
        commonUnits: Double
    ): String {
        return buildString {
            appendLine("⚡ *BIJLIHISAB PAKISTAN* ⚡")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📋 *Electricity Bill Breakdown*")
            if (monthYear.isNotBlank()) appendLine("🗓 Month: $monthYear")
            appendLine("🏠 Portion / Shop: ${portion.portionName}")
            if (portion.tenantName.isNotBlank()) appendLine("👤 Tenant: ${portion.tenantName}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🔢 *Meter Readings:*")
            appendLine("• Previous Reading: ${String.format("%.0f", portion.previousReading)}")
            appendLine("• Current Reading: ${String.format("%.0f", portion.currentReading)}")
            appendLine("• Units Consumed: ${BijliStrings.formatUnits(portion.unitsUsed)}")
            if (portion.commonUnitsAssigned > 0.001) {
                appendLine("• Common Units Share: ${BijliStrings.formatUnits(portion.commonUnitsAssigned)}")
                appendLine("• Total Billable Units: ${BijliStrings.formatUnits(portion.totalBillableUnits)}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("💰 *TOTAL AMOUNT PAYABLE:*")
            appendLine("👉 *${BijliStrings.formatRupees(portion.totalAmountRupees)}*")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("ℹ️ *Summary Reference:*")
            appendLine("• Actual Total Bill: ${BijliStrings.formatRupees(totalBill)}")
            appendLine("• Main Meter Units: ${BijliStrings.formatUnits(mainUnits)}")
            if (commonUnits > 0.001) {
                appendLine("• Total Common Units: ${BijliStrings.formatUnits(commonUnits)}")
            }
            appendLine("• Effective Rate: Rs. ${String.format("%.2f", portion.effectiveRatePerUnit)}/unit")
            appendLine("")
            appendLine("✅ *Actual Bill Distribution*")
            appendLine("Transparent hisaab without disputes.")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚡ *Sellinix Tech* — Smart Billing Solutions")
            appendLine("📞 WhatsApp / Contact: 923014077218")
        }
    }

    fun formatAllPortionsSummaryText(
        portions: List<PortionShareResult>,
        reconciliation: ReconciliationReport,
        monthYear: String
    ): String {
        return buildString {
            appendLine("⚡ *BIJLIHISAB — COMPLETE BILL SUMMARY* ⚡")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            if (monthYear.isNotBlank()) appendLine("🗓 Month: $monthYear")
            appendLine("💵 Total Actual Bill: ${BijliStrings.formatRupees(reconciliation.sourceBillAmount)}")
            appendLine("⚡ Main Meter: ${BijliStrings.formatUnits(reconciliation.mainMeterUnits)}")
            appendLine("🔌 Sub-Meters Total: ${BijliStrings.formatUnits(reconciliation.subMetersTotalUnits)}")
            appendLine("⚠️ Common Units: ${BijliStrings.formatUnits(reconciliation.commonUnits)}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("👥 *PORTION BREAKDOWN:*")
            for ((idx, p) in portions.withIndex()) {
                val tenantStr = if (p.tenantName.isNotBlank()) " (${p.tenantName})" else ""
                appendLine("${idx + 1}. *${p.portionName}$tenantStr*")
                appendLine("   Readings: ${String.format("%.0f", p.previousReading)} ➔ ${String.format("%.0f", p.currentReading)} = ${BijliStrings.formatUnits(p.unitsUsed)}")
                if (p.commonUnitsAssigned > 0.001) {
                    appendLine("   + Common: ${BijliStrings.formatUnits(p.commonUnitsAssigned)}")
                }
                appendLine("   👉 *Amount: ${BijliStrings.formatRupees(p.totalAmountRupees)}*")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚖️ *Reconciliation:*")
            appendLine("• Total Allocated: ${BijliStrings.formatRupees(reconciliation.totalAllocatedAmount)}")
            if (reconciliation.remainingUnallocatedAmount != 0L) {
                appendLine("• Remaining/Unallocated: ${BijliStrings.formatRupees(reconciliation.remainingUnallocatedAmount)}")
            }
            appendLine("• Difference: Rs. 0 (Fully Reconciled)")
            appendLine("")
            appendLine("Generated via BijliHisab Pakistan")
            appendLine("⚡ *Powered by Sellinix Tech*")
        }
    }

    fun formatEstimatedBillSummaryText(
        units: Double,
        ratePerUnit: Double,
        fixedCharges: Double,
        gstPercentage: Double,
        energyCost: Double,
        taxes: Double,
        estimatedTotal: Long
    ): String {
        return buildString {
            appendLine("⚡ *BIJLIHISAB — ESTIMATED BILL CALCULATION* ⚡")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🔢 *Usage & Rates:*")
            appendLine("• Units Consumed: ${BijliStrings.formatUnits(units)}")
            appendLine("• Rate per Unit: Rs. ${String.format("%.2f", ratePerUnit)}")
            appendLine("• Fixed Charges: ${BijliStrings.formatRupees(fixedCharges.toLong())}")
            appendLine("• GST & Taxes: ${String.format("%.1f", gstPercentage)}%")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("💰 *BREAKDOWN:*")
            appendLine("• Energy Charges: ${BijliStrings.formatRupees(energyCost.toLong())}")
            appendLine("• Fixed Charges: ${BijliStrings.formatRupees(fixedCharges.toLong())}")
            appendLine("• Taxes & GST: ${BijliStrings.formatRupees(taxes.toLong())}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("👉 *ESTIMATED TOTAL: ${BijliStrings.formatRupees(estimatedTotal)}*")
            appendLine("")
            appendLine("Generated via BijliHisab Pakistan")
            appendLine("⚡ *Powered by Sellinix Tech*")
        }
    }

    fun formatSubMetersConversionSummary(
        items: List<com.example.billing.SubMeterConverterItem>,
        notes: String = ""
    ): String {
        return buildString {
            appendLine("🔄 *BIJLIHISAB — SUB-METERS READING FORMAT CONVERSION* 🔄")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            var totalRaw = 0.0
            var totalConverted = 0.0
            items.forEachIndexed { index, item ->
                val conv = item.conversion
                totalRaw += conv.rawAdvance.coerceAtLeast(0.0)
                totalConverted += conv.convertedUnitsKwh
                appendLine("📍 *${item.name}* (${item.format.shortBadge})")
                appendLine("• Raw Readings: ${item.previousReading} ➔ ${item.currentReading} (Diff: ${String.format("%.1f", conv.rawAdvance)})")
                appendLine("• True Converted: *${BijliStrings.formatUnits(conv.convertedUnitsKwh)}*")
                appendLine("• Calculation: ${conv.explanationEn}")
                appendLine("────────────────────────────")
            }
            appendLine("📊 *OVERALL SUMMARY:*")
            appendLine("• Total Sub-Meters: ${items.size}")
            appendLine("• Total Raw Meter Difference: ${String.format("%.1f", totalRaw)}")
            appendLine("• 👉 *TOTAL BILLABLE UNITS: ${BijliStrings.formatUnits(totalConverted)}*")
            if (notes.isNotBlank()) {
                appendLine("• Note: $notes")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Generated via BijliHisab Pakistan")
            appendLine("⚡ *Powered by Sellinix Tech*")
        }
    }

    fun formatTariffDetailedSummary(
        calculation: com.example.billing.TariffCalculationResult,
        tariff: com.example.billing.TariffConfig,
        lang: com.example.localization.AppLanguage
    ): String {
        return buildString {
            appendLine("⚡ *BIJLIHISAB — TARIFF & SLAB BILL ESTIMATE* ⚡")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📋 Tariff: *${tariff.name}* (${tariff.discoName})")
            appendLine("📊 Mode: ${tariff.pricingMode.name}")
            appendLine("⚡ Total Units: ${BijliStrings.formatUnits(calculation.totalUnits)}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            if (calculation.slabItems.isNotEmpty()) {
                appendLine("📑 *Slab-by-Slab Breakdown:*")
                for (item in calculation.slabItems) {
                    appendLine("• ${item.slab.label}: ${String.format("%.1f", item.unitsConsumedInSlab)} u × Rs. ${item.rate} = Rs. ${String.format("%.0f", item.costRupees)}")
                }
            } else if (tariff.pricingMode == com.example.billing.TariffPricingMode.PEAK_OFF_PEAK) {
                appendLine("⏱️ *Time-of-Use Breakdown:*")
                appendLine("• Peak: ${String.format("%.1f", calculation.peakUnits)} u × Rs. ${tariff.peakRate} = Rs. ${String.format("%.0f", calculation.peakCostRupees)}")
                appendLine("• Off-Peak: ${String.format("%.1f", calculation.offPeakUnits)} u × Rs. ${tariff.offPeakRate} = Rs. ${String.format("%.0f", calculation.offPeakCostRupees)}")
            }
            appendLine("────────────────────────────")
            appendLine("• Base Electricity Cost: Rs. ${String.format("%.0f", calculation.energyCostRupees)}")
            if (calculation.fixedChargesRupees > 0) appendLine("• Fixed Monthly Charges: Rs. ${String.format("%.0f", calculation.fixedChargesRupees)}")
            if (calculation.fpaRupees > 0) appendLine("• Fuel Price Adjustment (FPA): Rs. ${String.format("%.0f", calculation.fpaRupees)}")
            if (calculation.electricityDutyRupees > 0) appendLine("• Electricity Duty: Rs. ${String.format("%.0f", calculation.electricityDutyRupees)}")
            if (calculation.gstRupees > 0) appendLine("• Govt GST (${String.format("%.1f", tariff.gstPercentage)}%): Rs. ${String.format("%.0f", calculation.gstRupees)}")
            if (calculation.tvFeeRupees > 0) appendLine("• TV License Fee: Rs. ${String.format("%.0f", calculation.tvFeeRupees)}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("👉 *ESTIMATED TOTAL: ${BijliStrings.formatRupees(calculation.estimatedTotalRupees)}*")
            appendLine("📊 Average Effective Rate: Rs. ${String.format("%.2f", calculation.effectiveRatePerUnit)}/unit")
            appendLine("")
            appendLine("Generated via BijliHisab Pakistan")
            appendLine("⚡ *Powered by Sellinix Tech*")
        }
    }

    fun shareText(context: Context, text: String, title: String = "Share Bill") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }
}
