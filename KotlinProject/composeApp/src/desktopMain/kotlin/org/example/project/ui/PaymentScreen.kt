package org.example.project.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import org.example.project.data.CartItem
import org.example.project.data.MetalRatesManager
import org.example.project.JewelryAppInitializer
import org.example.project.utils.CurrencyFormatter
import org.example.project.viewModels.PaymentViewModel
import org.example.project.viewModels.CartViewModel
import org.example.project.viewModels.CustomerViewModel

import org.example.project.data.PaymentMethod
import org.example.project.data.DiscountType
import org.example.project.data.PaymentSplit
import org.example.project.data.ExchangeGold
import org.example.project.ui.ProductPriceInputs
import org.example.project.ui.calculateProductPrice
import org.example.project.ui.calculateStonePrices
import org.example.project.viewModels.ProductsViewModel
import org.example.project.data.extractKaratFromMaterialType

enum class GstType { ZERO, THREE, FIVE, CUSTOM }

@Composable
fun PaymentScreen(
    paymentViewModel: PaymentViewModel,
    cartViewModel: CartViewModel,
    customerViewModel: CustomerViewModel,
    onBack: () -> Unit,
    onPaymentComplete: () -> Unit,
    productsViewModel: ProductsViewModel
) {
    // REQUIRE: Customer must be selected before opening PaymentScreen
    val selectedCustomer = requireNotNull(customerViewModel.selectedCustomer.value) {
        "PaymentScreen opened without selecting customer. Please select a customer first."
    }
    
    var showExchangeGold by remember { mutableStateOf(false) }
    var exchangeGold by remember { mutableStateOf<ExchangeGold?>(null) }
    val cart by cartViewModel.cart
    val cartImages by cartViewModel.cartImages
    val selectedPaymentMethod by paymentViewModel.selectedPaymentMethod
    val discountType by paymentViewModel.discountType
    val discountValue by paymentViewModel.discountValue
    val discountAmountState by paymentViewModel.discountAmount // Track applied discount amount
    val exchangeGoldValue = exchangeGold?.value ?: 0.0
    val isProcessing by paymentViewModel.isProcessing
    val errorMessage by paymentViewModel.errorMessage
    val successMessage by paymentViewModel.successMessage

    // Payment split state - always visible by default
    var cashAmount by remember { mutableStateOf("") }
    var cardAmount by remember { mutableStateOf("") }
    var bankAmount by remember { mutableStateOf("") }
    var onlineAmount by remember { mutableStateOf("") }
    var dueAmount by remember { mutableStateOf("") }
    var paymentSplit by remember { mutableStateOf<PaymentSplit?>(null) }
    
    // GST selection state
    var selectedGstType by remember { mutableStateOf<GstType?>(null) }
    var customGstValue by remember { mutableStateOf("") }
    
    // Notes state
    var notes by remember { mutableStateOf("") }
    var showNotesDialog by remember { mutableStateOf(false) }
    
    // Calculate totals using the EXACT same logic as cart screen (ProductPriceCalculator)
    val metalRates by MetalRatesManager.metalRates
    val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
    val baseSubtotal = remember(cart, metalRates) {
        val total = cart.items.sumOf { cartItem ->
            val currentProduct = cartItem.product
            
            // Use ProductPriceCalculator logic (same as CartScreen)
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
                ratesVM.calculateRateForMaterial(currentProduct.materialId, currentProduct.materialType, metalKarat)
            } catch (e: Exception) { 0.0 }
            val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
            val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else defaultGoldRate
            // Extract silver purity from material type
            val materialTypeLower = currentProduct.materialType.lowercase()
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
    
    // Keep base subtotal as is (before exchange gold reduction)
    val subtotal = baseSubtotal
    
    // Amount after reducing exchange gold (for discount and GST calculations)
    val amountAfterExchangeGold = remember(baseSubtotal, exchangeGoldValue) {
        (baseSubtotal - exchangeGoldValue).coerceAtLeast(0.0)
    }
    
    // Get GST percentage
    val gstPercentage = remember(selectedGstType, customGstValue) {
        when (selectedGstType) {
            GstType.ZERO -> 0.0
            GstType.THREE -> 3.0
            GstType.FIVE -> 5.0
            GstType.CUSTOM -> customGstValue.toDoubleOrNull() ?: 0.0
            null -> 0.0
        }
    }
    
    // Calculate discount amount - for TOTAL_PAYABLE, use reverse calculation
    // For TOTAL_PAYABLE: Solve for discount where totalPayable = (amountAfterExchangeGold - discount) * (1 + GST%)
    // discount = amountAfterExchangeGold - (totalPayable / (1 + GST%))
    // For AMOUNT and PERCENTAGE: Use applied discount amount from ViewModel (set by Apply button)
    val calculatedDiscountAmount = remember(discountValue, discountType, amountAfterExchangeGold, gstPercentage, discountAmountState) {
        if (discountType == DiscountType.TOTAL_PAYABLE && discountValue.isNotEmpty()) {
            // TOTAL_PAYABLE: Calculate automatically from entered value
            val totalPayable = discountValue.toDoubleOrNull() ?: 0.0
            if (gstPercentage == 0.0) {
                // No GST: discount = amountAfterExchangeGold - totalPayable
                (amountAfterExchangeGold - totalPayable).coerceAtLeast(0.0)
            } else {
                // With GST: totalPayable = (amountAfterExchangeGold - discount) * (1 + GST%)
                // Solving: discount = amountAfterExchangeGold - (totalPayable / (1 + GST%))
                val gstMultiplier = 1.0 + (gstPercentage / 100.0)
                val taxableAmount = totalPayable / gstMultiplier
                val discount = (amountAfterExchangeGold - taxableAmount).coerceAtLeast(0.0)
                discount
            }
        } else if (discountValue.isNotEmpty()) {
            // For PERCENTAGE and AMOUNT: Use calculateDiscountAmount which uses discountAmountState
            // This will be 0 if Apply button hasn't been clicked, or the calculated value if it has
            // The discountAmountState is set by applyDiscount() when Apply button is clicked
            paymentViewModel.calculateDiscountAmount(amountAfterExchangeGold, 0.0)
        } else {
            0.0
        }
    }
    
    // Amount after discount (exchange gold already reduced)
    val amountAfterDiscount = amountAfterExchangeGold - calculatedDiscountAmount
    
    // Calculate GST amount - always on amount after discount
    val gstAmount = remember(amountAfterDiscount, gstPercentage) {
        if (gstPercentage == 0.0) {
            0.0
        } else {
            amountAfterDiscount * (gstPercentage / 100.0)
        }
    }
    
    // Calculate final total
    // For TOTAL_PAYABLE: Use the entered total payable amount (reverse calculation result)
    // For other types: (Subtotal - Exchange Gold - Discount) + GST
    val finalTotal = remember(discountType, discountValue, amountAfterDiscount, gstAmount) {
        if (discountType == DiscountType.TOTAL_PAYABLE && discountValue.isNotEmpty()) {
            // Use the entered total payable amount directly
            // This is the result of reverse calculation: (amountAfterExchangeGold - discount) + GST
            discountValue.toDoubleOrNull() ?: (amountAfterDiscount + gstAmount)
        } else {
            // Normal calculation
            amountAfterDiscount + gstAmount
        }
    }
    
    // Calculate total amount for payment split
    val totalAmountForSplit = finalTotal

    // Calculate due amount automatically
    val calculatedDueAmount = remember(cashAmount, cardAmount, bankAmount, onlineAmount, totalAmountForSplit) {
        val paidAmount = (cashAmount.toDoubleOrNull() ?: 0.0) +
                (cardAmount.toDoubleOrNull() ?: 0.0) +
                (bankAmount.toDoubleOrNull() ?: 0.0) +
                (onlineAmount.toDoubleOrNull() ?: 0.0)
        val due = totalAmountForSplit - paidAmount
        if (due > 0) due else 0.0
    }

    // Update due amount when other amounts change
    LaunchedEffect(calculatedDueAmount) {
        dueAmount = calculatedDueAmount.toString()
    }

    // Update payment split when amounts change
    LaunchedEffect(cashAmount, cardAmount, bankAmount, onlineAmount, calculatedDueAmount) {
        val cash = cashAmount.toDoubleOrNull() ?: 0.0
        val card = cardAmount.toDoubleOrNull() ?: 0.0
        val bank = bankAmount.toDoubleOrNull() ?: 0.0
        val online = onlineAmount.toDoubleOrNull() ?: 0.0

        // Only create payment split if at least one amount is entered
        // Combine bank, card, and online into single bank field
        val bankTotal = bank + card + online
        if (cash > 0 || bankTotal > 0 || calculatedDueAmount > 0) {
            paymentSplit = PaymentSplit(
                bank = bankTotal, // Sum of bankAmount + cardAmount + onlineAmount
                cash = cash,
                dueAmount = calculatedDueAmount
            )
        } else {
            paymentSplit = null
        }
    }

    // Calculate if there's a payment split warning
    val hasPaymentSplitWarning = paymentSplit?.let { split ->
        val adjustedDueAmount = split.dueAmount - calculatedDiscountAmount
        adjustedDueAmount < 0
    } ?: false

    // Define confirm order function
    // IMPORTANT: Use snapshot state before submission to prevent desync during processing
    val onConfirmOrder: () -> Unit = run {
        // Take snapshot of current payment split state (captured once when composable is created)
        val snapshotPaymentSplit = paymentSplit?.copy()
        val snapshotSubtotal = subtotal // Base subtotal before exchange gold reduction
        val snapshotExchangeGoldValue = exchangeGoldValue
        val snapshotDiscountAmount = calculatedDiscountAmount
        val snapshotGstAmount = gstAmount
        val snapshotFinalTotal = finalTotal // Already calculated as: (subtotal - exchangeGold - discount) + GST
        val snapshotNotes = notes
        
        // Return the lambda function
        {
            // Re-capture snapshot values at call time to prevent desync
            val currentSnapshotPaymentSplit = paymentSplit?.copy()
            val currentSnapshotSubtotal = subtotal // Base subtotal before exchange gold reduction
            val currentSnapshotExchangeGoldValue = exchangeGoldValue
            val currentSnapshotDiscountAmount = calculatedDiscountAmount
            val currentSnapshotGstAmount = gstAmount
            val currentSnapshotFinalTotal = finalTotal // Already calculated as: (subtotal - exchangeGold - discount) + GST
            val currentSnapshotNotes = notes
            
            // Validate payment split if provided (using current snapshot)
            if (currentSnapshotPaymentSplit != null) {
                if (currentSnapshotPaymentSplit.exceedsTotal(currentSnapshotFinalTotal)) {
                    val difference = currentSnapshotPaymentSplit.getDifference(currentSnapshotFinalTotal)
                    paymentViewModel.setErrorMessage(
                        "Payment breakdown exceeds total amount by ₹${String.format("%.2f", difference)}"
                    )
                    return@run
                }
                if (!currentSnapshotPaymentSplit.isValid(currentSnapshotFinalTotal)) {
                    val difference = currentSnapshotPaymentSplit.getDifference(currentSnapshotFinalTotal)
                    paymentViewModel.setErrorMessage(
                        "Payment amounts don't match total. Difference: ₹${String.format("%.2f", kotlin.math.abs(difference))}"
                    )
                    return@run
                }
            }
            
            paymentViewModel.validateStockBeforeOrder(
                cart = cart,
                products = productsViewModel.products.value,
                onValidationResult = { isValid, errors ->
                    if (isValid) {
                        // Get GST percentage from selected GST type
                        val snapshotGstPercentage = when (selectedGstType) {
                            GstType.ZERO -> 0.0
                            GstType.THREE -> 3.0
                            GstType.FIVE -> 5.0
                            GstType.CUSTOM -> customGstValue.toDoubleOrNull() ?: 0.0
                            null -> 0.0
                        }
                        
                        // Use current snapshot values for order creation
                        paymentViewModel.saveOrderWithPaymentMethod(
                            cart = cart,
                            subtotal = currentSnapshotSubtotal,
                            discountAmount = currentSnapshotDiscountAmount,
                            gst = currentSnapshotGstAmount,
                            gstPercentage = snapshotGstPercentage, // Pass selected GST percentage
                            finalTotal = currentSnapshotFinalTotal,
                            paymentSplit = currentSnapshotPaymentSplit,
                            notes = currentSnapshotNotes,
                            customer = selectedCustomer, // Pass customer as parameter
                            exchangeGold = exchangeGold, // Pass exchange gold information
                            onSuccess = onPaymentComplete
                        )
                    } else {
                        paymentViewModel.setErrorMessage(
                            "Stock validation failed:\n${errors.joinToString("\n")}"
                        )
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Header Section
        PaymentHeader(onBack = onBack)

        // Main Content Split
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Left Side - Discounts & Payment Options (60%)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // Payment Options Section with Integrated Split Payment (Always Visible)
                PaymentOptionsSection(
                    selectedPaymentMethod = selectedPaymentMethod,
                    onPaymentMethodSelected = { paymentViewModel.setPaymentMethod(it) },
                    onExchangeGold = {
                        showExchangeGold = true
                    },
                    hasPaymentSplitWarning = hasPaymentSplitWarning,
                    exchangeGoldValue = exchangeGoldValue,
                    paymentSplit = paymentSplit,
                    totalAmount = totalAmountForSplit,
                    discountAmount = calculatedDiscountAmount,
                    cashAmount = cashAmount,
                    cardAmount = cardAmount,
                    bankAmount = bankAmount,
                    onlineAmount = onlineAmount,
                    dueAmount = dueAmount,
                    isProcessing = isProcessing,
                    onCashAmountChange = { cashAmount = it },
                    onCardAmountChange = { cardAmount = it },
                    onBankAmountChange = { bankAmount = it },
                    onOnlineAmountChange = { onlineAmount = it }
                )

                // Discount Section
                DiscountSection(
                    discountType = discountType,
                    discountValue = discountValue,
                    onDiscountTypeChange = { paymentViewModel.setDiscountType(it) },
                    onDiscountValueChange = { paymentViewModel.setDiscountValue(it) },
                    onApplyDiscount = { paymentViewModel.applyDiscount() },
                    errorMessage = errorMessage,
                    isProcessing = isProcessing
                )

                // Notes Section
                NotesSection(
                    notes = notes,
                    onNotesChange = { notes = it },
                    onAddNotesClick = { showNotesDialog = true }
                )

            }

            // Right Side - Order Summary (40%)
            OrderSummarySection(
                modifier = Modifier.weight(0.4f),
                cartItems = cart.items,
                cartImages = cartImages,
                subtotal = subtotal,
                discountAmount = calculatedDiscountAmount,
                gst = gstAmount,
                total = finalTotal,
                paymentSplit = paymentSplit,
                exchangeGoldValue = exchangeGoldValue,
                cartViewModel = cartViewModel,
                onConfirmOrder = onConfirmOrder,
                isProcessing = isProcessing,
                paymentMethodSelected = selectedPaymentMethod != null || paymentSplit != null,
                selectedGstType = selectedGstType,
                customGstValue = customGstValue,
                onGstTypeChange = { selectedGstType = it },
                onCustomGstValueChange = { customGstValue = it },
                discountType = discountType,
                discountValue = discountValue
            )
        }

        // Exchange Gold Screen
        if (showExchangeGold) {
            ExchangeGoldScreen(
                onExchangeGoldComplete = { exchange ->
                    exchangeGold = exchange
                    showExchangeGold = false
                },
                onBack = { showExchangeGold = false }
            )
        }
        
        // Notes Dialog
        if (showNotesDialog) {
            NotesDialog(
                notes = notes,
                onNotesChange = { notes = it },
                onDismiss = { showNotesDialog = false }
            )
        }
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit,
    onAddNotesClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E2E2E)
                )
                Button(
                    onClick = onAddNotesClick,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFB8973D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (notes.isNotEmpty()) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = "Add Notes",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (notes.isNotEmpty()) "Edit Notes" else "Add Notes",
                        fontSize = 12.sp
                    )
                }
            }
            
            if (notes.isNotEmpty()) {
                Text(
                    notes,
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    "No notes added",
                    fontSize = 13.sp,
                    color = Color(0xFF999999),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun NotesDialog(
    notes: String,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentNotes by remember { mutableStateOf(notes) }
    
    // Sync currentNotes with notes when dialog opens
    LaunchedEffect(notes) {
        currentNotes = notes
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.6f),
            elevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFFFAFAFA)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Add Notes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E2E2E)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF666666)
                        )
                    }
                }
                
                Divider(color = Color(0xFFE0E0E0))
                
                // Notes Input
                OutlinedTextField(
                    value = currentNotes,
                    onValueChange = { currentNotes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text("Notes") },
                    placeholder = { Text("Enter any additional notes for this order...") },
                    maxLines = 10,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF2E2E2E)
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFB8973D),
                        cursorColor = Color(0xFFB8973D)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF666666)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            onNotesChange(currentNotes)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFB8973D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}



