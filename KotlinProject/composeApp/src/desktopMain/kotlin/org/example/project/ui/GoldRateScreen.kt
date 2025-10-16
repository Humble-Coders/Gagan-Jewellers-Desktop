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
            .background(Color(0xFFFAFAFA))
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        MetalRatesHeader(onBack = onBack)

        // Main Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Side - Current Rates Display (50%)
            Column(
                modifier = Modifier.weight(0.5f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CurrentMetalRatesCard(
                    metalRates = metalRates,
                    loading = loading
                )
            }

            // Right Side - Update Rate Section (50%)
            Column(
                modifier = Modifier.weight(0.5f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Stone Rate update (per carat) similar to gold/silver
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
                        // ensure material exists with this name for rates storage
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
                            // Update the selected material ID when a new material is created
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
            }
        }
        
        // Error Message
        if (error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    error ?: "",
                    color = Color(0xFFD32F2F),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { 
                        metalRateViewModel.clearError()
                    }
                ) {
                    Text("Dismiss", color = Color(0xFFD32F2F))
                }
            }
        }
    }
}

@Composable
private fun MetalRatesHeader(onBack: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(0.dp),
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF2E2E2E),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                "Metal Rates Management",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E2E2E)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "CURRENT RATES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB8973D),
                letterSpacing = 1.sp
            )
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
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Current Metal Rates",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            Text(
                "Last updated: ${formatDate(System.currentTimeMillis())}",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFB8973D),
                        strokeWidth = 2.dp
                    )
                }
            } else if (metalRates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "No rates",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "No metal rates available",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                        Text(
                            "Add rates using the form on the right",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(metalRates) { metalRate ->
                        MetalRateRow(metalRate = metalRate)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetalRateRow(metalRate: MetalRate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFB8973D).copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "${metalRate.materialName} ${metalRate.materialType}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )
            Text(
                "Base: ${metalRate.karat}K",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }

        Text(
            "₹${formatCurrency(metalRate.pricePerGram)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB8973D)
        )
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
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Update Metal Rate",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            Text(
                "Enter 24K base rates only. Other karats will be calculated automatically.",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )

            // Material Selection
            AutoCompleteTextField(
                value = materialDisplayValue,
                onValueChange = onMaterialDisplayValueChange,
                onItemSelected = { materialName ->
                    // Find material ID from name
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
                onAddNew = { /* Material types are predefined */ },
                suggestions = materialTypeSuggestions,
                label = "Material Type",
                placeholder = "Select material type (e.g., 22K, 18K)",
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
                label = { Text("24K Rate per gram (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFB8973D),
                    cursorColor = Color(0xFFB8973D)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = onUpdateRate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                enabled = !loading && materialDisplayValue.isNotEmpty() && 
                         materialTypeDisplayValue.isNotEmpty() && rateInput.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFB8973D),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Updating...", fontSize = 14.sp)
                } else {
                    Text(
                        "Update Rate",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
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
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Update Stone Rate",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            Text("Set stone name, purity and rate", fontSize = 12.sp, color = Color(0xFF666666))

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

            // Stone Purity (we reuse stones' color as purity tag)
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
                label = { Text("Stone Rate (₹ per carat)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF6D4C41),
                    cursorColor = Color(0xFF6D4C41)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    val rate = stoneRateInput.toDoubleOrNull()
                    if (rate != null) onUpdate(rate)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                enabled = !loading && stoneName.isNotBlank() && stoneRateInput.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF6D4C41),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Updating...", fontSize = 14.sp)
                } else {
                    Text(
                        "Update Stone Rate",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}