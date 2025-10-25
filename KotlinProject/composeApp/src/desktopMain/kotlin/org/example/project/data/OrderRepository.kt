package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface OrderRepository {
    suspend fun getOrdersByCustomerId(customerId: String): List<Order>
    suspend fun getAllOrders(): List<Order>
    suspend fun getOrderById(orderId: String): Order?
}

class FirestoreOrderRepository(private val firestore: Firestore) : OrderRepository {

    override suspend fun getOrdersByCustomerId(customerId: String): List<Order> = withContext(Dispatchers.IO) {
        try {
            val ordersCollection = firestore.collection("orders")
            val future = ordersCollection.whereEqualTo("customerId", customerId).get()
            val snapshot = future.get()
            
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                Order(
                    orderId = doc.id,
                    customerId = data["customerId"] as? String ?: "",
                    paymentSplit = if (data["paymentSplit"] is Map<*, *>) {
                        val paymentData = data["paymentSplit"] as Map<*, *>
                        PaymentSplit(
                            cashAmount = (paymentData["cashAmount"] as? Number)?.toDouble() ?: 0.0,
                            cardAmount = (paymentData["cardAmount"] as? Number)?.toDouble() ?: 0.0,
                            bankAmount = (paymentData["bankAmount"] as? Number)?.toDouble() ?: 0.0,
                            onlineAmount = (paymentData["onlineAmount"] as? Number)?.toDouble() ?: 0.0,
                            dueAmount = (paymentData["dueAmount"] as? Number)?.toDouble() ?: 0.0,
                            totalAmount = (paymentData["totalAmount"] as? Number)?.toDouble() ?: 0.0
                        )
                    } else null,
                    paymentStatus = PaymentStatus.valueOf(data["paymentStatus"] as? String ?: PaymentStatus.PENDING.name),
                    subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                    discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                    discountPercent = (data["discountPercent"] as? Number)?.toDouble() ?: 0.0,
                    taxableAmount = (data["taxableAmount"] as? Number)?.toDouble() ?: 0.0,
                    gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                    totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                    finalAmount = (data["finalAmount"] as? Number)?.toDouble() ?: 0.0,
                    isGstIncluded = data["isGstIncluded"] as? Boolean ?: false,
                    items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
                        if (itemData is Map<*, *>) {
                            OrderItem(
                                productId = itemData["productId"] as? String ?: "",
                                barcodeId = itemData["barcodeId"] as? String ?: "",
                                quantity = (itemData["quantity"] as? Number)?.toInt() ?: 1,
                                defaultMakingRate = (itemData["defaultMakingRate"] as? Number)?.toDouble() ?: 0.0,
                                vaCharges = (itemData["vaCharges"] as? Number)?.toDouble() ?: 0.0,
                                materialType = itemData["materialType"] as? String ?: ""
                            )
                        } else null
                    } ?: emptyList(),
                    metalRatesReference = data["metalRatesReference"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    completedAt = (data["completedAt"] as? Number)?.toLong(),
                    transactionDate = data["transactionDate"] as? String ?: "",
                    notes = data["notes"] as? String ?: "",
                    status = OrderStatus.valueOf(data["status"] as? String ?: OrderStatus.CONFIRMED.name)
                )
            }
        } catch (e: Exception) {
            println("Error fetching orders for customer: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getAllOrders(): List<Order> = withContext(Dispatchers.IO) {
        try {
            val ordersCollection = firestore.collection("orders")
            val future = ordersCollection.get()
            val snapshot = future.get()
            
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                Order(
                    orderId = doc.id,
                    customerId = data["customerId"] as? String ?: "",
                    paymentSplit = if (data["paymentSplit"] is Map<*, *>) {
                        val paymentData = data["paymentSplit"] as Map<*, *>
                        PaymentSplit(
                            cashAmount = (paymentData["cashAmount"] as? Number)?.toDouble() ?: 0.0,
                            cardAmount = (paymentData["cardAmount"] as? Number)?.toDouble() ?: 0.0,
                            bankAmount = (paymentData["bankAmount"] as? Number)?.toDouble() ?: 0.0,
                            onlineAmount = (paymentData["onlineAmount"] as? Number)?.toDouble() ?: 0.0,
                            dueAmount = (paymentData["dueAmount"] as? Number)?.toDouble() ?: 0.0,
                            totalAmount = (paymentData["totalAmount"] as? Number)?.toDouble() ?: 0.0
                        )
                    } else null,
                    paymentStatus = PaymentStatus.valueOf(data["paymentStatus"] as? String ?: PaymentStatus.PENDING.name),
                    subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                    discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                    discountPercent = (data["discountPercent"] as? Number)?.toDouble() ?: 0.0,
                    taxableAmount = (data["taxableAmount"] as? Number)?.toDouble() ?: 0.0,
                    gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                    totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                    finalAmount = (data["finalAmount"] as? Number)?.toDouble() ?: 0.0,
                    isGstIncluded = data["isGstIncluded"] as? Boolean ?: false,
                    items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
                        if (itemData is Map<*, *>) {
                            OrderItem(
                                productId = itemData["productId"] as? String ?: "",
                                barcodeId = itemData["barcodeId"] as? String ?: "",
                                quantity = (itemData["quantity"] as? Number)?.toInt() ?: 1,
                                defaultMakingRate = (itemData["defaultMakingRate"] as? Number)?.toDouble() ?: 0.0,
                                vaCharges = (itemData["vaCharges"] as? Number)?.toDouble() ?: 0.0,
                                materialType = itemData["materialType"] as? String ?: ""
                            )
                        } else null
                    } ?: emptyList(),
                    metalRatesReference = data["metalRatesReference"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    completedAt = (data["completedAt"] as? Number)?.toLong(),
                    transactionDate = data["transactionDate"] as? String ?: "",
                    notes = data["notes"] as? String ?: "",
                    status = OrderStatus.valueOf(data["status"] as? String ?: OrderStatus.CONFIRMED.name)
                )
            }
        } catch (e: Exception) {
            println("Error fetching all orders: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getOrderById(orderId: String): Order? = withContext(Dispatchers.IO) {
        try {
            val orderDoc = firestore.collection("orders").document(orderId).get().get()
            
            if (!orderDoc.exists()) {
                println("Order with ID $orderId not found")
                return@withContext null
            }
            
            val data = orderDoc.data ?: return@withContext null
            
            Order(
                orderId = orderDoc.id,
                customerId = data["customerId"] as? String ?: "",
                paymentSplit = if (data["paymentSplit"] is Map<*, *>) {
                    val paymentData = data["paymentSplit"] as Map<*, *>
                    PaymentSplit(
                        cashAmount = (paymentData["cashAmount"] as? Number)?.toDouble() ?: 0.0,
                        cardAmount = (paymentData["cardAmount"] as? Number)?.toDouble() ?: 0.0,
                        bankAmount = (paymentData["bankAmount"] as? Number)?.toDouble() ?: 0.0,
                        onlineAmount = (paymentData["onlineAmount"] as? Number)?.toDouble() ?: 0.0,
                        dueAmount = (paymentData["dueAmount"] as? Number)?.toDouble() ?: 0.0,
                        totalAmount = (paymentData["totalAmount"] as? Number)?.toDouble() ?: 0.0
                    )
                } else null,
                paymentStatus = PaymentStatus.valueOf(data["paymentStatus"] as? String ?: PaymentStatus.PENDING.name),
                subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                discountPercent = (data["discountPercent"] as? Number)?.toDouble() ?: 0.0,
                taxableAmount = (data["taxableAmount"] as? Number)?.toDouble() ?: 0.0,
                gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                finalAmount = (data["finalAmount"] as? Number)?.toDouble() ?: 0.0,
                isGstIncluded = data["isGstIncluded"] as? Boolean ?: false,
                items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
                    if (itemData is Map<*, *>) {
                        OrderItem(
                            productId = itemData["productId"] as? String ?: "",
                            barcodeId = itemData["barcodeId"] as? String ?: "",
                            quantity = (itemData["quantity"] as? Number)?.toInt() ?: 1,
                            defaultMakingRate = (itemData["defaultMakingRate"] as? Number)?.toDouble() ?: 0.0,
                            vaCharges = (itemData["vaCharges"] as? Number)?.toDouble() ?: 0.0,
                            materialType = itemData["materialType"] as? String ?: ""
                        )
                    } else null
                } ?: emptyList(),
                metalRatesReference = data["metalRatesReference"] as? String ?: "",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                completedAt = (data["completedAt"] as? Number)?.toLong(),
                transactionDate = data["transactionDate"] as? String ?: "",
                notes = data["notes"] as? String ?: "",
                status = OrderStatus.valueOf(data["status"] as? String ?: OrderStatus.CONFIRMED.name)
            )
        } catch (e: Exception) {
            println("Error fetching order by ID $orderId: ${e.message}")
            null
        }
    }
}