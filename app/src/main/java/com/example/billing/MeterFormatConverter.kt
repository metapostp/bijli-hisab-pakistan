package com.example.billing

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

enum class MeterReadingFormat(
    val id: String,
    val titleEn: String,
    val titleUr: String,
    val shortBadge: String,
    val descriptionEn: String,
    val descriptionUr: String
) {
    STANDARD_DIGITAL(
        id = "digital",
        titleEn = "Standard Digital LCD",
        titleUr = "ڈیجیٹل ایل سی ڈی (عام)",
        shortBadge = "Digital 1x",
        descriptionEn = "Direct reading in kWh (1 unit = 1 kWh)",
        descriptionUr = "ڈائریکٹ ریڈنگ، ہر 1 پوائنٹ برابر ہے 1 کلو واٹ آور (kWh)"
    ),
    ANALOG_RED_DIAL(
        id = "analog_red_dial",
        titleEn = "Analog Dial (Red Wheel 0.1)",
        titleUr = "اینالاگ ڈائل (سرخ ہندسہ 0.1)",
        shortBadge = "Analog 0.1x",
        descriptionEn = "Last red digit is tenths (0.1 kWh). Corrects 10x overcounting.",
        descriptionUr = "آخری سرخ ہندسہ اعشاریہ 0.1 ہوتا ہے۔ 10 گنا غلطی درست کرتا ہے۔"
    ),
    CT_MULTIPLIER(
        id = "ct_multiplier",
        titleEn = "CT Multiplier (Current Transformer)",
        titleUr = "سی ٹی ملٹی پلائر (CT Ratio)",
        shortBadge = "CT Multiplier",
        descriptionEn = "Commercial/heavy loads (e.g. 5x, 10x, 20x, 40x CT ratio)",
        descriptionUr = "کمرشل یا موٹر لوڈ کے لیے سی ٹی تناسب (مثلاً 10x یا 20x)"
    ),
    DIAL_ROLLOVER(
        id = "dial_rollover",
        titleEn = "Meter Rollover / Reset",
        titleUr = "ڈائل رول اوور (Rollover)",
        shortBadge = "Rollover",
        descriptionEn = "When meter passed 9,999 or 99,999 and cycled back to zero",
        descriptionUr = "جب میٹر زیادہ سے زیادہ حد (9999 یا 99999) سے گزر کر دوبارہ صفر پر آئے"
    ),
    PULSE_IMPULSE(
        id = "pulse_impulse",
        titleEn = "Impulse / Pulse Counter",
        titleUr = "امپلس / پلس میٹر",
        shortBadge = "Pulse/imp",
        descriptionEn = "LED impulses per kWh (e.g. 1000, 1600, or 3200 imp/kWh)",
        descriptionUr = "ہر kWh پر ایل ای ڈی پلسز (مثلاً 1600 یا 3200 imp/kWh)"
    ),
    WATT_HOURS(
        id = "watt_hours",
        titleEn = "Watt-Hours (Wh)",
        titleUr = "واٹ آورز (Wh)",
        shortBadge = "Wh (÷1000)",
        descriptionEn = "Sub-meter measures in Wh (1,000 Wh = 1 kWh)",
        descriptionUr = "سب میٹر کی ریڈنگ واٹ آورز میں ہو تو 1000 سے تقسیم کرتا ہے"
    )
}

data class ConvertedSubMeterReading(
    val rawPrevious: Double,
    val rawCurrent: Double,
    val format: MeterReadingFormat,
    val multiplier: Double = 1.0,
    val rolloverCapacity: Double = 100000.0,
    val redDigitAsInteger: Boolean = true,
    val rawAdvance: Double,
    val convertedUnitsKwh: Double,
    val explanationEn: String,
    val explanationUr: String
)

data class SubMeterConverterItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Sub-Meter",
    val format: MeterReadingFormat = MeterReadingFormat.STANDARD_DIGITAL,
    val previousReading: String = "0",
    val currentReading: String = "0",
    val multiplier: Double = 1.0,
    val rolloverCapacity: Double = 100000.0,
    val redDigitAsInteger: Boolean = true
) {
    val prevDouble: Double get() = previousReading.toDoubleOrNull() ?: 0.0
    val currDouble: Double get() = currentReading.toDoubleOrNull() ?: 0.0

    val conversion: ConvertedSubMeterReading
        get() = MeterFormatConverter.convert(
            previousReading = prevDouble,
            currentReading = currDouble,
            format = format,
            multiplier = multiplier,
            rolloverCapacity = rolloverCapacity,
            redDigitAsInteger = redDigitAsInteger
        )
}

object MeterFormatConverter {

