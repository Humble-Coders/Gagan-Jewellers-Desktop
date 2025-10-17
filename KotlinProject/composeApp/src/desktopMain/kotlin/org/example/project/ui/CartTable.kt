package org.example.project.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.CartItem
import org.example.project.data.MetalRatesManager
import org.example.project.data.Product
import org.example.project.data.extractKaratFromMaterialType
import org.example.project.viewModels.ProductsViewModel
import java.text.DecimalFormat

import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.example.project.JewelryAppInitializer


// Color Palette - Updated to match theme
private val GoldPrimary = Color(0xFFB2935A)
private val GoldDark = Color(0xFF8B7355)
private val GoldLight = Color(0xFFFFF8DC)
private val BackgroundGray = Color(0xFFF5F5F5)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF757575)
private val ErrorRed = Color(0xFFD32F2F)

@Composable
fun CartTable(
    cartItems: List<CartItem>,
    onItemUpdate: (Int, CartItem) -> Unit,
    onItemRemove: (Int) -> Unit,
    productsViewModel: ProductsViewModel,
    cartImages: Map<String, androidx.compose.ui.graphics.ImageBitmap> = emptyMap(),
    modifier: Modifier = Modifier
) {
    println("🛒 CART TABLE RENDER START - ${cartItems.size} items")
    cartItems.forEachIndexed { index, item ->
        println("   Item $index: ${item.product.name} (qty=${item.quantity}, metal=${item.metal})")
    }
    
    val metalRates by MetalRatesManager.metalRates
    var selectedItemIndex by remember { mutableStateOf<Int?>(null) }
    var editingField by remember { mutableStateOf<String?>(null) }
    var fieldValues by remember { mutableStateOf<Map<String, TextFieldValue>>(emptyMap()) }
    var fetchedProduct by remember { mutableStateOf<Product?>(null) }
    var isLoadingProduct by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    fun onEditingFieldChange(newField: String?) {
        println("🔄 EDITING FIELD CHANGE: $editingField -> $newField")
        editingField?.let { currentField ->
            if (currentField != newField) {
                val currentValue = fieldValues[currentField]?.text ?: ""
                saveFieldValue(currentField, currentValue, cartItems, onItemUpdate)
            }
        }
        editingField = newField
        newField?.let { field ->
            if (!fieldValues.containsKey(field)) {
                val currentValue = getCurrentFieldValue(field, cartItems, fetchedProduct)
                fieldValues = fieldValues + (field to TextFieldValue(
                    text = currentValue,
                    selection = TextRange(0, currentValue.length)
                ))
            }
        }
    }

    fun onItemSelected(index: Int) {
        selectedItemIndex = index
        val cartItem = cartItems[index]
        isLoadingProduct = true
        fetchedProduct = null

        val barcodeId = cartItem.product.barcodeIds.firstOrNull()
        if (barcodeId != null) {
            coroutineScope.launch {
                try {
                    val product = productsViewModel.getProductByBarcodeId(barcodeId)
                    fetchedProduct = product
                } catch (e: Exception) {
                    println("Failed to fetch product by barcode ID: $barcodeId - ${e.message}")
                    try {
                        val product = productsViewModel.repository.getProductById(cartItem.productId)
                        fetchedProduct = product
                    } catch (e2: Exception) {
                        println("Failed to fetch product by ID: ${cartItem.productId} - ${e2.message}")
                    }
                } finally {
                    isLoadingProduct = false
                }
            }
        } else {
            coroutineScope.launch {
                try {
                    val product = productsViewModel.repository.getProductById(cartItem.productId)
                    fetchedProduct = product
                } catch (e: Exception) {
                    println("Failed to fetch product by ID: ${cartItem.productId} - ${e.message}")
                } finally {
                    isLoadingProduct = false
                }
            }
        }
    }

    fun saveAndCloseEditing() {
        editingField?.let { currentField ->
            val currentValue = fieldValues[currentField]?.text ?: ""
            saveFieldValue(currentField, currentValue, cartItems, onItemUpdate)
            editingField = null
            fieldValues = emptyMap()
            keyboardController?.hide()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (editingField != null) {
                            saveAndCloseEditing()
                        }
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        elevation = 8.dp,
        backgroundColor = BackgroundGray
    ) {
        if (cartItems.isEmpty()) {
            EmptyCartMessage()
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Side: Compact List
                CartItemsList(
                    cartItems = cartItems,
                    selectedIndex = selectedItemIndex,
                    onItemSelected = ::onItemSelected,
                    onItemRemove = { index ->
                        saveAndCloseEditing()
                        onItemRemove(index)
                        if (selectedItemIndex == index) {
                            selectedItemIndex = null
                        }
                    },
                    cartImages = cartImages,
                    metalRates = metalRates,
                    fetchedProduct = fetchedProduct,
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                )

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(GoldPrimary.copy(alpha = 0.3f))
                )

                // Right Side: Detail Panel
                if (selectedItemIndex != null && selectedItemIndex!! < cartItems.size) {
                    DetailPanel(
                        item = cartItems[selectedItemIndex!!],
                        index = selectedItemIndex!!,
                        metalRates = metalRates,
                        productsViewModel = productsViewModel,
                        fetchedProduct = fetchedProduct,
                        isLoadingProduct = isLoadingProduct,
                        editingField = editingField,
                        fieldValues = fieldValues,
                        onEditingFieldChange = ::onEditingFieldChange,
                        onFieldValueChange = { fieldKey, value ->
                            fieldValues = fieldValues + (fieldKey to value)
                        },
                        onSaveAndCloseEditing = ::saveAndCloseEditing,
                        onItemUpdate = onItemUpdate,
                        onClose = {
                            selectedItemIndex = null
                            fetchedProduct = null
                            isLoadingProduct = false
                        },
                        cartImage = cartImages[cartItems[selectedItemIndex!!].productId],
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                    )
                } else {
                    NoSelectionPlaceholder(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemsList(
    cartItems: List<CartItem>,
    selectedIndex: Int?,
    onItemSelected: (Int) -> Unit,
    onItemRemove: (Int) -> Unit,
    cartImages: Map<String, androidx.compose.ui.graphics.ImageBitmap>,
    metalRates: org.example.project.data.MetalRates,
    fetchedProduct: Product?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Compact Header - Fixed height 48dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(GoldPrimary)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Shopping Cart",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "${cartItems.size} ${if (cartItems.size == 1) "item" else "items"}",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // List with minimal padding
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(cartItems) { index, item ->
                CompactCartItem(
                    item = item,
                    index = index,
                    isSelected = selectedIndex == index,
                    onClick = { onItemSelected(index) },
                    onRemove = { onItemRemove(index) },
                    cartImage = cartImages[item.productId],
                    metalRates = metalRates,
                    fetchedProduct = if (selectedIndex == index) fetchedProduct else null
                )
            }
        }
    }
}

@Composable
private fun CompactCartItem(
    item: CartItem,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    cartImage: androidx.compose.ui.graphics.ImageBitmap?,
    metalRates: org.example.project.data.MetalRates,
    fetchedProduct: Product?
) {
    val currentProduct = fetchedProduct ?: item.product
    val metalKarat = if (item.metal.isNotEmpty()) {
        item.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(currentProduct.materialType)
    } else {
        extractKaratFromMaterialType(currentProduct.materialType)
    }

    val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
    val goldRate = if (item.customGoldRate > 0) item.customGoldRate else defaultGoldRate
    val silverPurity = extractSilverPurityFromMaterialType(currentProduct.materialType)
    val silverRate = metalRates.getSilverRateForPurity(silverPurity)

    val displayAmount = remember(item, metalRates, metalKarat, currentProduct) {
        println("🛒 CART CALCULATION START - CompactCartItem for ${currentProduct.name}")
        
        val grossWeight = if (item.grossWeight > 0) item.grossWeight else currentProduct.totalWeight
        val lessWeight = if (item.lessWeight > 0) item.lessWeight else currentProduct.lessWeight
        val netWeight = grossWeight - lessWeight
        
        println("📊 Weight Details: grossWeight=$grossWeight, lessWeight=$lessWeight, netWeight=$netWeight")

        val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
        val karatForRate = if (item.metal.isNotEmpty()) {
            item.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(currentProduct.materialType)
        } else {
            extractKaratFromMaterialType(currentProduct.materialType)
        }
        val collectionRate = try {
            ratesVM.calculateRateForMaterial(currentProduct.materialId, currentProduct.materialType, karatForRate)
        } catch (e: Exception) { 0.0 }
        val metalRate = if (collectionRate > 0) collectionRate else when {
            currentProduct.materialType.contains("gold", ignoreCase = true) -> goldRate
            currentProduct.materialType.contains("silver", ignoreCase = true) -> silverRate
            else -> goldRate
        }
        
        println("💰 Rate Details: karatForRate=$karatForRate, collectionRate=$collectionRate, metalRate=$metalRate")

        val baseAmount = netWeight * metalRate * item.quantity
        val makingChargesPerGram = if (item.makingCharges > 0) item.makingCharges else currentProduct.defaultMakingRate
        val cwWeight = if (item.cwWeight > 0) item.cwWeight else currentProduct.cwWeight
        val stoneRate = if (item.stoneRate > 0) item.stoneRate else currentProduct.stoneRate
        val stoneQuantity = if (item.stoneQuantity > 0) item.stoneQuantity else currentProduct.stoneQuantity
        val vaCharges = if (item.va > 0) item.va else currentProduct.vaCharges

        val makingCharges = netWeight * makingChargesPerGram * item.quantity
        val stoneAmount = stoneRate * stoneQuantity * cwWeight
        
        println("🔍 Stone Calculation Debug (Compact): stoneRate=$stoneRate, stoneQuantity=$stoneQuantity, cwWeight=$cwWeight, stoneAmount=$stoneAmount")
        
        val totalCharges = baseAmount + makingCharges + stoneAmount + vaCharges
        
        println("💵 Amount Breakdown:")
        println("   Base Amount: $baseAmount (netWeight=$netWeight * metalRate=$metalRate * quantity=${item.quantity})")
        println("   Making Charges: $makingCharges (netWeight=$netWeight * makingRate=$makingChargesPerGram * quantity=${item.quantity})")
        println("   Stone Amount: $stoneAmount (stoneRate=$stoneRate * stoneQuantity=$stoneQuantity * cwWeight=$cwWeight)")
        println("   VA Charges: $vaCharges")
        println("   TOTAL CHARGES: $totalCharges")
        println("🛒 CART CALCULATION END - CompactCartItem")
        
        totalCharges
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(10.dp),
        elevation = if (isSelected) 6.dp else 2.dp,
        backgroundColor = if (isSelected) GoldLight else CardWhite
    ) {
        Box {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(GoldPrimary)
                        .align(Alignment.CenterStart)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (isSelected) 16.dp else 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Image
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(3.dp, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundGray)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) GoldPrimary else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (cartImage != null) {
                        Image(
                            bitmap = cartImage,
                            contentDescription = "Product",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFFBDBDBD),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Product Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        currentProduct.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${item.quantity}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Text(
                            " • ${item.metal.ifEmpty { "${extractKaratFromMaterialType(currentProduct.materialType)}K" }}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Price
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "₹",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Text(
                            formatCurrencyNumber(displayAmount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                }

                // Remove Button
                IconButton(
                    onClick = { onRemove() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(ErrorRed.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailPanel(
    item: CartItem,
    index: Int,
    metalRates: org.example.project.data.MetalRates,
    productsViewModel: ProductsViewModel,
    fetchedProduct: Product?,
    isLoadingProduct: Boolean,
    editingField: String?,
    fieldValues: Map<String, TextFieldValue>,
    onEditingFieldChange: (String?) -> Unit,
    onFieldValueChange: (String, TextFieldValue) -> Unit,
    onSaveAndCloseEditing: () -> Unit,
    onItemUpdate: (Int, CartItem) -> Unit,
    onClose: () -> Unit,
    cartImage: androidx.compose.ui.graphics.ImageBitmap?,
    modifier: Modifier = Modifier
) {
    val currentProduct = fetchedProduct ?: item.product
    val metalKarat = if (item.metal.isNotEmpty()) {
        item.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(currentProduct.materialType)
    } else {
        extractKaratFromMaterialType(currentProduct.materialType)
    }

    val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
    val goldRate = if (item.customGoldRate > 0) item.customGoldRate else defaultGoldRate
    val silverPurity = extractSilverPurityFromMaterialType(currentProduct.materialType)
    val silverRate = metalRates.getSilverRateForPurity(silverPurity)

    val calculatedValues = remember(item, metalRates, metalKarat, currentProduct, item.stoneRate, item.stoneQuantity, item.cwWeight) {
        println("🔍 DETAIL PANEL CALCULATION START - ${currentProduct.name}")
        
        val grossWeight = if (item.grossWeight > 0) item.grossWeight else currentProduct.totalWeight
        val lessWeight = if (item.lessWeight > 0) item.lessWeight else currentProduct.lessWeight
        val quantity = item.quantity
        val makingChargesPerGram = if (item.makingCharges > 0) item.makingCharges else currentProduct.defaultMakingRate
        val cwWeight = if (item.cwWeight > 0) item.cwWeight else currentProduct.cwWeight
        val stoneRate = if (item.stoneRate > 0) item.stoneRate else currentProduct.stoneRate
        val stoneQuantity = if (item.stoneQuantity > 0) item.stoneQuantity else currentProduct.stoneQuantity
        val vaCharges = if (item.va > 0) item.va else currentProduct.vaCharges
        val discountPercent = item.discountPercent

        val netWeight = grossWeight - lessWeight
        println("📊 Detail Panel Weight Details: grossWeight=$grossWeight, lessWeight=$lessWeight, netWeight=$netWeight, quantity=$quantity")
        
        val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
        val karatForRate = if (item.metal.isNotEmpty()) {
            item.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(currentProduct.materialType)
        } else {
            extractKaratFromMaterialType(currentProduct.materialType)
        }
        val collectionRate = try {
            ratesVM.calculateRateForMaterial(currentProduct.materialId, currentProduct.materialType, karatForRate)
        } catch (e: Exception) { 0.0 }
        val metalRate = if (collectionRate > 0) collectionRate else when {
            currentProduct.materialType.contains("gold", ignoreCase = true) -> goldRate
            currentProduct.materialType.contains("silver", ignoreCase = true) -> silverRate
            else -> goldRate
        }
        
        println("💰 Detail Panel Rate Details: karatForRate=$karatForRate, collectionRate=$collectionRate, metalRate=$metalRate")
        
        val baseAmount = netWeight * metalRate * quantity
        val makingCharges = netWeight * makingChargesPerGram * quantity
        val stoneAmount = stoneRate * stoneQuantity * cwWeight
        println("🔍 Stone Calculation Debug: stoneRate=$stoneRate, stoneQuantity=$stoneQuantity, cwWeight=$cwWeight, stoneAmount=$stoneAmount")
        
        val totalCharges = baseAmount + makingCharges + stoneAmount + vaCharges
        val discountAmount = totalCharges * (discountPercent / 100)
        val taxableAmount = totalCharges - discountAmount
        val cgst = taxableAmount * 0.015
        val sgst = taxableAmount * 0.015
        val totalGst = cgst + sgst
        val finalAmount = taxableAmount + totalGst

        println("💵 Detail Panel Amount Breakdown:")
        println("   Base Amount: $baseAmount (netWeight=$netWeight * metalRate=$metalRate * quantity=$quantity)")
        println("   Making Charges: $makingCharges (netWeight=$netWeight * makingRate=$makingChargesPerGram * quantity=$quantity)")
        println("   Stone Amount: $stoneAmount (stoneRate=$stoneRate * stoneQuantity=$stoneQuantity * cwWeight=$cwWeight)")
        println("   VA Charges: $vaCharges")
        println("   Total Charges: $totalCharges")
        println("   Discount Amount: $discountAmount (${discountPercent}%)")
        println("   Taxable Amount: $taxableAmount")
        println("   CGST: $cgst (1.5%)")
        println("   SGST: $sgst (1.5%)")
        println("   Total GST: $totalGst")
        println("   Final Amount: $finalAmount")
        println("🔍 DETAIL PANEL CALCULATION END")

        mapOf(
            "grossWeight" to grossWeight,
            "lessWeight" to lessWeight,
            "netWeight" to netWeight,
            "quantity" to quantity.toDouble(),
            "makingChargesPerGram" to makingChargesPerGram,
            "cwWeight" to cwWeight,
            "stoneRate" to stoneRate,
            "vaCharges" to vaCharges,
            "discountPercent" to discountPercent,
            "baseAmount" to baseAmount,
            "makingCharges" to makingCharges,
            "stoneAmount" to stoneAmount,
            "totalCharges" to totalCharges,
            "discountAmount" to discountAmount,
            "taxableAmount" to taxableAmount,
            "cgst" to cgst,
            "sgst" to sgst,
            "totalGst" to totalGst,
            "finalAmount" to finalAmount,
            "metalRate" to metalRate
        )
    }

    Column(
        modifier = modifier
            .background(CardWhite)
    ) {
        // Compact Header - Fixed height 48dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(GoldPrimary)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Item Details",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Scrollable Content with reduced padding
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product Image - Compact
            if (cartImage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = cartImage,
                            contentDescription = "Product Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Product Info Card - Compact
            ElegantInfoCard(
                icon = Icons.Default.CheckCircle,
                title = "Product Information"
            ) {
                if (isLoadingProduct) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colors.primary
                        )
                    }
                } else {
                    InfoRow("Product Name", currentProduct.name, Icons.Default.CheckCircle)
                    Spacer(modifier = Modifier.height(4.dp))
                    InfoRow("Category", productsViewModel.getCategoryName(currentProduct.categoryId), Icons.Default.CheckCircle)
                    Spacer(modifier = Modifier.height(4.dp))
                    InfoRow("Barcode ID", currentProduct.barcodeIds.joinToString(", "), Icons.Default.Code)
                }
            }

            // Basic Information Section
            EditableSection(
                icon = Icons.Default.Edit,
                title = "Basic Information"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditableDetailField(
                            label = "Metal Type",
                            fieldKey = "metal_$index",
                            currentValue = item.metal.ifEmpty { "${extractKaratFromMaterialType(currentProduct.materialType)}K" },
                            editingField = editingField,
                            fieldValues = fieldValues,
                            onEditingFieldChange = onEditingFieldChange,
                            onFieldValueChange = onFieldValueChange,
                            onSaveAndCloseEditing = onSaveAndCloseEditing,
                            keyboardType = KeyboardType.Text,
                            icon = Icons.Default.Edit
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditableDetailField(
                            label = "Quantity",
                            fieldKey = "qty_$index",
                            currentValue = item.quantity.toString(),
                            editingField = editingField,
                            fieldValues = fieldValues,
                            onEditingFieldChange = onEditingFieldChange,
                            onFieldValueChange = onFieldValueChange,
                            onSaveAndCloseEditing = onSaveAndCloseEditing,
                            keyboardType = KeyboardType.Number,
                            icon = Icons.Default.Add
                        )
                    }
                }
            }

            // Weights Section
            EditableSection(
                icon = Icons.Default.Edit,
                title = "Weight Details"
            ) {
                EditableDetailField(
                    label = "Gross Weight (g)",
                    fieldKey = "grossWeight_$index",
                    currentValue = if (item.grossWeight > 0) item.grossWeight.toString() else currentProduct.totalWeight.toString(),
                    editingField = editingField,
                    fieldValues = fieldValues,
                    onEditingFieldChange = onEditingFieldChange,
                    onFieldValueChange = onFieldValueChange,
                    onSaveAndCloseEditing = onSaveAndCloseEditing,
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(8.dp))
                EditableDetailField(
                    label = "Less Weight (g)",
                    fieldKey = "lessWeight_$index",
                    currentValue = if (item.lessWeight > 0) item.lessWeight.toString() else currentProduct.lessWeight.toString(),
                    editingField = editingField,
                    fieldValues = fieldValues,
                    onEditingFieldChange = onEditingFieldChange,
                    onFieldValueChange = onFieldValueChange,
                    onSaveAndCloseEditing = onSaveAndCloseEditing,
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(8.dp))
                CalculatedValueRow(
                    label = "Net Weight",
                    value = formatWeight(calculatedValues["netWeight"] ?: 0.0),
                    color = GoldPrimary,
                    icon = Icons.Default.Done
                )
            }

            // Rates Section
            EditableSection(
                icon = Icons.Default.CheckCircle,
                title = "Rates & Charges"
            ) {
                CalculatedValueRow(
                    label = "Metal Rate",
                    value = formatCurrency(calculatedValues["metalRate"] ?: 0.0),
                    color = GoldPrimary,
                    icon = Icons.Default.Edit,
                    isLarge = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                EditableDetailField(
                    label = "Making Rate (per gram)",
                    fieldKey = "makingRate_$index",
                    currentValue = if (item.makingCharges > 0) item.makingCharges.toString() else if (currentProduct.makingRate > 0) currentProduct.makingRate.toString() else currentProduct.defaultMakingRate.toString(),
                    editingField = editingField,
                    fieldValues = fieldValues,
                    onEditingFieldChange = onEditingFieldChange,
                    onFieldValueChange = onFieldValueChange,
                    onSaveAndCloseEditing = onSaveAndCloseEditing,
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(8.dp))
                EditableDetailField(
                    label = "VA Charges",
                    fieldKey = "vaCharges_$index",
                    currentValue = if (item.va > 0) item.va.toString() else currentProduct.vaCharges.toString(),
                    editingField = editingField,
                    fieldValues = fieldValues,
                    onEditingFieldChange = onEditingFieldChange,
                    onFieldValueChange = onFieldValueChange,
                    onSaveAndCloseEditing = onSaveAndCloseEditing,
                    keyboardType = KeyboardType.Decimal
                )
            }

            // Stone Details Section (if applicable)
            if (currentProduct.hasStones) {
                EditableSection(
                    icon = Icons.Default.Diamond,
                    title = "Stone Details"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            EditableDetailField(
                                label = "Stone Name",
                                fieldKey = "stoneName_$index",
                                currentValue = currentProduct.stoneName,
                                editingField = editingField,
                                fieldValues = fieldValues,
                                onEditingFieldChange = onEditingFieldChange,
                                onFieldValueChange = onFieldValueChange,
                                onSaveAndCloseEditing = onSaveAndCloseEditing,
                                keyboardType = KeyboardType.Text,
                                icon = Icons.Default.Diamond
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            EditableDetailField(
                                label = "Stone Quantity",
                                fieldKey = "stoneQuantity_$index",
                                currentValue = if (item.stoneQuantity > 0) item.stoneQuantity.toString() else currentProduct.stoneQuantity.toString(),
                                editingField = editingField,
                                fieldValues = fieldValues,
                                onEditingFieldChange = onEditingFieldChange,
                                onFieldValueChange = onFieldValueChange,
                                onSaveAndCloseEditing = onSaveAndCloseEditing,
                                keyboardType = KeyboardType.Decimal,
                                icon = Icons.Default.Add
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            EditableDetailField(
                                label = "CW Weight",
                                fieldKey = "cwWeight_$index",
                                currentValue = if (item.cwWeight > 0) item.cwWeight.toString() else currentProduct.cwWeight.toString(),
                                editingField = editingField,
                                fieldValues = fieldValues,
                                onEditingFieldChange = onEditingFieldChange,
                                onFieldValueChange = onFieldValueChange,
                                onSaveAndCloseEditing = onSaveAndCloseEditing,
                                keyboardType = KeyboardType.Decimal
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            EditableDetailField(
                                label = "Stone Rate",
                                fieldKey = "stoneRate_$index",
                                currentValue = if (item.stoneRate > 0) item.stoneRate.toString() else currentProduct.stoneRate.toString(),
                                editingField = editingField,
                                fieldValues = fieldValues,
                                onEditingFieldChange = onEditingFieldChange,
                                onFieldValueChange = onFieldValueChange,
                                onSaveAndCloseEditing = onSaveAndCloseEditing,
                                keyboardType = KeyboardType.Decimal,
                                icon = Icons.Default.Edit
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CalculatedValueRow(
                        label = "Stone Amount",
                        value = formatCurrency(calculatedValues["stoneAmount"] ?: 0.0),
                        color = GoldPrimary,
                        icon = Icons.Default.CheckCircle
                    )
                }
            }

            // Calculated Amounts Section - Compact
            ElegantInfoCard(
                icon = Icons.Default.Edit,
                title = "Calculated Amounts",
                accentColor = GoldPrimary
            ) {
                AmountRow("Base Amount", calculatedValues["baseAmount"] ?: 0.0, GoldPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                AmountRow("Making Charges", calculatedValues["makingCharges"] ?: 0.0, GoldPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                AmountRow("Stone Amount", calculatedValues["stoneAmount"] ?: 0.0, GoldPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))
                AmountRow("Total Charges", calculatedValues["totalCharges"] ?: 0.0, GoldPrimary, isBold = true)
            }
        }
    }
}

@Composable
private fun NoSelectionPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CardWhite,
                        GoldLight.copy(alpha = 0.1f)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(GoldLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = GoldPrimary
                )
            }
            Text(
                "Select an item",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "Click on any item from the list\nto view and edit its details",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ElegantInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accentColor: Color = GoldPrimary,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = 3.dp,
        backgroundColor = CardWhite
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            content()
        }
    }
}

@Composable
private fun EditableSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = 3.dp,
        backgroundColor = CardWhite
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(GoldPrimary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundGray, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                label,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            value,
            fontSize = 13.sp,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AmountRow(
    label: String,
    amount: Double,
    color: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isBold) color.copy(alpha = 0.1f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = if (isBold) 10.dp else 0.dp, vertical = if (isBold) 6.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = if (isBold) 14.sp else 13.sp,
            color = TextPrimary,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "₹",
                fontSize = if (isBold) 13.sp else 12.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                color = color
            )
            Text(
                formatCurrencyNumber(amount),
                fontSize = if (isBold) 15.sp else 14.sp,
                color = color,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CalculatedValueRow(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isLarge: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(if (isLarge) 18.dp else 16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                label,
                fontSize = if (isLarge) 14.sp else 13.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            value,
            fontSize = if (isLarge) 15.sp else 14.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EditableDetailField(
    label: String,
    fieldKey: String,
    currentValue: String,
    editingField: String?,
    fieldValues: Map<String, TextFieldValue>,
    onEditingFieldChange: (String?) -> Unit,
    onFieldValueChange: (String, TextFieldValue) -> Unit,
    onSaveAndCloseEditing: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                label,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (editingField == fieldKey) {
            val focusRequester = remember { FocusRequester() }
            val textFieldValue = fieldValues[fieldKey] ?: TextFieldValue(
                text = currentValue,
                selection = TextRange(0, currentValue.length)
            )

            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { value ->
                    onFieldValueChange(fieldKey, value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 60.dp)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onSaveAndCloseEditing()
                    }
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = GoldPrimary,
                    cursorColor = GoldPrimary,
                    backgroundColor = GoldLight.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                maxLines = 1
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 60.dp)
                    .clickable { onEditingFieldChange(fieldKey) }
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                    .background(BackgroundGray, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    currentValue,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyCartMessage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GoldLight.copy(alpha = 0.2f),
                        BackgroundGray
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(GoldLight, GoldLight.copy(alpha = 0.3f))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Empty Cart",
                    modifier = Modifier.size(50.dp),
                    tint = GoldPrimary
                )
            }
            Text(
                "Your cart is empty",
                fontSize = 22.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Add products to start building your order",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper functions
private fun getCurrentFieldValue(fieldKey: String, cartItems: List<CartItem>, currentProduct: Product?): String {
    val parts = fieldKey.split("_")
    if (parts.size < 2) return ""
    val index = parts[1].toIntOrNull() ?: return ""
    if (index >= cartItems.size) return ""

    val item = cartItems[index]
    val fieldName = parts[0]
    val product = currentProduct ?: item.product

    return when (fieldName) {
        "metal" -> item.metal.ifEmpty { "${extractKaratFromMaterialType(product.materialType)}K" }
        "customGoldRate" -> if (item.customGoldRate > 0) item.customGoldRate.toString() else ""
        "size" -> "${item.selectedWeight}"
        "qty" -> item.quantity.toString()
        "grossWeight" -> if (item.grossWeight > 0) item.grossWeight.toString() else product.totalWeight.toString()
        "lessWeight" -> if (item.lessWeight > 0) item.lessWeight.toString() else product.lessWeight.toString()
        "netWeight" -> item.netWeight.toString()
        "makingRate" -> if (item.makingCharges > 0) item.makingCharges.toString() else if (product.makingRate > 0) product.makingRate.toString() else product.defaultMakingRate.toString()
        "cwWeight" -> if (item.cwWeight > 0) item.cwWeight.toString() else product.cwWeight.toString()
        "stoneRate" -> if (item.stoneRate > 0) item.stoneRate.toString() else product.stoneRate.toString()
        "vaCharges" -> if (item.va > 0) item.va.toString() else product.vaCharges.toString()
        "discountPercent" -> item.discountPercent.toString()
        "stoneName" -> product.stoneName
        "stoneQuantity" -> if (item.stoneQuantity > 0) item.stoneQuantity.toString() else product.stoneQuantity.toString()
        else -> ""
    }
}

private fun saveFieldValue(
    fieldKey: String,
    value: String,
    cartItems: List<CartItem>,
    onItemUpdate: (Int, CartItem) -> Unit
) {
    println("💾 SAVING FIELD VALUE: $fieldKey = $value")
    val parts = fieldKey.split("_")
    if (parts.size < 2) return

    val index = parts[1].toIntOrNull() ?: return
    if (index >= cartItems.size) return

    val item = cartItems[index]
    val fieldName = parts[0]

    try {
        when (fieldName) {
            "metal" -> {
                println("   Updating metal: ${item.metal} -> $value")
                onItemUpdate(index, item.copy(metal = value.trim()))
            }
            "customGoldRate" -> {
                val cleanValue = value.replace("₹", "").replace(",", "").trim()
                val rate = if (cleanValue.isNotEmpty()) cleanValue.toDoubleOrNull() ?: 0.0 else 0.0
                println("   Updating custom gold rate: ${item.customGoldRate} -> $rate")
                onItemUpdate(index, item.copy(customGoldRate = rate))
            }
            "size" -> {
                val cleanValue = value.replace("g", "").trim()
                val weight = if (cleanValue.isNotEmpty()) cleanValue.toDoubleOrNull() ?: item.selectedWeight else item.selectedWeight
                println("   Updating size: ${item.selectedWeight} -> $weight")
                onItemUpdate(index, item.copy(selectedWeight = weight))
            }
            "qty" -> {
                val qty = if (value.trim().isNotEmpty()) value.trim().toIntOrNull() ?: item.quantity else item.quantity
                println("   Updating quantity: ${item.quantity} -> $qty")
                onItemUpdate(index, item.copy(quantity = qty))
            }
            "grossWeight" -> {
                val weight = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.grossWeight else item.grossWeight
                val updatedItem = item.copy(
                    grossWeight = weight,
                    netWeight = weight - item.lessWeight
                )
                println("   Updating gross weight: ${item.grossWeight} -> $weight, net weight: ${item.netWeight} -> ${updatedItem.netWeight}")
                onItemUpdate(index, updatedItem)
            }
            "lessWeight" -> {
                val weight = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.lessWeight else item.lessWeight
                val updatedItem = item.copy(
                    lessWeight = weight,
                    netWeight = item.grossWeight - weight
                )
                println("   Updating less weight: ${item.lessWeight} -> $weight, net weight: ${item.netWeight} -> ${updatedItem.netWeight}")
                onItemUpdate(index, updatedItem)
            }
            "netWeight" -> {
                val weight = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.netWeight else item.netWeight
                println("   Updating net weight: ${item.netWeight} -> $weight")
                onItemUpdate(index, item.copy(netWeight = weight))
            }
            "makingRate" -> {
                val rate = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.makingCharges else item.makingCharges
                println("   Updating making rate: ${item.makingCharges} -> $rate")
                onItemUpdate(index, item.copy(makingCharges = rate))
            }
            "cwWeight" -> {
                val weight = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.cwWeight else item.cwWeight
                println("   Updating CW weight: ${item.cwWeight} -> $weight")
                onItemUpdate(index, item.copy(cwWeight = weight))
            }
            "stoneRate" -> {
                val rate = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.stoneRate else item.stoneRate
                println("   Updating stone rate: ${item.stoneRate} -> $rate")
                onItemUpdate(index, item.copy(stoneRate = rate))
            }
            "vaCharges" -> {
                val va = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.va else item.va
                println("   Updating VA charges: ${item.va} -> $va")
                onItemUpdate(index, item.copy(va = va))
            }
            "discountPercent" -> {
                val percent = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.discountPercent else item.discountPercent
                println("   Updating discount percent: ${item.discountPercent} -> $percent")
                onItemUpdate(index, item.copy(discountPercent = percent))
            }
            "stoneQuantity" -> {
                val quantity = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.stoneQuantity else item.stoneQuantity
                println("   Updating stone quantity: ${item.stoneQuantity} -> $quantity")
                onItemUpdate(index, item.copy(stoneQuantity = quantity))
            }
        }
    } catch (e: Exception) {
        println("❌ Error saving field $fieldKey with value $value: ${e.message}")
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "₹${formatter.format(amount)}"
}

private fun formatCurrencyNumber(amount: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return formatter.format(amount)
}

private fun formatWeight(weight: Double): String {
    val formatter = DecimalFormat("#,##0.000")
    return "${formatter.format(weight)}g"
}

private fun extractSilverPurityFromMaterialType(materialType: String): Int {
    val s = materialType.lowercase()
    if (s.contains("999")) return 999
    if (s.contains("925") || s.contains("92.5")) return 925
    if (s.contains("900") || s.contains("90.0")) return 900
    val threeDigits = Regex("(\\d{3})").find(s)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (threeDigits != null && threeDigits in listOf(900, 925, 999)) return threeDigits
    return 999
}