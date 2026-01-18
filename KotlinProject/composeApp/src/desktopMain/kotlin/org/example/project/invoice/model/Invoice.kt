package org.example.project.invoice.model

import java.time.LocalDate

/**
 * Pure data model for invoices - no formatting, no UI concerns
 * Values are already calculated and frozen
 */
data class Invoice(
    val invoiceNo: String,
    val issueDate: LocalDate,
    val seller: Party,
    val buyer: Party,
    val items: List<InvoiceItem>,
    val exchangeGold: ExchangeGoldInfo?,
    val subtotal: Double,
    val exchangeGoldValue: Double,
    val discountAmount: Double,
    val taxableAmount: Double,
    val gstPercentage: Double,
    val gstAmount: Double,
    val grossTotal: Double,
    val roundOff: Double,
    val netAmount: Double,
    val paymentSplit: PaymentSplit?,
    val notes: String,
    val irn: String,
    val ackNo: String,
    val ackDate: String,
    val bankInfo: BankInfo,
    // Additional fields for invoice header display
    val memoNo: String = "", // Memo number (defaults to "GST/{invoiceNo.takeLast(8)}")
    val city: String = "", // City (extracted from address by default)
    val placeOfDelivery: String = "" // Place of delivery (defaults to buyer.stateName)
)

data class Party(
    val name: String,
    val address: String,
    val phone: String,
    val phonePrimary: String = "",
    val phoneSecondary: String = "",
    val email: String,
    val gstin: String,
    val pan: String?,
    val stateCode: String,
    val stateName: String,
    val certification: String = ""
)

data class InvoiceItem(
    val variantNo: String,
    val productName: String,
    val materialWithPurity: String,
    val quantity: Int,
    val grossWeight: Double,
    val netStoneWeight: Double,
    val netMetalWeight: Double,
    val makingPercent: Double,
    val grossProductPrice: Double,
    val labourCharges: Double,
    val stoneAmount: Double,
    val costValue: Double,
    val barcodeId: String = "", // HSM - barcode ID of the product
    val diamondWeightInCarats: Double = 0.0, // Diamond weight in carats (cent value)
    val solitaireWeightInCarats: Double = 0.0, // Solitaire weight in carats (cent value)
    val materialRatePerGram: Double = 0.0 // Material rate per gram from materials collection
)

data class ExchangeGoldInfo(
    val productName: String,
    val goldWeight: Double,
    val goldPurity: String,
    val goldRate: Double,
    val finalGoldExchangePrice: Double,
    val totalProductWeight: Double = 0.0, // Total product weight in grams
    val percentage: Double = 0.0 // Percentage used for calculation
)

data class PaymentSplit(
    val cash: Double,
    val bank: Double,
    val dueAmount: Double
)

data class BankInfo(
    val accountHolder: String,
    val accountNumber: String,
    val ifscCode: String,
    val branch: String,
    val accountType: String,
    val pan: String = "" // PAN from bank_info document
)

