package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.data.Product
import org.example.project.viewModels.ProductsViewModel

@Composable
fun BarcodeEditScreen(
    barcodeId: String,
    viewModel: ProductsViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    println("✏️ BARCODE EDIT SCREEN OPENED")
    println("   - Barcode ID: $barcodeId")
    println("   - Timestamp: ${System.currentTimeMillis()}")
    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Form state - initialize with empty values
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var totalWeight by remember { mutableStateOf("") }
    var hasStones by remember { mutableStateOf(false) }
    var stoneName by remember { mutableStateOf("") }
    var stoneQuantity by remember { mutableStateOf("") }
    var stoneRate by remember { mutableStateOf("") }
    var available by remember { mutableStateOf(true) }
    var featured by remember { mutableStateOf(false) }
    var hasCustomPrice by remember { mutableStateOf(false) }
    var customPrice by remember { mutableStateOf("") }

    // Validation state
    var nameError by remember { mutableStateOf(false) }
    var totalWeightError by remember { mutableStateOf(false) }
    var stoneNameError by remember { mutableStateOf(false) }
    var customPriceError by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Load product data when screen opens
    LaunchedEffect(barcodeId) {
        isLoading = true
        try {
            val fetchedProduct = viewModel.getProductByBarcodeId(barcodeId)
            if (fetchedProduct != null) {
                product = fetchedProduct
                // Populate form fields
                name = fetchedProduct.name
                description = fetchedProduct.description ?: ""
                totalWeight = if (fetchedProduct.totalWeight > 0) fetchedProduct.totalWeight.toString() else ""
                hasStones = fetchedProduct.hasStones
                val firstStone = fetchedProduct.stones.firstOrNull()
                stoneName = firstStone?.name ?: ""
                stoneQuantity = if ((firstStone?.quantity ?: 0.0) > 0) firstStone?.quantity.toString() ?: "" else ""
                stoneRate = if ((firstStone?.rate ?: 0.0) > 0) firstStone?.rate.toString() ?: "" else ""
                available = fetchedProduct.available
                featured = fetchedProduct.featured
                hasCustomPrice = fetchedProduct.hasCustomPrice
                customPrice = if (fetchedProduct.customPrice > 0) fetchedProduct.customPrice.toString() else ""
            }
        } catch (e: Exception) {
            println("Error loading product for barcode $barcodeId: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (product == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Product not found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Text(
                        "No product found with barcode: $barcodeId",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Button(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            }
        } else {
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
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colors.primary
                            )
                        }
                        Column {
                            Text(
                                "Edit Product Document",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D2D2D)
                            )
                            Text(
                                "Barcode: $barcodeId",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
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
                                nameError = false  // Clear error when user types
                            },
                            label = "Product Name *",
                            placeholder = "e.g., Gold Necklace",
                            isError = nameError
                        )
                        if (nameError) {
                            Text(
                                "Product name is required",
                                color = MaterialTheme.colors.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }

                        StyledTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "Description",
                            placeholder = "Enter product details...",
                            singleLine = false,
                            maxLines = 3
                        )
                    }
                }

                // Weight & Specifications
                SectionCard(title = "Weight & Specifications") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StyledTextField(
                            value = totalWeight,
                            onValueChange = { 
                                totalWeight = it
                                totalWeightError = false  // Clear error when user types
                            },
                            label = "Total Weight",
                            placeholder = "0.00",
                            suffix = "grams",
                            keyboardType = KeyboardType.Decimal,
                            isError = totalWeightError
                        )
                        if (totalWeightError) {
                            Text(
                                "Total weight must be a valid number",
                                color = MaterialTheme.colors.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }


                    }
                }

                // Additional Components
                SectionCard(title = "Additional Components") {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
                                StyledTextField(
                                    value = stoneName,
                                    onValueChange = { 
                                        stoneName = it
                                        stoneNameError = false  // Clear error when user types
                                    },
                                    label = "Stone Name *",
                                    placeholder = "e.g., Diamond",
                                    isError = stoneNameError
                                )
                                if (stoneNameError) {
                                    Text(
                                        "Stone name is required when stones are enabled",
                                        color = MaterialTheme.colors.error,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                    )
                                }

                                StyledTextField(
                                    value = stoneQuantity,
                                    onValueChange = { stoneQuantity = it },
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
                        }
                    }
                }

                // Pricing Information
                SectionCard(title = "Pricing Information") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = hasCustomPrice,
                                onCheckedChange = { hasCustomPrice = it }
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
                                onValueChange = { 
                                    customPrice = it
                                    customPriceError = false  // Clear error when user types
                                },
                                label = "Custom Price *",
                                placeholder = "0.00",
                                prefix = "₹",
                                keyboardType = KeyboardType.Decimal,
                                isError = customPriceError
                            )
                            if (customPriceError) {
                                Text(
                                    "Custom price must be a valid number greater than 0",
                                    color = MaterialTheme.colors.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
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

                        StatusToggle(
                            title = "Featured Product",
                            description = "Highlight on homepage and promotions",
                            checked = featured,
                            onCheckedChange = { featured = it }
                        )
                    }
                }

                // Validation error display
                validationError?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                        elevation = 2.dp
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colors.error,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
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
                        onClick = onBack,
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
                            val currentProduct = product ?: run {
                                validationError = "Product not found. Please go back and try again."
                                return@Button
                            }

                            // Validate form before saving
                            nameError = name.trim().isEmpty()
                            totalWeightError = totalWeight.isNotBlank() && totalWeight.toDoubleOrNull() == null
                            stoneNameError = hasStones && stoneName.trim().isEmpty()
                            customPriceError = hasCustomPrice && (customPrice.isBlank() || customPrice.toDoubleOrNull() == null || customPrice.toDoubleOrNull()!! <= 0)

                            val isValid = !nameError && !totalWeightError && !stoneNameError && !customPriceError

                            if (!isValid) {
                                validationError = when {
                                    nameError -> "Product name is required"
                                    totalWeightError -> "Total weight must be a valid number"
                                    stoneNameError -> "Stone name is required when stones are enabled"
                                    customPriceError -> "Custom price must be a valid number greater than 0"
                                    else -> "Please fill all required fields correctly"
                                }
                                return@Button
                            }

                            validationError = null
                            isSaving = true
                            scope.launch {
                                try {
                                    // Create stones array
                                    val stonesList = if (hasStones && stoneName.isNotBlank()) {
                                        listOf(
                                            org.example.project.data.ProductStone(
                                                name = stoneName,
                                                purity = "", // BarcodeEditScreen doesn't have purity field
                                                quantity = stoneQuantity.toDoubleOrNull() ?: 0.0,
                                                rate = stoneRate.toDoubleOrNull() ?: 0.0,
                                                weight = 0.0, // BarcodeEditScreen doesn't have weight field
                                                amount = 0.0 // Will be calculated if needed
                                            )
                                        )
                                    } else emptyList()
                                    
                                    val updatedProduct = currentProduct.copy(
                                        name = name.trim(),
                                        description = description.trim(),
                                        totalWeight = totalWeight.toDoubleOrNull() ?: 0.0,
                                        hasStones = hasStones,
                                        stones = stonesList,
                                        available = available,
                                        featured = featured,
                                        hasCustomPrice = hasCustomPrice,
                                        customPrice = customPrice.toDoubleOrNull() ?: 0.0
                                    )
                                    
                                    viewModel.updateProduct(updatedProduct)
                                    isSaving = false
                                    onSave()
                                } catch (e: Exception) {
                                    println("Error updating product: ${e.message}")
                                    validationError = "Failed to save: ${e.message}"
                                    isSaving = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = Color.White
                        ),
                        enabled = !isSaving && product != null,
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.elevation(4.dp)
                    ) {
                        Text(
                            if (isSaving) "Saving..." else "Save Changes",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
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
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    isError: Boolean = false
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
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            enabled = enabled,
            isError = isError,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = if (isError) MaterialTheme.colors.error else MaterialTheme.colors.primary,
                unfocusedBorderColor = if (isError) MaterialTheme.colors.error else Color(0xFFE0E0E0),
                cursorColor = MaterialTheme.colors.primary,
                focusedLabelColor = if (isError) MaterialTheme.colors.error else MaterialTheme.colors.primary,
                errorBorderColor = MaterialTheme.colors.error,
                errorLabelColor = MaterialTheme.colors.error
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}


