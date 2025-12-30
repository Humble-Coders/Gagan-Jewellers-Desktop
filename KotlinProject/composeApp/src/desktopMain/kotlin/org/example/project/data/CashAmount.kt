package org.example.project.data

import java.util.*

data class CashAmount(
    val cashAmountId: String = "", // Same format as orderId but with TRA prefix
    val customerId: String = "",
    
    // Payment Information (matching Order structure)
    val paymentSplit: PaymentSplit? = null,
    val paymentStatus: PaymentStatus = PaymentStatus.COMPLETED, // Cash transactions are always completed
    
    // Financial Details (matching Order structure)
    val subtotal: Double = 0.0, // Base amount
    val discountAmount: Double = 0.0,
    val discountPercent: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val isGstIncluded: Boolean = false, // Cash transactions typically don't include GST
    
    // Cash Transaction Specific Fields
    val amount: Double = 0.0, // The actual cash amount
    val transactionType: CashTransactionType = CashTransactionType.GIVE,
    
    // Order Items - Empty for cash transactions but keeping structure
    val items: List<OrderItem> = emptyList(),
    
    // Timestamps (matching Order structure)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = System.currentTimeMillis(), // Cash transactions are immediately completed
    val transactionDate: String = "",
    
    // Additional Information (matching Order structure)
    val notes: String = "",
    val status: OrderStatus = OrderStatus.CONFIRMED, // Cash transactions are always confirmed
    val createdBy: String = "system"
)

enum class CashTransactionType {
    GIVE,    // Give cash to customer (increases customer balance)
    RECEIVE  // Receive cash from customer (decreases customer balance)
}
