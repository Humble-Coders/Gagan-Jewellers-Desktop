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
    var showPaymentSplit by remember { mutableStateOf(false) }
    var paymentSplit by remember { mutableStateOf<PaymentSplit?>(null) }
    var showExchangeGold by remember { mutableStateOf(false) }
    var exchangeGold by remember { mutableStateOf<ExchangeGold?>(null) }
    val cart by cartViewModel.cart
    val cartImages by cartViewModel.cartImages
    val selectedPaymentMethod by paymentViewModel.selectedPaymentMethod
    val discountType by paymentViewModel.discountType
    val discountValue by paymentViewModel.discountValue
    val calculatedDiscountAmount = paymentViewModel.calculateDiscountAmount(cartViewModel.getSubtotal())
    val exchangeGoldValue = exchangeGold?.value ?: 0.0
    val isProcessing by paymentViewModel.isProcessing
    val errorMessage by paymentViewModel.errorMessage
    val selectedCustomer by customerViewModel.selectedCustomer
    
    // Calculate if there's a payment split warning (due amount becomes negative after discount)
    val hasPaymentSplitWarning = paymentSplit?.let { split ->
        val adjustedDueAmount = split.dueAmount - calculatedDiscountAmount
        adjustedDueAmount < 0
    } ?: false

    // Define confirm order function that can be reused
    val onConfirmOrder = {
        // Validate stock before proceeding
        paymentViewModel.validateStockBeforeOrder(
            cart = cart,
            products = productsViewModel.products.value,
            onValidationResult = { isValid, errors ->
                if (isValid) {
                    paymentViewModel.saveOrderWithPaymentMethod(
                        cart = cart,
                        subtotal = cartViewModel.getSubtotal(),
                        discountAmount = calculatedDiscountAmount,
                        gst = cartViewModel.getGST(),
                        finalTotal = cartViewModel.getFinalTotal() - calculatedDiscountAmount - exchangeGoldValue,
                        paymentSplit = paymentSplit,
                        onSuccess = onPaymentComplete
                    )
                } else {
                    // Show error dialog or snackbar with stock issues
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

                // Payment Options Section
                PaymentOptionsSection(
                    selectedPaymentMethod = selectedPaymentMethod,
                    onPaymentMethodSelected = { paymentViewModel.setPaymentMethod(it) },
                    onPaymentSplit = { 
                        showPaymentSplit = true 
                    },
                    onExchangeGold = {
                        showExchangeGold = true
                    },
                    hasPaymentSplitWarning = hasPaymentSplitWarning,
                    exchangeGoldValue = exchangeGoldValue
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
                subtotal = cartViewModel.getSubtotal(),
                discountAmount = calculatedDiscountAmount,
                gst = cartViewModel.getGST(),
                total = cartViewModel.getFinalTotal() - calculatedDiscountAmount - exchangeGoldValue,
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

        // Payment Split Screen
        if (showPaymentSplit) {
            Dialog(onDismissRequest = { showPaymentSplit = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.8f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp
                ) {
                    PaymentSplitScreen(
                        totalAmount = cartViewModel.getFinalTotal() - calculatedDiscountAmount - exchangeGoldValue,
                        initialPaymentSplit = paymentSplit,
                        onPaymentSplitComplete = { split ->
                            paymentSplit = split
                            showPaymentSplit = false
                            // Automatically proceed to payment completion when split payment is confirmed
                            onConfirmOrder()
                        },
                        onBack = { showPaymentSplit = false }
                    )
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
            .height(56.dp), // reduced from 80.dp
        elevation = 2.dp, // slightly reduced elevation
        shape = RoundedCornerShape(0.dp),
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), // reduced horizontal padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp) // reduced size
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
                fontSize = 18.sp, // reduced from 24.sp
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E2E2E)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "PREMIUM JEWELRY",
                fontSize = 12.sp, // reduced from 14.sp
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
    onPaymentSplit: () -> Unit,
    onExchangeGold: () -> Unit,
    hasPaymentSplitWarning: Boolean = false,
    exchangeGoldValue: Double = 0.0
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
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Payment Method",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    PaymentMethod.CASH to "Cash",
                    PaymentMethod.CARD to "Credit/Debit Card",
                    PaymentMethod.UPI to "UPI",
                    PaymentMethod.NET_BANKING to "Net Banking",
                    PaymentMethod.BANK_TRANSFER to "Bank Transfer",
                    PaymentMethod.DUE to "Due Payment"
                ).forEach { (method, label) ->
                    PaymentMethodCard(
                        paymentMethod = method,
                        icon = when (method) {
                            PaymentMethod.CASH -> Icons.Default.AccountBox
                            PaymentMethod.CARD -> Icons.Default.AccountBox
                            PaymentMethod.UPI -> Icons.Default.AccountBox
                            PaymentMethod.NET_BANKING -> Icons.Default.AccountBox
                            PaymentMethod.BANK_TRANSFER -> Icons.Default.AccountBox
                            PaymentMethod.CASH_ON_DELIVERY -> Icons.Default.AccountBox
                            PaymentMethod.DUE -> Icons.Default.AccountBox
                        },
                        title = label,
                        isSelected = selectedPaymentMethod == method,
                        onClick = { onPaymentMethodSelected(method) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Payment Split Option
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onPaymentSplit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (hasPaymentSplitWarning) Color(0xFFD32F2F) else Color(0xFFB8973D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (hasPaymentSplitWarning) Icons.Default.Warning else Icons.Default.AccountBox,
                        contentDescription = "Split Payment",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (hasPaymentSplitWarning) "Split Payment Again (Warning!)" else "Split Payment (Cash + Card + Bank + Due)",
                        fontWeight = FontWeight.Medium
                    )
                }

                // Exchange Gold Option
                Spacer(modifier = Modifier.height(8.dp))
                
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
                        Icons.Default.AccountBox,
                        contentDescription = "Exchange Gold",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (exchangeGoldValue > 0) "Exchange Gold Added (₹${formatCurrency(exchangeGoldValue)})" else "Exchange Gold (Old Gold Exchange)",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


@Composable
private fun PaymentMethodCard(
    paymentMethod: PaymentMethod,
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val primaryColor = MaterialTheme.colors.primary
    val lightPrimary = primaryColor.copy(alpha = 0.1f)

    val backgroundColor = if (isSelected) lightPrimary else Color.White
    val borderColor = if (isSelected) primaryColor else Color(0xFFE5E5E5)
    val iconBgColor = if (isSelected) primaryColor else Color(0xFFFFE9E4)
    val iconTint = if (isSelected) Color.White else Color(0xFF5E4034)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight() // ✅ Shrinks vertically to wrap content
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(interactionSource, indication = null) { onClick() }
            .hoverable(interactionSource)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with circular background
        Box(
            modifier = Modifier
                .size(35.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2E2E2E)
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
    onConfirmOrder: () -> Unit,
    isProcessing: Boolean,
    paymentMethodSelected: Boolean,
    cartViewModel: CartViewModel
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(cartItems) { cartItem ->
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
                    paymentSplit = paymentSplit,
                    discountAmount = discountAmount
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
                enabled = (paymentMethodSelected || paymentSplit != null) && !isProcessing,
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

        // Price - using final amount from cart screen calculation
        val metalRates = MetalRatesManager.metalRates.value
        val finalAmount = cartViewModel.calculateItemFinalAmount(cartItem, metalRates)
        Text(
            "₹${formatCurrency(finalAmount)}",
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
    paymentSplit: PaymentSplit,
    discountAmount: Double = 0.0
) {
    // Calculate adjusted due amount after discount
    val adjustedDueAmount = paymentSplit.dueAmount - discountAmount
    val isDueAmountNegative = adjustedDueAmount < 0
    val totalPayment = paymentSplit.cashAmount + paymentSplit.cardAmount + paymentSplit.bankAmount + paymentSplit.onlineAmount + adjustedDueAmount

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
        
        // Show discount if applied
        if (discountAmount > 0) {
            PaymentSplitRow("Discount Applied", -discountAmount, Color(0xFF4CAF50))
        }
        
        // Show due amount (adjusted for discount)
        if (adjustedDueAmount > 0) {
            PaymentSplitRow("Due", adjustedDueAmount, Color(0xFFFF9800))
        } else if (adjustedDueAmount < 0) {
            PaymentSplitRow("Due (Overpaid)", adjustedDueAmount, Color(0xFFD32F2F))
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

        // Warning message when due amount is negative
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
                    "Due amount is negative! Please split payment again to adjust amounts.",
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