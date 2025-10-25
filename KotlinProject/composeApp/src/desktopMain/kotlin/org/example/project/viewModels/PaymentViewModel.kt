package org.example.project.viewModels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.example.project.JewelryAppInitializer
import org.example.project.data.*
import org.example.project.utils.PdfGeneratorService
import org.example.project.data.MetalRatesManager
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class PaymentViewModel(
    private val paymentRepository: PaymentRepository
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Lazy initialization to avoid loading at startup
    private val pdfGeneratorService by lazy { PdfGeneratorService() }

    // Payment Method State
    private val _selectedPaymentMethod = mutableStateOf<PaymentMethod?>(null)
    val selectedPaymentMethod: State<PaymentMethod?> = _selectedPaymentMethod

    // Discount State
    private val _discountType = mutableStateOf(DiscountType.AMOUNT)
    val discountType: State<DiscountType> = _discountType

    private val _discountValue = mutableStateOf("")
    val discountValue: State<String> = _discountValue

    private val _discountAmount = mutableStateOf(0.0)
    val discountAmount: State<Double> = _discountAmount

    // GST State
    private val _isGstIncluded = mutableStateOf(true)
    val isGstIncluded: State<Boolean> = _isGstIncluded

    // Loading and Error States
    private val _isProcessing = mutableStateOf(false)
    val isProcessing: State<Boolean> = _isProcessing

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _successMessage = mutableStateOf<String?>(null)
    val successMessage: State<String?> = _successMessage

    // Transaction State
    private val _lastTransaction = mutableStateOf<PaymentTransaction?>(null)
    val lastTransaction: State<PaymentTransaction?> = _lastTransaction

    // PDF Generation State
    private val _isGeneratingPDF = mutableStateOf(false)
    val isGeneratingPDF: State<Boolean> = _isGeneratingPDF

    private val _pdfPath = mutableStateOf<String?>(null)
    val pdfPath: State<String?> = _pdfPath

    fun setPaymentMethod(paymentMethod: PaymentMethod) {
        // Toggle selection: if already selected, deselect it
        if (_selectedPaymentMethod.value == paymentMethod) {
            _selectedPaymentMethod.value = null
        } else {
            _selectedPaymentMethod.value = paymentMethod
        }
        _errorMessage.value = null
    }

    fun setDiscountType(discountType: DiscountType) {
        _discountType.value = discountType
        _discountValue.value = ""
        _discountAmount.value = 0.0
        _errorMessage.value = null
    }

    fun setIsGstIncluded(isGstIncluded: Boolean) {
        _isGstIncluded.value = isGstIncluded
        _errorMessage.value = null
    }

    fun validateStockBeforeOrder(
        cart: Cart,
        products: List<Product>,
        onValidationResult: (Boolean, List<String>) -> Unit
    ) {
        viewModelScope.launch {
            val errors = mutableListOf<String>()
            val inventoryRepository = JewelryAppInitializer.getInventoryRepository()

            cart.items.forEach { cartItem ->
                val currentProduct = products.find { it.id == cartItem.productId }
                if (currentProduct == null) {
                    errors.add("${cartItem.product.name} is no longer available")
                } else {
                    // Check inventory-based availability instead of product.quantity
                    val availableInventoryCount = if (inventoryRepository != null) {
                        inventoryRepository.getAvailableInventoryItemsByProductId(cartItem.productId).size
                    } else {
                        currentProduct.quantity // Fallback to product quantity if inventory repository is not available
                    }
                    
                    println("🔍 STOCK VALIDATION for ${cartItem.product.name}")
                    println("   - Cart quantity: ${cartItem.quantity}")
                    println("   - Available inventory count: $availableInventoryCount")
                    println("   - Product quantity (old): ${currentProduct.quantity}")
                    
                    if (cartItem.quantity > availableInventoryCount) {
                        errors.add("${cartItem.product.name}: Only $availableInventoryCount available in inventory")
                    }
                }
            }

            println("🔍 STOCK VALIDATION RESULT")
            println("   - Errors count: ${errors.size}")
            println("   - Errors: $errors")
            println("   - Validation passed: ${errors.isEmpty()}")

            onValidationResult(errors.isEmpty(), errors)
        }
    }

    private suspend fun reduceStockAfterOrder(order: Order, productRepository: ProductRepository) {
        val inventoryRepository = JewelryAppInitializer.getInventoryRepository()
        
        order.items.forEach { orderItem ->
            try {
                println("🛒 Processing order item")
                println("   - Product ID: ${orderItem.productId}")
                println("   - Barcode ID: ${orderItem.barcodeId}")
                println("   - Quantity: ${orderItem.quantity}")
                
                // Delete the barcode document from inventory collection
                try {
                    println("   - Deleting barcode: ${orderItem.barcodeId}")
                    val inventoryItem = inventoryRepository?.getInventoryItemByBarcodeId(orderItem.barcodeId)
                    if (inventoryItem != null) {
                        val success = inventoryRepository.deleteInventoryItem(inventoryItem.id)
                        if (success) {
                            println("   - ✅ Barcode ${orderItem.barcodeId} deleted successfully")
                        } else {
                            println("   - ❌ Failed to delete barcode ${orderItem.barcodeId}")
                        }
                    } else {
                        println("   - ⚠️ Inventory item not found for barcode: ${orderItem.barcodeId}")
                    }
                } catch (e: Exception) {
                    println("   - ❌ Error deleting barcode ${orderItem.barcodeId}: ${e.message}")
                }
                
                println("🛒 Order Processing: Completed processing order item")
                println("   - Deleted barcode document: ${orderItem.barcodeId}")
                println("   - Product document remains unchanged")
                
            } catch (e: Exception) {
                println("❌ Error processing order item ${orderItem.productId}: ${e.message}")
            }
        }
        
        // Trigger inventory refresh to update dashboard quantities
        JewelryAppInitializer.getViewModel().triggerInventoryRefresh()
    }

    fun setDiscountValue(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*\$"))) {
            _discountValue.value = value
            _errorMessage.value = null
        }
    }

    fun applyDiscount() {
        val value = _discountValue.value.toDoubleOrNull()

        if (value == null || value <= 0) {
            _errorMessage.value = "Please enter a valid discount value"
            return
        }

        when (_discountType.value) {
            DiscountType.PERCENTAGE -> {
                if (value > 100) {
                    _errorMessage.value = "Percentage discount cannot exceed 100%"
                    return
                }
                _discountAmount.value = value
            }
            DiscountType.AMOUNT -> {
                _discountAmount.value = value
            }
            DiscountType.TOTAL_PAYABLE -> {
                // This will be handled in calculateDiscountAmount
                _discountAmount.value = value
            }
        }

        _errorMessage.value = null
    }

    fun calculateDiscountAmount(subtotal: Double, gstAmount: Double = 0.0): Double {
        return when (_discountType.value) {
            DiscountType.PERCENTAGE -> {
                subtotal * (_discountAmount.value / 100)
            }
            DiscountType.AMOUNT -> {
                minOf(_discountAmount.value, subtotal)
            }
            DiscountType.TOTAL_PAYABLE -> {
                // Calculate discount as: subtotal + GST - totalPayable
                // If totalPayable is greater than subtotal + GST, return 0 (no discount)
                val subtotalWithGst = subtotal + gstAmount
                maxOf(0.0, subtotalWithGst - _discountAmount.value)
            }
        }
    }

    fun saveOrderWithPaymentMethod(
        cart: Cart,
        subtotal: Double,
        discountAmount: Double,
        gst: Double,
        finalTotal: Double,
        paymentSplit: PaymentSplit? = null,
        onSuccess: () -> Unit
    ) {
        // Allow order processing if either a payment method is selected OR a split payment is configured
        if (_selectedPaymentMethod.value == null && paymentSplit == null) {
            _errorMessage.value = "Please select a payment method"
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null

            try {
                // Determine payment method: use selected method or default to CASH if split payment is configured
                val paymentMethod = _selectedPaymentMethod.value ?: PaymentMethod.CASH
                
                val gstRate = if (subtotal > 0) gst / subtotal else 0.0
                
                // Convert CartItem objects to OrderItem objects (only store barcodeId and productId)
                val orderItems = cart.items.flatMap { cartItem ->
                    cartItem.selectedBarcodeIds.map { barcodeId ->
                        OrderItem(
                            productId = cartItem.productId,
                            barcodeId = barcodeId,
                            quantity = 1 // Each barcode represents 1 item
                        )
                    }
                }
                
                println("📦 CREATING ORDER WITH SIMPLIFIED ITEMS")
                println("   - Cart items count: ${cart.items.size}")
                println("   - Order items count: ${orderItems.size}")
                orderItems.forEach { orderItem ->
                    println("   - OrderItem: productId=${orderItem.productId}, barcodeId=${orderItem.barcodeId}")
                }
                
                // Get customer information
                val customer = JewelryAppInitializer.getCustomerViewModel().selectedCustomer.value

                // Calculate taxable amount (subtotal - discount amount)
                val taxableAmount = subtotal - discountAmount

                // Format transaction date
                val dateFormat = SimpleDateFormat("dd MMMM yyyy 'at' HH:mm:ss z", Locale.getDefault())
                val transactionDate = dateFormat.format(Date())

                // Get metal rates reference - this should point to the actual rates collection
                val metalRatesReference = getCurrentMetalRatesReference()

                println("📦 ORDER CREATION DETAILS")
                println("   - Customer ID: ${cart.customerId}")
                println("   - Subtotal: $subtotal")
                println("   - Discount Amount: $discountAmount")
                println("   - Taxable Amount: $taxableAmount")
                println("   - GST Amount: $gst")
                println("   - Final Amount: $finalTotal")
                println("   - Transaction Date: $transactionDate")
                println("   - Metal Rates Reference: $metalRatesReference")

                val order = Order(
                    orderId = generateOrderId(),
                    customerId = cart.customerId, // Reference to users collection
                    paymentSplit = paymentSplit,
                    subtotal = subtotal,
                    discountAmount = discountAmount,
                    taxableAmount = taxableAmount, // Set taxable amount
                    gstAmount = gst,
                    totalAmount = finalTotal,
                    finalAmount = finalTotal, // Set final amount
                    items = orderItems, // Simplified references for Firestore storage
                    metalRatesReference = metalRatesReference, // Reference to rates collection
                    transactionDate = transactionDate, // Set transaction date
                    createdAt = System.currentTimeMillis(),
                    paymentStatus = PaymentStatus.COMPLETED
                )

                // Save order to database
                val success = paymentRepository.saveOrder(order)

                if (success) {
                    // Reduce stock quantities
                    val productRepository = JewelryAppInitializer.getViewModel().repository
                    reduceStockAfterOrder(order, productRepository)

                    // Update customer balance with due amount
                    updateCustomerBalance(order.customerId, order.paymentSplit?.dueAmount ?: 0.0)

                    // Refresh product list to update UI
                    JewelryAppInitializer.getViewModel().loadProducts()

                    // Create transaction record
                    _lastTransaction.value = PaymentTransaction(
                        id = order.orderId,
                        cartId = order.customerId,
                        paymentMethod = PaymentMethod.CASH, // Default payment method
                        paymentSplit = order.paymentSplit,
                        subtotal = order.subtotal,
                        discountAmount = order.discountAmount,
                        gstAmount = order.gstAmount,
                        totalAmount = order.totalAmount,
                        items = cart.items, // Use CartItem objects for receipt display
                        timestamp = order.createdAt,
                        status = PaymentStatus.COMPLETED
                    )

                    // ✅ FIX: Generate PDF after successful order creation
                    val customer = JewelryAppInitializer.getCustomerViewModel().selectedCustomer.value
                    if (customer != null) {
                        generateOrderPDF(order, customer)
                    } else {
                        // Create a default customer if none selected
                        val defaultCustomer = User(
                            id = "default",
                            name = "Walk-in Customer",
                            email = "",
                            phone = "",
                            address = ""
                        )
                        generateOrderPDF(order, defaultCustomer)
                    }

                    onSuccess()
                } else {
                    _errorMessage.value = "Failed to save order. Please try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving order: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    private fun generateOrderPDF(order: Order, customer: User) {
        viewModelScope.launch {
            _isGeneratingPDF.value = true
            _errorMessage.value = null
            
            try {
                val userHome = System.getProperty("user.home")
                if (userHome.isNullOrBlank()) {
                    throw Exception("User home directory not found")
                }
                
                val billsDirectory = File(userHome, "JewelryBills")
                if (!billsDirectory.exists()) {
                    billsDirectory.mkdirs()
                }
                
                if (!billsDirectory.canWrite()) {
                    throw Exception("Cannot write to bills directory: ${billsDirectory.absolutePath}")
                }

                val pdfFileName = "Invoice_${order.orderId}_${System.currentTimeMillis()}.pdf"
                val pdfOutputPath = File(billsDirectory, pdfFileName)

                // Fetch invoice configuration directly; fall back to defaults if not found
                val invoiceConfig = org.example.project.data.InvoiceConfigRepository()
                    .getInvoiceConfig() ?: org.example.project.data.InvoiceConfig()
                
                val pdfResult = pdfGeneratorService.generateInvoicePDF(
                    order = order,
                    customer = customer,
                    outputFile = pdfOutputPath,
                    invoiceConfig = invoiceConfig
                )

                if (pdfResult.isSuccess) {
                    val pdfFile = pdfResult.getOrThrow()
                    if (pdfFile.exists() && pdfFile.length() > 0) {
                        _pdfPath.value = pdfFile.absolutePath
                        
                        // Small delay to ensure file is fully written
                        kotlinx.coroutines.delay(500)
                        
                        // Try to open PDF with multiple methods
                        var opened = false
                        
                        // Method 1: Desktop.open()
                        try {
                            val desktop = java.awt.Desktop.getDesktop()
                            if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                                desktop.open(pdfFile)
                                opened = true
                                _successMessage.value = "PDF invoice generated and opened successfully!"
                            }
                        } catch (e: Exception) {
                            // Method 1 failed, try Method 2
                        }
                        
                        // Method 2: Runtime.exec() with system default
                        if (!opened) {
                            try {
                                val os = System.getProperty("os.name").lowercase()
                                when {
                                    os.contains("mac") -> {
                                        Runtime.getRuntime().exec(arrayOf("open", pdfFile.absolutePath))
                                        opened = true
                                    }
                                    os.contains("win") -> {
                                        Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", pdfFile.absolutePath))
                                        opened = true
                                    }
                                    os.contains("nix") || os.contains("nux") -> {
                                        Runtime.getRuntime().exec(arrayOf("xdg-open", pdfFile.absolutePath))
                                        opened = true
                                    }
                                }
                                if (opened) {
                                    _successMessage.value = "PDF invoice generated and opened successfully!"
                                }
                            } catch (e: Exception) {
                                // Method 2 failed
                            }
                        }
                        
                        // If both methods failed
                        if (!opened) {
                            _successMessage.value = "PDF invoice generated successfully! File saved at: ${pdfFile.absolutePath}"
                        }
                    } else {
                        _errorMessage.value = "PDF file was not created properly"
                    }
                } else {
                    _errorMessage.value = "Failed to generate PDF invoice: ${pdfResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "PDF generation failed: ${e.message}"
            } finally {
                _isGeneratingPDF.value = false
            }
        }
    }

    // Public helper: generate PDF for the last successful transaction
    fun generatePdfForLastTransaction(customer: User) {
        val transaction = _lastTransaction.value ?: return
        // Calculate taxable amount (subtotal - discount amount)
        val taxableAmount = transaction.subtotal - transaction.discountAmount
        
        // Format transaction date
        val dateFormat = SimpleDateFormat("dd MMMM yyyy 'at' HH:mm:ss z", Locale.getDefault())
        val transactionDate = dateFormat.format(Date(transaction.timestamp))
        
        val order = Order(
            orderId = transaction.id,
            customerId = transaction.cartId, // Reference to users collection
            paymentSplit = transaction.paymentSplit,
            subtotal = transaction.subtotal,
            discountAmount = transaction.discountAmount,
            taxableAmount = taxableAmount, // Set taxable amount
            gstAmount = transaction.gstAmount,
            totalAmount = transaction.totalAmount,
            finalAmount = transaction.totalAmount, // Set final amount
            items = transaction.items.map { cartItem ->
                // Convert CartItem to OrderItem for PDF generation
                OrderItem(
                    productId = cartItem.productId,
                    barcodeId = cartItem.selectedBarcodeIds.firstOrNull() ?: "",
                    quantity = cartItem.quantity
                )
            },
            metalRatesReference = getCurrentMetalRatesReference(), // Reference to rates collection
            transactionDate = transactionDate, // Set transaction date
            createdAt = transaction.timestamp,
            paymentStatus = PaymentStatus.COMPLETED
        )
        generateOrderPDF(order, customer)
    }

    fun runPDFDiagnostics() {
        viewModelScope.launch {
            _isGeneratingPDF.value = true
            _errorMessage.value = null
            
            try {
                val diagnostics = org.example.project.utils.PDFDiagnostics.runComprehensiveDiagnostics()
                
                if (diagnostics) {
                    _successMessage.value = "PDF diagnostics completed successfully. All systems are working properly."
                } else {
                    _errorMessage.value = "PDF diagnostics found issues. Check console logs for details."
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "PDF diagnostics failed: ${e.message}"
            } finally {
                _isGeneratingPDF.value = false
            }
        }
    }
    
    fun downloadBill() {
        _pdfPath.value?.let { path ->
            try {
                val sourceFile = File(path)
                if (sourceFile.exists()) {
                    val userHome = System.getProperty("user.home")
                    val downloadsDir = File(userHome, "Downloads")
                    
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    
                    val fileName = sourceFile.name
                    val destinationFile = File(downloadsDir, fileName)
                    
                    sourceFile.copyTo(destinationFile, overwrite = true)
                    
                    val isHtmlFile = fileName.endsWith(".html", ignoreCase = true)
                    val fileType = if (isHtmlFile) "HTML invoice" else "PDF bill"
                    _successMessage.value = "$fileType downloaded successfully to Downloads folder"
                    _errorMessage.value = null
                    
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(downloadsDir)
                    }
                } else {
                    _errorMessage.value = "Bill file does not exist"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error downloading bill: ${e.message}"
            }
        }
    }

    private fun generateOrderId(): String {
        return "ORD_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    fun clearDiscount() {
        _discountValue.value = ""
        _discountAmount.value = 0.0
        _errorMessage.value = null
    }

    fun clearPaymentMethod() {
        _selectedPaymentMethod.value = null
    }

    fun resetPaymentState() {
        _selectedPaymentMethod.value = null
        _discountType.value = DiscountType.AMOUNT
        _discountValue.value = ""
        _discountAmount.value = 0.0
        _isProcessing.value = false
        _errorMessage.value = null
        _lastTransaction.value = null
        _isGeneratingPDF.value = false
        _pdfPath.value = null
    }

    fun onCleared() {
        viewModelScope.cancel()
    }

    // Add these functions to your existing PaymentViewModel class

    // Helper function to calculate dynamic prices based on metal prices
    private fun calculateDynamicPrices(
        cart: Cart,
        goldPrice: Double,
        silverPrice: Double
    ): Triple<Double, Double, Double> {
        val subtotal = cart.items.sumOf { cartItem ->
            val pricePerGram = when {
                cartItem.product.materialType.contains("gold", ignoreCase = true) -> goldPrice
                cartItem.product.materialType.contains("silver", ignoreCase = true) -> silverPrice
                else -> goldPrice
            }
            val productWeight = parseWeight(cartItem.product.weight)
            val actualWeight = if (cartItem.selectedWeight > 0) cartItem.selectedWeight else productWeight
            actualWeight * cartItem.quantity * pricePerGram
        }

        val gst = subtotal * 0.18
        val subtotalWithGst = subtotal + gst
        val discountAmount = calculateDiscountAmount(subtotalWithGst, gst)
        val total = subtotalWithGst - discountAmount

        return Triple(subtotal, gst, total)
    }

    // Utility function to parse weight from string
    private fun parseWeight(weightStr: String): Double {
        return try {
            weightStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    // New overloaded function that accepts metal prices and calculates dynamic pricing
    fun saveOrderWithPaymentMethod(
        cart: Cart,
        goldPrice: Double,
        silverPrice: Double,
        onSuccess: () -> Unit
    ) {
        if (_selectedPaymentMethod.value == null) {
            _errorMessage.value = "Please select a payment method"
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null

            try {
                // Calculate dynamic prices based on current metal prices
                val (dynamicSubtotal, dynamicGst, dynamicTotal) = calculateDynamicPrices(cart, goldPrice, silverPrice)
                val discountAmount = calculateDiscountAmount(dynamicSubtotal, dynamicGst)

                // Convert CartItem objects to OrderItem objects (only store barcodeId and productId)
                val orderItems = cart.items.flatMap { cartItem ->
                    cartItem.selectedBarcodeIds.map { barcodeId ->
                        OrderItem(
                            productId = cartItem.productId,
                            barcodeId = barcodeId,
                            quantity = 1 // Each barcode represents 1 item
                        )
                    }
                }
                
                // Get customer information
                val customer = JewelryAppInitializer.getCustomerViewModel().selectedCustomer.value

                // Calculate taxable amount (subtotal - discount amount)
                val taxableAmount = dynamicSubtotal - discountAmount

                // Format transaction date
                val dateFormat = SimpleDateFormat("dd MMMM yyyy 'at' HH:mm:ss z", Locale.getDefault())
                val transactionDate = dateFormat.format(Date())

                val order = Order(
                    orderId = generateOrderId(),
                    customerId = cart.customerId, // Reference to users collection
                    subtotal = dynamicSubtotal,
                    discountAmount = discountAmount,
                    taxableAmount = taxableAmount, // Set taxable amount
                    gstAmount = dynamicGst,
                    totalAmount = dynamicTotal,
                    finalAmount = dynamicTotal, // Set final amount
                    items = orderItems, // Simplified references for Firestore storage
                    metalRatesReference = getCurrentMetalRatesReference(), // Reference to rates collection
                    transactionDate = transactionDate, // Set transaction date
                    createdAt = System.currentTimeMillis(),
                    paymentStatus = PaymentStatus.COMPLETED
                )

                // Save order to database
                val success = paymentRepository.saveOrder(order)

                if (success) {
                    // Reduce stock quantities
                    val productRepository = JewelryAppInitializer.getViewModel().repository
                    reduceStockAfterOrder(order, productRepository)

                    // Update customer balance with due amount
                    updateCustomerBalance(order.customerId, order.paymentSplit?.dueAmount ?: 0.0)

                    // Refresh product list to update UI
                    JewelryAppInitializer.getViewModel().loadProducts()

                    _lastTransaction.value = PaymentTransaction(
                        id = order.orderId,
                        cartId = order.customerId,
                        paymentMethod = PaymentMethod.CASH, // Default payment method
                        subtotal = order.subtotal,
                        discountAmount = order.discountAmount,
                        gstAmount = order.gstAmount,
                        totalAmount = order.totalAmount,
                        items = cart.items, // Use CartItem objects for receipt display
                        timestamp = order.createdAt,
                        status = PaymentStatus.COMPLETED
                    )
                    onSuccess()
                } else {
                    _errorMessage.value = "Failed to save order. Please try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving order: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Updates customer balance with due amount after bill processing
     * This adds the final amount to the customer's outstanding balance in the users collection
     */
    private suspend fun updateCustomerBalance(customerId: String, dueAmount: Double) {
        try {
            println("💰 PAYMENT VIEWMODEL: Updating customer balance")
            println("   - Customer ID: $customerId")
            println("   - Due Amount: $dueAmount")
            
            val customerRepository = JewelryAppInitializer.getCustomerRepository()
            val success = customerRepository.addToCustomerBalance(customerId, dueAmount)
            
            if (success) {
                println("✅ Customer balance updated successfully")
            } else {
                println("❌ Failed to update customer balance")
            }
        } catch (e: Exception) {
            println("❌ Error updating customer balance: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Gets the current metal rates reference for orders
     * This should point to the actual rates collection document
     */
    private fun getCurrentMetalRatesReference(): String {
        // TODO: This needs to be updated based on the actual collection structure
        // For now, returning "current" but this should be the actual document ID
        // from the rates collection that contains the current metal rates
        return "current"
    }
}