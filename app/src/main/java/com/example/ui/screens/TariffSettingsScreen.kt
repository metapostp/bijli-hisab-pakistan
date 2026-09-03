package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.*
import com.example.data.TariffRepository
import com.example.localization.AppLanguage
import com.example.localization.BijliStrings
import com.example.ui.ShareHelper
import com.example.ui.theme.BijliGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariffSettingsScreen(
    tariffRepository: TariffRepository,
    currentLang: AppLanguage,
    onBack: () -> Unit
) {
    val activeTariff by tariffRepository.activeTariff.collectAsState()
    val context = LocalContext.current

    // Local mutable editing state initialized from activeTariff
    var tariffName by remember(activeTariff) { mutableStateOf(activeTariff.name) }
    var discoName by remember(activeTariff) { mutableStateOf(activeTariff.discoName) }
    var pricingMode by remember(activeTariff) { mutableStateOf(activeTariff.pricingMode) }
    var isProgressive by remember(activeTariff) { mutableStateOf(activeTariff.isSlabProgressive) }

    // Slabs editing state
    var slabsList by remember(activeTariff) { mutableStateOf(activeTariff.slabs) }

    // Peak / Off-peak state
    var peakRateInput by remember(activeTariff) { mutableStateOf(activeTariff.peakRate.toString()) }
    var offPeakRateInput by remember(activeTariff) { mutableStateOf(activeTariff.offPeakRate.toString()) }
    var peakHoursDesc by remember(activeTariff) { mutableStateOf(activeTariff.peakHoursDescription) }

    // Flat rate state
    var flatRateInput by remember(activeTariff) { mutableStateOf(activeTariff.flatRate.toString()) }

    // Taxes & surcharges state
    var fixedChargesInput by remember(activeTariff) { mutableStateOf(activeTariff.fixedCharges.toString()) }
    var gstPercentageInput by remember(activeTariff) { mutableStateOf(activeTariff.gstPercentage.toString()) }
    var fpaPerUnitInput by remember(activeTariff) { mutableStateOf(activeTariff.fpaPerUnit.toString()) }
    var electricityDutyInput by remember(activeTariff) { mutableStateOf(activeTariff.electricityDutyPercentage.toString()) }
    var tvFeeInput by remember(activeTariff) { mutableStateOf(activeTariff.tvFee.toString()) }

    // Live Simulator inputs
    var testUnitsInput by remember { mutableStateOf("320") }
    var testPeakUnitsInput by remember { mutableStateOf("80") }
    var testOffPeakUnitsInput by remember { mutableStateOf("240") }

    var showResetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Build the current preview tariff config
    val currentConfig = remember(
        tariffName, discoName, pricingMode, isProgressive, slabsList,
        peakRateInput, offPeakRateInput, peakHoursDesc, flatRateInput,
        fixedChargesInput, gstPercentageInput, fpaPerUnitInput, electricityDutyInput, tvFeeInput
    ) {
        TariffConfig(
            id = activeTariff.id,
            name = tariffName,
            discoName = discoName,
            pricingMode = pricingMode,
            slabs = slabsList,
            isSlabProgressive = isProgressive,
            peakRate = peakRateInput.toDoubleOrNull() ?: 44.50,
            offPeakRate = offPeakRateInput.toDoubleOrNull() ?: 35.20,
            peakHoursDescription = peakHoursDesc,
            flatRate = flatRateInput.toDoubleOrNull() ?: 45.0,
            fixedCharges = fixedChargesInput.toDoubleOrNull() ?: 500.0,
            gstPercentage = gstPercentageInput.toDoubleOrNull() ?: 18.0,
            fpaPerUnit = fpaPerUnitInput.toDoubleOrNull() ?: 2.50,
            electricityDutyPercentage = electricityDutyInput.toDoubleOrNull() ?: 1.5,
            tvFee = tvFeeInput.toDoubleOrNull() ?: 35.0
        )
    }

    // Live test calculation
    val testUnits = testUnitsInput.toDoubleOrNull() ?: 0.0
    val testPeak = testPeakUnitsInput.toDoubleOrNull() ?: 0.0
    val testOffPeak = testOffPeakUnitsInput.toDoubleOrNull() ?: 0.0

    val liveCalculation = remember(currentConfig, testUnits, testPeak, testOffPeak) {
        TariffCalculator.calculate(
            config = currentConfig,
            totalUnits = testUnits,
            peakUnits = testPeak,
            offPeakUnits = testOffPeak
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = BijliStrings.tariffSettings(currentLang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentLang == AppLanguage.URDU) "سلیب ریٹس اور پیک اوقات" else "Slab Tiers & Peak/Off-Peak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tariff_settings_back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset_tariffs_button")
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Defaults")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    tariffRepository.saveTariff(currentConfig)
                    Toast.makeText(
                        context,
                        if (currentLang == AppLanguage.URDU) "ٹیرف ریٹس کامیابی سے محفوظ ہو گئے!" else "Tariff rates saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                icon = { Icon(imageVector = Icons.Default.Check, contentDescription = "Save") },
                text = {
                    Text(
                        text = BijliStrings.saveTariff(currentLang),
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("save_tariff_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Header Information & Active Status Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentConfig.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (currentLang == AppLanguage.URDU)
                                "تمام بلوں اور تخمینوں میں یہ ٹیرف لاگو ہوگا"
                            else
                                "Accurate calculations applied to all split & single bills",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. DISCO Presets Selector
            Text(
                text = "⚡ " + BijliStrings.loadPreset(currentLang),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Preset 1: NEPRA Domestic Unprotected
                ElevatedFilterChip(
                    selected = tariffName == TariffPresets.NEPRA_UNPROTECTED_DOMESTIC.name,
                    onClick = {
                        val p = TariffPresets.NEPRA_UNPROTECTED_DOMESTIC
                        tariffName = p.name
                        discoName = p.discoName
                        pricingMode = p.pricingMode
                        isProgressive = p.isSlabProgressive
                        slabsList = p.slabs
                        peakRateInput = p.peakRate.toString()
                        offPeakRateInput = p.offPeakRate.toString()
                        flatRateInput = p.flatRate.toString()
                        fixedChargesInput = p.fixedCharges.toString()
                        gstPercentageInput = p.gstPercentage.toString()
                        fpaPerUnitInput = p.fpaPerUnit.toString()
                        electricityDutyInput = p.electricityDutyPercentage.toString()
                        tvFeeInput = p.tvFee.toString()
                    },
                    label = { Text("NEPRA Slabs", fontSize = 12.sp) },
                    modifier = Modifier.testTag("preset_nepra_unprotected")
                )

                // Preset 2: Protected Lifeline
                ElevatedFilterChip(
                    selected = tariffName == TariffPresets.NEPRA_PROTECTED_DOMESTIC.name,
                    onClick = {
                        val p = TariffPresets.NEPRA_PROTECTED_DOMESTIC
                        tariffName = p.name
                        discoName = p.discoName
                        pricingMode = p.pricingMode
                        isProgressive = p.isSlabProgressive
                        slabsList = p.slabs
                        fixedChargesInput = p.fixedCharges.toString()
                        gstPercentageInput = p.gstPercentage.toString()
                        fpaPerUnitInput = p.fpaPerUnit.toString()
                    },
                    label = { Text("Protected (LifeLine)", fontSize = 12.sp) },
                    modifier = Modifier.testTag("preset_nepra_protected")
                )

                // Preset 3: TOU Peak / Off-Peak
                ElevatedFilterChip(
                    selected = pricingMode == TariffPricingMode.PEAK_OFF_PEAK,
                    onClick = {
                        val p = TariffPresets.DOMESTIC_TOU_PEAK_OFFPEAK
                        tariffName = p.name
                        discoName = p.discoName
                        pricingMode = p.pricingMode
                        peakRateInput = p.peakRate.toString()
                        offPeakRateInput = p.offPeakRate.toString()
                        peakHoursDesc = p.peakHoursDescription
                    },
                    label = { Text("Peak / Off-Peak", fontSize = 12.sp) },
                    modifier = Modifier.testTag("preset_tou")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Preset 4: Commercial A-2
                ElevatedFilterChip(
                    selected = tariffName == TariffPresets.COMMERCIAL_A2.name,
                    onClick = {
                        val p = TariffPresets.COMMERCIAL_A2
                        tariffName = p.name
                        discoName = p.discoName
                        pricingMode = p.pricingMode
                        flatRateInput = p.flatRate.toString()
                        fixedChargesInput = p.fixedCharges.toString()
                    },
                    label = { Text("Commercial (A-2)", fontSize = 12.sp) },
                    modifier = Modifier.testTag("preset_commercial")
                )

                // Preset 5: K-Electric Karachi
                ElevatedFilterChip(
                    selected = tariffName == TariffPresets.KELECTRIC_RESIDENTIAL.name,
                    onClick = {
                        val p = TariffPresets.KELECTRIC_RESIDENTIAL
                        tariffName = p.name
                        discoName = p.discoName
                        pricingMode = p.pricingMode
                        slabsList = p.slabs
                        fixedChargesInput = p.fixedCharges.toString()
                        fpaPerUnitInput = p.fpaPerUnit.toString()
                    },
                    label = { Text("K-Electric Karachi", fontSize = 12.sp) },
                    modifier = Modifier.testTag("preset_kelectric")
                )
            }

            // 3. Pricing Mode Selection Segmented Control
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = BijliStrings.pricingMode(currentLang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pricingMode = TariffPricingMode.SLAB_BASED },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_slab_based"),
                            colors = if (pricingMode == TariffPricingMode.SLAB_BASED) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("📊 " + if (currentLang == AppLanguage.URDU) "سلیب ریٹس" else "Slab Tiers", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { pricingMode = TariffPricingMode.PEAK_OFF_PEAK },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_peak_off_peak"),
                            colors = if (pricingMode == TariffPricingMode.PEAK_OFF_PEAK) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("⏱️ " + if (currentLang == AppLanguage.URDU) "پیک/آف پیک" else "Peak/Off-Peak", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { pricingMode = TariffPricingMode.FLAT_RATE },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_flat_rate"),
                            colors = if (pricingMode == TariffPricingMode.FLAT_RATE) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("⚡ " + if (currentLang == AppLanguage.URDU) "یکساں" else "Flat Rate", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 4. Dynamic Section Based on Mode
            when (pricingMode) {
                TariffPricingMode.SLAB_BASED -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (currentLang == AppLanguage.URDU) "سلیب ریٹس کی ترتیب" else "Define Slab Rates",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (currentLang == AppLanguage.URDU)
                                            "${slabsList.size} سلیبز مقرر ہیں"
                                        else
                                            "${slabsList.size} slabs configured",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        // Add a new upper slab
                                        val lastSlab = slabsList.lastOrNull()
                                        val newFrom = if (lastSlab != null) (lastSlab.toUnits + 1.0) else 1.0
                                        val newTo = newFrom + 100.0
                                        val newRate = if (lastSlab != null) lastSlab.ratePerUnit + 5.0 else 25.0
                                        val newSlab = TariffSlab(
                                            fromUnits = newFrom,
                                            toUnits = newTo,
                                            ratePerUnit = newRate,
                                            label = "${newFrom.toInt()} – ${newTo.toInt()} Units"
                                        )
                                        slabsList = slabsList + newSlab
                                    },
                                    modifier = Modifier.testTag("add_slab_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(BijliStrings.addSlab(currentLang))
                                }
                            }

                            // Progressive calculation switch
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (currentLang == AppLanguage.URDU) "مرحلہ وار سلیب طریقہ (NEPRA قاعدہ)" else "Progressive Slab Tiers",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (currentLang == AppLanguage.URDU)
                                                "ہر سلیب کے یونٹس اس سلیب کے مخصوص ریٹ پر گنے جائیں گے"
                                            else
                                                "Each tier charged at its specific rate (Standard NEPRA)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isProgressive,
                                        onCheckedChange = { isProgressive = it },
                                        modifier = Modifier.testTag("progressive_slabs_switch")
                                    )
                                }
                            }

                            // List of Slabs
                            slabsList.forEachIndexed { index, slab ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Slab ${index + 1}: ${slab.label}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (slabsList.size > 1) {
                                                IconButton(
                                                    onClick = {
                                                        slabsList = slabsList.filterIndexed { i, _ -> i != index }
                                                    },
                                                    modifier = Modifier.size(32.dp).testTag("delete_slab_$index")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Remove Slab",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // From Units
                                            OutlinedTextField(
                                                value = if (slab.fromUnits == slab.fromUnits.toInt().toDouble()) slab.fromUnits.toInt().toString() else slab.fromUnits.toString(),
                                                onValueChange = { newVal ->
                                                    val clean = newVal.filter { it.isDigit() || it == '.' }
                                                    val num = clean.toDoubleOrNull() ?: 0.0
                                                    slabsList = slabsList.mapIndexed { i, s ->
                                                        if (i == index) s.copy(fromUnits = num) else s
                                                    }
                                                },
                                                label = { Text("From Units") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            // To Units
                                            OutlinedTextField(
                                                value = if (slab.isUncapped) "Uncapped" else if (slab.toUnits == slab.toUnits.toInt().toDouble()) slab.toUnits.toInt().toString() else slab.toUnits.toString(),
                                                onValueChange = { newVal ->
                                                    val clean = newVal.filter { it.isDigit() || it == '.' }
                                                    val num = clean.toDoubleOrNull() ?: 99999.0
                                                    slabsList = slabsList.mapIndexed { i, s ->
                                                        if (i == index) s.copy(toUnits = num, label = "${s.fromUnits.toInt()} – ${num.toInt()} Units") else s
                                                    }
                                                },
                                                label = { Text("To Units") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            // Rate per Unit
                                            OutlinedTextField(
                                                value = slab.ratePerUnit.toString(),
                                                onValueChange = { newVal ->
                                                    val clean = newVal.filter { it.isDigit() || it == '.' }
                                                    val rate = clean.toDoubleOrNull() ?: 0.0
                                                    slabsList = slabsList.mapIndexed { i, s ->
                                                        if (i == index) s.copy(ratePerUnit = rate) else s
                                                    }
                                                },
                                                label = { Text("Rs./Unit") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1.2f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TariffPricingMode.PEAK_OFF_PEAK -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = if (currentLang == AppLanguage.URDU) "پیک اور آف پیک ریٹس (ٹائم آف یوز)" else "Peak & Off-Peak Rates (TOU)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = if (currentLang == AppLanguage.URDU)
                                    "پاکستان میں 3 فیز میٹرز کے لیے پیک اوقات (عام طور پر شام 5 سے رات 11 بجے) کا ریٹ الگ ہوتا ہے"
                                else
                                    "Applies to 3-phase commercial and residential meters with separate peak hour registers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = peakRateInput,
                                    onValueChange = { peakRateInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    label = { Text(if (currentLang == AppLanguage.URDU) "پیک ریٹ (روپے)" else "Peak Rate (Rs.)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("peak_rate_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = offPeakRateInput,
                                    onValueChange = { offPeakRateInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    label = { Text(if (currentLang == AppLanguage.URDU) "آف پیک ریٹ (روپے)" else "Off-Peak Rate (Rs.)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("off_peak_rate_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            OutlinedTextField(
                                value = peakHoursDesc,
                                onValueChange = { peakHoursDesc = it },
                                label = { Text(if (currentLang == AppLanguage.URDU) "پیک اوقات کی وضاحت" else "Peak Hours Schedule") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                TariffPricingMode.FLAT_RATE -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = if (currentLang == AppLanguage.URDU) "یکساں ریٹ فی یونٹ" else "Flat Rate per Unit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = flatRateInput,
                                onValueChange = { flatRateInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text(if (currentLang == AppLanguage.URDU) "ریٹ فی یونٹ (روپے)" else "Rate / Unit (Rs.)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("flat_rate_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // 5. Taxes, Surcharges & Fixed Charges Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = BijliGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLang == AppLanguage.URDU) "ٹیکسز، ایندھن ایڈجسٹمنٹ اور فکسڈ چارجز" else "Taxes, FPA & Fixed Monthly Surcharges",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = fixedChargesInput,
                            onValueChange = { fixedChargesInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Fixed Charges (Rs.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fixed_charges_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = gstPercentageInput,
                            onValueChange = { gstPercentageInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Govt GST (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gst_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = fpaPerUnitInput,
                            onValueChange = { fpaPerUnitInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("FPA / Unit (Rs.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fpa_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = electricityDutyInput,
                            onValueChange = { electricityDutyInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Electricity Duty (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = tvFeeInput,
                            onValueChange = { tvFeeInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("PTV Fee (Rs.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.9f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 6. Interactive Live Test Simulator
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = BijliStrings.testSimulator(currentLang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                ShareHelper.shareText(
                                    context = context,
                                    text = if (currentLang == AppLanguage.URDU) liveCalculation.calculationSummaryUr else liveCalculation.calculationSummaryEn,
                                    title = "Tariff Calculation Breakdown"
                                )
                            },
                            modifier = Modifier.size(36.dp).testTag("share_simulator_result")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Test inputs
                    if (pricingMode == TariffPricingMode.PEAK_OFF_PEAK) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = testPeakUnitsInput,
                                onValueChange = { testPeakUnitsInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Test Peak Units") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_peak_units_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = testOffPeakUnitsInput,
                                onValueChange = { testOffPeakUnitsInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Test Off-Peak Units") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_offpeak_units_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = testUnitsInput,
                            onValueChange = { testUnitsInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(if (currentLang == AppLanguage.URDU) "ٹیسٹ یونٹس درج کریں" else "Enter Test Consumption Units") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_units_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Live Breakdown Display
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Slabs Breakdown List
                            if (liveCalculation.slabItems.isNotEmpty()) {
                                Text(
                                    text = if (currentLang == AppLanguage.URDU) "سلیب وار لاگت:" else "Slab Breakdown:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                liveCalculation.slabItems.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.slab.label} (${String.format("%.1f", item.unitsConsumedInSlab)} u × Rs. ${item.rate})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Rs. ${String.format("%.0f", item.costRupees)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            } else if (pricingMode == TariffPricingMode.PEAK_OFF_PEAK) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Peak (${String.format("%.1f", liveCalculation.peakUnits)} u × Rs. ${currentConfig.peakRate}):", style = MaterialTheme.typography.bodySmall)
                                    Text("Rs. ${String.format("%.0f", liveCalculation.peakCostRupees)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Off-Peak (${String.format("%.1f", liveCalculation.offPeakUnits)} u × Rs. ${currentConfig.offPeakRate}):", style = MaterialTheme.typography.bodySmall)
                                    Text("Rs. ${String.format("%.0f", liveCalculation.offPeakCostRupees)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Base Energy Cost:", style = MaterialTheme.typography.bodySmall)
                                Text("Rs. ${String.format("%.0f", liveCalculation.energyCostRupees)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fixed + FPA + Taxes:", style = MaterialTheme.typography.bodySmall)
                                Text("Rs. ${String.format("%.0f", liveCalculation.totalTaxesAndSurcharges)}", style = MaterialTheme.typography.bodySmall)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (currentLang == AppLanguage.URDU) "کل تخمینی بل:" else "Total Estimated Bill:",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Effective: Rs. ${String.format("%.2f", liveCalculation.effectiveRatePerUnit)} / unit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Rs. ${liveCalculation.estimatedTotalRupees}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = if (currentLang == AppLanguage.URDU) "ٹیرف ری سیٹ کریں؟" else "Reset Tariff to Standard?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (currentLang == AppLanguage.URDU)
                        "کیا آپ ٹیرف کو نیپرا (NEPRA) کے بنیادی سرکاری سلیب ریٹس پر بحال کرنا چاہتے ہیں؟"
                    else
                        "This will reset all slabs, peak rates, and taxes to standard NEPRA 2024-2026 domestic tariffs."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = TariffPresets.NEPRA_UNPROTECTED_DOMESTIC
                        tariffRepository.saveTariff(p)
                        tariffName = p.name
                        discoName = p.discoName
                        pricingMode = p.pricingMode
                        isProgressive = p.isSlabProgressive
                        slabsList = p.slabs
                        peakRateInput = p.peakRate.toString()
                        offPeakRateInput = p.offPeakRate.toString()
                        flatRateInput = p.flatRate.toString()
                        fixedChargesInput = p.fixedCharges.toString()
                        gstPercentageInput = p.gstPercentage.toString()
                        fpaPerUnitInput = p.fpaPerUnit.toString()
                        electricityDutyInput = p.electricityDutyPercentage.toString()
                        tvFeeInput = p.tvFee.toString()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (currentLang == AppLanguage.URDU) "ہاں، ری سیٹ کریں" else "Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(if (currentLang == AppLanguage.URDU) "منسوخ" else "Cancel")
                }
            }
        )
    }
}
