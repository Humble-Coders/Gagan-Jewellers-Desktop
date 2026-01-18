package org.example.project.viewModels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.JewelryAppInitializer
import org.example.project.data.*
import org.example.project.data.StoreInfoRepository
import org.example.project.invoice.calculation.InvoiceCalculator
import org.example.project.invoice.model.*
import org.example.project.invoice.pdf.InvoiceRenderer
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * ViewModel for invoice editing with state-driven architecture
 * Manages InvoiceDraft (editable) and Invoice (calculated) states
 */
class InvoiceViewModel {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Editable draft state - source of truth for editing
    private val _draft = mutableStateOf<InvoiceDraft?>(null)
    val draft: State<InvoiceDraft?> = _draft
    
    // Calculated final invoice state - immutable, for display and PDF
    private val _finalInvoice = mutableStateOf<Invoice?>(null)
    val finalInvoice: State<Invoice?> = _finalInvoice
    
    // PDF file path
    private val _pdfPath = mutableStateOf<String?>(null)
    val pdfPath: State<String?> = _pdfPath
    
    // Loading and error states
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _isGeneratingPDF = mutableStateOf(false)
    val isGeneratingPDF: State<Boolean> = _isGeneratingPDF
    
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage
    
    /**
     * Initialize from existing Invoice (for viewing/editing existing invoices)
     */
    fun initializeFromInvoice(invoice: Invoice) {
        _isLoading.value = false // Data is ready
        // Convert Invoice to InvoiceDraft for editing
        val draft = InvoiceDraft(
            invoiceNo = invoice.invoiceNo,
            issueDate = invoice.issueDate,
            seller = invoice.seller,
            buyer = invoice.buyer,
            items = invoice.items.map { item ->
                InvoiceItemDraft(
                    variantNo = item.variantNo,
                    productName = item.productName,
                    materialWithPurity = item.materialWithPurity,
                    quantity = item.quantity,
                    grossWeight = item.grossWeight,
                    netStoneWeight = item.netStoneWeight, // Preserve original net weights
                    netMetalWeight = item.netMetalWeight, // Preserve original net weights
                    makingPercent = item.makingPercent,
                    grossProductPrice = item.grossProductPrice,
                    labourCharges = item.labourCharges,
                    stoneAmount = item.stoneAmount,
                    barcodeId = item.barcodeId,
                    materialRatePerGram = item.materialRatePerGram,
                    diamondWeightInCarats = item.diamondWeightInCarats, // Preserve original
                    solitaireWeightInCarats = item.solitaireWeightInCarats // Preserve original
                )
            },
            exchangeGold = invoice.exchangeGold,
            discountAmount = invoice.discountAmount,
            gstPercentage = invoice.gstPercentage,
            paymentSplit = invoice.paymentSplit,
            notes = invoice.notes.takeIf { it.isNotEmpty() },
            memoNo = invoice.memoNo.takeIf { it.isNotEmpty() },
            city = invoice.city.takeIf { it.isNotEmpty() },
            placeOfDelivery = invoice.placeOfDelivery.takeIf { it.isNotEmpty() }
        )
        
        _draft.value = draft
        _finalInvoice.value = invoice
    }
    
    /**
     * Initialize from InvoiceDraft (for creating new invoices)
     */
    fun initializeFromDraft(draft: InvoiceDraft) {
        _draft.value = draft
        recalculate()
    }
    
