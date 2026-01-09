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
                try {
                    val data = doc.data
                    if (data == null) {
                        println("⚠️ ORDER REPOSITORY: Document ${doc.id} has null data, skipping")
                        return@mapNotNull null
                    }
                
                Order(
                    orderId = doc.id,
                    customerId = data["customerId"] as? String ?: "",
                    paymentSplit = (data["paymentSplit"] as? Map<*, *>)?.let { paymentData ->
                        // Support both old and new format for backward compatibility
                        val bank = if (paymentData.containsKey("bank")) {
                            (paymentData["bank"] as? Number)?.toDouble() ?: 0.0
                        } else {
                            // Old format: sum bankAmount + cardAmount + onlineAmount
                            ((paymentData["bankAmount"] as? Number)?.toDouble() ?: 0.0) +
                            ((paymentData["cardAmount"] as? Number)?.toDouble() ?: 0.0) +
                            ((paymentData["onlineAmount"] as? Number)?.toDouble() ?: 0.0)
                        }
                        val cash = if (paymentData.containsKey("cash")) {
                            (paymentData["cash"] as? Number)?.toDouble() ?: 0.0
                        } else {
                            (paymentData["cashAmount"] as? Number)?.toDouble() ?: 0.0
                        }
                        PaymentSplit(
                            bank = bank,
                            cash = cash,
                            dueAmount = (paymentData["dueAmount"] as? Number)?.toDouble() ?: 0.0
                        )
                    },
                    totalProductValue = (data["totalProductValue"] as? Number)?.toDouble() ?: (data["finalAmount"] as? Number)?.toDouble() ?: 0.0,
                    discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                    discountPercent = (data["discountPercent"] as? Number)?.toDouble()?.takeIf { it > 0 },
                    gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                    gstPercentage = (data["gstPercentage"] as? Number)?.toDouble() ?: 0.0,
                    totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                    isGstIncluded = data["isGstIncluded"] as? Boolean ?: false,
                    items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
                        if (itemData is Map<*, *>) {
                            OrderItem(
                                barcodeId = itemData["barcodeId"] as? String ?: "",
                                productId = itemData["productId"] as? String ?: "",
                                quantity = (itemData["quantity"] as? Number)?.toInt() ?: 1,
                                makingPercentage = (itemData["makingPercentage"] as? Number)?.toDouble() ?: 0.0,
                                labourCharges = (itemData["labourCharges"] as? Number)?.toDouble() ?: 0.0,
                                labourRate = (itemData["labourRate"] as? Number)?.toDouble() ?: 0.0
                            )
                        } else null
                    } ?: emptyList(),
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    transactionDate = data["transactionDate"] as? String ?: "",
                    notes = data["notes"] as? String ?: ""
                )
                } catch (e: Exception) {
                    println("⚠️ ORDER REPOSITORY: Error parsing order document ${doc.id}: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
        } catch (e: Exception) {
            println("❌ ORDER REPOSITORY: Error fetching orders for customer $customerId: ${e.message}")
            e.printStackTrace()
            // Return empty list but log the error for debugging
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
                    paymentSplit = (data["paymentSplit"] as? Map<*, *>)?.let { paymentData ->
                        // Support both old and new format for backward compatibility
                        val bank = if (paymentData.containsKey("bank")) {
                            (paymentData["bank"] as? Number)?.toDouble() ?: 0.0
                        } else {
                            // Old format: sum bankAmount + cardAmount + onlineAmount
                            ((paymentData["bankAmount"] as? Number)?.toDouble() ?: 0.0) +
                            ((paymentData["cardAmount"] as? Number)?.toDouble() ?: 0.0) +
                            ((paymentData["onlineAmount"] as? Number)?.toDouble() ?: 0.0)
                        }
                        val cash = if (paymentData.containsKey("cash")) {
                            (paymentData["cash"] as? Number)?.toDouble() ?: 0.0
                        } else {
                            (paymentData["cashAmount"] as? Number)?.toDouble() ?: 0.0
                        }
                        PaymentSplit(
                            bank = bank,
                            cash = cash,
                            dueAmount = (paymentData["dueAmount"] as? Number)?.toDouble() ?: 0.0
                        )
                    },
                    totalProductValue = (data["totalProductValue"] as? Number)?.toDouble() ?: (data["finalAmount"] as? Number)?.toDouble() ?: 0.0,
                    discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                    discountPercent = (data["discountPercent"] as? Number)?.toDouble()?.takeIf { it > 0 },
                    gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                    gstPercentage = (data["gstPercentage"] as? Number)?.toDouble() ?: 0.0,
                    totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                    isGstIncluded = data["isGstIncluded"] as? Boolean ?: false,
                    items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
                        if (itemData is Map<*, *>) {
                            OrderItem(
                                barcodeId = itemData["barcodeId"] as? String ?: "",
                                productId = itemData["productId"] as? String ?: "",
                                quantity = (itemData["quantity"] as? Number)?.toInt() ?: 1,
                                makingPercentage = (itemData["makingPercentage"] as? Number)?.toDouble() ?: 0.0,
                                labourCharges = (itemData["labourCharges"] as? Number)?.toDouble() ?: 0.0,
                                labourRate = (itemData["labourRate"] as? Number)?.toDouble() ?: 0.0
                            )
                        } else null
                    } ?: emptyList(),
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    transactionDate = data["transactionDate"] as? String ?: "",
                    notes = data["notes"] as? String ?: ""
                )
            }
        } catch (e: Exception) {
            println("Error fetching all orders: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getOrderById(orderId: String): Order? = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("orders").document(orderId)
            val snapshot = docRef.get().get()
            if (!snapshot.exists()) return@withContext null

            val data = snapshot.data ?: return@withContext null

            Order(
                orderId = snapshot.id,
                customerId = data["customerId"] as? String ?: "",
                paymentSplit = (data["paymentSplit"] as? Map<*, *>)?.let { paymentData ->
                    // Support both old and new format for backward compatibility
                    val bank = if (paymentData.containsKey("bank")) {
                        (paymentData["bank"] as? Number)?.toDouble() ?: 0.0
                    } else {
                        // Old format: sum bankAmount + cardAmount + onlineAmount
                        ((paymentData["bankAmount"] as? Number)?.toDouble() ?: 0.0) +
                        ((paymentData["cardAmount"] as? Number)?.toDouble() ?: 0.0) +
                        ((paymentData["onlineAmount"] as? Number)?.toDouble() ?: 0.0)
                    }
                    val cash = if (paymentData.containsKey("cash")) {
                        (paymentData["cash"] as? Number)?.toDouble() ?: 0.0
                    } else {
                        (paymentData["cashAmount"] as? Number)?.toDouble() ?: 0.0
                    }
                    PaymentSplit(
                        bank = bank,
                        cash = cash,
                        dueAmount = (paymentData["dueAmount"] as? Number)?.toDouble() ?: 0.0
                    )
                },
                totalProductValue = (data["totalProductValue"] as? Number)?.toDouble() ?: (data["finalAmount"] as? Number)?.toDouble() ?: 0.0,
                discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                discountPercent = (data["discountPercent"] as? Number)?.toDouble()?.takeIf { it > 0 },
                gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                gstPercentage = (data["gstPercentage"] as? Number)?.toDouble() ?: 0.0,
                totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                isGstIncluded = data["isGstIncluded"] as? Boolean ?: false,
                items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
                    if (itemData is Map<*, *>) {
                        OrderItem(
                            barcodeId = itemData["barcodeId"] as? String ?: "",
                            productId = itemData["productId"] as? String ?: "",
                            quantity = (itemData["quantity"] as? Number)?.toInt() ?: 1,
                            makingPercentage = (itemData["makingPercentage"] as? Number)?.toDouble() ?: 0.0,
                            labourCharges = (itemData["labourCharges"] as? Number)?.toDouble() ?: 0.0,
                            labourRate = (itemData["labourRate"] as? Number)?.toDouble() ?: 0.0
                        )
                    } else null
                } ?: emptyList(),
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                transactionDate = data["transactionDate"] as? String ?: "",
                notes = data["notes"] as? String ?: ""
            )
        } catch (e: Exception) {
            println("Error fetching order by ID: ${e.message}")
            null
        }
    }
}