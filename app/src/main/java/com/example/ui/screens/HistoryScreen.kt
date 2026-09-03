package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Share
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
import com.example.data.BijliRepository
import com.example.data.PaymentEntryEntity
import com.example.data.SavedBillEntity
import com.example.data.SavedPortionShareEntity
import com.example.localization.AppLanguage
import com.example.localization.BijliStrings
import com.example.ui.ShareHelper
import com.example.ui.theme.BijliAmberDark
import com.example.ui.theme.BijliGreenPrimary
import com.example.ui.theme.BijliSuccess
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    repository: BijliRepository,
    currentLang: AppLanguage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val savedBills by repository.allSavedBills.collectAsState(initial = emptyList())
    var selectedBill by remember { mutableStateOf<SavedBillEntity?>(null) }
    var recordingPaymentForShare by remember { mutableStateOf<SavedPortionShareEntity?>(null) }

    val sharesForSelected by produceState<List<SavedPortionShareEntity>>(
        initialValue = emptyList(),
        key1 = selectedBill
    ) {
        selectedBill?.let { bill ->
            repository.getSharesForBill(bill.id).collect { value = it }
        } ?: run { value = emptyList() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = BijliStrings.history(currentLang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (savedBills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (currentLang == AppLanguage.URDU) "ابھی تک کوئی بل محفوظ نہیں کیا گیا" else "No saved bill history yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (currentLang == AppLanguage.URDU) "اصل بل تقسیم کریں اور محفوظ کریں" else "Split a bill and tap 'Save Record' to view it here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(savedBills) { bill ->
                    val isExpanded = selectedBill?.id == bill.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBill = if (isExpanded) null else bill },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = if (isExpanded) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BijliGreenPrimary)) else CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bill.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(bill.createdAt))
                                    Text(
                                        text = "Saved on $dateStr • ${bill.allocationMethod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = BijliStrings.formatRupees(bill.sourceBillRupees),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = BijliGreenPrimary
                                    )
                                    Text(
                                        text = "Main: ${bill.mainMeterUnits.toInt()} U | Common: ${bill.commonUnits.toInt()} U",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Details when expanded
                            if (isExpanded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                Text(
                                    text = if (currentLang == AppLanguage.URDU) "پورشنز اور ادائیگیاں (Portion Payments):" else "Portion Shares & Payments:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (share in sharesForSelected) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(text = share.portionName, fontWeight = FontWeight.Bold)
                                                        if (share.tenantName.isNotBlank()) {
                                                            Text(text = "👤 ${share.tenantName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(text = BijliStrings.formatRupees(share.allocatedAmountRupees), fontWeight = FontWeight.Bold, color = BijliGreenPrimary)
                                                        Surface(
                                                            color = when (share.paymentStatus) {
                                                                "PAID", "ADVANCE" -> BijliSuccess.copy(alpha = 0.15f)
                                                                "PARTIAL" -> BijliAmberDark.copy(alpha = 0.15f)
                                                                else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                            },
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = when (share.paymentStatus) {
                                                                    "PAID" -> "PAID"
                                                                    "ADVANCE" -> "ADVANCE / CREDIT"
                                                                    "PARTIAL" -> "PARTIAL: Paid ${BijliStrings.formatRupees(share.paidAmountRupees)}"
                                                                    else -> "UNPAID"
                                                                },
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = when (share.paymentStatus) {
                                                                    "PAID", "ADVANCE" -> BijliSuccess
                                                                    "PARTIAL" -> BijliAmberDark
                                                                    else -> MaterialTheme.colorScheme.error
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { recordingPaymentForShare = share },
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Record Payment", fontSize = 11.sp)
                                                    }

                                                    FilledTonalButton(
                                                        onClick = {
                                                            val shareText = buildString {
                                                                appendLine("⚡ *BIJLIHISAB RECEIPT / BILL*")
                                                                appendLine("Portion: ${share.portionName}")
                                                                if (share.tenantName.isNotBlank()) appendLine("Tenant: ${share.tenantName}")
                                                                appendLine("Bill Month: ${bill.billMonthYear}")
                                                                appendLine("Units Consumed: ${BijliStrings.formatUnits(share.unitsUsed)}")
                                                                appendLine("Total Bill Amount: ${BijliStrings.formatRupees(share.allocatedAmountRupees)}")
                                                                appendLine("Paid Amount: ${BijliStrings.formatRupees(share.paidAmountRupees)}")
                                                                appendLine("Status: ${share.paymentStatus}")
                                                                appendLine("Generated via BijliHisab")
                                                            }
                                                            ShareHelper.shareText(context, shareText, "Share Receipt")
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Share Receipt", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch { repository.deleteBill(bill) }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete Bill Record", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment Dialog
    recordingPaymentForShare?.let { share ->
        var paymentAmtInput by remember { mutableStateOf((share.allocatedAmountRupees - share.paidAmountRupees).coerceAtLeast(0L).toString()) }
        var selectedMethod by remember { mutableStateOf("Cash") }
        val paymentMethods = listOf("Cash", "Easypaisa", "JazzCash", "Bank Transfer", "SadaPay", "NayaPay")

        AlertDialog(
            onDismissRequest = { recordingPaymentForShare = null },
            title = { Text("Record Payment for ${share.portionName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Total Payable: ${BijliStrings.formatRupees(share.allocatedAmountRupees)}")
                    Text(text = "Already Paid: ${BijliStrings.formatRupees(share.paidAmountRupees)}")

                    OutlinedTextField(
                        value = paymentAmtInput,
                        onValueChange = { paymentAmtInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Payment Amount (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "Payment Method:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (m in paymentMethods.take(3)) {
                            FilterChip(
                                selected = selectedMethod == m,
                                onClick = { selectedMethod = m },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (m in paymentMethods.drop(3)) {
                            FilterChip(
                                selected = selectedMethod == m,
                                onClick = { selectedMethod = m },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = paymentAmtInput.toLongOrNull() ?: 0L
                        if (amt > 0) {
                            coroutineScope.launch {
                                val payment = PaymentEntryEntity(
                                    billId = share.billId,
                                    shareId = share.id,
                                    tenantName = share.tenantName,
                                    amountRupees = amt,
                                    method = selectedMethod
                                )
                                repository.recordPayment(payment, share)
                                recordingPaymentForShare = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BijliGreenPrimary)
                ) {
                    Text("Save Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingPaymentForShare = null }) { Text("Cancel") }
            }
        )
    }
}
