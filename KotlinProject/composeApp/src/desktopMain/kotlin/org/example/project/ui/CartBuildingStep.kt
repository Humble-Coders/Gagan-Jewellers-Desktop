package org.example.project.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.Category
import org.example.project.data.Product
import org.example.project.data.CartItem
import org.example.project.data.GroupedProduct
import org.example.project.data.MetalRatesManager
import org.example.project.data.extractKaratFromMaterialType
import org.example.project.ui.CartTable
import org.example.project.ui.ProductPriceInputs
import org.example.project.ui.calculateProductPrice
import org.example.project.ui.calculateStonePrices
import org.example.project.utils.ImageLoader
import org.example.project.viewModels.CartViewModel
import org.example.project.viewModels.ProductsViewModel
import org.example.project.JewelryAppInitializer
import org.example.project.utils.CurrencyFormatter

import org.jetbrains.skia.Image

// Update the ShopMainScreen function in your existing cart files

@Composable
fun ShopMainScreen(
    productsViewModel: ProductsViewModel,
    cartViewModel: CartViewModel,
    imageLoader: ImageLoader,
    onClose: (() -> Unit)? = null,
    onProceedToPayment: (() -> Unit)? = null // Add this parameter - navigates to BillingStep.PAYMENT
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val cart by cartViewModel.cart

    // Update existing cart items with current gold rate when cart is loaded
    LaunchedEffect(Unit) {
        cartViewModel.updateExistingCartItemsWithGoldRate()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with close button and tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Close button (top left)
            onClose?.let { closeCallback ->
                IconButton(
                    onClick = closeCallback,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Rounded TabRow
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                elevation = 2.dp
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.primary,
                    modifier = Modifier.height(40.dp),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colors.primary,
                            height = 2.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        modifier = Modifier.height(40.dp),
                        text = {
                            Text(
                                "Shop Inventory",
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        modifier = Modifier.height(40.dp),
                        text = {
                            Text(
                                "Cart (${cart.totalItems})",
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

        }

        // Content based on selected tab
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> CartBuildingScreen(
                    productsViewModel = productsViewModel,
                    cartViewModel = cartViewModel,
                    imageLoader = imageLoader
                )
                1 -> CartScreen(
                    cartViewModel = cartViewModel,
                    productsViewModel = productsViewModel,
                    onProceedToPayment = {
                        onProceedToPayment?.invoke() // Navigate to BillingStep.PAYMENT
                    },
                    onContinueShopping = { selectedTabIndex = 0 }
                )
            }

            // Fixed View Cart button for shop screen
            if (selectedTabIndex == 0 && cart.totalItems > 0) {
                Button(
                    onClick = { selectedTabIndex = 1 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp) // Increased vertical padding
                        .zIndex(1f),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                    elevation = ButtonDefaults.elevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(8.dp) // Slightly more rounded corners
                ){
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "View Cart (${cart.totalItems})",
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CartBuildingScreen(
    productsViewModel: ProductsViewModel,
    cartViewModel: CartViewModel,
    imageLoader: ImageLoader
) {
    val products by productsViewModel.products
    val categories by productsViewModel.categories
    val inventoryLoading by productsViewModel.inventoryLoading
    val inventoryRefreshTrigger by productsViewModel.inventoryRefreshTrigger
    val inventoryData by productsViewModel.inventoryData
    val coroutineScope = rememberCoroutineScope()
    val cart by cartViewModel.cart
    
    // Optimized loading: Load products first, then inventory (avoid multiple loads)
    LaunchedEffect(Unit) {
        if (products.isEmpty()) {
            productsViewModel.loadProducts()
        } else if (!inventoryLoading && inventoryData.isEmpty()) {
            // Only load inventory if products are already loaded and inventory isn't loading
            productsViewModel.loadInventoryData()
        }
    }
    
    // Trigger inventory loading when products are loaded (single trigger)
    LaunchedEffect(products.size) {
        if (products.isNotEmpty() && !inventoryLoading && inventoryData.isEmpty()) {
            productsViewModel.loadInventoryData()
        }
    }
    
    // Removed excessive cart logging for better performance
    
    // Get metal rates for dynamic pricing
    val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
    val metalRates by ratesVM.metalRates.collectAsState()
    
    // Barcode selection dialog state
    var showBarcodeDialog by remember { mutableStateOf(false) }
    var selectedGroupedProduct by remember { mutableStateOf<GroupedProduct?>(null) }
    var targetQuantity by remember { mutableStateOf(0) }
    // Map to store selected barcodes state per product (by product ID)
    var selectedBarcodeStates by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    
    // Load metal rates when screen loads
    LaunchedEffect(Unit) {
        ratesVM.loadMetalRates()
    }

    // Removed excessive logging for better performance

    // Force recalculation when cart screen opens by updating existing cart items with current rates
    LaunchedEffect(Unit) {
        cartViewModel.updateExistingCartItemsWithGoldRate()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var productImages by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    // Get grouped products like dashboard - update when inventory loads (same as DashboardScreen)
    val groupedProducts = remember(products, inventoryRefreshTrigger, inventoryData) {
        if (products.isEmpty()) {
            emptyList()
        } else {
            try {
                productsViewModel.getGroupedProducts()
            } catch (e: IllegalStateException) {
                // Recoverable: Data structure issue
                println("⚠️ Grouped products calculation error: ${e.message}")
                emptyList()
            } catch (e: Exception) {
                // Critical: Log and potentially show error
                println("❌ Critical error in getGroupedProducts: ${e.message}")
                e.printStackTrace()
                emptyList()  // Still return empty to prevent crash
            }
        }
    }

    // Track cart items for selection state
    val cartItemIds = cart.items.map { it.productId }.toSet()

    // Load product images
    LaunchedEffect(products) {
        products.forEach { product ->
            if (product.images.isNotEmpty() && !productImages.containsKey(product.id)) {
                coroutineScope.launch {
                    val imageUrl = product.images.first()
                    val imageBytes = imageLoader.loadImage(imageUrl)
                    if (imageBytes != null && imageBytes.isNotEmpty()) {
                        try {
                            val bitmap = withContext(Dispatchers.IO) {
                                Image.makeFromEncoded(imageBytes).toComposeImageBitmap()
                            }
                            productImages = productImages + (product.id to bitmap)
                        } catch (e: IllegalArgumentException) {
                            // Recoverable: Invalid image format
                            println("⚠️ Invalid image format for ${product.name}: ${e.message}")
                            // Keep null to show placeholder
                        } catch (e: Exception) {
                            // Critical: Log with context
                            println("❌ Failed to decode image for product ${product.name} (${product.id}):")
                            println("   - Image URL: $imageUrl")
                            println("   - Error: ${e.message}")
                            e.printStackTrace()
                            // Keep null to show placeholder
                        }
                    } else {
                        println("Failed to load image: $imageUrl - no data or empty data")
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        // Search bar styled like dashboard
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Search products...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(7.dp))

        // Category filter
        if (categories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    CategoryChip(
                        text = "All",
                        selected = selectedCategoryId.isEmpty(),
                        onClick = { selectedCategoryId = "" }
                    )
                }
                items(categories) { category ->
                    CategoryChip(
                        text = category.name,
                        selected = selectedCategoryId == category.id,
                        onClick = { selectedCategoryId = category.id }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Products grid - using grouped products like dashboard
        val filteredGroupedProducts = groupedProducts.filter { groupedProduct ->
            val product = groupedProduct.baseProduct

            val matchesSearch = if (searchQuery.isEmpty()) true else {
                val nameMatch = product.name.contains(searchQuery, ignoreCase = true)
                val descMatch = product.description.contains(searchQuery, ignoreCase = true)
                val categoryMatch = productsViewModel.getCategoryName(product.categoryId).contains(searchQuery, ignoreCase = true)
                val materialMatch = productsViewModel.getMaterialName(product.materialId).contains(searchQuery, ignoreCase = true)

                nameMatch || descMatch || categoryMatch || materialMatch
            }

            val matchesCategory = if (selectedCategoryId.isEmpty()) true else {
                product.categoryId == selectedCategoryId
            }

            // Only show products with quantity > 0 (strict filter)
            matchesSearch && matchesCategory && product.available && groupedProduct.quantity > 0
        }

        // Show loading indicator if products or inventory are loading
        if (products.isEmpty() && inventoryLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Loading products...",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (filteredGroupedProducts.isEmpty() && products.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "No products found",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (inventoryLoading) {
                        Text(
                            "Loading inventory data...",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
        }
                }
            }
        } else if (filteredGroupedProducts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No products found",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(190.dp),
                contentPadding = PaddingValues(bottom = 80.dp), // Increased bottom padding to prevent overlap
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredGroupedProducts) { groupedProduct ->
                    val currentItem = cart.items.find { it.productId == groupedProduct.baseProduct.id }
                    GroupedProductCard(
                        groupedProduct = groupedProduct,
                        image = productImages[groupedProduct.baseProduct.id],
                        categoryName = productsViewModel.getCategoryName(groupedProduct.baseProduct.categoryId),
                        materialName = productsViewModel.getMaterialName(groupedProduct.baseProduct.materialId),
                        isInCart = cartItemIds.contains(groupedProduct.baseProduct.id),
                        cartQuantity = cart.items.find { it.productId == groupedProduct.baseProduct.id }?.quantity ?: 0,
                        cartItem = currentItem,
                        cartItems = cart.items,
                        metalRates = metalRates,
                        onCardClick = {
                            // Toggle cart selection
                            if (cartItemIds.contains(groupedProduct.baseProduct.id)) {
                                cartViewModel.removeFromCart(groupedProduct.baseProduct.id)
                            } else {
                                // Show barcode selection dialog if product has multiple barcodes
                                if (groupedProduct.barcodeIds.size > 1) {
                                    selectedGroupedProduct = groupedProduct
                                    showBarcodeDialog = true
                                } else {
                                    cartViewModel.addToCart(groupedProduct.baseProduct, groupedProduct.barcodeIds)
                                }
                            }
                        },
                        onUpdateQuantity = { newQuantity ->
                            if (newQuantity > 0) {
                                val currentItem = cart.items.find { it.productId == groupedProduct.baseProduct.id }
                                if (currentItem != null) {
                                    val oldQuantity = currentItem.quantity
                                    // Check if product has multiple barcodes
                                    if (groupedProduct.barcodeIds.size > 1) {
                                        // Show barcode selection dialog for both increase and decrease
                                        if (newQuantity > oldQuantity || newQuantity < oldQuantity) {
                                            println("🖱️ Quantity change clicked for product with multiple barcodes")
                                            println("   - Product: ${groupedProduct.baseProduct.name}")
                                            println("   - Available barcodes: ${groupedProduct.barcodeIds}")
                                            println("   - Old quantity: $oldQuantity")
                                            println("   - New quantity: $newQuantity")
                                            selectedGroupedProduct = groupedProduct
                                            targetQuantity = newQuantity
                                            showBarcodeDialog = true
                                        }
                                    } else {
                                        // Single barcode - update directly
                                        cartViewModel.updateQuantity(groupedProduct.baseProduct.id, newQuantity)
                                    }
                                } else {
                                    // Product not in cart yet - show barcode dialog if multiple barcodes
                                    if (groupedProduct.barcodeIds.size > 1) {
                                        selectedGroupedProduct = groupedProduct
                                        showBarcodeDialog = true
                                    } else {
                                        // Single barcode - add directly
                                        cartViewModel.addToCart(groupedProduct.baseProduct, groupedProduct.barcodeIds)
                                        if (newQuantity > 1) {
                                            cartViewModel.updateQuantity(groupedProduct.baseProduct.id, newQuantity)
                                        }
                                    }
                                }
                            } else {
                                cartViewModel.removeFromCart(groupedProduct.baseProduct.id)
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Barcode Selection Dialog
    selectedGroupedProduct?.let { groupedProduct ->
        val productId = groupedProduct.baseProduct.id
        val currentItem = cart.items.find { it.productId == productId }
        
        // Get current selected barcodes from cart item if it exists, otherwise from stored state
        val initialSelectedBarcodes = currentItem?.selectedBarcodeIds 
            ?: selectedBarcodeStates[productId] 
            ?: emptyList()
        
        BarcodeSelectionDialog(
            groupedProduct = groupedProduct,
            isVisible = showBarcodeDialog,
            initialSelectedBarcodes = initialSelectedBarcodes,
            isRemovalMode = targetQuantity > 0 || (currentItem != null && targetQuantity < currentItem.quantity),
            onDismiss = {
                // Save the current state before dismissing
                selectedBarcodeStates = selectedBarcodeStates + (productId to initialSelectedBarcodes)
                showBarcodeDialog = false
                selectedGroupedProduct = null
            },
            onConfirm = { selectedBarcodes ->
                // Update the cart with selected barcodes
                if (currentItem != null) {
                    // Always use updateCartItemBarcodes to replace barcodes (not merge)
                    cartViewModel.updateCartItemBarcodes(productId, selectedBarcodes)
                } else {
                    // Adding new item
                    cartViewModel.addToCart(groupedProduct.baseProduct, selectedBarcodes)
                }
                
                // Save the selected state
                selectedBarcodeStates = selectedBarcodeStates + (productId to selectedBarcodes)
                
                showBarcodeDialog = false
                selectedGroupedProduct = null
                targetQuantity = 0
            }
        )
    }
}

@Composable
fun GroupedProductCard(
    groupedProduct: GroupedProduct,
    image: ImageBitmap?,
    categoryName: String,
    materialName: String,
    isInCart: Boolean,
    cartQuantity: Int,
    cartItem: CartItem?,
    cartItems: List<CartItem>,
    metalRates: List<org.example.project.data.MetalRate>,
    onCardClick: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    val product = groupedProduct.baseProduct

    // Dynamic price calculation - always use ProductPriceCalculator logic (same whether in cart or not)
    val displayPrice = remember(product, metalRates) {
        // Always use the same calculation as ProductPriceCalculator.kt
            if (product.hasCustomPrice) {
                product.customPrice
            } else {
                calculateProductTotalCost(product, metalRates)
        }
    }

    // For grouped products, use the grouped quantity instead of individual product quantity
    val availableToAdd = groupedProduct.quantity - cartQuantity

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clickable { onCardClick() },
        elevation = if (isInCart) 6.dp else 2.dp,
        shape = RoundedCornerShape(8.dp),
        border = if (isInCart) BorderStroke(2.dp, MaterialTheme.colors.primary) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Product image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF5F5F5))
                ) {
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize().height(140.dp) // Explicit height to avoid aspect ratio mismatch
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    if (product.featured) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .background(Color(0xFFFFA000), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Featured",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Removed green quantity badge as requested
                }

                // Product details with reduced padding
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 2.dp), // Reduced from 8.dp
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(0.3.dp)) // Reduced spacing

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = categoryName,
                                color = Color.Gray,
                                fontSize = 12.sp, // Reduced font size
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = product.weight,
                                color = Color.Gray,
                                fontSize = 12.sp, // Reduced font size
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(0.3.dp)) // Reduced spacing

                        // Material and stock indicator row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = materialName,
                                color = MaterialTheme.colors.primary,
                                fontSize = 12.sp, // Reduced font size
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            // Stock indicator - use grouped quantity minus cart quantity
                            val remainingQuantity = groupedProduct.quantity - cartQuantity
                            if (groupedProduct.quantity <= 5) {
                                Text(
                                    text = if (remainingQuantity <= 0) "Out of Stock" else "Qty: ${remainingQuantity} left",
                                    fontSize = 10.sp,
                                    color = if (remainingQuantity <= 0) Color.Red else Color.Red,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Price and quantity controls in the same row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Price
                        Text(
                            text = "${CurrencyFormatter.formatRupees(displayPrice)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.primary
                        )

                        Card(
                            elevation = 1.dp,
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Color.White,
                            modifier = Modifier
                                .padding(2.dp)
                                .height(28.dp)
                                .wrapContentWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .defaultMinSize(minWidth = 64.dp)
                            ) {
                                // Decrease button
                                IconButton(
                                    onClick = {
                                        if (cartQuantity > 0) onUpdateQuantity(cartQuantity - 1)
                                    },
                                    modifier = Modifier.size(22.dp),
                                    enabled = cartQuantity > 0
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "−",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cartQuantity > 0) Color.Gray else Color.LightGray
                                        )
                                    }
                                }

                                // Quantity display
                                Text(
                                    text = cartQuantity.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.widthIn(min = 24.dp),
                                    textAlign = TextAlign.Center
                                )

                                // Increase button - Updated with stock limit (use grouped quantity)
                                IconButton(
                                    onClick = {
                                        if (cartQuantity < groupedProduct.quantity) {
                                            onUpdateQuantity(cartQuantity + 1)
                                        }
                                    },
                                    modifier = Modifier.size(22.dp),
                                    enabled = cartQuantity < groupedProduct.quantity // Limit to grouped quantity
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "+",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cartQuantity < groupedProduct.quantity) Color(0xFFB2935A) else Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colors.primary else Color.LightGray
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Black,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    image: ImageBitmap?,
    categoryName: String,
    materialName: String,
    isInCart: Boolean,
    cartQuantity: Int,
    onCardClick: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    // Use the same price logic as dashboard: customPrice > calculated
    val displayPrice = if (product.hasCustomPrice) {
        product.customPrice
    } else {
        calculateProductTotalCost(product, emptyList())
    }
    val availableToAdd = product.quantity - cartQuantity

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clickable { onCardClick() },
        elevation = if (isInCart) 6.dp else 2.dp,
        shape = RoundedCornerShape(8.dp),
        border = if (isInCart) BorderStroke(2.dp, MaterialTheme.colors.primary) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Product image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF5F5F5))
                ) {
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize().height(140.dp) // Explicit height to avoid aspect ratio mismatch
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    if (product.featured) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .background(Color(0xFFFFA000), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Featured",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Product details with reduced padding
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 2.dp), // Reduced from 8.dp
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(0.3.dp)) // Reduced spacing

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = categoryName,
                                color = Color.Gray,
                                fontSize = 12.sp, // Reduced font size
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = product.weight,
                                color = Color.Gray,
                                fontSize = 12.sp, // Reduced font size
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(0.3.dp)) // Reduced spacing

                        // Material and stock indicator row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = materialName,
                                color = MaterialTheme.colors.primary,
                                fontSize = 12.sp, // Reduced font size
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            // Stock indicator - show remaining quantity after items in cart
                            val remainingQuantity = product.quantity - cartQuantity
                            if (product.quantity <= 5) {
                                Text(
                                    text = if (remainingQuantity <= 0) "Out of Stock" else "Qty: ${remainingQuantity} left",
                                    fontSize = 10.sp,
                                    color = if (remainingQuantity <= 0) Color.Red else Color.Red,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Price and quantity controls in the same row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Price
                        Text(
                            text = "${CurrencyFormatter.formatRupees(displayPrice)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.primary
                        )

                        Card(
                            elevation = 1.dp,
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Color.White,
                            modifier = Modifier
                                .padding(2.dp)
                                .height(28.dp)
                                .wrapContentWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .defaultMinSize(minWidth = 64.dp)
                            ) {
                                // Decrease button
                                IconButton(
                                    onClick = {
                                        if (cartQuantity > 0) onUpdateQuantity(cartQuantity - 1)
                                    },
                                    modifier = Modifier.size(22.dp),
                                    enabled = cartQuantity > 0
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "−",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cartQuantity > 0) Color.Gray else Color.LightGray
                                        )
                                    }
                                }

                                // Quantity display
                                Text(
                                    text = cartQuantity.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.widthIn(min = 24.dp),
                                    textAlign = TextAlign.Center
                                )

                                // Increase button - Updated with stock limit
                                IconButton(
                                    onClick = {
                                        if (cartQuantity < product.quantity) {
                                            onUpdateQuantity(cartQuantity + 1)
                                        }
                                    },
                                    modifier = Modifier.size(22.dp),
                                    enabled = cartQuantity < product.quantity // Limit to stock quantity
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "+",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cartQuantity < product.quantity) Color(0xFFB2935A) else Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriceEditorDialog(
    goldPrice: Double,
    silverPrice: Double,
    onPricesUpdated: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var goldPriceText by remember { mutableStateOf(goldPrice.toString()) }
    var silverPriceText by remember { mutableStateOf(silverPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Metal Prices") },
        text = {
            Column {
                OutlinedTextField(
                    value = goldPriceText,
                    onValueChange = { goldPriceText = it },
                    label = { Text("Metal Rate per gram") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = silverPriceText,
                    onValueChange = { silverPriceText = it },
                    label = { Text("Silver Price per gram") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val gold = goldPriceText.toDoubleOrNull() ?: goldPrice
                    val silver = silverPriceText.toDoubleOrNull() ?: silverPrice
                    onPricesUpdated(gold, silver)
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun parseWeight(weightStr: String): Double {
    return try {
        weightStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    } catch (e: Exception) {
        // Log parsing errors for debugging (weight parsing is usually safe)
        println("⚠️ Error parsing weight string '$weightStr': ${e.message}")
        0.0
    }
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    image: ImageBitmap?,
    onUpdateQuantity: (Int) -> Unit,
    onUpdateWeight: (Double) -> Unit,
    onRemove: () -> Unit
) {
    val metalRates by MetalRatesManager.metalRates
    val pricePerGram = when {
        cartItem.product.materialType.contains("gold", ignoreCase = true) ->
            metalRates.getGoldRateForKarat(extractKaratFromMaterialType(cartItem.product.materialType))
        cartItem.product.materialType.contains("silver", ignoreCase = true) ->
            metalRates.getSilverRateForPurity(999) // Default to 999 purity
        else -> metalRates.getGoldRateForKarat(22) // Default to 22k gold
    }

    val productWeight = parseWeight(cartItem.product.weight)
    val actualWeight = if (cartItem.selectedWeight > 0) cartItem.selectedWeight else productWeight
    val itemTotal = actualWeight * cartItem.quantity * pricePerGram
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to remove this item from cart?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove()
                    showDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = 4.dp,
        backgroundColor = Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product icon/image
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color(0xFFFFA726),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = cartItem.product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Default icon based on product type
                    Text(
                        text = when {
                            cartItem.product.name.contains("earring", ignoreCase = true) -> "💎"
                            cartItem.product.name.contains("necklace", ignoreCase = true) -> "💛"
                            else -> "💍"
                        },
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Product details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cartItem.product.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${String.format("%.1f", actualWeight)}g • ${cartItem.product.materialType} • Making: ${CurrencyFormatter.formatRupees(actualWeight * 100, includeDecimals = true)}/g",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Quantity controls
            Card(
                elevation = 1.dp,
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color.White,
                modifier = Modifier.wrapContentSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    // Decrease button
                    IconButton(
                        onClick = {
                            if (cartItem.quantity > 1) {
                                onUpdateQuantity(cartItem.quantity - 1)
                            }
                        },
                        modifier = Modifier.size(32.dp),
                        enabled = cartItem.quantity > 1
                    ) {
                        Text(
                            text = "−",
                            fontSize = 18.sp,
                            color = if (cartItem.quantity > 1) Color.Gray else Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quantity display
                    Text(
                        text = cartItem.quantity.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.widthIn(min = 24.dp),
                        textAlign = TextAlign.Center
                    )

                    // Increase button
                    IconButton(
                        onClick = {
                            onUpdateQuantity(cartItem.quantity + 1)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "+",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Price and delete
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = CurrencyFormatter.formatRupees(itemTotal),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    productsViewModel: ProductsViewModel,
    onProceedToPayment: () -> Unit = {},
    onContinueShopping: () -> Unit = {}
) {
    val cart by cartViewModel.cart
    val cartImages by cartViewModel.cartImages
    val isLoading by cartViewModel.loading

    LaunchedEffect(Unit) {
        cartViewModel.loadCartImages()
    }

    if (cart.items.isEmpty()) {
        // Empty cart state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Your cart is empty",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Add some products to get started",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onContinueShopping,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    Text("Continue Shopping", color = Color.White)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with Cart Title, Total, and Payment Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Cart Title
                Text(
                    "Your Cart (${cart.totalItems} Items)",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                // Right side - Total and Payment Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total Amount
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "Total",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        // Calculate total as sum of all cart item prices using ProductPriceCalculator logic
                        val metalRates by MetalRatesManager.metalRates
                        val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
                        val cartTotal = remember(cart, metalRates) {
                            val total = cart.items.sumOf { cartItem ->
                                val currentProduct = cartItem.product
                                
                                // Use ProductPriceCalculator logic (same as CompactCartItem)
                                val grossWeight = if (cartItem.grossWeight > 0) cartItem.grossWeight else currentProduct.totalWeight
                                val makingPercentage = currentProduct.makingPercent
                                val labourRatePerGram = currentProduct.labourRate
                                
                                // Extract all stone types from stones array using helper function
                                val stoneBreakdown = calculateStonePrices(currentProduct.stones)
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
                                val metalKarat = if (cartItem.metal.isNotEmpty()) {
                                    cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(currentProduct.materialType)
                                } else {
                                    extractKaratFromMaterialType(currentProduct.materialType)
                                }
                                val collectionRate = try {
                                    val rate = ratesVM.calculateRateForMaterial(currentProduct.materialId, currentProduct.materialType, metalKarat)
                                    if (rate <= 0) {
                                        println("⚠️ Collection rate returned 0 or negative for ${currentProduct.materialType} (${currentProduct.name})")
                                    }
                                    rate
                                } catch (e: IllegalStateException) {
                                    // Recoverable: Missing material configuration
                                    println("⚠️ Collection rate not found for material: ${currentProduct.materialId}, type: ${currentProduct.materialType}, karat: $metalKarat")
                                    println("   - Product: ${currentProduct.name}")
                                    0.0
                                } catch (e: Exception) {
                                    // Critical: Log error with context
                                    println("❌ Critical error calculating collection rate for product ${currentProduct.name}:")
                                    println("   - Material ID: ${currentProduct.materialId}")
                                    println("   - Material Type: ${currentProduct.materialType}")
                                    println("   - Karat: $metalKarat")
                                    println("   - Error: ${e.message}")
                                    e.printStackTrace()
                                    0.0
                                }
                                val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
                                val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else defaultGoldRate
                                val silverPurity = extractSilverPurityFromMaterialType(currentProduct.materialType)
                                val silverRate = metalRates.getSilverRateForPurity(silverPurity)
                                val goldRatePerGram = if (collectionRate > 0) collectionRate else when {
                                    currentProduct.materialType.contains("gold", ignoreCase = true) -> goldRate
                                    currentProduct.materialType.contains("silver", ignoreCase = true) -> silverRate
                                    else -> goldRate
                                }
                                
                                // Build ProductPriceInputs (same structure as ProductPriceCalculator)
                                val priceInputs = ProductPriceInputs(
                                    grossWeight = grossWeight,
                                    goldPurity = currentProduct.materialType,
                                    goldWeight = currentProduct.materialWeight.takeIf { it > 0 } ?: grossWeight,
                                    makingPercentage = makingPercentage,
                                    labourRatePerGram = labourRatePerGram,
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
                                
                                // Calculate per-item total, then multiply by quantity (no discount or GST)
                                val perItemTotal = result.totalProductPrice
                                val finalAmount = perItemTotal * cartItem.quantity
                                
                                finalAmount
                            }
                            total
                        }
                        
                        Text(
                            CurrencyFormatter.formatRupees(cartTotal),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }

                    // Proceed to Payment Button
                    Button(
                        onClick = onProceedToPayment,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Text(
                            "Proceed to Payment",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Proceed",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            // Cart items table
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                CartTable(
                    cartItems = cart.items,
                    onItemUpdate = { index, updatedItem ->
                        cartViewModel.updateCartItem(index, updatedItem)
                    },
                    onItemRemove = { index ->
                        val itemToRemove = cart.items[index]
                        cartViewModel.removeFromCart(itemToRemove.productId)
                    },
                    productsViewModel = productsViewModel,
                    cartImages = cartImages,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun CartSummary(
    subtotal: Double,
    total: Double,
    onClearCart: () -> Unit,
    onProceedToPayment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                "Order Summary",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    CurrencyFormatter.formatRupees(total),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearCart,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text("Clear Cart", fontSize = 12.sp)
                }

                Button(
                    onClick = onProceedToPayment,
                    modifier = Modifier
                        .weight(2f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    Text(
                        "Proceed to Payment",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}


/**
 * Calculate the total product cost using the same logic as ProductPriceCalculator
 * Fetches material rates from metal rates (same as ProductPriceCalculator)
 */
private fun calculateProductTotalCost(product: Product, metalRates: List<org.example.project.data.MetalRate>): Double {
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
    val goldRatePerGram = getMaterialRateForProduct(product, metalRates)
    
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
    
    return result.totalProductPrice
}

/**
 * Get material rate for a product based on material and type
 * Uses dynamic metal rates from MetalRateViewModel (same as dashboard)
 */
private fun getMaterialRateForProduct(product: Product, metalRates: List<org.example.project.data.MetalRate>): Double {
    val karat = extractKaratFromMaterialType(product.materialType)
    
    // Prefer collection rate from rate view model (same as cart detail)
    val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
    val collectionRate = try {
        val rate = ratesVM.calculateRateForMaterial(product.materialId, product.materialType, karat)
        if (rate <= 0) {
            println("⚠️ Collection rate returned 0 or negative for ${product.materialType} (${product.name})")
        }
        rate
    } catch (e: IllegalStateException) {
        // Recoverable: Missing configuration
        println("⚠️ Collection rate not configured for material: ${product.materialId}, type: ${product.materialType}, karat: $karat")
        println("   - Product: ${product.name}")
        0.0
    } catch (e: Exception) { 
        // Critical: Log with context
        println("❌ Critical error calculating rate for product ${product.name}:")
        println("   - Material ID: ${product.materialId}")
        println("   - Material Type: ${product.materialType}")
        println("   - Karat: $karat")
        println("   - Error: ${e.message}")
        e.printStackTrace()
        0.0 
    }

    if (collectionRate > 0) {
        return collectionRate
    }

    // Fallback to metal rates manager
    val metalRatesManager = MetalRatesManager.metalRates.value
    val fallbackRate = when {
        product.materialType.contains("gold", ignoreCase = true) -> metalRatesManager.getGoldRateForKarat(karat)
        product.materialType.contains("silver", ignoreCase = true) -> metalRatesManager.getSilverRateForPurity(999)
        else -> metalRatesManager.getGoldRateForKarat(22)
    }
    
    // Log when using fallback
    println("🔄 Using fallback rate for ${product.materialType} $karat K = $fallbackRate")
    println("   - Collection rate was: $collectionRate (using fallback)")
    println("   - Product: ${product.name}")
    
    return fallbackRate
}

/**
 * Extract silver purity from material type string
 */
private fun extractSilverPurityFromMaterialType(materialType: String): Int {
    val s = materialType.lowercase()
    if (s.contains("999")) return 999
    if (s.contains("925") || s.contains("92.5")) return 925
    if (s.contains("900") || s.contains("90.0")) return 900
    val threeDigits = Regex("(\\d{3})").find(s)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (threeDigits != null && threeDigits in listOf(900, 925, 999)) return threeDigits
    return 999
}

/**
 * Barcode Selection Dialog Component
 * Allows users to select specific barcodes when adding products to cart
 */
@Composable
fun BarcodeSelectionDialog(
    groupedProduct: GroupedProduct,
    isVisible: Boolean,
    initialSelectedBarcodes: List<String> = emptyList(),
    isRemovalMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    if (!isVisible) return

    // Initialize selected barcodes with the provided initial state
    // Use key to reset the state when initialSelectedBarcodes changes significantly
    var selectedBarcodes by remember(isVisible, initialSelectedBarcodes) { 
        mutableStateOf(initialSelectedBarcodes) 
    }

    androidx.compose.material.AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        title = {
            Text(
                if (isRemovalMode) "Select Barcodes to Keep" else "Select Barcodes",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = "Product: ${groupedProduct.baseProduct.name}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isRemovalMode) 
                        "Select barcodes to keep (${groupedProduct.barcodeIds.size} total):"
                    else 
                        "Available barcodes (${groupedProduct.barcodeIds.size}):",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Barcode selection list
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(groupedProduct.barcodeIds) { barcode ->
                        val isSelected = selectedBarcodes.contains(barcode)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBarcodes = if (isSelected) {
                                        selectedBarcodes.filter { it != barcode }
                                    } else {
                                        selectedBarcodes + barcode
                                    }
                                    println("🏷️ Barcode $barcode ${if (isSelected) "deselected" else "selected"}")
                                    println("   - Selected barcodes: $selectedBarcodes")
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = if (isSelected) "Selected" else "Not selected",
                                tint = if (isSelected) MaterialTheme.colors.primary else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = barcode,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colors.primary else Color.Black
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isRemovalMode)
                        "Keeping: ${selectedBarcodes.size} of ${groupedProduct.barcodeIds.size}"
                    else
                        "Selected: ${selectedBarcodes.size} of ${groupedProduct.barcodeIds.size}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedBarcodes)
                },
                enabled = selectedBarcodes.isNotEmpty()
            ) {
                Text(if (isRemovalMode) "Update" else "Add to Cart")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}