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
                "paymentStatus" to order.paymentStatus.name,
                "subtotal" to order.subtotal,
                "discountAmount" to order.discountAmount,
                "discountPercent" to order.discountPercent,
                "taxableAmount" to order.taxableAmount,
                "gstAmount" to order.gstAmount,
                "totalAmount" to order.totalAmount,
                "finalAmount" to order.finalAmount,
                "isGstIncluded" to order.isGstIncluded,
                "status" to order.status.name,
                "createdAt" to order.createdAt,
                "updatedAt" to order.updatedAt,
                "completedAt" to order.completedAt,
                "transactionDate" to order.transactionDate,
                "notes" to order.notes,
                "metalRatesReference" to order.metalRatesReference, // Reference to rates collection
                "items" to order.items.map { item ->
                    mapOf(
                        "productId" to item.productId,
                        "barcodeId" to item.barcodeId,
                        "quantity" to item.quantity,
                        "defaultMakingRate" to item.defaultMakingRate,
                        "vaCharges" to item.vaCharges,
                        "materialType" to item.materialType
                    )
                },
                "paymentSplit" to order.paymentSplit?.let { split ->
                    mapOf(
                        "cashAmount" to split.cashAmount,
                        "cardAmount" to split.cardAmount,
                        "bankAmount" to split.bankAmount,
                        "onlineAmount" to split.onlineAmount,
                        "dueAmount" to split.dueAmount,
                        "totalAmount" to split.totalAmount
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
