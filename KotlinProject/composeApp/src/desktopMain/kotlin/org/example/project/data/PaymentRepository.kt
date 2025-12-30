package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PaymentRepository {
    suspend fun saveOrder(order: Order): Boolean
}

class FirestorePaymentRepository(private val firestore: Firestore) : PaymentRepository {
    
    override suspend fun saveOrder(order: Order): Boolean = withContext(Dispatchers.IO) {
        try {
            println("💾 PAYMENT REPOSITORY: Saving order to Firestore")
            println("   - Order ID: ${order.orderId}")
            println("   - Customer ID: ${order.customerId}")
            println("   - Total Amount: ₹${String.format("%.2f", order.totalAmount)}")
            
            val orderMap = mapOf(
                "orderId" to order.orderId,
                "customerId" to order.customerId, // Reference to users collection
                "totalProductValue" to order.totalProductValue, // Stored as finalAmount in Firestore for backward compatibility
                "finalAmount" to order.totalProductValue, // Backward compatibility
                "discountAmount" to order.discountAmount,
                "discountPercent" to (order.discountPercent ?: 0.0), // Store 0.0 if null
                "gstAmount" to order.gstAmount,
                "gstPercentage" to order.gstPercentage,
                "totalAmount" to order.totalAmount, // Total payable amount after GST and discount applied
                "isGstIncluded" to order.isGstIncluded,
                "createdAt" to order.createdAt,
                "updatedAt" to order.updatedAt,
                "transactionDate" to order.transactionDate,
                "notes" to order.notes,
                "items" to order.items.map { item ->
                    mapOf(
                        "barcodeId" to item.barcodeId,
                        "productId" to item.productId,
                        "quantity" to item.quantity,
                        "makingPercentage" to item.makingPercentage,
                        "labourCharges" to item.labourCharges,
                        "labourRate" to item.labourRate
                    )
                },
                "paymentSplit" to order.paymentSplit?.let { split ->
                    mapOf(
                        "bank" to split.bank, // Sum of bankAmount + cardAmount + onlineAmount
                        "cash" to split.cash,
                        "dueAmount" to split.dueAmount
                    )
                }
            )
            
            firestore.collection("orders").document(order.orderId).set(orderMap).get()
            
            println("✅ PAYMENT REPOSITORY: Order saved successfully")
            println("   - Document ID: ${order.orderId}")
            true
        } catch (e: Exception) {
            println("❌ PAYMENT REPOSITORY: Failed to save order")
            println("   - Error: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
