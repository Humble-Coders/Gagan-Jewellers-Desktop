package org.example.project.invoice.model

import java.time.LocalDate

/**
 * Editable draft model for invoices
 * This is the source of truth for editing - all changes happen here
 * InvoiceDraft is converted to Invoice via InvoiceCalculator
 */
data class InvoiceDraft(
    val invoiceNo: String,
    val issueDate: LocalDate,
    val seller: Party,
    val buyer: Party,
    val items: List<InvoiceItemDraft>,
    val exchangeGold: ExchangeGoldInfo?,
    val discountAmount: Double,
    val gstPercentage: Double,
    val paymentSplit: PaymentSplit?,
    val notes: String?,
    // Additional editable fields for invoice header
    val memoNo: String? = null, // Editable memo number (defaults to "GST/{invoiceNo.takeLast(8)}")
    val city: String? = null, // Editable city (extracted from address by default)
    val placeOfDelivery: String? = null // Editable place of delivery (defaults to buyer.stateName)
)

/**
 * Editable draft model for invoice items
 * Contains editable fields that will be used in calculations
 * IMPORTANT: When editing invoice, items should remain unchanged - only party section changes
 */
data class InvoiceItemDraft(
    val variantNo: String,
    val productName: String,
    val materialWithPurity: String,
    val quantity: Int,
    val grossWeight: Double,
    val netStoneWeight: Double = 0.0, // Preserved from original invoice to keep items unchanged
    val netMetalWeight: Double = 0.0, // Preserved from original invoice to keep items unchanged
    val makingPercent: Double,
    val grossProductPrice: Double,
    val labourCharges: Double,
    val stoneAmount: Double,
    val barcodeId: String = "",
    val materialRatePerGram: Double = 0.0,
    val diamondWeightInCarats: Double = 0.0, // Preserved from original invoice
    val solitaireWeightInCarats: Double = 0.0 // Preserved from original invoice
)

