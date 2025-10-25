package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface OrderRepository {
    suspend fun getOrdersByCustomerId(customerId: String): List<Order>
    suspend fun getAllOrders(): List<Order>
}

class FirestoreOrderRepository(private val firestore: Firestore) : OrderRepository {
    
    override suspend fun getOrdersByCustomerId(customerId: String): List<Order> = withContext(Dispatchers.IO) {
        try {
            println("📋 ORDER REPOSITORY: Fetching orders for customer $customerId")
            
            val ordersCollection = firestore.collection("orders")
            val query = ordersCollection.whereEqualTo("customerId", customerId)
            val future = query.get()
            val snapshot = future.get()
            
            val orders = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                try {
                    Order(
                        orderId = data["orderId"] as? String ?: "",
                        customerId = data["customerId"] as? String ?: "",
                        paymentStatus = PaymentStatus.valueOf(data["paymentStatus"] as? String ?: "PENDING"),
                        subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                        discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                        discountPercent = (data["discountPercent"] as? Number)?.toDouble() ?: 0.0,
                        taxableAmount = (data["taxableAmount"] as? Number)?.toDouble() ?: 0.0,
                        gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                        totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                        finalAmount = (data["finalAmount"] as? Number)?.toDouble() ?: 0.0,
                        isGstIncluded = data["isGstIncluded"] as? Boolean ?: true,
                        status = OrderStatus.valueOf(data["status"] as? String ?: "CONFIRMED"),
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        completedAt = (data["completedAt"] as? Number)?.toLong(),
                        transactionDate = data["transactionDate"] as? String ?: "",
                        notes = data["notes"] as? String ?: "",
                        metalRatesReference = data["metalRatesReference"] as? String ?: "",
                        items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
                            if (itemData is Map<*, *>) {
                                OrderItem(
                                    productId = itemData["productId"] as? String ?: "",
                                    barcodeId = itemData["barcodeId"] as? String ?: "",
                                    quantity = (itemData["quantity"] as? Number)?.toInt() ?: 1
                                )
                            } else null
                        } ?: emptyList(),
                        paymentSplit = (data["paymentSplit"] as? Map<*, *>)?.let { splitData ->
                            PaymentSplit(
                                cashAmount = (splitData["cashAmount"] as? Number)?.toDouble() ?: 0.0,
                                cardAmount = (splitData["cardAmount"] as? Number)?.toDouble() ?: 0.0,
                                bankAmount = (splitData["bankAmount"] as? Number)?.toDouble() ?: 0.0,
                                onlineAmount = (splitData["onlineAmount"] as? Number)?.toDouble() ?: 0.0,
                                dueAmount = (splitData["dueAmount"] as? Number)?.toDouble() ?: 0.0,
                                totalAmount = (splitData["totalAmount"] as? Number)?.toDouble() ?: 0.0
                            )
                        }
                    )
                } catch (e: Exception) {
                    println("❌ Error parsing order document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            println("📋 ORDER REPOSITORY: Found ${orders.size} orders for customer $customerId")
            orders.sortedByDescending { it.createdAt }
            
        } catch (e: Exception) {
            println("❌ Error fetching orders for customer $customerId: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getAllOrders(): List<Order> = withContext(Dispatchers.IO) {
        try {
            println("📋 ORDER REPOSITORY: Fetching all orders")
            
            val ordersCollection = firestore.collection("orders")
            val future = ordersCollection.get()
            val snapshot = future.get()
            
            val orders = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                try {
                    Order(
                        orderId = data["orderId"] as? String ?: "",
                        customerId = data["customerId"] as? String ?: "",
                        paymentStatus = PaymentStatus.valueOf(data["paymentStatus"] as? String ?: "PENDING"),
                        subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                        discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                        discountPercent = (data["discountPercent"] as? Number)?.toDouble() ?: 0.0,
                        taxableAmount = (data["taxableAmount"] as? Number)?.toDouble() ?: 0.0,
                        gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                        totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                        finalAmount = (data["finalAmount"] as? Number)?.toDouble() ?: 0.0,
                        isGstIncluded = data["isGstIncluded"] as? Boolean ?: true,
                        status = OrderStatus.valueOf(data["status"] as? String ?: "CONFIRMED"),
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        completedAt = (data["completedAt"] as? Number)?.toLong(),
                        transactionDate = data["transactionDate"] as? String ?: "",
                        notes = data["notes"] as? String ?: "",
                        metalRatesReference = data["metalRatesReference"] as? String ?: "",
                        items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
                            if (itemData is Map<*, *>) {
                                OrderItem(
                                    productId = itemData["productId"] as? String ?: "",
                                    barcodeId = itemData["barcodeId"] as? String ?: "",
                                    quantity = (itemData["quantity"] as? Number)?.toInt() ?: 1
                                )
                            } else null
                        } ?: emptyList(),
                        paymentSplit = (data["paymentSplit"] as? Map<*, *>)?.let { splitData ->
                            PaymentSplit(
                                cashAmount = (splitData["cashAmount"] as? Number)?.toDouble() ?: 0.0,
                                cardAmount = (splitData["cardAmount"] as? Number)?.toDouble() ?: 0.0,
                                bankAmount = (splitData["bankAmount"] as? Number)?.toDouble() ?: 0.0,
                                onlineAmount = (splitData["onlineAmount"] as? Number)?.toDouble() ?: 0.0,
                                dueAmount = (splitData["dueAmount"] as? Number)?.toDouble() ?: 0.0,
                                totalAmount = (splitData["totalAmount"] as? Number)?.toDouble() ?: 0.0
                            )
                        }
                    )
                } catch (e: Exception) {
                    println("❌ Error parsing order document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            println("📋 ORDER REPOSITORY: Found ${orders.size} total orders")
            orders.sortedByDescending { it.createdAt }
            
        } catch (e: Exception) {
            println("❌ Error fetching all orders: ${e.message}")
            emptyList()
        }
    }
}
