package com.example.localization

import java.text.NumberFormat
import java.util.Locale

enum class AppLanguage {
    ENGLISH,
    URDU
}

object BijliStrings {

    fun formatRupees(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        return "Rs. ${formatter.format(amount)}"
    }

    fun formatUnits(units: Double): String {
        return if (units == units.toLong().toDouble()) {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            "${formatter.format(units.toLong())} Units"
        } else {
            "${String.format("%.1f", units)} Units"
        }
    }

    // Dynamic strings provider based on current language
    fun splitActualBill(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Split Actual Bill"
        AppLanguage.URDU -> "اصل بل تقسیم کریں"
    }

    fun splitActualBillSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Divide your actual bill fairly among portions, flats, or shops"
        AppLanguage.URDU -> "اپنا اصل بل پورشنز، فلیٹس یا دکانوں میں تقسیم کریں"
    }

    fun calculateBill(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Calculate Bill"
        AppLanguage.URDU -> "بل کا حساب لگائیں"
    }

    fun calculateBillSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Calculate estimated electricity bill from units"
        AppLanguage.URDU -> "یونٹس کے استعمال سے بل کا تخمینہ لگائیں"
    }

    fun actualBillAmount(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Actual Bill Amount"
        AppLanguage.URDU -> "اصل بل کی رقم"
    }

    fun mainMeter(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Main Meter"
        AppLanguage.URDU -> "مین میٹر (Main Meter)"
    }

    fun subMeters(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Sub-Meters (Portions / Shops)"
        AppLanguage.URDU -> "سب میٹرز (پورشنز / دکانیں)"
    }

    fun previousReading(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Previous Reading"
        AppLanguage.URDU -> "پچھلی ریڈنگ"
    }

    fun currentReading(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Current Reading"
        AppLanguage.URDU -> "موجودہ ریڈنگ"
    }

    fun unitsUsed(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Units Used"
        AppLanguage.URDU -> "استعمال شدہ یونٹس"
    }

    fun commonUnitsDetected(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Common Units"
        AppLanguage.URDU -> "مشترکہ یونٹس (Common Units)"
    }

    fun commonUnitsExplanation(units: Double, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "There is a difference of ${formatUnits(units)} between Main Meter and Sub-Meters (Water pump, common lights, stairs, meter loss, etc.)."
        AppLanguage.URDU -> "مین میٹر اور سب میٹرز کے درمیان ${formatUnits(units)} کا فرق ہے (موٹر، مشترکہ لائٹس، سیڑھیاں وغیرہ)۔"
    }

    fun noCommonUnits(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "All Main Meter Units are accounted for!"
        AppLanguage.URDU -> "مین میٹر کے تمام یونٹس کا مکمل حساب برابر ہے!"
    }

    fun howToHandleCommonUnits(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "How should Common Units be handled?"
        AppLanguage.URDU -> "مشترکہ یونٹس (Common Units) کا کیا کرنا ہے؟"
    }

    fun proRataLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Proportional to units used"
        AppLanguage.URDU -> "سب میں استعمال شدہ یونٹس کے تناسب سے بانٹیں"
    }

    fun equalSplitLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Divide equally among all"
        AppLanguage.URDU -> "سب میں برابر تقسیم کریں"
    }

    fun commonExpenseLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Keep as separate Common Expense"
        AppLanguage.URDU -> "مشترکہ خرچہ (Common Expense) الگ رکھیں"
    }

    fun specificPortionLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Assign to specific portion/owner"
        AppLanguage.URDU -> "کسی خاص پورشن یا مالک کو دیں"
    }

    fun unallocatedLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Keep unallocated for now"
        AppLanguage.URDU -> "اسے ابھی غیر تقسیم شدہ رکھیں"
    }

    fun customRateLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Custom Rate (Rs. / unit)"
        AppLanguage.URDU -> "مخصوص ریٹ فی یونٹ (Custom Rate)"
    }

    fun howWasCalculated(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "How was this calculated?"
        AppLanguage.URDU -> "یہ حساب کیسے ہوا؟ (تفصیل دیکھیں)"
    }

    fun reconciliationTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Bill Reconciliation"
        AppLanguage.URDU -> "بل کی تصدیق اور مفاہمت"
    }

    fun trustLabelActualBill(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Actual Bill Distribution — Based on the bill amount entered by you."
        AppLanguage.URDU -> "اصل بل کی تقسیم — آپ کی درج کردہ بل کی رقم کی بنیاد پر۔"
    }

    fun trustLabelEstimated(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Estimated Bill — Based on estimated unit tariff."
        AppLanguage.URDU -> "تخمینہ بل — درج شدہ ٹیرف کی بنیاد پر۔"
    }

    fun shareBills(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Share Bill"
        AppLanguage.URDU -> "بل شیئر کریں"
    }

    fun shareViaWhatsApp(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Share via WhatsApp"
        AppLanguage.URDU -> "واٹس ایپ پر بھیجیں"
    }

    fun saveBill(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Save Record"
        AppLanguage.URDU -> "ریکارڈ محفوظ کریں"
    }

    fun properties(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Properties"
        AppLanguage.URDU -> "جائیدادیں / پلازہ"
    }

    fun history(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "History"
        AppLanguage.URDU -> "تاریخچہ اور بلز"
    }

    fun propertyManagerMode(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Property Manager Mode"
        AppLanguage.URDU -> "پراپرٹی مینیجر موڈ"
    }

    fun meterConverter(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Meter Format Converter"
        AppLanguage.URDU -> "میٹر فارمیٹ کنورٹر"
    }

    fun meterConverterSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Convert analog red dials, CT ratios (5x-50x), rollovers & pulse sub-meters"
        AppLanguage.URDU -> "اینالاگ سرخ ہندسہ، سی ٹی تناسب، رول اوور اور پلس سب میٹرز کو درست یونٹس میں بدلیں"
    }

    fun subMeterFormat(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Meter Reading Format"
        AppLanguage.URDU -> "میٹر ریڈنگ کا طریقہ کار"
    }

    fun convertedUnits(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Converted Billable Units"
        AppLanguage.URDU -> "تبدیل شدہ اصل یونٹس"
    }

    fun rawUnitsAdvance(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Raw Meter Difference"
        AppLanguage.URDU -> "میٹر کا ظاہری فرق"
    }

    fun copyConvertedSummary(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Copy Converted Units"
        AppLanguage.URDU -> "تبدیل شدہ یونٹس کاپی کریں"
    }

    fun transferToSplitBill(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Use in Split Bill"
        AppLanguage.URDU -> "بل تقسیم میں استعمال کریں"
    }

    fun tariffSettings(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Tariff & Slab Rates"
        AppLanguage.URDU -> "ٹیرف اور سلیب ریٹس"
    }

    fun tariffSettingsSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Define local electricity slabs, Peak/Off-Peak (TOU) rates & DISCO taxes"
        AppLanguage.URDU -> "مقامی بجلی کے سلیب، پیک/آف پیک اوقات کے ریٹس اور ٹیکسز مقرر کریں"
    }

    fun pricingMode(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Pricing Structure"
        AppLanguage.URDU -> "نرخ کا طریقہ کار"
    }

    fun slabBased(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Slab-Based (ٹیرف سلیبز)"
        AppLanguage.URDU -> "سلیب ریٹس (مرحلہ وار)"
    }

    fun peakOffPeak(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Peak / Off-Peak (TOU)"
        AppLanguage.URDU -> "پیک / آف پیک (ٹائم آف یوز)"
    }

    fun flatRate(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Flat Fixed Rate"
        AppLanguage.URDU -> "یکساں ریٹ"
    }

    fun loadPreset(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Load DISCO Preset"
        AppLanguage.URDU -> "سرکاری ٹیرف لوڈ کریں"
    }

    fun saveTariff(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Save Tariff Rates"
        AppLanguage.URDU -> "ٹیرف محفوظ کریں"
    }

    fun resetTariff(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Reset to Default"
        AppLanguage.URDU -> "ڈیفالٹ پر بحال کریں"
    }

    fun addSlab(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Add Slab"
        AppLanguage.URDU -> "نیا سلیب شامل کریں"
    }

    fun testSimulator(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Live Bill Simulator"
        AppLanguage.URDU -> "لائیو بل ٹیسٹ کیلکولیٹر"
    }

    // Sellinix Tech Branding & Contact
    fun developedBy(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Developed by Sellinix Tech"
        AppLanguage.URDU -> "تیار کردہ: سیلینکس ٹیک (Sellinix Tech)"
    }

    fun contactUs(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Contact & Support"
        AppLanguage.URDU -> "رابطہ اور سپورٹ"
    }

    fun sellinixTechDescription(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Software Development • Smart Billing Systems • Tech Solutions"
        AppLanguage.URDU -> "سافٹ ویئر ڈویلپمنٹ • اسمارٹ بلنگ سسٹمز • ٹیک سلوشنز"
    }
}

