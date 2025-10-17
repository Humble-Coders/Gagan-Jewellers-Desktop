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
import androidx.compose.runtime.*
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
import org.example.project.utils.ImageLoader
import org.example.project.viewModels.CartViewModel
import org.example.project.viewModels.ProductsViewModel
import org.example.project.JewelryAppInitializer

import org.jetbrains.skia.Image
import java.text.NumberFormat
import java.util.Locale


// Utility function to format numbers with commas
private fun formatNumber(number: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(number)
}

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
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .offset(y = 6.dp) // pushes it slightly downward
                        .zIndex(1f),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                    elevation = ButtonDefaults.elevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(6.dp)
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
    val coroutineScope = rememberCoroutineScope()
    val cart by cartViewModel.cart

    // Add comprehensive logging for debugging
    println("🏪 DEBUG: CartBuildingScreen initialized")
    println("   - Total products count: ${products.size}")
    println("   - Categories count: ${categories.size}")
    println("   - Cart items count: ${cart.items.size}")
    println("   - Cart item IDs: ${cart.items.map { it.productId }}")

    // Log all products to see what's available
    products.forEach { product ->
        println("   📦 Product: ${product.name} (ID: ${product.id})")
        println("      - Category: ${product.categoryId}")
        println("      - Available: ${product.available}")
        println("      - Quantity: ${product.quantity}")
        println("      - Material Type: ${product.materialType}")
    }

    // Log categories
    categories.forEach { category ->
        println("   📂 Category: ${category.name} (ID: ${category.id})")
    }

    // Force recalculation when cart screen opens by updating existing cart items with current rates
    LaunchedEffect(Unit) {
        cartViewModel.updateExistingCartItemsWithGoldRate()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var productImages by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    // Get grouped products like dashboard
    val groupedProducts = remember(products) { productsViewModel.getGroupedProducts() }

    // Log grouped products
    println("📊 DEBUG: Grouped products count: ${groupedProducts.size}")
    groupedProducts.forEach { groupedProduct ->
        println("   📦 Grouped Product: ${groupedProduct.baseProduct.name}")
        println("      - Base Product ID: ${groupedProduct.baseProduct.id}")
        println("      - Grouped Quantity: ${groupedProduct.quantity}")
        println("      - Individual Products Count: ${groupedProduct.individualProducts.size}")
        println("      - Barcode IDs: ${groupedProduct.barcodeIds}")
        println("      - Common ID: ${groupedProduct.commonId}")
    }

    // Track cart items for selection state
    val cartItemIds = cart.items.map { it.productId }.toSet()
    println("🛒 DEBUG: Cart item IDs: $cartItemIds")

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

            // Add comprehensive logging for debugging
            println("🔍 DEBUG: Checking product: ${product.name}")
            println("   - Product ID: ${product.id}")
            println("   - Category ID: ${product.categoryId}")
            println("   - Category Name: ${productsViewModel.getCategoryName(product.categoryId)}")
            println("   - Available: ${product.available}")
            println("   - Grouped Quantity: ${groupedProduct.quantity}")
            println("   - Material Type: ${product.materialType}")
            println("   - Description: ${product.description}")

            val matchesSearch = if (searchQuery.isEmpty()) true else {
                val nameMatch = product.name.contains(searchQuery, ignoreCase = true)
                val descMatch = product.description.contains(searchQuery, ignoreCase = true)
                val categoryMatch = productsViewModel.getCategoryName(product.categoryId).contains(searchQuery, ignoreCase = true)
                val materialMatch = productsViewModel.getMaterialName(product.materialId).contains(searchQuery, ignoreCase = true)

                println("   - Search Query: '$searchQuery'")
                println("   - Name Match: $nameMatch")
                println("   - Description Match: $descMatch")
                println("   - Category Match: $categoryMatch")
                println("   - Material Match: $materialMatch")

                nameMatch || descMatch || categoryMatch || materialMatch
            }

            val matchesCategory = if (selectedCategoryId.isEmpty()) true else {
                val categoryMatch = product.categoryId == selectedCategoryId
                println("   - Selected Category ID: '$selectedCategoryId'")
                println("   - Category Match: $categoryMatch")
                categoryMatch
            }

            // Add inventory check - only show products with quantity > 0
            val finalResult = matchesSearch && matchesCategory && product.available && groupedProduct.quantity > 0

            println("   - Final Filter Result: $finalResult")
            println("   - Matches Search: $matchesSearch")
            println("   - Matches Category: $matchesCategory")
            println("   - Product Available: ${product.available}")
            println("   - Quantity > 0: ${groupedProduct.quantity > 0}")
            if (groupedProduct.quantity == 0) {
                println("   - ⚠️ Product filtered out due to 0 quantity (correct behavior for CartBuildingStep)")
            }
            println("   ==========================================")

            finalResult
        }

        // Log filtered results
        println("🔍 DEBUG: Filtered grouped products count: ${filteredGroupedProducts.size}")
        filteredGroupedProducts.forEach { groupedProduct ->
            println("   ✅ Filtered Product: ${groupedProduct.baseProduct.name}")
            println("      - Product ID: ${groupedProduct.baseProduct.id}")
            println("      - Category: ${productsViewModel.getCategoryName(groupedProduct.baseProduct.categoryId)}")
            println("      - Available: ${groupedProduct.baseProduct.available}")
            println("      - Quantity: ${groupedProduct.quantity}")
        }

        if (filteredGroupedProducts.isEmpty()) {
            println("❌ DEBUG: No products found after filtering")
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
                contentPadding = PaddingValues(bottom = 12.dp),
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
                        onCardClick = {
                            // Add logging for card click
                            println("🖱️ DEBUG: Card clicked for product: ${groupedProduct.baseProduct.name}")
                            println("   - Product ID: ${groupedProduct.baseProduct.id}")
                            println("   - Is in cart: ${cartItemIds.contains(groupedProduct.baseProduct.id)}")
                            println("   - Cart item IDs: $cartItemIds")

                            // Toggle cart selection
                            if (cartItemIds.contains(groupedProduct.baseProduct.id)) {
                                println("   - Removing from cart")
                                cartViewModel.removeFromCart(groupedProduct.baseProduct.id)
                            } else {
                                println("   - Adding to cart")
                                cartViewModel.addToCart(groupedProduct.baseProduct)
                            }
                        },
                        onUpdateQuantity = { newQuantity ->
                            if (newQuantity > 0) {
                                val currentItem = cart.items.find { it.productId == groupedProduct.baseProduct.id }
                                if (currentItem != null) {
                                    cartViewModel.updateQuantity(groupedProduct.baseProduct.id, newQuantity)
                                } else {
                                    // Add to cart first, then update quantity
                                    cartViewModel.addToCart(groupedProduct.baseProduct)
                                    if (newQuantity > 1) {
                                        cartViewModel.updateQuantity(groupedProduct.baseProduct.id, newQuantity)
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
    onCardClick: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    val product = groupedProduct.baseProduct

    // If item is already in cart, show the same total as cart (Total Charges)
    val displayPrice = remember(cartItems, product, cartItem) {
        if (cartItem != null) {
            // Match cart item left card: use preferred metal rate and item fields
            val karat = if (cartItem.metal.isNotEmpty()) {
                cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(product.materialType)
            } else extractKaratFromMaterialType(product.materialType)
            val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
            val collectionRate = try {
                ratesVM.calculateRateForMaterial(product.materialId, product.materialType, karat)
            } catch (e: Exception) { 0.0 }
            val metalRates = MetalRatesManager.metalRates.value
            val metalRate = if (collectionRate > 0) collectionRate else metalRates.getGoldRateForKarat(karat)

            val grossWeight = if (cartItem.grossWeight > 0) cartItem.grossWeight else product.totalWeight
            val lessWeight = if (cartItem.lessWeight > 0) cartItem.lessWeight else product.lessWeight
            val netWeight = grossWeight - lessWeight
            val makingPerGram = if (cartItem.makingCharges > 0) cartItem.makingCharges else product.defaultMakingRate
            val cwWeight = if (cartItem.cwWeight > 0) cartItem.cwWeight else product.cwWeight
            val stoneRate = if (cartItem.stoneRate > 0) cartItem.stoneRate else product.stoneRate
            val stoneQuantity = if (cartItem.stoneQuantity > 0) cartItem.stoneQuantity else product.stoneQuantity
            val vaCharges = if (cartItem.va > 0) cartItem.va else product.vaCharges

            val baseAmount = netWeight * metalRate * cartItem.quantity
            val makingCharges = netWeight * makingPerGram * cartItem.quantity
            val stoneAmount = stoneRate * stoneQuantity * cwWeight
            baseAmount + makingCharges + stoneAmount + vaCharges
        } else {
            // Not in cart: use per-item total charges calculated from product defaults
            if (product.hasCustomPrice) {
                product.customPrice
            } else if (product.totalProductCost > 0) {
                product.totalProductCost
            } else {
                calculateProductTotalCost(product)
            }
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

                    // Show quantity badge for grouped products
                    if (groupedProduct.quantity > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(Color(0xFF4CAF50), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Qty: ${groupedProduct.quantity}",
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

                            // Stock indicator - use grouped quantity
                            if (groupedProduct.quantity <= 5) {
                                Text(
                                    text = if (groupedProduct.quantity == 0) "Out of Stock" else "Only ${groupedProduct.quantity} left",
                                    fontSize = 10.sp,
                                    color = if (groupedProduct.quantity == 0) Color.Red else Color.Red,
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
                            text = "₹${formatCurrency(displayPrice)}",
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
    // Use the same price logic as dashboard: customPrice > totalProductCost > calculated
    val displayPrice = if (product.hasCustomPrice) {
        product.customPrice
    } else if (product.totalProductCost > 0) {
        product.totalProductCost
    } else {
        calculateProductTotalCost(product)
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

                            // Stock indicator
                            if (product.quantity <= 5) {
                                Text(
                                    text = if (product.quantity == 0) "Out of Stock" else "Only ${product.quantity} left",
                                    fontSize = 10.sp,
                                    color = if (product.quantity == 0) Color.Red else Color.Red,
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
                            text = "₹${formatCurrency(displayPrice)}",
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
                    text = "${String.format("%.1f", actualWeight)}g • ${cartItem.product.materialType} • Making: ₹${String.format("%.2f", actualWeight * 100)}/g",
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
                    text = "₹${String.format("%.0f", itemTotal)}",
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
                        Text(
                            "₹${String.format("%.0f", cartViewModel.getTotalCharges())}",
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
                    "₹${String.format("%.0f", total)}",
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

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

/**
 * Calculate the total product cost based on the same logic used in AddEditProductScreen
 */
private fun calculateProductTotalCost(product: Product): Double {
    // Calculate net weight (total weight - less weight)
    val netWeight = (product.totalWeight - product.lessWeight).coerceAtLeast(0.0)
    // Always show per-item price in inventory
    val qty = 1

    // Material cost (net weight × material rate × 1)
    val materialRate = getMaterialRateForProduct(product)
    val baseAmount = netWeight * materialRate * qty

    // Making charges (net weight × making rate × 1)
    val makingCharges = netWeight * product.defaultMakingRate * qty

    // Stone amount (if has stones) - STONE_RATE × STONE_QUANTITY × CW_WT
    val stoneAmount = if (product.hasStones) {
        if (product.cwWeight > 0 && product.stoneRate > 0) {
            product.stoneRate * (product.stoneQuantity.takeIf { it > 0 } ?: 1.0) * product.cwWeight
        } else 0.0
    } else 0.0

    // Total Charges = Base Amount + Making Charges + Stone Amount + VA Charges
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
    val karat = extractKaratFromMaterialType(product.materialType)
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