    /**
     * Initialize from Order (for viewing/editing existing orders)
     * This converts Order to InvoiceDraft via InvoiceCalculator
     */
    fun initializeFromOrder(
        order: Order,
        customer: User,
        storeInfo: StoreInfo,
        products: Map<String, Product>,
        cartItems: List<CartItem>? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true // Start loading
                // First calculate Invoice from Order (using existing logic)
                val calculator = InvoiceCalculator()
                val invoice = calculator.calculate(order, customer, storeInfo, products, cartItems)
                
                // Then convert Invoice to InvoiceDraft for editing
                initializeFromInvoice(invoice)
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Error initializing from order: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Update seller party
     */
    fun updateSeller(party: Party) {
        val currentDraft = _draft.value ?: return
        _draft.value = currentDraft.copy(seller = party)
        recalculate()
    }
    
    /**
     * Update buyer party
     */
    fun updateBuyer(party: Party) {
        val currentDraft = _draft.value ?: return
        _draft.value = currentDraft.copy(buyer = party)
        recalculate()
    }
    
    /**
     * Update discount amount
     */
    fun updateDiscountAmount(amount: Double) {
        val currentDraft = _draft.value ?: return
        _draft.value = currentDraft.copy(discountAmount = amount)
        recalculate()
    }
    
    /**
     * Update GST percentage
     */
    fun updateGstPercentage(percentage: Double) {
        val currentDraft = _draft.value ?: return
        _draft.value = currentDraft.copy(gstPercentage = percentage)
        recalculate()
    }
    
    /**
     * Update notes
     */
    fun updateNotes(notes: String?) {
        val currentDraft = _draft.value ?: return
        _draft.value = currentDraft.copy(notes = notes)
        recalculate()
    }
    
    /**
     * Update an invoice item
     */
    fun updateItem(index: Int, item: InvoiceItemDraft) {
        val currentDraft = _draft.value ?: return
        val updatedItems = currentDraft.items.toMutableList()
        if (index in updatedItems.indices) {
            updatedItems[index] = item
            _draft.value = currentDraft.copy(items = updatedItems)
            recalculate()
        }
    }
    
    /**
     * Add a new invoice item
     */
    fun addItem(item: InvoiceItemDraft) {
        val currentDraft = _draft.value ?: return
        val updatedItems = currentDraft.items.toMutableList()
        updatedItems.add(item)
        _draft.value = currentDraft.copy(items = updatedItems)
        recalculate()
    }
    
    /**
     * Remove an invoice item
     */
    fun removeItem(index: Int) {
        val currentDraft = _draft.value ?: return
        val updatedItems = currentDraft.items.toMutableList()
        if (index in updatedItems.indices) {
            updatedItems.removeAt(index)
            _draft.value = currentDraft.copy(items = updatedItems)
            recalculate()
        }
    }
    
    /**
     * Recalculate final invoice from draft
     */
    private fun recalculate() {
        val currentDraft = _draft.value ?: return
        
        viewModelScope.launch {
            try {
                val storeInfoRepository = StoreInfoRepository()
                val storeInfo = withContext(Dispatchers.IO) {
                    storeInfoRepository.getStoreInfo()
                }
                
                val calculator = InvoiceCalculator()
                val calculatedInvoice = calculator.calculate(currentDraft, storeInfo)
                
                _finalInvoice.value = calculatedInvoice
                // Clear PDF path to force regeneration with updated values
                _pdfPath.value = null
                _errorMessage.value = null
                println("✅ INVOICE VIEWMODEL: Invoice recalculated, PDF will regenerate")
            } catch (e: Exception) {
                _errorMessage.value = "Error calculating invoice: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Generate PDF from final invoice
     */
    /**
     * Generate PDF from final invoice
     * Uses the current finalInvoice value to ensure latest data is used
     * Uploads to Firebase Storage and saves URL to order document
     */
    fun generatePDF(outputFile: File) {
        val invoice = _finalInvoice.value
        if (invoice == null) {
            println("⚠️ INVOICE VIEWMODEL: Cannot generate PDF - invoice is null")
            return
        }
        
        println("🔄 INVOICE VIEWMODEL: Generating PDF with invoice:")
        println("   - Invoice No: ${invoice.invoiceNo}")
        println("   - Buyer Name: ${invoice.buyer.name}")
        println("   - Buyer Address: ${invoice.buyer.address}")
        println("   - City: ${invoice.city}")
        println("   - Memo No: ${invoice.memoNo}")
        println("   - Date: ${invoice.issueDate}")
        
        viewModelScope.launch {
            _isGeneratingPDF.value = true
            _errorMessage.value = null
            
            try {
                // Generate PDF file
                withContext(Dispatchers.IO) {
                    val renderer = InvoiceRenderer()
                    // Use the invoice parameter (current value) to ensure we use the latest data
                    renderer.render(invoice, outputFile)
                }
                
                // Verify PDF was created successfully
                if (!outputFile.exists() || outputFile.length() <= 0) {
                    throw Exception("PDF file was not created or is empty")
                }
                
                _pdfPath.value = outputFile.absolutePath
                println("✅ INVOICE VIEWMODEL: PDF generated successfully at ${outputFile.absolutePath}")
                
                // Upload PDF to Firebase Storage and save URL to order document
                try {
                    val storageService = JewelryAppInitializer.getStorageService()
                    val firestore = JewelryAppInitializer.getFirestore()
                    val orderId = invoice.invoiceNo // invoiceNo is the orderId
                    
                    println("📤 INVOICE VIEWMODEL: Uploading PDF to Firebase Storage...")
                    println("   - Order ID: $orderId")
                    println("   - File path: ${outputFile.absolutePath}")
                    
                    // Upload under "invoices/<orderId>/"
                    val uploadedUrl = withContext(Dispatchers.IO) {
                        storageService.uploadFile(
                            outputFile.toPath(),
                            directoryPath = "invoices/$orderId"
                        )
                    }
                    
                    if (!uploadedUrl.isNullOrEmpty()) {
                        // Save URL to order document in Firestore
                        withContext(Dispatchers.IO) {
                            firestore.collection("orders")
                                .document(orderId)
                                .update(mapOf("invoiceUrl" to uploadedUrl))
                                .get()
                        }
                        println("✅ INVOICE VIEWMODEL: Invoice PDF uploaded and URL saved to order: $uploadedUrl")
                    } else {
                        println("⚠️ INVOICE VIEWMODEL: Invoice PDF upload failed or returned null URL")
                        // Don't throw error - PDF was generated successfully, just upload failed
                    }
                } catch (e: Exception) {
                    println("⚠️ INVOICE VIEWMODEL: Error uploading invoice PDF to Storage: ${e.message}")
                    e.printStackTrace()
                    // Don't fail the whole operation if upload fails - PDF was generated successfully
                }
                
                _isGeneratingPDF.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Error generating PDF: ${e.message}"
                _isGeneratingPDF.value = false
                println("❌ INVOICE VIEWMODEL: PDF generation failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Save draft (updates the draft state)
     */
    fun saveDraft(updatedDraft: InvoiceDraft) {
        _draft.value = updatedDraft
        // Clear PDF path to force regeneration with updated values
        _pdfPath.value = null
        recalculate()
    }
    
    fun onCleared() {
        viewModelScope.cancel()
    }
}

