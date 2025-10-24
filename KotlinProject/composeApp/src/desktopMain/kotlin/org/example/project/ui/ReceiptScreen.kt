package org.example.project.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.PaymentTransaction
import org.example.project.data.CartItem
import org.example.project.data.MetalRatesManager
import org.example.project.viewModels.PaymentViewModel
import org.example.project.viewModels.CustomerViewModel
import org.example.project.utils.ImageLoader
import org.example.project.JewelryAppInitializer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReceiptScreen(
    paymentViewModel: PaymentViewModel,
    customerViewModel: CustomerViewModel,
    onStartNewOrder: () -> Unit,
    onBackToBilling: () -> Unit,
    productsViewModel: org.example.project.viewModels.ProductsViewModel = JewelryAppInitializer.getViewModel()
) {
    val lastTransaction by paymentViewModel.lastTransaction
    val isGeneratingPDF by paymentViewModel.isGeneratingPDF
    val pdfPath by paymentViewModel.pdfPath
    val selectedCustomer by customerViewModel.selectedCustomer
    val errorMessage by paymentViewModel.errorMessage
    val successMessage by paymentViewModel.successMessage
    val isGstIncluded by paymentViewModel.isGstIncluded

    // Auto-generate PDF if we have a transaction and no file yet
    LaunchedEffect(lastTransaction, selectedCustomer, pdfPath) {
        if (lastTransaction != null && pdfPath == null && !isGeneratingPDF) {
            selectedCustomer?.let { paymentViewModel.generatePdfForLastTransaction(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Animation/Icon
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(60.dp),
            backgroundColor = Color(0xFF4CAF50),
            elevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Success Message
        Text(
            "Payment Successful!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E2E2E)
        )

        Text(
            "Your order has been confirmed and receipt is being generated",
            fontSize = 16.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Transaction Details Card
        lastTransaction?.let { transaction ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Transaction Details",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E2E2E)
                        )

                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFF4CAF50),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "CONFIRMED",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Order Details
                    DetailRow("Order ID", transaction.id)
                    DetailRow("Date & Time", formatDateTime(transaction.timestamp))

                    selectedCustomer?.let { customer ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Customer Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E2E2E)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        DetailRow("Name", customer.name)
                        if (customer.email.isNotEmpty()) {
                            DetailRow("Email", customer.email)
                        }
                        if (customer.phone.isNotEmpty()) {
                            DetailRow("Phone", customer.phone)
                        }
                    }

                    // Items Details Section (moved before payment summary)
                    if (transaction.items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Items Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E2E2E)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        transaction.items.forEachIndexed { index, cartItem ->
                            ItemDetailCard(
                                cartItem = cartItem,
                                index = index,
                                productsViewModel = productsViewModel
                            )
                            if (index < transaction.items.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    // Payment Summary (moved after items details)
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Payment Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E2E2E)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Simplified Payment Summary without making charges
                    PaymentSummaryBreakdown(
                        transaction = transaction,
                        productsViewModel = productsViewModel
                    )

                    // Payment Breakdown Section (after payment summary)
                    if (transaction.paymentSplit != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        PaymentBreakdownSection(
                            paymentSplit = transaction.paymentSplit
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PDF Generation Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isGeneratingPDF) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFF9800)
                        )
                        Text(
                            "Generating invoice...",
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                    } else if (pdfPath != null) {
                        val isHtmlFile = pdfPath!!.endsWith(".html", ignoreCase = true)
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Invoice Ready",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            if (isHtmlFile) "Invoice Generated and Opened in Browser" else "PDF Generated Successfully",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4CAF50)
                        )
                    } else {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Invoice will be generated shortly",
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }

                // Always show Download button; enable only when PDF is ready
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { if (pdfPath != null) paymentViewModel.downloadBill() },
                    enabled = pdfPath != null && !isGeneratingPDF,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF2196F3),
                        disabledBackgroundColor = Color(0xFF90CAF9),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download Invoice",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download PDF Invoice",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error Message
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFFFEBEE),
                border = BorderStroke(1.dp, Color(0xFFE57373)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        error,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Success Message
        successMessage?.let { success ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFE8F5E8),
                border = BorderStroke(1.dp, Color(0xFF81C784)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        success,
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Diagnostic Button (only show if there's an error)
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { paymentViewModel.runPDFDiagnostics() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = "Diagnose PDF",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Diagnose PDF Issue",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBackToBilling,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF666666))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Back to Billing",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onStartNewOrder,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFB8973D)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New Order",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "New Order",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Footer Message
        Text(
            "Thank you for choosing Premium Jewelry Store!\nA PDF copy of your receipt has been generated.",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2E2E2E),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun AmountRow(
    label: String,
    amount: Double,
    isDiscount: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
        Text(
            "${if (isDiscount) "-" else ""}₹${formatCurrency(kotlin.math.abs(amount))}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                isDiscount -> Color(0xFF4CAF50)
                else -> Color(0xFF2E2E2E)
            }
        )
    }
}

private fun formatDateTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

private fun formatPaymentMethod(paymentMethod: org.example.project.data.PaymentMethod): String {
    return when (paymentMethod) {
        org.example.project.data.PaymentMethod.CASH -> "Cash"
        org.example.project.data.PaymentMethod.CARD -> "Credit/Debit Card"
        org.example.project.data.PaymentMethod.UPI -> "UPI"
        org.example.project.data.PaymentMethod.NET_BANKING -> "Net Banking"
        org.example.project.data.PaymentMethod.BANK_TRANSFER -> "Bank Transfer"
        org.example.project.data.PaymentMethod.CASH_ON_DELIVERY -> "Cash on Delivery"
        org.example.project.data.PaymentMethod.DUE -> "Due Payment"
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

@Composable
private fun PaymentBreakdownSection(
    paymentSplit: org.example.project.data.PaymentSplit
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
                    "Due amount is negative! Payment split amounts need adjustment.",
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
private fun PaymentSummaryBreakdown(
    transaction: PaymentTransaction,
    productsViewModel: org.example.project.viewModels.ProductsViewModel
) {
    val metalRates = MetalRatesManager.metalRates.value
    
    // Calculate subtotal as sum of all item totals (like payment screen)
    val itemSubtotal = transaction.items.sumOf { cartItem ->
        // Use the same calculation logic as ItemDetailCard
        val metalKarat = if (cartItem.metal.isNotEmpty()) {
            cartItem.metal.replace("K", "").toIntOrNull() ?: org.example.project.data.extractKaratFromMaterialType(cartItem.product.materialType)
        } else {
            org.example.project.data.extractKaratFromMaterialType(cartItem.product.materialType)
        }
        
        val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else metalRates.getGoldRateForKarat(metalKarat)
        val silverRate = metalRates.getSilverRateForPurity(999)
        
        // Use product values as fallbacks if cart item values are 0
        val grossWeight = if (cartItem.grossWeight > 0) cartItem.grossWeight else cartItem.product.totalWeight
        val lessWeight = if (cartItem.lessWeight > 0) cartItem.lessWeight else cartItem.product.lessWeight
        val netWeight = grossWeight - lessWeight
        val quantity = cartItem.quantity
        val makingChargesPerGram = if (cartItem.makingCharges > 0) cartItem.makingCharges else cartItem.product.defaultMakingRate
        val cwWeight = if (cartItem.cwWeight > 0) cartItem.cwWeight else cartItem.product.cwWeight
        val stoneRate = if (cartItem.stoneRate > 0) cartItem.stoneRate else cartItem.product.stoneRate
        val stoneQuantity = if (cartItem.stoneQuantity > 0) cartItem.stoneQuantity else cartItem.product.stoneQuantity
        val vaCharges = if (cartItem.va > 0) cartItem.va else cartItem.product.vaCharges
        
        // Calculate item total (same as ItemDetailCard)
        val baseAmount = when {
            cartItem.product.materialType.contains("gold", ignoreCase = true) -> netWeight * goldRate * quantity
            cartItem.product.materialType.contains("silver", ignoreCase = true) -> netWeight * silverRate * quantity
            else -> netWeight * goldRate * quantity // Default to gold rate
        }
        
        val makingCharges = netWeight * makingChargesPerGram * quantity
        val stoneAmount = stoneRate * stoneQuantity * cwWeight
        val totalCharges = baseAmount + makingCharges + stoneAmount + vaCharges
        
        totalCharges
    }
    
    // Apply discount to subtotal
    val discountAmount = transaction.discountAmount
    val subtotalWithGst = itemSubtotal + (itemSubtotal * transaction.gstRate)
    val taxableAmount = subtotalWithGst - discountAmount
    
    // Calculate GST on original subtotal (not on discounted amount)
    val gstPercentage = (transaction.gstRate * 100).toInt() // Convert from decimal to percentage
    val gst = itemSubtotal * transaction.gstRate
    
    // Final total = subtotal + GST - discount
    val finalTotal = itemSubtotal + gst - discountAmount

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AmountRow(
            label = "Subtotal",
            amount = itemSubtotal
        )
        
        if (discountAmount > 0) {
            AmountRow(
                label = "Discount",
                amount = discountAmount,
                isDiscount = true
            )
        }

        if (gst > 0) {
            AmountRow(
                label = "GST (${gstPercentage}%)",
                amount = gst
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(thickness = 1.dp, color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(8.dp))

        // Total Amount row with bold styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total Amount",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )
            Text(
                text = "₹${formatCurrency(finalTotal)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }

        // Add GST status indicator
//        Spacer(modifier = Modifier.height(8.dp))
//        Text(
//            if (gst > 0) "* GST (${gstPercentage}%) included in total amount" else "* No GST applied",
//            fontSize = 12.sp,
//            color = Color(0xFF666666),
//            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
//        )
    }
}

@Composable
private fun PriceBreakdown(
    subtotal: Double,
    makingCharges: Double,
    discountAmount: Double,
    gst: Double,
    total: Double,
    isGstIncluded: Boolean,
    transaction: PaymentTransaction? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Calculate stone charges and VA charges from transaction items
        val stoneCharges = transaction?.items?.sumOf { item ->
            item.stoneRate * item.stoneQuantity * item.cwWeight
        } ?: 0.0
        
        val vaCharges = transaction?.items?.sumOf { item ->
            item.va
        } ?: 0.0

        AmountRow(
            label = "Subtotal",
            amount = subtotal
        )

        AmountRow(
            label = "Making Charges",
            amount = makingCharges
        )

        if (stoneCharges > 0) {
            AmountRow(
                label = "Stone Charges",
                amount = stoneCharges
            )
        }

        if (vaCharges > 0) {
            AmountRow(
                label = "VA Charges",
                amount = vaCharges
            )
        }

        if (discountAmount > 0) {
            AmountRow(
                label = "Discount",
                amount = -discountAmount,
                isDiscount = true
            )
        }

        // Calculate GST percentage dynamically
        val gstPercentage = if (subtotal > 0) (gst / subtotal * 100).toInt() else 0
        
        if (gst > 0) {
            AmountRow(
                label = "GST (${gstPercentage}%)",
                amount = gst
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(thickness = 1.dp, color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Total Amount",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )
            Text(
                "₹${formatCurrency(total)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }

        // Add GST status indicator
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (gst > 0) "* GST (${gstPercentage}%) included in total amount" else "* No GST applied",
            fontSize = 12.sp,
            color = Color(0xFF666666),
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

@Composable
private fun ItemDetailCard(
    cartItem: CartItem,
    index: Int,
    productsViewModel: org.example.project.viewModels.ProductsViewModel
) {
    val metalRates = MetalRatesManager.metalRates.value
    val product = cartItem.product
    
    // Use the same calculation logic as CartViewModel
    val metalKarat = if (cartItem.metal.isNotEmpty()) {
        cartItem.metal.replace("K", "").toIntOrNull() ?: org.example.project.data.extractKaratFromMaterialType(cartItem.product.materialType)
    } else {
        org.example.project.data.extractKaratFromMaterialType(cartItem.product.materialType)
    }
    
    val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else metalRates.getGoldRateForKarat(metalKarat)
    val silverRate = metalRates.getSilverRateForPurity(999)
    
    // Use product values as fallbacks if cart item values are 0
    val grossWeight = if (cartItem.grossWeight > 0) cartItem.grossWeight else product.totalWeight
    val lessWeight = if (cartItem.lessWeight > 0) cartItem.lessWeight else product.lessWeight
    val netWeight = grossWeight - lessWeight
    val quantity = cartItem.quantity
    val makingChargesPerGram = if (cartItem.makingCharges > 0) cartItem.makingCharges else product.defaultMakingRate
    val cwWeight = if (cartItem.cwWeight > 0) cartItem.cwWeight else product.cwWeight
    val stoneRate = if (cartItem.stoneRate > 0) cartItem.stoneRate else product.stoneRate
    val stoneQuantity = if (cartItem.stoneQuantity > 0) cartItem.stoneQuantity else product.stoneQuantity
    val vaCharges = if (cartItem.va > 0) cartItem.va else product.vaCharges
    
    // Calculate amounts using the same logic as CartViewModel
    val baseAmount = when {
        product.materialType.contains("gold", ignoreCase = true) -> netWeight * goldRate * quantity
        product.materialType.contains("silver", ignoreCase = true) -> netWeight * silverRate * quantity
        else -> netWeight * goldRate * quantity // Default to gold rate
    }
    
    val makingCharges = netWeight * makingChargesPerGram * quantity
    val stoneAmount = stoneRate * stoneQuantity * cwWeight
    val totalCharges = baseAmount + makingCharges + stoneAmount + vaCharges

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(0xFFF8F9FA)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Item Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}. ${product.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E2E2E)
                )
                Text(
                    text = "Qty: ${quantity}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
            }

            // Material and Weight Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Material: ${cartItem.metal}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "Weight: ${String.format("%.2f", netWeight)}g",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (makingChargesPerGram > 0) {
                        Text(
                            text = "Making: ₹${String.format("%.2f", makingChargesPerGram)}/g",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            // Amount Breakdown
            Divider(thickness = 1.dp, color = Color(0xFFE0E0E0))
            
            ItemAmountRow(label = "Base Amount", amount = baseAmount)
            if (makingCharges > 0) {
                ItemAmountRow(label = "Making Charges", amount = makingCharges)
            }
            if (stoneAmount > 0) {
                ItemAmountRow(label = "Stone Amount", amount = stoneAmount)
            }
            if (vaCharges > 0) {
                ItemAmountRow(label = "VA Charges", amount = vaCharges)
            }
            
            Divider(thickness = 1.dp, color = Color(0xFFE0E0E0))
            
            ItemAmountRow(
                label = "Item Total",
                amount = totalCharges,
                isTotal = true
            )

            // Customization Notes (if available in CartItem)
            // Note: CartItem doesn't have customizationNotes field, so removing this section
        }
    }
}

@Composable
private fun ItemAmountRow(
    label: String,
    amount: Double,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isTotal) 14.sp else 12.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) Color(0xFF2E2E2E) else Color(0xFF666666)
        )
        Text(
            text = "₹${String.format("%.2f", amount)}",
            fontSize = if (isTotal) 14.sp else 12.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) Color(0xFF4CAF50) else Color(0xFF666666)
        )
    }
}