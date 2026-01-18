package org.example.project.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.invoice.model.*
import org.example.project.viewModels.InvoiceViewModel
import org.example.project.utils.CurrencyFormatter
import java.io.File
import java.time.format.DateTimeFormatter
import java.awt.Desktop
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

/**
 * Invoice Screen with inline editing via modal dialog
 * Follows iOS invoice app pattern: Edit button → modal → same screen updates
 */
@Composable
fun InvoiceScreen(
    viewModel: InvoiceViewModel,
    orderId: String? = null,
    onBack: () -> Unit = {},
    onStartNewOrder: (() -> Unit)? = null
) {
    val draft by viewModel.draft
    val finalInvoice by viewModel.finalInvoice
    val pdfPath by viewModel.pdfPath
    val isLoading by viewModel.isLoading
    val isGeneratingPDF by viewModel.isGeneratingPDF
    val errorMessage by viewModel.errorMessage
    
    var showEditDialog by remember { mutableStateOf(false) }
    
    // Track last invoice hash to detect content changes (not just reference changes)
    var lastInvoiceHash by remember { mutableStateOf<String?>(null) }
    // Track the PDF file path for this invoice to replace it on edits
    var currentPdfFile by remember { mutableStateOf<File?>(null) }
    
    // Auto-generate PDF when final invoice changes (including after edits)
    // Regenerates when pdfPath is null OR when invoice content changes
    // Replaces existing PDF file instead of creating new ones
    LaunchedEffect(finalInvoice, pdfPath, isGeneratingPDF) {
        finalInvoice?.let { invoice ->
            // Create a hash of invoice content to detect changes
            val invoiceHash = "${invoice.invoiceNo}_${invoice.buyer.name}_${invoice.buyer.address}_${invoice.city}_${invoice.memoNo}_${invoice.issueDate}_${invoice.discountAmount}_${invoice.gstPercentage}_${invoice.buyer.phone}_${invoice.placeOfDelivery}"
            
            // Regenerate PDF if:
            // 1. PDF path is null (initial load or after edit save)
            // 2. Invoice content changed (hash changed)
            // 3. Not currently generating
            val shouldRegenerate = (pdfPath == null || lastInvoiceHash != invoiceHash) && !isGeneratingPDF
            
            if (shouldRegenerate) {
                println("🔄 INVOICE SCREEN: Regenerating PDF")
                println("   - Hash changed: ${lastInvoiceHash} -> $invoiceHash")
                println("   - Invoice details: ${invoice.buyer.name}, ${invoice.city}, ${invoice.memoNo}")
                lastInvoiceHash = invoiceHash
                
                // Small delay to ensure invoice state is fully updated
                kotlinx.coroutines.delay(100)
                
                // Save to persistent location: ~/JewelryBills/ (same as PaymentViewModel)
                val userHome = System.getProperty("user.home")
                if (userHome != null) {
                    val billsDirectory = File(userHome, "JewelryBills")
                    if (!billsDirectory.exists()) {
                        billsDirectory.mkdirs()
                    }
                    
                    // Use consistent filename based on invoice number (replace existing file on edits)
                    // For initial generation, include timestamp. For edits, replace existing file.
                    val pdfFile = if (currentPdfFile != null && currentPdfFile!!.exists()) {
                        // Replace existing PDF file for edits
                        println("   - Replacing existing PDF: ${currentPdfFile!!.absolutePath}")
                        currentPdfFile!!
                    } else {
                        // Create new PDF file for initial generation
                        val pdfFileName = "Invoice_${invoice.invoiceNo}_${System.currentTimeMillis()}.pdf"
                        val newPdfFile = File(billsDirectory, pdfFileName)
                        println("   - Creating new PDF: ${newPdfFile.absolutePath}")
                        currentPdfFile = newPdfFile
                        newPdfFile
                    }
                    
                    viewModel.generatePDF(pdfFile)
                } else {
                    // Fallback to temp directory if user.home is not available
                    val tempDir = File(System.getProperty("java.io.tmpdir"))
                    val pdfFile = if (currentPdfFile != null && currentPdfFile!!.exists()) {
                        currentPdfFile!!
                    } else {
                        val newPdfFile = File(tempDir, "invoice_${invoice.invoiceNo}.pdf")
                        currentPdfFile = newPdfFile
                        newPdfFile
                    }
                    viewModel.generatePDF(pdfFile)
                }
            }
        }
    }
    
    // Update currentPdfFile when pdfPath changes
    LaunchedEffect(pdfPath) {
        pdfPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                currentPdfFile = file
            }
        }
    }
    
    // Show error if any
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // Error is already displayed in UI
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success Animation/Icon (matching ReceiptScreen)
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

            // Success Message (matching ReceiptScreen)
            Text(
                "Payment Successful!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            Text(
                "Your order has been confirmed and invoice is being generated",
                fontSize = 16.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            // Invoice Display with Edit button
            finalInvoice?.let { invoice ->
                // Transaction Details Card (matching ReceiptScreen style)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 6.dp,
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Header with Edit button and CONFIRMED badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Invoice Details",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E2E2E)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                
                                Button(
                                    onClick = { showEditDialog = true },
                                    enabled = draft != null,
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF2196F3)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Invoice Header Section - Two Column Layout (matching image with border)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 2.dp,
                            shape = RoundedCornerShape(8.dp),
                            backgroundColor = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Left Column: Customer Details
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InvoiceDetailRow("Name", invoice.buyer.name)
                                    InvoiceDetailRow("Address", invoice.buyer.address)
                                    InvoiceDetailRow("City", invoice.city)
                                    InvoiceDetailRow("Ph No.", invoice.buyer.phone)
                                }
                                
                                // Right Column: Order Details
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InvoiceDetailRow("Order No.", invoice.invoiceNo)
                                    InvoiceDetailRow("Memo No.", invoice.memoNo.ifEmpty { "GST/${invoice.invoiceNo.takeLast(8)}" })
                                    InvoiceDetailRow("Date", invoice.issueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                    val stateDisplay = if (invoice.buyer.stateName.isNotEmpty() && invoice.buyer.stateCode.isNotEmpty()) {
                                        "${invoice.buyer.stateName} (${invoice.buyer.stateCode})"
                                    } else if (invoice.buyer.stateName.isNotEmpty()) {
                                        invoice.buyer.stateName
                                    } else if (invoice.buyer.stateCode.isNotEmpty()) {
                                        "(${invoice.buyer.stateCode})"
                                    } else {
                                        ""
                                    }
                                    InvoiceDetailRow("State Name & Code", stateDisplay)
                                    InvoiceDetailRow("Place of Delivery", invoice.placeOfDelivery.ifEmpty { invoice.buyer.stateName })
                                }
                            }
                        }

                        // Items Details Section
                        if (invoice.items.isNotEmpty()) {
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

                            invoice.items.forEachIndexed { index, item ->
                                InvoiceItemDetailCard(
                                    item = item,
                                    index = index
                                )
                                if (index < invoice.items.size - 1) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }

                        // Payment Summary
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

                        InvoicePaymentSummary(invoice = invoice)

                        // Payment Breakdown Section
                        if (invoice.paymentSplit != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))

                            InvoicePaymentBreakdownSection(
                                paymentSplit = invoice.paymentSplit
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // PDF Status
                PDFStatusCard(
                    pdfPath = pdfPath,
                    isGenerating = isGeneratingPDF,
                    error = errorMessage,
                    onDownload = {
                        pdfPath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
                                try {
                                    if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().open(file.parentFile)
                                    }
                                } catch (e: Exception) {
                                    // Fallback: just show the path
                                }
                            }
                        }
                    },
                    onOpen = {
                        pdfPath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
                                try {
                                    if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().open(file)
                                    }
                                } catch (e: Exception) {
                                    // Error opening file
                                }
                            }
                        }
                    }
                )
            } ?: run {
                // Loading or no invoice state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color.White
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading || isGeneratingPDF || (draft == null && finalInvoice == null)) {
                            // Show loading state while data is being fetched or PDF is being generated
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = Color(0xFFB8973D)
                                )
                                Text(
                                    if (isGeneratingPDF) "Generating PDF..." else "Loading invoice data...",
                                    fontSize = 16.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        } else {
                            // Only show "No invoice data available" if we're sure data is not loading
                            Text(
                                "No invoice data available",
                                fontSize = 16.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Button - New Order only
            onStartNewOrder?.let { onNewOrder ->
                Button(
                    onClick = onNewOrder,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
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
                    Text("New Order", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        
        // Edit Dialog (Modal)
        if (showEditDialog && draft != null) {
            InvoiceEditDialog(
                draft = draft!!,
                onDismiss = { showEditDialog = false },
                onSave = { updatedDraft ->
                    viewModel.saveDraft(updatedDraft)
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
private fun InvoiceDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$label:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666),
            modifier = Modifier.widthIn(min = 120.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            value.ifEmpty { "-" },
            fontSize = 14.sp,
            color = Color(0xFF2E2E2E),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InvoiceItemDetailCard(
    item: InvoiceItem,
    index: Int
) {
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
                    text = "${index + 1}. ${item.productName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E2E2E)
                )
                Text(
                    text = "Qty: ${item.quantity}",
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
                        text = "Material: ${item.materialWithPurity}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "Weight: ${String.format("%.2f", item.grossWeight)}g",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (item.makingPercent > 0) {
                        Text(
                            text = "Making: ${String.format("%.2f", item.makingPercent)}%",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            // Amount Breakdown
            Divider(thickness = 1.dp, color = Color(0xFFE0E0E0))
            
            InvoiceItemAmountRow(label = "Base Amount", amount = item.grossProductPrice)
            if (item.labourCharges > 0) {
                InvoiceItemAmountRow(label = "Labour Charges", amount = item.labourCharges)
            }
            if (item.stoneAmount > 0) {
                InvoiceItemAmountRow(label = "Stone Amount", amount = item.stoneAmount)
            }
            
            Divider(thickness = 1.dp, color = Color(0xFFE0E0E0))
            
            InvoiceItemAmountRow(
                label = "Item Total",
                amount = item.costValue,
                isTotal = true
            )
        }
    }
}

@Composable
private fun InvoiceItemAmountRow(
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
            text = "${CurrencyFormatter.formatRupees(amount, includeDecimals = true)}",
            fontSize = if (isTotal) 14.sp else 12.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) Color(0xFF4CAF50) else Color(0xFF666666)
        )
    }
}

@Composable
private fun InvoicePaymentSummary(invoice: Invoice) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InvoiceAmountRow("Subtotal", invoice.subtotal)
        
        if (invoice.exchangeGoldValue > 0) {
            InvoiceAmountRow("Exchange Gold", -invoice.exchangeGoldValue, isDiscount = true)
        }
        
        if (invoice.discountAmount > 0) {
            InvoiceAmountRow("Discount", -invoice.discountAmount, isDiscount = true)
        }

        if (invoice.gstAmount > 0) {
            val gstPercentageDisplay = if (invoice.gstPercentage % 1.0 == 0.0) {
                "${invoice.gstPercentage.toInt()}%"
            } else {
                "${String.format("%.1f", invoice.gstPercentage)}%"
            }
            InvoiceAmountRow("GST ($gstPercentageDisplay)", invoice.gstAmount)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(thickness = 1.dp, color = Color(0xFFE0E0E0))
        Spacer(modifier = Modifier.height(8.dp))

        // Total Amount row with bold styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total Amount",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )
            Text(
                text = "${CurrencyFormatter.formatRupees(invoice.netAmount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun InvoiceAmountRow(
    label: String,
    amount: Double,
    isDiscount: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
        Text(
            "${if (isDiscount) "-" else ""}${CurrencyFormatter.formatRupees(kotlin.math.abs(amount))}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                isDiscount -> Color(0xFF4CAF50)
                else -> Color(0xFF2E2E2E)
            }
        )
    }
}

@Composable
private fun InvoicePaymentBreakdownSection(
    paymentSplit: PaymentSplit
) {
    val dueAmount = paymentSplit.dueAmount
    val isDueAmountNegative = dueAmount < 0
    val totalPayment = paymentSplit.bank + paymentSplit.cash + dueAmount

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Payment Breakdown",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDueAmountNegative) Color(0xFFD32F2F) else Color(0xFF2E2E2E)
        )

        if (paymentSplit.cash > 0) {
            InvoicePaymentSplitRow("Cash", paymentSplit.cash, Color(0xFF4CAF50))
        }
        if (paymentSplit.bank > 0) {
            InvoicePaymentSplitRow("Bank/Card/Online", paymentSplit.bank, Color(0xFF2196F3))
        }

        if (dueAmount > 0) {
            InvoicePaymentSplitRow("Due", dueAmount, Color(0xFFFF9800))
        } else if (dueAmount < 0) {
            InvoicePaymentSplitRow("Due (Overpaid)", dueAmount, Color(0xFFD32F2F))
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
                "${CurrencyFormatter.formatRupees(totalPayment)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDueAmountNegative) Color(0xFFD32F2F) else Color(0xFFB8973D)
            )
        }
    }
}

