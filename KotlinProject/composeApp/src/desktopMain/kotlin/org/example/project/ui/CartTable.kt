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
import java.text.DecimalFormat

// Color Palette
private val GoldPrimary = Color(0xFFFFD700)
private val GoldDark = Color(0xFFDAA520)
private val GoldLight = Color(0xFFFFF8DC)
private val DarkGreen = Color(0xFF1B5E20)
private val MediumGreen = Color(0xFF2E7D32)
private val LightGreen = Color(0xFF4CAF50)
private val BackgroundGray = Color(0xFFF5F5F5)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF212121)
private val TextSecondary = Color(0xFF757575)
private val AccentOrange = Color(0xFFFF6F00)
private val AccentBlue = Color(0xFF1976D2)
private val AccentPurple = Color(0xFF6A1B9A)
private val ErrorRed = Color(0xFFD32F2F)

@Composable
fun CartTable(
    cartItems: List<CartItem>,
    onItemUpdate: (Int, CartItem) -> Unit,
    onItemRemove: (Int) -> Unit,
    cartImages: Map<String, androidx.compose.ui.graphics.ImageBitmap> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val metalRates by MetalRatesManager.metalRates
    var selectedItemIndex by remember { mutableStateOf<Int?>(null) }
    var editingField by remember { mutableStateOf<String?>(null) }
    var fieldValues by remember { mutableStateOf<Map<String, TextFieldValue>>(emptyMap()) }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun onEditingFieldChange(newField: String?) {
        editingField?.let { currentField ->
            if (currentField != newField) {
                val currentValue = fieldValues[currentField]?.text ?: ""
                saveFieldValue(currentField, currentValue, cartItems, onItemUpdate)
            }
        }
        editingField = newField
        newField?.let { field ->
            if (!fieldValues.containsKey(field)) {
                val currentValue = getCurrentFieldValue(field, cartItems)
                fieldValues = fieldValues + (field to TextFieldValue(
                    text = currentValue,
                    selection = TextRange(0, currentValue.length)
                ))
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
        shape = RoundedCornerShape(16.dp),
        elevation = 12.dp,
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
                    onItemSelected = { index -> selectedItemIndex = index },
                    onItemRemove = { index ->
                        saveAndCloseEditing()
                        onItemRemove(index)
                        if (selectedItemIndex == index) {
                            selectedItemIndex = null
                        }
                    },
                    cartImages = cartImages,
                    metalRates = metalRates,
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                )

                // Elegant Divider
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    GoldLight.copy(alpha = 0.3f),
                                    GoldPrimary.copy(alpha = 0.5f),
                                    GoldLight.copy(alpha = 0.3f)
                                )
                            )
                        )
                )

                // Right Side: Detail Panel
                if (selectedItemIndex != null && selectedItemIndex!! < cartItems.size) {
                    DetailPanel(
                        item = cartItems[selectedItemIndex!!],
                        index = selectedItemIndex!!,
                        metalRates = metalRates,
                        editingField = editingField,
                        fieldValues = fieldValues,
                        onEditingFieldChange = ::onEditingFieldChange,
                        onFieldValueChange = { fieldKey, value ->
                            fieldValues = fieldValues + (fieldKey to value)
                        },
                        onSaveAndCloseEditing = ::saveAndCloseEditing,
                        onItemUpdate = onItemUpdate,
                        onClose = { selectedItemIndex = null },
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Enhanced Header with Golden Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GoldDark, GoldPrimary)
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Shopping Cart",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                    Text(
                        "${cartItems.size} ${if (cartItems.size == 1) "item" else "items"}",
                        fontSize = 12.sp,
                        color = DarkGreen.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = DarkGreen,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // List with padding
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(cartItems) { index, item ->
                CompactCartItem(
                    item = item,
                    index = index,
                    isSelected = selectedIndex == index,
                    onClick = { onItemSelected(index) },
                    onRemove = { onItemRemove(index) },
                    cartImage = cartImages[item.productId],
                    metalRates = metalRates
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
    metalRates: org.example.project.data.MetalRates
) {
    val metalKarat = if (item.metal.isNotEmpty()) {
        item.metal.replace("K", "").toIntOrNull() ?: item.product.karat
    } else {
        item.product.karat
    }

    val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
    val goldRate = if (item.customGoldRate > 0) item.customGoldRate else defaultGoldRate
    val silverRate = metalRates.getSilverRateForPurity(999)

    val finalAmount = remember(item, metalRates, metalKarat) {
        val netWeight = item.grossWeight - item.lessWeight
        val baseAmount = when {
            item.product.materialType.contains("gold", ignoreCase = true) -> netWeight * goldRate * item.quantity
            item.product.materialType.contains("silver", ignoreCase = true) -> netWeight * silverRate * item.quantity
            else -> netWeight * goldRate * item.quantity
        }
        val makingCharges = netWeight * item.makingCharges * item.quantity
        val stoneAmount = item.cwWeight * item.stoneRate * item.quantity
        val totalCharges = baseAmount + makingCharges + stoneAmount + item.va
        val discountAmount = totalCharges * (item.discountPercent / 100)
        val taxableAmount = totalCharges - discountAmount
        val totalGst = taxableAmount * 0.03
        taxableAmount + totalGst
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = if (isSelected) 8.dp else 3.dp,
        backgroundColor = if (isSelected) GoldLight else CardWhite
    ) {
        Box {
            // Selected indicator stripe
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(5.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(GoldPrimary, GoldDark)
                            )
                        )
                        .align(Alignment.CenterStart)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (isSelected) 20.dp else 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Enhanced Product Image
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(BackgroundGray)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) GoldPrimary else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(12.dp)
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
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Product Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        item.product.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MediumGreen.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "ID: ${item.product.id}",
                                fontSize = 11.sp,
                                color = MediumGreen,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${item.quantity}x",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                        Text(
                            " • ${item.metal.ifEmpty { "${item.product.karat}K" }}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Price with elegant styling
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "₹",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MediumGreen
                        )
                        Text(
                            formatCurrencyNumber(finalAmount),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MediumGreen
                        )
                    }
                }

                // Remove Button with better design
                IconButton(
                    onClick = { onRemove() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(ErrorRed.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Remove",
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
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
    val metalKarat = if (item.metal.isNotEmpty()) {
        item.metal.replace("K", "").toIntOrNull() ?: item.product.karat
    } else {
        item.product.karat
    }

    val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
    val goldRate = if (item.customGoldRate > 0) item.customGoldRate else defaultGoldRate
    val silverRate = metalRates.getSilverRateForPurity(999)

    val calculatedValues = remember(item, metalRates, metalKarat) {
        val grossWeight = item.grossWeight
        val lessWeight = item.lessWeight
        val quantity = item.quantity
        val makingChargesPerGram = item.makingCharges
        val cwWeight = item.cwWeight
        val stoneRate = item.stoneRate
        val vaCharges = item.va
        val discountPercent = item.discountPercent

        val netWeight = grossWeight - lessWeight
        val baseAmount = when {
            item.product.materialType.contains("gold", ignoreCase = true) -> netWeight * goldRate * quantity
            item.product.materialType.contains("silver", ignoreCase = true) -> netWeight * silverRate * quantity
            else -> netWeight * goldRate * quantity
        }
        val makingCharges = netWeight * makingChargesPerGram * quantity
        val stoneAmount = cwWeight * stoneRate * quantity
        val totalCharges = baseAmount + makingCharges + stoneAmount + vaCharges
        val discountAmount = totalCharges * (discountPercent / 100)
        val taxableAmount = totalCharges - discountAmount
        val cgst = taxableAmount * 0.015
        val sgst = taxableAmount * 0.015
        val totalGst = cgst + sgst
        val finalAmount = taxableAmount + totalGst

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
            "goldRate" to goldRate
        )
    }

    Column(
        modifier = modifier
            .background(CardWhite)
    ) {
        // Enhanced Header with Golden Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GoldDark, GoldPrimary)
                    )
                )
                .padding(12.dp)
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
                        tint = DarkGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Item Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(DarkGreen.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DarkGreen
                    )
                }
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Product Image (Large) with elegant styling
            if (cartImage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 6.dp
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

            // Product Info Card with modern design
            ElegantInfoCard(
                icon = Icons.Default.CheckCircle,
                title = "Product Information"
            ) {
                InfoRow("Product Name", item.product.name, Icons.Default.CheckCircle)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Item ID", item.product.id, Icons.Default.CheckCircle)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Category", item.product.categoryId, Icons.Default.CheckCircle)
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Material", item.product.materialType, Icons.Default.CheckCircle)
            }

            // Basic Information Section
            EditableSection(
                icon = Icons.Default.Edit,
                title = "Basic Information"
            ) {
                EditableDetailField(
                    label = "Metal Type",
                    fieldKey = "metal_$index",
                    currentValue = item.metal.ifEmpty { "${item.product.karat}K" },
                    editingField = editingField,
                    fieldValues = fieldValues,
                    onEditingFieldChange = onEditingFieldChange,
                    onFieldValueChange = onFieldValueChange,
                    onSaveAndCloseEditing = onSaveAndCloseEditing,
                    keyboardType = KeyboardType.Text,
                    icon = Icons.Default.Edit
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                    Box(modifier = Modifier.weight(1f)) {
                        EditableDetailField(
                            label = "Size/Weight",
                            fieldKey = "size_$index",
                            currentValue = "${item.selectedWeight}g",
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
            }

            // Weights Section
            EditableSection(
                icon = Icons.Default.Edit,
                title = "Weight Details"
            ) {
                EditableDetailField(
                    label = "Gross Weight (g)",
                    fieldKey = "grossWeight_$index",
                    currentValue = item.grossWeight.toString(),
                    editingField = editingField,
                    fieldValues = fieldValues,
                    onEditingFieldChange = onEditingFieldChange,
                    onFieldValueChange = onFieldValueChange,
                    onSaveAndCloseEditing = onSaveAndCloseEditing,
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(16.dp))
                EditableDetailField(
                    label = "Less Weight (g)",
                    fieldKey = "lessWeight_$index",
                    currentValue = item.lessWeight.toString(),
                    editingField = editingField,
                    fieldValues = fieldValues,
                    onEditingFieldChange = onEditingFieldChange,
                    onFieldValueChange = onFieldValueChange,
                    onSaveAndCloseEditing = onSaveAndCloseEditing,
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(16.dp))
                CalculatedValueRow(
                    label = "Net Weight",
                    value = formatWeight(calculatedValues["netWeight"] ?: 0.0),
                    color = MediumGreen,
                    icon = Icons.Default.Done
                )
            }

            // Rates Section with premium design
            EditableSection(
                icon = Icons.Default.CheckCircle,
                title = "Rates & Charges"
            ) {
                CalculatedValueRow(
                    label = "Gold Rate",
                    value = formatCurrency(calculatedValues["goldRate"] ?: 0.0),
                    color = GoldDark,
                    icon = Icons.Default.Edit,
                    isLarge = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                EditableDetailField(
                    label = "Making Rate (per gram)",
                    fieldKey = "makingRate_$index",
                    currentValue = item.makingCharges.toString(),
                    editingField = editingField,
                    fieldValues = fieldValues,
                    onEditingFieldChange = onEditingFieldChange,
                    onFieldValueChange = onFieldValueChange,
                    onSaveAndCloseEditing = onSaveAndCloseEditing,
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditableDetailField(
                            label = "CW Weight",
                            fieldKey = "cwWeight_$index",
                            currentValue = item.cwWeight.toString(),
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
                            currentValue = item.stoneRate.toString(),
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
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditableDetailField(
                            label = "VA Charges",
                            fieldKey = "vaCharges_$index",
                            currentValue = item.va.toString(),
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
                            label = "Discount %",
                            fieldKey = "discountPercent_$index",
                            currentValue = item.discountPercent.toString(),
                            editingField = editingField,
                            fieldValues = fieldValues,
                            onEditingFieldChange = onEditingFieldChange,
                            onFieldValueChange = onFieldValueChange,
                            onSaveAndCloseEditing = onSaveAndCloseEditing,
                            keyboardType = KeyboardType.Decimal,
                            icon = Icons.Default.CheckCircle
                        )
                    }
                }
            }

            // Calculated Amounts Section
            ElegantInfoCard(
                icon = Icons.Default.Edit,
                title = "Calculated Amounts",
                accentColor = AccentBlue
            ) {
                AmountRow("Base Amount", calculatedValues["baseAmount"] ?: 0.0, MediumGreen)
                Spacer(modifier = Modifier.height(8.dp))
                AmountRow("Making Charges", calculatedValues["makingCharges"] ?: 0.0, AccentBlue)
                Spacer(modifier = Modifier.height(8.dp))
                AmountRow("Stone Amount", calculatedValues["stoneAmount"] ?: 0.0, AccentPurple)
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                AmountRow("Total Charges", calculatedValues["totalCharges"] ?: 0.0, AccentBlue, isBold = true)
                Spacer(modifier = Modifier.height(8.dp))
                AmountRow("Discount Amount", calculatedValues["discountAmount"] ?: 0.0, ErrorRed)
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                AmountRow("Taxable Amount", calculatedValues["taxableAmount"] ?: 0.0, AccentOrange, isBold = true)
            }

            // Tax Details Section
            ElegantInfoCard(
                icon = Icons.Default.Edit,
                title = "Tax Details",
                accentColor = AccentPurple
            ) {
                AmountRow("CGST (1.5%)", calculatedValues["cgst"] ?: 0.0, AccentPurple)
                Spacer(modifier = Modifier.height(8.dp))
                AmountRow("SGST (1.5%)", calculatedValues["sgst"] ?: 0.0, AccentPurple)
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                AmountRow("Total GST", calculatedValues["totalGst"] ?: 0.0, AccentPurple, isBold = true)
            }

            // Final Amount Highlight - Premium Design
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    AccentOrange.copy(alpha = 0.1f),
                                    GoldLight.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(AccentOrange, GoldPrimary)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "FINAL AMOUNT",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentOrange
                                )
                            }
                            Text(
                                "Including all taxes",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(start = 28.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "₹",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentOrange
                                )
                                Text(
                                    formatCurrencyNumber(calculatedValues["finalAmount"] ?: 0.0),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentOrange
                                )
                            }
                        }
                    }
                }
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
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(GoldLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = GoldDark
                )
            }
            Text(
                "Select an item",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "Click on any item from the list\nto view and edit its details",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun ElegantInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accentColor: Color = GoldDark,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        backgroundColor = CardWhite
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    title,
                    fontSize = 15.sp,
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
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        backgroundColor = CardWhite
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(GoldDark.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = GoldDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    title,
                    fontSize = 15.sp,
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
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                label,
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            value,
            fontSize = 14.sp,
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
            .padding(horizontal = if (isBold) 12.dp else 0.dp, vertical = if (isBold) 8.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = if (isBold) 15.sp else 14.sp,
            color = TextPrimary,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "₹",
                fontSize = if (isBold) 14.sp else 13.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                color = color
            )
            Text(
                formatCurrencyNumber(amount),
                fontSize = if (isBold) 16.sp else 15.sp,
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
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(if (isLarge) 22.dp else 18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                label,
                fontSize = if (isLarge) 15.sp else 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            value,
            fontSize = if (isLarge) 17.sp else 15.sp,
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
                    tint = GoldDark,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                label,
                fontSize = 12.sp,
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
                    .height(44.dp)
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
                    focusedBorderColor = GoldDark,
                    unfocusedBorderColor = GoldDark,
                    cursorColor = GoldDark,
                    backgroundColor = GoldLight.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable { onEditingFieldChange(fieldKey) }
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .background(BackgroundGray, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    currentValue,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
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
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
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
                    modifier = Modifier.size(60.dp),
                    tint = GoldDark
                )
            }
            Text(
                "Your cart is empty",
                fontSize = 24.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Add products to start building your order",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper functions
private fun getCurrentFieldValue(fieldKey: String, cartItems: List<CartItem>): String {
    val parts = fieldKey.split("_")
    if (parts.size < 2) return ""
    val index = parts[1].toIntOrNull() ?: return ""
    if (index >= cartItems.size) return ""

    val item = cartItems[index]
    val fieldName = parts[0]

    return when (fieldName) {
        "metal" -> item.metal.ifEmpty { "${item.product.karat}K" }
        "customGoldRate" -> if (item.customGoldRate > 0) item.customGoldRate.toString() else ""
        "size" -> "${item.selectedWeight}"
        "qty" -> item.quantity.toString()
        "grossWeight" -> item.grossWeight.toString()
        "lessWeight" -> item.lessWeight.toString()
        "netWeight" -> item.netWeight.toString()
        "makingRate" -> item.makingCharges.toString()
        "cwWeight" -> item.cwWeight.toString()
        "stoneRate" -> item.stoneRate.toString()
        "vaCharges" -> item.va.toString()
        "discountPercent" -> item.discountPercent.toString()
        else -> ""
    }
}

private fun saveFieldValue(
    fieldKey: String,
    value: String,
    cartItems: List<CartItem>,
    onItemUpdate: (Int, CartItem) -> Unit
) {
    val parts = fieldKey.split("_")
    if (parts.size < 2) return

    val index = parts[1].toIntOrNull() ?: return
    if (index >= cartItems.size) return

    val item = cartItems[index]
    val fieldName = parts[0]

    try {
        when (fieldName) {
            "metal" -> {
                onItemUpdate(index, item.copy(metal = value.trim()))
            }
            "customGoldRate" -> {
                val cleanValue = value.replace("₹", "").replace(",", "").trim()
                val rate = if (cleanValue.isNotEmpty()) cleanValue.toDoubleOrNull() ?: 0.0 else 0.0
                onItemUpdate(index, item.copy(customGoldRate = rate))
            }
            "size" -> {
                val cleanValue = value.replace("g", "").trim()
                val weight = if (cleanValue.isNotEmpty()) cleanValue.toDoubleOrNull() ?: item.selectedWeight else item.selectedWeight
                onItemUpdate(index, item.copy(selectedWeight = weight))
            }
            "qty" -> {
                val qty = if (value.trim().isNotEmpty()) value.trim().toIntOrNull() ?: item.quantity else item.quantity
                onItemUpdate(index, item.copy(quantity = qty))
            }
            "grossWeight" -> {
                val weight = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.grossWeight else item.grossWeight
                val updatedItem = item.copy(
                    grossWeight = weight,
                    netWeight = weight - item.lessWeight
                )
                onItemUpdate(index, updatedItem)
            }
            "lessWeight" -> {
                val weight = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.lessWeight else item.lessWeight
                val updatedItem = item.copy(
                    lessWeight = weight,
                    netWeight = item.grossWeight - weight
                )
                onItemUpdate(index, updatedItem)
            }
            "netWeight" -> {
                val weight = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.netWeight else item.netWeight
                onItemUpdate(index, item.copy(netWeight = weight))
            }
            "makingRate" -> {
                val rate = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.makingCharges else item.makingCharges
                onItemUpdate(index, item.copy(makingCharges = rate))
            }
            "cwWeight" -> {
                val weight = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.cwWeight else item.cwWeight
                onItemUpdate(index, item.copy(cwWeight = weight))
            }
            "stoneRate" -> {
                val rate = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.stoneRate else item.stoneRate
                onItemUpdate(index, item.copy(stoneRate = rate))
            }
            "vaCharges" -> {
                val va = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.va else item.va
                onItemUpdate(index, item.copy(va = va))
            }
            "discountPercent" -> {
                val percent = if (value.trim().isNotEmpty()) value.trim().toDoubleOrNull() ?: item.discountPercent else item.discountPercent
                onItemUpdate(index, item.copy(discountPercent = percent))
            }
        }
    } catch (e: Exception) {
        println("Error saving field $fieldKey with value $value: ${e.message}")
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