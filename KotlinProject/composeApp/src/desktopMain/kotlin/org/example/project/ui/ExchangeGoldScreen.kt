package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.JewelryAppInitializer
import org.example.project.data.ExchangeGold
import org.example.project.data.extractKaratFromMaterialType
import org.example.project.data.normalizeMaterialType
import org.example.project.utils.CurrencyFormatter
import org.example.project.viewModels.MetalRateViewModel

@Composable
fun ExchangeGoldScreen(
    onExchangeGoldComplete: (ExchangeGold) -> Unit,
    onBack: () -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var totalProductWeight by remember { mutableStateOf("") }
    var percentage by remember { mutableStateOf("") }
    var goldWeight by remember { mutableStateOf("") }
    var selectedPurity by remember { mutableStateOf("") }
    var goldRateDisplay by remember { mutableStateOf("") }
    var isRateAutoFetched by remember { mutableStateOf(false) } // Track if rate was auto-fetched from metal rates
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val metalRateViewModel = JewelryAppInitializer.getMetalRateViewModel()
    val metalRates by metalRateViewModel.metalRates.collectAsState()

    // Load metal rates if not already loaded
    LaunchedEffect(Unit) {
        metalRateViewModel.loadMetalRates()
    }

    // Get Gold material rates from MetalRateRepository (filters for Gold material only)
    val goldRates = remember(metalRates) {
        metalRates.filter { rate ->
            rate.materialName.equals("Gold", ignoreCase = true) && 
            rate.materialId.isNotEmpty() // Exclude stones (stones have empty materialId)
        }
    }

    // Get Gold material ID from first Gold rate (all Gold rates have same materialId)
    val goldMaterialId = goldRates.firstOrNull()?.materialId ?: ""

    // Get available purities for Gold material from metal rates
    val availablePurities = remember(goldRates) {
        goldRates
            .map { it.materialType } // materialType contains the purity (e.g., "22K", "24K")
                .distinct()
                .sorted()
    }

    // Auto-calculate gold weight from total product weight and percentage
    // Formula: goldWeight = totalProductWeight * (percentage / 100)
    LaunchedEffect(totalProductWeight, percentage) {
        val totalWeight = totalProductWeight.toDoubleOrNull() ?: 0.0
        val percent = percentage.toDoubleOrNull() ?: 0.0
        
        if (totalWeight > 0 && percent >= 0) {
            val calculatedWeight = totalWeight * (percent / 100.0)
            goldWeight = String.format("%.2f", calculatedWeight)
        } else if (totalProductWeight.isEmpty() && percentage.isEmpty()) {
            // Only clear if both fields are empty
            goldWeight = ""
        }
    }

    // Calculate gold rate from display string (updated by AutoCompleteTextField handlers)
    val goldRate = goldRateDisplay.toDoubleOrNull() ?: 0.0

    // Calculate final exchange gold price
    val finalExchangePrice = remember(goldWeight, goldRate) {
        val weight = goldWeight.toDoubleOrNull() ?: 0.0
        weight * goldRate
    }

    Dialog(onDismissRequest = onBack) {
        Card(
            modifier = Modifier
                .width(800.dp)
                .height(750.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFBF5),
                                Color(0xFFFFF8E1)
                            )
                        )
                    )
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFB8973D),
                                    Color(0xFFD4AF37)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Exchange Gold",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 32.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Product Name (Optional)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            elevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = productName,
                                    onValueChange = { productName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Product Name (Optional)") },
                                    placeholder = { Text("Enter product name", color = Color.Gray.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color(0xFFB8973D),
                                        unfocusedBorderColor = Color(0xFFE0E0E0),
                                        cursorColor = Color(0xFFB8973D),
                                        focusedLabelColor = Color(0xFFB8973D)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Total Product Weight
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            elevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Total Product Weight (grams)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB8973D)
                                    )
                                    Text(
                                        " *",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = totalProductWeight,
                                    onValueChange = { 
                                        // Validate input (allow decimal numbers)
                                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                            totalProductWeight = it
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Total Product Weight") },
                                    placeholder = { Text("Enter total product weight in grams", color = Color.Gray.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color(0xFFB8973D),
                                        unfocusedBorderColor = Color(0xFFE0E0E0),
                                        cursorColor = Color(0xFFB8973D),
                                        focusedLabelColor = Color(0xFFB8973D)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Percentage
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            elevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Percentage (%)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB8973D)
                                    )
                                    Text(
                                        " *",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = percentage,
                                    onValueChange = { 
                                        // Validate input (allow decimal numbers, max 100)
                                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                            val value = it.toDoubleOrNull() ?: 0.0
                                            if (value <= 100.0) {
                                                percentage = it
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Percentage") },
                                    placeholder = { Text("Enter percentage (0-100)", color = Color.Gray.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color(0xFFB8973D),
                                        unfocusedBorderColor = Color(0xFFE0E0E0),
                                        cursorColor = Color(0xFFB8973D),
                                        focusedLabelColor = Color(0xFFB8973D)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Gold Weight (Auto-calculated)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Gold Weight (grams)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB8973D)
                                )
                                Text(
                                    " *",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = goldWeight,
                                onValueChange = { 
                                    // Validate input (allow decimal numbers)
                                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        goldWeight = it
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Gold Weight") },
                                placeholder = { Text("Enter gold weight in grams", color = Color.Gray.copy(alpha = 0.5f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color(0xFFB8973D),
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    cursorColor = Color(0xFFB8973D),
                                    focusedLabelColor = Color(0xFFB8973D)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                        // Metal Purity
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            elevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Metal Purity",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB8973D)
                                    )
                                    Text(
                                        " *",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Purity Selection (similar to Step3SelectAndSpecs for metals)
                                // Get available purities from gold material's types array (always for gold)
                                // Wrap in Box to ensure dropdown is properly positioned above other elements
                                Box(modifier = Modifier.fillMaxWidth()) {
                                AutoCompleteTextField(
                                    value = selectedPurity,
                                    onValueChange = { newValue ->
                                        selectedPurity = newValue
                                            // Normalize user input for comparison (22, 22K, 22k should match)
                                            val normalizedInput = normalizeMaterialType(newValue)
                                            
                                            // Check if purity exists in available purities (already normalized)
                                            val purityExists = availablePurities.any { 
                                                normalizeMaterialType(it) == normalizedInput
                                            }
                                            
                                            if (newValue.isNotEmpty() && goldMaterialId.isNotEmpty() && purityExists) {
                                                // Purity exists - try to get rate from metal rates
                                                try {
                                                    val goldRate = goldRates.find { 
                                                        normalizeMaterialType(it.materialType) == normalizedInput
                                                    }
                                                    val rateFromMetalRates = goldRate?.pricePerGram ?: 0.0
                                                
                                                if (rateFromMetalRates > 0) {
                                                    // Use rate directly from metal rates
                                                    goldRateDisplay = String.format("%.2f", rateFromMetalRates)
                                                        isRateAutoFetched = true // Rate was auto-fetched, disable editing
                                                    } else {
                                                        // Purity exists but no rate - allow user to type
                                                        goldRateDisplay = ""
                                                        isRateAutoFetched = false // Rate not found, enable editing
                                                    }
                                                } catch (e: Exception) {
                                                    println("⚠️ Error fetching rate: ${e.message}")
                                                    goldRateDisplay = ""
                                                    isRateAutoFetched = false
                                                }
                                            } else if (newValue.isNotEmpty()) {
                                                // New purity (not in available purities) - allow user to type rate
                                                goldRateDisplay = ""
                                                isRateAutoFetched = false // New purity, enable editing
                                            } else {
                                                // Clear when purity is empty
                                                goldRateDisplay = ""
                                                isRateAutoFetched = false
                                            }
                                        },
                                        suggestions = availablePurities, // All purities from Gold metal rates
                                        label = "Purity",
                                        placeholder = if (goldMaterialId.isEmpty()) "Gold material not found" else "Select or enter purity (e.g., 22K, 24K)",
                                        modifier = Modifier.fillMaxWidth(), // Remove height constraint to allow dropdown to show
                                        maxSuggestions = 100, // Show all available purities (high limit ensures all are shown)
                                    onItemSelected = { selected ->
                                        selectedPurity = selected
                                            // Normalize selected purity for comparison (22, 22K, 22k should match)
                                            val normalizedSelected = normalizeMaterialType(selected)
                                            
                                            // Purity selected from dropdown - fetch rate from metal rates
                                        if (selected.isNotEmpty() && goldMaterialId.isNotEmpty()) {
                                            try {
                                                    val goldRate = goldRates.find { 
                                                        normalizeMaterialType(it.materialType) == normalizedSelected
                                                    }
                                                    val rateFromMetalRates = goldRate?.pricePerGram ?: 0.0
                                                
                                                if (rateFromMetalRates > 0) {
                                                    // Use rate directly from metal rates
                                                    goldRateDisplay = String.format("%.2f", rateFromMetalRates)
                                                        isRateAutoFetched = true // Rate was auto-fetched, disable editing
                                                    } else {
                                                        // Purity exists but no rate - allow user to type
                                                        goldRateDisplay = ""
                                                        isRateAutoFetched = false // Rate not found, enable editing
                                                    }
                                                } catch (e: Exception) {
                                                    println("⚠️ Error fetching rate: ${e.message}")
                                                    goldRateDisplay = ""
                                                    isRateAutoFetched = false
                                                }
                                            } else {
                                                // Clear if selection is invalid
                                                goldRateDisplay = ""
                                                isRateAutoFetched = false
                                            }
                                        },
                                        onAddNew = { newPurity ->
                                            // New purity added - allow user to type rate manually
                                            selectedPurity = newPurity
                                            goldRateDisplay = ""
                                            isRateAutoFetched = false // New purity, enable editing
                                    },
                                    enabled = goldMaterialId.isNotEmpty()
                                )
                                }

                    Spacer(modifier = Modifier.height(16.dp))

                                // Rate Field (auto-filled from material types or editable for new purity)
                                OutlinedTextField(
                                    value = goldRateDisplay,
                                    onValueChange = { newValue ->
                                        // Only allow editing if rate is not auto-fetched
                                        if (!isRateAutoFetched) {
                                            // Validate input (allow decimal numbers)
                                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                                goldRateDisplay = newValue
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Rate per gram (₹)") },
                                    placeholder = { 
                                        Text(
                                            if (isRateAutoFetched) "Rate will be auto-filled" else "Enter rate manually",
                                            color = Color.Gray.copy(alpha = 0.5f)
                                        ) 
                                    },
                                    singleLine = true,
                                    readOnly = isRateAutoFetched, // Read-only when auto-fetched, but still visible
                                    enabled = true, // Always enabled for proper display
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp,
                                        color = if (isRateAutoFetched) Color(0xFF1B5E20) else Color(0xFF000000) // Dark green when auto-fetched, black when editable
                                    ),
                                    colors = if (isRateAutoFetched) {
                                        // Green styling for auto-fetched rate - make text clearly visible
                                        TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color(0xFF4CAF50),
                                            unfocusedBorderColor = Color(0xFF4CAF50),
                                            focusedLabelColor = Color(0xFF4CAF50),
                                            unfocusedLabelColor = Color(0xFF4CAF50),
                                            cursorColor = Color(0xFF4CAF50)
                                        )
                                    } else {
                                        // Normal styling for editable rate
                                        TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color(0xFFB8973D),
                                            unfocusedBorderColor = Color(0xFFE0E0E0),
                                            focusedLabelColor = Color(0xFFB8973D),
                                            unfocusedLabelColor = Color(0xFF666666),
                                            cursorColor = Color(0xFFB8973D)
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }


                        // Final Exchange Gold Price
                        if (finalExchangePrice > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color(0xFFFFF9C4),
                        shape = RoundedCornerShape(12.dp),
                                elevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Final Exchange Gold Price:",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB8973D)
                                    )
                                    Text(
                                        CurrencyFormatter.formatRupees(finalExchangePrice),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB8973D)
                                    )
                                }
                            }
                        }

                    // Error Message
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp),
                            elevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    error,
                                    color = Color(0xFFD32F2F),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(bottom = 24.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF666666)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE0E0E0))
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Button(
                            onClick = {
                                val totalWeight = totalProductWeight.toDoubleOrNull() ?: 0.0
                                val percent = percentage.toDoubleOrNull() ?: 0.0
                                val weight = goldWeight.toDoubleOrNull() ?: 0.0
                                
                                if (totalWeight <= 0) {
                                    errorMessage = "Please enter a valid total product weight"
                                    return@Button
                                }
                                if (percent <= 0 || percent > 100) {
                                    errorMessage = "Please enter a valid percentage (0-100)"
                                    return@Button
                                }
                                if (weight <= 0) {
                                    errorMessage = "Please ensure total product weight and percentage are entered to calculate gold weight"
                                    return@Button
                                }
                                if (selectedPurity.isEmpty()) {
                                    errorMessage = "Please select purity"
                                    return@Button
                                }
                                if (goldMaterialId.isEmpty()) {
                                    errorMessage = "Gold material not found. Please contact administrator."
                                    return@Button
                                }
                                if (goldRate <= 0) {
                                    if (isRateAutoFetched) {
                                    errorMessage = "Unable to fetch gold rate. Please check purity selection."
                                    } else {
                                        errorMessage = "Please enter rate for the selected purity."
                                    }
                                    return@Button
                                }

                                // Create ExchangeGold object with simplified data
                                // Store purity in description field for later extraction
                                val exchangeGold = ExchangeGold(
                                    name = productName.ifEmpty { "Exchange Gold" },
                                    grossWeight = weight,
                                    rate = goldRate,
                                    value = finalExchangePrice,
                                    type = "GOLD",
                                    description = selectedPurity, // Store purity directly in description
                                    totalProductWeight = totalWeight, // Store total product weight
                                    percentage = percent // Store percentage
                                )

                                onExchangeGoldComplete(exchangeGold)
                                errorMessage = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFB8973D)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.elevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Add Exchange",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
