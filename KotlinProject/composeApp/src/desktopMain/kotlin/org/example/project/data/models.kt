package org.example.project.data

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val categoryId: String = "",
    val materialId: String = "",
    val materialType: String = "",
    val gender: String = "",
    val weight: String = "",
    val available: Boolean = true,
    val featured: Boolean = false,
    val images: List<String> = emptyList(),
    val quantity: Int = 0, // Add this line
    val createdAt: Long = System.currentTimeMillis()
)

data class Category(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val hasGenderVariants: Boolean = false,
    val order: Int = 0
)

data class Material(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val types: List<String> = emptyList()
)

// User.kt

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
)


data class CartItem(
    val productId: String,
    val product: Product,
    val quantity: Int = 1,
    val selectedWeight: Double = 0.0, // in grams
    val addedAt: Long = System.currentTimeMillis()
)

data class Cart(
    val items: List<CartItem> = emptyList(),
    val customerId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalItems: Int get() = items.sumOf { it.quantity }
    val totalWeight: Double get() = items.sumOf { it.selectedWeight * it.quantity }

    fun calculateTotalPrice(goldPricePerGram: Double, silverPricePerGram: Double): Double {
        return items.sumOf { item ->
            val pricePerGram = when {
                item.product.materialType.contains("gold", ignoreCase = true) -> goldPricePerGram
                item.product.materialType.contains("silver", ignoreCase = true) -> silverPricePerGram
                else -> goldPricePerGram // Default to gold price
            }
            item.selectedWeight * item.quantity * pricePerGram
        }
    }
}

data class MetalPrices(
    val goldPricePerGram: Double = 6080.0, // Current gold price per gram in Rs
    val silverPricePerGram: Double = 75.0,  // Current silver price per gram in Rs
    val lastUpdated: Long = System.currentTimeMillis()
)



// Add these to your existing models.kt file

enum class PaymentMethod {
    CARD,
    UPI,
    NET_BANKING,
    CASH_ON_DELIVERY
}

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REFUNDED
}

enum class OrderStatus {
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}

enum class DiscountType {
    AMOUNT,
    PERCENTAGE
}

data class Order(
    val id: String = "",
    val customerId: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val subtotal: Double = 0.0, // Metal cost only
    val makingCharges: Double = 0.0, // Making charges
    val discountAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0, // Final total
    val status: OrderStatus = OrderStatus.CONFIRMED,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<CartItem> = emptyList(),
    val isGstIncluded: Boolean = false
)

data class PaymentTransaction(
    val id: String = "",
    val cartId: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val subtotal: Double = 0.0, // Metal cost only
    val makingCharges: Double = 0.0, // Making charges
    val discountAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0, // Final total
    val status: PaymentStatus = PaymentStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<CartItem> = emptyList(),
    val isGstIncluded: Boolean = false
)