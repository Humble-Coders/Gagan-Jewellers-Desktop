package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.data.Material
import org.example.project.data.ProductMaterial
import org.example.project.data.Stone
import org.example.project.viewModels.MetalRateViewModel
import org.example.project.viewModels.ProductsViewModel
import org.example.project.utils.CurrencyFormatter
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.CircularProgressIndicator

// 3-Step Dialog for Adding Materials
@Composable
fun AddMaterialDialog(
    openDialog: Boolean,
    existingMaterial: ProductMaterial? = null, // Optional existing material for editing
    onDismiss: () -> Unit,
    onSave: (ProductMaterial) -> Unit,
    materials: List<Material>,
    totalWeight: String,
    existingMaterials: List<ProductMaterial>,
    metalRateViewModel: MetalRateViewModel,
    productsViewModel: ProductsViewModel
) {
    if (!openDialog) return

    var currentStep by remember { mutableStateOf(1) }
    var isMetal by remember { mutableStateOf<Boolean?>(null) } // null = not selected, true = metal, false = stone
    var stoneType by remember { mutableStateOf<String?>(null) } // null = not selected, "Kundan", "Diamond", "Jarkan"
    var selectedMaterialId by remember { mutableStateOf("") }
    var selectedMaterialName by remember { mutableStateOf("") }
    var selectedStoneId by remember { mutableStateOf("") }
    var selectedStoneName by remember { mutableStateOf("") }
    var karatOrPurity by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var cwWeight by remember { mutableStateOf("") } // Carat weight for stones
    var rate by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    
    // State for add new karat dialog
    var showAddKaratDialog by remember { mutableStateOf(false) }
    var newKaratInput by remember { mutableStateOf("") }
    var newKaratRateInput by remember { mutableStateOf("") }
    var isAddingKarat by remember { mutableStateOf(false) }

    // Load stones when dialog opens
    LaunchedEffect(openDialog) {
        if (openDialog) {
            productsViewModel.loadStoneSuggestions()
        }
    }
    
    // Get stones and purities
    val stones = productsViewModel.stones.value
    val stonePurities = productsViewModel.stonePurities.value

    // Initialize or reset when dialog opens
    LaunchedEffect(openDialog, existingMaterial) {
        if (openDialog) {
            if (existingMaterial != null) {
                // Pre-fill with existing material data (edit mode)
                isMetal = existingMaterial.isMetal
                selectedMaterialId = existingMaterial.materialId
                selectedMaterialName = existingMaterial.materialName
                selectedStoneName = existingMaterial.materialName
                karatOrPurity = existingMaterial.materialType
                weight = existingMaterial.weight.toString()
                rate = String.format("%.2f", existingMaterial.rate)
                
                // Determine stone type and set appropriate fields
                if (!existingMaterial.isMetal) {
                    val stoneName = existingMaterial.materialName
                    when {
                        stoneName.equals("Diamond", ignoreCase = true) || 
                        stoneName.equals("Solitaire", ignoreCase = true) -> {
                            stoneType = stoneName
                            // Extract purity and cent from materialType (format: "purity|cent")
                            val parts = existingMaterial.materialType.split("|")
                            karatOrPurity = if (parts.size > 1 && parts[0].isNotEmpty()) parts[0] else ""
                            cwWeight = parts.lastOrNull()?.toDoubleOrNull()?.toString() ?: ""
                        }
                        stoneName.equals("Kundan", ignoreCase = true) || 
                        stoneName.equals("Jarkan", ignoreCase = true) -> {
                            stoneType = stoneName
                            // For Kundan/Jarkan, materialType is empty, so karatOrPurity stays empty
                        }
                        else -> {
                            stoneType = "Color Stones"
                            // For Color Stones, materialType is empty, so karatOrPurity stays empty
                        }
                    }
                    // Skip step 1 and go directly to step 3 for stones
                    currentStep = 3
                } else {
                    // For metals, go to step 2 (Select & Specs)
                    currentStep = 2
                    // Fetch rate for metal when editing
                    if (existingMaterial.materialId.isNotEmpty() && existingMaterial.materialType.isNotEmpty()) {
                        scope.launch {
                            val karatRegex = Regex("""(\d+)K""", RegexOption.IGNORE_CASE)
                            val match = karatRegex.find(existingMaterial.materialType)
                            val karat = match?.groupValues?.get(1)?.toIntOrNull() ?: 22
                            val calculatedRate = metalRateViewModel.calculateRateForMaterial(
                                existingMaterial.materialId,
                                existingMaterial.materialType,
                                karat
                            )
                            if (calculatedRate > 0) {
                                rate = String.format("%.2f", calculatedRate)
                            }
                        }
                    }
                }
            } else {
                // Reset for new material (add mode)
                currentStep = 1
                isMetal = null
                stoneType = null
                selectedMaterialId = ""
                selectedMaterialName = ""
                selectedStoneId = ""
                selectedStoneName = ""
                karatOrPurity = ""
                weight = ""
                cwWeight = ""
                rate = ""
                weightError = ""
            }
        }
    }
    
    // Calculate total steps based on selection
    val totalSteps = remember(isMetal, stoneType) {
        when {
            isMetal == true -> 3 // Metal: Choose Type -> Select & Specs -> Weight
            isMetal == false && (stoneType == "Diamond" || stoneType == "Solitaire") -> 3 // Stone Diamond/Solitaire: Choose Type -> Stone Type -> Details (no step 4)
            isMetal == false && stoneType == "Color Stones" -> 3 // Stone Color Stones: Choose Type -> Stone Type -> Details (no step 4)
            isMetal == false && (stoneType == "Kundan" || stoneType == "Jarkan") -> 3 // Stone Kundan/Jarkan: Choose Type -> Stone Type -> Price & Weight
            else -> 3 // Default
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colors.surface,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header with back button, centered title, and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button (icon only) - shown only when not on step 1
                    if (currentStep > 1) {
                        IconButton(onClick = { 
                            currentStep--
                            // Reset stone type when going back from step 3 to step 2
                            if (currentStep == 2 && isMetal == false) {
                                stoneType = null
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    } else {
                        // Spacer to maintain layout when back button is hidden
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    
                    // Centered title
                    Text(
                        text = "Add Material",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    
                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step Indicator - Dynamic based on flow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StepIndicator(step = 1, currentStep = currentStep, label = "Choose Type")
                    when {
                        isMetal == false && stoneType == null -> {
                            // Show stone type step when stone is selected but type not chosen
                            StepIndicator(step = 2, currentStep = currentStep, label = "Stone Type")
                        }
                        isMetal == false && (stoneType == "Diamond" || stoneType == "Solitaire") -> {
                            // Diamond/Solitaire flow: Stone Type -> Details (Name, Weight/Cent, Rate) - no step 4
                            StepIndicator(step = 2, currentStep = currentStep, label = "Stone Type")
                            StepIndicator(step = 3, currentStep = currentStep, label = "Details")
                        }
                        isMetal == false && stoneType == "Color Stones" -> {
                            // Color Stones flow: Stone Type -> Name & Weight & Rate (no step 4)
                            StepIndicator(step = 2, currentStep = currentStep, label = "Stone Type")
                            StepIndicator(step = 3, currentStep = currentStep, label = "Details")
                        }
                        isMetal == false && (stoneType == "Kundan" || stoneType == "Jarkan") -> {
                            // Kundan/Jarkan flow: Stone Type -> Price & Weight
                            StepIndicator(step = 2, currentStep = currentStep, label = "Stone Type")
                            StepIndicator(step = 3, currentStep = currentStep, label = "Price & Weight")
                        }
                        isMetal == true -> {
                            // Metal flow: Select & Specs -> Weight
                            StepIndicator(step = 2, currentStep = currentStep, label = "Select & Specs")
                            StepIndicator(step = 3, currentStep = currentStep, label = "Weight")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(24.dp))

                // Step Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        1 -> Step1ChooseType(
                            isMetal = isMetal,
                            onTypeSelected = { isMetalSelected ->
                                isMetal = isMetalSelected
                                stoneType = null // Reset stone type when changing material type
                                // Reset selections when type changes
                                selectedMaterialId = ""
                                selectedMaterialName = ""
                                selectedStoneId = ""
                                selectedStoneName = ""
                                karatOrPurity = ""
                                weight = ""
                                cwWeight = ""
                                rate = ""
                            },
                            onNext = {
                                if (isMetal != null) {
                                    currentStep++
                                }
                            },
                            canProceed = isMetal != null
                        )
                        2 -> {
                            // Step 2: Stone Type selection (if stone) OR Select & Specs (if metal)
                            if (isMetal == false && stoneType == null) {
                                // Show stone type selection
                                Step2ChooseStoneType(
                                    stoneType = stoneType,
                                    onStoneTypeSelected = { selectedType ->
                                        stoneType = selectedType
                                        // Reset stone-specific fields when type changes
                                        selectedStoneId = ""
                                        selectedStoneName = ""
                                        karatOrPurity = ""
                                        rate = ""
                                        weight = ""
                                        cwWeight = ""
                                        // Automatically advance to step 3 when stone type is selected
                                        currentStep = 3
                                    },
                                    onNext = {
                                        if (stoneType != null) {
                                            currentStep++
                                        }
                                    },
                                    canProceed = stoneType != null
                                )
                            } else if (isMetal == true) {
                                // Metal: Select & Specs (Step 2 for metal)
                                Step3SelectAndSpecs(
                                    isMetal = true,
                                    materials = materials,
                                    stones = stones,
                                    selectedMaterialId = selectedMaterialId,
                                    selectedMaterialName = selectedMaterialName,
                                    selectedStoneId = selectedStoneId,
                                    selectedStoneName = selectedStoneName,
                                    onMaterialSelected = { materialId, materialName ->
                                        selectedMaterialId = materialId
                                        selectedMaterialName = materialName
                                        // Clear karat and rate when material changes
                                        karatOrPurity = ""
                                        rate = ""
                                    },
                                    onMaterialNameChange = { newName ->
                                        selectedMaterialName = newName
                                    },
                                    onStoneSelected = { stoneId, stoneName ->
                                        selectedStoneId = stoneId
                                        selectedStoneName = stoneName
                                        // Clear purity and rate when stone changes
                                        karatOrPurity = ""
                                        rate = ""
                                    },
                                    onStoneNameChange = { newName ->
                                        selectedStoneName = newName
                                    },
                                    karatOrPurity = karatOrPurity,
                                    onKaratOrPurityChange = { karatOrPurity = it },
                                    rate = rate,
                                    onRateChange = { rate = it },
                                    metalRateViewModel = metalRateViewModel,
                                    stonePurities = stonePurities,
                                    onShowAddKaratDialog = { showAddKaratDialog = true },
                                    onNewKaratInputChange = { newKaratInput = it },
                                    onNewKaratRateInputChange = { newKaratRateInput = it },
                                    onNext = {
                                        if (selectedMaterialId.isNotEmpty() && karatOrPurity.isNotEmpty()) {
                                            currentStep++
                                        }
                                    },
                                    canProceed = selectedMaterialId.isNotEmpty() && karatOrPurity.isNotEmpty()
                                )
                            } else {
                                // If stone type is selected but we're still on step 2, this shouldn't happen
                                // Stone type selection should automatically advance to step 3
                                // This is a fallback - just show a spacer
                                    Spacer(modifier = Modifier)
                            }
                        }
                        3 -> {
                            // Step 3: Select & Specs (for Metal/Diamond) OR Price & Weight (for Kundan/Jarkan)
                            if (isMetal == true) {
                                // Metal: Weight (Step 3 for metal)
                                Step4Weight(
                                    isMetal = true,
                                    weight = weight,
                                    cwWeight = cwWeight,
                                    onWeightChange = { weight = it },
                                    onCwWeightChange = { cwWeight = it },
                                    onSave = {
                                        val weightValue = weight.toDoubleOrNull() ?: 0.0
                                        val rateValue = rate.toDoubleOrNull() ?: 0.0
                                        
                                        if (weightValue <= 0) {
                                            weightError = "Weight must be greater than 0"
                                            return@Step4Weight
                                        }
                                        
                                            val productMaterial = ProductMaterial(
                                                materialId = selectedMaterialId,
                                                materialName = selectedMaterialName,
                                                materialType = karatOrPurity,
                                                weight = weightValue,
                                                rate = rateValue,
                                                isMetal = true
                                            )
                                        weightError = ""
                                            onSave(productMaterial)
                                            onDismiss()
                                    },
                                canSave = weight.toDoubleOrNull()?.let { it > 0 } ?: false,
                                weightError = weightError
                                )
                            } else if (isMetal == false && (stoneType == "Diamond" || stoneType == "Solitaire")) {
                                // Diamond/Solitaire: Name (optional), Weight (grams), Cent (carats), Rate - Step 3
                                Step3DiamondSolitaireDetails(
                                    stoneType = stoneType ?: "",
                                    stoneName = selectedStoneName,
                                    onStoneNameChange = { selectedStoneName = it },
                                    weight = weight, // Weight in grams
                                    onWeightChange = { weight = it },
                                    cent = cwWeight, // Cent in carats
                                    onCentChange = { cwWeight = it },
                                    rate = rate,
                                    onRateChange = { rate = it },
                                    weightError = weightError,
                                    onSave = {
                                        val weightValue = weight.toDoubleOrNull() ?: 0.0 // Weight in grams
                                        val centValue = cwWeight.toDoubleOrNull() ?: 0.0 // Cent in carats
                                        val rateValue = rate.toDoubleOrNull() ?: 0.0 // Rate per carat
                                        val amountValue = centValue * rateValue // Amount = cent * rate
                                        
                                        if (weightValue <= 0) {
                                            weightError = "Weight (grams) must be greater than 0"
                                            return@Step3DiamondSolitaireDetails
                                        }
                                        if (centValue <= 0) {
                                            weightError = "Cent (carats) must be greater than 0"
                                            return@Step3DiamondSolitaireDetails
                                        }
                                        if (rateValue <= 0) {
                                            weightError = "Rate must be greater than 0"
                                            return@Step3DiamondSolitaireDetails
                                        }
                                        
                                        // Create ProductMaterial - will be converted to ProductStone in AddEditProductScreen
                                        // For Diamond/Solitaire: store weight (grams) in weight field for validation
                                        // Store cent (carats) in materialType field as "purity|cent" for amount calculation
                                        weightError = ""
                                        val materialTypeWithCent = if (karatOrPurity.isNotEmpty()) "$karatOrPurity|$centValue" else "|$centValue"
                                        onSave(ProductMaterial(
                                            materialId = selectedStoneId,
                                            materialName = if (selectedStoneName.isNotEmpty()) selectedStoneName else stoneType ?: "",
                                            materialType = materialTypeWithCent, // Store purity and cent as "purity|cent"
                                            weight = weightValue, // Weight in grams (for validation)
                                            rate = rateValue, // Rate per carat
                                            isMetal = false
                                        ))
                                        onDismiss()
                                    },
                                    canSave = {
                                        val centValue = cwWeight.toDoubleOrNull() ?: 0.0
                                        val rateValue = rate.toDoubleOrNull() ?: 0.0
                                        centValue > 0 && rateValue > 0
                                    }()
                                )
                            } else if (isMetal == false && stoneType == "Color Stones") {
                                // Color Stones: Name (optional), Weight, Rate - Step 3 (no step 4)
                                Step3ColorStonesDetails(
                                    stoneName = selectedStoneName,
                                    onStoneNameChange = { selectedStoneName = it },
                                    weight = weight,
                                    onWeightChange = { weight = it },
                                    rate = rate,
                                    onRateChange = { rate = it },
                                    weightError = weightError,
                                    onSave = {
                                        val weightValue = weight.toDoubleOrNull() ?: 0.0 // Weight in grams
                                        val rateValue = rate.toDoubleOrNull() ?: 0.0 // Rate per gram
                                        val amountValue = weightValue * rateValue // Color Stones: weight * rate
                                        
                                        if (weightValue <= 0) {
                                            weightError = "Weight must be greater than 0"
                                            return@Step3ColorStonesDetails
                                        }
                                        if (rateValue <= 0) {
                                            weightError = "Rate must be greater than 0"
                                            return@Step3ColorStonesDetails
                                        }
                                        
                                        // Create ProductMaterial - will be converted to ProductStone in AddEditProductScreen
                                        // For Color Stones: weight is in grams, rate is per gram, amount = weight * rate
                                        weightError = ""
                                        onSave(ProductMaterial(
                                            materialId = "",
                                            materialName = if (selectedStoneName.isNotEmpty()) selectedStoneName else "Color Stone",
                                            materialType = "", // Purity not used for color stones
                                            weight = weightValue, // Weight in grams
                                            rate = rateValue, // Rate per gram
                                            isMetal = false
                                        ))
                                        onDismiss()
                                    },
                                    canSave = {
                                        val weightValue = weight.toDoubleOrNull() ?: 0.0
                                        val rateValue = rate.toDoubleOrNull() ?: 0.0
                                        weightValue > 0 && rateValue > 0
                                    }()
                                )
                                } else if (isMetal == false && (stoneType == "Kundan" || stoneType == "Jarkan")) {
                                    // Kundan/Jarkan: Amount & Weight (Step 3)
                                    // Amount is direct entry, not calculated from weight * rate
                                    // For Jarkan, amount can be 0; for Kundan, amount must be > 0
                                    val isJarkan = stoneType == "Jarkan"
                                    Step3PriceAndWeight(
                                        stoneType = stoneType ?: "",
                                        rate = rate,
                                        weight = weight,
                                        onRateChange = { rate = it },
                                        onWeightChange = { weight = it },
                                        weightError = weightError,
                                        onSave = {
                                            val weightValue = weight.toDoubleOrNull() ?: 0.0
                                            val rateValue = rate.toDoubleOrNull() ?: 0.0
                                            
                                            if (weightValue <= 0) {
                                                weightError = "Weight must be greater than 0"
                                                return@Step3PriceAndWeight
                                            }
                                            // For Jarkan: amount can be >= 0, for Kundan: amount must be > 0
                                            if (!isJarkan && rateValue <= 0) {
                                                weightError = "Amount must be greater than 0 for Kundan"
                                                return@Step3PriceAndWeight
                                            }
                                            
                                                val productMaterial = ProductMaterial(
                                                    materialId = "",
                                                    materialName = stoneType ?: "",
                                                    materialType = "",
                                                    weight = weightValue,
                                                    rate = rateValue, // This is the amount, not a rate per gram
                                                    isMetal = false
                                                )
                                            weightError = ""
                                                onSave(productMaterial)
                                                onDismiss()
                                        },
                                        canSave = {
                                            val weightValue = weight.toDoubleOrNull() ?: 0.0
                                            val rateValue = rate.toDoubleOrNull() ?: 0.0
                                            // Weight must always be > 0
                                            // For Jarkan: amount can be >= 0, for Kundan: amount must be > 0
                                            weightValue > 0 && (isJarkan || rateValue > 0)
                                        }()
                                    )
                            }
                        }
                        4 -> {
                            // Step 4: Weight (only for Diamond/Solitaire flow - but now handled in step 3)
                            // This step is no longer used for Diamond/Solitaire as they are handled in step 3
                            // Keep for backward compatibility but shouldn't be reached
                                Spacer(modifier = Modifier)
                        }
                    }
                }
            }
        }
        
        // Add Karat Rate Dialog
        if (showAddKaratDialog) {
            AddKaratRateDialog(
                materialId = selectedMaterialId,
                materialName = selectedMaterialName,
                karatInput = newKaratInput,
                rateInput = newKaratRateInput,
                onKaratInputChange = { newKaratInput = it },
                onRateInputChange = { newKaratRateInput = it },
                onDismiss = { 
                    showAddKaratDialog = false
                    newKaratInput = ""
                    newKaratRateInput = ""
                },
                onSave = {
                    scope.launch {
                        isAddingKarat = true
                        try {
                            val rateValue = newKaratRateInput.toDoubleOrNull() ?: 0.0
                            if (rateValue > 0 && newKaratInput.isNotEmpty() && selectedMaterialId.isNotEmpty()) {
                                // Extract karat number
                                val karatRegex = Regex("""(\d+)K""", RegexOption.IGNORE_CASE)
                                val match = karatRegex.find(newKaratInput)
                                val karat = match?.groupValues?.get(1)?.toIntOrNull() ?: 24
                                
                                // Add or update the metal rate
                                metalRateViewModel.updateMetalRateWithHistory(
                                    materialId = selectedMaterialId,
                                    materialName = selectedMaterialName,
                                    materialType = newKaratInput,
                                    newPricePerGram = rateValue
                                )
                                
                                // Refresh metal rates
                                metalRateViewModel.loadMetalRates()
                                
                                // Update the karat and rate in the form
                                karatOrPurity = newKaratInput
                                rate = String.format("%.2f", rateValue)
                                
                                // Close dialog
                                showAddKaratDialog = false
                                newKaratInput = ""
                                newKaratRateInput = ""
                                        }
                        } catch (e: Exception) {
                            println("Error adding karat rate: ${e.message}")
                        } finally {
                            isAddingKarat = false
                            }
                        }
                },
                loading = isAddingKarat
            )
        }
    }
}

@Composable
fun RowScope.StepIndicator(step: Int, currentStep: Int, label: String) {
    val isActive = step == currentStep
    val isCompleted = step < currentStep
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = when {
                        isCompleted -> MaterialTheme.colors.primary
                        isActive -> MaterialTheme.colors.primary
                        else -> Color.Gray
                    },
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = step.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isActive || isCompleted) MaterialTheme.colors.primary else Color.Gray
        )
    }
}