    fun convert(
        previousReading: Double,
        currentReading: Double,
        format: MeterReadingFormat,
        multiplier: Double = 1.0,
        rolloverCapacity: Double = 100000.0,
        redDigitAsInteger: Boolean = true
    ): ConvertedSubMeterReading {
        val rawAdvance = currentReading - previousReading

        val (convertedUnits, expEn, expUr) = when (format) {
            MeterReadingFormat.STANDARD_DIGITAL -> {
                val units = (currentReading - previousReading).coerceAtLeast(0.0)
                Triple(
                    units,
                    "Direct reading: ${String.format("%.1f", currentReading)} - ${String.format("%.1f", previousReading)} = ${String.format("%.1f", units)} kWh",
                    "ڈائریکٹ ریڈنگ: ${String.format("%.1f", currentReading)} - ${String.format("%.1f", previousReading)} = ${String.format("%.1f", units)} یونٹس"
                )
            }
            MeterReadingFormat.ANALOG_RED_DIAL -> {
                val rawDiff = (currentReading - previousReading).coerceAtLeast(0.0)
                val units = if (redDigitAsInteger) {
                    rawDiff / 10.0
                } else {
                    rawDiff
                }
                Triple(
                    units,
                    if (redDigitAsInteger) {
                        "Raw advance ($rawDiff) ÷ 10 = ${String.format("%.2f", units)} kWh (Red tenths digit 0.1x corrected)"
                    } else {
                        "Direct decimal advance: ${String.format("%.2f", units)} kWh"
                    },
                    if (redDigitAsInteger) {
                        "سرخ ہندسہ 0.1 ہونے کی وجہ سے 10 پر تقسیم: ${String.format("%.2f", units)} یونٹس"
                    } else {
                        "اعشاریہ کے ساتھ ریڈنگ: ${String.format("%.2f", units)} یونٹس"
                    }
                )
            }
            MeterReadingFormat.CT_MULTIPLIER -> {
                val effectiveMult = if (multiplier <= 0.0) 1.0 else multiplier
                val rawDiff = (currentReading - previousReading).coerceAtLeast(0.0)
                val units = rawDiff * effectiveMult
                Triple(
                    units,
                    "Raw advance ($rawDiff) × CT Multiplier (${String.format("%.1f", effectiveMult)}x) = ${String.format("%.1f", units)} kWh",
                    "میٹر کا فرق ($rawDiff) ضرب سی ٹی تناسب (${String.format("%.1f", effectiveMult)}x) = ${String.format("%.1f", units)} یونٹس"
                )
            }
            MeterReadingFormat.DIAL_ROLLOVER -> {
                val effectiveCap = if (rolloverCapacity <= 0.0) 100000.0 else rolloverCapacity
                val units = if (currentReading < previousReading) {
                    (effectiveCap - previousReading) + currentReading
                } else {
                    currentReading - previousReading
                }.coerceAtLeast(0.0)
                Triple(
                    units,
                    if (currentReading < previousReading) {
                        "Rollover at ${effectiveCap.toLong()}: (${effectiveCap.toLong()} - $previousReading) + $currentReading = ${String.format("%.1f", units)} kWh"
                    } else {
                        "Standard advance: $currentReading - $previousReading = ${String.format("%.1f", units)} kWh"
                    },
                    if (currentReading < previousReading) {
                        "میٹر کی ریڈنگ ری سیٹ ہوئی: (${effectiveCap.toLong()} - $previousReading) + $currentReading = ${String.format("%.1f", units)} یونٹس"
                    } else {
                        "عام ریڈنگ: ${String.format("%.1f", units)} یونٹس"
                    }
                )
            }
            MeterReadingFormat.PULSE_IMPULSE -> {
                val pulsesPerKwh = if (multiplier <= 0.0) 1600.0 else multiplier
                val pulseDiff = (currentReading - previousReading).coerceAtLeast(0.0)
                val units = pulseDiff / pulsesPerKwh
                Triple(
                    units,
                    "Impulses ($pulseDiff) ÷ $pulsesPerKwh imp/kWh = ${String.format("%.2f", units)} kWh",
                    "پلسز کا فرق ($pulseDiff) تقسیم $pulsesPerKwh imp/kWh = ${String.format("%.2f", units)} یونٹس"
                )
            }
            MeterReadingFormat.WATT_HOURS -> {
                val whDiff = (currentReading - previousReading).coerceAtLeast(0.0)
                val units = whDiff / 1000.0
                Triple(
                    units,
                    "Watt-Hours ($whDiff Wh) ÷ 1000 = ${String.format("%.2f", units)} kWh",
                    "واٹ آورز ($whDiff Wh) تقسیم 1000 = ${String.format("%.2f", units)} یونٹس"
                )
            }
        }

        return ConvertedSubMeterReading(
            rawPrevious = previousReading,
            rawCurrent = currentReading,
            format = format,
            multiplier = multiplier,
            rolloverCapacity = rolloverCapacity,
            redDigitAsInteger = redDigitAsInteger,
            rawAdvance = rawAdvance,
            convertedUnitsKwh = BigDecimal(convertedUnits).setScale(2, RoundingMode.HALF_UP).toDouble(),
            explanationEn = expEn,
            explanationUr = expUr
        )
    }
}
