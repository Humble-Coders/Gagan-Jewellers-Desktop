package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.JewelryAppInitializer
import org.example.project.data.MetalRate
import org.example.project.data.Material
import org.example.project.data.Category
import org.example.project.viewModels.MetalRateViewModel
import org.example.project.viewModels.ProductsViewModel
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Color Palette
private val GoldPrimary = Color(0xFFB8973D)
private val GoldLight = Color(0xFFF5F0E5)
private val BrownAccent = Color(0xFF6D4C41)
private val TextPrimary = Color(0xFF2E2E2E)
private val TextSecondary = Color(0xFF666666)
private val BackgroundLight = Color(0xFFFAFAFA)
private val CardWhite = Color.White
private val ErrorRed = Color(0xFFD32F2F)

@Composable
fun GoldRateScreen(
    onBack: () -> Unit
) {
    val metalRateViewModel = JewelryAppInitializer.getMetalRateViewModel()
    val productsViewModel = JewelryAppInitializer.getViewModel()

    val metalRates by metalRateViewModel.metalRates.collectAsState()
    val materials by productsViewModel.materials
    val loading by metalRateViewModel.loading.collectAsState()
    val error by metalRateViewModel.error.collectAsState()
    val scope = rememberCoroutineScope()

    // Local state for update form
    var selectedMaterialId by remember { mutableStateOf("") }
    var selectedMaterialType by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("") }
    var materialDisplayValue by remember { mutableStateOf("") }
    var materialTypeDisplayValue by remember { mutableStateOf("") }

    // Material suggestions from Firestore
    val materialSuggestions = materials.map { it.name }

    // Material type suggestions - Only 24K for base rate input
    val materialTypeSuggestions = listOf("24K")

    // Stones suggestions from Firestore (names and treat colors as purities)
    val stoneNameSuggestions = productsViewModel.stoneNames.value
    val stonePuritySuggestions = productsViewModel.stoneColors.value

    // Stone UI local state
    var stoneName by remember { mutableStateOf("") }
    var stonePurity by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Header
        MetalRatesHeader(onBack = onBack)

        // Error Message
        if (error != null) {
            ErrorBanner(
                error = error ?: "",
                onDismiss = { metalRateViewModel.clearError() }
            )
        }

        // Main Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Side - Current Rates Display (60%)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CurrentMetalRatesCard(
                    metalRates = metalRates,
                    loading = loading
                )
            }

            // Right Side - Update Rate Section (40%)
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Metal Rate Update Card
                UpdateRateCard(
                    materials = materials,
                    materialSuggestions = materialSuggestions,
                    materialTypeSuggestions = materialTypeSuggestions,
                    materialDisplayValue = materialDisplayValue,
                    materialTypeDisplayValue = materialTypeDisplayValue,
                    rateInput = rateInput,
                    onMaterialDisplayValueChange = { materialDisplayValue = it },
                    onMaterialTypeDisplayValueChange = { materialTypeDisplayValue = it },
                    onRateInputChange = { rateInput = it },
                    onMaterialSelected = { materialId, materialName ->
                        selectedMaterialId = materialId
                        materialDisplayValue = materialName
                    },
                    onMaterialTypeSelected = { materialType ->
                        selectedMaterialType = materialType
                        materialTypeDisplayValue = materialType
                    },
                    onAddNewMaterial = { materialName ->
                        productsViewModel.addMaterialSuggestion(materialName) { newMaterialId ->
                            selectedMaterialId = newMaterialId
                        }
                    },
                    onUpdateRate = {
                        if (selectedMaterialId.isNotEmpty() && selectedMaterialType.isNotEmpty() && rateInput.isNotEmpty()) {
                            val rate = rateInput.toDoubleOrNull()
                            if (rate != null) {
                                val materialName = materials.find { it.id == selectedMaterialId }?.name ?: ""
                                metalRateViewModel.updateMetalRateWithHistory(
                                    selectedMaterialId,
                                    materialName,
                                    selectedMaterialType,
                                    rate
                                )

                                // Clear form
                                selectedMaterialId = ""
                                selectedMaterialType = ""
                                rateInput = ""
                                materialDisplayValue = ""
                                materialTypeDisplayValue = ""
                            }
                        }
                    },
                    loading = loading
                )

                // Stone Rate Update Card
                StoneRateInputCard(
                    materials = materials,
                    stoneName = stoneName,
                    stonePurity = stonePurity,
                    stoneNameSuggestions = stoneNameSuggestions,
                    stonePuritySuggestions = stonePuritySuggestions,
                    onStoneNameChange = { stoneName = it },
                    onStonePurityChange = { stonePurity = it },
                    onAddNewStoneName = { newName ->
                        productsViewModel.addStoneSuggestion(newName, stonePurity.ifBlank { "" }) { }
                        if (materials.none { it.name.equals(newName, ignoreCase = true) }) {
                            productsViewModel.addMaterialSuggestion(newName) { }
                        }
                    },
                    onAddNewStonePurity = { newPurity ->
                        productsViewModel.addStoneSuggestion(stoneName.ifBlank { "" }, newPurity) { }
                    },
                    onUpdate = { rate ->
                        val targetName = stoneName.trim()
                        val targetPurity = stonePurity.trim().ifBlank { "DEFAULT" }
                        if (targetName.isEmpty()) return@StoneRateInputCard
                        scope.launch {
                            val existing = materials.find { it.name.equals(targetName, ignoreCase = true) }
                            if (existing != null) {
                                metalRateViewModel.updateOrCreateMetalRate(existing.id, existing.name, targetPurity, rate)
                            } else {
                                productsViewModel.addMaterialSuggestion(targetName) { newId ->
                                    metalRateViewModel.updateOrCreateMetalRate(newId, targetName, targetPurity, rate)
                                }
                            }
                            stoneName = ""
                            stonePurity = ""
                        }
                    },
                    loading = loading
                )
            }
        }
    }
}

