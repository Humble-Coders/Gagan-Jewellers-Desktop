package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PaymentRepository {
    suspend fun saveOrder(order: Order): Boolean
    suspend fun getOrderById(orderId: String): Order?
    suspend fun getOrdersByCustomer(customerId: String): List<Order>
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Boolean
}

class FirestorePaymentRepository(private val firestore: Firestore) : PaymentRepository {

    override suspend fun saveOrder(order: Order): Boolean = withContext(Dispatchers.IO) {
        try {
            val orderMap = mapOf(
                "id" to order.id,
                "customerId" to order.customerId,
                "paymentMethod" to order.paymentMethod.name,
                "subtotal" to order.subtotal,
                "discountAmount" to order.discountAmount,
                "gstAmount" to order.gstAmount,
                "totalAmount" to order.totalAmount,
                "status" to order.status.name,
                "timestamp" to order.timestamp,
                "isGstIncluded" to order.isGstIncluded, // Add the new field
                "items" to order.items.map { item ->
                    mapOf(
                        "productId" to item.productId,
                        "productName" to item.product.name,
                        "quantity" to item.quantity,
                        "price" to item.product.price,
                        "weight" to item.selectedWeight
                    )
                }
            )

            firestore.collection("orders")
                .document(order.id)
                .set(orderMap)
                .get()

            true
        } catch (e: Exception) {
            println("Error saving order: ${e.message}")
            false
        }
    }

    override suspend fun getOrderById(orderId: String): Order? = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("orders").document(orderId)
            val document = docRef.get().get()

            if (document.exists()) {
                val data = document.data ?: return@withContext null

                Order(
                    id = data["id"] as? String ?: "",
                    customerId = data["customerId"] as? String ?: "",
                    paymentMethod = PaymentMethod.valueOf(data["paymentMethod"] as? String ?: "CARD"),
                    subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                    discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                    gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                    totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                    status = OrderStatus.valueOf(data["status"] as? String ?: "CONFIRMED"),
                    timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isGstIncluded = data["isGstIncluded"] as? Boolean ?: false, // Handle the new field
                    items = emptyList() // Items would need to be reconstructed if needed
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error fetching order: ${e.message}")
            null
        }
    }

    override suspend fun getOrdersByCustomer(customerId: String): List<Order> = withContext(Dispatchers.IO) {
        try {
            val querySnapshot = firestore.collection("orders")
                .whereEqualTo("customerId", customerId)
                .orderBy("timestamp", com.google.cloud.firestore.Query.Direction.DESCENDING)
                .get()
                .get()

            querySnapshot.documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null

                Order(
                    id = data["id"] as? String ?: "",
                    customerId = data["customerId"] as? String ?: "",
                    paymentMethod = PaymentMethod.valueOf(data["paymentMethod"] as? String ?: "CARD"),
                    subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                    discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                    gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                    totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                    status = OrderStatus.valueOf(data["status"] as? String ?: "CONFIRMED"),
                    timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isGstIncluded = data["isGstIncluded"] as? Boolean ?: false, // Handle the new field
                    items = emptyList() // Items would need to be reconstructed if needed
                )
            }
        } catch (e: Exception) {
            println("Error fetching customer orders: ${e.message}")
            emptyList()
        }
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("orders")
                .document(orderId)
                .update("status", status.name)
                .get()

            true
        } catch (e: Exception) {
            println("Error updating order status: ${e.message}")
            false
        }
    }
}