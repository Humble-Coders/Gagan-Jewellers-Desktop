package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.JewelryAppInitializer
import org.example.project.data.Product
import org.example.project.data.calculateRateForKarat
import org.example.project.viewModels.ProductsViewModel
import org.example.project.data.MetalRatesManager
import org.example.project.viewModels.MetalRateViewModel
import org.example.project.ui.AutoCompleteTextField
import org.example.project.data.InventoryItem
import org.example.project.data.InventoryStatus
import kotlinx.coroutines.launch

// Helper function to extract karat from material type
fun extractKaratFromMaterialType(materialType: String): Int {
    val karatRegex = Regex("""(\d+)K""", RegexOption.IGNORE_CASE)
    val match = karatRegex.find(materialType)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: 22 // Default to 22K
}

// Helper function to find fallback rate for material
fun findFallbackRateForMaterial(metalRateViewModel: MetalRateViewModel, materialId: String, targetKarat: Int): Double {
    return try {
        // Get all metal rates for this material
        val allRates = metalRateViewModel.metalRates.value.filter {
            it.materialId == materialId && it.isActive
        }

        if (allRates.isNotEmpty()) {
            // Find the closest karat rate or use the first available rate
            val bestRate = allRates.minByOrNull {
                kotlin.math.abs(it.karat - targetKarat)
            } ?: allRates.first()

            // Calculate rate for target karat based on the found rate
            val calculatedRate = bestRate.calculateRateForKarat(targetKarat)
            println("🔄 Found fallback rate: ${bestRate.materialType} ${bestRate.karat}K -> ${targetKarat}K = $calculatedRate")
            calculatedRate
        } else {
            println("⚠️ No rates found for material: $materialId")
            0.0
        }
    } catch (e: Exception) {
        println("❌ Error finding fallback rate: ${e.message}")
        0.0
    }
}