@Composable
private fun PaymentHeader(onBack: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(0.dp),
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF2E2E2E),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                "Complete Your Purchase",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E2E2E)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "PREMIUM JEWELRY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB8973D),
                letterSpacing = 1.sp
            )
        }
    }
}


@Composable
private fun DiscountSection(
    discountType: DiscountType,
    discountValue: String,
    onDiscountTypeChange: (DiscountType) -> Unit,
    onDiscountValueChange: (String) -> Unit,
    onApplyDiscount: () -> Unit,
    errorMessage: String?,
    isProcessing: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Apply Discount",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            // Discount Type Selection
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DiscountTypeOption(
                        text = "By Amount (₹)",
                        isSelected = discountType == DiscountType.AMOUNT,
                        onClick = { if (!isProcessing) onDiscountTypeChange(DiscountType.AMOUNT) },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    )

                    DiscountTypeOption(
                        text = "By Percentage (%)",
                        isSelected = discountType == DiscountType.PERCENTAGE,
                        onClick = { if (!isProcessing) onDiscountTypeChange(DiscountType.PERCENTAGE) },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    )
                }

                DiscountTypeOption(
                    text = "Enter Total Payable Amount (₹)",
                    isSelected = discountType == DiscountType.TOTAL_PAYABLE,
                    onClick = { if (!isProcessing) onDiscountTypeChange(DiscountType.TOTAL_PAYABLE) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                )
            }

            // Discount Value Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = discountValue,
                    onValueChange = onDiscountValueChange,
                    enabled = !isProcessing,
                    label = {
                        Text(
                            when (discountType) {
                                DiscountType.AMOUNT -> "Enter discount amount"
                                DiscountType.PERCENTAGE -> "Enter percentage"
                                DiscountType.TOTAL_PAYABLE -> "Enter total payable amount"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF2E2E2E)
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFB8973D),
                        cursorColor = Color(0xFFB8973D),
                        textColor = Color(0xFF2E2E2E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = onApplyDiscount,
                    modifier = Modifier.height(48.dp),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFB8973D),
                        contentColor = Color.White,
                        disabledBackgroundColor = Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Apply",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Error Message
            if (errorMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        errorMessage,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscountTypeOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { if (enabled) onClick() }
            .border(
                width = 1.5.dp,
                color = if (isSelected && enabled) Color(0xFFB8973D) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(10.dp)
            ),
        elevation = if (isSelected && enabled) 2.dp else 0.dp,
        shape = RoundedCornerShape(10.dp),
        backgroundColor = if (isSelected && enabled) Color(0xFFFFF8E1) else if (!enabled) Color(0xFFF5F5F5) else Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.size(18.dp),
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFFB8973D),
                    unselectedColor = Color(0xFFE0E0E0)
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFFB8973D) else Color.Gray
            )
        }
    }
}

