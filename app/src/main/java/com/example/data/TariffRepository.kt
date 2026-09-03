package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.billing.TariffConfig
import com.example.billing.TariffPricingMode
import com.example.billing.TariffPresets
import com.example.billing.TariffSlab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class TariffRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bijli_tariff_prefs", Context.MODE_PRIVATE)

    private val _activeTariff = MutableStateFlow(loadTariff())
    val activeTariff: StateFlow<TariffConfig> = _activeTariff.asStateFlow()

    fun saveTariff(config: TariffConfig) {
        val json = serializeTariff(config)
        prefs.edit().putString(KEY_ACTIVE_TARIFF, json).apply()
        _activeTariff.value = config
    }

    fun resetToPreset(presetId: String) {
        val preset = TariffPresets.ALL_PRESETS.find { it.id == presetId }
            ?: TariffPresets.NEPRA_UNPROTECTED_DOMESTIC
        saveTariff(preset)
    }

    private fun loadTariff(): TariffConfig {
        val json = prefs.getString(KEY_ACTIVE_TARIFF, null)
        if (json != null) {
            try {
                return deserializeTariff(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return TariffPresets.NEPRA_UNPROTECTED_DOMESTIC
    }

    companion object {
        const val KEY_ACTIVE_TARIFF = "active_tariff_json"

        fun serializeTariff(config: TariffConfig): String {
            val obj = JSONObject()
            obj.put("id", config.id)
            obj.put("name", config.name)
            obj.put("discoName", config.discoName)
            obj.put("pricingMode", config.pricingMode.name)
            obj.put("isSlabProgressive", config.isSlabProgressive)
            obj.put("peakRate", config.peakRate)
            obj.put("offPeakRate", config.offPeakRate)
            obj.put("peakHoursDescription", config.peakHoursDescription)
            obj.put("flatRate", config.flatRate)
            obj.put("fixedCharges", config.fixedCharges)
            obj.put("gstPercentage", config.gstPercentage)
            obj.put("fpaPerUnit", config.fpaPerUnit)
            obj.put("electricityDutyPercentage", config.electricityDutyPercentage)
            obj.put("tvFee", config.tvFee)
            obj.put("isProtectedConsumer", config.isProtectedConsumer)
            obj.put("notes", config.notes)

            val slabsArray = JSONArray()
            for (s in config.slabs) {
                val sObj = JSONObject()
                sObj.put("id", s.id)
                sObj.put("fromUnits", s.fromUnits)
                sObj.put("toUnits", s.toUnits)
                sObj.put("ratePerUnit", s.ratePerUnit)
                sObj.put("label", s.label)
                slabsArray.put(sObj)
            }
            obj.put("slabs", slabsArray)

            return obj.toString()
        }

        fun deserializeTariff(json: String): TariffConfig {
            val obj = JSONObject(json)
            val modeStr = obj.optString("pricingMode", TariffPricingMode.SLAB_BASED.name)
            val mode = try {
                TariffPricingMode.valueOf(modeStr)
            } catch (e: Exception) {
                TariffPricingMode.SLAB_BASED
            }

            val slabs = mutableListOf<TariffSlab>()
            val slabsArray = obj.optJSONArray("slabs")
            if (slabsArray != null) {
                for (i in 0 until slabsArray.length()) {
                    val sObj = slabsArray.getJSONObject(i)
                    slabs.add(
                        TariffSlab(
                            id = sObj.optString("id", "s_$i"),
                            fromUnits = sObj.optDouble("fromUnits", 1.0),
                            toUnits = sObj.optDouble("toUnits", 100.0),
                            ratePerUnit = sObj.optDouble("ratePerUnit", 16.48),
                            label = sObj.optString("label", "Slab ${i + 1}")
                        )
                    )
                }
            }

            return TariffConfig(
                id = obj.optString("id", "custom_id"),
                name = obj.optString("name", "Custom Tariff"),
                discoName = obj.optString("discoName", "Custom"),
                pricingMode = mode,
                slabs = if (slabs.isNotEmpty()) slabs else TariffPresets.NEPRA_UNPROTECTED_DOMESTIC.slabs,
                isSlabProgressive = obj.optBoolean("isSlabProgressive", true),
                peakRate = obj.optDouble("peakRate", 44.50),
                offPeakRate = obj.optDouble("offPeakRate", 35.20),
                peakHoursDescription = obj.optString("peakHoursDescription", "5:00 PM – 11:00 PM"),
                flatRate = obj.optDouble("flatRate", 45.0),
                fixedCharges = obj.optDouble("fixedCharges", 500.0),
                gstPercentage = obj.optDouble("gstPercentage", 18.0),
                fpaPerUnit = obj.optDouble("fpaPerUnit", 2.50),
                electricityDutyPercentage = obj.optDouble("electricityDutyPercentage", 1.5),
                tvFee = obj.optDouble("tvFee", 35.0),
                isProtectedConsumer = obj.optBoolean("isProtectedConsumer", false),
                notes = obj.optString("notes", "")
            )
        }
    }
}