@Composable
fun AddEditProductScreen(
    viewModel: ProductsViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onEditBarcode: (String) -> Unit = {},
    isEditing: Boolean = false
) {
    val product = viewModel.currentProduct.value ?: Product()
    val categories by remember { viewModel.categories }
    val materials by remember { viewModel.materials }
    val metalRateViewModel = JewelryAppInitializer.getMetalRateViewModel()
    val metalRates by metalRateViewModel.metalRates.collectAsState()
    val imageLoader = JewelryAppInitializer.getImageLoader()
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Form state
    var autoGenerateId by remember { mutableStateOf(true) }
    var customProductId by remember { mutableStateOf("") }
    var generatedBarcodes by remember { mutableStateOf(emptyList<String>()) }
    var name by remember { mutableStateOf(product.name) }
    var description by remember { mutableStateOf(product.description ?: "") }

    // Sample suggestions for autocomplete
    val descriptionSuggestions = listOf(
        "Gold ring with diamond",
        "Silver necklace with pendant",
        "Platinum earrings",
        "Gold bracelet with stones",
        "Diamond engagement ring",
        "Pearl necklace",
        "Gold chain",
        "Silver bangles",
        "Platinum wedding ring",
        "Gold pendant with chain"
    )

    // Material suggestions from Firestore
    val materialSuggestions = materials.map { it.name }
    
    // Debug logging
    LaunchedEffect(materials) {
        println("🔍 DEBUG: Materials loaded: ${materials.size} items")
        materials.forEach { material ->
            println("   - ${material.name} (ID: ${material.id})")
        }
    }

    // Material type suggestions
    val materialTypeSuggestions = listOf(
        "22K",
        "18K",
        "14K",
        "10K",
        "24K",
        "925 Sterling",
        "950 Platinum",
        "900 Platinum",
        "999 Pure",
        "750 Gold"
    )

    // Category suggestions from Firestore
    val categorySuggestions = categories.map { it.name }
    
    // Debug logging for categories
    LaunchedEffect(categories) {
        println("🔍 DEBUG: Categories loaded: ${categories.size} items")
        categories.forEach { category ->
            println("   - ${category.name} (ID: ${category.id})")
        }
    }
    var materialRate by remember { mutableStateOf(if (product.price > 0) product.price.toString() else "") }
    var isMaterialRateEditable by remember { mutableStateOf(true) } // Always editable by default
    var categoryId by remember { mutableStateOf(product.categoryId) }
    var materialId by remember { mutableStateOf(product.materialId ?: "") }
    var materialType by remember { mutableStateOf(product.materialType ?: "") }

    // Display values for autocomplete fields
    var materialDisplayValue by remember { mutableStateOf(materials.find { it.id == materialId }?.name ?: "") }
    var materialTypeDisplayValue by remember { mutableStateOf(materialType) }
    var categoryDisplayValue by remember { mutableStateOf(categories.find { it.id == categoryId }?.name ?: "") }
    var quantity by remember { mutableStateOf(if (product.quantity > 0) product.quantity else 1) }
    var barcodeDigits by remember { mutableStateOf("12") }
    var totalWeight by remember { mutableStateOf(if (product.totalWeight > 0) product.totalWeight.toString() else "") }
    var defaultMakingRate by remember { mutableStateOf(if (product.defaultMakingRate > 0) product.defaultMakingRate.toString() else "") }
    var isOtherThanGold by remember { mutableStateOf(product.isOtherThanGold) }
    var lessWeight by remember { mutableStateOf(if (product.lessWeight > 0) product.lessWeight.toString() else "") }
    var hasStones by remember { mutableStateOf(product.hasStones) }
    var stoneName by remember { mutableStateOf(product.stoneName) }
    var stoneQuantity by remember { mutableStateOf(if (product.stoneQuantity > 0) product.stoneQuantity.toString() else "") }
    var stoneRate by remember { mutableStateOf(if (product.stoneRate > 0) product.stoneRate.toString() else "") }
    var cwWeight by remember { mutableStateOf(if (product.cwWeight > 0) product.cwWeight.toString() else "") }
    var vaCharges by remember { mutableStateOf(if (product.vaCharges > 0) product.vaCharges.toString() else "") }
    var available by remember { mutableStateOf(product.available) }
    var featured by remember { mutableStateOf(product.featured) }
    var images by remember { mutableStateOf(product.images) }
    var hasCustomPrice by remember { mutableStateOf(product.hasCustomPrice) }
    var customPrice by remember { mutableStateOf(if (product.customPrice > 0) product.customPrice.toString() else "") }

    // Show/visibility toggles for fields
    var showName by remember { mutableStateOf(product.show.name) }
    var showDescription by remember { mutableStateOf(product.show.description) }
    var showCategory by remember { mutableStateOf(product.show.category) }
    var showMaterial by remember { mutableStateOf(product.show.material) }
    var showMaterialType by remember { mutableStateOf(product.show.materialType) }
    var showQuantity by remember { mutableStateOf(product.show.quantity) }
    var showTotalWeight by remember { mutableStateOf(product.show.totalWeight) }
    var showPrice by remember { mutableStateOf(product.show.price) }
    var showDefaultMakingRate by remember { mutableStateOf(product.show.defaultMakingRate) }
    var showVaCharges by remember { mutableStateOf(product.show.vaCharges) }
    var showIsOtherThanGold by remember { mutableStateOf(product.show.isOtherThanGold) }
    var showLessWeight by remember { mutableStateOf(product.show.lessWeight) }
    var showHasStones by remember { mutableStateOf(product.show.hasStones) }
    var showStoneName by remember { mutableStateOf(product.show.stoneName) }
    var showStoneQuantity by remember { mutableStateOf(product.show.stoneQuantity) }
    var showStoneAmount by remember { mutableStateOf(product.show.stoneAmount) }
    var showStoneRate by remember { mutableStateOf(product.show.stoneRate) }
    var showCwWeight by remember { mutableStateOf(product.show.cwWeight) }
    var showNetWeight by remember { mutableStateOf(product.show.netWeight) }
    var showTotalProductCost by remember { mutableStateOf(product.show.totalProductCost) }
    var showCustomPrice by remember { mutableStateOf(product.show.customPrice) }
    var showImages by remember { mutableStateOf(product.show.images) }
    var showAvailable by remember { mutableStateOf(product.show.available) }
    var showFeatured by remember { mutableStateOf(product.show.featured) }

    // Calculated values
    val netWeight = remember(totalWeight, lessWeight) {
        val total = totalWeight.toDoubleOrNull() ?: 0.0
        val less = lessWeight.toDoubleOrNull() ?: 0.0
        total - less
    }

    val netWeightAfterCW = remember(netWeight, cwWeight) {
        val net = netWeight
        val cw = cwWeight.toDoubleOrNull() ?: 0.0
        net - cw
    }

    val totalProductCost = remember(totalWeight, lessWeight, defaultMakingRate, vaCharges, cwWeight, materialRate, stoneRate, stoneQuantity, hasStones) {
        val totalWeightValue = totalWeight.toDoubleOrNull() ?: 0.0
        val lessWeightValue = lessWeight.toDoubleOrNull() ?: 0.0
        val netWeight = (totalWeightValue - lessWeightValue).coerceAtLeast(0.0)
        val makingRate = defaultMakingRate.toDoubleOrNull() ?: 0.0
        val va = vaCharges.toDoubleOrNull() ?: 0.0
        val cw = cwWeight.toDoubleOrNull() ?: 0.0

        // Material cost (net weight × material rate)
        val materialCost = netWeight * (materialRate.toDoubleOrNull() ?: 0.0)

        // Making charges (net weight × making rate)
        val makingCharges = netWeight * makingRate

        // Stone amount (if has stones)
        val stoneAmount = if (hasStones) {
            val stoneRateValue = stoneRate.toDoubleOrNull() ?: 0.0
            val stoneQuantityValue = stoneQuantity.toDoubleOrNull() ?: 0.0
            if (cw > 0 && stoneRateValue > 0 && stoneQuantityValue > 0) {
                cw * stoneRateValue * stoneQuantityValue
            } else 0.0
        } else 0.0

        // Total = Material Cost + Making Charges + Stone Amount + VA Charges
        materialCost + makingCharges + stoneAmount + va
    }

    // Auto-calculate material rate when material/type or rates change
    LaunchedEffect(materialId, materialType, metalRates) {
        if (materialId.isNotEmpty() && materialType.isNotEmpty()) {
            // Extract karat from material type (e.g., "18K" -> 18, "22K" -> 22)
            val karat = extractKaratFromMaterialType(materialType)

            val calculatedRate = metalRateViewModel.calculateRateForMaterial(
                materialId,
                materialType,
                karat
            )
            if (calculatedRate > 0) {
                materialRate = String.format("%.2f", calculatedRate)
                println("💰 Auto-calculated material rate: $calculatedRate for $materialType ($karat K)")
            } else {
                // Try to find any rate for this material and calculate based on karat
                val fallbackRate = findFallbackRateForMaterial(metalRateViewModel, materialId, karat)
                if (fallbackRate > 0) {
                    materialRate = String.format("%.2f", fallbackRate)
                    println("💰 Fallback calculated material rate: $fallbackRate for $materialType ($karat K)")
                } else {
                    // Final fallback: use global MetalRatesManager (live gold/silver rates)
                    val selectedMaterialName = materials.find { it.id == materialId }?.name ?: ""
                    val globalRates = MetalRatesManager.metalRates.value
                    val globalRate = when {
                        selectedMaterialName.contains("gold", ignoreCase = true) || materialType.contains("K", ignoreCase = true) -> {
                            globalRates.getGoldRateForKarat(karat)
                        }
                        selectedMaterialName.contains("silver", ignoreCase = true) || materialType.contains("999") -> {
                            globalRates.getSilverRateForPurity(999)
                        }
                        else -> 0.0
                    }
                    if (globalRate > 0) {
                        materialRate = String.format("%.2f", globalRate)
                        println("🌐 Global rates fallback used: $globalRate for $materialType ($karat K)")
                    } else {
                        // If no rate found, clear the field to show user needs to enter manually
                        if (materialRate.isEmpty()) {
                            materialRate = ""
                            println("⚠️ No rate found for $materialType, please enter manually")
                        }
                    }
                }
            }
        }
    }

    // Auto-calculate stone amount when stone fields change
    LaunchedEffect(cwWeight, stoneRate, stoneQuantity) {
        if (hasStones && cwWeight.isNotEmpty() && stoneRate.isNotEmpty() && stoneQuantity.isNotEmpty()) {
            val cwWeightValue = cwWeight.toDoubleOrNull() ?: 0.0
            val stoneRateValue = stoneRate.toDoubleOrNull() ?: 0.0
            val stoneQuantityValue = stoneQuantity.toDoubleOrNull() ?: 0.0

            if (cwWeightValue > 0 && stoneRateValue > 0 && stoneQuantityValue > 0) {
                val calculatedStoneAmount = cwWeightValue * stoneRateValue * stoneQuantityValue
                println("💎 Auto-calculated stone amount: $calculatedStoneAmount (CW: $cwWeightValue × Rate: $stoneRateValue × Qty: $stoneQuantityValue)")
            }
        }
    }

    // Auto-disable show featured when featured is false
    LaunchedEffect(featured) {
        if (!featured) {
            showFeatured = false
        }
    }

    // Validation state
    var nameError by remember { mutableStateOf(false) }
    var materialRateError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }
    var materialError by remember { mutableStateOf(false) }
    var customIdError by remember { mutableStateOf(false) }
    var barcodeError by remember { mutableStateOf("") }

    // Expanded dropdown states

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isEditing) {
                        IconButton(onClick = onCancel) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colors.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            if (isEditing) "Edit Product" else "Add New Product",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D2D2D)
                        )
                        Text(
                            if (isEditing) "Update product information" else "Create a new jewelry item",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }


            // Product Identification (Inventory-based)
            SectionCard(title = "Product Identification") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isEditing) {
                        // Show existing inventory information in edit mode
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFFF5F5F5),
                            elevation = 0.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Product Information",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Product ID: ${product.id}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colors.primary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Note: Product information is separate from inventory items. Inventory items with barcodes are managed separately.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    } else {
                        // Show inventory creation options for new products
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = autoGenerateId,
                                    onClick = {
                                        autoGenerateId = true
                                        customIdError = false
                                        barcodeError = ""
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colors.primary
                                    )
                                )
                                Text("Auto Generate Inventory Items", modifier = Modifier.padding(start = 8.dp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = !autoGenerateId,
                                    onClick = {
                                        autoGenerateId = false
                                        customIdError = false
                                        barcodeError = ""
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colors.primary
                                    )
                                )
                                Text("Single Inventory Item", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }

                    if (!isEditing) {
                        if (autoGenerateId) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color(0xFFFFF8E1),
                                elevation = 0.dp,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.padding(start = 12.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            NumericSelectAllField(
                                                value = barcodeDigits,
                                                onValueChange = { v ->
                                                    val filtered = v.filter { it.isDigit() }.take(2)
                                                    barcodeDigits = filtered
                                                    barcodeError = ""
                                                },
                                                label = "Barcode digits",
                                                modifier = Modifier.width(160.dp),
                                                maxLength = 2
                                            )
                                            StyledTextField(
                                                value = if (quantity > 0) quantity.toString() else "",
                                                onValueChange = {
                                                    val digitsOnly = it.filter { ch -> ch.isDigit() }
                                                    if (digitsOnly.isEmpty()) {
                                                        quantity = 0
                                                        barcodeError = ""
                                                    } else {
                                                        val newQuantity = digitsOnly.toIntOrNull()
                                                        if (newQuantity != null && newQuantity > 0) {
                                                            quantity = newQuantity
                                                            barcodeError = ""
                                                        }
                                                    }
                                                },
                                                label = "Quantity",
                                                modifier = Modifier.width(160.dp),
                                                keyboardType = KeyboardType.Number
                                            )
                                            Button(onClick = {
                                                val digits = barcodeDigits.toIntOrNull() ?: 12
                                                val q = quantity.coerceAtLeast(1)
                                                if (digits !in 8..13) {
                                                    barcodeError = "Barcode digits must be between 8 and 13"
                                                } else {
                                                    scope.launch {
                                                        println("Generating $q barcodes with $digits digits")
                                                        generatedBarcodes = viewModel.generateUniqueBarcodes(q, digits)
                                                        println("Generated barcodes: $generatedBarcodes")
                                                        barcodeError = ""
                                                    }
                                                }
                                            }) { Text("Generate Barcodes") }
                                        }
                                        if (barcodeError.isNotEmpty()) {
                                            Text(
                                                barcodeError,
                                                color = MaterialTheme.colors.error,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                        if (generatedBarcodes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Generated Barcodes:", fontSize = 12.sp, color = Color.Gray)
                                            Column {
                                                generatedBarcodes.forEach { code ->
                                                    Text(
                                                        code,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colors.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            StyledTextField(
                                value = customProductId,
                                onValueChange = {
                                    customProductId = it
                                    customIdError = it.isEmpty()
                                },
                                label = "Barcode",
                                isError = customIdError,
                                errorMessage = "Barcode is required"
                            )
                        }
                    }
                }
            }

            // Basic Information Section
            SectionCard(title = "Basic Information") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StyledTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = it.isEmpty()
                        },
                        label = "Product Name",
                        isError = nameError,
                        errorMessage = "Product name is required",
                        placeholder = "e.g., Gold Necklace"
                    )

                    AutoCompleteTextField(
                        value = description,
                        onValueChange = { description = it },
                        onItemSelected = { selectedDescription ->
                            description = selectedDescription
                        },
                        onAddNew = { newDescription ->
                            description = newDescription
                            // You could add this to a persistent suggestions list here
                            println("New description added: $newDescription")
                        },
                        suggestions = descriptionSuggestions,
                        label = "Description",
                        placeholder = "Enter product details...",
                        singleLine = false,
                        maxSuggestions = 5
                    )
                }
            }

            // Material & Specifications
            SectionCard(title = "Material & Specifications") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            AutoCompleteTextField(
                                value = materialDisplayValue,
                                onValueChange = { materialDisplayValue = it },
                                onItemSelected = { selectedMaterial ->
                                    materialDisplayValue = selectedMaterial
                                    // Find material ID from the selected name
                                    val foundMaterial = materials.find { it.name == selectedMaterial }
                                    if (foundMaterial != null) {
                                        materialId = foundMaterial.id
                                        materialType = ""
                                        materialTypeDisplayValue = ""
                                        materialError = false
                                    }
                                },
                                onAddNew = { newMaterial ->
                                    viewModel.addMaterialSuggestion(newMaterial) { newId ->
                                        materialDisplayValue = newMaterial
                                        materialId = newId
                                        materialType = ""
                                        materialTypeDisplayValue = ""
                                        materialError = false
                                        println("✅ New material saved: $newMaterial ($newId)")
                                    }
                                },
                                suggestions = materialSuggestions,
                                label = "Material",
                                placeholder = "Select or enter material...",
                                maxSuggestions = 5
                            )
                            if (materialError) {
                                Text(
                                    text = "Material is required",
                                    color = MaterialTheme.colors.error,
                                    style = MaterialTheme.typography.caption,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }
                    }

                    if (materialId.isNotEmpty()) {
                        val selectedMaterial = materials.find { it.id == materialId }
                        val materialTypes = selectedMaterial?.types ?: emptyList()

                        // Combine predefined types with suggestions
                        val allMaterialTypes = (materialTypes + materialTypeSuggestions).distinct()

                        if (allMaterialTypes.isNotEmpty()) {
                            AutoCompleteTextField(
                                value = materialTypeDisplayValue,
                                onValueChange = { materialTypeDisplayValue = it },
                                onItemSelected = { selectedType ->
                                    materialTypeDisplayValue = selectedType
                                    materialType = selectedType
                                },
                                onAddNew = { newType ->
                                    if (materialId.isNotBlank()) {
                                        viewModel.addMaterialTypeSuggestion(materialId, newType) {
                                            materialTypeDisplayValue = newType
                                            materialType = newType
                                            println("✅ New material type saved: $newType for $materialId")
                                        }
                                    } else {
                                        materialTypeDisplayValue = newType
                                        materialType = newType
                                    }
                                },
                                suggestions = allMaterialTypes,
                                label = "Material Type",
                                placeholder = "Select or enter material type...",
                                maxSuggestions = 5
                            )
                        }
                    }

                    AutoCompleteTextField(
                        value = categoryDisplayValue,
                        onValueChange = { categoryDisplayValue = it },
                        onItemSelected = { selectedCategory ->
                            categoryDisplayValue = selectedCategory
                            // Find category ID from the selected name
                            val foundCategory = categories.find { it.name == selectedCategory }
                            if (foundCategory != null) {
                                categoryId = foundCategory.id
                                categoryError = false
                            }
                        },
                        onAddNew = { newCategory ->
                            viewModel.addCategorySuggestion(newCategory) { newId ->
                                categoryDisplayValue = newCategory
                                categoryId = newId
                                categoryError = false
                                println("✅ New category saved: $newCategory ($newId)")
                            }
                        },
                        suggestions = categorySuggestions,
                        label = "Category",
                        placeholder = "Select or enter category...",
                        maxSuggestions = 5
                    )
                    if (categoryError) {
                        Text(
                            text = "Category is required",
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            }

            // Weight & Inventory
            SectionCard(title = "Weight & Inventory") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StyledTextField(
                            value = totalWeight,
                            onValueChange = { totalWeight = it },
                            label = "Total Weight",
                            placeholder = "0.00",
                            suffix = "grams",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )

                        StyledTextField(
                            value = if (quantity > 0) quantity.toString() else "",
                            onValueChange = { input ->
                                // Allow only digits
                                val digitsOnly = input.filter { char -> char.isDigit() }
                                if (digitsOnly.isEmpty()) {
                                    quantity = 0
                                } else {
                                    val newQuantity = digitsOnly.toIntOrNull()
                                    if (newQuantity != null && newQuantity > 0) {
                                        quantity = newQuantity
                                    }
                                }
                            },
                            label = "Quantity",
                            placeholder = "1",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Pricing Information
            SectionCard(title = "Pricing Information") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            StyledTextField(
                                value = materialRate,
                                onValueChange = {
                                    materialRate = it
                                    materialRateError = if (it.isNotEmpty()) {
                                        try {
                                            it.toDouble() <= 0
                                        } catch (e: NumberFormatException) {
                                            true
                                        }
                                    } else false
                                },
                                label = "Material Rate",
                                placeholder = "0.00",
                                prefix = "₹",
                                suffix = "/gram",
                                keyboardType = KeyboardType.Decimal,
                                isError = materialRateError,
                                errorMessage = "Valid material rate required"
                            )

                            // Auto-calculation indicator
                            if (materialRate.isNotEmpty() && materialId.isNotEmpty() && materialType.isNotEmpty()) {
                                Text(
                                    text = "Auto-calculated from rates (editable)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colors.primary,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                // Re-calculate material rate from current material and type
                                if (materialId.isNotEmpty() && materialType.isNotEmpty()) {
                                    val karat = extractKaratFromMaterialType(materialType)
                                    val calculatedRate = metalRateViewModel.calculateRateForMaterial(
                                        materialId,
                                        materialType,
                                        karat
                                    )
                                    if (calculatedRate > 0) {
                                        materialRate = String.format("%.2f", calculatedRate)
                                        println("💰 Re-calculated material rate: $calculatedRate for $materialType ($karat K)")
                                    } else {
                                        // Try fallback calculation
                                        val fallbackRate = findFallbackRateForMaterial(metalRateViewModel, materialId, karat)
                                        if (fallbackRate > 0) {
                                            materialRate = String.format("%.2f", fallbackRate)
                                            println("💰 Fallback re-calculated material rate: $fallbackRate for $materialType ($karat K)")
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Re-calculate rate from current rates",
                                tint = MaterialTheme.colors.primary
                            )
                        }

                        StyledTextField(
                            value = defaultMakingRate,
                            onValueChange = { defaultMakingRate = it },
                            label = "Making Rate",
                            placeholder = "0.00",
                            prefix = "₹",
                            suffix = "/gram",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // VA Charges
                    StyledTextField(
                        value = vaCharges,
                        onValueChange = { vaCharges = it },
                        label = "VA Charges",
                        placeholder = "0.00",
                        prefix = "₹",
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }

            // Additional Components
            SectionCard(title = "Additional Components") {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Other Than Gold Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Other Than Gold",
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            Text(
                                "Include non-gold components",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Switch(
                            checked = isOtherThanGold,
                            onCheckedChange = { isOtherThanGold = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.primary,
                                checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                            )
                        )
                    }

                    if (isOtherThanGold) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFFFAFAFA),
                            elevation = 0.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                StyledTextField(
                                    value = lessWeight,
                                    onValueChange = { lessWeight = it },
                                    label = "Less Weight",
                                    placeholder = "0.00",
                                    suffix = "grams",
                                    keyboardType = KeyboardType.Decimal
                                )

                                Divider(color = Color(0xFFE0E0E0))

                                // Has Stones Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Has Stones",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            "Include gemstones or diamonds",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = hasStones,
                                        onCheckedChange = { hasStones = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colors.primary,
                                            checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                if (hasStones) {
                                    // Stone Name (autocomplete from stones collection)
                                    val stoneNameSuggestions = viewModel.stoneNames.value

                                    AutoCompleteTextField(
                                        value = stoneName,
                                        onValueChange = { stoneName = it },
                                        onItemSelected = { selected -> stoneName = selected },
                                        onAddNew = { newName ->
                                            // Add stone name
                                            viewModel.addStoneSuggestion(newName, "") { }
                                        },
                                        suggestions = stoneNameSuggestions,
                                        label = "Stone Name",
                                        placeholder = "e.g., Diamond",
                                        maxSuggestions = 6
                                    )

                                    StyledTextField(
                                        value = stoneQuantity,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                                stoneQuantity = input
                                            }
                                        },
                                        label = "Stone Quantity",
                                        placeholder = "0.00",
                                        keyboardType = KeyboardType.Decimal
                                    )

                                    StyledTextField(
                                        value = stoneRate,
                                        onValueChange = { stoneRate = it },
                                        label = "Stone Rate",
                                        placeholder = "0.00",
                                        prefix = "₹",
                                        keyboardType = KeyboardType.Decimal
                                    )

                                    StyledTextField(
                                        value = cwWeight,
                                        onValueChange = { cwWeight = it },
                                        label = "CW Weight",
                                        placeholder = "0.00",
                                        suffix = "carats",
                                        keyboardType = KeyboardType.Decimal
                                    )

                                    // Display calculated stone amount
                                    val calculatedStoneAmount = if (hasStones && cwWeight.isNotEmpty() && stoneRate.isNotEmpty() && stoneQuantity.isNotEmpty()) {
                                        val cwWeightValue = cwWeight.toDoubleOrNull() ?: 0.0
                                        val stoneRateValue = stoneRate.toDoubleOrNull() ?: 0.0
                                        val stoneQuantityValue = stoneQuantity.toDoubleOrNull() ?: 0.0
                                        if (cwWeightValue > 0 && stoneRateValue > 0 && stoneQuantityValue > 0) {
                                            cwWeightValue * stoneRateValue * stoneQuantityValue
                                        } else 0.0
                                    } else 0.0

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        backgroundColor = Color(0xFFF5F5F5),
                                        elevation = 0.dp,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                "Stone Amount Calculation",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF2E7D32)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Formula: CW_WT × STONE_RATE × QTY",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "₹${String.format("%.2f", calculatedStoneAmount)}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colors.primary
                                            )
                                        }
                                    }
                                }

                                // Net Weight display
                                Divider(color = Color(0xFFE0E0E0))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Net Weight",
                                        fontSize = 14.sp,
                                        color = Color(0xFF555555)
                                    )
                                    Text(
                                        "${String.format("%.2f", netWeight)} g",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2D2D2D)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Calculated Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFFFF8E1),
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Price & Cost Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )

                    Divider(color = MaterialTheme.colors.primary.copy(alpha = 0.3f))

                    // Material Cost (Net Weight = Total - Less)
                    val materialRateValue = materialRate.toDoubleOrNull() ?: 0.0
                    val totalWeightValue = totalWeight.toDoubleOrNull() ?: 0.0
                    val lessWeightValue = lessWeight.toDoubleOrNull() ?: 0.0
                    val netWeightValue = (totalWeightValue - lessWeightValue).coerceAtLeast(0.0)
                    val materialCost = netWeightValue * materialRateValue
                    if (materialCost > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Material Cost ((${String.format("%.2f", totalWeightValue)}g − ${String.format("%.2f", lessWeightValue)}g) × ₹${String.format("%.2f", materialRateValue)})",
                                fontSize = 14.sp,
                                color = Color(0xFF555555)
                            )
                            Text(
                                "₹${String.format("%.2f", materialCost)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF555555)
                            )
                        }
                    }

                    // Making Charges (Net Weight = Total - Less)
                    val makingRateValue = defaultMakingRate.toDoubleOrNull() ?: 0.0
                    val makingCharges = netWeightValue * makingRateValue
                    if (makingCharges > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Making Charges ((${String.format("%.2f", totalWeightValue)}g − ${String.format("%.2f", lessWeightValue)}g) × ₹${String.format("%.2f", makingRateValue)})",
                                fontSize = 14.sp,
                                color = Color(0xFF555555)
                            )
                            Text(
                                "₹${String.format("%.2f", makingCharges)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF555555)
                            )
                        }
                    }

                    // Stone Amount
                    val calculatedStoneAmount = if (hasStones && cwWeight.isNotEmpty() && stoneRate.isNotEmpty() && stoneQuantity.isNotEmpty()) {
                        val cwWeightValue = cwWeight.toDoubleOrNull() ?: 0.0
                        val stoneRateValue = stoneRate.toDoubleOrNull() ?: 0.0
                        val stoneQuantityValue = stoneQuantity.toDoubleOrNull() ?: 0.0
                        if (cwWeightValue > 0 && stoneRateValue > 0 && stoneQuantityValue > 0) {
                            cwWeightValue * stoneRateValue * stoneQuantityValue
                        } else 0.0
                    } else 0.0

                    if (hasStones && calculatedStoneAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Stone Amount (${String.format("%.2f", cwWeight.toDoubleOrNull() ?: 0.0)}ct × ₹${String.format("%.2f", stoneRate.toDoubleOrNull() ?: 0.0)} × ${String.format("%.2f", stoneQuantity.toDoubleOrNull() ?: 0.0)})",
                                fontSize = 14.sp,
                                color = Color(0xFF555555)
                            )
                            Text(
                                "₹${String.format("%.2f", calculatedStoneAmount)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF555555)
                            )
                        }
                    }

                    // VA Charges
                    val vaChargesValue = vaCharges.toDoubleOrNull() ?: 0.0
                    if (vaChargesValue > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "VA Charges",
                                fontSize = 14.sp,
                                color = Color(0xFF555555)
                            )
                            Text(
                                "₹${String.format("%.2f", vaChargesValue)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF555555)
                            )
                        }
                    }

                    // Divider before total
                    if (materialCost > 0 || makingCharges > 0 || calculatedStoneAmount > 0 || vaChargesValue > 0) {
                        Divider(color = MaterialTheme.colors.primary.copy(alpha = 0.3f))
                    }

                    // Total Product Cost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total Product Cost",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D2D2D)
                        )
                        Text(
                            "₹${String.format("%.2f", totalProductCost)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }

                    Divider(color = MaterialTheme.colors.primary.copy(alpha = 0.3f))

                    // Custom Price Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = hasCustomPrice,
                            onCheckedChange = { checked ->
                                hasCustomPrice = checked
                                if (checked) {
                                    // When custom price is enabled, disable total product cost visibility
                                    showTotalProductCost = false
                                    showCustomPrice = true
                                } else {
                                    // When custom price is disabled, enable total product cost visibility
                                    showTotalProductCost = true
                                    showCustomPrice = false
                                }
                            }
                        )
                        Text(
                            "Use Custom Price",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2D2D2D)
                        )
                    }

                    if (hasCustomPrice) {
                        StyledTextField(
                            value = customPrice,
                            onValueChange = { customPrice = it },
                            label = "Custom Price",
                            placeholder = "0.00",
                            prefix = "₹",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Product Status
            SectionCard(title = "Product Status") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatusToggle(
                        title = "Available for Sale",
                        description = "Product can be sold to customers",
                        checked = available,
                        onCheckedChange = { available = it }
                    )

                    Divider(color = Color(0xFFE0E0E0))

                    StatusToggle(
                        title = "Featured Product",
                        description = "Highlight on homepage and promotions",
                        checked = featured,
                        onCheckedChange = { featured = it }
                    )
                }
            }

            // Field Visibility Configuration
            SectionCard(title = "Field Visibility") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusToggle("Show Name", "Display product name", showName) { showName = it }
                    StatusToggle("Show Description", "Display description", showDescription) { showDescription = it }
                    StatusToggle("Show Category", "Display category", showCategory) { showCategory = it }
                    StatusToggle("Show Material", "Display material", showMaterial) { showMaterial = it }
                    StatusToggle("Show Material Type", "Display material type", showMaterialType) { showMaterialType = it }
                    StatusToggle("Show Quantity", "Display quantity", showQuantity) { showQuantity = it }
                    StatusToggle("Show Total Weight", "Display total weight", showTotalWeight) { showTotalWeight = it }
                    StatusToggle("Show Price", "Display price/material rate", showPrice) { showPrice = it }
                    StatusToggle("Show Making Rate", "Display making rate", showDefaultMakingRate) { showDefaultMakingRate = it }
                    StatusToggle("Show VA Charges", "Display VA charges", showVaCharges) { showVaCharges = it }
                    StatusToggle("Show 'Other Than Gold'", "Display other-than-gold inputs", showIsOtherThanGold) { showIsOtherThanGold = it }
                    StatusToggle("Show Less Weight", "Display less weight", showLessWeight) { showLessWeight = it }
                    StatusToggle("Show Has Stones", "Display stones toggle", showHasStones) { showHasStones = it }
                    StatusToggle("Show Stone Name", "Display stone name", showStoneName) { showStoneName = it }
                    StatusToggle("Show Stone Quantity", "Display stone quantity", showStoneQuantity) { showStoneQuantity = it }
                    StatusToggle("Show Stone Amount", "Display calculated stone amount", showStoneAmount) { showStoneAmount = it }
                    StatusToggle("Show Stone Rate", "Display stone rate", showStoneRate) { showStoneRate = it }
                    StatusToggle("Show CW Weight", "Display CW weight", showCwWeight) { showCwWeight = it }
                    StatusToggle("Show Net Weight", "Display net weight", showNetWeight) { showNetWeight = it }
                    StatusToggle("Show Total Product Cost", "Display total product cost", showTotalProductCost) { showTotalProductCost = it }
                    StatusToggle("Show Custom Price", "Display custom price field", showCustomPrice) { showCustomPrice = it }
                    StatusToggle("Show Images", "Display product images section", showImages) { showImages = it }
                    StatusToggle("Show Available", "Display available status", showAvailable) { showAvailable = it }
                    StatusToggle("Show Featured", "Display featured status", showFeatured) { showFeatured = it }
                }
            }

            // Product Images
            SectionCard(title = "Product Images") {
                if (showImages) {
                    ProductImageManager(
                        existingImages = images,
                        onImagesChanged = { updatedImages -> images = updatedImages },
                        imageLoader = imageLoader,
                        productId = product.id
                    )
                } else {
                    Text("Images hidden by visibility settings", color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(end = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }

                Button(
                    onClick = {
                        println("=== ADD PRODUCT BUTTON CLICKED ===")
                        println("Form state - Name: '$name', Category: '$categoryId', Material: '$materialId', MaterialRate: '$materialRate'")
                        println("Auto generate: $autoGenerateId, Custom barcode: '$customProductId', Quantity: $quantity")

                        // Validate required fields
                        nameError = name.isEmpty()
                        categoryError = categoryId.isEmpty()
                        materialError = materialId.isEmpty()

                        // Material rate validation
                        materialRateError = if (materialRate.isNotEmpty()) {
                            try {
                                materialRate.toDouble() <= 0
                            } catch (e: NumberFormatException) {
                                true
                            }
                        } else false

                        customIdError = !autoGenerateId && customProductId.isEmpty()

                        println("Validation results - Name: $nameError, Category: $categoryError, Material: $materialError, MaterialRate: $materialRateError, CustomId: $customIdError")

                        if (!nameError && !materialRateError && !categoryError && !materialError && !customIdError) {
                            println("✅ Basic validation passed, checking inventory requirements...")

                            // Skip inventory validation when editing existing products
                            if (isEditing) {
                                println("✅ Editing mode: skipping inventory validation - updating product only")
                            } else if (autoGenerateId) {
                                println("Auto-generate mode: checking quantity and barcode requirements")
                                if (quantity < 1) {
                                    barcodeError = "Quantity must be at least 1"
                                    println("❌ Validation failed: quantity < 1")
                                    return@Button
                                }
                                val digits = barcodeDigits.toIntOrNull() ?: 12
                                if (digits !in 8..13) {
                                    barcodeError = "Barcode digits must be between 8 and 13"
                                    println("❌ Validation failed: invalid barcode digits ($digits)")
                                    return@Button
                                }
                                if (generatedBarcodes.isEmpty()) {
                                    barcodeError = "Please generate barcodes first by clicking 'Generate Barcodes' button"
                                    println("❌ Validation failed: no barcodes generated")
                                    return@Button
                                }
                                println("✅ Auto-generate validation passed - Quantity: $quantity, Digits: $digits, Generated barcodes: ${generatedBarcodes.size}")
                            } else {
                                println("✅ Custom barcode mode validation passed")
                            }

                            println("🚀 All validation passed, creating product and inventory...")
                            isSaving = true

                            println("📝 Creating product object with form data...")
                            println("Form data - Description: '$description', MaterialId: '$materialId', MaterialType: '$materialType', CustomProductId: '$customProductId'")

                            val updatedProduct = Product(
                                id = if (isEditing) product.id else "",
                                name = name,
                                description = description,
                                price = materialRate.toDoubleOrNull() ?: 0.0,
                                categoryId = categoryId,
                                materialId = materialId,
                                materialType = materialType,
                                quantity = 1,
                                totalWeight = totalWeight.toDoubleOrNull() ?: 0.0,
                                defaultMakingRate = defaultMakingRate.toDoubleOrNull() ?: 0.0,
                                isOtherThanGold = isOtherThanGold,
                                lessWeight = lessWeight.toDoubleOrNull() ?: 0.0,
                                hasStones = hasStones,
                                stoneName = if (hasStones) stoneName else "",
                                stoneQuantity = if (hasStones) (stoneQuantity.toDoubleOrNull() ?: 0.0) else 0.0,
                                stoneRate = if (hasStones) (stoneRate.toDoubleOrNull() ?: 0.0) else 0.0,
                                cwWeight = cwWeight.toDoubleOrNull() ?: 0.0,
                                stoneAmount = if (hasStones) {
                                    val cwWeightValue = cwWeight.toDoubleOrNull() ?: 0.0
                                    val stoneRateValue = stoneRate.toDoubleOrNull() ?: 0.0
                                    val stoneQuantityValue = stoneQuantity.toDoubleOrNull() ?: 0.0
                                    if (cwWeightValue > 0 && stoneRateValue > 0 && stoneQuantityValue > 0) {
                                        cwWeightValue * stoneRateValue * stoneQuantityValue
                                    } else 0.0
                                } else 0.0,
                                vaCharges = vaCharges.toDoubleOrNull() ?: 0.0,
                                netWeight = netWeight,
                                totalProductCost = totalProductCost,
                                hasCustomPrice = hasCustomPrice,
                                customPrice = customPrice.toDoubleOrNull() ?: 0.0,
                                available = available,
                                featured = featured,
                                images = images,
                                autoGenerateId = autoGenerateId,
                                customProductId = customProductId,
                                createdAt = product.createdAt,
                                show = org.example.project.data.ProductShowConfig(
                                    name = showName,
                                    description = showDescription,
                                    category = showCategory,
                                    material = showMaterial,
                                    materialType = showMaterialType,
                                    quantity = showQuantity,
                                    totalWeight = showTotalWeight,
                                    price = showPrice,
                                    defaultMakingRate = showDefaultMakingRate,
                                    vaCharges = showVaCharges,
                                    isOtherThanGold = showIsOtherThanGold,
                                    lessWeight = showLessWeight,
                                    hasStones = showHasStones,
                                    stoneName = showStoneName,
                                    stoneQuantity = showStoneQuantity,
                                    stoneRate = showStoneRate,
                                    cwWeight = showCwWeight,
                                    stoneAmount = showStoneAmount,
                                    netWeight = showNetWeight,
                                    totalProductCost = showTotalProductCost,
                                    customPrice = showCustomPrice,
                                    images = showImages,
                                    available = showAvailable,
                                    featured = showFeatured
                                )
                            )

                            println("✅ Product object created successfully: ${updatedProduct.name} (ID: ${updatedProduct.id})")

                            if (isEditing) {
                                // For editing, use bulk update to update all products with same commonId
                                println("🔄 Editing mode: updating product and all products with same commonId")
                                scope.launch {
                                    try {
                                        if (updatedProduct != null) {
                                            println("📤 Updating product and related products...")
                                            println("   - Product ID: ${updatedProduct.id}")
                                            println("   - Common ID: ${updatedProduct.commonId}")
                                            println("   - Product Name: ${updatedProduct.name}")
                                            
                                            // Use the new bulk update method
                                            viewModel.updateProductsWithCommonId(updatedProduct)
                                            println("✅ Product and related products updated successfully")
                                            isSaving = false
                                            onSave()
                                        } else {
                                            println("❌ updatedProduct is null for update!")
                                            isSaving = false
                                        }
                                    } catch (e: Exception) {
                                        println("💥 Error updating product: ${e.message}")
                                        e.printStackTrace()
                                        isSaving = false
                                    }
                                }
                            } else if (autoGenerateId) {
                                println("🔄 Using auto-generate mode with ${generatedBarcodes.size} inventory items")
                                println("Generated barcodes: $generatedBarcodes")
                                scope.launch {
                                    try {
                                        if (updatedProduct != null) {
                                            println("📤 Creating product and inventory items...")
                                            // First create the product
                                            val productId = viewModel.addProductSync(updatedProduct)
                                            if (productId != null) {
                                                println("✅ Product created with ID: $productId")
                                                
                                                // Then create inventory items for each barcode
                                                for (barcode in generatedBarcodes) {
                                                    val inventoryItem = InventoryItem(
                                                        productId = productId,
                                                        barcodeId = barcode,
                                                        status = InventoryStatus.AVAILABLE,
                                                        location = "",
                                                        notes = ""
                                                    )
                                                    val inventoryId = viewModel.addInventoryItem(inventoryItem)
                                                    if (inventoryId != null) {
                                                        println("✅ Inventory item created for barcode: $barcode")
                                                    } else {
                                                        println("❌ Failed to create inventory item for barcode: $barcode")
                                                    }
                                                }
                                                
                                                println("✅ Product and inventory items added successfully")
                                                isSaving = false
                                                onSave()
                                            } else {
                                                println("❌ Failed to create product")
                                                isSaving = false
                                            }
                                        } else {
                                            println("❌ updatedProduct is null!")
                                        }
                                    } catch (e: Exception) {
                                        println("💥 Error adding product and inventory: ${e.message}")
                                        e.printStackTrace()
                                        isSaving = false
                                    }
                                }
                            } else {
                                // Custom barcode mode for new products only
                                println("🔄 Using custom barcode mode: '$customProductId'")
                                println("📝 Final product for custom barcode: ${updatedProduct?.name}")
                                scope.launch {
                                    try {
                                        println("📤 Adding single product and inventory item...")
                                        if (updatedProduct != null) {
                                            // First create the product
                                            val productId = viewModel.addProductSync(updatedProduct)
                                            if (productId != null) {
                                                println("✅ Product created with ID: $productId")
                                                
                                                // Then create inventory item for the custom barcode
                                                val inventoryItem = InventoryItem(
                                                    productId = productId,
                                                    barcodeId = customProductId,
                                                    status = InventoryStatus.AVAILABLE,
                                                    location = "",
                                                    notes = ""
                                                )
                                                val inventoryId = viewModel.addInventoryItem(inventoryItem)
                                                if (inventoryId != null) {
                                                    println("✅ Inventory item created for barcode: $customProductId")
                                                } else {
                                                    println("❌ Failed to create inventory item for barcode: $customProductId")
                                                }
                                                
                                                println("✅ Product and inventory item added successfully")
                                                isSaving = false
                                                onSave()
                                            } else {
                                                println("❌ Failed to create product")
                                                isSaving = false
                                            }
                                        } else {
                                            println("❌ updatedProduct is null for add!")
                                            isSaving = false
                                        }
                                    } catch (e: Exception) {
                                        println("Error adding product and inventory: ${e.message}")
                                        e.printStackTrace()
                                        isSaving = false
                                    }
                                }
                            }
                        } else {
                            println("Validation failed")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White
                    ),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.elevation(4.dp)
                ) {
                    Text(
                        if (isEditing) "Save Changes" else if (isSaving) "Saving..." else "Add Product",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    prefix: String = "",
    suffix: String = "",
    isError: Boolean = false,
    errorMessage: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    readOnly: Boolean = false
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = Color.Gray.copy(alpha = 0.5f)) },
            leadingIcon = if (prefix.isNotEmpty()) {
                { Text(prefix, color = Color.Gray, modifier = Modifier.padding(start = 8.dp)) }
            } else null,
            trailingIcon = if (suffix.isNotEmpty()) {
                { Text(suffix, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            enabled = enabled,
            readOnly = readOnly,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                errorBorderColor = MaterialTheme.colors.error,
                cursorColor = MaterialTheme.colors.primary,
                focusedLabelColor = MaterialTheme.colors.primary
            ),
            shape = RoundedCornerShape(8.dp)
        )
        if (isError && errorMessage.isNotEmpty()) {
            Text(
                errorMessage,
                color = MaterialTheme.colors.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun NumericSelectAllField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLength: Int = Int.MAX_VALUE
) {
    var tfv by remember { mutableStateOf(TextFieldValue(text = value)) }

    LaunchedEffect(value) {
        if (value != tfv.text) tfv = tfv.copy(text = value)
    }

    OutlinedTextField(
        value = tfv,
        onValueChange = { newV ->
            val filtered = newV.text.filter { it.isDigit() }.take(maxLength)
            tfv = TextFieldValue(
                text = filtered,
                selection = TextRange(filtered.length)
            )
            onValueChange(filtered)
        },
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (state.isFocused) {
                    tfv = tfv.copy(selection = TextRange(0, tfv.text.length))
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Delete) {
                    tfv = TextFieldValue(text = "", selection = TextRange.Zero)
                    onValueChange("")
                    true
                } else false
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = Color(0xFFE0E0E0),
            cursorColor = MaterialTheme.colors.primary,
            focusedLabelColor = MaterialTheme.colors.primary
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun StyledDropdown(
    value: String,
    label: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { onExpandChange(true) }) {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Select $label",
                        tint = if (isError) MaterialTheme.colors.error else Color.Gray
                    )
                }
            },
            readOnly = true,
            isError = isError,
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                errorBorderColor = MaterialTheme.colors.error,
                disabledTextColor = Color.Black,
                disabledBorderColor = Color(0xFFE0E0E0),
                disabledLabelColor = Color.Gray
            ),
            shape = RoundedCornerShape(8.dp)
        )
        if (isError && errorMessage.isNotEmpty()) {
            Text(
                errorMessage,
                color = MaterialTheme.colors.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
