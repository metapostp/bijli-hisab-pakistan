package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.MeterFormatConverter
import com.example.billing.MeterReadingFormat
import com.example.billing.PortionInput
import com.example.billing.SubMeterConverterItem
import com.example.localization.AppLanguage
import com.example.localization.BijliStrings
import com.example.ui.ShareHelper
import com.example.ui.theme.BijliAmber
import com.example.ui.theme.BijliAmberDark
import com.example.ui.theme.BijliGreenPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterConverterScreen(
    currentLang: AppLanguage,
    onBack: () -> Unit,
    onTransferToSplitBill: ((List<PortionInput>) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Multi Sub-Meters, 1 = Format Guide & Quick Calc

    // Pre-populated default sub-meters showcasing varied formats
    var subMeters by remember {
        mutableStateOf(
            listOf(
                SubMeterConverterItem(
                    name = "Ground Floor (Analog Dial)",
                    format = MeterReadingFormat.ANALOG_RED_DIAL,
                    previousReading = "14250",
                    currentReading = "14580",
                    redDigitAsInteger = true
                ),
                SubMeterConverterItem(
                    name = "Commercial Shop (CT 10x)",
                    format = MeterReadingFormat.CT_MULTIPLIER,
                    previousReading = "320",
                    currentReading = "355",
                    multiplier = 10.0
                ),
                SubMeterConverterItem(
                    name = "Upper Flat (Digital LCD)",
                    format = MeterReadingFormat.STANDARD_DIGITAL,
                    previousReading = "4500",
                    currentReading = "4685"
                ),
                SubMeterConverterItem(
                    name = "Water Pump (Rollover)",
                    format = MeterReadingFormat.DIAL_ROLLOVER,
                    previousReading = "99850",
                    currentReading = "00120",
                    rolloverCapacity = 100000.0
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = BijliStrings.meterConverter(currentLang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentLang == AppLanguage.URDU) "سب میٹر ریڈنگز نارملائزیشن" else "Sub-Meters Normalization",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("meter_converter_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0 && subMeters.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val summary = ShareHelper.formatSubMetersConversionSummary(subMeters)
                        ShareHelper.shareText(
                            context = context,
                            text = summary,
                            title = "Share Converted Sub-Meters"
                        )
                    },
                    icon = { Icon(Icons.Default.Share, contentDescription = "Share Converted Units") },
                    text = {
                        Text(
                            text = if (currentLang == AppLanguage.URDU) "شیئر کریں" else "Share Summary",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("converter_share_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("tab_multi_submeters"),
                    text = {
                        Text(
                            text = if (currentLang == AppLanguage.URDU) "متعدد سب میٹرز (${subMeters.size})" else "Multiple Sub-Meters (${subMeters.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.ElectricMeter, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("tab_format_guide"),
                    text = {
                        Text(
                            text = if (currentLang == AppLanguage.URDU) "رہنمائی اور کوئیک کیلیبریٹر" else "Format Guide & Types",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                MultiSubMeterBatchList(
                    subMeters = subMeters,
                    currentLang = currentLang,
                    onUpdateSubMeters = { subMeters = it },
                    onCopySummary = {
                        val summary = ShareHelper.formatSubMetersConversionSummary(subMeters)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Meter Conversion", summary))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Converted units copied to clipboard!")
                        }
                    },
                    onTransferToSplitBill = {
                        if (onTransferToSplitBill != null) {
                            val portions = subMeters.mapIndexed { idx, item ->
                                val conv = item.conversion
                                PortionInput(
                                    id = (idx + 1).toString(),
                                    name = item.name,
                                    previousReading = item.prevDouble,
                                    currentReading = item.currDouble,
                                    format = item.format,
                                    formatMultiplier = item.multiplier,
                                    rolloverCapacity = item.rolloverCapacity,
                                    redDigitDecimal = item.redDigitAsInteger
                                )
                            }
                            onTransferToSplitBill(portions)
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Sub-meters prepared for bill calculation!")
                            }
                        }
                    }
                )
            } else {
                MeterFormatGuideView(currentLang = currentLang)
            }
        }
    }
}

@Composable
fun MultiSubMeterBatchList(
    subMeters: List<SubMeterConverterItem>,
    currentLang: AppLanguage,
    onUpdateSubMeters: (List<SubMeterConverterItem>) -> Unit,
    onCopySummary: () -> Unit,
    onTransferToSplitBill: () -> Unit
) {
    val totalRaw = subMeters.sumOf { it.conversion.rawAdvance.coerceAtLeast(0.0) }
    val totalConverted = subMeters.sumOf { it.conversion.convertedUnitsKwh }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // OVERALL CONVERSION SUMMARY CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("conversion_summary_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentLang == AppLanguage.URDU) "کل تبدیل شدہ یونٹس" else "Total Converted Units",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "${subMeters.size} Sub-Meters",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = BijliStrings.formatUnits(totalConverted),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Raw Dial Sum: ${String.format("%.1f", totalRaw)} units",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        if (kotlin.math.abs(totalConverted - totalRaw) > 0.1) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BijliAmberDark.copy(alpha = 0.2f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = if (totalConverted > totalRaw) "+${String.format("%.1f", totalConverted - totalRaw)} (CT ratio)"
                                    else "${String.format("%.1f", totalConverted - totalRaw)} (Red decimal)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCopySummary,
                            modifier = Modifier.weight(1f).testTag("copy_converted_summary_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(BijliStrings.copyConvertedSummary(currentLang), fontSize = 12.sp)
                        }

                        Button(
                            onClick = onTransferToSplitBill,
                            modifier = Modifier.weight(1f).testTag("transfer_to_split_bill_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(BijliStrings.transferToSplitBill(currentLang), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // SECTION HEADER & ADD SUBMETER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentLang == AppLanguage.URDU) "سب میٹرز کی فہرست" else "Sub-Meters Readings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = {
                        val nextIndex = subMeters.size + 1
                        onUpdateSubMeters(
                            subMeters + SubMeterConverterItem(
                                name = "Sub-Meter $nextIndex",
                                format = MeterReadingFormat.STANDARD_DIGITAL
                            )
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_submeter_converter_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (currentLang == AppLanguage.URDU) "سب میٹر شامل کریں" else "Add Sub-Meter")
                }
            }
        }

        // SUBMETERS LIST
        itemsIndexed(subMeters) { index, item ->
            SubMeterConverterCard(
                item = item,
                currentLang = currentLang,
                canDelete = subMeters.size > 1,
                onUpdate = { updatedItem ->
                    onUpdateSubMeters(
                        subMeters.toMutableList().also { it[index] = updatedItem }
                    )
                },
                onDelete = {
                    if (subMeters.size > 1) {
                        onUpdateSubMeters(subMeters.filterIndexed { i, _ -> i != index })
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubMeterConverterCard(
    item: SubMeterConverterItem,
    currentLang: AppLanguage,
    canDelete: Boolean,
    onUpdate: (SubMeterConverterItem) -> Unit,
    onDelete: () -> Unit
) {
    var expandedFormatMenu by remember { mutableStateOf(false) }
    val conv = item.conversion

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("submeter_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Name and Format Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("Sub-Meter Name") }
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Remove Sub-Meter",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // FORMAT SELECTOR
            Text(
                text = BijliStrings.subMeterFormat(currentLang),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Format Dropdown Box
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedFormatMenu = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (item.format) {
                                MeterReadingFormat.STANDARD_DIGITAL -> Icons.Default.Speed
                                MeterReadingFormat.ANALOG_RED_DIAL -> Icons.Default.Adjust
                                MeterReadingFormat.CT_MULTIPLIER -> Icons.Default.ElectricBolt
                                MeterReadingFormat.DIAL_ROLLOVER -> Icons.Default.RestartAlt
                                MeterReadingFormat.PULSE_IMPULSE -> Icons.Default.Sensors
                                MeterReadingFormat.WATT_HOURS -> Icons.Default.OfflineBolt
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (currentLang == AppLanguage.URDU) item.format.titleUr else item.format.titleEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (currentLang == AppLanguage.URDU) item.format.descriptionUr else item.format.descriptionEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                DropdownMenu(
                    expanded = expandedFormatMenu,
                    onDismissRequest = { expandedFormatMenu = false }
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
                                onUpdate(item.copy(format = fmt))
                                expandedFormatMenu = false
                            }
                        )
                    }
                }
            }

            // FORMAT-SPECIFIC CONTROLS
            when (item.format) {
                MeterReadingFormat.CT_MULTIPLIER -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Current Transformer (CT) Multiplier Ratio:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(5.0, 10.0, 20.0, 40.0, 50.0).forEach { ratio ->
                            FilterChip(
                                selected = item.multiplier == ratio,
                                onClick = { onUpdate(item.copy(multiplier = ratio)) },
                                label = { Text("${ratio.toInt()}x", fontSize = 11.sp) }
                            )
                        }
                    }
                }
                MeterReadingFormat.ANALOG_RED_DIAL -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFEBEE),
                        border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (currentLang == AppLanguage.URDU) "سرخ ہندسہ (Tenths / 0.1):" else "Red Dial Wheel (0.1 kWh):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB71C1C)
                                )
                                Text(
                                    text = if (currentLang == AppLanguage.URDU) "اگر ریڈنگ 12345[6] بغیر اعشاریہ لکھی ہے تو یہ خود بخود 10 سے تقسیم کر کے اصل یونٹس نکالے گا۔"
                                    else "Divided by 10 automatically if raw numbers were written without decimal point.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                        }
                    }
                }
                MeterReadingFormat.DIAL_ROLLOVER -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Rollover Capacity:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = item.rolloverCapacity == 10000.0,
                            onClick = { onUpdate(item.copy(rolloverCapacity = 10000.0)) },
                            label = { Text("4 Digits (9,999)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = item.rolloverCapacity == 100000.0,
                            onClick = { onUpdate(item.copy(rolloverCapacity = 100000.0)) },
                            label = { Text("5 Digits (99,999)", fontSize = 11.sp) }
                        )
                    }
                }
                MeterReadingFormat.PULSE_IMPULSE -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Impulses per kWh (imp/kWh):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1000.0, 1600.0, 3200.0).forEach { imp ->
                            FilterChip(
                                selected = item.multiplier == imp,
                                onClick = { onUpdate(item.copy(multiplier = imp)) },
                                label = { Text("${imp.toInt()} imp", fontSize = 11.sp) }
                            )
                        }
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PREVIOUS & CURRENT READINGS INPUTS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = item.previousReading,
                    onValueChange = { onUpdate(item.copy(previousReading = it)) },
                    label = { Text(BijliStrings.previousReading(currentLang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = item.currentReading,
                    onValueChange = { onUpdate(item.copy(currentReading = it)) },
                    label = { Text(BijliStrings.currentReading(currentLang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // LIVE CONVERSION BADGE & EXPLANATION
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = BijliStrings.convertedUnits(currentLang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Raw advance: ${String.format("%.1f", conv.rawAdvance)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        Text(
                            text = BijliStrings.formatUnits(conv.convertedUnitsKwh),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (currentLang == AppLanguage.URDU) conv.explanationUr else conv.explanationEn,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun MeterFormatGuideView(currentLang: AppLanguage) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (currentLang == AppLanguage.URDU) "پاکستانی سب میٹرز کی اقسام اور درست طریقہ" else "Sub-Meter Types & Reading Formats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentLang == AppLanguage.URDU)
                            "مختلف ماڈلز کے سب میٹرز میں ریڈنگ پڑھنے کا طریقہ مختلف ہوتا ہے۔ غلط پڑھنے سے بل 10 گنا تک زیادہ ہو سکتا ہے۔"
                        else
                            "Sub-meters installed across Pakistani rental portions and shops come in different display types. Misreading them is the #1 cause of billing disputes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // TYPE 1: ANALOG RED DIAL WHEEL
        item {
            FormatGuideCard(
                icon = Icons.Default.Adjust,
                title = if (currentLang == AppLanguage.URDU) "1. اینالاگ مکینیکل ڈائل (سرخ ہندسہ 0.1)" else "1. Analog Dial with Red Decimal Wheel",
                subtitle = if (currentLang == AppLanguage.URDU) "اکثر کرائے دار سرخ ہندسے کو پورا یونٹ سمجھ لیتے ہیں" else "The red rightmost wheel represents tenths (0.1 kWh)",
                details = if (currentLang == AppLanguage.URDU)
                    "پرانے ماڈلز میں دائیں طرف آخری سرخ ہندسہ اعشاریہ (tenths) ہوتا ہے۔ اگر میٹر پر [1425] سیاہ اور [6] سرخ ہے، تو اصل ریڈنگ 1425.6 ہے، نہ کہ 14256۔ اگر آپ اسے پورا ہندسہ لکھیں گے تو بل 10 گنا زیادہ آئے گا۔ ہمارا کنورٹر اسے خود بخود 10 سے تقسیم کر کے درست کرتا ہے۔"
                else
                    "On older mechanical roller meters (MicroTech, Kangshu, Siemens), the rightmost digit wheel is red or framed in red. It represents 1/10th of a unit (0.1 kWh). Entering 12345[6] as 123456 without decimal creates a massive 10x overcharge. The converter automatically normalizes this.",
                badge = "Analog 0.1x",
                accentColor = Color(0xFFC62828)
            )
        }

        // TYPE 2: CT MULTIPLIER
        item {
            FormatGuideCard(
                icon = Icons.Default.ElectricBolt,
                title = if (currentLang == AppLanguage.URDU) "2. سی ٹی تناسب / ملٹی پلائر (CT Multiplier)" else "2. Current Transformer (CT) Multipliers",
                subtitle = if (currentLang == AppLanguage.URDU) "کمرشل شاپس، واٹر پمپس یا 3 فیز لوڈز" else "Heavy loads & commercial sub-meters with CT coils",
                details = if (currentLang == AppLanguage.URDU)
                    "بھاری لوڈز یا موٹرز کے سب میٹرز میں سی ٹی کوائل لگی ہوتی ہے۔ میٹر کی ڈائل پر فرق صرف اشارہ ہوتا ہے، جسے سی ٹی ریشو (مثلاً 10x یا 20x) سے ضرب دے کر اصل استعمال شدہ یونٹس حاصل کیے جاتے ہیں۔"
                else
                    "Heavy-duty or 3-phase sub-meters measure current through an external Current Transformer. If your CT ratio is 100:5 (20x) or 50:5 (10x), the dial advance must be multiplied by that factor to determine actual billable consumption.",
                badge = "CT Ratio (5x-50x)",
                accentColor = BijliGreenPrimary
            )
        }

        // TYPE 3: METER ROLLOVER
        item {
            FormatGuideCard(
                icon = Icons.Default.RestartAlt,
                title = if (currentLang == AppLanguage.URDU) "3. میٹر رول اوور / زیرو ری سیٹ (Rollover)" else "3. Meter Register Rollover / Capacity Overflow",
                subtitle = if (currentLang == AppLanguage.URDU) "جب میٹر 9999 یا 99999 پر پہنچ کر صفر ہو جائے" else "When a 4 or 5-digit meter exceeds capacity and resets to 0000",
                details = if (currentLang == AppLanguage.URDU)
                    "اگر پچھلی ریڈنگ 99850 تھی اور نئی ریڈنگ 00120 ہے، تو عام تفریق منفی آئے گی۔ لیکن کنورٹر سمجھداری سے رول اوور کیپسٹی (100,000) کو مدنظر رکھ کر اصل استعمال (270 یونٹس) نکالتا ہے۔"
                else
                    "When a mechanical meter passes 99,999 (5 digits) or 9,999 (4 digits), the next reading shows 00050. Standard subtraction gives a negative value, but our rollover calculation correctly yields (Capacity - Previous) + Current units.",
                badge = "Rollover Auto-Detect",
                accentColor = BijliAmberDark
            )
        }

        // TYPE 4: DIGITAL LCD & PULSE COUNTERS
        item {
            FormatGuideCard(
                icon = Icons.Default.Sensors,
                title = if (currentLang == AppLanguage.URDU) "4. ڈیجیٹل ایل سی ڈی اور پلس کاؤنٹرز" else "4. Digital LCD & LED Pulse Meters",
                subtitle = if (currentLang == AppLanguage.URDU) "جدید الیکٹرانک سب میٹرز" else "Modern sub-meters displaying direct kWh or impulse count",
                details = if (currentLang == AppLanguage.URDU)
                    "جدید ڈیجیٹل میٹرز پر بڑی اسکرین پر کلو واٹ آور (kWh) براہ راست آتا ہے۔ کچھ پلس کاؤنٹرز 1600 یا 3200 پلس فی یونٹ پر کام کرتے ہیں۔"
                else
                    "Standard electronic sub-meters show direct kWh readouts. Pulse sub-meters count blinking impulses (e.g. 1600 imp/kWh or 3200 imp/kWh) and require division by the impulse constant.",
                badge = "Digital 1:1",
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun FormatGuideCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    details: String,
    badge: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