@Composable
private fun InvoicePaymentSplitRow(
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
private fun InvoiceDisplayCard(invoice: Invoice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 6.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Invoice Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Invoice #${invoice.invoiceNo}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E2E2E)
                    )
                    Text(
                        "Date: ${invoice.issueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
            
            Divider()
            
            // Parties Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Seller
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Seller",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(invoice.seller.name, fontSize = 14.sp, color = Color(0xFF2E2E2E))
                    Text(invoice.seller.address, fontSize = 12.sp, color = Color(0xFF666666))
                    if (invoice.seller.gstin.isNotEmpty()) {
                        Text("GSTIN: ${invoice.seller.gstin}", fontSize = 12.sp, color = Color(0xFF666666))
                    }
                }
                
                // Buyer
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        "Buyer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(invoice.buyer.name, fontSize = 14.sp, color = Color(0xFF2E2E2E), textAlign = TextAlign.End)
                    Text(invoice.buyer.address, fontSize = 12.sp, color = Color(0xFF666666), textAlign = TextAlign.End)
                    if (invoice.buyer.gstin.isNotEmpty()) {
                        Text("GSTIN: ${invoice.buyer.gstin}", fontSize = 12.sp, color = Color(0xFF666666), textAlign = TextAlign.End)
                    }
                }
            }
            
            Divider()
            
            // Items Table
            Text(
                "Items",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )
            
            invoice.items.forEachIndexed { index, item ->
                InvoiceItemRow(item = item, index = index + 1)
                if (index < invoice.items.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            Divider()
            
            // Totals Section
            InvoiceTotalsSection(invoice = invoice)
            
            // Notes
            if (invoice.notes.isNotEmpty()) {
                Divider()
                Column {
                    Text(
                        "Notes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(invoice.notes, fontSize = 14.sp, color = Color(0xFF2E2E2E))
                }
            }
        }
    }
}

