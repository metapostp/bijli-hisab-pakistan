package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BijliRepository
import com.example.data.PropertyEntity
import com.example.data.PropertyUnitEntity
import com.example.localization.AppLanguage
import com.example.localization.BijliStrings
import com.example.ui.theme.BijliGreenPrimary
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyManagerScreen(
    repository: BijliRepository,
    currentLang: AppLanguage,
    onBack: () -> Unit,
    onSelectPropertyForSplit: (PropertyEntity, List<PropertyUnitEntity>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val properties by repository.allProperties.collectAsState(initial = emptyList())
    var selectedProperty by remember { mutableStateOf<PropertyEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val unitsForSelected by produceState<List<PropertyUnitEntity>>(
        initialValue = emptyList(),
        key1 = selectedProperty
    ) {
        selectedProperty?.let { p ->
            repository.getUnitsForProperty(p.id).collect { value = it }
        } ?: run { value = emptyList() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = BijliStrings.properties(currentLang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(imageVector = Icons.Default.AddBusiness, contentDescription = "Add Property")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BijliGreenPrimary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Property")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Pakistani Templates Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (currentLang == AppLanguage.URDU) "فوری ٹیمپلیٹس (Quick Setup)" else "Quick Property Templates",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    coroutineScope.launch {
                                        createTemplate(repository, "Al-Rehman Plaza", "PLAZA", 6)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("🏬 Plaza (6 Shops)", fontSize = 12.sp)
                            }
                            FilledTonalButton(
                                onClick = {
                                    coroutineScope.launch {
                                        createTemplate(repository, "Gulberg House", "HOUSE", 3)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("🏠 House (3 Portions)", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Properties list
            if (properties.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Apartment,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (currentLang == AppLanguage.URDU) "ابھی تک کوئی پراپرٹی شامل نہیں کی گئی" else "No properties created yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (currentLang == AppLanguage.URDU) "اوپر دیے گئے ٹیمپلیٹ سے شروع کریں" else "Tap a template above or '+' to create one",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(properties) { prop ->
                    val isSelected = selectedProperty?.id == prop.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProperty = if (isSelected) null else prop },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) BijliGreenPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BijliGreenPrimary)) else CardDefaults.outlinedCardBorder()
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
                                            .clip(CircleShape)
                                            .background(BijliGreenPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (prop.propertyType) {
                                                "PLAZA" -> Icons.Default.Storefront
                                                "HOSTEL" -> Icons.Default.Hotel
                                                else -> Icons.Default.Home
                                            },
                                            contentDescription = null,
                                            tint = BijliGreenPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = prop.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(text = "${prop.propertyType} • ${prop.address.ifBlank { "Pakistan" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(onClick = {
                                    coroutineScope.launch { repository.deleteProperty(prop) }
                                }) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            if (isSelected) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                Text(
                                    text = if (currentLang == AppLanguage.URDU) "شامل دکانیں / پورشنز:" else "Portions / Units in Property:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (unitsForSelected.isEmpty()) {
                                    Text(
                                        text = "No units added yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (u in unitsForSelected) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(text = u.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    if (u.tenantName.isNotBlank()) {
                                                        Text(text = "👤 ${u.tenantName} (${u.tenantPhone.ifBlank { "03XX-XXXXXXX" }})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                Text(
                                                    text = "Last: ${u.lastReading.toInt()} U",
                                                    fontSize = 12.sp,
                                                    color = BijliGreenPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        onSelectPropertyForSplit(prop, unitsForSelected)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BijliGreenPrimary),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLang == AppLanguage.URDU) "اس پراپرٹی کا بل بنائیں" else "Calculate Bill For This Property"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var propName by remember { mutableStateOf("") }
        var propType by remember { mutableStateOf("PLAZA") }
        var address by remember { mutableStateOf("") }
        var unitCount by remember { mutableStateOf("4") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (currentLang == AppLanguage.URDU) "نئی پراپرٹی یا پلازہ" else "Add New Property") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = propName,
                        onValueChange = { propName = it },
                        label = { Text("Property Name (e.g. Model Town Plaza)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / City (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unitCount,
                        onValueChange = { unitCount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Number of Units / Shops") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (propName.isNotBlank()) {
                            val count = unitCount.toIntOrNull() ?: 4
                            coroutineScope.launch {
                                createTemplate(repository, propName, propType, count, address)
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BijliGreenPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private suspend fun createTemplate(
    repo: BijliRepository,
    name: String,
    type: String,
    unitCount: Int,
    address: String = ""
) {
    val propId = UUID.randomUUID().toString()
    val prop = PropertyEntity(
        id = propId,
        name = name,
        propertyType = type,
        address = address
    )
    repo.insertProperty(prop)

    val units = (1..unitCount).map { i ->
        val prefix = if (type == "PLAZA") "Shop" else "Portion"
        PropertyUnitEntity(
            propertyId = propId,
            name = "$prefix $i",
            tenantName = "Tenant $i",
            tenantPhone = "0300-123456$i",
            lastReading = (i * 100).toDouble()
        )
    }
    repo.insertUnits(units)
}
