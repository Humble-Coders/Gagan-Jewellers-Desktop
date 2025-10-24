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
    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Form state - initialize with empty values
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var totalWeight by remember { mutableStateOf("") }
    var defaultMakingRate by remember { mutableStateOf("") }
    var isOtherThanGold by remember { mutableStateOf(false) }
    var lessWeight by remember { mutableStateOf("") }
    var hasStones by remember { mutableStateOf(false) }
    var stoneName by remember { mutableStateOf("") }
    var stoneQuantity by remember { mutableStateOf("") }
    var stoneRate by remember { mutableStateOf("") }
    var cwWeight by remember { mutableStateOf("") }
    var vaCharges by remember { mutableStateOf("") }
    var available by remember { mutableStateOf(true) }
    var featured by remember { mutableStateOf(false) }
    var hasCustomPrice by remember { mutableStateOf(false) }
    var customPrice by remember { mutableStateOf("") }

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
                defaultMakingRate = if (fetchedProduct.defaultMakingRate > 0) fetchedProduct.defaultMakingRate.toString() else ""
                isOtherThanGold = fetchedProduct.isOtherThanGold
                lessWeight = if (fetchedProduct.lessWeight > 0) fetchedProduct.lessWeight.toString() else ""
                hasStones = fetchedProduct.hasStones
                stoneName = fetchedProduct.stoneName
                stoneQuantity = if (fetchedProduct.stoneQuantity > 0) fetchedProduct.stoneQuantity.toString() else ""
                stoneRate = if (fetchedProduct.stoneRate > 0) fetchedProduct.stoneRate.toString() else ""
                cwWeight = if (fetchedProduct.cwWeight > 0) fetchedProduct.cwWeight.toString() else ""
                vaCharges = if (fetchedProduct.vaCharges > 0) fetchedProduct.vaCharges.toString() else ""
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
                            onValueChange = { name = it },
                            label = "Product Name",
                            placeholder = "e.g., Gold Necklace"
                        )

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
                            onValueChange = { totalWeight = it },
                            label = "Total Weight",
                            placeholder = "0.00",
                            suffix = "grams",
                            keyboardType = KeyboardType.Decimal
                        )

                        StyledTextField(
                            value = defaultMakingRate,
                            onValueChange = { defaultMakingRate = it },
                            label = "Making Rate",
                            placeholder = "0.00",
                            prefix = "₹",
                            suffix = "/gram",
                            keyboardType = KeyboardType.Decimal
                        )

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
                            StyledTextField(
                                value = lessWeight,
                                onValueChange = { lessWeight = it },
                                label = "Less Weight",
                                placeholder = "0.00",
                                suffix = "grams",
                                keyboardType = KeyboardType.Decimal
                            )

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
                                    onValueChange = { stoneName = it },
                                    label = "Stone Name",
                                    placeholder = "e.g., Diamond"
                                )

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

                                StyledTextField(
                                    value = cwWeight,
                                    onValueChange = { cwWeight = it },
                                    label = "CW Weight",
                                    placeholder = "0.00",
                                    suffix = "carats",
                                    keyboardType = KeyboardType.Decimal
                                )
                            }
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
                                onValueChange = { customPrice = it },
                                label = "Custom Price",
                                placeholder = "0.00",
                                prefix = "₹",
                                keyboardType = KeyboardType.Decimal
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

                        StatusToggle(
                            title = "Featured Product",
                            description = "Highlight on homepage and promotions",
                            checked = featured,
                            onCheckedChange = { featured = it }
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
                            if (product != null) {
                                isSaving = true
                                scope.launch {
                                    try {
                                        val updatedProduct = product!!.copy(
                                            name = name,
                                            description = description,
                                            totalWeight = totalWeight.toDoubleOrNull() ?: 0.0,
                                            defaultMakingRate = defaultMakingRate.toDoubleOrNull() ?: 0.0,
                                            isOtherThanGold = isOtherThanGold,
                                            lessWeight = lessWeight.toDoubleOrNull() ?: 0.0,
                                            hasStones = hasStones,
                                            stoneName = stoneName,
                                            stoneQuantity = stoneQuantity.toDoubleOrNull() ?: 0.0,
                                            stoneRate = stoneRate.toDoubleOrNull() ?: 0.0,
                                            cwWeight = cwWeight.toDoubleOrNull() ?: 0.0,
                                            vaCharges = vaCharges.toDoubleOrNull() ?: 0.0,
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
                                        isSaving = false
                                    }
                                }
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
    enabled: Boolean = true
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
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = MaterialTheme.colors.primary,
                focusedLabelColor = MaterialTheme.colors.primary
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}


