package com.example

import com.example.billing.*
import com.example.data.TariffRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TariffCalculatorTest {

    @Test
    fun calculate_nepraProgressiveSlabs_accuratelyComputesCostAndTaxes() {
        val config = TariffPresets.NEPRA_UNPROTECTED_DOMESTIC
        val result = TariffCalculator.calculate(
            config = config,
            totalUnits = 250.0
        )

        assertEquals(250.0, result.totalUnits, 0.001)
        assertEquals(3, result.slabItems.size)

        // Tier 1: 100 units @ 16.48
        assertEquals(100.0, result.slabItems[0].unitsConsumedInSlab, 0.001)
        assertEquals(1648.0, result.slabItems[0].costRupees, 0.01)

        // Tier 2: 100 units @ 22.95
        assertEquals(100.0, result.slabItems[1].unitsConsumedInSlab, 0.001)
        assertEquals(2295.0, result.slabItems[1].costRupees, 0.01)

        // Tier 3: 50 units @ 27.14
        assertEquals(50.0, result.slabItems[2].unitsConsumedInSlab, 0.001)
        assertEquals(1357.0, result.slabItems[2].costRupees, 0.01)

        val expectedEnergyCost = 1648.0 + 2295.0 + 1357.0 // 5300.0
        assertEquals(expectedEnergyCost, result.energyCostRupees, 0.01)

        assertTrue(result.fixedChargesRupees > 0)
        assertTrue(result.fpaRupees > 0)
        assertTrue(result.gstRupees > 0)
        assertTrue(result.estimatedTotalRupees > result.energyCostRupees.toLong())
        assertTrue(result.effectiveRatePerUnit > 20.0)
    }

    @Test
    fun calculate_peakOffPeakTOU_computesDualRegisters() {
        val config = TariffPresets.DOMESTIC_TOU_PEAK_OFFPEAK
        val result = TariffCalculator.calculate(
            config = config,
            totalUnits = 320.0,
            peakUnits = 80.0,
            offPeakUnits = 240.0
        )

        assertEquals(80.0, result.peakUnits, 0.001)
        assertEquals(240.0, result.offPeakUnits, 0.001)
        assertEquals(80.0 * 44.50, result.peakCostRupees, 0.01)
        assertEquals(240.0 * 35.20, result.offPeakCostRupees, 0.01)

        val expectedEnergy = (80.0 * 44.50) + (240.0 * 35.20)
        assertEquals(expectedEnergy, result.energyCostRupees, 0.01)
        assertTrue(result.estimatedTotalRupees > expectedEnergy.toLong())
    }

    @Test
    fun calculate_flatRate_computesLinearCost() {
        val config = TariffPresets.COMMERCIAL_A2
        val result = TariffCalculator.calculate(
            config = config,
            totalUnits = 100.0
        )

        assertEquals(100.0 * config.flatRate, result.energyCostRupees, 0.01)
    }

    @Test
    fun calculate_zeroUnits_returnsZeroEnergyCost() {
        val config = TariffPresets.NEPRA_UNPROTECTED_DOMESTIC
        val result = TariffCalculator.calculate(
            config = config,
            totalUnits = 0.0
        )

        assertEquals(0.0, result.energyCostRupees, 0.01)
        assertEquals(0.0, result.fpaRupees, 0.01)
        assertTrue(result.fixedChargesRupees > 0) // Fixed meter rent remains
    }

    @Test
    fun tariffSerialization_roundTrip_preservesAllFields() {
        val original = TariffPresets.NEPRA_UNPROTECTED_DOMESTIC
        val json = TariffRepository.serializeTariff(original)
        val parsed = TariffRepository.deserializeTariff(json)

        assertNotNull(parsed)
        assertEquals(original.name, parsed.name)
        assertEquals(original.discoName, parsed.discoName)
        assertEquals(original.pricingMode, parsed.pricingMode)
        assertEquals(original.slabs.size, parsed.slabs.size)
        assertEquals(original.peakRate, parsed.peakRate, 0.001)
        assertEquals(original.offPeakRate, parsed.offPeakRate, 0.001)
        assertEquals(original.fixedCharges, parsed.fixedCharges, 0.001)
        assertEquals(original.gstPercentage, parsed.gstPercentage, 0.001)
    }
}