@Composable
private fun InvoiceItemRow(item: InvoiceItem, index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(0xFFF8F9FA)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$index. ${item.productName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E2E2E)
                )
                Text(
                    CurrencyFormatter.formatRupees(item.costValue),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Qty: ${item.quantity} | ${item.materialWithPurity} | ${String.format("%.2f", item.grossWeight)}g",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun InvoiceTotalsSection(invoice: Invoice) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InvoiceTotalRow("Subtotal", invoice.subtotal)
        
        if (invoice.exchangeGoldValue > 0) {
            InvoiceTotalRow("Exchange Gold", -invoice.exchangeGoldValue, isDiscount = true)
        }
        
        if (invoice.discountAmount > 0) {
            InvoiceTotalRow("Discount", -invoice.discountAmount, isDiscount = true)
        }
        
        if (invoice.gstAmount > 0) {
            InvoiceTotalRow("GST (${invoice.gstPercentage}%)", invoice.gstAmount)
        }
        
        Divider()
        
        InvoiceTotalRow(
            "Total Amount",
            invoice.netAmount,
            isTotal = true
        )
    }
}

@Composable
private fun InvoiceTotalRow(
    label: String,
    amount: Double,
    isDiscount: Boolean = false,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = if (isTotal) 16.sp else 14.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) Color(0xFF2E2E2E) else Color(0xFF666666)
        )
        Text(
            "${if (isDiscount) "-" else ""}${CurrencyFormatter.formatRupees(kotlin.math.abs(amount))}",
            fontSize = if (isTotal) 16.sp else 14.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isTotal -> Color(0xFF4CAF50)
                isDiscount -> Color(0xFF4CAF50)
                else -> Color(0xFF2E2E2E)
            }
        )
    }
}

