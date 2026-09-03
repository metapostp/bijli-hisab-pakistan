package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.billing.PortionShareResult
import com.example.billing.ReconciliationReport
import com.example.localization.AppLanguage
import com.example.localization.BijliStrings
import com.example.ui.theme.BijliGreenPrimary

@Composable
fun ExplanationDialog(
    portion: PortionShareResult,
    reconciliation: ReconciliationReport,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (lang == AppLanguage.URDU) "حساب کی شفاف تفصیل" else "Calculation Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BijliGreenPrimary
                        )
                        Text(
                            text = portion.portionName + if (portion.tenantName.isNotBlank()) " (${portion.tenantName})" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Meter Readings Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (lang == AppLanguage.URDU) "1. میٹر ریڈنگز اور استعمال" else "1. Meter Readings & Consumption",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (lang == AppLanguage.URDU) "پچھلی ریڈنگ:" else "Previous Reading:")
                            Text(text = "${String.format("%.0f", portion.previousReading)}", fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (lang == AppLanguage.URDU) "موجودہ ریڈنگ:" else "Current Reading:")
                            Text(text = "${String.format("%.0f", portion.currentReading)}", fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (lang == AppLanguage.URDU) "استعمال شدہ یونٹس:" else "Units Used (Current - Prev):",
                                fontWeight = FontWeight.SemiBold,
                                color = BijliGreenPrimary
                            )
                            Text(
                                text = BijliStrings.formatUnits(portion.unitsUsed),
                                fontWeight = FontWeight.Bold,
                                color = BijliGreenPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Consumption Share & Common Units
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (lang == AppLanguage.URDU) "2. تناسب اور مشترکہ یونٹس" else "2. Share & Common Units",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (lang == AppLanguage.URDU) "پورشن کا حصہ:" else "Portion Share:")
                            Text(text = "${String.format("%.1f", portion.consumptionSharePct)}%", fontWeight = FontWeight.Bold)
                        }
                        if (portion.commonUnitsAssigned > 0.001) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = if (lang == AppLanguage.URDU) "مشترکہ یونٹس حصہ:" else "Common Units Assigned:")
                                Text(text = "+${BijliStrings.formatUnits(portion.commonUnitsAssigned)}", fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.URDU) "کل قابل بل یونٹس:" else "Total Billable Units:",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(text = BijliStrings.formatUnits(portion.totalBillableUnits), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Calculation
                Card(
                    colors = CardDefaults.cardColors(containerColor = BijliGreenPrimary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (lang == AppLanguage.URDU) "3. رقم کا حساب" else "3. Amount Calculation",
                            fontWeight = FontWeight.Bold,
                            color = BijliGreenPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = portion.calculationNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (portion.roundingAdjustment != 0L) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Exact decimal: Rs. ${String.format("%.2f", portion.exactDecimalAmount.toDouble())} (Largest remainder rounding adjustment: ${if (portion.roundingAdjustment > 0) "+Rs. " else "-Rs. "}${Math.abs(portion.roundingAdjustment)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == AppLanguage.URDU) "کل واجب الادا رقم:" else "Total Payable Amount:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = BijliStrings.formatRupees(portion.totalAmountRupees),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = BijliGreenPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Trust Note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BijliGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == AppLanguage.URDU) "تمام پورشنز کی کل رقم اصل بل کے بالکل برابر ہے۔" else "All portions sum up exactly to the source bill. Zero discrepancy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BijliGreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (lang == AppLanguage.URDU) "سمجھ گیا (ٹھیک ہے)" else "Understood (Close)")
                }
            }
        }
    }
}
