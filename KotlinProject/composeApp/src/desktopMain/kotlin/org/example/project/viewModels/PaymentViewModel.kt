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
import org.example.project.utils.HtmlCssBillGenerator
import org.example.project.utils.HtmlToPdfBillGenerator
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class PaymentViewModel(
    private val paymentRepository: PaymentRepository
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Lazy initialization to avoid loading at startup
    private val htmlCssBillGenerator by lazy { HtmlCssBillGenerator() }
    private val htmlToPdfBillGenerator by lazy { HtmlToPdfBillGenerator() }

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
        println("📦 DEBUG: Starting stock reduction for order: ${order.id}")
        println("   - Order items count: ${order.items.size}")
        
        order.items.forEach { cartItem ->
            try {
                println("   🔄 Processing item: ${cartItem.product.name}")
                println("      - Product ID: ${cartItem.productId}")
                println("      - Cart quantity: ${cartItem.quantity}")
                
                val product = productRepository.getProductById(cartItem.productId)
                if (product != null) {
                    println("      - Current product quantity: ${product.quantity}")
                    val newQuantity = maxOf(0, product.quantity - cartItem.quantity)
                    println("      - New quantity after reduction: $newQuantity")
                    
                    val updatedProduct = product.copy(
                        quantity = newQuantity
                    )
                    productRepository.updateProduct(updatedProduct)
                    println("      - ✅ Successfully updated product quantity")
                } else {
                    println("      - ❌ Product not found in repository")
                }
            } catch (e: Exception) {
                println("      - ❌ Failed to reduce stock for product ${cartItem.productId}: ${e.message}")
            }
        }
        
        println("📦 DEBUG: Completed stock reduction for order: ${order.id}")
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
                
                val order = Order(
                    id = generateOrderId(),
                    customerId = cart.customerId,
                    paymentMethod = paymentMethod,
                    paymentSplit = paymentSplit,
                    subtotal = subtotal,
                    discountAmount = discountAmount,
                    gstAmount = gst,
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

                    // Create transaction record
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
            _errorMessage.value = null // Clear any previous errors
            
            println("🚀 ========== PDF GENERATION STARTED ==========")
            println("📋 Order ID: ${order.id}")
            println("👤 Customer: ${customer.name} (ID: ${customer.id})")
            println("💰 Order Total: ₹${String.format("%.2f", order.totalAmount)}")
            println("📦 Items Count: ${order.items.size}")
            println("💳 Payment Method: ${order.paymentMethod}")
            println("⏰ Timestamp: ${java.util.Date(order.timestamp)}")

            try {
                // Run system diagnostics
                runSystemDiagnostics()
                
                // Validate system environment
                val userHome = System.getProperty("user.home")
                println("🏠 User Home Directory: $userHome")
                
                if (userHome.isNullOrBlank()) {
                    throw Exception("User home directory not found")
                }
                
                val billsDirectory = File(userHome, "JewelryBills")
                println("📁 Bills Directory: ${billsDirectory.absolutePath}")
                
                if (!billsDirectory.exists()) {
                    val created = billsDirectory.mkdirs()
                    println("📁 Created bills directory: $created")
                }
                
                // Check directory permissions
                if (!billsDirectory.canWrite()) {
                    throw Exception("Cannot write to bills directory: ${billsDirectory.absolutePath}")
                }
                println("✅ Directory permissions verified")

                // First generate HTML/CSS files
                val htmlFileName = "Invoice_${order.id}_${System.currentTimeMillis()}.html"
                val htmlOutputPath = Paths.get(billsDirectory.absolutePath, htmlFileName)

                println("🔄 ========== STEP 1: HTML/CSS GENERATION ==========")
                println("📄 HTML output path: $htmlOutputPath")
                println("🔧 Using HtmlCssBillGenerator...")

                // Get invoice configuration
                val invoiceConfig = JewelryAppInitializer.getInvoiceConfigViewModel().invoiceConfig.value
                
                val htmlSuccess = htmlCssBillGenerator.generateHtmlBill(
                    order = order,
                    customer = customer,
                    outputPath = htmlOutputPath,
                    invoiceConfig = invoiceConfig
                )

                if (htmlSuccess) {
                    println("✅ HTML/CSS generation successful")
                    
                    // Verify HTML file was created
                    val htmlFile = htmlOutputPath.toFile()
                    if (htmlFile.exists() && htmlFile.length() > 0) {
                        println("✅ HTML file verified: ${htmlFile.length()} bytes")
                        
                        // Open HTML file in browser instead of generating PDF
                        try {
                            val desktop = java.awt.Desktop.getDesktop()
                            if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                                desktop.browse(htmlFile.toURI())
                                println("🌐 HTML invoice opened in browser: ${htmlFile.absolutePath}")
                                _pdfPath.value = htmlOutputPath.toString() // Store HTML path for download
                                _successMessage.value = "Invoice generated and opened in browser successfully!"
                            } else {
                                println("⚠️ Desktop browsing not supported, HTML file created at: ${htmlFile.absolutePath}")
                                _pdfPath.value = htmlOutputPath.toString()
                                _successMessage.value = "Invoice generated successfully! File saved at: ${htmlFile.absolutePath}"
                            }
                        } catch (e: Exception) {
                            println("⚠️ Could not open browser, but HTML file created: ${e.message}")
                            _pdfPath.value = htmlOutputPath.toString()
                            _successMessage.value = "Invoice generated successfully! File saved at: ${htmlFile.absolutePath}"
                        }
                    } else {
                        println("⚠️ HTML file verification failed")
                        _errorMessage.value = "HTML file was not created properly"
                    }
                } else {
                    println("❌ ========== HTML/CSS GENERATION FAILED ==========")
                    _errorMessage.value = "Failed to generate invoice HTML file"
                }
            } catch (e: Exception) {
                println("❌ ========== CRITICAL ERROR IN PDF GENERATION ==========")
                println("💥 Exception: ${e.javaClass.simpleName}")
                println("💥 Message: ${e.message}")
                println("💥 Stack trace:")
                e.printStackTrace()
                _errorMessage.value = "PDF generation failed: ${e.message}"
            } finally {
                _isGeneratingPDF.value = false
                println("🏁 ========== PDF GENERATION PROCESS COMPLETED ==========")
            }
        }
    }

    fun runPDFDiagnostics() {
        viewModelScope.launch {
            _isGeneratingPDF.value = true
            _errorMessage.value = null
            
            try {
                println("🔬 Running PDF diagnostics...")
                val diagnostics = org.example.project.utils.PDFDiagnostics.runComprehensiveDiagnostics()
                
                if (diagnostics) {
                    _successMessage.value = "PDF diagnostics completed successfully. All systems are working properly."
                } else {
                    _errorMessage.value = "PDF diagnostics found issues. Check console logs for details."
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "PDF diagnostics failed: ${e.message}"
                e.printStackTrace()
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
                    // Get user's Downloads directory
                    val userHome = System.getProperty("user.home")
                    val downloadsDir = File(userHome, "Downloads")
                    
                    // Ensure Downloads directory exists
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    
                    // Create destination file in Downloads folder
                    val fileName = sourceFile.name
                    val destinationFile = File(downloadsDir, fileName)
                    
                    // Copy file to Downloads directory
                    sourceFile.copyTo(destinationFile, overwrite = true)
                    
                    println("✅ Bill downloaded successfully to: ${destinationFile.absolutePath}")
                    _errorMessage.value = null
                    
                    // Check if it's an HTML file or PDF file
                    val isHtmlFile = fileName.endsWith(".html", ignoreCase = true)
                    val fileType = if (isHtmlFile) "HTML invoice" else "PDF bill"
                    _successMessage.value = "$fileType downloaded successfully to Downloads folder"
                    
                    // Optionally open the Downloads folder
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(downloadsDir)
                    }
                } else {
                    _errorMessage.value = "Bill file does not exist"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error downloading bill: ${e.message}"
                println("❌ Error downloading bill: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun tryAlternativePdfGeneration(
        order: Order,
        customer: User,
        outputPath: Path
    ): Boolean {
        return try {
            println("🔄 ========== ALTERNATIVE PDF GENERATION ==========")
            println("🔧 Using HtmlToPdfBillGenerator with fallback approach...")
            
            // Check if output directory exists and is writable
            val outputFile = outputPath.toFile()
            val outputDir = outputFile.parentFile
            
            if (!outputDir.exists()) {
                val created = outputDir.mkdirs()
                println("📁 Created output directory: $created")
            }
            
            if (!outputDir.canWrite()) {
                throw Exception("Cannot write to output directory: ${outputDir.absolutePath}")
            }
            
            // Use the HtmlToPdfBillGenerator with simplified approach
            val generator = HtmlToPdfBillGenerator()
            println("🔧 Generator created successfully")
            
            val success = generator.generatePdfBill(order, customer, outputPath)
            
            if (success) {
                println("✅ Alternative PDF generation successful")
                
                // Verify the generated file
                if (outputFile.exists() && outputFile.length() > 0) {
                    println("✅ Alternative PDF file verified: ${outputFile.length()} bytes")
                } else {
                    println("⚠️ Alternative PDF file verification failed")
                }
            } else {
                println("❌ Alternative PDF generation failed")
            }
            
            success
        } catch (e: Exception) {
            println("❌ ========== ALTERNATIVE PDF GENERATION EXCEPTION ==========")
            println("💥 Exception: ${e.javaClass.simpleName}")
            println("💥 Message: ${e.message}")
            println("💥 Stack trace:")
            e.printStackTrace()
            false
        }
    }

    private fun createSimplePdf(
        order: Order,
        customer: User,
        outputPath: Path
    ): Boolean {
        return try {
            println("🔄 ========== SIMPLE PDF GENERATION (LAST RESORT) ==========")
            println("🔧 Using basic PDFBox approach...")
            
            // Check output directory
            val outputFile = outputPath.toFile()
            val outputDir = outputFile.parentFile
            
            if (!outputDir.exists()) {
                val created = outputDir.mkdirs()
                println("📁 Created output directory: $created")
            }
            
            if (!outputDir.canWrite()) {
                throw Exception("Cannot write to output directory: ${outputDir.absolutePath}")
            }
            
            // Create a very basic PDF using PDFBox with error handling
            println("📄 Creating PDDocument...")
            val document = org.apache.pdfbox.pdmodel.PDDocument()
            
            try {
                println("📄 Adding A4 page...")
                val page = org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4)
                document.addPage(page)
                
                println("📄 Creating content stream...")
                val contentStream = org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)
                
                try {
                    // Add basic content with safe font handling
                    println("📝 Adding text content...")
                    var yPosition = 750f
                    
                    // Use default font to avoid font loading issues
                    contentStream.setFont(
                        org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD), 
                        12f
                    )
                    
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("GAGAN JEWELLERS")
                    contentStream.endText()
                    
                    yPosition -= 30f
                    contentStream.setFont(
                        org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 
                        10f
                    )
                    
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("Invoice: ${order.id}")
                    contentStream.endText()
                    
                    yPosition -= 20f
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("Customer: ${customer.name}")
                    contentStream.endText()
                    
                    yPosition -= 20f
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("Total: ₹${String.format("%.2f", order.totalAmount)}")
                    contentStream.endText()
                    
                    yPosition -= 20f
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("Items: ${order.items.size}")
                    contentStream.endText()
                    
                    yPosition -= 20f
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("Date: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(order.timestamp))}")
                    contentStream.endText()
                    
                    yPosition -= 20f
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("Payment: ${order.paymentMethod}")
                    contentStream.endText()
                    
                    // Add items list
                    yPosition -= 30f
                    contentStream.setFont(
                        org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD), 
                        10f
                    )
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, yPosition)
                    contentStream.showText("Items:")
                    contentStream.endText()
                    
                    contentStream.setFont(
                        org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 
                        9f
                    )
                    
                    for (item in order.items) {
                        yPosition -= 15f
                        if (yPosition < 100) break // Prevent content from going off page
                        
                        contentStream.beginText()
                        contentStream.newLineAtOffset(70f, yPosition)
                        contentStream.showText("• ${item.product.name} (Qty: ${item.quantity})")
                        contentStream.endText()
                    }
                    
                } finally {
                    println("📝 Closing content stream...")
                    contentStream.close()
                }
                
                // Save the document
                println("💾 Saving document to: ${outputPath}")
                document.save(outputPath.toFile())
                
                // Verify the file was created
                if (outputFile.exists() && outputFile.length() > 0) {
                    println("✅ ========== SIMPLE PDF CREATED SUCCESSFULLY ==========")
                    println("📄 File size: ${outputFile.length()} bytes")
                    println("📄 File path: ${outputFile.absolutePath}")
                    true
                } else {
                    println("❌ Simple PDF file verification failed")
                    false
                }
                
            } finally {
                println("📄 Closing document...")
                document.close()
            }
            
        } catch (e: Exception) {
            println("❌ ========== SIMPLE PDF CREATION EXCEPTION ==========")
            println("💥 Exception: ${e.javaClass.simpleName}")
            println("💥 Message: ${e.message}")
            println("💥 Stack trace:")
            e.printStackTrace()
            
            // Try absolute minimal PDF creation
            return tryMinimalPdfCreation(order, customer, outputPath)
        }
    }
    
    private fun tryMinimalPdfCreation(
        order: Order,
        customer: User,
        outputPath: Path
    ): Boolean {
        return try {
            println("🔄 ========== MINIMAL PDF CREATION (EMERGENCY FALLBACK) ==========")
            
            val outputFile = outputPath.toFile()
            
            // Create minimal PDF with just text
            val document = org.apache.pdfbox.pdmodel.PDDocument()
            try {
                val page = org.apache.pdfbox.pdmodel.PDPage()
                document.addPage(page)
                
                val contentStream = org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)
                try {
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, 750f)
                    contentStream.showText("GAGAN JEWELLERS - Invoice ${order.id}")
                    contentStream.endText()
                    
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, 700f)
                    contentStream.showText("Customer: ${customer.name}")
                    contentStream.endText()
                    
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, 650f)
                    contentStream.showText("Total: ₹${String.format("%.2f", order.totalAmount)}")
                    contentStream.endText()
                    
                } finally {
                    contentStream.close()
                }
                
                document.save(outputFile)
                
                if (outputFile.exists() && outputFile.length() > 0) {
                    println("✅ Minimal PDF created successfully")
                    true
                } else {
                    false
                }
                
            } finally {
                document.close()
            }
            
        } catch (e: Exception) {
            println("❌ Even minimal PDF creation failed: ${e.message}")
            false
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
        val discountAmount = calculateDiscountAmount(subtotal, gst)
        val total = subtotal + gst - discountAmount

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
    
    private fun runSystemDiagnostics() {
        try {
            println("🔍 ========== SYSTEM DIAGNOSTICS ==========")
            
            // Check Java version
            val javaVersion = System.getProperty("java.version")
            println("☕ Java Version: $javaVersion")
            
            // Check OS
            val osName = System.getProperty("os.name")
            val osVersion = System.getProperty("os.version")
            println("💻 OS: $osName $osVersion")
            
            // Check available memory
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            val totalMemory = runtime.totalMemory() / 1024 / 1024
            val freeMemory = runtime.freeMemory() / 1024 / 1024
            println("🧠 Memory: ${freeMemory}MB free / ${totalMemory}MB total / ${maxMemory}MB max")
            
            // Check PDF libraries
            try {
                val pdfBoxClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument")
                println("✅ PDFBox library available: ${pdfBoxClass.name}")
            } catch (e: ClassNotFoundException) {
                println("❌ PDFBox library not found")
            }
            
            try {
                val xhtmlClass = Class.forName("org.xhtmlrenderer.pdf.ITextRenderer")
                println("✅ Flying Saucer library available: ${xhtmlClass.name}")
            } catch (e: ClassNotFoundException) {
                println("❌ Flying Saucer library not found")
            }
            
            // Check file system permissions
            val tempDir = System.getProperty("java.io.tmpdir")
            val tempFile = File(tempDir)
            println("📁 Temp directory: $tempDir (writable: ${tempFile.canWrite()})")
            
            val userHome = System.getProperty("user.home")
            val homeFile = File(userHome)
            println("🏠 User home: $userHome (writable: ${homeFile.canWrite()})")
            
            println("✅ ========== SYSTEM DIAGNOSTICS COMPLETED ==========")
            
        } catch (e: Exception) {
            println("⚠️ System diagnostics failed: ${e.message}")
        }
    }
}