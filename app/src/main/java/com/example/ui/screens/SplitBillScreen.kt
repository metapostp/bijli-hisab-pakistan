package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.*
import com.example.data.BijliRepository
import com.example.data.SavedBillEntity
import com.example.data.SavedPortionShareEntity
import com.example.data.TariffRepository
import com.example.localization.AppLanguage
import com.example.localization.BijliStrings
import com.example.ui.ShareHelper
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    repository: BijliRepository,
    currentLang: AppLanguage,
    onBack: () -> Unit,
    onBillSaved: () -> Unit,
    initialPortions: List<PortionInput>? = null,
    onNavigateToConverter: (() -> Unit)? = null,
    tariffRepository: TariffRepository? = null,
    onNavigateToTariffSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val activeTariff = tariffRepository?.activeTariff?.collectAsState()?.value

    // State for Step 1: Bill Info
    var billAmountInput by remember { mutableStateOf("8000") }
    var billMonthInput by remember { mutableStateOf("Current Month") }

    // State for Step 2: Main Meter
    var mainMeterPrevInput by remember { mutableStateOf("1000") }
    var mainMeterCurrInput by remember { mutableStateOf("1235") }

    // State for Step 3: Portions
    var portionsList by remember {
        mutableStateOf(
            initialPortions ?: listOf(
                PortionInput(id = "1", name = "Flat 1", tenantName = "Ali Raza", previousReading = 500.0, currentReading = 585.0),
                PortionInput(id = "2", name = "Flat 2", tenantName = "Usman Khan", previousReading = 700.0, currentReading = 735.0),
                PortionInput(id = "3", name = "Flat 3", tenantName = "Hamza Farooq", previousReading = 900.0, currentReading = 980.0)
            )
        )
    }

    LaunchedEffect(initialPortions) {
        if (initialPortions != null && initialPortions.isNotEmpty()) {
            portionsList = initialPortions
        }
    }

    // Method & Common units handling
    var selectedMethod by remember { mutableStateOf(AllocationMethod.PRO_RATA) }
    var selectedCommonHandling by remember { mutableStateOf(CommonUnitsHandling.PROPORTIONAL_TO_UNITS) }
    var customRateInput by remember { mutableStateOf("65") }

    // Dialog state for explanation
    var explainingPortion by remember { mutableStateOf<PortionShareResult?>(null) }
    var showAddPortionDialog by remember { mutableStateOf(false) }

    // Parse values safely
    val billRupees = billAmountInput.toLongOrNull() ?: 0L
    val mainPrev = mainMeterPrevInput.toDoubleOrNull() ?: 0.0
    val mainCurr = mainMeterCurrInput.toDoubleOrNull() ?: 0.0
    val mainMeter = MeterReading(mainPrev, mainCurr)
    val customRate = customRateInput.toDoubleOrNull() ?: 65.0

    // Automatic calculation via isolated pure BillingEngine
    val calculationResult = remember(
        billRupees,
        mainPrev,
        mainCurr,
        portionsList,
        selectedMethod,
        selectedCommonHandling,
        customRate
    ) {
        BillingEngine.calculateBill(
            sourceBillRupees = billRupees,
            mainMeter = mainMeter,
            portions = portionsList,
            allocationMethod = selectedMethod,
            commonUnitsHandling = selectedCommonHandling,
            defaultCustomRate = customRate
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = BijliStrings.splitActualBill(currentLang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "BijliHisab Pakistan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Reset to canonical example
                        billAmountInput = "8000"
                        mainMeterPrevInput = "1000"
                        mainMeterCurrInput = "1235"
                        portionsList = listOf(
                            PortionInput(id = "1", name = "Flat 1", tenantName = "Ali Raza", previousReading = 500.0, currentReading = 585.0),
                            PortionInput(id = "2", name = "Flat 2", tenantName = "Usman Khan", previousReading = 700.0, currentReading = 735.0),
                            PortionInput(id = "3", name = "Flat 3", tenantName = "Hamza Farooq", previousReading = 900.0, currentReading = 980.0)
                        )
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset example")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (calculationResult.isSuccess) {
                        val summary = ShareHelper.formatAllPortionsSummaryText(
                            portions = calculationResult.portionShares,
                            reconciliation = calculationResult.reconciliation,
                            monthYear = billMonthInput
                        )
                        ShareHelper.shareText(
                            context = context,
                            text = summary,
                            title = "Share Bill Calculation Summary"
                        )
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                calculationResult.errorMessage ?: "Please enter valid bill details to export summary."
                            )
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Bill Summary"
                    )
                },
                text = {
                    Text(
                        text = if (currentLang == AppLanguage.URDU) "خلاصہ شیئر کریں" else "Share Summary",
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("share_summary_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // STEP 1: BILL AMOUNT
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("step1_bill_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(BijliGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = BijliStrings.actualBillAmount(currentLang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = billAmountInput,
                            onValueChange = { billAmountInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text(if (currentLang == AppLanguage.URDU) "بل کی رقم (روپے)" else "Actual Bill (Rupees)") },
                            prefix = { Text("Rs. ", fontWeight = FontWeight.Bold, color = BijliGreenPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = billMonthInput,
                            onValueChange = { billMonthInput = it },
                            label = { Text(if (currentLang == AppLanguage.URDU) "بل کا مہینہ / تفصیل (اختیاری)" else "Bill Month / Reference (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // STEP 2: MAIN METER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("step2_main_meter_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(BijliGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = BijliStrings.mainMeter(currentLang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = mainMeterPrevInput,
                                onValueChange = { mainMeterPrevInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text(BijliStrings.previousReading(currentLang)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("main_prev_reading"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = mainMeterCurrInput,
                                onValueChange = { mainMeterCurrInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text(BijliStrings.currentReading(currentLang)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("main_curr_reading"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Auto-computed units badge
                        Surface(
                            color = if (mainMeter.isValid) BijliGreenPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentLang == AppLanguage.URDU) "مین میٹر استعمال شدہ یونٹس:" else "Main Meter Units Used:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (mainMeter.isValid) BijliStrings.formatUnits(mainMeter.unitsUsed) else "Invalid",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mainMeter.isValid) BijliGreenPrimary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // STEP 3: SUB-METERS / PORTIONS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BijliGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("3", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = BijliStrings.subMeters(currentLang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onNavigateToConverter != null) {
                            OutlinedButton(
                                onClick = onNavigateToConverter,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("open_converter_from_split_button")
                            ) {
                                Icon(Icons.Default.ElectricMeter, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (currentLang == AppLanguage.URDU) "کنورٹر" else "Converter", fontSize = 12.sp)
                            }
                        }

                        FilledTonalButton(
                            onClick = { showAddPortionDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_portion_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (currentLang == AppLanguage.URDU) "نیا پورشن" else "Add Portion")
                        }
                    }
                }
            }

            // List of Portions Inputs
            itemsIndexed(portionsList) { index, portion ->
                var showFormatMenu by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = portion.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (portion.format != MeterReadingFormat.STANDARD_DIGITAL)
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { showFormatMenu = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = portion.format.shortBadge,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (portion.format != MeterReadingFormat.STANDARD_DIGITAL)
                                                        MaterialTheme.colorScheme.onTertiaryContainer
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = showFormatMenu,
                                            onDismissRequest = { showFormatMenu = false }
                                        ) {
                                            MeterReadingFormat.values().forEach { fmt ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(
                                                                text = if (currentLang == AppLanguage.URDU) fmt.titleUr else fmt.titleEn,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = if (currentLang == AppLanguage.URDU) fmt.descriptionUr else fmt.descriptionEn,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        portionsList = portionsList.toMutableList().also {
                                                            it[index] = it[index].copy(
                                                                format = fmt,
                                                                formatMultiplier = if (fmt == MeterReadingFormat.CT_MULTIPLIER) 10.0 else 1.0
                                                            )
                                                        }
                                                        showFormatMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                if (portion.tenantName.isNotBlank()) {
                                    Text(
                                        text = portion.tenantName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        if (portionsList.size > 1) {
                                            portionsList = portionsList.filterIndexed { i, _ -> i != index }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // CT multiplier selector if CT format
                        if (portion.format == MeterReadingFormat.CT_MULTIPLIER) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CT Ratio:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                listOf(5.0, 10.0, 20.0, 40.0).forEach { ratio ->
                                    FilterChip(
                                        selected = portion.formatMultiplier == ratio,
                                        onClick = {
                                            portionsList = portionsList.toMutableList().also {
                                                it[index] = it[index].copy(formatMultiplier = ratio)
                                            }
                                        },
                                        label = { Text("${ratio.toInt()}x", fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = if (portion.previousReading == 0.0) "" else portion.previousReading.toString().removeSuffix(".0"),
                                onValueChange = { newVal ->
                                    val d = newVal.toDoubleOrNull() ?: 0.0
                                    portionsList = portionsList.toMutableList().also {
                                        it[index] = it[index].copy(previousReading = d)
                                    }
                                },
                                label = { Text(BijliStrings.previousReading(currentLang)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = if (portion.currentReading == 0.0) "" else portion.currentReading.toString().removeSuffix(".0"),
                                onValueChange = { newVal ->
                                    val d = newVal.toDoubleOrNull() ?: 0.0
                                    portionsList = portionsList.toMutableList().also {
                                        it[index] = it[index].copy(currentReading = d)
                                    }
                                },
                                label = { Text(BijliStrings.currentReading(currentLang)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Units badge with format explanation
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${portion.name} ${BijliStrings.unitsUsed(currentLang)}:",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (portion.format != MeterReadingFormat.STANDARD_DIGITAL) {
                                        Text(
                                            text = " (${portion.format.shortBadge})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = BijliStrings.formatUnits(portion.unitsUsed.coerceAtLeast(0.0)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BijliGreenPrimary
                                )
                            }
                        }
                    }
                }
            }

            // STEP 4: COMMON UNITS AUTOMATIC DETECTION & ALLOCATION
            item {
                if (calculationResult.isSuccess) {
                    val commonUnits = calculationResult.commonUnits
                    val subTotal = calculationResult.subMetersTotalUnits
                    val mainUnits = calculationResult.mainMeterUnits

                    if (commonUnits > 0.001) {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("common_units_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = BijliAmberLight.copy(alpha = 0.5f)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BijliAmber))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = BijliAmberDark
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "⚠️ ${BijliStrings.formatUnits(commonUnits)} ${BijliStrings.commonUnitsDetected(currentLang)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BijliAmberDark
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = BijliStrings.commonUnitsExplanation(commonUnits, currentLang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = BijliStrings.howToHandleCommonUnits(currentLang),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Options
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CommonOptionRow(
                                        label = "A. " + BijliStrings.proRataLabel(currentLang),
                                        selected = selectedCommonHandling == CommonUnitsHandling.PROPORTIONAL_TO_UNITS,
                                        onClick = { selectedCommonHandling = CommonUnitsHandling.PROPORTIONAL_TO_UNITS }
                                    )
                                    CommonOptionRow(
                                        label = "B. " + BijliStrings.equalSplitLabel(currentLang),
                                        selected = selectedCommonHandling == CommonUnitsHandling.DIVIDED_EQUALLY,
                                        onClick = { selectedCommonHandling = CommonUnitsHandling.DIVIDED_EQUALLY }
                                    )
                                    CommonOptionRow(
                                        label = "C. " + BijliStrings.commonExpenseLabel(currentLang),
                                        selected = selectedCommonHandling == CommonUnitsHandling.COMMON_EXPENSE,
                                        onClick = { selectedCommonHandling = CommonUnitsHandling.COMMON_EXPENSE }
                                    )
                                    CommonOptionRow(
                                        label = "D. " + BijliStrings.unallocatedLabel(currentLang),
                                        selected = selectedCommonHandling == CommonUnitsHandling.KEEP_UNALLOCATED,
                                        onClick = { selectedCommonHandling = CommonUnitsHandling.KEEP_UNALLOCATED }
                                    )
                                }
                            }
                        }
                    } else {
                        // All accounted for trust badge
                        Surface(
                            color = BijliSuccess.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BijliSuccess)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BijliSuccess
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = BijliStrings.noCommonUnits(currentLang),
                                        fontWeight = FontWeight.Bold,
                                        color = BijliSuccess
                                    )
                                    Text(
                                        text = "Main Meter (${mainUnits.toInt()}) = Sub-Meters (${subTotal.toInt()})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ALLOCATION METHOD SELECTOR
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (currentLang == AppLanguage.URDU) "تقسیم کا طریقہ کار (Billing Method)" else "Billing Method",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedMethod == AllocationMethod.PRO_RATA,
                                onClick = { selectedMethod = AllocationMethod.PRO_RATA },
                                label = { Text("Pro-Rata (Units Share)") },
                                leadingIcon = if (selectedMethod == AllocationMethod.PRO_RATA) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            FilterChip(
                                selected = selectedMethod == AllocationMethod.CUSTOM_RATE,
                                onClick = { selectedMethod = AllocationMethod.CUSTOM_RATE },
                                label = { Text("Custom Rate (Rs/Unit)") },
                                leadingIcon = if (selectedMethod == AllocationMethod.CUSTOM_RATE) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }

                        if (selectedMethod == AllocationMethod.CUSTOM_RATE) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customRateInput,
                                onValueChange = { customRateInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text(if (currentLang == AppLanguage.URDU) "فی یونٹ ریٹ (روپے)" else "Rate per unit (Rs.)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("custom_rate_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            if (activeTariff != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val suggestedRate = when (activeTariff.pricingMode) {
                                        TariffPricingMode.FLAT_RATE -> activeTariff.flatRate
                                        TariffPricingMode.PEAK_OFF_PEAK -> activeTariff.offPeakRate
                                        TariffPricingMode.SLAB_BASED -> {
                                            // Compute effective rate from active tariff for current sub-meter units
                                            val subUnits = portionsList.sumOf { it.unitsUsed }
                                            val calc = TariffCalculator.calculate(activeTariff, if (subUnits > 0.0) subUnits else 250.0)
                                            String.format("%.2f", calc.effectiveRatePerUnit).toDoubleOrNull() ?: 35.0
                                        }
                                    }

                                    AssistChip(
                                        onClick = {
                                            customRateInput = String.format("%.2f", suggestedRate)
                                        },
                                        label = {
                                            Text(
                                                text = "⚡ Use ${activeTariff.name} (Rs. ${String.format("%.1f", suggestedRate)}/u)",
                                                fontSize = 11.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    )

                                    if (onNavigateToTariffSettings != null) {
                                        TextButton(
                                            onClick = onNavigateToTariffSettings,
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Text("Edit Slabs ⚙️", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ERROR OR CALCULATION RESULTS
            if (!calculationResult.isSuccess) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = calculationResult.errorMessage ?: "Calculation error",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                // RECONCILIATION SUMMARY BOX
                item {
                    val rec = calculationResult.reconciliation
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("reconciliation_box"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BijliGreenPrimary.copy(alpha = 0.08f)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BijliGreenPrimary))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = BijliGreenPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = BijliStrings.reconciliationTitle(currentLang),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BijliGreenPrimary
                                    )
                                }

                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            if (rec.isFullyReconciled) "100% Reconciled" else "Difference Rs. ${rec.remainingUnallocatedAmount}",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Unit Reconciliation equation
                            Text(
                                text = "Units: Main (${rec.mainMeterUnits.toInt()}) = Sub-Meters (${rec.subMetersTotalUnits.toInt()}) + Common (${rec.commonUnits.toInt()})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (currentLang == AppLanguage.URDU) "اصل بل کی رقم:" else "Source Bill:",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = BijliStrings.formatRupees(rec.sourceBillAmount),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (currentLang == AppLanguage.URDU) "تقسیم شدہ رقم:" else "Total Allocated:",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = BijliStrings.formatRupees(rec.totalAllocatedAmount),
                                    fontWeight = FontWeight.Bold,
                                    color = BijliGreenPrimary
                                )
                            }

                            if (rec.remainingUnallocatedAmount != 0L) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (currentLang == AppLanguage.URDU) "باقی غیر تقسیم رقم:" else "Remaining / Unallocated:",
                                        fontWeight = FontWeight.Medium,
                                        color = BijliAmberDark
                                    )
                                    Text(
                                        text = BijliStrings.formatRupees(rec.remainingUnallocatedAmount),
                                        fontWeight = FontWeight.Bold,
                                        color = BijliAmberDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Trust label
                            Text(
                                text = "🛡️ " + BijliStrings.trustLabelActualBill(currentLang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // PORTION RESULTS CARDS
                item {
                    Text(
                        text = if (currentLang == AppLanguage.URDU) "ہر پورشن کا بل:" else "Portion Bill Shares:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                itemsIndexed(calculationResult.portionShares) { _, portionShare ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = portionShare.portionName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (portionShare.tenantName.isNotBlank()) {
                                        Text(
                                            text = "👤 ${portionShare.tenantName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = BijliStrings.formatRupees(portionShare.totalAmountRupees),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = BijliGreenPrimary
                                    )
                                    Text(
                                        text = "Effective: Rs. ${String.format("%.1f", portionShare.effectiveRatePerUnit)}/unit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Readings & Units Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Readings: ${String.format("%.0f", portionShare.previousReading)} ➔ ${String.format("%.0f", portionShare.currentReading)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "${BijliStrings.formatUnits(portionShare.unitsUsed)} (${String.format("%.1f", portionShare.consumptionSharePct)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (portionShare.commonUnitsAssigned > 0.001) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "+ Common units share:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BijliAmberDark
                                    )
                                    Text(
                                        text = "+${BijliStrings.formatUnits(portionShare.commonUnitsAssigned)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BijliAmberDark
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // Actions: Explain & Share
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { explainingPortion = portionShare },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.URDU) "حساب تفصیل" else "Breakdown",
                                        fontSize = 13.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        val text = ShareHelper.formatTenantShareText(
                                            portion = portionShare,
                                            monthYear = billMonthInput,
                                            totalBill = calculationResult.sourceBillAmount,
                                            mainUnits = calculationResult.mainMeterUnits,
                                            commonUnits = calculationResult.commonUnits
                                        )
                                        ShareHelper.shareText(context, text, "Share ${portionShare.portionName} Bill")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BijliGreenPrimary),
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.URDU) "بل شیئر کریں" else "Share Bill",
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // BOTTOM MASTER ACTIONS
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val summary = ShareHelper.formatAllPortionsSummaryText(
                                    portions = calculationResult.portionShares,
                                    reconciliation = calculationResult.reconciliation,
                                    monthYear = billMonthInput
                                )
                                ShareHelper.shareText(context, summary, "Share Complete Bill Summary")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BijliGreenDark),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("share_all_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentLang == AppLanguage.URDU) "تمام پورشنز کا خلاصہ شیئر کریں" else "Share All Bills Summary",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val billId = UUID.randomUUID().toString()
                                    val billEntity = SavedBillEntity(
                                        id = billId,
                                        title = if (billMonthInput.isNotBlank()) "Bill: $billMonthInput" else "Quick Split Bill",
                                        billMonthYear = billMonthInput,
                                        sourceBillRupees = calculationResult.sourceBillAmount,
                                        mainMeterPrev = mainPrev,
                                        mainMeterCurr = mainCurr,
                                        mainMeterUnits = calculationResult.mainMeterUnits,
                                        subMetersTotalUnits = calculationResult.subMetersTotalUnits,
                                        commonUnits = calculationResult.commonUnits,
                                        allocationMethod = selectedMethod.name,
                                        commonUnitsHandling = selectedCommonHandling.name,
                                        totalAllocatedRupees = calculationResult.reconciliation.totalAllocatedAmount,
                                        remainingRupees = calculationResult.reconciliation.remainingUnallocatedAmount
                                    )
                                    val shareEntities = calculationResult.portionShares.map { p ->
                                        SavedPortionShareEntity(
                                            billId = billId,
                                            portionName = p.portionName,
                                            tenantName = p.tenantName,
                                            tenantPhone = p.tenantPhone,
                                            previousReading = p.previousReading,
                                            currentReading = p.currentReading,
                                            unitsUsed = p.unitsUsed,
                                            commonUnitsShare = p.commonUnitsAssigned,
                                            totalBillableUnits = p.totalBillableUnits,
                                            allocatedAmountRupees = p.totalAmountRupees
                                        )
                                    }
                                    repository.saveBillWithShares(billEntity, shareEntities)
                                    snackbarHostState.showSnackbar("Bill record successfully saved to History!")
                                    onBillSaved()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_bill_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = BijliStrings.saveBill(currentLang),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Explanation Dialog
    explainingPortion?.let { portion ->
        ExplanationDialog(
            portion = portion,
            reconciliation = calculationResult.reconciliation,
            lang = currentLang,
            onDismiss = { explainingPortion = null }
        )
    }

    // Add Portion Dialog
    if (showAddPortionDialog) {
        var newPortionName by remember { mutableStateOf("Portion ${portionsList.size + 1}") }
        var newTenantName by remember { mutableStateOf("") }
        var newPrev by remember { mutableStateOf("0") }
        var newCurr by remember { mutableStateOf("50") }

        AlertDialog(
            onDismissRequest = { showAddPortionDialog = false },
            title = { Text(if (currentLang == AppLanguage.URDU) "نیا پورشن یا دکان شامل کریں" else "Add New Portion / Shop") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPortionName,
                        onValueChange = { newPortionName = it },
                        label = { Text(if (currentLang == AppLanguage.URDU) "نام (پورشن / فلیٹ / شاپ)" else "Name (e.g. Shop 4, Portion B)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTenantName,
                        onValueChange = { newTenantName = it },
                        label = { Text(if (currentLang == AppLanguage.URDU) "کرایہ دار کا نام (اختیاری)" else "Tenant Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newPrev,
                            onValueChange = { newPrev = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(BijliStrings.previousReading(currentLang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newCurr,
                            onValueChange = { newCurr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(BijliStrings.currentReading(currentLang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = newPrev.toDoubleOrNull() ?: 0.0
                        val c = newCurr.toDoubleOrNull() ?: 0.0
                        portionsList = portionsList + PortionInput(
                            id = UUID.randomUUID().toString(),
                            name = newPortionName.ifBlank { "Portion ${portionsList.size + 1}" },
                            tenantName = newTenantName,
                            previousReading = p,
                            currentReading = c
                        )
                        showAddPortionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BijliGreenPrimary)
                ) {
                    Text(if (currentLang == AppLanguage.URDU) "شامل کریں" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPortionDialog = false }) {
                    Text(if (currentLang == AppLanguage.URDU) "منسوخ" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun CommonOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (selected) BijliGreenPrimary.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = BijliGreenPrimary)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) BijliGreenPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
