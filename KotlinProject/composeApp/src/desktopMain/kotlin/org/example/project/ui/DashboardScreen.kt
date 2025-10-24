package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.RadioButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.utils.ImageLoader
import org.example.project.data.Product
import org.example.project.data.GroupedProduct
import org.example.project.data.MetalRatesManager
import org.example.project.viewModels.ProductsViewModel
import org.jetbrains.skia.Image
import java.text.NumberFormat
import java.util.Locale
import org.example.project.JewelryAppInitializer

@Composable
fun DashboardScreen(
    viewModel: ProductsViewModel,
    imageLoader: ImageLoader,
    onAddProduct: () -> Unit,
    onViewProductDetails: (String) -> Unit,
    onEditBarcode: (String) -> Unit = {},
    onDuplicateProduct: (String, String) -> Unit = { _, _ -> }
) {
    val products by remember { viewModel.products }
    val groupedProducts = remember(products) { viewModel.getGroupedProducts() }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val productToDelete = remember { mutableStateOf<String?>(null) }
    val showDuplicateDialog = remember { mutableStateOf(false) }
    val productToDuplicate = remember { mutableStateOf<String?>(null) }
    
    // Add logging to verify dashboard shows products with 0 quantity
    println("📊 DEBUG: Dashboard screen loaded")
    println("   - Total products: ${products.size}")
    println("   - Grouped products: ${groupedProducts.size}")
    groupedProducts.forEach { groupedProduct ->
        println("   📦 Product: ${groupedProduct.baseProduct.name}")
        println("      - Quantity: ${groupedProduct.quantity}")
        println("      - Available: ${groupedProduct.baseProduct.available}")
        if (groupedProduct.quantity == 0) {
            println("      - ⚠️ Product has 0 quantity but still displayed in dashboard")
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header with title and add button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                "Products",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onAddProduct,
                colors = ButtonDefaults.buttonColors(MaterialTheme.colors.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add New Product")
            }
        }

        // Search bar
        OutlinedTextField(
            value = "",
            onValueChange = {  },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Search products...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        // Products table
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
        ) {
            // Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Image", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Name", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                Text("Category", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text("Material", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text("Price", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                Text("Available", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Quantity", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text("Actions", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }

            Divider()

            // Table content
            if (groupedProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No products found. Add a new product to get started.",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn {
                    items(groupedProducts) { groupedProduct ->
                        GroupedProductRow(
                            groupedProduct = groupedProduct,
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onDelete = {
                                // For grouped products, delete all products in the group
                                productToDelete.value = groupedProduct.baseProduct.id
                                showDeleteDialog.value = true
                            },
                            onClick = { 
                                // For grouped products, show details of the first product
                                onViewProductDetails(groupedProduct.baseProduct.id) 
                            },
                            onEditBarcode = onEditBarcode,
                            onDuplicateProduct = { productId ->
                                productToDuplicate.value = productId
                                showDuplicateDialog.value = true
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this product? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete.value?.let { viewModel.deleteProduct(it) }
                        showDeleteDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Duplicate product dialog
    if (showDuplicateDialog.value) {
        DuplicateProductDialog(
            productId = productToDuplicate.value ?: "",
            viewModel = viewModel,
            onDismiss = { 
                showDuplicateDialog.value = false
                productToDuplicate.value = null
            },
            onConfirm = { barcodeId ->
                showDuplicateDialog.value = false
                productToDuplicate.value?.let { productId ->
                    onDuplicateProduct(productId, barcodeId)
                }
                productToDuplicate.value = null
            }
        )
    }
}

@Composable
private fun GroupedProductRow(
    groupedProduct: GroupedProduct,
    viewModel: ProductsViewModel,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEditBarcode: (String) -> Unit,
    onDuplicateProduct: (String) -> Unit
) {
    val product = groupedProduct.baseProduct
    var productImage by remember { mutableStateOf<ImageBitmap?>(null) }

    // Load product image
    LaunchedEffect(product.images) {
        if (product.images.isNotEmpty()) {
            try {
                val imageBytes = imageLoader.loadImage(product.images.first())
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    productImage = withContext(Dispatchers.IO) {
                        Image.makeFromEncoded(imageBytes).toComposeImageBitmap()
                    }
                }
            } catch (e: Exception) {
                println("Failed to load product image: ${e.message}")
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product image
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (productImage != null) {
                Image(
                    bitmap = productImage!!,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        // Name
        Text(
            text = product.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f)
        )

        // Category
        Text(
            text = viewModel.getCategoryName(product.categoryId),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f)
        )

        // Material
        Text(
            text = viewModel.getMaterialName(product.materialId),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f)
        )

        // Price - Show custom price if enabled, otherwise calculated total cost
        val displayPrice = if (product.hasCustomPrice) {
            product.customPrice
        } else if (product.totalProductCost > 0) {
            product.totalProductCost
        } else {
            calculateProductTotalCost(product)
        }
        Text(
            text = "₹${formatCurrency(displayPrice)}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f)
        )

        // Availability status
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 30.dp)
                    .background(
                        if (product.available) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (product.available) "Yes" else "No",
                    color = if (product.available) Color(0xFF388E3C) else Color(0xFFD32F2F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Quantity - Show dropdown with all barcodes
        Box(
            modifier = Modifier.weight(1.5f),
            contentAlignment = Alignment.CenterStart
        ) {
            var expanded by remember { mutableStateOf(false) }
            
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${groupedProduct.quantity}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Show barcodes",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 180.dp, max = 200.dp)
            ) {
                Text(
                    text = "Barcodes (${groupedProduct.barcodeIds.size})",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Divider()
                groupedProduct.barcodeIds.forEach { barcode ->
                    DropdownMenuItem(
                        onClick = { expanded = false }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = barcode,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    expanded = false
                                    onEditBarcode(barcode)
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit barcode $barcode",
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Actions
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duplicate button
                IconButton(
                    onClick = { onDuplicateProduct(product.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Duplicate Product",
                        tint = MaterialTheme.colors.primary
                    )
                }
                
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductRow(
    product: Product,
    viewModel: ProductsViewModel,
    imageLoader: ImageLoader,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var productImage by remember { mutableStateOf<ImageBitmap?>(null) }

    // Load the first image if available
    LaunchedEffect(product.images) {
        if (product.images.isNotEmpty()) {
            val imageUrl = product.images.first()
            coroutineScope.launch {
                val imageBytes = imageLoader.loadImage(imageUrl)
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    try {
                        val image = withContext(Dispatchers.IO) {
                            Image.makeFromEncoded(imageBytes).toComposeImageBitmap()
                        }
                        productImage = image
                    } catch (e: Exception) {
                        println("Failed to decode image: $imageUrl - ${e.message}")
                        // Keep null to show placeholder
                    }
                } else {
                    println("Failed to load image: $imageUrl - no data or empty data")
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product image
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (productImage != null) {
                Image(
                    bitmap = productImage!!,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        // Name
        Text(
            text = product.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f)
        )

        // Category
        Text(
            text = viewModel.getCategoryName(product.categoryId),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f)
        )

        // Material
        Text(
            text = viewModel.getMaterialName(product.materialId),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f)
        )

        // Price - Show custom price if enabled, otherwise calculated total cost
        val displayPrice = if (product.hasCustomPrice) {
            product.customPrice
        } else if (product.totalProductCost > 0) {
            product.totalProductCost
        } else {
            calculateProductTotalCost(product)
        }
        Text(
            text = "₹${formatCurrency(displayPrice)}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f)
        )

        // Availability status
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 30.dp)
                    .background(
                        if (product.available) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (product.available) "Yes" else "No",
                    color = if (product.available) Color(0xFF388E3C) else Color(0xFFD32F2F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Quantity with out of stock indicator above controls
        Box(
            modifier = Modifier.weight(1.5f),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Out of stock indicator (only show when quantity is 0)
                if (product.quantity == 0) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Out of Stock",
                            fontSize = 9.sp,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Quantity controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Decrease button
                    IconButton(
                        onClick = {
                            if (product.quantity > 0) {
                                viewModel.updateProductQuantity(product.id, product.quantity - 1)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text(
                            "−",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }

                    // Quantity display
                    Text(
                        text = product.quantity.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.widthIn(min = 20.dp),
                        textAlign = TextAlign.Center
                    )

                    // Increase button
                    IconButton(
                        onClick = {
                            viewModel.updateProductQuantity(product.id, product.quantity + 1)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text(
                            "+",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }

        // Action buttons
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFFFEBEE), CircleShape)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFD32F2F)
                )
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

/**
 * Calculate the total product cost using the same logic as cart item detail screen (total charges)
 * This returns Total Charges (without GST) to match the item detail screen display
 */
private fun calculateProductTotalCost(product: Product): Double {
    // Calculate net weight (total weight - less weight)
    val netWeight = (product.totalWeight - product.lessWeight).coerceAtLeast(0.0)
    
    // Material cost (net weight × material rate × quantity)
    val materialRate = getMaterialRateForProduct(product)
    val baseAmount = when {
        product.materialType.contains("gold", ignoreCase = true) -> netWeight * materialRate * product.quantity
        product.materialType.contains("silver", ignoreCase = true) -> netWeight * materialRate * product.quantity
        else -> netWeight * materialRate * product.quantity // Default to gold rate
    }
    
    // Making charges (net weight × making rate × quantity)
    val makingCharges = netWeight * product.defaultMakingRate * product.quantity
    
    // Stone amount (if has stones) - STONE_RATE × STONE_QUANTITY × CW_WT × QTY
    val stoneAmount = if (product.hasStones) {
        if (product.cwWeight > 0 && product.stoneRate > 0) {
            product.stoneRate * (product.stoneQuantity.takeIf { it > 0 } ?: 1.0) * product.cwWeight * product.quantity
        } else 0.0
    } else 0.0
    
    // Total Charges = Base Amount + Making Charges + Stone Amount + VA Charges
    // This matches the "Total Charges" display in cart item detail screen
    return baseAmount + makingCharges + stoneAmount + product.vaCharges
}

/**
 * Get material rate for a product based on material and type
 * Uses MetalRatesManager to get current rates from Firestore
 */
private fun getMaterialRateForProduct(product: Product): Double {
    val metalRates = MetalRatesManager.metalRates.value
    // Prefer collection rate from rate view model (same as cart detail)
    val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
    val karat = extractKaratFromMaterialTypeString(product.materialType)
    val collectionRate = try {
        ratesVM.calculateRateForMaterial(product.materialId, product.materialType, karat)
    } catch (e: Exception) { 0.0 }

    if (collectionRate > 0) return collectionRate

    return when {
        product.materialType.contains("gold", ignoreCase = true) -> metalRates.getGoldRateForKarat(karat)
        product.materialType.contains("silver", ignoreCase = true) -> metalRates.getSilverRateForPurity(999)
        else -> metalRates.getGoldRateForKarat(22)
    }
}

/**
 * Extract karat from material type string (e.g., "18K Gold" -> 18)
 */
private fun extractKaratFromMaterialTypeString(materialType: String): Int {
    val karatRegex = Regex("""(\d+)K""", RegexOption.IGNORE_CASE)
    val match = karatRegex.find(materialType)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: 22 // Default to 22K
}

@Composable
private fun DuplicateProductDialog(
    productId: String,
    viewModel: ProductsViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val product = viewModel.products.value.find { it.id == productId }
    var barcodeType by remember { mutableStateOf("auto") } // "auto" or "custom"
    var customBarcodeId by remember { mutableStateOf("") }
    var barcodeDigits by remember { mutableStateOf("12") }
    
    if (product == null) {
        onDismiss()
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duplicate Product") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Create a new product document with the same details as '${product.name}'")
                
                // Barcode type selection
                Text("Barcode ID Type:", fontWeight = FontWeight.Medium)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = barcodeType == "auto",
                        onClick = { barcodeType = "auto" }
                    )
                    Text("Auto-generated")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = barcodeType == "custom",
                        onClick = { barcodeType = "custom" }
                    )
                    Text("Custom")
                }
                
                // Custom barcode input
                if (barcodeType == "custom") {
                    OutlinedTextField(
                        value = customBarcodeId,
                        onValueChange = { customBarcodeId = it },
                        label = { Text("Custom Barcode ID") },
                        placeholder = { Text("Enter custom barcode") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                
                // Auto-generated barcode digits
                if (barcodeType == "auto") {
                    OutlinedTextField(
                        value = barcodeDigits,
                        onValueChange = { barcodeDigits = it },
                        label = { Text("Barcode Digits") },
                        placeholder = { Text("12") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalBarcodeId = when (barcodeType) {
                        "custom" -> customBarcodeId.trim()
                        "auto" -> {
                            val digits = barcodeDigits.toIntOrNull() ?: 12
                            viewModel.generateBarcode(digits)
                        }
                        else -> viewModel.generateBarcode(12)
                    }
                    onConfirm(finalBarcodeId)
                },
                enabled = when (barcodeType) {
                    "custom" -> customBarcodeId.trim().isNotEmpty()
                    "auto" -> barcodeDigits.toIntOrNull() != null
                    else -> false
                }
            ) {
                Text("Create Duplicate")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}