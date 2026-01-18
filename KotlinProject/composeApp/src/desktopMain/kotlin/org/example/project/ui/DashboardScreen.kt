package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.RadioButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.utils.ImageLoader
import org.example.project.data.Product
import org.example.project.data.Category
import org.example.project.data.GroupedProduct
import org.example.project.data.MetalRatesManager
import org.example.project.data.extractKaratFromMaterialType
import org.example.project.viewModels.ProductsViewModel
import org.jetbrains.skia.Image
import java.text.NumberFormat
import java.util.Locale
import java.net.URL
import javax.imageio.ImageIO
import org.example.project.JewelryAppInitializer
import org.example.project.utils.CurrencyFormatter
import org.example.project.ui.ProductPriceInputs
import org.example.project.ui.calculateProductPrice
import org.example.project.ui.calculateStonePrices

// Image cache to store loaded images
object ImageCache {
    private val cache = mutableMapOf<String, ImageBitmap>()

    fun get(url: String): ImageBitmap? = cache[url]

    fun put(url: String, bitmap: ImageBitmap) {
        cache[url] = bitmap
    }

    fun clear() {
        cache.clear()
    }

    fun size(): Int = cache.size
}

/**
 * Resize an image during decoding to reduce memory usage
 * For thumbnails (50dp), we resize to 100px max (2x for high DPI screens)
 * This significantly reduces memory usage and improves rendering performance
 * Smaller size = less memory = faster rendering = no UI lag
 */
fun decodeAndResizeImage(imageBytes: ByteArray, maxSize: Int = 100): ImageBitmap? {
    return try {
        val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(imageBytes) ?: return null
        val width = skiaImage.width
        val height = skiaImage.height
        
        // If image is already smaller, decode as-is
        if (width <= maxSize && height <= maxSize) {
            skiaImage.toComposeImageBitmap()
        } else {
            // Calculate new dimensions maintaining aspect ratio
            val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
            val newWidth = (width * scale).toInt().coerceAtLeast(1)
            val newHeight = (height * scale).toInt().coerceAtLeast(1)
            
            // Create a surface and draw the resized image
            val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(newWidth, newHeight)
            val canvas = surface.canvas
            val paint = org.jetbrains.skia.Paint().apply {
                isAntiAlias = true
            }
            
            // Draw the image scaled to new size
            val srcRect = org.jetbrains.skia.Rect.makeXYWH(0f, 0f, width.toFloat(), height.toFloat())
            val dstRect = org.jetbrains.skia.Rect.makeXYWH(0f, 0f, newWidth.toFloat(), newHeight.toFloat())
            canvas.drawImageRect(skiaImage, srcRect, dstRect, paint)
            
            // Get the resized image
            val resizedImage = surface.makeImageSnapshot()
            resizedImage.toComposeImageBitmap()
        }
    } catch (e: Exception) {
        println("Failed to decode/resize image: ${e.message}")
        null
    }
}