@Composable
private fun MetalRatesHeader(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        color = CardWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(GoldLight, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    "Metal Rates Management",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "Manage your precious metal and stone rates",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GoldLight
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "LIVE RATES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    error: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFEBEE),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Error",
                tint = ErrorRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                error,
                color = ErrorRed,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = ErrorRed, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun CurrentMetalRatesCard(
    metalRates: List<MetalRate>,
    loading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 6.dp,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Current Metal Rates",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Last updated: ${formatDate(System.currentTimeMillis())}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldLight
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "${metalRates.size} Items",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            Spacer(modifier = Modifier.height(20.dp))

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = GoldPrimary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            "Loading rates...",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else if (metalRates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = GoldLight
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "No rates",
                                tint = GoldPrimary,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(40.dp)
                            )
                        }
                        Text(
                            "No metal rates available",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            "Add rates using the forms on the right",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 500.dp)
                ) {
                    items(metalRates) { metalRate ->
                        EnhancedMetalRateRow(metalRate = metalRate)
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedMetalRateRow(metalRate: MetalRate) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GoldLight,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardWhite
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                    )
                }

                Column {
                    Text(
                        "${metalRate.materialName} ${metalRate.materialType}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Base: ${metalRate.karat}K",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardWhite
            ) {
                Text(
                    "₹${formatCurrency(metalRate.pricePerGram)}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun UpdateRateCard(
    materials: List<Material>,
    materialSuggestions: List<String>,
    materialTypeSuggestions: List<String>,
    materialDisplayValue: String,
    materialTypeDisplayValue: String,
    rateInput: String,
    onMaterialDisplayValueChange: (String) -> Unit,
    onMaterialTypeDisplayValueChange: (String) -> Unit,
    onRateInputChange: (String) -> Unit,
    onMaterialSelected: (String, String) -> Unit,
    onMaterialTypeSelected: (String) -> Unit,
    onAddNewMaterial: (String) -> Unit,
    onUpdateRate: () -> Unit,
    loading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoldLight
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Column {
                    Text(
                        "Update Metal Rate",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Enter 24K base rates",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // Material Selection
            AutoCompleteTextField(
                value = materialDisplayValue,
                onValueChange = onMaterialDisplayValueChange,
                onItemSelected = { materialName ->
                    val material = materials.find { it.name == materialName }
                    if (material != null) {
                        onMaterialSelected(material.id, material.name)
                    }
                },
                onAddNew = onAddNewMaterial,
                suggestions = materialSuggestions,
                label = "Material Name",
                placeholder = "Select or type material name",
                maxSuggestions = 5
            )

            // Material Type Selection
            AutoCompleteTextField(
                value = materialTypeDisplayValue,
                onValueChange = onMaterialTypeDisplayValueChange,
                onItemSelected = onMaterialTypeSelected,
                onAddNew = { },
                suggestions = materialTypeSuggestions,
                label = "Material Type",
                placeholder = "Select material type (e.g., 24K)",
                maxSuggestions = 5
            )

            // Rate Input
            OutlinedTextField(
                value = rateInput,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                        onRateInputChange(input)
                    }
                },
                label = { Text("24K Rate per gram (₹)", fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = GoldPrimary,
                    cursorColor = GoldPrimary,
                    focusedLabelColor = GoldPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = onUpdateRate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !loading && materialDisplayValue.isNotEmpty() &&
                        materialTypeDisplayValue.isNotEmpty() && rateInput.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = GoldPrimary,
                    contentColor = CardWhite,
                    disabledBackgroundColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = CardWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Updating...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Update Rate",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StoneRateInputCard(
    materials: List<Material>,
    stoneName: String,
    stonePurity: String,
    stoneNameSuggestions: List<String>,
    stonePuritySuggestions: List<String>,
    onStoneNameChange: (String) -> Unit,
    onStonePurityChange: (String) -> Unit,
    onAddNewStoneName: (String) -> Unit,
    onAddNewStonePurity: (String) -> Unit,
    onUpdate: (Double) -> Unit,
    loading: Boolean
) {
    var stoneRateInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF3E5D7)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = BrownAccent,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Column {
                    Text(
                        "Update Stone Rate",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Set stone name, purity and rate",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // Stone Name autocomplete
            AutoCompleteTextField(
                value = stoneName,
                onValueChange = onStoneNameChange,
                onItemSelected = { onStoneNameChange(it) },
                onAddNew = onAddNewStoneName,
                suggestions = stoneNameSuggestions,
                label = "Stone Name",
                placeholder = "e.g., Diamond",
                maxSuggestions = 6
            )

            // Stone Purity
            AutoCompleteTextField(
                value = stonePurity,
                onValueChange = onStonePurityChange,
                onItemSelected = { onStonePurityChange(it) },
                onAddNew = onAddNewStonePurity,
                suggestions = stonePuritySuggestions,
                label = "Stone Purity",
                placeholder = "e.g., VVS, VS, Colorless",
                maxSuggestions = 6
            )

            OutlinedTextField(
                value = stoneRateInput,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        stoneRateInput = input
                    }
                },
                label = { Text("Stone Rate (₹ per carat)", fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = BrownAccent,
                    cursorColor = BrownAccent,
                    focusedLabelColor = BrownAccent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    val rate = stoneRateInput.toDoubleOrNull()
                    if (rate != null) {
                        onUpdate(rate)
                        stoneRateInput = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !loading && stoneName.isNotBlank() && stoneRateInput.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = BrownAccent,
                    contentColor = CardWhite,
                    disabledBackgroundColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = CardWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Updating...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Update Stone Rate",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

private fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return formatter.format(date)
}