@Composable
private fun GstSection(
    selectedGstType: GstType?,
    customGstValue: String,
    onGstTypeChange: (GstType?) -> Unit,
    onCustomGstValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Select GST",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            // GST Type Selection Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GstTypeOption(
                    text = "0%",
                    isSelected = selectedGstType == GstType.ZERO,
                    onClick = { onGstTypeChange(GstType.ZERO) },
                    modifier = Modifier.weight(1f)
                )

                GstTypeOption(
                    text = "3%",
                    isSelected = selectedGstType == GstType.THREE,
                    onClick = { onGstTypeChange(GstType.THREE) },
                    modifier = Modifier.weight(1f)
                )

                GstTypeOption(
                    text = "5%",
                    isSelected = selectedGstType == GstType.FIVE,
                    onClick = { onGstTypeChange(GstType.FIVE) },
                    modifier = Modifier.weight(1f)
                )

                GstTypeOption(
                    text = "Custom",
                    isSelected = selectedGstType == GstType.CUSTOM,
                    onClick = { onGstTypeChange(GstType.CUSTOM) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Custom GST Value Input (only show when Custom is selected)
            if (selectedGstType == GstType.CUSTOM) {
                OutlinedTextField(
                    value = customGstValue,
                    onValueChange = onCustomGstValueChange,
                    label = { Text("Enter GST percentage") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = { 
                        Text(
                            "%", 
                            color = Color(0xFF666666), 
                            modifier = Modifier.padding(end = 12.dp)
                        ) 
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF2E2E2E)
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFB8973D),
                        cursorColor = Color(0xFFB8973D),
                        textColor = Color(0xFF2E2E2E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
private fun GstTypeOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) Color(0xFFB8973D) else Color(0xFFF5F5F5),
            contentColor = if (isSelected) Color.White else Color(0xFF2E2E2E)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = if (isSelected) ButtonDefaults.elevation(defaultElevation = 4.dp) else ButtonDefaults.elevation(defaultElevation = 0.dp)
    ) {
        Text(
            text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun GstTypeOptionSmall(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(24.dp).width(32.dp).padding(0.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected && enabled) Color(0xFFB8973D) else Color(0xFFF5F5F5),
            contentColor = if (isSelected && enabled) Color.White else Color(0xFF2E2E2E),
            disabledBackgroundColor = Color(0xFFE0E0E0),
            disabledContentColor = Color(0xFF9E9E9E)
        ),
        shape = RoundedCornerShape(4.dp),
        elevation = if (isSelected && enabled) ButtonDefaults.elevation(defaultElevation = 2.dp) else ButtonDefaults.elevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(
            text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp
        )
    }
}


@Composable
private fun PaymentOptionsSection(
    selectedPaymentMethod: PaymentMethod?,
    onPaymentMethodSelected: (PaymentMethod) -> Unit,
    onExchangeGold: () -> Unit,
    hasPaymentSplitWarning: Boolean = false,
    exchangeGoldValue: Double = 0.0,
    paymentSplit: PaymentSplit?,
    totalAmount: Double,
    discountAmount: Double,
    cashAmount: String,
    cardAmount: String,
    bankAmount: String,
    onlineAmount: String,
    dueAmount: String,
    onCashAmountChange: (String) -> Unit,
    onCardAmountChange: (String) -> Unit,
    onBankAmountChange: (String) -> Unit,
    onOnlineAmountChange: (String) -> Unit,
    isProcessing: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Payment Method",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            // Payment Split Form (Always Visible)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color(0xFFF5F5F5)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Amount Display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp,
                        shape = RoundedCornerShape(10.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Total Amount to Split",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF666666)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${CurrencyFormatter.formatRupees(totalAmount)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8973D)
                            )
                        }
                    }

                    // Payment Input Fields
                    // Disable all inputs when processing to prevent desync
                    PaymentInputField(
                        label = "Cash Amount",
                        value = cashAmount,
                        onValueChange = onCashAmountChange,
                        icon = Icons.Default.AccountBox,
                        iconColor = Color(0xFF4CAF50),
                        enabled = !isProcessing
                    )

                    PaymentInputField(
                        label = "Card Amount",
                        value = cardAmount,
                        onValueChange = onCardAmountChange,
                        icon = Icons.Default.AccountBox,
                        iconColor = Color(0xFF2196F3),
                        enabled = !isProcessing
                    )

                    PaymentInputField(
                        label = "Bank Transfer Amount",
                        value = bankAmount,
                        onValueChange = onBankAmountChange,
                        icon = Icons.Default.AccountBox,
                        iconColor = Color(0xFF9C27B0),
                        enabled = !isProcessing
                    )

                    PaymentInputField(
                        label = "Online Amount",
                        value = onlineAmount,
                        onValueChange = onOnlineAmountChange,
                        icon = Icons.Default.AccountBox,
                        iconColor = Color(0xFF00BCD4),
                        enabled = !isProcessing
                    )

                    PaymentInputField(
                        label = "Due Amount ",
                        value = dueAmount,
                        onValueChange = {},
                        icon = Icons.Default.AccountBox,
                        iconColor = Color(0xFFFF9800),
                        enabled = false
                    )

                    // Payment Summary
                    if (paymentSplit != null) {
                        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                        PaymentSplitSummaryCompact(
                            paymentSplit = paymentSplit,
                            totalAmount = totalAmount
                        )
                    }
                }
            }

            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            // Exchange Gold Option
            // Disable when processing
            Button(
                onClick = onExchangeGold,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (exchangeGoldValue > 0) Color(0xFF4CAF50) else Color(0xFFB8973D),
                    contentColor = Color.White,
                    disabledBackgroundColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color(0xFF9E9E9E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Exchange Gold",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (exchangeGoldValue > 0) "Exchange Gold Added (${CurrencyFormatter.formatRupees(exchangeGoldValue)})" else "Exchange Gold (Old Gold Exchange)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PaymentInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Format value when not focused, show raw when focused
    val displayValue = remember(value, isFocused) {
        if (isFocused || value.isEmpty()) {
            // When focused or empty, show raw value for editing
            value
        } else {
            // When not focused, format the value
            val numericValue = value.toDoubleOrNull() ?: 0.0
            if (numericValue > 0) {
                CurrencyFormatter.formatRupeesNumber(numericValue, includeDecimals = true)
            } else {
                value
            }
        }
    }
    
    OutlinedTextField(
        value = displayValue,
        onValueChange = { input: String ->
            // Remove formatting (commas) for editing
            val rawInput = input.replace(",", "").replace("₹", "").trim()
            if (rawInput.isEmpty() || rawInput.matches(Regex("^\\d*\\.?\\d*\$"))) {
                onValueChange(rawInput)
            }
        },
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            },
        singleLine = true,
        enabled = enabled,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            color = Color(0xFF2E2E2E)
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = if (enabled) Color(0xFFB8973D) else Color(0xFFE0E0E0),
            cursorColor = if (enabled) Color(0xFFB8973D) else Color(0xFFE0E0E0),
            disabledTextColor = Color(0xFF666666),
            disabledBorderColor = Color(0xFFE0E0E0),
            disabledLabelColor = Color(0xFF666666)
        ),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
private fun PaymentSplitSummaryCompact(
    paymentSplit: PaymentSplit,
    totalAmount: Double
) {
    // Use PaymentSplit validation methods
    val exceedsTotal = paymentSplit.exceedsTotal(totalAmount)
    val isValid = paymentSplit.isValid(totalAmount)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Payment Summary",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (exceedsTotal) Color(0xFFD32F2F) else Color(0xFF2E2E2E)
        )

        if (paymentSplit.cash > 0) {
            PaymentSplitRowCompact("Cash", paymentSplit.cash, Color(0xFF4CAF50))
        }
        if (paymentSplit.bank > 0) {
            PaymentSplitRowCompact("Bank/Card/Online", paymentSplit.bank, Color(0xFF2196F3))
        }
        if (paymentSplit.dueAmount > 0) {
            PaymentSplitRowCompact("Due", paymentSplit.dueAmount, Color(0xFFFF9800))
        }

        Divider(color = if (exceedsTotal) Color(0xFFD32F2F) else Color(0xFFE0E0E0), thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Total",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (exceedsTotal) Color(0xFFD32F2F) else Color(0xFF2E2E2E)
            )
            // Always show the actual total amount, not the breakdown total
            Text(
                "${CurrencyFormatter.formatRupees(totalAmount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (exceedsTotal) Color(0xFFD32F2F) else if (isValid) Color(0xFF4CAF50) else Color(0xFFB8973D)
            )
        }

        // Validation Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isValid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    RoundedCornerShape(8.dp)
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (isValid) "Valid" else "Invalid",
                tint = if (isValid) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isValid) "Payment amounts match total" else "Amounts don't match total",
                fontSize = 12.sp,
                color = if (isValid) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PaymentSplitRowCompact(
    label: String,
    amount: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666)
        )
        Text(
            "${CurrencyFormatter.formatRupees(amount)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}


@Composable
private fun OrderSummarySection(
    modifier: Modifier = Modifier,
    cartItems: List<CartItem>,
    cartImages: Map<String, ImageBitmap>,
    subtotal: Double,
    discountAmount: Double,
    gst: Double,
    total: Double,
    paymentSplit: PaymentSplit?,
    exchangeGoldValue: Double,
    cartViewModel: CartViewModel,
    onConfirmOrder: () -> Unit,
    isProcessing: Boolean,
    paymentMethodSelected: Boolean,
    selectedGstType: GstType? = null,
    customGstValue: String = "",
    onGstTypeChange: ((GstType?) -> Unit)? = null,
    onCustomGstValueChange: ((String) -> Unit)? = null,
    discountType: DiscountType? = null,
    discountValue: String = ""
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Order Summary",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cart Items List
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                cartItems.forEach { cartItem ->
                    OrderSummaryItem(
                        cartItem = cartItem,
                        image = cartImages[cartItem.productId],
                        cartViewModel = cartViewModel
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price Breakdown
            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            PriceBreakdown(
                subtotal = subtotal,
                exchangeGoldValue = exchangeGoldValue,
                discountAmount = discountAmount,
                gst = gst,
                total = total,
                selectedGstType = selectedGstType,
                customGstValue = customGstValue,
                onGstTypeChange = onGstTypeChange,
                onCustomGstValueChange = onCustomGstValueChange,
                isProcessing = isProcessing,
                discountType = discountType,
                discountValue = discountValue
            )

            // Payment Split Information
            if (paymentSplit != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                PaymentSplitSummary(
                    paymentSplit = paymentSplit,
                    totalAmount = total
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Exchange Gold Information
            if (exchangeGoldValue > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                ExchangeGoldSummary(
                    exchangeGoldValue = exchangeGoldValue
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Order Button
            // Validate payment split against total amount
            val isPaymentSplitValid = paymentSplit?.let { split ->
                split.isValid(total) // Use isValid(totalAmount) method
            } ?: true // If no payment split, consider valid (payment method selected)
            
            Button(
                onClick = onConfirmOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                enabled = (paymentMethodSelected || paymentSplit != null) && !isProcessing && isPaymentSplitValid,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFB8973D),
                    contentColor = Color.White,
                    disabledBackgroundColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving Order...", fontSize = 14.sp)
                } else {
                    Text(
                        "Confirm Order - ${CurrencyFormatter.formatRupees(total)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryItem(
    cartItem: CartItem,
    image: ImageBitmap?,
    cartViewModel: CartViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product Image
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF5F5F5)),
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
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Product",
                    tint = Color(0xFFB8973D),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Product Details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                cartItem.product.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2E2E2E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "Qty: ${cartItem.quantity}",
                fontSize = 10.sp,
                color = Color(0xFF666666)
            )
        }

        // Price - using same ProductPriceCalculator logic as subtotal
        val metalRates by MetalRatesManager.metalRates
        val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
        val itemPrice = remember(cartItem, metalRates) {
            val currentProduct = cartItem.product
            
            // Use ProductPriceCalculator logic (same as subtotal calculation)
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
                ratesVM.calculateRateForMaterial(currentProduct.materialId, currentProduct.materialType, metalKarat)
            } catch (e: Exception) { 0.0 }
            val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
            val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else defaultGoldRate
            // Extract silver purity from material type
            val materialTypeLower = currentProduct.materialType.lowercase()
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
        Text(
            "${CurrencyFormatter.formatRupees(itemPrice)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB8973D)
        )

    }
}

@Composable
private fun PriceBreakdown(
    subtotal: Double,
    exchangeGoldValue: Double = 0.0,
    discountAmount: Double,
    gst: Double,
    total: Double,
    selectedGstType: GstType? = null,
    customGstValue: String = "",
    onGstTypeChange: ((GstType?) -> Unit)? = null,
    onCustomGstValueChange: ((String) -> Unit)? = null,
    isProcessing: Boolean = false,
    discountType: DiscountType? = null,
    discountValue: String = ""
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Subtotal (base amount before exchange gold reduction)
        PriceRow(
            label = "Subtotal",
            amount = subtotal,
            isTotal = false
        )

        // Exchange Gold (shown after subtotal, before discount)
        if (exchangeGoldValue > 0) {
            PriceRow(
                label = "Exchange Gold",
                amount = -exchangeGoldValue,
                isDiscount = true,
                isTotal = false
            )
        }

        // Discount (calculated on amount after exchange gold reduction)
        // Show discount if:
        // 1. Discount amount is greater than 0.01 (to handle rounding)
        // 2. OR if discount type is selected and value is entered (even if calculated amount is 0)
        val shouldShowDiscount = discountAmount > 0.01 || 
            (discountType != null && discountValue.isNotEmpty() && discountValue.toDoubleOrNull() != null)
        
        if (shouldShowDiscount) {
            PriceRow(
                label = "Discount",
                amount = -discountAmount,
                isDiscount = true,
                isTotal = false
            )
        }

        // GST Amount with selection buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "GST",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
                
                // Small GST selection buttons
                // Disable all GST inputs when processing
                GstTypeOptionSmall(
                    text = "0%",
                    isSelected = selectedGstType == GstType.ZERO,
                    onClick = { if (!isProcessing) onGstTypeChange?.invoke(GstType.ZERO) },
                    enabled = !isProcessing
                )
                GstTypeOptionSmall(
                    text = "3%",
                    isSelected = selectedGstType == GstType.THREE,
                    onClick = { if (!isProcessing) onGstTypeChange?.invoke(GstType.THREE) },
                    enabled = !isProcessing
                )
                GstTypeOptionSmall(
                    text = "5%",
                    isSelected = selectedGstType == GstType.FIVE,
                    onClick = { if (!isProcessing) onGstTypeChange?.invoke(GstType.FIVE) },
                    enabled = !isProcessing
                )
                GstTypeOptionSmall(
                    text = "C",
                    isSelected = selectedGstType == GstType.CUSTOM,
                    onClick = { if (!isProcessing) onGstTypeChange?.invoke(GstType.CUSTOM) },
                    enabled = !isProcessing
                )
            }
            
            Text(
                "${CurrencyFormatter.formatRupees(gst)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2E2E2E)
            )
        }
        
        // Custom GST input (only show when Custom is selected)
        if (selectedGstType == GstType.CUSTOM) {
            Spacer(modifier = Modifier.height(4.dp))
            val focusManager = LocalFocusManager.current
            OutlinedTextField(
                value = customGstValue,
                onValueChange = { onCustomGstValueChange?.invoke(it) },
                enabled = !isProcessing,
                label = { Text("Enter GST %") },
                placeholder = { Text("0.00", color = Color.Gray.copy(alpha = 0.5f)) },
                trailingIcon = { 
                    Text(
                        "%", 
                        color = Color.Gray, 
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    ) 
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // Just clear focus, don't move to next field or trigger buttons
                        focusManager.clearFocus()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                            // Just clear focus, don't move to next field or trigger buttons
                            focusManager.clearFocus()
                            true
                        } else false
                    },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF2E2E2E)
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFB8973D),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    cursorColor = Color(0xFFB8973D),
                    focusedLabelColor = Color(0xFFB8973D),
                    textColor = Color(0xFF2E2E2E)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

        PriceRow(
            label = "Total",
            amount = total,
            isTotal = true
        )
    }
}

