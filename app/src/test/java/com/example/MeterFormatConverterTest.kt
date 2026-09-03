package com.example

import com.example.billing.MeterFormatConverter
import com.example.billing.MeterReadingFormat
import com.example.billing.SubMeterConverterItem
import com.example.ui.ShareHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeterFormatConverterTest {

    @Test
    fun testStandardDigitalFormat() {
        val result = MeterFormatConverter.convert(
            previousReading = 1000.0,
            currentReading = 1250.0,
            format = MeterReadingFormat.STANDARD_DIGITAL
        )
        assertEquals(250.0, result.convertedUnitsKwh, 0.001)
        assertEquals(250.0, result.rawAdvance, 0.001)
    }

    @Test
    fun testAnalogRedDialIntegerTenths() {
        // Raw input was written without decimal: 14250 -> 14580 (advance = 330)
        // Red wheel represents tenths, so actual advance is 33.0 kWh
        val result = MeterFormatConverter.convert(
            previousReading = 14250.0,
            currentReading = 14580.0,
            format = MeterReadingFormat.ANALOG_RED_DIAL,
            redDigitAsInteger = true
        )
        assertEquals(33.0, result.convertedUnitsKwh, 0.001)
        assertEquals(330.0, result.rawAdvance, 0.001)
        assertTrue(result.explanationEn.contains("÷ 10"))
    }

    @Test
    fun testCtMultiplierFormat() {
        // Commercial 10x CT meter: dial advances from 320 to 355 (diff = 35)
        // True kWh = 35 * 10 = 350.0 kWh
        val result = MeterFormatConverter.convert(
            previousReading = 320.0,
            currentReading = 355.0,
            format = MeterReadingFormat.CT_MULTIPLIER,
            multiplier = 10.0
        )
        assertEquals(350.0, result.convertedUnitsKwh, 0.001)
        assertEquals(35.0, result.rawAdvance, 0.001)
        assertTrue(result.explanationEn.contains("10.0x"))
    }

    @Test
    fun testDialRolloverFormat() {
        // 5-digit meter rolls over: 99850 -> 00120
        // Normal subtraction gives -99730, but with rollover: (100,000 - 99,850) + 120 = 270 kWh
        val result = MeterFormatConverter.convert(
            previousReading = 99850.0,
            currentReading = 120.0,
            format = MeterReadingFormat.DIAL_ROLLOVER,
            rolloverCapacity = 100000.0
        )
        assertEquals(270.0, result.convertedUnitsKwh, 0.001)
        assertTrue(result.explanationEn.contains("Rollover at 100000"))
    }

    @Test
    fun testPulseImpulseFormat() {
        // 3200 impulses recorded on a 1600 imp/kWh meter = 2.0 kWh
        val result = MeterFormatConverter.convert(
            previousReading = 0.0,
            currentReading = 3200.0,
            format = MeterReadingFormat.PULSE_IMPULSE,
            multiplier = 1600.0
        )
        assertEquals(2.0, result.convertedUnitsKwh, 0.001)
    }

    @Test
    fun testWattHoursFormat() {
        // 45,000 Wh = 45.0 kWh
        val result = MeterFormatConverter.convert(
            previousReading = 10000.0,
            currentReading = 55000.0,
            format = MeterReadingFormat.WATT_HOURS
        )
        assertEquals(45.0, result.convertedUnitsKwh, 0.001)
    }

    @Test
    fun testFormatSubMetersConversionSummaryShare() {
        val items = listOf(
            SubMeterConverterItem(
                name = "Shop 1 (CT 10x)",
                format = MeterReadingFormat.CT_MULTIPLIER,
                previousReading = "100",
                currentReading = "120",
                multiplier = 10.0
            ),
            SubMeterConverterItem(
                name = "Flat 1 (Analog)",
                format = MeterReadingFormat.ANALOG_RED_DIAL,
                previousReading = "1000",
                currentReading = "1250",
                redDigitAsInteger = true
            )
        )

        val summary = ShareHelper.formatSubMetersConversionSummary(items)
        assertTrue(summary.contains("SUB-METERS READING FORMAT CONVERSION"))
        assertTrue(summary.contains("Shop 1"))
        assertTrue(summary.contains("Flat 1"))
        assertTrue(summary.contains("TOTAL BILLABLE UNITS"))
    }
}
