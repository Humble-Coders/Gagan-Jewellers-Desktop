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
import org.example.project.utils.CurrencyFormatter
import org.example.project.data.extractKaratFromMaterialType
import org.jetbrains.skia.Image

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
    val metalRatesList by MetalRatesManager.metalRates

    // Load all product images
    LaunchedEffect(product) {
        product?.let { p ->
            if (p.images.isNotEmpty()) {
                productImages = p.images.map { it to null }
                p.images.forEachIndexed { index, imageUrl ->
                    coroutineScope.launch {
                        val imageBytes = imageLoader.loadImage(imageUrl)
                        if (imageBytes != null && imageBytes.isNotEmpty()) {
                            try {
                                val image = withContext(Dispatchers.IO) {
                                    Image.makeFromEncoded(imageBytes).toComposeImageBitmap()
                                }
                                withContext(Dispatchers.Main) {
                                val updatedList = productImages.toMutableList()
                                if (index < updatedList.size) {
                                    updatedList[index] = imageUrl to image
                                    productImages = updatedList
                                    }
                                }
                            } catch (e: Exception) {
                                println("Failed to decode image: $imageUrl - ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
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
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(MaterialTheme.colors.primary)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit")
            }
        }

        // Display product info or placeholder if not available
        product?.let { p ->
            // Calculate price using dashboard method
            val dynamicPrice = remember(p.id, p.totalWeight, p.materialWeight, p.makingPercent, p.labourRate, p.stones, metalRatesList) {
                try {
                    val metalRates = MetalRatesManager.metalRates.value
                    val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
                    
                    // Extract kundan and jarkan from stones array
                    val kundanStones = p.stones.filter { it.name.equals("Kundan", ignoreCase = true) }
                    val jarkanStones = p.stones.filter { it.name.equals("Jarkan", ignoreCase = true) }
                    
                    // Sum all Kundan prices and weights
                    val kundanPrice = kundanStones.sumOf { it.amount }
                    val kundanWeight = kundanStones.sumOf { it.weight }
                    
                    // Sum all Jarkan prices and weights
                    val jarkanPrice = jarkanStones.sumOf { it.amount }
                    val jarkanWeight = jarkanStones.sumOf { it.weight }
                    
                    // Get material rate
                    val metalKarat = extractKaratFromMaterialType(p.materialType)
                    val collectionRate = try {
                        ratesVM.calculateRateForMaterial(p.materialId, p.materialType, metalKarat)
                    } catch (e: Exception) { 0.0 }
                    val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
                    
                    // Extract silver purity from material type
                    val materialTypeLower = p.materialType.lowercase()
                    val silverPurity = when {
                        materialTypeLower.contains("999") -> 999
                        materialTypeLower.contains("925") || materialTypeLower.contains("92.5") -> 925
                        materialTypeLower.contains("900") || materialTypeLower.contains("90.0") -> 900
                        else -> {
                            val threeDigits = Regex("(\\d{3})").find(materialTypeLower)?.groupValues?.getOrNull(1)?.toIntOrNull()
                            if (threeDigits != null && threeDigits in listOf(900, 925, 999)) threeDigits else 999
                        }
                    }
                    val silverRate = metalRates.getSilverRateForPurity(silverPurity)
                    val goldRatePerGram = if (collectionRate > 0) collectionRate else when {
                        p.materialType.contains("gold", ignoreCase = true) -> defaultGoldRate
                        p.materialType.contains("silver", ignoreCase = true) -> silverRate
                        else -> defaultGoldRate
                    }
                    
                    // Build ProductPriceInputs
                    val priceInputs = ProductPriceInputs(
                        grossWeight = p.totalWeight,
                        goldPurity = p.materialType,
                        goldWeight = p.materialWeight.takeIf { it > 0 } ?: p.totalWeight,
                        makingPercentage = p.makingPercent,
                        labourRatePerGram = p.labourRate,
                        kundanPrice = kundanPrice,
                        kundanWeight = kundanWeight,
                        jarkanPrice = jarkanPrice,
                        jarkanWeight = jarkanWeight,
                        goldRatePerGram = goldRatePerGram
                    )
                    
                    // Use the same calculation function as ProductPriceCalculator
                    val result = calculateProductPrice(priceInputs)
                    result.totalProductPrice
                } catch (e: Exception) {
                    println("Error calculating price: ${e.message}")
                    0.0
                }
            }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                // Product Images Section
                    if (p.images.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Main selected image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                    .height(400.dp)
                                .background(Color(0xFFF5F5F5))
                                    .clip(RoundedCornerShape(8.dp)),
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

                                // Navigation arrows
                            if (productImages.size > 1) {
                                IconButton(
                                    onClick = {
                                        selectedImageIndex = if (selectedImageIndex > 0)
                                            selectedImageIndex - 1
                                        else
                                            productImages.size - 1
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                            .background(Color(0x80000000), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous",
                                        tint = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        selectedImageIndex = if (selectedImageIndex < productImages.size - 1)
                                            selectedImageIndex + 1
                                        else
                                            0
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                            .background(Color(0x80000000), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                            // Image indicators
                        if (productImages.size > 1) {
                                Spacer(modifier = Modifier.height(12.dp))
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

                                Spacer(modifier = Modifier.height(12.dp))

                        // Thumbnail row
                            LazyRow(
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
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Product Name and Price Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color.White
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                            Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = p.name,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                                if (p.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = p.description,
                                        fontSize = 14.sp,
                                        color = Color(0xFF6B7280),
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                            Text(
                                    text = CurrencyFormatter.formatRupees(dynamicPrice),
                                    fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )
                            if (p.hasCustomPrice && p.customPrice > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Custom: ${CurrencyFormatter.formatRupees(p.customPrice)}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status badges - check quantity for stock status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Stock status based on quantity: if quantity is 0, show "Out of Stock"
                            val isInStock = p.quantity > 0 && p.available
                            StatusBadge(
                                text = if (isInStock) "In Stock" else "Out of Stock",
                                color = if (isInStock) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                                backgroundColor = if (isInStock) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                        if (p.featured) {
                                StatusBadge(
                                    text = "Featured",
                                    color = Color(0xFFFFA000),
                                    backgroundColor = Color(0xFFFFF8E1)
                                )
                            }
                            if (p.isCollectionProduct && p.collectionId.isNotEmpty()) {
                                StatusBadge(
                                    text = "Collection",
                                    color = Color(0xFF9C27B0),
                                    backgroundColor = Color(0xFFF3E5F5)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Basic Information Card
                InfoCard(title = "Basic Information") {
                    InfoRow("Product ID", p.id)
                    InfoRow("Category", viewModel.getCategoryName(p.categoryId).takeIf { it.isNotEmpty() } ?: "Not specified")
                    InfoRow("Material", viewModel.getMaterialName(p.materialId).takeIf { it.isNotEmpty() } ?: "Not specified")
                    if (p.materialType.isNotEmpty()) {
                        InfoRow("Material Type", p.materialType)
                    }
                    if (p.materialName.isNotEmpty()) {
                        InfoRow("Material Name", p.materialName)
                    }
                    if (p.gender.isNotEmpty()) {
                        InfoRow("Gender", p.gender)
                    }
                    InfoRow("Quantity", p.quantity.toString() + if (p.quantity == 0) " (Out of Stock)" else "")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                // Weight Information Card
                InfoCard(title = "Weight Details") {
                    if (p.totalWeight > 0) {
                        InfoRow("Total Weight", "${String.format("%.2f", p.totalWeight)} g")
                    }
                    if (p.materialWeight > 0) {
                        InfoRow("Material Weight", "${String.format("%.2f", p.materialWeight)} g")
                    }
                    if (p.stoneWeight > 0) {
                        InfoRow("Stone Weight", "${String.format("%.2f", p.stoneWeight)} g")
                    }
                    if (p.effectiveWeight > 0) {
                        InfoRow("Effective Weight", "${String.format("%.2f", p.effectiveWeight)} g")
                    }
                    if (p.effectiveMetalWeight > 0) {
                        InfoRow("Effective Metal Weight", "${String.format("%.2f", p.effectiveMetalWeight)} g")
                    }
                    if (p.weight.isNotEmpty()) {
                        InfoRow("Weight (Legacy)", p.weight)
                    }
                }

                    Spacer(modifier = Modifier.height(16.dp))

                // Pricing & Charges Card
                InfoCard(title = "Pricing & Charges") {
                    if (p.makingPercent > 0) {
                        InfoRow("Making Percentage", "${String.format("%.2f", p.makingPercent)}%")
                    }
                    if (p.labourRate > 0) {
                        InfoRow("Labour Rate", "${CurrencyFormatter.formatRupees(p.labourRate)}/g")
                    }
                    if (p.labourCharges > 0) {
                        InfoRow("Labour Charges", CurrencyFormatter.formatRupees(p.labourCharges))
                    }
                    if (p.makingCharges > 0) {
                        InfoRow("Making Charges", CurrencyFormatter.formatRupees(p.makingCharges))
                    }
                    if (p.price > 0) {
                        InfoRow("Base Price", CurrencyFormatter.formatRupees(p.price))
                    }
                }

                    Spacer(modifier = Modifier.height(16.dp))

                // Stones Information Card
                if (p.hasStones && p.stones.isNotEmpty()) {
                    InfoCard(title = "Stones Information") {
                        if (p.stoneAmount > 0) {
                            InfoRow("Total Stone Amount", CurrencyFormatter.formatRupees(p.stoneAmount))
                        }
                        p.stones.forEachIndexed { index, stone ->
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "Stone ${index + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            if (stone.name.isNotEmpty()) {
                                InfoRow("Name", stone.name)
                            }
                            if (stone.weight > 0) {
                                InfoRow("Weight", "${String.format("%.2f", stone.weight)} g")
                            }
                            if (stone.quantity > 0) {
                                InfoRow("Quantity", String.format("%.2f", stone.quantity))
                            }
                            if (stone.rate > 0) {
                                InfoRow("Rate", CurrencyFormatter.formatRupees(stone.rate))
                            }
                            if (stone.amount > 0) {
                                InfoRow("Amount", CurrencyFormatter.formatRupees(stone.amount))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Additional Information Card
                InfoCard(title = "Additional Information") {
                    InfoRow("Created At", java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(p.createdAt)))
                    if (p.autoGenerateId) {
                        InfoRow("ID Generation", "Auto-generated")
                    }
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Product not found or still loading...",
                        color = Color.Gray,
                        fontSize = 16.sp
                )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: Color,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            fontSize = 14.sp,
            modifier = Modifier.width(160.dp)
        )
        Text(
            text = value,
            color = Color(0xFF1A1A1A),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