@Composable
private fun PriceRow(
    label: String,
    amount: Double,
    isTotal: Boolean = false,
    isDiscount: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = if (isTotal) 16.sp else 14.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium,
            color = if (isTotal) Color(0xFF2E2E2E) else Color(0xFF666666)
        )

        Text(
            "${if (isDiscount) "-" else ""}${CurrencyFormatter.formatRupees(kotlin.math.abs(amount))}",
            fontSize = if (isTotal) 16.sp else 14.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isTotal -> Color(0xFFB8973D)
                isDiscount -> Color(0xFF4CAF50)
                else -> Color(0xFF2E2E2E)
            }
        )
    }
}

@Composable
private fun PaymentSplitSummary(
    paymentSplit: PaymentSplit,
    totalAmount: Double
) {
    // Use PaymentSplit validation methods
    val exceedsTotal = paymentSplit.exceedsTotal(totalAmount)
    val isValid = paymentSplit.isValid(totalAmount)
    val isDueAmountNegative = paymentSplit.dueAmount < 0
    val dueAmount = paymentSplit.dueAmount
    val totalPaymentBreakdown = paymentSplit.getTotalBreakdown()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Payment Breakdown",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (exceedsTotal || isDueAmountNegative) Color(0xFFD32F2F) else Color(0xFF2E2E2E)
        )

        if (paymentSplit.cash > 0) {
            PaymentSplitRow("Cash", paymentSplit.cash, Color(0xFF4CAF50))
        }
        if (paymentSplit.bank > 0) {
            PaymentSplitRow("Bank/Card/Online", paymentSplit.bank, Color(0xFF2196F3))
        }

        if (dueAmount > 0) {
            PaymentSplitRow("Due", dueAmount, Color(0xFFFF9800))
        } else if (dueAmount < 0) {
            PaymentSplitRow("Due (Overpaid)", dueAmount, Color(0xFFD32F2F))
        }

        Divider(color = if (exceedsTotal || isDueAmountNegative) Color(0xFFD32F2F) else Color(0xFFE0E0E0), thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Total Payment",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (exceedsTotal || isDueAmountNegative) Color(0xFFD32F2F) else Color(0xFF2E2E2E)
            )
            // Always show the actual total amount, not the breakdown total
            Text(
                "${CurrencyFormatter.formatRupees(totalAmount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (exceedsTotal || isDueAmountNegative) Color(0xFFD32F2F) else Color(0xFFB8973D)
            )
        }

        if (exceedsTotal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Payment breakdown exceeds total amount by ₹${CurrencyFormatter.formatRupees(totalPaymentBreakdown - totalAmount)}. Please adjust payment amounts.",
                    fontSize = 12.sp,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Medium
                )
            }
        } else if (isDueAmountNegative) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Due amount is negative! Please adjust payment split amounts.",
                    fontSize = 12.sp,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PaymentSplitRow(
    label: String,
    amount: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666)
        )
        Text(
            "${CurrencyFormatter.formatRupees(amount)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun ExchangeGoldSummary(
    exchangeGoldValue: Double
) {
    Column {
        Text(
            "Exchange Gold",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E2E2E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Old Gold Exchange Value:",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )

            Text(
                "-${CurrencyFormatter.formatRupees(exchangeGoldValue)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }
    }
}
