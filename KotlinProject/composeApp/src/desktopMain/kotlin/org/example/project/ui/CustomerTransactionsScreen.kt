package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.Order
import org.example.project.data.PaymentStatus
import org.example.project.data.User
import org.example.project.data.CashAmount
import org.example.project.data.CashAmountRepository
import org.example.project.data.FirestoreCashAmountRepository
import org.example.project.data.UnifiedTransaction
import org.example.project.data.TransactionType
import org.example.project.data.CashTransactionType
import org.example.project.viewModels.ProfileViewModel
import org.example.project.utils.PdfGeneratorService
import org.example.project.utils.CurrencyFormatter
import org.example.project.JewelryAppInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Desktop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomerTransactionsScreen(
    customer: User,
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.customerTransactions
    val loading by viewModel.loading
    val error by viewModel.error

    // Get updated customer data from the customers list
    val allCustomers by viewModel.customers
    val updatedCustomer = allCustomers.find { it.customerId == customer.customerId } ?: customer

    // PDF Generation State - per order
    var generatingOrderIds by remember { mutableStateOf(setOf<String>()) }
    var pdfGenerationMessage by remember { mutableStateOf<String?>(null) }
    var pdfGenerationError by remember { mutableStateOf<String?>(null) }
    val pdfGeneratorService = remember { PdfGeneratorService() }

    // Cash Amount Dialog State
    var showCashDialog by remember { mutableStateOf(false) }
    var cashTransactionMessage by remember { mutableStateOf<String?>(null) }
    var cashTransactionError by remember { mutableStateOf<String?>(null) }
    val cashAmountRepository = remember { FirestoreCashAmountRepository(JewelryAppInitializer.getFirestore()) }

    // PDF Generation Function
    fun generateOrderPDF(order: Order, customer: User) {
        generatingOrderIds = generatingOrderIds + order.orderId
        pdfGenerationError = null
        pdfGenerationMessage = null
        
        // Use coroutine scope for async operations
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Show file dialog to select save location
                val fileDialog = FileDialog(Frame()).apply {
                    mode = FileDialog.SAVE
                    title = "Save Invoice PDF"
                    file = "Invoice_${order.orderId}_${System.currentTimeMillis()}.pdf"
                    isVisible = true
                }

                val selectedFile = if (fileDialog.directory != null && fileDialog.file != null) {
                    val fileName = fileDialog.file
                    val finalFileName = if (!fileName.lowercase().endsWith(".pdf")) {
                        "$fileName.pdf"
                    } else {
                        fileName
                    }
                    File(fileDialog.directory, finalFileName)
                } else {
                    // User cancelled the dialog
                    generatingOrderIds = generatingOrderIds - order.orderId
                    return@launch
                }

                // Fetch invoice configuration directly; fall back to defaults if not found
                val invoiceConfig = org.example.project.data.InvoiceConfigRepository()
                    .getInvoiceConfig() ?: org.example.project.data.InvoiceConfig()
                
                val pdfResult = pdfGeneratorService.generateInvoicePDF(
                    order = order,
                    customer = customer,
                    outputFile = selectedFile,
                    invoiceConfig = invoiceConfig
                )

                if (pdfResult.isSuccess) {
                    pdfGenerationMessage = "PDF invoice generated successfully! Opening PDF..."
                    
                    // Automatically open the PDF
                    try {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(selectedFile)
                            pdfGenerationMessage = "PDF invoice generated and opened successfully!"
                        } else {
                            pdfGenerationMessage = "PDF invoice generated successfully! File saved at: ${selectedFile.absolutePath}"
                        }
                    } catch (e: Exception) {
                        pdfGenerationMessage = "PDF generated successfully! File saved at: ${selectedFile.absolutePath} (Could not open automatically: ${e.message})"
                    }
                } else {
                    pdfGenerationError = "Failed to generate PDF invoice: ${pdfResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                pdfGenerationError = "PDF generation failed: ${e.message}"
            } finally {
                generatingOrderIds = generatingOrderIds - order.orderId
            }
        }
    }

    // Cash Transaction Function
    fun saveCashTransaction(cashAmount: CashAmount) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                cashTransactionError = null
                cashTransactionMessage = null
                
                println("💰 CASH TRANSACTION: Attempting to save cash transaction for customer ${cashAmount.customerId}")
                println("💰 CASH TRANSACTION: Amount: ${cashAmount.amount}, Type: ${cashAmount.transactionType}")
                
                val transactionId = cashAmountRepository.addCashTransaction(cashAmount)
                
                println("💰 CASH TRANSACTION: Transaction ID returned: $transactionId")
                
                if (transactionId.isNotEmpty()) {
                    cashTransactionMessage = "Cash transaction saved successfully!"
                    showCashDialog = false
                    
                    // Refresh customer data to show updated balance
                    viewModel.loadCustomers()
                    
                    // Refresh transaction list to show the new cash transaction
                    viewModel.loadCustomerTransactions(updatedCustomer.customerId)
                } else {
                    cashTransactionError = "Failed to save cash transaction"
                }
            } catch (e: Exception) {
                cashTransactionError = "Error saving cash transaction: ${e.message}"
                println("❌ CASH TRANSACTION: Error saving: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 0.dp,
                backgroundColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFFB8973D),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFFF8E1),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = updatedCustomer.name.firstOrNull()?.uppercase() ?: "?",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB8973D)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = updatedCustomer.name.ifBlank { "Unnamed Customer" },
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1A1A)
                                    )
                                    Text(
                                        text = "Transaction History",
                                        fontSize = 14.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Current Balance",
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF),
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cash Amount Button (Small)
                                Button(
                                    onClick = { showCashDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFFB8973D),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = "💰 Cash",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Green balance Surface
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (updatedCustomer.balance >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = CurrencyFormatter.formatRupees(updatedCustomer.balance, includeDecimals = true),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (updatedCustomer.balance >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color(0xFFE5E7EB))

                    // Customer Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (updatedCustomer.email.isNotBlank() || updatedCustomer.phone.isNotBlank()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (updatedCustomer.email.isNotBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "📧", fontSize = 16.sp)
                                        Text(
                                            text = updatedCustomer.email,
                                            fontSize = 14.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }
                                if (updatedCustomer.phone.isNotBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "📱", fontSize = 16.sp)
                                        Text(
                                            text = updatedCustomer.phone,
                                            fontSize = 14.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "📅", fontSize = 16.sp)
                                Text(
                                    text = "Joined ${dateFormat.format(Date(updatedCustomer.createdAt))}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error message
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFFEE2E2),
                    elevation = 0.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 20.sp)
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // PDF Generation Messages
            pdfGenerationError?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFFEE2E2),
                    elevation = 0.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            pdfGenerationMessage?.let { successMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFDCFCE7),
                    elevation = 0.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = successMessage,
                            color = Color(0xFF16A34A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Cash Transaction Messages
            cashTransactionError?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFFEE2E2),
                    elevation = 0.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            cashTransactionMessage?.let { successMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFDCFCE7),
                    elevation = 0.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = successMessage,
                            color = Color(0xFF16A34A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Content
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFB8973D),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                if (transactions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        elevation = 0.dp,
                        backgroundColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFF3F4F6),
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Receipt,
                                            contentDescription = "No transactions",
                                            modifier = Modifier.size(40.dp),
                                            tint = Color(0xFF9CA3AF)
                                        )
                                    }
                                }
                                Text(
                                    text = "No transactions found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4B5563),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "This customer hasn't made any purchases yet",
                                    fontSize = 14.sp,
                                    color = Color(0xFF9CA3AF),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📜 Transactions",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF8E1)
                            ) {
                                Text(
                                    text = "${transactions.size} transactions",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 13.sp,
                                    color = Color(0xFFB8973D),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(transactions) { transaction ->
                                UnifiedTransactionCard(
                                    transaction = transaction,
                                    customer = updatedCustomer,
                                    onGenerateBill = { 
                                        if (transaction.transactionType == TransactionType.ORDER) {
                                            // Convert back to Order for PDF generation
                                            val order = Order(
                                                orderId = transaction.orderId ?: "",
                                                customerId = transaction.customerId,
                                                paymentSplit = transaction.paymentSplit,
                                                totalProductValue = transaction.subtotal,
                                                discountAmount = transaction.discountAmount,
                                                discountPercent = transaction.discountPercent.takeIf { it > 0 },
                                                gstAmount = transaction.gstAmount,
                                                gstPercentage = if (transaction.taxableAmount > 0 && transaction.gstAmount > 0) {
                                                    (transaction.gstAmount / transaction.taxableAmount) * 100.0
                                                } else 0.0,
                                                totalAmount = transaction.totalAmount,
                                                isGstIncluded = transaction.isGstIncluded,
                                                items = transaction.items,
                                                createdAt = transaction.createdAt,
                                                updatedAt = transaction.updatedAt,
                                                transactionDate = transaction.transactionDate,
                                                notes = transaction.notes
                                            )
                                            generateOrderPDF(order, updatedCustomer)
                                        }
                                    },
                                    isGeneratingPDF = generatingOrderIds.contains(transaction.orderId)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cash Amount Dialog
        if (showCashDialog) {
            CashAmountDialog(
                customer = updatedCustomer,
                onDismiss = { showCashDialog = false },
                onSave = { cashAmount ->
                    saveCashTransaction(cashAmount)
                }
            )
        }
    }
}

@Composable
fun TransactionCard(
    order: Order,
    customer: User,
    onGenerateBill: () -> Unit,
    isGeneratingPDF: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Order #${order.orderId}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )

                    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                    Text(
                        text = dateFormat.format(Date(order.createdAt)),
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )

                    Text(
                        text = "${order.items.size} items",
                        fontSize = 13.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF8E1)
                    ) {
                        Text(
                            text = CurrencyFormatter.formatRupees(order.totalAmount, includeDecimals = true),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB8973D)
                        )
                    }

                    if (order.paymentSplit != null) {
                        val dueAmount = order.paymentSplit.dueAmount
                        if (dueAmount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = "Due: ${CurrencyFormatter.formatRupees(dueAmount, includeDecimals = true)}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Generate Bill Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onGenerateBill,
                    enabled = !isGeneratingPDF,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFB8973D),
                        disabledBackgroundColor = Color(0xFF9CA3AF)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (isGeneratingPDF) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generating...",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Generate Bill",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Bill",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Status and PaymentStatus removed from Order model

            // Notes if available
            if (order.notes.isNotBlank()) {
                Divider(color = Color(0xFFE5E7EB))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "📝", fontSize = 16.sp)
                    Text(
                        text = order.notes,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun UnifiedTransactionCard(
    transaction: UnifiedTransaction,
    customer: User,
    onGenerateBill: () -> Unit,
    isGeneratingPDF: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with transaction type indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Transaction type indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (transaction.transactionType) {
                            TransactionType.ORDER -> Color(0xFFE3F2FD)
                            TransactionType.CASH -> Color(0xFFFFF8E1)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (transaction.transactionType) {
                                    TransactionType.ORDER -> "🛍️"
                                    TransactionType.CASH -> "💰"
                                },
                                fontSize = 12.sp
                            )
                            Text(
                                text = when (transaction.transactionType) {
                                    TransactionType.ORDER -> "ORDER"
                                    TransactionType.CASH -> "CASH"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (transaction.transactionType) {
                                    TransactionType.ORDER -> Color(0xFF1976D2)
                                    TransactionType.CASH -> Color(0xFFB8973D)
                                }
                            )
                        }
                    }
                    
                    Text(
                        text = when (transaction.transactionType) {
                            TransactionType.ORDER -> transaction.orderId ?: "Unknown Order"
                            TransactionType.CASH -> transaction.cashAmountId ?: "Unknown Transaction"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                }

                Text(
                    text = CurrencyFormatter.formatRupees(transaction.finalAmount, includeDecimals = true),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.transactionType == TransactionType.CASH && transaction.cashTransactionType != null) {
                        when (transaction.cashTransactionType) {
                            CashTransactionType.GIVE -> Color(0xFFDC2626)
                            CashTransactionType.RECEIVE -> Color(0xFF16A34A)
                        }
                    } else {
                        Color(0xFFB8973D)
                    }
                )
            }

            // Transaction details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = transaction.transactionDate,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                    
                    if (transaction.transactionType == TransactionType.CASH && transaction.cashTransactionType != null) {
                        Text(
                            text = when (transaction.cashTransactionType) {
                                CashTransactionType.GIVE -> "💸 Cash Given"
                                CashTransactionType.RECEIVE -> "💰 Cash Received"
                            },
                            fontSize = 12.sp,
                            color = when (transaction.cashTransactionType) {
                                CashTransactionType.GIVE -> Color(0xFFDC2626)
                                CashTransactionType.RECEIVE -> Color(0xFF16A34A)
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (transaction.paymentSplit != null) {
                    val dueAmount = transaction.paymentSplit.dueAmount
                    if (dueAmount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "Due: ${CurrencyFormatter.formatRupees(dueAmount, includeDecimals = true)}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Generate Bill Button (only for orders)
            if (transaction.transactionType == TransactionType.ORDER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onGenerateBill,
                        enabled = !isGeneratingPDF,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFB8973D),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        if (isGeneratingPDF) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generating...",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Generate Bill",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generate Bill",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Status badges - REMOVED as requested
            // Row(
            //     horizontalArrangement = Arrangement.spacedBy(8.dp),
            //     modifier = Modifier.fillMaxWidth()
            // ) {
            //     StatusChip(
            //         text = transaction.status.name,
            //         color = when (transaction.status) {
            //             OrderStatus.CONFIRMED -> Color(0xFF10B981)
            //             OrderStatus.PROCESSING -> Color(0xFF3B82F6)
            //             OrderStatus.SHIPPED -> Color(0xFF8B5CF6)
            //             OrderStatus.DELIVERED -> Color(0xFF059669)
            //             OrderStatus.CANCELLED -> Color(0xFF6B7280)
            //         }
            //     )
            //     
            //     StatusChip(
            //         text = transaction.paymentStatus.name,
            //         color = when (transaction.paymentStatus) {
            //             PaymentStatus.PENDING -> Color(0xFFF59E0B)
            //             PaymentStatus.PROCESSING -> Color(0xFF3B82F6)
            //             PaymentStatus.COMPLETED -> Color(0xFF10B981)
            //             PaymentStatus.FAILED -> Color(0xFFEF4444)
            //             PaymentStatus.CANCELLED -> Color(0xFF6B7280)
            //             PaymentStatus.REFUNDED -> Color(0xFFA855F7)
            //         }
            //     )
            // }

            // Notes if available
            if (transaction.notes.isNotBlank()) {
                Divider(color = Color(0xFFE5E7EB))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "📝", fontSize = 16.sp)
                    Text(
                        text = transaction.notes,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}