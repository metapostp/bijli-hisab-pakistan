package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
fun CalculateBillScreen(
    currentLang: AppLanguage,
    tariffRepository: TariffRepository? = null,
    onNavigateToTariffSettings: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeTariff = tariffRepository?.activeTariff?.collectAsState()?.value
        ?: TariffPresets.NEPRA_UNPROTECTED_DOMESTIC

    var useDefinedTariff by remember { mutableStateOf(true) }

    // Defined Tariff Mode Inputs
    var unitsInput by remember { mutableStateOf("250") }
    var peakUnitsInput by remember { mutableStateOf("60") }
    var offPeakUnitsInput by remember { mutableStateOf("190") }

    // Manual Quick Rate Mode Inputs
    var manualRatePerUnitInput by remember { mutableStateOf("45") }
    var manualFixedChargesInput by remember { mutableStateOf("500") }
    var manualGstPercentageInput by remember { mutableStateOf("18") }

    val units = unitsInput.toDoubleOrNull() ?: 0.0
    val peakUnits = peakUnitsInput.toDoubleOrNull() ?: 0.0
    val offPeakUnits = offPeakUnitsInput.toDoubleOrNull() ?: 0.0

    // Tariff calculation
    val tariffCalculation = remember(activeTariff, units, peakUnits, offPeakUnits, useDefinedTariff) {
        TariffCalculator.calculate(
            config = activeTariff,
            totalUnits = units,
            peakUnits = peakUnits,
            offPeakUnits = offPeakUnits
        )
    }

    // Manual calculation
    val manualRate = manualRatePerUnitInput.toDoubleOrNull() ?: 45.0
    val manualFixed = manualFixedChargesInput.toDoubleOrNull() ?: 0.0
    val manualGst = manualGstPercentageInput.toDoubleOrNull() ?: 18.0
    val manualEnergyCost = units * manualRate
    val manualSubtotal = manualEnergyCost + manualFixed
    val manualTaxes = manualSubtotal * (manualGst / 100.0)
    val manualTotal = (manualSubtotal + manualTaxes).toLong()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = BijliStrings.calculateBill(currentLang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (useDefinedTariff) "Tariff: ${activeTariff.name}" else "Manual Rate Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToTariffSettings,
                        modifier = Modifier.testTag("settings_tariff_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Tariff Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val summary = if (useDefinedTariff) {
                        ShareHelper.formatTariffDetailedSummary(
                            calculation = tariffCalculation,
                            tariff = activeTariff,
                            lang = currentLang
                        )
                    } else {
                        ShareHelper.formatEstimatedBillSummaryText(
                            units = units,
                            ratePerUnit = manualRate,
                            fixedCharges = manualFixed,
                            gstPercentage = manualGst,
                            energyCost = manualEnergyCost,
                            taxes = manualTaxes,
                            estimatedTotal = manualTotal
                        )
                    }
                    ShareHelper.shareText(
                        context = context,
                        text = summary,
                        title = "Share Estimated Bill Summary"
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Estimated Bill"
                    )
                },
                text = {
                    Text(
                        text = if (currentLang == AppLanguage.URDU) "بل شیئر کریں" else "Share Summary",
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("share_estimated_bill_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Calculation Mode Segmented Selector
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { useDefinedTariff = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_defined_tariff"),
                        colors = if (useDefinedTariff) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(
                            text = "⚡ " + if (currentLang == AppLanguage.URDU) "مقررہ ٹیرف ریٹس" else "Defined Tariff",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { useDefinedTariff = false },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_manual_rate"),
                        colors = if (!useDefinedTariff) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(
                            text = "✏️ " + if (currentLang == AppLanguage.URDU) "سادہ دستی ریٹ" else "Manual Rate",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Trust Notice
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = BijliGreenPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "📋 " + BijliStrings.trustLabelEstimated(currentLang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Active Tariff Banner (When using Defined Tariff)
            if (useDefinedTariff) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeTariff.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Mode: ${activeTariff.pricingMode.name} • ${activeTariff.discoName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalButton(
                            onClick = onNavigateToTariffSettings,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("edit_tariff_settings_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Consumption Inputs Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (currentLang == AppLanguage.URDU) "استعمال شدہ یونٹس درج کریں" else "Enter Units Consumed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (useDefinedTariff && activeTariff.pricingMode == TariffPricingMode.PEAK_OFF_PEAK) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = peakUnitsInput,
                                onValueChange = { peakUnitsInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Peak Units (${activeTariff.peakRate} Rs/u)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("calc_peak_units_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = offPeakUnitsInput,
                                onValueChange = { offPeakUnitsInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Off-Peak (${activeTariff.offPeakRate} Rs/u)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("calc_offpeak_units_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = unitsInput,
                            onValueChange = { unitsInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(if (currentLang == AppLanguage.URDU) "استعمال شدہ یونٹس" else "Units Consumed") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("calc_units_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Manual Inputs (Only in Manual Mode)
                    if (!useDefinedTariff) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = manualRatePerUnitInput,
                                onValueChange = { manualRatePerUnitInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text(if (currentLang == AppLanguage.URDU) "ریٹ فی یونٹ (روپے)" else "Rate / Unit (Rs.)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = manualFixedChargesInput,
                                onValueChange = { manualFixedChargesInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text(if (currentLang == AppLanguage.URDU) "فکسڈ چارجز (روپے)" else "Fixed Charges (Rs.)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = manualGstPercentageInput,
                            onValueChange = { manualGstPercentageInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(if (currentLang == AppLanguage.URDU) "ٹیکس و ڈیوٹی فیصد (%)" else "Govt Taxes & GST (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Calculation Results Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BijliGreenPrimary.copy(alpha = 0.08f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BijliGreenPrimary))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (currentLang == AppLanguage.URDU) "تخمینہ بل کی تفصیل" else "Estimated Bill Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BijliGreenPrimary
                    )

                    if (useDefinedTariff) {
                        // Slab breakdown items
                        if (tariffCalculation.slabItems.isNotEmpty()) {
                            tariffCalculation.slabItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.slab.label} (${String.format("%.1f", item.unitsConsumedInSlab)} u × Rs. ${item.rate}):",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Rs. ${String.format("%.0f", item.costRupees)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            HorizontalDivider()
                        } else if (activeTariff.pricingMode == TariffPricingMode.PEAK_OFF_PEAK) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Peak (${String.format("%.1f", tariffCalculation.peakUnits)} u × Rs. ${activeTariff.peakRate}):", style = MaterialTheme.typography.bodySmall)
                                Text("Rs. ${String.format("%.0f", tariffCalculation.peakCostRupees)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Off-Peak (${String.format("%.1f", tariffCalculation.offPeakUnits)} u × Rs. ${activeTariff.offPeakRate}):", style = MaterialTheme.typography.bodySmall)
                                Text("Rs. ${String.format("%.0f", tariffCalculation.offPeakCostRupees)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            HorizontalDivider()
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Base Energy Charges:")
                            Text("Rs. ${String.format("%.0f", tariffCalculation.energyCostRupees)}", fontWeight = FontWeight.Bold)
                        }

                        if (tariffCalculation.fixedChargesRupees > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fixed Monthly Surcharge:")
                                Text("Rs. ${String.format("%.0f", tariffCalculation.fixedChargesRupees)}")
                            }
                        }

                        if (tariffCalculation.fpaRupees > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fuel Price Adjustment (FPA):")
                                Text("Rs. ${String.format("%.0f", tariffCalculation.fpaRupees)}")
                            }
                        }

                        if (tariffCalculation.gstRupees > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Govt GST (${String.format("%.1f", activeTariff.gstPercentage)}%):")
                                Text("Rs. ${String.format("%.0f", tariffCalculation.gstRupees)}")
                            }
                        }

                        if (tariffCalculation.tvFeeRupees > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("PTV License Fee:")
                                Text("Rs. ${String.format("%.0f", tariffCalculation.tvFeeRupees)}")
                            }
                        }

                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(
                                    text = if (currentLang == AppLanguage.URDU) "کل متوقع بل:" else "Total Estimated Bill:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Effective: Rs. ${String.format("%.2f", tariffCalculation.effectiveRatePerUnit)}/unit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Rs. ${tariffCalculation.estimatedTotalRupees}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = BijliGreenPrimary
                            )
                        }
                    } else {
                        // Manual Mode Breakdown
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Energy Charges (${units.toInt()} units × Rs. $manualRate):")
                            Text(BijliStrings.formatRupees(manualEnergyCost.toLong()), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Fixed Charges:")
                            Text(BijliStrings.formatRupees(manualFixed.toLong()))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Taxes & GST (${manualGst.toInt()}%):")
                            Text(BijliStrings.formatRupees(manualTaxes.toLong()))
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (currentLang == AppLanguage.URDU) "کل متوقع بل:" else "Total Estimated Bill:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = BijliStrings.formatRupees(manualTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = BijliGreenPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
