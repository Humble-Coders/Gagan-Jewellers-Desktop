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
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
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
import org.example.project.ui.ProductPriceInputs
import org.example.project.ui.calculateProductPrice
import org.example.project.utils.CurrencyFormatter
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
    
    // Barcode selection dialog state
    var showBarcodeDialog by remember { mutableStateOf(false) }
    var selectedCartItem by remember { mutableStateOf<CartItem?>(null) }
    
    // Track unsaved changes state for preview in shopping cart (index, previewMakingPercent, previewLabourRate)
    var unsavedChangesState by remember { mutableStateOf<Triple<Int, Double?, Double?>?>(null) }
    
    // Track save changes function from DetailPanel
    var saveChangesFunction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun onEditingFieldChange(newField: String?) {
        println("🔄 EDITING FIELD CHANGE: $editingField -> $newField")
        editingField?.let { currentField ->
            if (currentField != newField) {
                val currentValue = fieldValues[currentField]?.text ?: ""
                if (currentValue.isNotEmpty()) {
                saveFieldValue(currentField, currentValue, cartItems, onItemUpdate)
                }
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

        // Try to get the product by ID directly since barcodes are now in inventory
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

    fun saveAndCloseEditing() {
        editingField?.let { currentField ->
            val currentValue = fieldValues[currentField]?.text ?: ""
            println("💾 SAVE AND CLOSE: field=$currentField, value='$currentValue'")
            // Always save the value - for makingPercent and labourRate, allow empty to save as 0
            if (currentValue.isNotEmpty() || currentField.contains("makingPercent") || currentField.contains("labourRate")) {
            saveFieldValue(currentField, currentValue, cartItems, onItemUpdate)
            }
            editingField = null
            // Clear fieldValues for the saved field to ensure fresh value is loaded from updated product
            fieldValues = fieldValues.filterKeys { it != currentField }
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
                        val item = cartItems[index]
                        // Check if item has multiple selected barcodes
                        if (item.selectedBarcodeIds.size > 1) {
                            // Show barcode selection dialog for removal
                            selectedCartItem = item
                            showBarcodeDialog = true
                        } else {
                            // Single barcode or empty - remove directly
                            saveAndCloseEditing()
                            onItemRemove(index)
                            if (selectedItemIndex == index) {
                                selectedItemIndex = null
                            }
                        }
                    },
                    cartImages = cartImages,
                    metalRates = metalRates,
                    fetchedProduct = fetchedProduct,
                    unsavedChangesState = unsavedChangesState,
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
                        onUnsavedChangesStateChange = { hasChanges, previewMakingPercent, previewLabourRate ->
                            // Store unsaved changes state for preview in shopping cart
                            unsavedChangesState = if (hasChanges) {
                                Triple(selectedItemIndex!!, previewMakingPercent, previewLabourRate)
                            } else {
                                null
                            }
                        },
                        onSaveChangesFunctionChange = { saveFunc ->
                            saveChangesFunction = saveFunc
                        },
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
        
        // Barcode Selection Dialog
        selectedCartItem?.let { cartItem ->
            // For now, use the selected barcodes from the cart item as the available options
            // In a real scenario, you'd need to fetch all available barcodes from the inventory
            val allBarcodeIds = cartItem.selectedBarcodeIds
            
            if (allBarcodeIds.size > 1) {
                BarcodeRemovalDialog(
                    cartItem = cartItem,
                    allBarcodeIds = allBarcodeIds,
                    isVisible = showBarcodeDialog,
                    onDismiss = {
                        showBarcodeDialog = false
                        selectedCartItem = null
                    },
                    onConfirm = { remainingBarcodes ->
                        val index = cartItems.indexOf(cartItem)
                        if (remainingBarcodes.isEmpty()) {
                            // Remove entire item if no barcodes selected
                            saveAndCloseEditing()
                            onItemRemove(index)
                            if (selectedItemIndex == index) {
                                selectedItemIndex = null
                            }
                        } else {
                            // Update item with remaining barcodes
                            val updatedItem = cartItem.copy(
                                selectedBarcodeIds = remainingBarcodes,
                                quantity = remainingBarcodes.size
                            )
                            onItemUpdate(index, updatedItem)
                        }
                        showBarcodeDialog = false
                        selectedCartItem = null
                    }
                )
            }
        }
        
        // Save Changes Button - Fixed at bottom right with z-index
        if (saveChangesFunction != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1000f) // High z-index to appear on top
            ) {
                Button(
                    onClick = {
                        saveChangesFunction?.invoke()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .width(180.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = GoldPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 10.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Save Changes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BarcodeRemovalDialog(
    cartItem: CartItem,
    allBarcodeIds: List<String>,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    if (!isVisible) return

    var selectedBarcodes by remember { mutableStateOf(cartItem.selectedBarcodeIds.toList()) }

    androidx.compose.material.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Barcodes to Keep",
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
                    text = "Product: ${cartItem.product.name}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select barcodes to keep (${allBarcodeIds.size} total):",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Barcode selection list
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(allBarcodeIds) { barcode ->
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
                    text = "Keeping: ${selectedBarcodes.size} of ${allBarcodeIds.size}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedBarcodes)
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

@Composable
private fun CartItemsList(
    cartItems: List<CartItem>,
    selectedIndex: Int?,
    onItemSelected: (Int) -> Unit,
    onItemRemove: (Int) -> Unit,
    cartImages: Map<String, androidx.compose.ui.graphics.ImageBitmap>,
    metalRates: org.example.project.data.MetalRates,
    fetchedProduct: Product?,
    unsavedChangesState: Triple<Int, Double?, Double?>?,
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
                // Check if this item has unsaved changes for preview
                val previewValues = if (unsavedChangesState != null && unsavedChangesState.first == index) {
                    Triple(unsavedChangesState.second, unsavedChangesState.third, true)
                } else {
                    Triple(null, null, false)
                }
                
                CompactCartItem(
                    item = item,
                    index = index,
                    isSelected = selectedIndex == index,
                    onClick = { onItemSelected(index) },
                    onRemove = { onItemRemove(index) },
                    cartImage = cartImages[item.productId],
                    metalRates = metalRates,
                    fetchedProduct = if (selectedIndex == index) fetchedProduct else null,
                    previewMakingPercent = previewValues.first,
                    previewLabourRate = previewValues.second,
                    hasUnsavedChanges = previewValues.third
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
    fetchedProduct: Product?,
    previewMakingPercent: Double? = null,
    previewLabourRate: Double? = null,
    hasUnsavedChanges: Boolean = false
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

    // Use preview values if available (unsaved changes), otherwise use saved values
    val makingPercentage = previewMakingPercent ?: currentProduct.makingPercent
    val labourRatePerGram = previewLabourRate ?: currentProduct.labourRate

    // Ensure recalculation when product values change (after save)
    // Use item.product directly to ensure recalculation when product is updated
    val displayAmount = remember(
        item.product.makingPercent,
        item.product.labourRate,
        item.product.totalWeight,
        item.product.materialWeight,
        item.grossWeight,
        item.quantity,
        metalRates, 
        metalKarat,
        previewMakingPercent,  // Include preview values so preview shows correctly
        previewLabourRate,      // Include preview values so preview shows correctly
        hasUnsavedChanges       // Recalculate when unsaved changes state changes
    ) {
        // Use preview values if available (unsaved changes), otherwise use saved product values
        val effectiveMakingPercent = previewMakingPercent ?: item.product.makingPercent
        val effectiveLabourRate = previewLabourRate ?: item.product.labourRate
        println("🛒 CART CALCULATION START - CompactCartItem for ${item.product.name}")
        
        // Use ProductPriceCalculator logic (same as ProductPriceCalculator.kt)
        val grossWeight = if (item.grossWeight > 0) item.grossWeight else item.product.totalWeight
        val makingPercentage = effectiveMakingPercent
        val labourRatePerGram = effectiveLabourRate
        
        // Extract kundan and jarkan from stones array
        val kundanStones = item.product.stones.filter { it.name.equals("Kundan", ignoreCase = true) }
        val jarkanStones = item.product.stones.filter { it.name.equals("Jarkan", ignoreCase = true) }
        
        // Sum all Kundan prices and weights
        val kundanPrice = kundanStones.sumOf { it.amount }
        val kundanWeight = kundanStones.sumOf { it.weight }
        
        // Sum all Jarkan prices and weights
        val jarkanPrice = jarkanStones.sumOf { it.amount }
        val jarkanWeight = jarkanStones.sumOf { it.weight }
        
        // Get material rate (fetched from metal rates, same as ProductPriceCalculator)
        val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
        val karatForRate = if (item.metal.isNotEmpty()) {
            item.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(item.product.materialType)
        } else {
            extractKaratFromMaterialType(item.product.materialType)
        }
        val collectionRate = try {
            ratesVM.calculateRateForMaterial(item.product.materialId, item.product.materialType, karatForRate)
        } catch (e: Exception) { 0.0 }
        val goldRatePerGram = if (collectionRate > 0) collectionRate else when {
            item.product.materialType.contains("gold", ignoreCase = true) -> goldRate
            item.product.materialType.contains("silver", ignoreCase = true) -> silverRate
            else -> goldRate
        }
        
        // Build ProductPriceInputs (same structure as ProductPriceCalculator)
        val priceInputs = ProductPriceInputs(
            grossWeight = grossWeight,
            goldPurity = item.product.materialType,
            goldWeight = item.product.materialWeight.takeIf { it > 0 } ?: grossWeight,
            makingPercentage = makingPercentage,
            labourRatePerGram = labourRatePerGram,
            kundanPrice = kundanPrice,
            kundanWeight = kundanWeight,
            jarkanPrice = jarkanPrice,
            jarkanWeight = jarkanWeight,
            goldRatePerGram = goldRatePerGram
        )
        
        // Use the same calculation function as ProductPriceCalculator
        val result = calculateProductPrice(priceInputs)
        
        // Calculate per-item total, then multiply by quantity (no discount or GST)
        val perItemTotal = result.totalProductPrice
        val finalAmount = perItemTotal * item.quantity
        
        println("💵 Amount Breakdown (ProductPriceCalculator logic):")
        println("   Gross Weight: ${grossWeight}g")
        println("   Making %: ${makingPercentage}%")
        println("   Making Weight: ${result.makingWeight}g")
        println("   New Weight: ${result.newWeight}g")
        println("   Effective Gold Weight: ${result.effectiveGoldWeight}g")
        println("   Gold Rate: ₹${goldRatePerGram}/g")
        println("   Gold Price: ₹${result.goldPrice}")
        println("   Kundan Price: ₹${kundanPrice} (Weight: ${kundanWeight}g)")
        println("   Jarkan Price: ₹${jarkanPrice} (Weight: ${jarkanWeight}g)")
        println("   Labour Charges: ₹${result.labourCharges} (Rate: ₹${labourRatePerGram}/g × New Weight: ${result.newWeight}g)")
        println("   Per Item Total: ₹${result.totalProductPrice}")
        println("   Quantity: ${item.quantity}")
        println("   FINAL AMOUNT: ₹$finalAmount")
        println("🛒 CART CALCULATION END - CompactCartItem")
        
        finalAmount
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${currentProduct.name} ${item.metal.ifEmpty { "${extractKaratFromMaterialType(currentProduct.materialType)}K" }}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                        // Show selected barcode IDs next to quantity
                        if (item.selectedBarcodeIds.isNotEmpty()) {
                            Text(
                                " • ${item.selectedBarcodeIds.joinToString(", ")}",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Total Charges (with preview indicator if unsaved changes)
                    Column {
                        if (hasUnsavedChanges) {
                            Text(
                                "Total Charges (Preview)",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            CurrencyFormatter.formatRupees(displayAmount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                                color = if (hasUnsavedChanges) GoldDark else GoldPrimary
                        )
                        }
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
    onUnsavedChangesStateChange: (Boolean, Double?, Double?) -> Unit, // (hasUnsavedChanges, previewMakingPercent, previewLabourRate)
    onSaveChangesFunctionChange: ((() -> Unit)?) -> Unit, // Pass save function to parent
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

    val firstStone = currentProduct.stones.firstOrNull()
    
    // Local state for Making % and Labour Rate (like Total Weight in AddEditProductScreen)
    // Use item.productId as key to reset only when switching to a different cart item
    var makingPercentText by remember(item.productId) { 
        mutableStateOf(if (currentProduct.makingPercent > 0) currentProduct.makingPercent.toString() else "") 
    }
    var labourRateText by remember(item.productId) { 
        mutableStateOf(if (currentProduct.labourRate > 0) currentProduct.labourRate.toString() else "") 
    }
    
    // Get making % and labour rate from local state or product (for recalculation preview)
    val makingPercent = makingPercentText.toDoubleOrNull() ?: currentProduct.makingPercent
    val labourRate = labourRateText.toDoubleOrNull() ?: currentProduct.labourRate
    
    // Track if there are unsaved changes
    val hasUnsavedChanges = remember(makingPercentText, labourRateText, currentProduct.makingPercent, currentProduct.labourRate) {
        val currentMakingPercent = makingPercentText.toDoubleOrNull() ?: 0.0
        val currentLabourRate = labourRateText.toDoubleOrNull() ?: 0.0
        val savedMakingPercent = currentProduct.makingPercent
        val savedLabourRate = currentProduct.labourRate
        
        // Check if values have changed (with small tolerance for floating point comparison)
        kotlin.math.abs(currentMakingPercent - savedMakingPercent) > 0.001 ||
        kotlin.math.abs(currentLabourRate - savedLabourRate) > 0.001
    }
    
    // Notify parent of unsaved changes state for preview in shopping cart
    LaunchedEffect(hasUnsavedChanges, makingPercent, labourRate) {
        if (hasUnsavedChanges) {
            onUnsavedChangesStateChange(true, makingPercent, labourRate)
        } else {
            onUnsavedChangesStateChange(false, null, null)
        }
    }
    
    // Function to save changes
    fun saveChanges() {
        val percent = makingPercentText.toDoubleOrNull() ?: 0.0
        val rate = labourRateText.toDoubleOrNull() ?: 0.0
        val updatedProduct = currentProduct.copy(
            makingPercent = percent,
            labourRate = rate
        )
        onItemUpdate(index, item.copy(product = updatedProduct))
        // Clear unsaved changes state
        onUnsavedChangesStateChange(false, null, null)
    }
    
    // Notify parent of save function
    LaunchedEffect(hasUnsavedChanges) {
        onSaveChangesFunctionChange(if (hasUnsavedChanges) ::saveChanges else null)
    }
    
    // Ensure remember block recomputes when product changes (including makingPercent and labourRate updates)
    // Use local state values (makingPercent, labourRate) for preview calculations even before saving
    val calculatedValues = remember(
        item.grossWeight,
        item.quantity,
        metalRates, 
        metalKarat, 
        makingPercent,  // Use local state for preview (shows unsaved changes)
        labourRate,      // Use local state for preview (shows unsaved changes)
        editingField
    ) {
        println("🔍 DETAIL PANEL CALCULATION START - ${currentProduct.name}")
        
        // Use ProductPriceCalculator logic (same as ProductPriceCalculator.kt)
        val grossWeight = if (item.grossWeight > 0) item.grossWeight else currentProduct.totalWeight
        val makingPercentage = makingPercent // Use from fieldValues or product
        val labourRatePerGram = labourRate // Use from fieldValues or product
        val quantity = item.quantity

        // Extract kundan and jarkan from stones array
        val kundanStones = currentProduct.stones.filter { it.name.equals("Kundan", ignoreCase = true) }
        val jarkanStones = currentProduct.stones.filter { it.name.equals("Jarkan", ignoreCase = true) }
        
        // Sum all Kundan prices and weights
        val kundanPrice = kundanStones.sumOf { it.amount }
        val kundanWeight = kundanStones.sumOf { it.weight }
        
        // Sum all Jarkan prices and weights
        val jarkanPrice = jarkanStones.sumOf { it.amount }
        val jarkanWeight = jarkanStones.sumOf { it.weight }
        
        // Get material rate (fetched from metal rates, same as ProductPriceCalculator)
        val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
        val karatForRate = if (item.metal.isNotEmpty()) {
            item.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(currentProduct.materialType)
        } else {
            extractKaratFromMaterialType(currentProduct.materialType)
        }
        val collectionRate = try {
            ratesVM.calculateRateForMaterial(currentProduct.materialId, currentProduct.materialType, karatForRate)
        } catch (e: Exception) { 0.0 }
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
            goldRatePerGram = goldRatePerGram
        )
        
        // Use the same calculation function as ProductPriceCalculator
        val result = calculateProductPrice(priceInputs)
        
        // Calculate per-item total, then multiply by quantity (no discount or GST)
        val perItemTotal = result.totalProductPrice
        val totalCharges = perItemTotal * quantity
        val finalAmount = totalCharges
        
        // Calculate component totals for display
        val goldPriceTotal = result.goldPrice * quantity
        val labourChargesTotal = result.labourCharges * quantity
        val kundanPriceTotal = result.kundanPrice * quantity
        val jarkanPriceTotal = result.jarkanPrice * quantity

        println("💵 Detail Panel Amount Breakdown (ProductPriceCalculator logic):")
        println("   Gross Weight: ${grossWeight}g")
        println("   Making %: ${makingPercentage}%")
        println("   Making Weight: ${result.makingWeight}g")
        println("   New Weight: ${result.newWeight}g")
        println("   Effective Gold Weight: ${result.effectiveGoldWeight}g")
        println("   Gold Rate: ₹${goldRatePerGram}/g")
        println("   Gold Price (per item): ₹${result.goldPrice}")
        println("   Kundan Price (per item): ₹${result.kundanPrice} (Weight: ${kundanWeight}g)")
        println("   Jarkan Price (per item): ₹${result.jarkanPrice} (Weight: ${jarkanWeight}g)")
        println("   Labour Charges (per item): ₹${result.labourCharges} (Rate: ₹${labourRatePerGram}/g × New Weight: ${result.newWeight}g)")
        println("   Per Item Total: ₹${perItemTotal}")
        println("   Quantity: $quantity")
        println("   Total Charges: ₹$totalCharges")
        println("   Final Amount: ₹$finalAmount")
        println("🔍 DETAIL PANEL CALCULATION END")

        mapOf(
            "grossWeight" to grossWeight,
            "lessWeight" to 0.0, // lessWeight not used in ProductPriceCalculator logic
            "netWeight" to result.effectiveGoldWeight, // Use effective gold weight
            "quantity" to quantity.toDouble(),
            "makingChargesPerGram" to labourRatePerGram,
            "makingWeight" to result.makingWeight,
            "newWeight" to result.newWeight,
            "effectiveGoldWeight" to result.effectiveGoldWeight,
            "cwWeight" to (kundanWeight + jarkanWeight), // Combined kundan + jarkan weight
            "stoneRate" to 0.0, // Not used in ProductPriceCalculator logic
            "vaCharges" to 0.0, // Not used in ProductPriceCalculator logic (labour charges used instead)
            "baseAmount" to goldPriceTotal, // Gold price total
            "makingCharges" to labourChargesTotal, // Labour charges total
            "stoneAmount" to (kundanPriceTotal + jarkanPriceTotal), // Kundan + Jarkan total
            "goldPrice" to goldPriceTotal,
            "kundanPrice" to kundanPriceTotal,
            "jarkanPrice" to jarkanPriceTotal,
            "labourCharges" to labourChargesTotal,
            "totalCharges" to totalCharges,
            "finalAmount" to finalAmount,
            "metalRate" to goldRatePerGram,
            "perItemTotal" to perItemTotal
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
                    // Display barcode IDs with quantity in brackets
                    val barcodeDisplay = if (item.selectedBarcodeIds.isNotEmpty()) {
                        val barcodes = item.selectedBarcodeIds.joinToString(", ")
                        "$barcodes (Qty: ${item.quantity})"
                    } else {
                        "No barcode (Qty: ${item.quantity})"
                    }
                    InfoRow("Barcode ID", barcodeDisplay, Icons.Default.Code)
                }
            }

            // Basic Information Section (removed - now empty or can be removed entirely)
            // Note: Metal Type moved to Metal Information, Quantity moved to Barcode ID section

            // Weight Details Section
            EditableSection(
                icon = Icons.Default.Edit,
                title = "Weight Details"
            ) {
                // Gross Weight
                CalculatedValueRow(
                    label = "Gross Weight",
                    value = formatWeight(calculatedValues["grossWeight"] ?: 0.0),
                    color = GoldPrimary,
                    icon = Icons.Default.CheckCircle
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Making % (Editable) - Simple text field like Total Weight in AddEditProductScreen
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Making %",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedTextField(
                        value = makingPercentText,
                        onValueChange = { 
                            makingPercentText = it
                            // Don't save immediately - wait for "Save Changes" button
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp, max = 60.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
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
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Effective Weight (New Weight)
                CalculatedValueRow(
                    label = "Effective Weight (New Weight)",
                    value = formatWeight(calculatedValues["newWeight"] ?: 0.0),
                    color = GoldPrimary,
                    icon = Icons.Default.Done
                )
            }

            // Metal Information Section
            EditableSection(
                icon = Icons.Default.CheckCircle,
                title = "Metal Information"
            ) {
                // Metal Type (Editable)
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
                Spacer(modifier = Modifier.height(8.dp))
                
                // Material Name
                CalculatedValueRow(
                    label = "Material Name",
                    value = productsViewModel.getMaterialName(currentProduct.materialId),
                    color = GoldPrimary,
                    icon = Icons.Default.CheckCircle
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Metal Weight (Material Weight)
                CalculatedValueRow(
                    label = "Metal Weight",
                    value = formatWeight(currentProduct.materialWeight.takeIf { it > 0 } ?: calculatedValues["grossWeight"] ?: 0.0),
                    color = GoldPrimary,
                    icon = Icons.Default.CheckCircle
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Metal Rate
                CalculatedValueRow(
                    label = "Metal Rate",
                    value = CurrencyFormatter.formatRupees(calculatedValues["metalRate"] ?: 0.0),
                    color = GoldPrimary,
                    icon = Icons.Default.Edit,
                    isLarge = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Effective Metal Weight
                CalculatedValueRow(
                    label = "Effective Metal Weight",
                    value = formatWeight(calculatedValues["effectiveGoldWeight"] ?: 0.0),
                    color = GoldPrimary,
                    icon = Icons.Default.Done
                )
            }

            // Labour Section
            EditableSection(
                icon = Icons.Default.Edit,
                title = "Labour"
            ) {
                // Labour Rate (Editable) - Simple text field like Total Weight in AddEditProductScreen
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Labour Rate (per gram)",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedTextField(
                        value = labourRateText,
                        onValueChange = { 
                            labourRateText = it
                            // Don't save immediately - wait for "Save Changes" button
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp, max = 60.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
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
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Labour Charges (Calculated)
                CalculatedValueRow(
                    label = "Labour Charges",
                    value = CurrencyFormatter.formatRupees(calculatedValues["labourCharges"] ?: 0.0),
                    color = GoldPrimary,
                    icon = Icons.Default.CheckCircle,
                    isLarge = true
                )
            }
            

            // Stone Details Section - Show all stones from stones array
            if (currentProduct.hasStones && currentProduct.stones.isNotEmpty()) {
                EditableSection(
                    icon = Icons.Default.Diamond,
                    title = "Stone Details"
                ) {
                    currentProduct.stones.forEachIndexed { stoneIndex, stone ->
                        if (stoneIndex > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        // Stone Name
                        CalculatedValueRow(
                            label = "Stone Name",
                            value = stone.name.ifEmpty { "N/A" },
                            color = GoldPrimary,
                            icon = Icons.Default.Diamond
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                                CalculatedValueRow(
                                    label = "Purity",
                                    value = stone.purity.ifEmpty { "N/A" },
                                    color = GoldPrimary,
                                    icon = Icons.Default.CheckCircle
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                                CalculatedValueRow(
                                    label = "Quantity",
                                    value = stone.quantity.toString(),
                                    color = GoldPrimary,
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
                                CalculatedValueRow(
                                    label = "Weight (g)",
                                    value = formatWeight(stone.weight),
                                    color = GoldPrimary,
                                    icon = Icons.Default.CheckCircle
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                                CalculatedValueRow(
                                    label = "Rate",
                                    value = CurrencyFormatter.formatRupees(stone.rate),
                                    color = GoldPrimary,
                                icon = Icons.Default.Edit
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                        
                    CalculatedValueRow(
                            label = "Amount",
                            value = CurrencyFormatter.formatRupees(stone.amount),
                        color = GoldPrimary,
                            icon = Icons.Default.CheckCircle,
                            isLarge = true
                        )
                    }
                    
                    // Total Stone Amount
                    if (currentProduct.stones.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        CalculatedValueRow(
                            label = "Total Stone Amount",
                        value = CurrencyFormatter.formatRupees(calculatedValues["stoneAmount"] ?: 0.0),
                        color = GoldPrimary,
                            icon = Icons.Default.CheckCircle,
                            isLarge = true
                    )
                    }
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
                AmountRow("Labour Charges", calculatedValues["labourCharges"] ?: 0.0, GoldPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                AmountRow("Stone Amount", calculatedValues["stoneAmount"] ?: 0.0, GoldPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))
                AmountRow("Total Charges", calculatedValues["finalAmount"] ?: calculatedValues["totalCharges"] ?: 0.0, GoldPrimary, isBold = true)
                
                // Show per-product price if quantity > 1
                val quantity = (calculatedValues["quantity"] ?: 1.0).toInt()
                if (quantity > 1) {
                    val totalCharges = calculatedValues["totalCharges"] ?: 0.0
                    val perProductCharges = totalCharges / quantity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "(${CurrencyFormatter.formatRupees(perProductCharges)} per item)",
                            fontSize = 11.sp,
                            color = Color(0xFF666666),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
            
            // Blank white card below Calculated Amounts (same width as Save Changes button)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .width(180.dp)
                    .height(60.dp),
                backgroundColor = Color.White,
                shape = RoundedCornerShape(8.dp),
                elevation = 2.dp
            ) {
                // Empty card - just for spacing/visual consistency
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
            Text(
            CurrencyFormatter.formatRupees(amount),
                fontSize = if (isBold) 15.sp else 14.sp,
                color = color,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
            )
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
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isReadOnly: Boolean = false
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
                    .then(if (!isReadOnly) Modifier.clickable { onEditingFieldChange(fieldKey) } else Modifier)
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                    .background(if (isReadOnly) BackgroundGray.copy(alpha = 0.5f) else BackgroundGray, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    currentValue,
                    fontSize = 14.sp,
                    color = if (isReadOnly) TextSecondary else TextPrimary,
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
        "lessWeight" -> if (item.lessWeight > 0) item.lessWeight.toString() else "0.0" // lessWeight removed from Product
        "netWeight" -> item.netWeight.toString()
        "makingRate" -> if (item.makingCharges > 0) item.makingCharges.toString() else if (product.makingRate > 0) product.makingRate.toString() else "0.0" // defaultMakingRate removed from Product
        "cwWeight" -> if (item.cwWeight > 0) item.cwWeight.toString() else product.stoneWeight.toString() // Use stoneWeight instead of cwWeight
        "stoneRate" -> {
            val firstStone = product.stones.firstOrNull()
            if (item.stoneRate > 0) item.stoneRate.toString() else (firstStone?.rate ?: 0.0).toString()
        }
        "vaCharges" -> if (item.va > 0) item.va.toString() else product.labourCharges.toString() // Use labourCharges instead of vaCharges
        "discountPercent" -> item.discountPercent.toString()
        "makingPercent" -> product.makingPercent.toString()
        "labourRate" -> product.labourRate.toString()
        "materialName" -> "" // Read-only, handled separately
        "stoneName" -> {
            val firstStone = product.stones.firstOrNull()
            firstStone?.name ?: ""
        }
        "stoneQuantity" -> {
            val firstStone = product.stones.firstOrNull()
            if (item.stoneQuantity > 0) item.stoneQuantity.toString() else (firstStone?.quantity ?: 0.0).toString()
        }
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
            "makingPercent" -> {
                // Allow empty string to be treated as 0
                val percent = if (value.trim().isEmpty()) {
                    0.0
                } else {
                    value.trim().toDoubleOrNull() ?: item.product.makingPercent
                }
                println("   Updating making percent: ${item.product.makingPercent} -> $percent")
                // Update product's makingPercent - this will trigger recalculation via remember block
                val updatedProduct = item.product.copy(makingPercent = percent)
                onItemUpdate(index, item.copy(product = updatedProduct))
            }
            "labourRate" -> {
                // Allow empty string to be treated as 0
                val rate = if (value.trim().isEmpty()) {
                    0.0
                } else {
                    value.trim().toDoubleOrNull() ?: item.product.labourRate
                }
                println("   Updating labour rate: ${item.product.labourRate} -> $rate")
                // Update product's labourRate - this will trigger recalculation via remember block
                val updatedProduct = item.product.copy(labourRate = rate)
                onItemUpdate(index, item.copy(product = updatedProduct))
            }
        }
    } catch (e: Exception) {
        println("❌ Error saving field $fieldKey with value $value: ${e.message}")
    }
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