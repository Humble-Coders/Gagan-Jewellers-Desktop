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
import org.example.project.data.extractKaratFromMaterialType
import org.example.project.ui.ProductPriceInputs
import org.example.project.ui.calculateProductPrice
import java.awt.Desktop
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class PaymentViewModel(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository
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
    
    // Receipt UI State - centralized state management
    data class ReceiptUiState(
        val isLoading: Boolean = false,
        val pdfPath: String? = null,
        val error: String? = null
    )
    
    private val _receiptUiState = mutableStateOf(ReceiptUiState())
    val receiptUiState: State<ReceiptUiState> = _receiptUiState
    
    // Track which order ID is currently being processed to prevent duplicates
    private var currentGeneratingOrderId: String? = null

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
        notes: String = "",
        customer: User, // Require customer to be passed, not fetched
        onSuccess: () -> Unit
    ) {
        // Allow order processing if either a payment method is selected OR a split payment is configured
        if (_selectedPaymentMethod.value == null && paymentSplit == null) {
            _errorMessage.value = "Please select a payment method"
            return
        }

        // Validate payment split if provided
        if (paymentSplit != null) {
            if (paymentSplit.exceedsTotal(finalTotal)) {
                val difference = paymentSplit.getDifference(finalTotal)
                _errorMessage.value = "Payment breakdown exceeds total amount by ₹${String.format("%.2f", difference)}"
                return
            }
            if (!paymentSplit.isValid(finalTotal)) {
                val difference = paymentSplit.getDifference(finalTotal)
                _errorMessage.value = "Payment amounts don't match total. Difference: ₹${String.format("%.2f", kotlin.math.abs(difference))}"
                return
            }
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // Determine payment method: use selected method or default to CASH if split payment is configured
                val paymentMethod = _selectedPaymentMethod.value ?: PaymentMethod.CASH
                
                val gstRate = if (subtotal > 0) gst / subtotal else 0.0
                
                // Convert CartItem objects to OrderItem objects with additional fields
                // Use ProductPriceCalculator to get making percentage, labour charges, and labour rate
                val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
                val orderItems = cart.items.flatMap { cartItem ->
                    cartItem.selectedBarcodeIds.map { barcodeId ->
                        val product = cartItem.product
                        val grossWeight = if (cartItem.grossWeight > 0) cartItem.grossWeight else product.totalWeight
                        val makingPercentage = product.makingPercent
                        val labourRate = product.labourRate
                        
                        // Calculate labour charges using ProductPriceCalculator logic
                        val metalKarat = if (cartItem.metal.isNotEmpty()) {
                            cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(product.materialType)
                        } else {
                            extractKaratFromMaterialType(product.materialType)
                        }
                        
                        // Get material rate
                        val collectionRate = try {
                            ratesVM.calculateRateForMaterial(product.materialId, product.materialType, metalKarat)
                        } catch (e: Exception) { 0.0 }
                        
                        val metalRates = MetalRatesManager.metalRates.value
                        val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
                        val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else defaultGoldRate
                        
                        // Extract kundan and jarkan from stones array
                        val kundanStones = product.stones.filter { it.name.equals("Kundan", ignoreCase = true) }
                        val jarkanStones = product.stones.filter { it.name.equals("Jarkan", ignoreCase = true) }
                        val kundanPrice = kundanStones.sumOf { it.amount }
                        val jarkanPrice = jarkanStones.sumOf { it.amount }
                        
                        // Calculate goldRatePerGram (use collectionRate if available, otherwise use goldRate)
                        val materialTypeLower = product.materialType.lowercase()
                        val silverPurity = when {
                            materialTypeLower.contains("999") -> 999
                            materialTypeLower.contains("925") || materialTypeLower.contains("92.5") -> 925
                            materialTypeLower.contains("900") || materialTypeLower.contains("90.0") -> 900
                            else -> 999
                        }
                        val silverRate = metalRates.getSilverRateForPurity(silverPurity)
                        val goldRatePerGram = if (collectionRate > 0) collectionRate else when {
                            product.materialType.contains("gold", ignoreCase = true) -> goldRate
                            product.materialType.contains("silver", ignoreCase = true) -> silverRate
                            else -> goldRate
                        }
                        
                        // Calculate using ProductPriceCalculator
                        val priceInputs = ProductPriceInputs(
                            grossWeight = grossWeight,
                            goldPurity = product.materialType,
                            goldWeight = product.materialWeight.takeIf { it > 0 } ?: grossWeight,
                            makingPercentage = makingPercentage,
                            labourRatePerGram = labourRate,
                            kundanPrice = kundanPrice,
                            kundanWeight = kundanStones.sumOf { it.weight },
                            jarkanPrice = jarkanPrice,
                            jarkanWeight = jarkanStones.sumOf { it.weight },
                            goldRatePerGram = goldRatePerGram
                        )
                        
                        val priceResult = calculateProductPrice(priceInputs)
                        val labourCharges = priceResult.labourCharges
                        
                        OrderItem(
                            barcodeId = barcodeId,
                            productId = cartItem.productId,
                            quantity = 1, // Each barcode represents 1 item
                            makingPercentage = makingPercentage,
                            labourCharges = labourCharges,
                            labourRate = labourRate
                        )
                    }
                }
                
                println("📦 CREATING ORDER WITH SIMPLIFIED ITEMS")
                println("   - Cart items count: ${cart.items.size}")
                println("   - Order items count: ${orderItems.size}")
                orderItems.forEach { orderItem ->
                    println("   - OrderItem: productId=${orderItem.productId}, barcodeId=${orderItem.barcodeId}")
                }
                
                // Customer is now passed as parameter, no need to fetch
                // Calculate totalProductValue (sum of all product prices before discount and GST)
                // This is the subtotal from PaymentScreen
                val totalProductValue = subtotal

                // Calculate discount percentage if discount is applied
                val discountPercent = if (discountAmount > 0 && totalProductValue > 0) {
                    when (_discountType.value) {
                        DiscountType.PERCENTAGE -> _discountAmount.value
                        DiscountType.AMOUNT -> (discountAmount / totalProductValue) * 100.0
                        DiscountType.TOTAL_PAYABLE -> {
                            // Calculate percentage from total payable
                            val totalPayable = totalProductValue + gst - discountAmount
                            if (totalProductValue > 0) {
                                ((totalProductValue - (totalProductValue + gst - _discountAmount.value)) / totalProductValue) * 100.0
                            } else 0.0
                        }
                    }
                } else null

                // Calculate GST percentage
                val taxableAmount = totalProductValue - discountAmount
                val gstPercentage = if (taxableAmount > 0 && gst > 0) {
                    (gst / taxableAmount) * 100.0
                } else 0.0

                // PaymentSplit already has bank (sum), cash, and dueAmount in new format
                // No conversion needed - use as is
                val updatedPaymentSplit = paymentSplit

                // Format transaction date
                val dateFormat = SimpleDateFormat("dd MMMM yyyy 'at' HH:mm:ss z", Locale.getDefault())
                val transactionDate = dateFormat.format(Date())

                println("📦 ORDER CREATION DETAILS")
                println("   - Customer ID: ${cart.customerId}")
                println("   - Total Product Value: $totalProductValue")
                println("   - Discount Amount: $discountAmount")
                println("   - Discount Percent: $discountPercent")
                println("   - GST Amount: $gst")
                println("   - GST Percentage: $gstPercentage")
                println("   - Total Amount: $finalTotal")
                println("   - Transaction Date: $transactionDate")

                val order = Order(
                    orderId = generateOrderId(),
                    customerId = cart.customerId, // Reference to users collection
                    paymentSplit = updatedPaymentSplit,
                    totalProductValue = totalProductValue,
                    discountAmount = discountAmount,
                    discountPercent = discountPercent,
                    gstAmount = gst,
                    gstPercentage = gstPercentage,
                    totalAmount = finalTotal, // Total payable amount after GST and discount applied
                    isGstIncluded = _isGstIncluded.value,
                    items = orderItems,
                    transactionDate = transactionDate, // Set transaction date
                    notes = notes, // Add notes from payment screen
                    createdAt = System.currentTimeMillis()
                )

                // Get inventory item IDs before transaction (needed for atomic deletion)
                // Note: There's a potential race condition here - items may be deleted between lookup and transaction
                // However, the transaction in PaymentRepository will validate items still exist before deletion
                val inventoryRepository = JewelryAppInitializer.getInventoryRepository()
                val inventoryItemIds = order.items.mapNotNull { orderItem ->
                    try {
                        val inventoryItem = inventoryRepository?.getInventoryItemByBarcodeId(orderItem.barcodeId)
                        if (inventoryItem == null) {
                            println("⚠️ Inventory item not found for barcode: ${orderItem.barcodeId}")
                            println("   - This may indicate the item was already sold or deleted")
                        }
                        inventoryItem?.id
                    } catch (e: Exception) {
                        println("⚠️ Error finding inventory item for barcode ${orderItem.barcodeId}: ${e.message}")
                        null
                    }
                }
                
                // Validate all inventory items were found
                if (inventoryItemIds.size < order.items.size) {
                    val missingCount = order.items.size - inventoryItemIds.size
                    println("⚠️ WARNING: $missingCount inventory item(s) not found")
                    println("   - Transaction will validate items exist before deletion")
                    println("   - Order may fail if items were deleted by another process")
                }

                // Save order to database (includes customer balance update AND inventory deletion in transaction)
                val success = paymentRepository.saveOrder(order, inventoryItemIds)

                if (success) {
                    // Note: Customer balance and inventory are now updated atomically within saveOrder transaction
                    // Trigger inventory refresh to update dashboard quantities
                    JewelryAppInitializer.getViewModel().triggerInventoryRefresh()

                    // Refresh product list to update UI
                    JewelryAppInitializer.getViewModel().loadProducts()

                    // Create transaction record
                    _lastTransaction.value = PaymentTransaction(
                        id = order.orderId,
                        cartId = order.customerId,
                        paymentMethod = PaymentMethod.CASH, // Default payment method
                        paymentSplit = order.paymentSplit,
                        subtotal = order.totalProductValue, // Use totalProductValue for subtotal
                        discountAmount = order.discountAmount,
                        gstAmount = order.gstAmount,
                        totalAmount = order.totalAmount,
                        items = cart.items, // Use CartItem objects for receipt display
                        timestamp = order.createdAt,
                        status = PaymentStatus.COMPLETED
                    )

                    // PDF will be generated automatically by ReceiptScreen when it's displayed
                    // Removed duplicate PDF generation here to prevent double generation

                    _successMessage.value = "Order saved successfully"
                    onSuccess()
                } else {
                    _errorMessage.value = "Failed to save order. Please try again."
                }
            } catch (e: Exception) {
                // Better error handling with specific error messages
                val errorMsg = when {
                    e.message?.contains("Customer not found", ignoreCase = true) == true -> {
                        "Customer not found. Please select a valid customer."
                    }
                    e.message?.contains("not found", ignoreCase = true) == true ||
                    e.message?.contains("no longer available", ignoreCase = true) == true ||
                    e.message?.contains("may have been sold", ignoreCase = true) == true -> {
                        "One or more items are no longer available. They may have been sold to another customer. Please refresh and try again."
                    }
                    e.message?.contains("inventory", ignoreCase = true) == true -> {
                        "Inventory error: ${e.message ?: "Item no longer available"}"
                    }
                    e.message?.contains("balance", ignoreCase = true) == true -> {
                        "Failed to update customer balance: ${e.message ?: "Unknown error"}"
                    }
                    e.message?.contains("Transaction conflict", ignoreCase = true) == true ||
                    e.message?.contains("ABORTED", ignoreCase = true) == true -> {
                        "Transaction conflict detected. The inventory may have been modified. Please try again."
                    }
                    e.message?.contains("timeout", ignoreCase = true) == true ||
                    e.message?.contains("deadline", ignoreCase = true) == true -> {
                        "Request timeout. Please check your connection and try again."
                    }
                    else -> {
                        "Error saving order: ${e.localizedMessage ?: e.message ?: "Something went wrong. Please try again."}"
                    }
                }
                _errorMessage.value = errorMsg
                println("❌ Error in saveOrderWithPaymentMethod: ${e.message}")
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

    private fun generateOrderPDF(order: Order, customer: User, transaction: PaymentTransaction? = null) {
        viewModelScope.launch {
            _isGeneratingPDF.value = true
            _errorMessage.value = null
            
            // Also update receipt UI state if this is for receipt screen
            if (_receiptUiState.value.isLoading) {
                _receiptUiState.value = _receiptUiState.value.copy(
                    isLoading = true,
                    error = null
                )
            }
            
            try {
                // 🔧 FIX: Retrieve order from Firestore to ensure we have the latest stored data
                println("📄 PDF GENERATION: Retrieving order from Firestore for accurate bill generation")
                val firestoreOrder = orderRepository.getOrderById(order.orderId)
                
                if (firestoreOrder == null) {
                    throw Exception("Order not found in Firestore: ${order.orderId}")
                }
                
                println("📄 PDF GENERATION: Using Firestore order data")
                println("   - Order ID: ${firestoreOrder.orderId}")
                println("   - Items count: ${firestoreOrder.items.size}")
                firestoreOrder.items.forEach { item ->
                    println("   - Item: productId=${item.productId}, makingPercentage=${item.makingPercentage}, labourCharges=${item.labourCharges}, labourRate=${item.labourRate}")
                }
                
                // Use the order retrieved from Firestore instead of the passed order
                val orderToUse = firestoreOrder
                // Get CartItem data from transaction if available (for accurate pricing with overrides)
                val cartItems = transaction?.items
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

                val pdfFileName = "Invoice_${orderToUse.orderId}_${System.currentTimeMillis()}.pdf"
                val pdfOutputPath = File(billsDirectory, pdfFileName)

                // Fetch invoice configuration directly; fall back to defaults if not found
                val invoiceConfig = org.example.project.data.InvoiceConfigRepository()
                    .getInvoiceConfig() ?: org.example.project.data.InvoiceConfig()
                
                val pdfResult = pdfGeneratorService.generateInvoicePDF(
                    order = orderToUse,
                    customer = customer,
                    outputFile = pdfOutputPath,
                    invoiceConfig = invoiceConfig
                )

                if (pdfResult.isSuccess) {
                    val pdfFile = pdfResult.getOrThrow()
                    if (pdfFile.exists() && pdfFile.length() > 0) {
                        val pdfFilePath = pdfFile.absolutePath
                        _pdfPath.value = pdfFilePath
                        
                        // Update receipt UI state if loading
                        if (_receiptUiState.value.isLoading) {
                            _receiptUiState.value = _receiptUiState.value.copy(
                                isLoading = false,
                                pdfPath = pdfFilePath,
                                error = null
                            )
                        }
                        
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
                        val errorMsg = "PDF file was not created properly"
                        _errorMessage.value = errorMsg
                        if (_receiptUiState.value.isLoading) {
                            _receiptUiState.value = _receiptUiState.value.copy(
                                isLoading = false,
                                error = errorMsg
                            )
                        }
                    }
                } else {
                    val errorMsg = "Failed to generate PDF invoice: ${pdfResult.exceptionOrNull()?.message}"
                    _errorMessage.value = errorMsg
                    if (_receiptUiState.value.isLoading) {
                        _receiptUiState.value = _receiptUiState.value.copy(
                            isLoading = false,
                            error = errorMsg
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "PDF generation failed: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                _errorMessage.value = errorMsg
                if (_receiptUiState.value.isLoading) {
                    _receiptUiState.value = _receiptUiState.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } finally {
                _isGeneratingPDF.value = false
            }
        }
    }

    // Public helper: generate PDF for the last successful transaction
    /**
     * Generate receipt PDF for a specific order ID.
     * Prevents duplicate generation and handles errors properly.
     * Owns generation logic and error handling.
     */
    fun generateReceiptPdf(orderId: String) {
        viewModelScope.launch {
            // Prevent duplicate generation
            if (currentGeneratingOrderId == orderId || _receiptUiState.value.isLoading) {
                println("⚠️ PDF generation already in progress for order: $orderId")
                return@launch
            }
            
            val transaction = _lastTransaction.value
            if (transaction == null || transaction.id != orderId) {
                _receiptUiState.value = ReceiptUiState(
                    isLoading = false,
                    pdfPath = null,
                    error = "Transaction not found for order: $orderId"
                )
                return@launch
            }
            
            val customer = JewelryAppInitializer.getCustomerViewModel().selectedCustomer.value
            if (customer == null) {
                _receiptUiState.value = ReceiptUiState(
                    isLoading = false,
                    pdfPath = null,
                    error = "Customer not selected"
                )
                return@launch
            }
            
            // Update state to loading
            currentGeneratingOrderId = orderId
            _receiptUiState.value = ReceiptUiState(
                isLoading = true,
                pdfPath = _receiptUiState.value.pdfPath, // Keep existing path while loading
                error = null
            )
            
            try {
                // Use existing generatePdfForLastTransaction logic
                // generateOrderPDF (called internally) will update _receiptUiState when complete
                generatePdfForLastTransactionInternal(customer)
                
                // Note: State is updated in generateOrderPDF when _receiptUiState.value.isLoading is true
                // No need to update here as it will be handled asynchronously
            } catch (e: Exception) {
                // Handle errors properly
                val errorMsg = when {
                    e.message?.contains("not found", ignoreCase = true) == true -> {
                        "Order not found: ${e.message}"
                    }
                    e.message?.contains("customer", ignoreCase = true) == true -> {
                        "Customer information missing: ${e.message}"
                    }
                    e.message?.contains("PDF", ignoreCase = true) == true -> {
                        "PDF generation failed: ${e.message}"
                    }
                    else -> {
                        "Failed to generate receipt: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                    }
                }
                
                _receiptUiState.value = ReceiptUiState(
                    isLoading = false,
                    pdfPath = null,
                    error = errorMsg
                )
                
                println("❌ Error generating receipt PDF: ${e.message}")
                e.printStackTrace()
            } finally {
                currentGeneratingOrderId = null
            }
        }
    }
    
    fun generatePdfForLastTransaction(customer: User) {
        val transaction = _lastTransaction.value ?: return
        generatePdfForLastTransactionInternal(customer)
    }
    
    private fun generatePdfForLastTransactionInternal(customer: User) {
        val transaction = _lastTransaction.value ?: return
        // Calculate taxable amount (subtotal - discount amount)
        val taxableAmount = transaction.subtotal - transaction.discountAmount
        
        // Format transaction date
        val dateFormat = SimpleDateFormat("dd MMMM yyyy 'at' HH:mm:ss z", Locale.getDefault())
        val transactionDate = dateFormat.format(Date(transaction.timestamp))
        
        // Calculate discount percent from transaction
        val discountPercent = if (transaction.discountAmount > 0 && transaction.subtotal > 0) {
            (transaction.discountAmount / transaction.subtotal) * 100.0
        } else null
        
        // Calculate GST percentage
        val gstPercentage = if (taxableAmount > 0 && transaction.gstAmount > 0) {
            (transaction.gstAmount / taxableAmount) * 100.0
        } else 0.0
        
        val order = Order(
            orderId = transaction.id,
            customerId = transaction.cartId, // Reference to users collection
            paymentSplit = transaction.paymentSplit,
            totalProductValue = transaction.subtotal, // Use subtotal as totalProductValue
            discountAmount = transaction.discountAmount,
            discountPercent = discountPercent,
            gstAmount = transaction.gstAmount,
            gstPercentage = gstPercentage,
            totalAmount = transaction.totalAmount, // Total payable amount after GST and discount applied
            isGstIncluded = true,
            items = transaction.items.map { cartItem ->
                // Convert CartItem to OrderItem for PDF generation
                val product = cartItem.product
                val grossWeight = if (cartItem.grossWeight > 0) cartItem.grossWeight else product.totalWeight
                val makingPercentage = product.makingPercent
                val labourRate = product.labourRate
                
                // Calculate labour charges using ProductPriceCalculator logic
                val metalKarat = if (cartItem.metal.isNotEmpty()) {
                    cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(product.materialType)
                } else {
                    extractKaratFromMaterialType(product.materialType)
                }
                
                val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
                val collectionRate = try {
                    ratesVM.calculateRateForMaterial(product.materialId, product.materialType, metalKarat)
                } catch (e: Exception) { 0.0 }
                
                val metalRates = MetalRatesManager.metalRates.value
                val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
                val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else defaultGoldRate
                
                val kundanStones = product.stones.filter { it.name.equals("Kundan", ignoreCase = true) }
                val jarkanStones = product.stones.filter { it.name.equals("Jarkan", ignoreCase = true) }
                val kundanPrice = kundanStones.sumOf { it.amount }
                val jarkanPrice = jarkanStones.sumOf { it.amount }
                
                // Calculate goldRatePerGram
                val materialTypeLower = product.materialType.lowercase()
                val silverPurity = when {
                    materialTypeLower.contains("999") -> 999
                    materialTypeLower.contains("925") || materialTypeLower.contains("92.5") -> 925
                    materialTypeLower.contains("900") || materialTypeLower.contains("90.0") -> 900
                    else -> 999
                }
                val silverRate = metalRates.getSilverRateForPurity(silverPurity)
                val goldRatePerGram = if (collectionRate > 0) collectionRate else when {
                    product.materialType.contains("gold", ignoreCase = true) -> goldRate
                    product.materialType.contains("silver", ignoreCase = true) -> silverRate
                    else -> goldRate
                }
                
                val priceInputs = ProductPriceInputs(
                    grossWeight = grossWeight,
                    goldPurity = product.materialType,
                    goldWeight = product.materialWeight.takeIf { it > 0 } ?: grossWeight,
                    makingPercentage = makingPercentage,
                    labourRatePerGram = labourRate,
                    kundanPrice = kundanPrice,
                    kundanWeight = kundanStones.sumOf { it.weight },
                    jarkanPrice = jarkanPrice,
                    jarkanWeight = jarkanStones.sumOf { it.weight },
                    goldRatePerGram = goldRatePerGram
                )
                
                val priceResult = calculateProductPrice(priceInputs)
                val labourCharges = priceResult.labourCharges
                
                OrderItem(
                    barcodeId = cartItem.selectedBarcodeIds.firstOrNull() ?: "",
                    productId = cartItem.productId,
                    quantity = cartItem.quantity,
                    makingPercentage = makingPercentage,
                    labourCharges = labourCharges,
                    labourRate = labourRate
                )
            },
            transactionDate = transactionDate, // Set transaction date
            createdAt = transaction.timestamp
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
}