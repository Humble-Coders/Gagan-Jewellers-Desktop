package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.JewelryAppInitializer
import org.example.project.data.Product
import org.example.project.viewModels.ProductsViewModel

@Composable
fun AddEditProductScreen(
    viewModel: ProductsViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isEditing: Boolean = false
) {
    val product = viewModel.currentProduct.value ?: Product()
    val categories by remember { viewModel.categories }
    val materials by remember { viewModel.materials }
    val imageLoader = JewelryAppInitializer.getImageLoader()

    // Form state
    var name by remember { mutableStateOf(product.name) }
    var description by remember { mutableStateOf(product.description) }
    var price by remember { mutableStateOf(product.price.toString()) }
    var categoryId by remember { mutableStateOf(product.categoryId) }
    var materialId by remember { mutableStateOf(product.materialId) }
    var materialType by remember { mutableStateOf(product.materialType) }
    var gender by remember { mutableStateOf(product.gender) }
    var weight by remember { mutableStateOf(product.weight) }
    var available by remember { mutableStateOf(product.available) }
    var featured by remember { mutableStateOf(product.featured) }
    var images by remember { mutableStateOf(product.images) }

    // Validation state
    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }
    var materialError by remember { mutableStateOf(false) }

    // Expanded dropdown states
    var categoryExpanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }
    var materialTypeExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(product) {
        if (isEditing) {
            println("Editing product: ${product.id} - ${product.name}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            if (isEditing) "Edit Product" else "Add New Product",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isEmpty()
                    },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError,
                    singleLine = true
                )
                if (nameError) {
                    Text(
                        "Product name is required",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )

                // Price
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        price = it
                        priceError = try {
                            it.toDouble() <= 0
                        } catch (e: NumberFormatException) {
                            print(e)
                            true
                        }
                    },
                    label = { Text("Price (Rs)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceError,
                    singleLine = true
                )
                if (priceError) {
                    Text(
                        "Please enter a valid price",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                // Category dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = categories.find { it.id == categoryId }?.name ?: "",
                        onValueChange = { },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { categoryExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Category")
                            }
                        },
                        readOnly = true,
                        isError = categoryError
                    )

                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(onClick = {
                                categoryId = category.id
                                categoryExpanded = false
                                categoryError = false
                            }) {
                                Text(category.name)
                            }
                        }
                    }
                }
                if (categoryError) {
                    Text(
                        "Category is required",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                // Material dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = materials.find { it.id == materialId }?.name ?: "",
                        onValueChange = { },
                        label = { Text("Material") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { materialExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Material")
                            }
                        },
                        readOnly = true,
                        isError = materialError
                    )

                    DropdownMenu(
                        expanded = materialExpanded,
                        onDismissRequest = { materialExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        materials.forEach { material ->
                            DropdownMenuItem(onClick = {
                                materialId = material.id
                                materialType = "" // Reset material type when material changes
                                materialExpanded = false
                                materialError = false
                            }) {
                                Text(material.name)
                            }
                        }
                    }
                }
                if (materialError) {
                    Text(
                        "Material is required",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                // Material Type dropdown (only show if material is selected)
                if (materialId.isNotEmpty()) {
                    val selectedMaterial = materials.find { it.id == materialId }
                    val materialTypes = selectedMaterial?.types ?: emptyList()

                    if (materialTypes.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = materialType,
                                onValueChange = { },
                                label = { Text("Material Type") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { materialTypeExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Material Type")
                                    }
                                },
                                readOnly = true
                            )

                            DropdownMenu(
                                expanded = materialTypeExpanded,
                                onDismissRequest = { materialTypeExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                materialTypes.forEach { type ->
                                    DropdownMenuItem(onClick = {
                                        materialType = type
                                        materialTypeExpanded = false
                                    }) {
                                        Text(type)
                                    }
                                }
                            }
                        }
                    }
                }

                // Gender dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = { },
                        label = { Text("Gender") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { genderExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Gender")
                            }
                        },
                        readOnly = true
                    )

                    DropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        listOf("Men", "Women", "Unisex").forEach { genderOption ->
                            DropdownMenuItem(onClick = {
                                gender = genderOption
                                genderExpanded = false
                            }) {
                                Text(genderOption)
                            }
                        }
                    }
                }

                // Weight
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (e.g. 8.5g)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Availability switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available", modifier = Modifier.weight(1f))
                    Switch(
                        checked = available,
                        onCheckedChange = { available = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
                    )
                }

                // Featured switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Featured", modifier = Modifier.weight(1f))
                    Switch(
                        checked = featured,
                        onCheckedChange = { featured = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Product Image Manager Component
                ProductImageManager(
                    existingImages = images,
                    onImagesChanged = { updatedImages ->
                        images = updatedImages
                    },
                    imageLoader = imageLoader,
                    productId = product.id // Pass product ID for better organization in storage
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Form buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            // Validate form
                            nameError = name.isEmpty()
                            priceError = try {
                                price.toDouble() <= 0
                            } catch (e: NumberFormatException) {
                                print(e)
                                true
                            }
                            categoryError = categoryId.isEmpty()
                            materialError = materialId.isEmpty()

                            if (!nameError && !priceError && !categoryError && !materialError) {
                                // Create updated product
                                val updatedProduct = Product(
                                    id = product.id,
                                    name = name,
                                    description = description,
                                    price = price.toDoubleOrNull() ?: 0.0,
                                    categoryId = categoryId,
                                    materialId = materialId,
                                    materialType = materialType,
                                    gender = gender,
                                    weight = weight,
                                    available = available,
                                    featured = featured,
                                    images = images,
                                    createdAt = product.createdAt
                                )

                                if (isEditing) {
                                    viewModel.updateProduct(updatedProduct)
                                } else {
                                    viewModel.addProduct(updatedProduct)
                                }

                                onSave()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                    ) {
                        Text(if (isEditing) "Save Changes" else "Add Product", color = Color.White)
                    }
                }
            }
        }
    }
}