@Composable
fun Step1ChooseType(
    isMetal: Boolean?,
    onTypeSelected: (Boolean) -> Unit, // true = metal, false = stone
    onNext: () -> Unit,
    canProceed: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose Type",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metal option
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp)
                    .clickable { onTypeSelected(true) },
                shape = RoundedCornerShape(12.dp),
                color = if (isMetal == true) 
                    MaterialTheme.colors.primary.copy(alpha = 0.1f) 
                else 
                    MaterialTheme.colors.surface,
                elevation = if (isMetal == true) 8.dp else 2.dp,
                border = if (isMetal == true) 
                    BorderStroke(2.dp, MaterialTheme.colors.primary)
                else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Metal",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMetal == true) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                    )
                    if (isMetal == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            // Stone option
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp)
                    .clickable { onTypeSelected(false) },
                shape = RoundedCornerShape(12.dp),
                color = if (isMetal == false) 
                    MaterialTheme.colors.primary.copy(alpha = 0.1f) 
                else 
                    MaterialTheme.colors.surface,
                elevation = if (isMetal == false) 8.dp else 2.dp,
                border = if (isMetal == false) 
                    BorderStroke(2.dp, MaterialTheme.colors.primary)
                else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Stone",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMetal == false) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                    )
                    if (isMetal == false) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Next Button
        Button(
            onClick = onNext,
            enabled = canProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Step2ChooseStoneType(
    stoneType: String?,
    onStoneTypeSelected: (String) -> Unit,
    onNext: () -> Unit,
    canProceed: Boolean
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose Stone Type",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kundan option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { onStoneTypeSelected("Kundan") },
                shape = RoundedCornerShape(12.dp),
                color = if (stoneType == "Kundan") 
                    MaterialTheme.colors.primary.copy(alpha = 0.1f) 
                else 
                    MaterialTheme.colors.surface,
                elevation = if (stoneType == "Kundan") 8.dp else 2.dp,
                border = if (stoneType == "Kundan") 
                    BorderStroke(2.dp, MaterialTheme.colors.primary)
                else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Kundan",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stoneType == "Kundan") MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                    )
                    if (stoneType == "Kundan") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            // Diamond option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { onStoneTypeSelected("Diamond") },
                shape = RoundedCornerShape(12.dp),
                color = if (stoneType == "Diamond") 
                    MaterialTheme.colors.primary.copy(alpha = 0.1f) 
                else 
                    MaterialTheme.colors.surface,
                elevation = if (stoneType == "Diamond") 8.dp else 2.dp,
                border = if (stoneType == "Diamond") 
                    BorderStroke(2.dp, MaterialTheme.colors.primary)
                else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Diamond",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stoneType == "Diamond") MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                    )
                    if (stoneType == "Diamond") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            // Jarkan option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { onStoneTypeSelected("Jarkan") },
                shape = RoundedCornerShape(12.dp),
                color = if (stoneType == "Jarkan") 
                    MaterialTheme.colors.primary.copy(alpha = 0.1f) 
                else 
                    MaterialTheme.colors.surface,
                elevation = if (stoneType == "Jarkan") 8.dp else 2.dp,
                border = if (stoneType == "Jarkan") 
                    BorderStroke(2.dp, MaterialTheme.colors.primary)
                else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Jarkan",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stoneType == "Jarkan") MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                    )
                    if (stoneType == "Jarkan") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            // Solitaire option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { onStoneTypeSelected("Solitaire") },
                shape = RoundedCornerShape(12.dp),
                color = if (stoneType == "Solitaire") 
                    MaterialTheme.colors.primary.copy(alpha = 0.1f) 
                else 
                    MaterialTheme.colors.surface,
                elevation = if (stoneType == "Solitaire") 8.dp else 2.dp,
                border = if (stoneType == "Solitaire") 
                    BorderStroke(2.dp, MaterialTheme.colors.primary)
                else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Solitaire",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stoneType == "Solitaire") MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                    )
                    if (stoneType == "Solitaire") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            // Color Stones option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { onStoneTypeSelected("Color Stones") },
                shape = RoundedCornerShape(12.dp),
                color = if (stoneType == "Color Stones") 
                    MaterialTheme.colors.primary.copy(alpha = 0.1f) 
                else 
                    MaterialTheme.colors.surface,
                elevation = if (stoneType == "Color Stones") 8.dp else 2.dp,
                border = if (stoneType == "Color Stones") 
                    BorderStroke(2.dp, MaterialTheme.colors.primary)
                else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Color Stones",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stoneType == "Color Stones") MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                    )
                    if (stoneType == "Color Stones") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        
        // Next Button
        Button(
            onClick = onNext,
            enabled = canProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Step3SelectAndSpecs(
    isMetal: Boolean?,
    materials: List<Material>,
    stones: List<Stone>,
    selectedMaterialId: String,
    selectedMaterialName: String,
    selectedStoneId: String,
    selectedStoneName: String,
    onMaterialSelected: (String, String) -> Unit,
    onStoneSelected: (String, String) -> Unit,
    onMaterialNameChange: (String) -> Unit,
    onStoneNameChange: (String) -> Unit,
    karatOrPurity: String,
    onKaratOrPurityChange: (String) -> Unit,
    rate: String,
    onRateChange: (String) -> Unit,
    metalRateViewModel: MetalRateViewModel,
    stonePurities: List<String>,
    onShowAddKaratDialog: (() -> Unit)? = null,
    onNewKaratInputChange: ((String) -> Unit)? = null,
    onNewKaratRateInputChange: ((String) -> Unit)? = null,
    onNext: () -> Unit,
    canProceed: Boolean
) {
    val metalRates by metalRateViewModel.metalRates.collectAsState()
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isMetal == true) "Select Material & Karat" else "Select Stone & Purity",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        if (isMetal == true) {
            // Metal flow: Material autocomplete + Karat
            // Material autocomplete - materials come from materials collection
            val materialNames = materials.map { it.name }
            
            AutoCompleteTextField(
                value = selectedMaterialName,
                onValueChange = { newValue ->
                    // Update the material name as user types
                    onMaterialNameChange(newValue)
                    // Try to find matching material and update ID
                    val matchingMaterial = materials.find { it.name.equals(newValue, ignoreCase = true) }
                    if (matchingMaterial != null) {
                        onMaterialSelected(matchingMaterial.id, matchingMaterial.name)
                    } else {
                        // Clear the ID if no match found (user is typing a new name)
                        onMaterialSelected("", newValue)
                    }
                },
                onItemSelected = { selectedName ->
                    // When user selects from suggestions, find the material and update both ID and name
                    val matchingMaterial = materials.find { it.name.equals(selectedName, ignoreCase = true) }
                    if (matchingMaterial != null) {
                        onMaterialSelected(matchingMaterial.id, matchingMaterial.name)
                    }
                },
                onAddNew = { newName ->
                    // When user adds a new material name, it will be handled when they proceed
                    // Just update the name for now
                    onMaterialNameChange(newName)
                    onMaterialSelected("", newName)
                },
                suggestions = materialNames,
                label = "Material",
                placeholder = "Select or enter material name",
                maxSuggestions = 10,
                enabled = true
            )

            // Karat input for metals - filter by selected material from materials collection types array
            val availableKarats = remember(selectedMaterialId, materials) {
                if (selectedMaterialId.isEmpty()) {
                    emptyList()
                } else {
                    // Get all material types (karats) for the selected material from materials collection types array
                    val selectedMaterial = materials.find { it.id == selectedMaterialId }
                    selectedMaterial?.types
                        ?.map { it.purity }
                        ?.filter { it.isNotEmpty() }
                        ?.distinct()
                        ?.sorted()
                        ?: emptyList()
                }
            }
            
            AutoCompleteTextField(
                value = karatOrPurity,
                onValueChange = { newValue ->
                    onKaratOrPurityChange(newValue)
                    // Auto-calculate rate
                    if (newValue.isNotEmpty() && selectedMaterialId.isNotEmpty()) {
                        val karatRegex = Regex("""(\d+)K""", RegexOption.IGNORE_CASE)
                        val match = karatRegex.find(newValue)
                        val karat = match?.groupValues?.get(1)?.toIntOrNull() ?: 22
                        val calculatedRate = metalRateViewModel.calculateRateForMaterial(
                            selectedMaterialId,
                            newValue,
                            karat
                        )
                        if (calculatedRate > 0) {
                            onRateChange(String.format("%.2f", calculatedRate))
                        } else {
                            // Check if karat doesn't exist in available karats
                            val karatExists = availableKarats.any { it.equals(newValue, ignoreCase = true) }
                            if (!karatExists && newValue.isNotEmpty() && newValue.matches(Regex("""\d+K""", RegexOption.IGNORE_CASE))) {
                                // Show dialog to add new karat
                                onNewKaratInputChange?.invoke(newValue)
                                onNewKaratRateInputChange?.invoke("")
                                onShowAddKaratDialog?.invoke()
                            }
                        }
                    }
                },
                onItemSelected = { selected ->
                    onKaratOrPurityChange(selected)
                    val karatRegex = Regex("""(\d+)K""", RegexOption.IGNORE_CASE)
                    val match = karatRegex.find(selected)
                    val karat = match?.groupValues?.get(1)?.toIntOrNull() ?: 22
                    val calculatedRate = metalRateViewModel.calculateRateForMaterial(
                        selectedMaterialId,
                        selected,
                        karat
                    )
                    if (calculatedRate > 0) {
                        onRateChange(String.format("%.2f", calculatedRate))
                    }
                },
                onAddNew = { newKarat ->
                    // When user adds a new karat, show dialog to add rate
                    onNewKaratInputChange?.invoke(newKarat)
                    onNewKaratRateInputChange?.invoke("")
                    onShowAddKaratDialog?.invoke()
                },
                suggestions = availableKarats,
                label = "Karat",
                placeholder = if (selectedMaterialId.isEmpty()) "Select material first" else "Select or enter karat (e.g., 24K, 22K)",
                maxSuggestions = 10,
                enabled = selectedMaterialId.isNotEmpty()
            )

            // Rate field (editable with currency formatting)
            var isRateFocused by remember { mutableStateOf(false) }
            val displayRateValue = remember(rate, isRateFocused) {
                if (isRateFocused || rate.isEmpty()) {
                    // When focused or empty, show raw value for editing
                    rate
                } else {
                    // When not focused, format the value with commas
                    val numericValue = rate.toDoubleOrNull() ?: 0.0
                    if (numericValue > 0) {
                        CurrencyFormatter.formatRupeesNumber(numericValue, includeDecimals = true)
                    } else {
                        rate
                    }
                }
            }
            
            OutlinedTextField(
                value = displayRateValue,
                onValueChange = { input ->
                    // Remove formatting (commas) for editing
                    val rawInput = input.replace(",", "").replace("₹", "").trim()
                    if (rawInput.isEmpty() || rawInput.matches(Regex("^\\d*\\.?\\d*\$"))) {
                        onRateChange(rawInput)
                    }
                },
                label = { Text("Rate per gram (₹)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isRateFocused = focusState.isFocused
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        } else false
                    },
                enabled = true // Always enabled for editing
            )
        } else {
            // Stone flow: Stone autocomplete + Purity dropdown
            // Stone autocomplete - stones come from stones collection
            val stoneNames = stones.map { it.name }
            
            AutoCompleteTextField(
                value = selectedStoneName,
                onValueChange = { newValue ->
                    // Update the stone name as user types
                    onStoneNameChange(newValue)
                    // Try to find matching stone and update ID
                    val matchingStone = stones.find { it.name.equals(newValue, ignoreCase = true) }
                    if (matchingStone != null) {
                        onStoneSelected(matchingStone.id, matchingStone.name)
                    } else {
                        // Clear the ID if no match found (user is typing a new name)
                        onStoneSelected("", newValue)
                    }
                },
                onItemSelected = { selectedName ->
                    // When user selects from suggestions, find the stone and update both ID and name
                    val matchingStone = stones.find { it.name.equals(selectedName, ignoreCase = true) }
                    if (matchingStone != null) {
                        onStoneSelected(matchingStone.id, matchingStone.name)
                    }
                },
                onAddNew = { newName ->
                    // When user adds a new stone name, it will be handled when they proceed
                    // Just update the name for now
                    onStoneNameChange(newName)
                    onStoneSelected("", newName)
                },
                suggestions = stoneNames,
                label = "Stone",
                placeholder = "Select or enter stone name",
                maxSuggestions = 10,
                enabled = true
            )

            // Purity dropdown for stones - filter by selected stone
            val availablePurities = remember(selectedStoneName, metalRates) {
                if (selectedStoneName.isEmpty()) {
                    emptyList()
                } else {
                    // Get all material types (purities) for the selected stone from rates collection
                    // Match by material_name (stone name)
                    metalRates
                        .filter { 
                            it.materialName.equals(selectedStoneName, ignoreCase = true) && 
                            it.isActive 
                        }
                        .map { it.materialType }
                        .distinct()
                        .sorted()
                }
            }
            
            AutoCompleteTextField(
                value = karatOrPurity,
                onValueChange = { newValue ->
                    onKaratOrPurityChange(newValue)
                    // Auto-calculate rate for stones when purity is selected
                    if (newValue.isNotEmpty() && selectedStoneName.isNotEmpty()) {
                        // Find rate from rates collection using stone name and purity
                        val stoneRate = metalRates.find { 
                            it.materialName.equals(selectedStoneName, ignoreCase = true) &&
                            it.materialType.equals(newValue, ignoreCase = true) &&
                            it.isActive
                        }
                        if (stoneRate != null && stoneRate.pricePerGram > 0) {
                            onRateChange(String.format("%.2f", stoneRate.pricePerGram))
                        }
                    }
                },
                onItemSelected = { selected ->
                    onKaratOrPurityChange(selected)
                    // Auto-calculate rate when purity is selected from dropdown
                    if (selected.isNotEmpty() && selectedStoneName.isNotEmpty()) {
                        // Find rate from rates collection using stone name and purity
                        val stoneRate = metalRates.find { 
                            it.materialName.equals(selectedStoneName, ignoreCase = true) &&
                            it.materialType.equals(selected, ignoreCase = true) &&
                            it.isActive
                        }
                        if (stoneRate != null && stoneRate.pricePerGram > 0) {
                            onRateChange(String.format("%.2f", stoneRate.pricePerGram))
                        }
                    }
                },
                onAddNew = { },
                suggestions = availablePurities,
                label = "Purity",
                placeholder = if (selectedStoneName.isEmpty()) "Select stone first" else "Select or enter purity (e.g., VVS, VS, Colorless)",
                maxSuggestions = 10,
                enabled = selectedStoneName.isNotEmpty()
            )

            // Auto-calculate rate when stone name or purity changes
            LaunchedEffect(selectedStoneName, karatOrPurity) {
                if (selectedStoneName.isNotEmpty() && karatOrPurity.isNotEmpty()) {
                    // Find rate from rates collection using stone name and purity
                    val stoneRate = metalRates.find { 
                        it.materialName.equals(selectedStoneName, ignoreCase = true) &&
                        it.materialType.equals(karatOrPurity, ignoreCase = true) &&
                        it.isActive
                    }
                    if (stoneRate != null && stoneRate.pricePerGram > 0) {
                        onRateChange(String.format("%.2f", stoneRate.pricePerGram))
                    }
                }
            }

            // Rate field for stones
            OutlinedTextField(
                value = rate,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                        onRateChange(input)
                    }
                },
                label = { Text("Rate per carat (₹)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Next Button
        Button(
            onClick = onNext,
            enabled = canProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Step3PriceAndWeight(
    stoneType: String,
    rate: String,
    weight: String,
    onRateChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    weightError: String = ""
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "$stoneType - Amount & Weight",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Amount/Price field - direct entry, no calculation
        // For Jarkan, price can be 0; for Kundan, price must be > 0
        val isJarkan = stoneType == "Jarkan"
        OutlinedTextField(
            value = rate,
            onValueChange = { input: String ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                    onRateChange(input)
                }
            },
            label = { Text("Amount (₹)") },
            placeholder = { Text(if (isJarkan) "0.00 (optional)" else "0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        if (isJarkan) {
            Text(
                "Amount can be 0 for Jarkan",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        // Weight field
        OutlinedTextField(
            value = weight,
            onValueChange = { input: String ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                    onWeightChange(input)
                }
            },
            label = { Text("Weight (grams)") },
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            isError = weightError.isNotEmpty()
        )
        if (weightError.isNotEmpty()) {
            Text(
                text = weightError,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
        }
        }
        
        // Save Button
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Step3DiamondSolitaireDetails(
    stoneType: String,
    stoneName: String,
    onStoneNameChange: (String) -> Unit,
    weight: String, // Weight in grams
    onWeightChange: (String) -> Unit,
    cent: String, // Cent in carats
    onCentChange: (String) -> Unit,
    rate: String, // Rate per carat
    onRateChange: (String) -> Unit,
    weightError: String,
    onSave: () -> Unit,
    canSave: Boolean
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    
    // Calculate amount: cent * rate
    val amount = remember(cent, rate) {
        val centValue = cent.toDoubleOrNull() ?: 0.0
        val rateValue = rate.toDoubleOrNull() ?: 0.0
        centValue * rateValue
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "$stoneType - Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Name field (optional)
        OutlinedTextField(
            value = stoneName,
            onValueChange = onStoneNameChange,
            label = { Text("Name (Optional)") },
            placeholder = { Text("Enter stone name") },
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                        focusManager.moveFocus(FocusDirection.Down)
                        true
                    } else false
                },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = MaterialTheme.colors.primary,
                focusedLabelColor = MaterialTheme.colors.primary
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Weight field - in grams
        OutlinedTextField(
            value = weight,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                    onWeightChange(input)
                }
            },
            label = { Text("Weight (grams)") },
            placeholder = { Text("Enter weight in grams") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                        focusManager.moveFocus(FocusDirection.Down)
                        true
                    } else false
                },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = MaterialTheme.colors.primary,
                focusedLabelColor = MaterialTheme.colors.primary
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Cent field - in carats
        OutlinedTextField(
            value = cent,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                    onCentChange(input)
                }
            },
            label = { Text("Cent (Carats)") },
            placeholder = { Text("Enter cent in carats") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                        focusManager.moveFocus(FocusDirection.Down)
                        true
                    } else false
                },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = MaterialTheme.colors.primary,
                focusedLabelColor = MaterialTheme.colors.primary
            ),
            shape = RoundedCornerShape(8.dp),
            isError = weightError.isNotEmpty() && (weightError.contains("Cent", ignoreCase = true) || weightError.contains("Weight", ignoreCase = true)),
            trailingIcon = {
                Text("carats", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
            }
        )

        // Rate field - per carat
        OutlinedTextField(
            value = rate,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                    onRateChange(input)
                }
            },
            label = { Text("Rate per Carat (₹)") },
            placeholder = { Text("Enter rate per carat") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                        onSave()
                        true
                    } else false
                },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = MaterialTheme.colors.primary,
                focusedLabelColor = MaterialTheme.colors.primary
            ),
            shape = RoundedCornerShape(8.dp),
            isError = weightError.isNotEmpty() && weightError.contains("Rate", ignoreCase = true)
        )
        
        // Display calculated amount
        if (weight.isNotEmpty() && rate.isNotEmpty() && amount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFF0F9FF),
                elevation = 1.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Calculated Amount:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E40AF)
                    )
                    Text(
                        CurrencyFormatter.formatRupees(amount, includeDecimals = true),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                }
            }
        }
        
        if (weightError.isNotEmpty()) {
            Text(
                text = weightError,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        }
        
        // Save Button
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Step3ColorStonesDetails(
    stoneName: String,
    onStoneNameChange: (String) -> Unit,
    weight: String, // Weight in grams
    onWeightChange: (String) -> Unit,
    rate: String, // Rate per gram
    onRateChange: (String) -> Unit,
    weightError: String,
    onSave: () -> Unit,
    canSave: Boolean
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    
    // Calculate amount: weight * rate
    val amount = remember(weight, rate) {
        val weightValue = weight.toDoubleOrNull() ?: 0.0
        val rateValue = rate.toDoubleOrNull() ?: 0.0
        weightValue * rateValue
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Color Stones - Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

        // Name field (optional)
        OutlinedTextField(
            value = stoneName,
            onValueChange = onStoneNameChange,
            label = { Text("Name (Optional)") },
            placeholder = { Text("Enter stone name (e.g., Ruby, Emerald)") },
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                        focusManager.moveFocus(FocusDirection.Down)
                        true
                    } else false
                },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = MaterialTheme.colors.primary,
                focusedLabelColor = MaterialTheme.colors.primary
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Weight field - in grams
        OutlinedTextField(
            value = weight,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                    onWeightChange(input)
                }
            },
            label = { Text("Weight (grams)") },
            placeholder = { Text("Enter weight in grams") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                        focusManager.moveFocus(FocusDirection.Down)
                        true
                    } else false
                },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = MaterialTheme.colors.primary,
                focusedLabelColor = MaterialTheme.colors.primary
            ),
            shape = RoundedCornerShape(8.dp),
            isError = weightError.isNotEmpty() && weightError.contains("Weight", ignoreCase = true)
        )

        // Rate field - per gram
        OutlinedTextField(
            value = rate,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                    onRateChange(input)
                }
            },
            label = { Text("Rate per Gram (₹)") },
            placeholder = { Text("Enter rate per gram") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                        onSave()
                        true
                    } else false
                },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = MaterialTheme.colors.primary,
                focusedLabelColor = MaterialTheme.colors.primary
            ),
            shape = RoundedCornerShape(8.dp),
            isError = weightError.isNotEmpty() && weightError.contains("Rate", ignoreCase = true)
        )
        
        // Display calculated amount
        if (weight.isNotEmpty() && rate.isNotEmpty() && amount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFF0F9FF),
                elevation = 1.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Calculated Amount:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E40AF)
                    )
                    Text(
                        CurrencyFormatter.formatRupees(amount, includeDecimals = true),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                }
            }
        }
        
        if (weightError.isNotEmpty()) {
            Text(
                text = weightError,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        }
        
        // Save Button
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Step4Weight(
    isMetal: Boolean?,
    weight: String,
    cwWeight: String,
    onWeightChange: (String) -> Unit,
    onCwWeightChange: (String) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    weightError: String
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isMetal == true) "Enter Weight" else "Enter CW Weight",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        if (isMetal == true) {
            // Weight for metals (in grams)
            OutlinedTextField(
                value = weight,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                        onWeightChange(input)
                    }
                },
                label = { Text("Weight (grams)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                            onSave()
                            true
                        } else false
                    }
            )
            if (weightError.isNotEmpty()) {
                Text(
                    text = weightError,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            // CW Weight for stones (in carats)
            OutlinedTextField(
                value = cwWeight,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                        onCwWeightChange(input)
                    }
                },
                label = { Text("CW Weight (carats)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                            onSave()
                            true
                        } else false
                    }
            )
        }
        }
        
        // Save Button
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

