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
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

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

            cart.items.forEach { cartItem ->
                val currentProduct = products.find { it.id == cartItem.productId }
                if (currentProduct == null) {
                    errors.add("${cartItem.product.name} is no longer available")
                } else if (cartItem.quantity > currentProduct.quantity) {
                    errors.add("${cartItem.product.name}: Only ${currentProduct.quantity} available")
                }
            }

            onValidationResult(errors.isEmpty(), errors)
        }
    }

    private suspend fun reduceStockAfterOrder(order: Order, productRepository: ProductRepository) {
        order.items.forEach { cartItem ->
            try {
                val product = productRepository.getProductById(cartItem.productId)
                if (product != null) {
                    // Reduce quantity
                    val newQuantity = maxOf(0, product.quantity - cartItem.quantity)
                    
                    // For each sold item, nullify one barcode ID
                    val updatedBarcodeIds = product.barcodeIds.toMutableList()
                    val itemsToSell = minOf(cartItem.quantity, updatedBarcodeIds.size)
                    
                    // Remove barcode IDs for sold items (set to null by removing from list)
                    repeat(itemsToSell) {
                        if (updatedBarcodeIds.isNotEmpty()) {
                            updatedBarcodeIds.removeAt(0) // Remove first barcode ID
                        }
                    }
                    
                    // Set barcodeIds to empty string if all items are sold (for Firestore compatibility)
                    val finalBarcodeIds = if (updatedBarcodeIds.isEmpty()) {
                        emptyList() // Will be stored as empty string in Firestore
                    } else {
                        updatedBarcodeIds
                    }
                    
                    val updatedProduct = product.copy(
                        quantity = newQuantity,
                        barcodeIds = finalBarcodeIds
                    )
                    productRepository.updateProduct(updatedProduct)
                    
                    println("🛒 Order Processing: Updated product ${product.name}")
                    println("   - Original quantity: ${product.quantity}, New quantity: $newQuantity")
                    println("   - Original barcodes: ${product.barcodeIds.size}, New barcodes: ${updatedBarcodeIds.size}")
                    println("   - Items sold: $itemsToSell")
                }
            } catch (e: Exception) {
                println("❌ Error processing product ${cartItem.productId}: ${e.message}")
            }
        }
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
                
                val order = Order(
                    id = generateOrderId(),
                    customerId = cart.customerId,
                    paymentMethod = paymentMethod,
                    paymentSplit = paymentSplit,
                    subtotal = subtotal,
                    discountAmount = discountAmount,
                    gstAmount = gst,
                    gstRate = gstRate,
                    totalAmount = finalTotal,
                    items = cart.items,
                    timestamp = System.currentTimeMillis(),
                    status = OrderStatus.CONFIRMED
                )

                // Save order to database
                val success = paymentRepository.saveOrder(order)

                if (success) {
                    // Reduce stock quantities
                    val productRepository = JewelryAppInitializer.getViewModel().repository
                    reduceStockAfterOrder(order, productRepository)

                    // Refresh product list to update UI
                    JewelryAppInitializer.getViewModel().loadProducts()

                    // Create transaction record
                    _lastTransaction.value = PaymentTransaction(
                        id = order.id,
                        cartId = order.customerId,
                        paymentMethod = order.paymentMethod,
                        paymentSplit = order.paymentSplit,
                        subtotal = order.subtotal,
                        discountAmount = order.discountAmount,
                        gstAmount = order.gstAmount,
                        gstRate = order.gstRate,
                        totalAmount = order.totalAmount,
                        items = order.items,
                        timestamp = order.timestamp,
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

                val pdfFileName = "Invoice_${order.id}_${System.currentTimeMillis()}.pdf"
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
        val order = Order(
            id = transaction.id,
            customerId = transaction.cartId,
            paymentMethod = transaction.paymentMethod,
            paymentSplit = transaction.paymentSplit,
            subtotal = transaction.subtotal,
            discountAmount = transaction.discountAmount,
            gstAmount = transaction.gstAmount,
            gstRate = transaction.gstRate,
            totalAmount = transaction.totalAmount,
            items = transaction.items,
            timestamp = transaction.timestamp,
            status = OrderStatus.CONFIRMED
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

                val order = Order(
                    id = generateOrderId(),
                    customerId = cart.customerId,
                    paymentMethod = _selectedPaymentMethod.value!!,
                    subtotal = dynamicSubtotal,
                    discountAmount = discountAmount,
                    gstAmount = dynamicGst,
                    totalAmount = dynamicTotal,
                    items = cart.items,
                    timestamp = System.currentTimeMillis(),
                    status = OrderStatus.CONFIRMED
                )

                // Save order to database
                val success = paymentRepository.saveOrder(order)

                if (success) {
                    // Reduce stock quantities
                    val productRepository = JewelryAppInitializer.getViewModel().repository
                    reduceStockAfterOrder(order, productRepository)

                    // Refresh product list to update UI
                    JewelryAppInitializer.getViewModel().loadProducts()

                    _lastTransaction.value = PaymentTransaction(
                        id = order.id,
                        cartId = order.customerId,
                        paymentMethod = order.paymentMethod,
                        subtotal = order.subtotal,
                        discountAmount = order.discountAmount,
                        gstAmount = order.gstAmount,
                        totalAmount = order.totalAmount,
                        items = order.items,
                        timestamp = order.timestamp,
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
}