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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.data.CartItem
import org.example.project.data.MetalRatesManager
import org.example.project.JewelryAppInitializer
import org.example.project.viewModels.PaymentViewModel
import org.example.project.viewModels.CartViewModel
import org.example.project.viewModels.CustomerViewModel

import org.example.project.data.PaymentMethod
import org.example.project.data.DiscountType
import org.example.project.data.PaymentSplit
import org.example.project.data.ExchangeGold
import org.example.project.viewModels.ProductsViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PaymentScreen(
    paymentViewModel: PaymentViewModel,
    cartViewModel: CartViewModel,
    customerViewModel: CustomerViewModel,
    onBack: () -> Unit,
    onPaymentComplete: () -> Unit,
    productsViewModel: ProductsViewModel
) {
    var showExchangeGold by remember { mutableStateOf(false) }
    var exchangeGold by remember { mutableStateOf<ExchangeGold?>(null) }
    val cart by cartViewModel.cart
    val cartImages by cartViewModel.cartImages
    val selectedPaymentMethod by paymentViewModel.selectedPaymentMethod
    val discountType by paymentViewModel.discountType
    val discountValue by paymentViewModel.discountValue
    val exchangeGoldValue = exchangeGold?.value ?: 0.0
    val isProcessing by paymentViewModel.isProcessing
    val errorMessage by paymentViewModel.errorMessage
    val selectedCustomer by customerViewModel.selectedCustomer

    // Payment split state - always visible by default
    var cashAmount by remember { mutableStateOf("") }
    var cardAmount by remember { mutableStateOf("") }
    var bankAmount by remember { mutableStateOf("") }
    var onlineAmount by remember { mutableStateOf("") }
    var dueAmount by remember { mutableStateOf("") }
    var paymentSplit by remember { mutableStateOf<PaymentSplit?>(null) }
    
    // Calculate totals using the EXACT same logic as cart screen
    val metalRates by MetalRatesManager.metalRates
    val subtotal = remember(cart, metalRates) {
        val total = cart.items.sumOf { cartItem ->
            val currentProduct = cartItem.product
            val metalKarat = if (cartItem.metal.isNotEmpty()) {
                cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(currentProduct.materialType)
            } else {
                extractKaratFromMaterialType(currentProduct.materialType)
            }
            
            val grossWeight = currentProduct.totalWeight // grossWeight removed, using totalWeight
            val lessWeight = if (cartItem.lessWeight > 0) cartItem.lessWeight else 0.0 // lessWeight removed from Product
            val netWeight = grossWeight - lessWeight
            
            val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
            val collectionRate = try {
                ratesVM.calculateRateForMaterial(currentProduct.materialId, currentProduct.materialType, metalKarat)
            } catch (e: Exception) { 0.0 }
            
            val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
            val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else defaultGoldRate
            val silverRate = metalRates.getSilverRateForPurity(999) // Default to 999 purity
            
            val metalRate = if (collectionRate > 0) collectionRate else when {
                currentProduct.materialType.contains("gold", ignoreCase = true) -> goldRate
                currentProduct.materialType.contains("silver", ignoreCase = true) -> silverRate
                else -> goldRate
            }
            
            val quantity = cartItem.quantity
            val makingChargesPerGram = if (cartItem.makingCharges > 0) cartItem.makingCharges else 0.0 // defaultMakingRate removed from Product
            val firstStone = currentProduct.stones.firstOrNull()
            val cwWeight = if (cartItem.cwWeight > 0) cartItem.cwWeight else (firstStone?.weight ?: currentProduct.stoneWeight)
            val stoneRate = if (cartItem.stoneRate > 0) cartItem.stoneRate else (firstStone?.rate ?: 0.0)
            val stoneQuantity = if (cartItem.stoneQuantity > 0) cartItem.stoneQuantity else (firstStone?.quantity ?: 0.0)
            val vaCharges = if (cartItem.va > 0) cartItem.va else currentProduct.labourCharges // Use labourCharges instead of vaCharges
            
            val baseAmount = netWeight * metalRate * quantity
            val makingCharges = netWeight * makingChargesPerGram * quantity
            val stoneAmount = stoneRate * stoneQuantity * cwWeight * quantity
            val totalCharges = baseAmount + makingCharges + stoneAmount + (vaCharges * quantity)
            
            println("💰 PAYMENT SCREEN DEBUG: ${cartItem.product.name} = $totalCharges")
            totalCharges
        }
        println("💰 PAYMENT SCREEN TOTAL CALCULATION: Total = $total")
        total
    }
    
    // Calculate GST amount using split model (3% on metal, 5% on making charges)
    val gstAmount = remember(cart, metalRates) {
        val total = cart.items.sumOf { cartItem ->
            val currentProduct = cartItem.product
            val metalKarat = if (cartItem.metal.isNotEmpty()) {
                cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(currentProduct.materialType)
            } else {
                extractKaratFromMaterialType(currentProduct.materialType)
            }

            val grossWeight = currentProduct.totalWeight // grossWeight removed, using totalWeight
            val lessWeight = if (cartItem.lessWeight > 0) cartItem.lessWeight else 0.0 // lessWeight removed from Product
            val netWeight = grossWeight - lessWeight

            val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
            val collectionRate = try {
                ratesVM.calculateRateForMaterial(currentProduct.materialId, currentProduct.materialType, metalKarat)
            } catch (e: Exception) { 0.0 }

            val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
            val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else defaultGoldRate
            val silverRate = metalRates.getSilverRateForPurity(999) // Default to 999 purity

            val metalRate = if (collectionRate > 0) collectionRate else when {
                currentProduct.materialType.contains("gold", ignoreCase = true) -> goldRate
                currentProduct.materialType.contains("silver", ignoreCase = true) -> silverRate
                else -> goldRate
            }

            val quantity = cartItem.quantity
            val makingChargesPerGram = if (cartItem.makingCharges > 0) cartItem.makingCharges else 0.0 // defaultMakingRate removed from Product

            // Calculate base amount (metal cost)
            val baseAmount = netWeight * metalRate * quantity
            // Calculate making charges
            val makingCharges = netWeight * makingChargesPerGram * quantity

            // Apply discount if any (assuming discount is applied proportionally)
            val discountPercent = cartItem.discountPercent
            val totalCharges = baseAmount + makingCharges
            val discountAmount = totalCharges * (discountPercent / 100.0)
            val taxableAmount = totalCharges - discountAmount

            val discountFactor = if (totalCharges > 0) (taxableAmount / totalCharges) else 1.0
            val discountedBase = baseAmount * discountFactor
            val discountedMaking = makingCharges * discountFactor

            // Calculate GST: 3% on metal, 5% on making charges
            val gstOnBase = discountedBase * 0.03
            val gstOnMaking = discountedMaking * 0.05
            val itemGST = gstOnBase + gstOnMaking

            println("💰 GST CALCULATION DEBUG: ${cartItem.product.name}")
            println("   - Base Amount: $baseAmount, Making Charges: $makingCharges")
            println("   - Discount Factor: $discountFactor")
            println("   - Discounted Base: $discountedBase, Discounted Making: $discountedMaking")
            println("   - GST on Base (3%): $gstOnBase, GST on Making (5%): $gstOnMaking")
            println("   - Total GST: $itemGST")
            itemGST
        }
        println("💰 TOTAL GST CALCULATION: $total")
        total
    }
    
    // Calculate discount amount using the new method that includes GST
    val calculatedDiscountAmount = if (discountValue.isNotEmpty()) {
        paymentViewModel.calculateDiscountAmount(subtotal, gstAmount)
    } else {
        0.0
    }
    
    // Calculate total amount for payment split
    val totalAmountForSplit = subtotal + gstAmount - calculatedDiscountAmount - exchangeGoldValue

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
        if (cash > 0 || card > 0 || bank > 0 || online > 0 || calculatedDueAmount > 0) {
            paymentSplit = PaymentSplit(
                cashAmount = cash,
                cardAmount = card,
                bankAmount = bank,
                onlineAmount = online,
                dueAmount = calculatedDueAmount,
                totalAmount = totalAmountForSplit
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
    val onConfirmOrder = {
        paymentViewModel.validateStockBeforeOrder(
            cart = cart,
            products = productsViewModel.products.value,
            onValidationResult = { isValid, errors ->
                if (isValid) {
                    paymentViewModel.saveOrderWithPaymentMethod(
                        cart = cart,
                        subtotal = subtotal,
                        discountAmount = calculatedDiscountAmount,
                        gst = gstAmount,
                        finalTotal = subtotal + gstAmount - calculatedDiscountAmount - exchangeGoldValue,
                        paymentSplit = paymentSplit,
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
                    errorMessage = errorMessage
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
                total = subtotal + gstAmount - calculatedDiscountAmount - exchangeGoldValue,
                paymentSplit = paymentSplit,
                exchangeGoldValue = exchangeGoldValue,
                cartViewModel = cartViewModel,
                onConfirmOrder = onConfirmOrder,
                isProcessing = isProcessing,
                paymentMethodSelected = selectedPaymentMethod != null || paymentSplit != null
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
    errorMessage: String?
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
                        onClick = { onDiscountTypeChange(DiscountType.AMOUNT) },
                        modifier = Modifier.weight(1f)
                    )

                    DiscountTypeOption(
                        text = "By Percentage (%)",
                        isSelected = discountType == DiscountType.PERCENTAGE,
                        onClick = { onDiscountTypeChange(DiscountType.PERCENTAGE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                DiscountTypeOption(
                    text = "Enter Total Payable Amount (₹)",
                    isSelected = discountType == DiscountType.TOTAL_PAYABLE,
                    onClick = { onDiscountTypeChange(DiscountType.TOTAL_PAYABLE) },
                    modifier = Modifier.fillMaxWidth()
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
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFB8973D),
                        contentColor = Color.White
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.5.dp,
                color = if (isSelected) Color(0xFFB8973D) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(10.dp)
            ),
        elevation = if (isSelected) 2.dp else 0.dp,
        shape = RoundedCornerShape(10.dp),
        backgroundColor = if (isSelected) Color(0xFFFFF8E1) else Color.White
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
    onOnlineAmountChange: (String) -> Unit
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
                                "₹${formatCurrency(totalAmount)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8973D)
                            )
                        }
                    }

                    // Payment Input Fields
                    PaymentInputField(
                        label = "Cash Amount",
                        value = cashAmount,
                        onValueChange = onCashAmountChange,
                        icon = Icons.Default.AccountBox,
                        iconColor = Color(0xFF4CAF50)
                    )

                    PaymentInputField(
                        label = "Card Amount",
                        value = cardAmount,
                        onValueChange = onCardAmountChange,
                        icon = Icons.Default.AccountBox,
                        iconColor = Color(0xFF2196F3)
                    )

                    PaymentInputField(
                        label = "Bank Transfer Amount",
                        value = bankAmount,
                        onValueChange = onBankAmountChange,
                        icon = Icons.Default.AccountBox,
                        iconColor = Color(0xFF9C27B0)
                    )

                    PaymentInputField(
                        label = "Online Amount",
                        value = onlineAmount,
                        onValueChange = onOnlineAmountChange,
                        icon = Icons.Default.AccountBox,
                        iconColor = Color(0xFF00BCD4)
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
            Button(
                onClick = onExchangeGold,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (exchangeGoldValue > 0) Color(0xFF4CAF50) else Color(0xFFB8973D),
                    contentColor = Color.White
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
                    if (exchangeGoldValue > 0) "Exchange Gold Added (₹${formatCurrency(exchangeGoldValue)})" else "Exchange Gold (Old Gold Exchange)",
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
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                onValueChange(input)
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().height(56.dp),
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
    val isValid = paymentSplit.isValid()
    val totalPaid = paymentSplit.cashAmount + paymentSplit.cardAmount +
            paymentSplit.bankAmount + paymentSplit.onlineAmount + paymentSplit.dueAmount

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Payment Summary",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E2E2E)
        )

        if (paymentSplit.cashAmount > 0) {
            PaymentSplitRowCompact("Cash", paymentSplit.cashAmount, Color(0xFF4CAF50))
        }
        if (paymentSplit.cardAmount > 0) {
            PaymentSplitRowCompact("Card", paymentSplit.cardAmount, Color(0xFF2196F3))
        }
        if (paymentSplit.bankAmount > 0) {
            PaymentSplitRowCompact("Bank", paymentSplit.bankAmount, Color(0xFF9C27B0))
        }
        if (paymentSplit.onlineAmount > 0) {
            PaymentSplitRowCompact("Online", paymentSplit.onlineAmount, Color(0xFF00BCD4))
        }
        if (paymentSplit.dueAmount > 0) {
            PaymentSplitRowCompact("Due", paymentSplit.dueAmount, Color(0xFFFF9800))
        }

        Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Total",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )
            Text(
                "₹${formatCurrency(totalPaid)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isValid) Color(0xFF4CAF50) else Color(0xFFD32F2F)
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
            "₹${formatCurrency(amount)}",
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
    paymentMethodSelected: Boolean
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
                discountAmount = discountAmount,
                gst = gst,
                total = total
            )

            // Payment Split Information
            if (paymentSplit != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                PaymentSplitSummary(
                    paymentSplit = paymentSplit
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
            Button(
                onClick = onConfirmOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                enabled = (paymentMethodSelected || paymentSplit != null) && !isProcessing && (paymentSplit?.isValid() != false),
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
                        "Confirm Order - ₹${formatCurrency(total)}",
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

        // Price (base amount without GST to match subtotal) - using same calculation as main subtotal
        val metalRates by MetalRatesManager.metalRates
        val baseAmount = remember(cartItem, metalRates) {
            val currentProduct = cartItem.product
            val metalKarat = if (cartItem.metal.isNotEmpty()) {
                cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(currentProduct.materialType)
            } else {
                extractKaratFromMaterialType(currentProduct.materialType)
            }

            val grossWeight = currentProduct.totalWeight // grossWeight removed, using totalWeight
            val lessWeight = if (cartItem.lessWeight > 0) cartItem.lessWeight else 0.0 // lessWeight removed from Product
            val netWeight = grossWeight - lessWeight

            val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
            val collectionRate = try {
                ratesVM.calculateRateForMaterial(currentProduct.materialId, currentProduct.materialType, metalKarat)
            } catch (e: Exception) { 0.0 }

            val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
            val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else defaultGoldRate
            val silverRate = metalRates.getSilverRateForPurity(999) // Default to 999 purity

            val metalRate = if (collectionRate > 0) collectionRate else when {
                currentProduct.materialType.contains("gold", ignoreCase = true) -> goldRate
                currentProduct.materialType.contains("silver", ignoreCase = true) -> silverRate
                else -> goldRate
            }

            val quantity = cartItem.quantity
            val makingChargesPerGram = if (cartItem.makingCharges > 0) cartItem.makingCharges else 0.0 // defaultMakingRate removed from Product
            val firstStone = currentProduct.stones.firstOrNull()
            val cwWeight = if (cartItem.cwWeight > 0) cartItem.cwWeight else (firstStone?.weight ?: currentProduct.stoneWeight)
            val stoneRate = if (cartItem.stoneRate > 0) cartItem.stoneRate else (firstStone?.rate ?: 0.0)
            val stoneQuantity = if (cartItem.stoneQuantity > 0) cartItem.stoneQuantity else (firstStone?.quantity ?: 0.0)
            val vaCharges = if (cartItem.va > 0) cartItem.va else currentProduct.labourCharges // Use labourCharges instead of vaCharges

            val baseAmount = netWeight * metalRate * quantity
            val makingCharges = netWeight * makingChargesPerGram * quantity
            val stoneAmount = stoneRate * stoneQuantity * cwWeight * quantity
            val totalCharges = baseAmount + makingCharges + stoneAmount + vaCharges

            println("💰 ORDER SUMMARY ITEM DEBUG: ${cartItem.product.name} = $totalCharges")
            totalCharges
        }
        Text(
            "₹${formatCurrency(baseAmount)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB8973D)
        )

    }
}

@Composable
private fun PriceBreakdown(
    subtotal: Double,
    discountAmount: Double,
    gst: Double,
    total: Double
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PriceRow(
            label = "Subtotal",
            amount = subtotal,
            isTotal = false
        )

        if (discountAmount > 0) {
            PriceRow(
                label = "Discount",
                amount = -discountAmount,
                isDiscount = true,
                isTotal = false
            )
        }

        // GST Amount (calculated from split GST: 3% base + 5% making)
        PriceRow(
            label = "GST",
            amount = gst
        )

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
            "${if (isDiscount) "-" else ""}₹${formatCurrency(kotlin.math.abs(amount))}",
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
    paymentSplit: PaymentSplit
) {
    val dueAmount = paymentSplit.dueAmount
    val isDueAmountNegative = dueAmount < 0
    val totalPayment = paymentSplit.cashAmount + paymentSplit.cardAmount + paymentSplit.bankAmount + paymentSplit.onlineAmount + dueAmount

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Payment Breakdown",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDueAmountNegative) Color(0xFFD32F2F) else Color(0xFF2E2E2E)
        )

        if (paymentSplit.cashAmount > 0) {
            PaymentSplitRow("Cash", paymentSplit.cashAmount, Color(0xFF4CAF50))
        }
        if (paymentSplit.cardAmount > 0) {
            PaymentSplitRow("Card", paymentSplit.cardAmount, Color(0xFF2196F3))
        }
        if (paymentSplit.bankAmount > 0) {
            PaymentSplitRow("Bank Transfer", paymentSplit.bankAmount, Color(0xFF9C27B0))
        }
        if (paymentSplit.onlineAmount > 0) {
            PaymentSplitRow("Online", paymentSplit.onlineAmount, Color(0xFF00BCD4))
        }


        if (dueAmount > 0) {
            PaymentSplitRow("Due", dueAmount, Color(0xFFFF9800))
        } else if (dueAmount < 0) {
            PaymentSplitRow("Due (Overpaid)", dueAmount, Color(0xFFD32F2F))
        }

        Divider(color = if (isDueAmountNegative) Color(0xFFD32F2F) else Color(0xFFE0E0E0), thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Total Payment",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDueAmountNegative) Color(0xFFD32F2F) else Color(0xFF2E2E2E)
            )
            Text(
                "₹${formatCurrency(totalPayment)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDueAmountNegative) Color(0xFFD32F2F) else Color(0xFFB8973D)
            )
        }

        if (isDueAmountNegative) {
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
            "₹${formatCurrency(amount)}",
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
                "-₹${formatCurrency(exchangeGoldValue)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}
