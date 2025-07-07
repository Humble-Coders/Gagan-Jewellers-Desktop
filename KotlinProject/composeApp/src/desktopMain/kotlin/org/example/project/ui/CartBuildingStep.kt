package org.example.project.ui

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
import org.example.project.utils.ImageLoader
import org.example.project.viewModels.CartViewModel
import org.example.project.viewModels.ProductsViewModel
import org.jetbrains.skia.Image


@Composable
fun ShopMainScreen(
    productsViewModel: ProductsViewModel,
    cartViewModel: CartViewModel,
    imageLoader: ImageLoader,
    onClose: (() -> Unit)? = null
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val cart by cartViewModel.cart
    val metalPrices by cartViewModel.metalPrices
    var showPriceEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with close button and tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Close button (top left)
            onClose?.let { closeCallback ->
                IconButton(
                    onClick = closeCallback,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }

            // Rounded TabRow
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colors.primary,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Shop Inventory") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Cart (${cart.totalItems})") }
                    )
                }
            }

            // Metal prices in a row with edit button
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = 2.dp,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Gold: ₹${metalPrices.goldPricePerGram}/g",
                        fontSize = 12.sp,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )

                    Divider(
                        modifier = Modifier
                            .height(16.dp)
                            .width(1.dp),
                        color = Color.LightGray
                    )

                    Text(
                        "Silver: ₹${metalPrices.silverPricePerGram}/g",
                        fontSize = 12.sp,
                        color = Color(0xFFC0C0C0),
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showPriceEditor = true },
                        modifier = Modifier.size(32.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit prices",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
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
                    onProceedToPayment = { /* Handle payment */ },
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
                        .padding(16.dp)
                        .zIndex(1f),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                    elevation = ButtonDefaults.elevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        tint = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "View Cart (${cart.totalItems})",
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Price editor dialog
    if (showPriceEditor) {
        PriceEditorDialog(
            goldPrice = metalPrices.goldPricePerGram,
            silverPrice = metalPrices.silverPricePerGram,
            onPricesUpdated = { gold, silver ->
                cartViewModel.updateMetalPrices(gold, silver)
                showPriceEditor = false
            },
            onDismiss = { showPriceEditor = false }
        )
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var productImages by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    // Load product images
    LaunchedEffect(products) {
        products.forEach { product ->
            if (product.images.isNotEmpty() && !productImages.containsKey(product.id)) {
                coroutineScope.launch {
                    val imageUrl = product.images.first()
                    val imageBytes = imageLoader.loadImage(imageUrl)
                    imageBytes?.let {
                        val bitmap = withContext(Dispatchers.IO) {
                            Image.makeFromEncoded(it).toComposeImageBitmap()
                        }
                        productImages = productImages + (product.id to bitmap)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar sized for full text visibility
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search products...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category filter
        if (categories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Products grid
        val filteredProducts = products.filter { product ->
            val matchesSearch = if (searchQuery.isEmpty()) true else {
                product.name.contains(searchQuery, ignoreCase = true) ||
                        product.description.contains(searchQuery, ignoreCase = true) ||
                        productsViewModel.getCategoryName(product.categoryId).contains(searchQuery, ignoreCase = true) ||
                        productsViewModel.getMaterialName(product.materialId).contains(searchQuery, ignoreCase = true)
            }
            val matchesCategory = if (selectedCategoryId.isEmpty()) true else {
                product.categoryId == selectedCategoryId
            }
            matchesSearch && matchesCategory && product.available
        }

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No products found",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(200.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductCard(
                        product = product,
                        image = productImages[product.id],
                        categoryName = productsViewModel.getCategoryName(product.categoryId),
                        materialName = productsViewModel.getMaterialName(product.materialId),
                        goldPrice = cartViewModel.metalPrices.value.goldPricePerGram,
                        silverPrice = cartViewModel.metalPrices.value.silverPricePerGram,
                        onAddToCart = { cartViewModel.addToCart(product) }
                    )
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
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colors.primary else Color.LightGray
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Black,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    image: ImageBitmap?,
    categoryName: String,
    materialName: String,
    goldPrice: Double,
    silverPrice: Double,
    onAddToCart: () -> Unit
) {
    val weight = parseWeight(product.weight)
    val pricePerGram = when {
        product.materialType.contains("gold", ignoreCase = true) -> goldPrice
        product.materialType.contains("silver", ignoreCase = true) -> silverPrice
        else -> goldPrice
    }
    val calculatedPrice = weight * pricePerGram

    var isClicked by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Product image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFFF5F5F5))
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                if (product.featured) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color(0xFFFFA000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Featured",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Product details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = categoryName,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = product.weight,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = materialName,
                        color = MaterialTheme.colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Price and Add to Cart in Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${String.format("%.0f", calculatedPrice)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.primary
                    )

                    IconButton(
                        onClick = {
                            isClicked = !isClicked
                            onAddToCart()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isClicked) Color(0xFF4CAF50) else MaterialTheme.colors.primary,
                                CircleShape
                            )
                    ) {
                        Icon(
                            if (isClicked) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = if (isClicked) "Added to cart" else "Add to cart",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
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
                    label = { Text("Gold Price per gram") },
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
    goldPrice: Double,
    silverPrice: Double,
    onUpdateQuantity: (Int) -> Unit,
    onUpdateWeight: (Double) -> Unit,
    onRemove: () -> Unit
) {
    val pricePerGram = when {
        cartItem.product.materialType.contains("gold", ignoreCase = true) -> goldPrice
        cartItem.product.materialType.contains("silver", ignoreCase = true) -> silverPrice
        else -> goldPrice
    }

    val productWeight = parseWeight(cartItem.product.weight)
    val actualWeight = if (cartItem.selectedWeight > 0) cartItem.selectedWeight else productWeight
    val itemTotal = actualWeight * cartItem.quantity * pricePerGram

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

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove item",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    onProceedToPayment: () -> Unit = {},
    onContinueShopping: () -> Unit = {}
) {
    val cart by cartViewModel.cart
    val cartImages by cartViewModel.cartImages
    val metalPrices by cartViewModel.metalPrices
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                "Your Cart (${cart.totalItems} Items)",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Cart items
            cart.items.forEach { cartItem ->
                CartItemCard(
                    cartItem = cartItem,
                    image = cartImages[cartItem.productId],
                    goldPrice = metalPrices.goldPricePerGram,
                    silverPrice = metalPrices.silverPricePerGram,
                    onUpdateQuantity = { newQuantity ->
                        cartViewModel.updateQuantity(cartItem.productId, newQuantity)
                    },
                    onUpdateWeight = { newWeight ->
                        cartViewModel.updateWeight(cartItem.productId, newWeight)
                    },
                    onRemove = {
                        cartViewModel.removeFromCart(cartItem.productId)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Cart summary
            CartSummary(
                subtotal = cartViewModel.getSubtotal(),
                gst = cartViewModel.getGST(),
                total = cartViewModel.getFinalTotal(),
                onClearCart = { cartViewModel.clearCart() },
                onProceedToPayment = onProceedToPayment
            )
        }
    }
}

@Composable
fun CartSummary(
    subtotal: Double,
    gst: Double,
    total: Double,
    onClearCart: () -> Unit,
    onProceedToPayment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Order Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal:", fontSize = 16.sp)
                Text(
                    "₹${String.format("%.0f", subtotal)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("GST (18.0%):", fontSize = 16.sp)
                Text(
                    "₹${String.format("%.0f", gst)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "₹${String.format("%.0f", total)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClearCart,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Cart")
                }

                Button(
                    onClick = onProceedToPayment,
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    Text(
                        "Proceed to Payment",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}