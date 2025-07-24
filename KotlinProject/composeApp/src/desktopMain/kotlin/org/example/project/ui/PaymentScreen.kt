package org.example.project.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import org.example.project.data.CartItem
import org.example.project.viewModels.PaymentViewModel
import org.example.project.viewModels.CartViewModel
import org.example.project.data.PaymentMethod
import org.example.project.data.DiscountType
import org.example.project.viewModels.ProductsViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PaymentScreen(
    paymentViewModel: PaymentViewModel,
    cartViewModel: CartViewModel,
    customerViewModel: org.example.project.viewModels.CustomerViewModel,
    onBack: () -> Unit,
    onPaymentComplete: () -> Unit,
    productsViewModel: ProductsViewModel
) {
    val cart by cartViewModel.cart
    val cartImages by cartViewModel.cartImages
    val selectedPaymentMethod by paymentViewModel.selectedPaymentMethod
    val discountType by paymentViewModel.discountType
    val discountValue by paymentViewModel.discountValue
    val calculatedDiscountAmount = paymentViewModel.calculateDiscountAmount(cartViewModel.getSubtotal())
    val isProcessing by paymentViewModel.isProcessing
    val errorMessage by paymentViewModel.errorMessage
    val selectedCustomer by customerViewModel.selectedCustomer
    val isGstIncluded by paymentViewModel.isGstIncluded

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
            // Left Side - GST, Discounts & Payment Options (60%)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // GST Inclusion Section
                GstInclusionSection(
                    isGstIncluded = isGstIncluded,
                    onGstInclusionChange = { paymentViewModel.setGstIncluded(it) }
                )

                // Payment Options Section
                PaymentOptionsSection(
                    selectedPaymentMethod = selectedPaymentMethod,
                    onPaymentMethodSelected = { paymentViewModel.setPaymentMethod(it) }
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
                cartViewModel = cartViewModel, // Add this
                discountAmount = calculatedDiscountAmount,
                isGstIncluded = isGstIncluded,
                onConfirmOrder = {
                    // Update the order saving logic
                    val finalTotal = if (isGstIncluded) {
                        cartViewModel.getGrossTotal() + cartViewModel.getGST() - calculatedDiscountAmount
                    } else {
                        cartViewModel.getGrossTotal() - calculatedDiscountAmount
                    }

                    paymentViewModel.validateStockBeforeOrder(
                        cart = cart,
                        products = productsViewModel.products.value,
                        onValidationResult = { isValid, errors ->
                            if (isValid) {
                                paymentViewModel.saveOrderWithPaymentMethod(
                                    cart = cart,
                                    subtotal = cartViewModel.getSubtotal(),
                                    makingCharges = cartViewModel.getMakingCharges(),
                                    discountAmount = calculatedDiscountAmount,
                                    gst = if (isGstIncluded) cartViewModel.getGST() else 0.0,
                                    finalTotal = finalTotal,
                                    customerId = selectedCustomer?.id ?: "",
                                    isGstIncluded = isGstIncluded,
                                    onSuccess = onPaymentComplete
                                )
                            } else {
                                paymentViewModel.setErrorMessage(
                                    "Stock validation failed:\n${errors.joinToString("\n")}"
                                )
                            }
                        }
                    )
                },
                isProcessing = isProcessing,
                paymentMethodSelected = selectedPaymentMethod != null
            )
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
                            if (discountType == DiscountType.AMOUNT) "Enter amount" else "Enter percentage"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                      //  .height(48.dp)
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFB8973D),
                        cursorColor = Color(0xFFB8973D)
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
    onPaymentMethodSelected: (PaymentMethod) -> Unit
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
                    PaymentMethod.CARD to "Credit/Debit Card",
                    PaymentMethod.UPI to "UPI",
                    PaymentMethod.NET_BANKING to "Net Banking",
                    PaymentMethod.CASH_ON_DELIVERY to "Cash on Delivery"
                ).forEach { (method, label) ->
                    PaymentMethodCard(
                        paymentMethod = method,
                        icon = when (method) {
                            PaymentMethod.CARD -> Icons.Default.AccountCircle
                            PaymentMethod.UPI -> Icons.Default.AccountCircle
                            PaymentMethod.NET_BANKING -> Icons.Default.AccountCircle
                            PaymentMethod.CASH_ON_DELIVERY -> Icons.Default.AccountCircle
                        },
                        title = label,
                        isSelected = selectedPaymentMethod == method,
                        onClick = { onPaymentMethodSelected(method) },
                        modifier = Modifier.fillMaxWidth()
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




// Fixed PaymentScreen.kt - Order Summary Section with correct calculations
@Composable
private fun OrderSummarySection(
    modifier: Modifier = Modifier,
    cartItems: List<CartItem>,
    cartImages: Map<String, ImageBitmap>,
    cartViewModel: CartViewModel, // Add this parameter
    discountAmount: Double,
    isGstIncluded: Boolean,
    onConfirmOrder: () -> Unit,
    isProcessing: Boolean,
    paymentMethodSelected: Boolean
) {
    // Get correct calculations from CartViewModel
    val subtotal = cartViewModel.getSubtotal()
    val makingCharges = cartViewModel.getMakingCharges()
    val grossTotal = cartViewModel.getGrossTotal()
    val gst = if (isGstIncluded) cartViewModel.getGST() else 0.0
    val finalTotal = grossTotal + gst - discountAmount

    Card(
        modifier = modifier.fillMaxHeight(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                "Order Summary",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cart Items List with weight-based prices
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems) { cartItem ->
                    OrderSummaryItem(
                        cartItem = cartItem,
                        image = cartImages[cartItem.productId],
                        cartViewModel = cartViewModel // Pass cartViewModel to get correct price
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Price Breakdown
            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            PriceBreakdown(
                subtotal = subtotal,
                makingCharges = makingCharges,
                discountAmount = discountAmount,
                gst = gst,
                total = finalTotal,
                isGstIncluded = isGstIncluded
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm Order Button
            Button(
                onClick = onConfirmOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = paymentMethodSelected && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFB8973D),
                    contentColor = Color.White,
                    disabledBackgroundColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving Order...")
                } else {
                    Text(
                        "Confirm Order - ₹${formatCurrency(finalTotal)}",
                        fontSize = 16.sp,
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
    val itemPrice = cartViewModel.getItemPrice(cartItem)
    val itemWeight = cartViewModel.getItemWeight(cartItem)
    val pricePerGram = cartViewModel.getItemPricePerGram(cartItem)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product Image
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
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

        Spacer(modifier = Modifier.width(12.dp))

        // Product Details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                cartItem.product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2E2E2E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "Qty: ${cartItem.quantity} | Weight: ${String.format("%.2f", itemWeight * cartItem.quantity)}g",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )

            Text(
                "₹${formatCurrency(pricePerGram)}/g",
                fontSize = 11.sp,
                color = Color(0xFF888888)
            )
        }

        // Price - Fixed to show total price for quantity
        Text(
            "₹${formatCurrency(itemPrice)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB8973D)
        )
    }
}

@Composable
private fun PriceBreakdown(
    subtotal: Double,
    makingCharges: Double,
    discountAmount: Double,
    gst: Double,
    total: Double,
    isGstIncluded: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PriceRow(
            label = "Metal Cost",
            amount = subtotal,
            isTotal = false
        )

        PriceRow(
            label = "Making Charges",
            amount = makingCharges,
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

        if (isGstIncluded) {
            PriceRow(
                label = "GST (18%)",
                amount = gst,
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
private fun OrderSummaryItem(
    cartItem: CartItem,
    image: ImageBitmap?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product Image
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
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

        Spacer(modifier = Modifier.width(12.dp))

        // Product Details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                cartItem.product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2E2E2E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "Qty: ${cartItem.quantity}",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }

        // Price
        Text(
            "₹${formatCurrency(cartItem.product.price * cartItem.quantity)}",
            fontSize = 14.sp,
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
    total: Double,
    isGstIncluded: Boolean
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

        if (isGstIncluded) {
            PriceRow(
                label = "GST (18%)",
                amount = gst,
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

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

@Composable
private fun GstInclusionSection(
    isGstIncluded: Boolean,
    onGstInclusionChange: (Boolean) -> Unit
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
                .padding(20.dp)
        ) {
            Text(
                "Tax Options",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked = isGstIncluded,
                    onCheckedChange = onGstInclusionChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFB8973D),
                        uncheckedColor = Color(0xFFE0E0E0)
                    )
                )

                Column {
                    Text(
                        "Include GST (18%)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E2E2E)
                    )
                    Text(
                        "Check this box to add 18% GST to the final amount",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}