@Composable
fun DashboardScreen(
    viewModel: ProductsViewModel,
    imageLoader: ImageLoader,
    onAddProduct: () -> Unit,
    onViewProductDetails: (String) -> Unit,
    onEditBarcode: (String) -> Unit = {},
    onDeleteBarcode: (String) -> Unit = {},
    onDuplicateProduct: (String, String) -> Unit = { _, _ -> },
    startInProductsView: Boolean = false,
    initialSelectedCategoryId: String? = null
) {
    val products by viewModel.products
    val categories by viewModel.categories
    val materials by viewModel.materials
    val loading by viewModel.loading
    val inventoryLoading by viewModel.inventoryLoading
    val inventoryRefreshTrigger by viewModel.inventoryRefreshTrigger
    val inventoryData by viewModel.inventoryData
    val error by viewModel.error

    // State for view mode: true = category view, false = products view
    var showCategoryView by remember(startInProductsView) { mutableStateOf(!startInProductsView) }
    var selectedCategoryId by remember(startInProductsView, initialSelectedCategoryId) { mutableStateOf<String?>(initialSelectedCategoryId) }

    // Search state for categories
    var searchQuery by remember { mutableStateOf("") }

    // Search state for products
    var productSearchQuery by remember { mutableStateOf("") }

    // Ensure rates are loaded when dashboard loads and observe rate changes
    val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
    val metalRatesList by ratesVM.metalRates.collectAsState()
    val metalRates by MetalRatesManager.metalRates

    // Consolidated LaunchedEffect for initial data loading
    LaunchedEffect(Unit) {
        // Load products if empty
        if (products.isEmpty()) {
            viewModel.loadProducts()
        }
        
        // Load rates (independent operation)
        ratesVM.loadMetalRates()
    }

    // Single LaunchedEffect for inventory loading (depends on products)
    LaunchedEffect(products.size, inventoryRefreshTrigger, products) {
        // Only load inventory if:
        // 1. Products are loaded and not empty
        // 2. Not already loading
        // 3. Inventory data is empty OR products have changed (stale check)
        if (products.isNotEmpty() && !inventoryLoading) {
            val productIds = products.map { it.id }.toSet()
            val inventoryProductIds = inventoryData.keys.toSet()
            
            // Reload if products changed or inventory is empty
            if (inventoryData.isEmpty() || productIds != inventoryProductIds) {
                println("🔄 DASHBOARD: Loading inventory data - Products: ${products.size}, Inventory: ${inventoryData.size}")
            viewModel.loadInventoryData()
        }
    }
    }

    // Load all product images upfront in bulk (like CartBuildingStep)
    var productImages by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(products) {
        // Batch cache lookups first to avoid multiple recompositions
        val cachedUpdates = mutableMapOf<String, ImageBitmap>()
        products.forEach { product ->
            if (product.images.isNotEmpty() && !productImages.containsKey(product.id)) {
                val imageUrl = product.images.first()
                val cachedImage = ImageCache.get(imageUrl)
                if (cachedImage != null) {
                    cachedUpdates[product.id] = cachedImage
                }
            }
        }
        // Apply all cached images at once
        if (cachedUpdates.isNotEmpty()) {
            productImages = productImages + cachedUpdates
        }
        
        // Load missing images asynchronously (limit concurrent loads to prevent UI lag)
        val imagesToLoad = products.filter { product ->
            product.images.isNotEmpty() && 
            !productImages.containsKey(product.id) && 
            ImageCache.get(product.images.first()) == null
        }
        
        // Load images in batches to prevent overwhelming the UI thread
                    scope.launch {
            imagesToLoad.chunked(5).forEach { batch ->
                batch.forEach { product ->
                    val imageUrl = product.images.first()
                    launch {
                        try {
                            val imageBytes = imageLoader.loadImage(imageUrl)
                            if (imageBytes != null && imageBytes.isNotEmpty()) {
                                // Decode and resize image to thumbnail size (100px max) to reduce memory usage
                                val bitmap = withContext(Dispatchers.IO) {
                                    decodeAndResizeImage(imageBytes, maxSize = 100)
                                }
                                if (bitmap != null) {
                                // Cache the loaded image
                                ImageCache.put(imageUrl, bitmap)
                                    // Update state on Main dispatcher to prevent UI lag
                                    withContext(Dispatchers.Main) {
                                productImages = productImages + (product.id to bitmap)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Silently fail - will show placeholder
                        }
                    }
                }
                // Small delay between batches to prevent UI thread blocking
                delay(50)
            }
        }
    }

    // Cache category and material name lookups for performance
    val categoryNameMap = remember(categories) {
        categories.associate { it.id to it.name }
    }
    
    val materialNameMap = remember(materials) {
        materials.associate { it.id to it.name }
    }

    // Calculate grouped products efficiently (like CartBuildingStep - use remember instead of derivedStateOf)
    // Only return empty list if products are empty, not if inventory is loading
    // This allows products to show even while inventory is still loading
    val groupedProducts = remember(products, inventoryRefreshTrigger, inventoryData) {
        if (products.isEmpty()) {
            emptyList()
        } else {
            try {
                viewModel.getGroupedProducts()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    // Filter grouped products (separate from calculation for better performance)
    // Pre-compute lowercase strings to avoid repeated calls
    val filteredGroupedProducts = remember(groupedProducts, selectedCategoryId, productSearchQuery, categoryNameMap, materialNameMap) {
        if (groupedProducts.isEmpty()) {
            emptyList()
        } else {
            var filtered = groupedProducts
            
            // Filter by selected category if one is selected
            if (selectedCategoryId != null) {
                filtered = filtered.filter { groupedProduct ->
                    groupedProduct.baseProduct.categoryId == selectedCategoryId
                }
            }

            // Filter by search query if one is provided (use cached maps and pre-computed lowercase)
            if (productSearchQuery.isNotBlank()) {
                val query = productSearchQuery.lowercase()
                filtered = filtered.filter { groupedProduct ->
                    val product = groupedProduct.baseProduct
                    // Pre-compute lowercase strings once per product
                    val productNameLower = product.name.lowercase()
                    val productDescLower = product.description.lowercase()
                    val categoryNameLower = (categoryNameMap[product.categoryId] ?: "").lowercase()
                    val materialNameLower = (materialNameMap[product.materialId] ?: "").lowercase()
                    
                    productNameLower.contains(query) ||
                            productDescLower.contains(query) ||
                            categoryNameLower.contains(query) ||
                            materialNameLower.contains(query)
                }
            }
            
            filtered
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<String?>(null) }
    var groupedProductToDelete by remember { mutableStateOf<GroupedProduct?>(null) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var productToDuplicate by remember { mutableStateOf<String?>(null) }

    // Error display UI
    error?.let { errorMessage ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
            elevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colors.error,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.clearError() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colors.error
                    )
                }
            }
        }
    }

    // Loading state - only show loading while products are loading (not inventory)
    // Inventory loading happens in background and products can still be displayed
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFB8973D),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = if (loading) "Loading products..." else "Loading inventory...",
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        return
    }

    // Empty state - consistent with other screens
    if (products.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = 0.dp,
            backgroundColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No products found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "Add your first product to get started",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
        return
    }

    // Removed logging for better performance

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header with title and add button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (only show when in products view)
                if (!showCategoryView) {
                    IconButton(
                        onClick = {
                            showCategoryView = true
                            selectedCategoryId = null
                            productSearchQuery = "" // Clear product search when going back
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }

                Text(
                    if (showCategoryView) "Inventory" else "Products",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A202C)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddProduct,
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colors.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add New Product")
                }
            }
        }

        // Show category cards or products table based on view mode
        if (showCategoryView) {
            // Search bar for categories
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                placeholder = {
                    Text(
                        "Search inventory categories...",
                        color = Color(0xFF9CA3AF)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFB8973D)
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFB8973D),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    cursorColor = Color(0xFFB8973D)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            CategoryCardsView(
                categories = categories,
                products = products,
                searchQuery = searchQuery,
                onCategoryClick = { categoryId ->
                    // Ensure inventory is loaded before showing products
                    if (products.isNotEmpty() && !inventoryLoading && inventoryData.isEmpty()) {
                        viewModel.loadInventoryData()
                    }
                    selectedCategoryId = categoryId
                    showCategoryView = false
                }
            )
        } else {
            // Search bar (only show in products view)
            OutlinedTextField(
                value = productSearchQuery,
                onValueChange = { productSearchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                placeholder = {
                    Text(
                        "Search products by name, description, category, or material...",
                        color = Color(0xFF9CA3AF)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFB8973D)
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFB8973D),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    cursorColor = Color(0xFFB8973D)
                ),
                shape = RoundedCornerShape(12.dp)
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
                    Text("Image", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Name", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Category", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Material", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Price", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Available", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Quantity", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Actions", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Divider()

                // Table content
                if (filteredGroupedProducts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (productSearchQuery.isBlank()) {
                                    "No products found. Add a new product to get started."
                                } else {
                                    "No products match your search"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B7280)
                            )
                            if (productSearchQuery.isNotBlank()) {
                                Text(
                                    text = "Try searching for different keywords or clear the search",
                                    fontSize = 14.sp,
                                    color = Color(0xFF9CA3AF),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        // Optimize LazyColumn for better performance with many items
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(
                            items = filteredGroupedProducts,
                            key = { it.baseProduct.id }
                        ) { groupedProduct ->
                            GroupedProductRow(
                                groupedProduct = groupedProduct,
                                productImage = productImages[groupedProduct.baseProduct.id],
                                viewModel = viewModel,
                                metalRates = metalRates,
                                metalRatesList = metalRatesList,
                                categoryName = categoryNameMap[groupedProduct.baseProduct.categoryId] ?: "",
                                materialName = materialNameMap[groupedProduct.baseProduct.materialId] ?: "",
                                onDelete = {
                                    // For grouped products, delete all products in the group except parent
                                    productToDelete = groupedProduct.baseProduct.id
                                    groupedProductToDelete = groupedProduct
                                    showDeleteDialog = true
                                },
                                onClick = {
                                    // For grouped products, show details of the first product
                                    onViewProductDetails(groupedProduct.baseProduct.id)
                                },
                                onEditBarcode = onEditBarcode,
                                onDeleteBarcode = onDeleteBarcode,
                                onDuplicateProduct = { productId ->
                                    productToDuplicate = productId
                                    showDuplicateDialog = true
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                productToDelete = null
                groupedProductToDelete = null
            },
            title = { Text("Confirm Deletion") },
            text = {
                val groupedProduct = groupedProductToDelete
                if (groupedProduct?.commonId != null) {
                    Text("Are you sure you want to delete this grouped product? This will delete all products in the group except the parent product. This action cannot be undone.")
                } else {
                    Text("Are you sure you want to delete this product? This action cannot be undone.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        groupedProductToDelete?.let { groupedProduct ->
                            viewModel.deleteGroupedProduct(groupedProduct)
                        } ?: productToDelete?.let { productId ->
                            viewModel.deleteProduct(productId)
                        }
                        showDeleteDialog = false
                        productToDelete = null
                        groupedProductToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteDialog = false
                    productToDelete = null
                    groupedProductToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Duplicate product dialog
    if (showDuplicateDialog) {
        DuplicateProductDialog(
            productId = productToDuplicate ?: "",
            viewModel = viewModel,
            onDismiss = {
                showDuplicateDialog = false
                productToDuplicate = null
            },
            onConfirm = { barcodeId ->
                showDuplicateDialog = false
                productToDuplicate?.let { productId ->
                    onDuplicateProduct(productId, barcodeId)
                }
                productToDuplicate = null
            }
        )
    }
}

@Composable
private fun GroupedProductRow(
    groupedProduct: GroupedProduct,
    productImage: ImageBitmap?,
    viewModel: ProductsViewModel,
    metalRates: org.example.project.data.MetalRates,
    metalRatesList: List<org.example.project.data.MetalRate>,
    categoryName: String,
    materialName: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEditBarcode: (String) -> Unit,
    onDeleteBarcode: (String) -> Unit,
    onDuplicateProduct: (String) -> Unit
) {
    val product = groupedProduct.baseProduct

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
            productImage?.let { image ->
                Image(
                    bitmap = image,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
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

        // Category (use cached name to avoid expensive lookups)
        Text(
            text = categoryName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f)
        )

        // Material (use cached name to avoid expensive lookups)
        Text(
            text = materialName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f)
        )

        // Price - Always compute dynamic price; optionally show custom price as secondary
        // lessWeight removed from Product, using 0.0 as default
        // Use ProductPriceCalculator logic (same as AddEditProductScreen)
        val dynamicPrice = remember(product.id, product.totalWeight, product.materialWeight, product.makingPercent, product.labourRate, product.stones, metalRatesList) {
            try {
                val metalRates = MetalRatesManager.metalRates.value
                val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
                
                // Extract all stone types from stones array using helper function
                val stoneBreakdown = calculateStonePrices(product.stones)
                val kundanPrice = stoneBreakdown.kundanPrice
                val kundanWeight = stoneBreakdown.kundanWeight
                val jarkanPrice = stoneBreakdown.jarkanPrice
                val jarkanWeight = stoneBreakdown.jarkanWeight
                val diamondPrice = stoneBreakdown.diamondPrice
                val diamondWeight = stoneBreakdown.diamondWeight // in carats (for display)
                val diamondWeightInGrams = stoneBreakdown.diamondWeightInGrams // in grams (for calculation)
                val solitairePrice = stoneBreakdown.solitairePrice
                val solitaireWeight = stoneBreakdown.solitaireWeight // in carats (for display)
                val solitaireWeightInGrams = stoneBreakdown.solitaireWeightInGrams // in grams (for calculation)
                val colorStonesPrice = stoneBreakdown.colorStonesPrice
                val colorStonesWeight = stoneBreakdown.colorStonesWeight // in grams
                
                // Get material rate (fetched from metal rates, same as ProductPriceCalculator)
                val metalKarat = extractKaratFromMaterialType(product.materialType)
                val collectionRate = try {
                    ratesVM.calculateRateForMaterial(product.materialId, product.materialType, metalKarat)
                } catch (e: Exception) { 0.0 }
                val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
                // Extract silver purity from material type
                val materialTypeLower = product.materialType.lowercase()
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
                    product.materialType.contains("gold", ignoreCase = true) -> defaultGoldRate
                    product.materialType.contains("silver", ignoreCase = true) -> silverRate
                    else -> defaultGoldRate
                }
                
                // Build ProductPriceInputs (same structure as ProductPriceCalculator)
                val priceInputs = ProductPriceInputs(
                    grossWeight = product.totalWeight,
                    goldPurity = product.materialType,
                    goldWeight = product.materialWeight.takeIf { it > 0 } ?: product.totalWeight,
                    makingPercentage = product.makingPercent,
                    labourRatePerGram = product.labourRate,
                    kundanPrice = kundanPrice,
                    kundanWeight = kundanWeight,
                    jarkanPrice = jarkanPrice,
                    jarkanWeight = jarkanWeight,
                    diamondPrice = diamondPrice,
                    diamondWeight = diamondWeight,
                    diamondWeightInGrams = diamondWeightInGrams,
                    solitairePrice = solitairePrice,
                    solitaireWeight = solitaireWeight,
                    solitaireWeightInGrams = solitaireWeightInGrams,
                    colorStonesPrice = colorStonesPrice,
                    colorStonesWeight = colorStonesWeight,
                    goldRatePerGram = goldRatePerGram
                )
                
                // Use the same calculation function as ProductPriceCalculator
                val result = calculateProductPrice(priceInputs)
                result.totalProductPrice
            } catch (e: Exception) {
                0.0
            }
        }
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = "${CurrencyFormatter.formatRupees(dynamicPrice)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (product.hasCustomPrice && product.customPrice > 0) {
                Text(
                    text = "Custom: ${CurrencyFormatter.formatRupees(product.customPrice)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }
        }

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
                    .clickable {
                        expanded = true
                    }
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
                onDismissRequest = {
                    expanded = false
                },
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
                    // Barcode header
                    DropdownMenuItem(
                        onClick = { expanded = false }
                    ) {
                        Text(
                            text = barcode,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Edit option
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onEditBarcode(barcode)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit barcode $barcode",
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit",
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }

                    // Delete option
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onDeleteBarcode(barcode)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete barcode $barcode",
                                tint = Color.Red,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delete",
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                        }
                    }

                    // Divider between barcodes
                    if (barcode != groupedProduct.barcodeIds.last()) {
                        Divider(modifier = Modifier.padding(horizontal = 8.dp))
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
                    onClick = {
                        onDelete()
                    },
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
    metalRates: org.example.project.data.MetalRates,
    metalRatesList: List<org.example.project.data.MetalRate>,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val categories = viewModel.categories.value
    val materials = viewModel.materials.value
    var productImage by remember { mutableStateOf<ImageBitmap?>(null) }

    // Load the first image if available
    LaunchedEffect(product.images) {
        if (product.images.isNotEmpty()) {
            val imageUrl = product.images.first()
            // Check cache first
            val cachedImage = ImageCache.get(imageUrl)
            if (cachedImage != null) {
                productImage = cachedImage
            } else {
            coroutineScope.launch {
                val imageBytes = imageLoader.loadImage(imageUrl)
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    try {
                            // Decode and resize image to thumbnail size (100px max) to reduce memory usage
                        val image = withContext(Dispatchers.IO) {
                                decodeAndResizeImage(imageBytes, maxSize = 100)
                        }
                            if (image != null) {
                                // Cache the loaded image
                                ImageCache.put(imageUrl, image)
                                // Update state on Main dispatcher to prevent UI lag
                                withContext(Dispatchers.Main) {
                        productImage = image
                                }
                            }
                    } catch (e: Exception) {
                        // Keep null to show placeholder
                        }
                    }
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
            productImage?.let { image ->
                Image(
                    bitmap = image,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
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

        // Price - Always compute dynamic price; optionally show custom price as secondary
        // lessWeight removed from Product, using 0.0 as default
        // Use ProductPriceCalculator logic (same as AddEditProductScreen)
        val dynamicPrice = remember(product.id, product.totalWeight, product.materialWeight, product.makingPercent, product.labourRate, product.stones, metalRatesList) {
            try {
                val metalRates = MetalRatesManager.metalRates.value
                val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
                
                // Extract all stone types from stones array using helper function
                val stoneBreakdown = calculateStonePrices(product.stones)
                val kundanPrice = stoneBreakdown.kundanPrice
                val kundanWeight = stoneBreakdown.kundanWeight
                val jarkanPrice = stoneBreakdown.jarkanPrice
                val jarkanWeight = stoneBreakdown.jarkanWeight
                val diamondPrice = stoneBreakdown.diamondPrice
                val diamondWeight = stoneBreakdown.diamondWeight // in carats (for display)
                val diamondWeightInGrams = stoneBreakdown.diamondWeightInGrams // in grams (for calculation)
                val solitairePrice = stoneBreakdown.solitairePrice
                val solitaireWeight = stoneBreakdown.solitaireWeight // in carats (for display)
                val solitaireWeightInGrams = stoneBreakdown.solitaireWeightInGrams // in grams (for calculation)
                val colorStonesPrice = stoneBreakdown.colorStonesPrice
                val colorStonesWeight = stoneBreakdown.colorStonesWeight // in grams
                
                // Get material rate (fetched from metal rates, same as ProductPriceCalculator)
                val metalKarat = extractKaratFromMaterialType(product.materialType)
                val collectionRate = try {
                    ratesVM.calculateRateForMaterial(product.materialId, product.materialType, metalKarat)
                } catch (e: Exception) { 0.0 }
                val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
                // Extract silver purity from material type
                val materialTypeLower = product.materialType.lowercase()
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
                    product.materialType.contains("gold", ignoreCase = true) -> defaultGoldRate
                    product.materialType.contains("silver", ignoreCase = true) -> silverRate
                    else -> defaultGoldRate
                }
                
                // Build ProductPriceInputs (same structure as ProductPriceCalculator)
                val priceInputs = ProductPriceInputs(
                    grossWeight = product.totalWeight,
                    goldPurity = product.materialType,
                    goldWeight = product.materialWeight.takeIf { it > 0 } ?: product.totalWeight,
                    makingPercentage = product.makingPercent,
                    labourRatePerGram = product.labourRate,
                    kundanPrice = kundanPrice,
                    kundanWeight = kundanWeight,
                    jarkanPrice = jarkanPrice,
                    jarkanWeight = jarkanWeight,
                    diamondPrice = diamondPrice,
                    diamondWeight = diamondWeight,
                    diamondWeightInGrams = diamondWeightInGrams,
                    solitairePrice = solitairePrice,
                    solitaireWeight = solitaireWeight,
                    solitaireWeightInGrams = solitaireWeightInGrams,
                    colorStonesPrice = colorStonesPrice,
                    colorStonesWeight = colorStonesWeight,
                    goldRatePerGram = goldRatePerGram
                )
                
                // Use the same calculation function as ProductPriceCalculator
                val result = calculateProductPrice(priceInputs)
                result.totalProductPrice
            } catch (e: Exception) {
                0.0
            }
        }
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = "${CurrencyFormatter.formatRupees(dynamicPrice)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (product.hasCustomPrice && product.customPrice > 0) {
                Text(
                    text = "Custom: ${CurrencyFormatter.formatRupees(product.customPrice)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }
        }

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

// Removed heavy calculateProductTotalCost function to prevent UI blocking

// Removed getMaterialRateForProduct function to prevent UI blocking

// Removed extractKaratFromMaterialTypeString function to prevent UI blocking

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

@Composable
private fun CategoryCardsView(
    categories: List<Category>,
    products: List<Product>,
    searchQuery: String,
    onCategoryClick: (String) -> Unit
) {
    // Load all category images upfront in bulk (like product images)
    var categoryImages by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(categories) {
        // Batch cache lookups first to avoid multiple recompositions
        val cachedUpdates = mutableMapOf<String, ImageBitmap>()
        categories.forEach { category ->
            if (category.imageUrl.isNotEmpty() && !categoryImages.containsKey(category.id)) {
                val imageUrl = category.imageUrl
                val cachedImage = ImageCache.get(imageUrl)
                if (cachedImage != null) {
                    cachedUpdates[category.id] = cachedImage
                }
            }
        }
        // Apply all cached images at once
        if (cachedUpdates.isNotEmpty()) {
            categoryImages = categoryImages + cachedUpdates
        }
        
        // Load missing images asynchronously (limit concurrent loads to prevent UI lag)
        val categoriesToLoad = categories.filter { category ->
            category.imageUrl.isNotEmpty() && 
            !categoryImages.containsKey(category.id) && 
            ImageCache.get(category.imageUrl) == null
        }
        
        // Load images in batches to prevent overwhelming the UI thread
                    scope.launch {
            categoriesToLoad.chunked(5).forEach { batch ->
                batch.forEach { category ->
                    val imageUrl = category.imageUrl
                    launch {
                        try {
                            // Load image bytes first
                            val imageBytes = withContext(Dispatchers.IO) {
                                try {
                                    URL(imageUrl).openStream().readBytes()
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (imageBytes != null && imageBytes.isNotEmpty()) {
                                // Decode and resize image to thumbnail size (100px max) to reduce memory usage
                            val loadedBitmap = withContext(Dispatchers.IO) {
                                    decodeAndResizeImage(imageBytes, maxSize = 100)
                            }
                            if (loadedBitmap != null) {
                                // Cache the loaded image
                                ImageCache.put(imageUrl, loadedBitmap)
                                    // Update state on Main dispatcher to prevent UI lag
                                    withContext(Dispatchers.Main) {
                                categoryImages = categoryImages + (category.id to loadedBitmap)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Silently fail - will show placeholder
                        }
                    }
                }
                // Small delay between batches to prevent UI thread blocking
                delay(50)
            }
        }
    }

    // Filter categories based on search query
    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) {
            categories
        } else {
            categories.filter { category ->
                category.name.contains(searchQuery, ignoreCase = true) ||
                        category.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Calculate product count for each filtered category efficiently
    val categoryProductCounts = remember(filteredCategories, products) {
        // Pre-compute product counts by category ID for O(1) lookup
        val productCountByCategoryId = products.groupingBy { it.categoryId }.eachCount()
        filteredCategories.associateWith { category: Category ->
            productCountByCategoryId[category.id] ?: 0
        }
    }

    if (filteredCategories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No categories found" else "No categories match your search",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "Try searching for: ${categories.take(3).joinToString(", ") { it.name }}",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = filteredCategories,
            key = { it.id }
        ) { category: Category ->
            CategoryCard(
                category = category,
                productCount = categoryProductCounts[category] ?: 0,
                categoryImage = categoryImages[category.id],
                onClick = { onCategoryClick(category.id) }
            )
        }
    }
}

// Shared gradients created once outside composable to avoid recreation
private val categoryCardBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF8F9FA),
        Color(0xFFFFFFFF)
    ),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY
)

private val categoryCardImageBackgroundGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFFB8973D).copy(alpha = 0.15f),
        Color(0xFFB8973D).copy(alpha = 0.05f)
    ),
    radius = 50f
)

private val categoryCardImageBorderGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFB8973D).copy(alpha = 0.3f),
        Color(0xFFB8973D).copy(alpha = 0.1f)
    )
)

private val categoryCardBadgeGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFB8973D).copy(alpha = 0.1f),
        Color(0xFFD4AF37).copy(alpha = 0.1f)
    )
)

private val categoryCardBorderGlowGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFB8973D).copy(alpha = 0.2f),
        Color.Transparent,
        Color(0xFFB8973D).copy(alpha = 0.2f)
    )
)

private val categoryCardFallbackGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFB8973D),
        Color(0xFFD4AF37)
    )
)

@Composable
private fun CategoryCard(
    category: Category,
    productCount: Int,
    categoryImage: ImageBitmap?,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        elevation = 8.dp,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Gradient background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = categoryCardBackgroundGradient,
                        shape = RoundedCornerShape(20.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Image container with elegant styling
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = categoryCardImageBackgroundGradient,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 2.dp,
                            brush = categoryCardImageBorderGradient,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    categoryImage?.let { image ->
                        Image(
                            bitmap = image,
                            contentDescription = category.name,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } ?: run {
                        // Elegant fallback with gradient text
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(
                                    brush = categoryCardFallbackGradient,
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.name.take(2).uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        offset = Offset(1f, 1f),
                                        blurRadius = 2f
                                    )
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category name with elegant typography
                Text(
                    text = category.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A202C),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Product count with badge-like styling
                Box(
                    modifier = Modifier
                        .background(
                            brush = categoryCardBadgeGradient,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$productCount products",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFB8973D),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Subtle border glow effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        brush = categoryCardBorderGlowGradient,
                        shape = RoundedCornerShape(20.dp)
                    )
            )
        }
    }
}

/**
 * Get material rate for a product based on material and type
 * Uses dynamic metal rates from MetalRateViewModel (same as billing screen)
 * Note: No caching here as remember() in the composable already handles memoization
 */
private fun getMaterialRateForProduct(product: Product, metalRatesList: List<org.example.project.data.MetalRate>): Double {
    val karat = extractKaratFromMaterialType(product.materialType)
    
    // Prefer collection rate from rate view model
    val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
    val collectionRate = try {
        ratesVM.calculateRateForMaterial(product.materialId, product.materialType, karat)
    } catch (e: Exception) { 
        0.0 
    }

    if (collectionRate > 0) {
        return collectionRate
    }

    // Fallback to metal rates manager
    val metalRatesManager = MetalRatesManager.metalRates.value
    return when {
        product.materialType.contains("gold", ignoreCase = true) -> metalRatesManager.getGoldRateForKarat(karat)
        product.materialType.contains("silver", ignoreCase = true) -> metalRatesManager.getSilverRateForPurity(999)
        else -> metalRatesManager.getGoldRateForKarat(22)
    }
}