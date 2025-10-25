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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.utils.ImageLoader
import org.example.project.viewModels.ProductsViewModel
import org.example.project.data.Product
import org.example.project.data.MetalRatesManager
import org.example.project.JewelryAppInitializer
import org.jetbrains.skia.Image
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProductDetailScreen(
    viewModel: ProductsViewModel,
    imageLoader: ImageLoader,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val product = viewModel.currentProduct.value
    val coroutineScope = rememberCoroutineScope()
    var productImages by remember { mutableStateOf<List<Pair<String, ImageBitmap?>>>(emptyList()) }
    var selectedImageIndex by remember { mutableStateOf(0) }

    // Load all product images
    LaunchedEffect(product) {
        product?.let { p ->
            if (p.images.isNotEmpty()) {
                // Initialize the list with null images first
                productImages = p.images.map { it to null }

                // Load each image asynchronously
                p.images.forEachIndexed { index, imageUrl ->
                    coroutineScope.launch {
                        val imageBytes = imageLoader.loadImage(imageUrl)
                        if (imageBytes != null && imageBytes.isNotEmpty()) {
                            try {
                                val image = withContext(Dispatchers.IO) {
                                    Image.makeFromEncoded(imageBytes).toComposeImageBitmap()
                                }
                                // Update just this image in the list
                                val updatedList = productImages.toMutableList()
                                if (index < updatedList.size) {
                                    updatedList[index] = imageUrl to image
                                    productImages = updatedList
                                }
                            } catch (e: Exception) {
                                println("Failed to decode image: $imageUrl - ${e.message}")
                                // Keep the null image to show placeholder
                            }
                        } else {
                            println("Failed to load image: $imageUrl - no data or empty data")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Product Details",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(MaterialTheme.colors.primary)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit Product")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display product info or placeholder if not available
        product?.let { p ->
            // Product Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Product images with image gallery
                    if (p.images.isNotEmpty()) {
                        // Main selected image with navigation arrows
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp)
                                .background(Color(0xFFF5F5F5))
                                .border(1.dp, Color(0xFFEEEEEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            val currentImage = if (productImages.isNotEmpty() &&
                                selectedImageIndex < productImages.size) {
                                productImages[selectedImageIndex].second
                            } else null

                            if (currentImage != null) {
                                Image(
                                    bitmap = currentImage,
                                    contentDescription = "Product Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                CircularProgressIndicator()
                            }

                            // Add navigation arrows (only if there are multiple images)
                            if (productImages.size > 1) {
                                // Left arrow
                                IconButton(
                                    onClick = {
                                        selectedImageIndex = if (selectedImageIndex > 0)
                                            selectedImageIndex - 1
                                        else
                                            productImages.size - 1
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .size(48.dp)
                                        .background(
                                            color = Color(0x80000000),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous image",
                                        tint = Color.White
                                    )
                                }

                                // Right arrow
                                IconButton(
                                    onClick = {
                                        selectedImageIndex = if (selectedImageIndex < productImages.size - 1)
                                            selectedImageIndex + 1
                                        else
                                            0
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(48.dp)
                                        .background(
                                            color = Color(0x80000000),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next image",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Image pagination indicators
                        if (productImages.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                for (i in productImages.indices) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(10.dp)
                                            .background(
                                                color = if (i == selectedImageIndex)
                                                    MaterialTheme.colors.primary
                                                else
                                                    Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedImageIndex = i }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Thumbnail row
                        if (productImages.size > 1) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(productImages.size) { index ->
                                    val (_, image) = productImages[index]
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFF5F5F5))
                                            .border(
                                                width = if (index == selectedImageIndex) 2.dp else 1.dp,
                                                color = if (index == selectedImageIndex)
                                                    MaterialTheme.colors.primary else Color(0xFFEEEEEE),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable { selectedImageIndex = index },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (image != null) {
                                            Image(
                                                bitmap = image,
                                                contentDescription = "Thumbnail ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    } else {
                        // No images placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color(0xFFF5F5F5))
                                .border(1.dp, Color(0xFFEEEEEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No images available", color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Product name and price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = p.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "₹${formatCurrency(calculateProductTotalCost(p))}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status indicators (Available, Featured)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Available indicator
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(
                                    if (p.available) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (p.available) "In Stock" else "Out of Stock",
                                color = if (p.available) Color(0xFF388E3C) else Color(0xFFD32F2F),
                                fontSize = 14.sp
                            )
                        }

                        // Featured indicator
                        if (p.featured) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .background(
                                        Color(0xFFFFF8E1),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Featured",
                                    color = Color(0xFFFFA000),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Product details section
                    Text(
                        text = "Product Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Product attributes in a grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailRow("Category", viewModel.getCategoryName(p.categoryId))
                        DetailRow("Material", viewModel.getMaterialName(p.materialId))
                        if (p.materialType.isNotEmpty()) {
                            DetailRow("Material Type", p.materialType)
                        }
                        if (p.gender.isNotEmpty()) {
                            DetailRow("Gender", p.gender)
                        }
                        if (p.weight.isNotEmpty()) {
                            DetailRow("Weight", p.weight)
                        }
                        DetailRow("Product ID", p.id)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Description section
                    Text(
                        text = "Description",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = p.description.ifEmpty { "No description available" },
                        color = if (p.description.isEmpty()) Color.Gray else Color.Unspecified
                    )
                }
            }
        } ?: run {
            // Placeholder if product is null
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Product not found or still loading...",
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            color = Color.DarkGray
        )
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
    
    // Stone amount (if has stones) - CW_WT × STONE_RATE × QTY
    val stoneAmount = if (product.hasStones) {
        if (product.cwWeight > 0 && product.stoneRate > 0) {
            product.cwWeight * product.stoneRate * product.quantity
        } else 0.0
    } else 0.0
    
    // Total Charges = Base Amount + Making Charges + Stone Amount + VA Charges
    // This matches the "Total Charges" display in cart item detail screen
    return baseAmount + makingCharges + stoneAmount + product.vaCharges
}

/**
 * Get material rate for a product based on material and type
 * Uses price_per_gram from rates collection to align with cart screen calculation
 */
private fun getMaterialRateForProduct(product: Product): Double {
    val metalRates = MetalRatesManager.metalRates.value
    // Use collection rate from rate view model (same as cart detail screen)
    val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
    val karat = extractKaratFromMaterialTypeString(product.materialType)
    
    println("💰 PRODUCT DETAIL RATE CALCULATION for ${product.name}:")
    println("   - Material ID: ${product.materialId}")
    println("   - Material Type: ${product.materialType}")
    println("   - Karat: $karat")
    println("   - Total Metal Rates Available: ${ratesVM.metalRates.value.size}")
    
    // Get price_per_gram from rates collection (same as cart screen)
    val collectionRate = try {
        val rate = ratesVM.calculateRateForMaterial(product.materialId, product.materialType, karat)
        println("   - Collection Rate: $rate")
        rate
    } catch (e: Exception) { 
        println("   - Collection Rate Error: ${e.message}")
        0.0 
    }

    // Always prefer collection rate if available, otherwise fallback to metal rates
    if (collectionRate > 0) {
        println("   - Using Collection Rate: $collectionRate")
        return collectionRate
    }

    // Fallback to metal rates if collection rate not available
    val fallbackRate = when {
        product.materialType.contains("gold", ignoreCase = true) -> metalRates.getGoldRateForKarat(karat)
        product.materialType.contains("silver", ignoreCase = true) -> metalRates.getSilverRateForPurity(999)
        else -> metalRates.getGoldRateForKarat(22)
    }
    println("   - Using Fallback Rate: $fallbackRate")
    return fallbackRate
}

/**
 * Extract karat from material type string (e.g., "18K Gold" -> 18)
 */
private fun extractKaratFromMaterialTypeString(materialType: String): Int {
    val karatRegex = Regex("""(\d+)K""", RegexOption.IGNORE_CASE)
    val match = karatRegex.find(materialType)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: 22 // Default to 22K
}