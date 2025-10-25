package org.example.project.data

import java.util.*

// Unified transaction model for displaying both orders and cash transactions
data class UnifiedTransaction(
    val id: String = "",
    val customerId: String = "",
    val transactionType: TransactionType = TransactionType.ORDER,
    val amount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val transactionDate: String = "",
    val status: OrderStatus = OrderStatus.CONFIRMED,
    val paymentStatus: PaymentStatus = PaymentStatus.COMPLETED,
    
    // Order-specific fields (null for cash transactions)
    val orderId: String? = null,
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val discountPercent: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val isGstIncluded: Boolean = true,
    val paymentSplit: PaymentSplit? = null,
    val metalRatesReference: String = "",
    
    // Cash transaction-specific fields (null for orders)
    val cashAmountId: String? = null,
    val cashTransactionType: CashTransactionType? = null,
    val createdBy: String = "system"
)

enum class TransactionType {
    ORDER,      // Regular order transaction
    CASH        // Cash amount transaction
}

// Extension functions to convert Order and CashAmount to UnifiedTransaction
fun Order.toUnifiedTransaction(): UnifiedTransaction {
    return UnifiedTransaction(
        id = this.orderId,
        customerId = this.customerId,
        transactionType = TransactionType.ORDER,
        amount = this.finalAmount,
        finalAmount = this.finalAmount,
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        completedAt = this.completedAt,
        transactionDate = this.transactionDate,
        status = this.status,
        paymentStatus = this.paymentStatus,
        orderId = this.orderId,
        items = this.items,
        subtotal = this.subtotal,
        discountAmount = this.discountAmount,
        discountPercent = this.discountPercent,
        taxableAmount = this.taxableAmount,
        gstAmount = this.gstAmount,
        totalAmount = this.totalAmount,
        isGstIncluded = this.isGstIncluded,
        paymentSplit = this.paymentSplit,
        metalRatesReference = this.metalRatesReference,
        cashAmountId = null,
        cashTransactionType = null,
        createdBy = "system"
    )
}

fun CashAmount.toUnifiedTransaction(): UnifiedTransaction {
    return UnifiedTransaction(
        id = this.cashAmountId,
        customerId = this.customerId,
        transactionType = TransactionType.CASH,
        amount = this.amount,
        finalAmount = this.finalAmount,
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        completedAt = this.completedAt,
        transactionDate = this.transactionDate,
        status = this.status,
        paymentStatus = this.paymentStatus,
        orderId = null,
        items = this.items,
        subtotal = this.subtotal,
        discountAmount = this.discountAmount,
        discountPercent = this.discountPercent,
        taxableAmount = this.taxableAmount,
        gstAmount = this.gstAmount,
        totalAmount = this.totalAmount,
        isGstIncluded = this.isGstIncluded,
        paymentSplit = this.paymentSplit,
        metalRatesReference = this.metalRatesReference,
        cashAmountId = this.cashAmountId,
        cashTransactionType = this.transactionType,
        createdBy = this.createdBy
    )
}