@Composable
private fun PDFStatusCard(
    pdfPath: String?,
    isGenerating: Boolean,
    error: String?,
    onDownload: () -> Unit = {},
    onOpen: () -> Unit = {}
) {
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
            when {
                isGenerating -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFF9800)
                        )
                        Text(
                            "Generating PDF...",
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                error != null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            error,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
                pdfPath != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "PDF Ready",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "PDF Generated Successfully",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        
                        // Show file location
                        val file = File(pdfPath!!)
                        Text(
                            "Saved to: ${file.parent}",
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "File: ${file.name}",
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center
                        )
                        
                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDownload,
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = "Open Folder",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Folder", fontSize = 12.sp)
                            }
                            
                            Button(
                                onClick = onOpen,
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF2196F3)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Open PDF",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open PDF", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "PDF will be generated shortly",
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modal dialog for editing invoice
 */
@Composable
private fun InvoiceEditDialog(
    draft: InvoiceDraft,
    onDismiss: () -> Unit,
    onSave: (InvoiceDraft) -> Unit
) {
    var editedDraft by remember { mutableStateOf(draft) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            elevation = 12.dp,
            backgroundColor = Color(0xFFFAFAFA)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                // Header with better styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Edit Invoice",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        letterSpacing = 0.5.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Editable Content - Two Column Layout (matching image)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Two-column layout: Customer Details (Left) | Order Details (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left Column: Customer Details
                        Card(
                            modifier = Modifier.weight(1f),
                            elevation = 2.dp,
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = Color.White
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Customer Details",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A),
                                    letterSpacing = 0.3.sp
                                )
                                
                                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                            
                            InvoiceEditField(
                                label = "Name",
                                value = editedDraft.buyer.name,
                                onValueChange = { value ->
                                    editedDraft = editedDraft.copy(
                                        buyer = editedDraft.buyer.copy(name = value)
                                    )
                                }
                            )
                            
                            InvoiceEditField(
                                label = "Address",
                                value = editedDraft.buyer.address,
                                onValueChange = { value ->
                                    editedDraft = editedDraft.copy(
                                        buyer = editedDraft.buyer.copy(address = value)
                                    )
                                },
                                isMultiline = true
                            )
                            
                            InvoiceEditField(
                                label = "City",
                                value = editedDraft.city ?: "",
                                onValueChange = { value ->
                                    editedDraft = editedDraft.copy(city = value.takeIf { it.isNotEmpty() })
                                }
                            )
                            
                            InvoiceEditField(
                                label = "Ph No.",
                                value = editedDraft.buyer.phone,
                                onValueChange = { value ->
                                    editedDraft = editedDraft.copy(
                                        buyer = editedDraft.buyer.copy(phone = value)
                                    )
                                }
                            )
                            }
                        }
                        
                        // Right Column: Order Details
                        Card(
                            modifier = Modifier.weight(1f),
                            elevation = 2.dp,
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = Color.White
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Order Details",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A),
                                    letterSpacing = 0.3.sp
                                )
                                
                                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                            
                            // Order No. (read-only)
                            Column {
                                Text(
                                    "Order No.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF555555),
                                    letterSpacing = 0.2.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = editedDraft.invoiceNo,
                                    onValueChange = { },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    readOnly = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        backgroundColor = Color(0xFFF5F5F5),
                                        disabledBorderColor = Color(0xFFE0E0E0),
                                        disabledTextColor = Color(0xFF999999)
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp,
                                        letterSpacing = 0.3.sp
                                    )
                                )
                            }
                            
                            InvoiceEditField(
                                label = "Memo No.",
                                value = editedDraft.memoNo ?: "GST/${editedDraft.invoiceNo.takeLast(8)}",
                                onValueChange = { value ->
                                    editedDraft = editedDraft.copy(memoNo = value.takeIf { it.isNotEmpty() })
                                }
                            )
                            
                            // Date (editable)
                            InvoiceEditField(
                                label = "Date",
                                value = editedDraft.issueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                onValueChange = { value ->
                                    try {
                                        val date = java.time.LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                        editedDraft = editedDraft.copy(issueDate = date)
                                    } catch (e: Exception) {
                                        // Invalid date format, ignore
                                    }
                                }
                            )
                            
                            // State Name & Code
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InvoiceEditField(
                                    label = "State Name",
                                    value = editedDraft.buyer.stateName,
                                    onValueChange = { value ->
                                        editedDraft = editedDraft.copy(
                                            buyer = editedDraft.buyer.copy(stateName = value)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                
                                InvoiceEditField(
                                    label = "State Code",
                                    value = editedDraft.buyer.stateCode,
                                    onValueChange = { value ->
                                        editedDraft = editedDraft.copy(
                                            buyer = editedDraft.buyer.copy(stateCode = value)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            InvoiceEditField(
                                label = "Place of Delivery",
                                value = editedDraft.placeOfDelivery ?: "",
                                onValueChange = { value ->
                                    editedDraft = editedDraft.copy(placeOfDelivery = value.takeIf { it.isNotEmpty() })
                                }
                            )
                            }
                        }
                    }
                    
                    // Notes Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp,
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Notes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A),
                                letterSpacing = 0.3.sp
                            )
                            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            InvoiceEditField(
                                label = "",
                                value = editedDraft.notes ?: "",
                                onValueChange = { value ->
                                    editedDraft = editedDraft.copy(notes = value.takeIf { it.isNotEmpty() })
                                },
                                isMultiline = true
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons with better styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE0E0E0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF666666)
                        )
                    ) {
                        Text(
                            "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Button(
                        onClick = { onSave(editedDraft) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            "Save",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isMultiline: Boolean = false
) {
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF555555),
                letterSpacing = 0.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = !isMultiline,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = Color(0xFFFAFAFA),
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedLabelColor = Color(0xFF4CAF50),
                cursorColor = Color(0xFF4CAF50),
                textColor = Color(0xFF1A1A1A)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                letterSpacing = 0.3.sp
            )
        )
    }
}
