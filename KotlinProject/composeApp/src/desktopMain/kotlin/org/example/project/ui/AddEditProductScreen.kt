package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.JewelryAppInitializer
import org.example.project.data.Product
import org.example.project.data.calculateRateForKarat
import org.example.project.utils.CurrencyFormatter
import org.example.project.viewModels.ProductsViewModel
import org.example.project.data.MetalRatesManager
import org.example.project.viewModels.MetalRateViewModel
import org.example.project.ui.AutoCompleteTextField
import org.example.project.data.InventoryItem
import org.example.project.data.ThemedCollection
import org.example.project.data.ThemedCollectionRepository
import org.example.project.data.FirestoreThemedCollectionRepository
import org.example.project.data.ProductMaterial
import org.example.project.data.Material
import kotlinx.coroutines.launch
import org.example.project.ui.ProductPriceCalculatorComposable
import org.example.project.ui.ProductPriceInputs
import org.example.project.ui.calculateProductPrice

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
    val enterMovesFocusModifier = remember(focusManager) {
        Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                focusManager.moveFocus(FocusDirection.Down)
                true
            } else {
                false
            }
        }
    }

    // Collection state
    val collectionRepository = remember { FirestoreThemedCollectionRepository(JewelryAppInitializer.getFirestore()) }
    var collections by remember { mutableStateOf<List<ThemedCollection>>(emptyList()) }
    var selectedCollection by remember { mutableStateOf<ThemedCollection?>(null) }
    var collectionSearchQuery by remember { mutableStateOf("") }
    var showCollectionDropdown by remember { mutableStateOf(false) }

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
    var materialName by remember { mutableStateOf(product.materialName ?: "") }
    var showAddMaterialDialog by remember { mutableStateOf(false) }

    // Display values for autocomplete fields
    var materialDisplayValue by remember { mutableStateOf(materialName.ifEmpty { materials.find { it.id == materialId }?.name ?: "" }) }
    var materialTypeDisplayValue by remember { mutableStateOf(materialType) }
    var categoryDisplayValue by remember { mutableStateOf(categories.find { it.id == categoryId }?.name ?: "") }
    var quantity by remember { mutableStateOf(if (product.quantity > 0) product.quantity else 1) }
    var barcodeDigits by remember { mutableStateOf("12") }
    var totalWeight by remember { mutableStateOf(if (product.totalWeight > 0) product.totalWeight.toString() else "") }
    var hasStones by remember { mutableStateOf(product.hasStones) }
    // Manage stones array
    var productStones by remember { mutableStateOf(product.stones) }
    // Use first stone from array for backward compatibility with UI fields
    val firstStone = productStones.firstOrNull()
    var stoneName by remember { mutableStateOf(firstStone?.name ?: "") }
    var stonePurity by remember { mutableStateOf(firstStone?.purity ?: "") }
    var stoneQuantity by remember { mutableStateOf(if ((firstStone?.quantity ?: 0.0) > 0) firstStone?.quantity.toString() ?: "" else "") }
    var stoneRate by remember { mutableStateOf(if ((firstStone?.rate ?: 0.0) > 0) firstStone?.rate.toString() ?: "" else "") }
    var cwWeight by remember { mutableStateOf(if ((firstStone?.weight ?: 0.0) > 0) firstStone?.weight.toString() ?: "" else "") }
    var materialWeight by remember { mutableStateOf(if (product.materialWeight > 0) product.materialWeight.toString() else "") }
    var stoneWeight by remember { mutableStateOf(if (product.stoneWeight > 0) product.stoneWeight.toString() else "") }
    var makingPercent by remember { mutableStateOf(if (product.makingPercent > 0) product.makingPercent.toString() else "") }
    var labourCharges by remember { mutableStateOf(if (product.labourCharges > 0) product.labourCharges.toString() else "") }
    var effectiveWeight by remember { mutableStateOf(if (product.effectiveWeight > 0) product.effectiveWeight.toString() else "") }
    var effectiveMetalWeight by remember { mutableStateOf(if (product.effectiveMetalWeight > 0) product.effectiveMetalWeight.toString() else "") }
    var labourRate by remember { mutableStateOf(if (product.labourRate > 0) product.labourRate.toString() else "") }
    var available by remember { mutableStateOf(product.available) }
    var featured by remember { mutableStateOf(product.featured) }
    var isCollectionProduct by remember { mutableStateOf(product.isCollectionProduct) }
    var collectionId by remember { mutableStateOf(product.collectionId) }
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
    var showHasStones by remember { mutableStateOf(product.show.hasStones) }
    var showStones by remember { mutableStateOf(product.show.stones) }
    var showCustomPrice by remember { mutableStateOf(product.show.customPrice) }
    var showMaterialWeight by remember { mutableStateOf(product.show.materialWeight) }
    var showStoneWeight by remember { mutableStateOf(product.show.stoneWeight) }
    var showMakingPercent by remember { mutableStateOf(product.show.makingPercent) }
    var showLabourCharges by remember { mutableStateOf(product.show.labourCharges) }
    var showEffectiveWeight by remember { mutableStateOf(product.show.effectiveWeight) }
    var showEffectiveMetalWeight by remember { mutableStateOf(product.show.effectiveMetalWeight) }
    var showLabourRate by remember { mutableStateOf(product.show.labourRate) }
    var showImages by remember { mutableStateOf(product.show.images) }
    var showAvailable by remember { mutableStateOf(product.show.available) }
    var showFeatured by remember { mutableStateOf(product.show.featured) }
    var showIsCollectionProduct by remember { mutableStateOf(product.show.isCollectionProduct) }
    var showCollectionId by remember { mutableStateOf(product.show.collectionId) }


    // Extract kundan/jarkan stones from productStones
    val kundanStones = remember(productStones) {
        productStones.filter { it.name.equals("Kundan", ignoreCase = true) }
    }
    
    val jarkanStones = remember(productStones) {
        productStones.filter { it.name.equals("Jarkan", ignoreCase = true) }
    }
    
    // Prepare inputs for price calculator
    val priceCalculatorInputs = remember(
        totalWeight,
        materialId,
        materialType,
        materialName,
        materialWeight,
        kundanStones,
        jarkanStones,
        makingPercent,
        labourRate,
        metalRateViewModel
    ) {
        val totalWeightValue = totalWeight.toDoubleOrNull() ?: 0.0
        val metalPurity = materialType
        val metalWeightValue = materialWeight.toDoubleOrNull() ?: totalWeightValue
        
        // Fetch metal rate using materialId and materialType
        val metalRatePerGram = if (materialId.isNotEmpty() && materialType.isNotEmpty()) {
            try {
                val karat = extractKaratFromMaterialType(materialType)
                val metalRate = metalRateViewModel.calculateRateForMaterial(materialId, materialType, karat)
                metalRate
            } catch (e: Exception) {
                println("⚠️ Error fetching metal rate: ${e.message}")
                0.0
            }
        } else {
            0.0
        }
        
        // Sum all Kundan prices and weights from stones array
        val kundanPrice = kundanStones.sumOf { it.amount }
        val kundanWeight = kundanStones.sumOf { it.weight }
        
        // Sum all Jarkan prices and weights from stones array
        val jarkanPrice = jarkanStones.sumOf { it.amount }
        val jarkanWeight = jarkanStones.sumOf { it.weight }
        
        ProductPriceInputs(
            grossWeight = totalWeightValue,
            goldPurity = metalPurity,
            goldWeight = metalWeightValue,
            makingPercentage = makingPercent.toDoubleOrNull() ?: 0.0,
            labourRatePerGram = labourRate.toDoubleOrNull() ?: 0.0,
            kundanPrice = kundanPrice,
            kundanWeight = kundanWeight,
            jarkanPrice = jarkanPrice,
            jarkanWeight = jarkanWeight,
            goldRatePerGram = metalRatePerGram
        )
    }
    


    // Auto-calculate stone amount when stone fields change
    LaunchedEffect(stoneWeight, stoneRate, stoneQuantity) {
        if (hasStones && stoneWeight.isNotEmpty() && stoneRate.isNotEmpty() && stoneQuantity.isNotEmpty()) {
            val stoneWeightValue = stoneWeight.toDoubleOrNull() ?: 0.0
            val stoneRateValue = stoneRate.toDoubleOrNull() ?: 0.0
            val stoneQuantityValue = stoneQuantity.toDoubleOrNull() ?: 0.0

            if (stoneWeightValue > 0 && stoneRateValue > 0 && stoneQuantityValue > 0) {
                val calculatedStoneAmount = stoneWeightValue * stoneRateValue * stoneQuantityValue
                println("💎 Auto-calculated stone amount: $calculatedStoneAmount (Weight: $stoneWeightValue × Rate: $stoneRateValue × Qty: $stoneQuantityValue)")
            }
        }
    }

    // Auto-disable show featured when featured is false
    LaunchedEffect(featured) {
        if (!featured) {
            showFeatured = false
        }
    }

    // Load collections and initialize selected collection
    LaunchedEffect(Unit) {
        try {
            collections = collectionRepository.getAllCollections()
            // Initialize selected collection if product has a collection ID
            if (collectionId.isNotBlank()) {
                selectedCollection = collections.find { it.id == collectionId }
            }
        } catch (e: Exception) {
            println("Error loading collections: ${e.message}")
        }
    }

    // Update collection ID when selected collection changes
    LaunchedEffect(selectedCollection) {
        selectedCollection?.let { collection ->
            collectionId = collection.id
        } ?: run {
            if (selectedCollection == null) {
                collectionId = ""
            }
        }
    }

    // Validation state
    var nameError by remember { mutableStateOf(false) }
    var materialRateError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }
    var materialError by remember { mutableStateOf(false) }
    var customIdError by remember { mutableStateOf(false) }
    var barcodeError by remember { mutableStateOf("") }
    var weightMismatchError by remember { mutableStateOf("") }

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

                                // Show barcodes for this product with edit option (same behavior as dashboard)
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = Color(0xFFE0E0E0))
                                Spacer(modifier = Modifier.height(12.dp))

                                val inventoryDataForProduct = viewModel.inventoryData.value[product.id]
                                val barcodeIdsForProduct = inventoryDataForProduct?.barcodeIds ?: emptyList()

                                Text(
                                    text = "Barcodes (${barcodeIdsForProduct.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2D2D2D)
                                )

                                if (barcodeIdsForProduct.isEmpty()) {
                                    Text(
                                        text = "No barcodes found for this product.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        barcodeIdsForProduct.forEachIndexed { index, barcode ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = barcode,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colors.primary
                                                )

                                                IconButton(onClick = { onEditBarcode(barcode) }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit barcode $barcode",
                                                        tint = MaterialTheme.colors.primary
                                                    )
                                                }
                                            }

                                            if (index != barcodeIdsForProduct.lastIndex) {
                                                Divider(color = Color(0xFFE0E0E0), modifier = Modifier.padding(vertical = 6.dp))
                                            }
                                        }
                                    }
                                }
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
                        placeholder = "e.g., Gold Necklace",
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(enterMovesFocusModifier)
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

                    // Category field
                    AutoCompleteTextField(
                        value = categoryDisplayValue,
                        onValueChange = { query ->
                            categoryDisplayValue = query
                            // Find matching category
                            val matchedCategory = categories.find { 
                                it.name.equals(query, ignoreCase = true) 
                            }
                            if (matchedCategory != null) {
                                categoryId = matchedCategory.id
                                categoryError = false
                            } else {
                                // If no match, keep the display value but don't update categoryId
                                // This allows users to type new category names
                            }
                        },
                        onItemSelected = { selectedCategoryName ->
                            categoryDisplayValue = selectedCategoryName
                            val selectedCategory = categories.find { 
                                it.name.equals(selectedCategoryName, ignoreCase = true) 
                            }
                            selectedCategory?.let {
                                categoryId = it.id
                                categoryError = false
                            }
                        },
                        onAddNew = { newCategoryName ->
                            categoryDisplayValue = newCategoryName
                            // You could add logic here to create a new category
                            println("New category name entered: $newCategoryName")
                        },
                        suggestions = categorySuggestions,
                        label = "Category",
                        placeholder = "Select or enter category...",
                        singleLine = true,
                        maxSuggestions = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(enterMovesFocusModifier)
                    )
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
                            onValueChange = {
                                totalWeight = it
                                weightMismatchError = ""
                            },
                            label = "Total Weight",
                            placeholder = "0.00",
                            suffix = "grams",
                            keyboardType = KeyboardType.Decimal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(enterMovesFocusModifier)
                        )
                    }
                    if (weightMismatchError.isNotEmpty()) {
                        Text(
                            text = weightMismatchError,
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // Material & Specifications
            SectionCard(title = "Material & Specifications") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Add Metal/Stone Button
                    Button(
                        onClick = { showAddMaterialDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Metal or Stone")
                    }

                    // Display selected metal
                    if (materialId.isNotEmpty() || materialName.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFFF5F5F5),
                            elevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Metal: ${materialName.ifEmpty { "Not selected" }}", fontWeight = FontWeight.Medium)
                                    Text("Type: ${materialType.ifEmpty { "Not selected" }}", fontSize = 12.sp, color = Color.Gray)
                                    Text("Weight: ${materialWeight.ifEmpty { "0" }}g", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    materialId = ""
                                    materialType = ""
                                    materialName = ""
                                    materialWeight = ""
                                    materialDisplayValue = ""
                                    materialTypeDisplayValue = ""
                                    weightMismatchError = "" // Clear error when material is removed
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    }

                    // Display stones
                    if (productStones.isNotEmpty()) {
                        productStones.forEachIndexed { index, stone ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color(0xFFF5F5F5),
                                elevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Stone: ${stone.name}", fontWeight = FontWeight.Medium)
                                        Text("Purity: ${stone.purity}", fontSize = 12.sp, color = Color.Gray)
                                        Text("Weight: ${stone.weight}g, Rate: ${CurrencyFormatter.formatRupees(stone.rate, includeDecimals = true)}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = {
                                        productStones = productStones.filterIndexed { i, _ -> i != index }
                                        if (productStones.isEmpty()) {
                                            hasStones = false
                                        }
                                        weightMismatchError = "" // Clear error when stone is removed
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove")
                                    }
                                }
                            }
                        }
                    }

                    if (materialId.isEmpty() && productStones.isEmpty()) {
                            Text(
                            text = "No materials added yet. Click 'Add Metal or Stone' to add materials.",
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Add Material Dialog
            AddMaterialDialog(
                openDialog = showAddMaterialDialog,
                onDismiss = { showAddMaterialDialog = false },
                onSave = { newMaterial ->
                    if (newMaterial.isMetal) {
                        // If it's a metal, update materialId, materialType, materialName, and materialWeight
                        materialId = newMaterial.materialId
                        materialType = newMaterial.materialType
                        materialName = newMaterial.materialName
                        materialWeight = newMaterial.weight.toString()
                        materialDisplayValue = newMaterial.materialName
                        materialTypeDisplayValue = newMaterial.materialType
                        weightMismatchError = ""
                    } else {
                        // If it's a stone (like jarkan), add it to the stones array
                        val newStone = org.example.project.data.ProductStone(
                            name = newMaterial.materialName,
                            purity = newMaterial.materialType,
                            quantity = 0.0, // For jarkan/kundan, quantity is 0
                            rate = newMaterial.rate,
                            weight = newMaterial.weight,
                            amount = newMaterial.rate // For jarkan/kundan, amount is the rate
                        )
                        productStones = productStones + newStone
                        hasStones = true
                        // Update stone fields for backward compatibility
                        stoneName = newMaterial.materialName
                        stonePurity = newMaterial.materialType
                        stoneRate = newMaterial.rate.toString()
                        stoneWeight = newMaterial.weight.toString()
                        weightMismatchError = "" // Clear error when material/stone is added
                    }
                },
                materials = materials,
                totalWeight = totalWeight,
                existingMaterials = emptyList(), // No longer using materials array
                metalRateViewModel = metalRateViewModel,
                productsViewModel = viewModel
            )

            // Price Calculator (only show if metal is selected)
            if (materialId.isNotEmpty()) {
                ProductPriceCalculatorComposable(
                    inputs = priceCalculatorInputs,
                    onMakingPercentageChange = { makingPercent = it.toString() },
                    onLabourRateChange = { labourRate = it.toString() },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Custom Price Option
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
                                    // When custom price is enabled, show custom price field
                                    showCustomPrice = true
                                } else {
                                    // When custom price is disabled, hide custom price field
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(enterMovesFocusModifier)
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

                    Divider(color = Color(0xFFE0E0E0))

                    StatusToggle(
                        title = "Collection Product",
                        description = "Add this product to a themed collection",
                        checked = isCollectionProduct,
                        onCheckedChange = { isCollectionProduct = it }
                    )
                }
            }

            // Collection Selection (only show if collection product is enabled)
            if (isCollectionProduct) {
                SectionCard(title = "Collection Selection") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Select a themed collection for this product",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Collection dropdown using AutoCompleteTextField
                        AutoCompleteTextField(
                            value = collectionSearchQuery,
                            onValueChange = { query ->
                                collectionSearchQuery = query
                                showCollectionDropdown = true
                            },
                            onItemSelected = { selectedCollectionName ->
                                val collection = collections.find { it.name == selectedCollectionName }
                                selectedCollection = collection
                                collectionSearchQuery = selectedCollectionName
                                showCollectionDropdown = false
                            },
                            onAddNew = { newCollectionName ->
                                // For now, just show a message that collections need to be created separately
                                println("New collection creation not implemented yet: $newCollectionName")
                            },
                            suggestions = collections.filter { collection ->
                                collection.name.contains(collectionSearchQuery, ignoreCase = true) && collection.isActive
                            }.map { it.name },
                            label = "Search Collections",
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isCollectionProduct
                        )

                        // Selected collection display
                        selectedCollection?.let { collection ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color(0xFFF0F9FF),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = collection.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E40AF)
                                        )
                                        if (collection.description.isNotBlank()) {
                                            Text(
                                                text = collection.description,
                                                fontSize = 12.sp,
                                                color = Color(0xFF6B7280),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedCollection = null
                                            collectionSearchQuery = ""
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove Collection",
                                            tint = Color(0xFF6B7280),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // No collections message
                        if (collections.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color(0xFFFFF3CD),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF856404),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No themed collections found. Create collections first to assign products.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF856404)
                                    )
                                }
                            }
                        }
                    }
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
                    StatusToggle("Show Has Stones", "Display stones toggle", showHasStones) { showHasStones = it }
                    StatusToggle("Show Stones", "Display stones information", showStones) { showStones = it }
                    StatusToggle("Show Custom Price", "Display custom price field", showCustomPrice) { showCustomPrice = it }
                    StatusToggle("Show Material Weight", "Display material weight", showMaterialWeight) { showMaterialWeight = it }
                    StatusToggle("Show Stone Weight", "Display stone weight", showStoneWeight) { showStoneWeight = it }
                    StatusToggle("Show Making Percent", "Display making percentage", showMakingPercent) { showMakingPercent = it }
                    StatusToggle("Show Labour Charges", "Display labour charges", showLabourCharges) { showLabourCharges = it }
                    StatusToggle("Show Effective Weight", "Display effective weight", showEffectiveWeight) { showEffectiveWeight = it }
                    StatusToggle("Show Effective Metal Weight", "Display effective metal weight", showEffectiveMetalWeight) { showEffectiveMetalWeight = it }
                    StatusToggle("Show Labour Rate", "Display labour rate", showLabourRate) { showLabourRate = it }
                    StatusToggle("Show Images", "Display product images section", showImages) { showImages = it }
                    StatusToggle("Show Available", "Display available status", showAvailable) { showAvailable = it }
                    StatusToggle("Show Featured", "Display featured status", showFeatured) { showFeatured = it }
                    StatusToggle("Show Collection Product", "Display collection product status", showIsCollectionProduct) { showIsCollectionProduct = it }
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
                        nameError = name.trim().isEmpty()
                        // Category is no longer required (removed from UI)
                        categoryError = false
                        // Material validation: check if materialId is not empty
                        materialError = materialId.isEmpty()

                        // Material rate validation: not required since materials have their own rates
                        materialRateError = false

                        customIdError = !autoGenerateId && customProductId.isEmpty()

                        println("Validation results - Name: $nameError, Category: $categoryError, Material: $materialError, MaterialRate: $materialRateError, CustomId: $customIdError")

                        if (!nameError && !materialRateError && !categoryError && !materialError && !customIdError) {
                            // Ensure total weight matches sum of all materials and stones
                            val totalWeightValue = totalWeight.toDoubleOrNull() ?: 0.0
                            val materialWeightValue = materialWeight.toDoubleOrNull() ?: 0.0
                            val stonesWeightSum = productStones.sumOf { it.weight }
                            val totalMaterialsWeight = materialWeightValue + stonesWeightSum
                            
                            if (totalMaterialsWeight > 0) {
                                val weightDiff = kotlin.math.abs(totalWeightValue - totalMaterialsWeight)
                                if (weightDiff > 0.001) {
                                    weightMismatchError = "Total weight ($totalWeightValue g) should match sum of materials and stones ($totalMaterialsWeight g)"
                                    println("❌ Validation failed: weight mismatch -> total: $totalWeightValue, materials + stones sum: $totalMaterialsWeight (material: $materialWeightValue, stones: $stonesWeightSum)")
                                    return@Button
                                } else {
                                    weightMismatchError = ""
                                }
                            }

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

                            // Use productStones array (already contains all stones including jarkan/kundan)
                            val stonesList = productStones

                            // Calculate effective metal weight using price calculator
                            // Effective Metal Weight = New Weight - (Jarkan Weight + Kundan Weight)
                            val calculatedEffectiveMetalWeight = run {
                                val totalWeightValue = totalWeight.toDoubleOrNull() ?: 0.0
                                val makingPercentValue = makingPercent.toDoubleOrNull() ?: 0.0
                                val makingWeightValue = totalWeightValue * makingPercentValue / 100.0
                                val newWeightValue = totalWeightValue + makingWeightValue
                                
                                // Sum jarkan and kundan weights from stones
                                val jarkanWeight = jarkanStones.sumOf { it.weight }
                                val kundanWeight = kundanStones.sumOf { it.weight }
                                
                                // Effective Metal Weight = New Weight - (Jarkan Weight + Kundan Weight)
                                (newWeightValue - jarkanWeight - kundanWeight).coerceAtLeast(0.0)
                            }

                            val updatedProduct = Product(
                                id = if (isEditing) product.id else "",
                                name = name,
                                description = description,
                                price = materialRate.toDoubleOrNull() ?: 0.0,
                                categoryId = categoryId,
                                materialId = materialId,
                                materialType = materialType,
                                materialName = materialName,
                                quantity = 1,
                                totalWeight = totalWeight.toDoubleOrNull() ?: 0.0,
                                hasStones = hasStones,
                                stones = stonesList,
                                materialWeight = materialWeight.toDoubleOrNull() ?: 0.0,
                                makingPercent = makingPercent.toDoubleOrNull() ?: 0.0,
                                // Calculate labour charges: labour rate × new weight
                                labourCharges = run {
                                    val totalWeightValue = totalWeight.toDoubleOrNull() ?: 0.0
                                    val makingPercentValue = makingPercent.toDoubleOrNull() ?: 0.0
                                    val makingWeightValue = totalWeightValue * makingPercentValue / 100.0
                                    val newWeightValue = totalWeightValue + makingWeightValue
                                    val labourRateValue = labourRate.toDoubleOrNull() ?: 0.0
                                    labourRateValue * newWeightValue
                                },
                                // Calculate newWeight = totalWeight + makingWeight and store in effectiveWeight
                                // makingWeight = totalWeight * makingPercent / 100
                                // newWeight = totalWeight + makingWeight
                                effectiveWeight = run {
                                    val totalWeightValue = totalWeight.toDoubleOrNull() ?: 0.0
                                    val makingPercentValue = makingPercent.toDoubleOrNull() ?: 0.0
                                    val makingWeightValue = totalWeightValue * makingPercentValue / 100.0
                                    val newWeightValue = totalWeightValue + makingWeightValue
                                    // Use manually entered effectiveWeight if provided, otherwise use calculated newWeight
                                    val manualEffectiveWeight = effectiveWeight.toDoubleOrNull() ?: 0.0
                                    if (manualEffectiveWeight > 0) manualEffectiveWeight else newWeightValue
                                },
                                effectiveMetalWeight = calculatedEffectiveMetalWeight,
                                labourRate = labourRate.toDoubleOrNull() ?: 0.0,
                                // Calculate stoneAmount and stoneWeight from stones array
                                stoneAmount = stonesList.sumOf { it.amount },
                                stoneWeight = stonesList.sumOf { it.weight },
                                hasCustomPrice = hasCustomPrice,
                                customPrice = customPrice.toDoubleOrNull() ?: 0.0,
                                available = available,
                                featured = featured,
                                isCollectionProduct = isCollectionProduct,
                                collectionId = collectionId,
                                images = images,
                                autoGenerateId = autoGenerateId,
                                createdAt = if (isEditing) product.createdAt else System.currentTimeMillis(),
                                show = org.example.project.data.ProductShowConfig(
                                    name = showName,
                                    description = showDescription,
                                    category = showCategory,
                                    material = showMaterial,
                                    materialType = showMaterialType,
                                    quantity = showQuantity,
                                    totalWeight = showTotalWeight,
                                    price = showPrice,
                                    hasStones = showHasStones,
                                    stones = showStones,
                                    customPrice = showCustomPrice,
                                    materialWeight = showMaterialWeight,
                                    stoneWeight = showStoneWeight,
                                    makingPercent = showMakingPercent,
                                    labourCharges = showLabourCharges,
                                    effectiveWeight = showEffectiveWeight,
                                    effectiveMetalWeight = showEffectiveMetalWeight,
                                    labourRate = showLabourRate,
                                    stoneAmount = true,
                                    images = showImages,
                                    available = showAvailable,
                                    featured = showFeatured,
                                    isCollectionProduct = showIsCollectionProduct,
                                    collectionId = showCollectionId
                                )
                            )

                            println("✅ Product object created successfully: ${updatedProduct.name} (ID: ${updatedProduct.id})")

                            if (isEditing) {
                                // For editing, use bulk update to update all products with same commonId
                                println("🔄 Editing mode: updating product and all products with same commonId")
                                scope.launch {
                                    try {
                                        if (updatedProduct != null) {
                                            println("📤 Updating product...")
                                            println("   - Product ID: ${updatedProduct.id}")
                                            println("   - Product Name: ${updatedProduct.name}")

                                            // Update the product
                                            viewModel.updateProduct(updatedProduct)
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
                                                        barcodeId = barcode
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
                                                    barcodeId = customProductId
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
    readOnly: Boolean = false,
    onEnter: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val enterHandler = remember(focusManager, onEnter, singleLine) {
        if (!singleLine) null else onEnter ?: { focusManager.moveFocus(FocusDirection.Down) }
    }
    val enterModifier = remember(enterHandler) {
        if (enterHandler == null) {
            Modifier
        } else {
            Modifier.onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                    enterHandler()
                    true
                } else false
            }
        }
    }

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
            modifier = Modifier
                .fillMaxWidth()
                .then(enterModifier),
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = if (enterHandler != null) ImeAction.Next else ImeAction.Default
            ),
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

// Material Specifications Table Component
@Composable
fun MaterialSpecificationsTable(
    materials: List<ProductMaterial>,
    onRemoveMaterial: (ProductMaterial) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Material", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
            Text("Type", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
            Text("Weight (g)", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Rate (₹/g)", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Amount", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(40.dp)) // Space for delete button
        }

        // Table Rows
        materials.forEach { material ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    material.materialName,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    material.materialType,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    String.format("%.2f", material.weight),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    CurrencyFormatter.formatRupees(material.rate, includeDecimals = true),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    // For Jarkan/Kundan, amount is the rate itself, not weight * rate
                    String.format("%.2f", if (material.materialName.equals("Jarkan", ignoreCase = true) || material.materialName.equals("Kundan", ignoreCase = true)) {
                        material.rate
                    } else {
                        material.weight * material.rate
                    }),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onRemoveMaterial(material) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colors.error
                    )
                }
            }
            if (material != materials.last()) {
                Divider()
            }
        }
    }
}
