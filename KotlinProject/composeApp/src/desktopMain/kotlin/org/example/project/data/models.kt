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
    val karat: Int = 22, // New field for gold karat (22k, 20k, 18k, etc.)
    val makingCharges: Double = 0.0, // New field for making charges per gram
    val available: Boolean = true,
    val featured: Boolean = false,
    val images: List<String> = emptyList(),
    val barcodeIds: List<String> = emptyList(), // Unique barcodes for each unit
    val quantity: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    // New fields for enhanced product details
    val autoGenerateId: Boolean = true, // Radio button for auto-generating product ID
    val customProductId: String = "", // Custom product ID when auto-generate is false
    val totalWeight: Double = 0.0, // Total weight in grams
    val defaultMakingRate: Double = 0.0, // Default making rate per gram
    val isOtherThanGold: Boolean = false, // Radio button for other than gold
    val lessWeight: Double = 0.0, // Less weight in grams
    val hasStones: Boolean = false, // Radio button for has stones
    val stoneName: String = "",
    val stoneQuantity: Double = 0.0, // Quantity of stones
    val stoneRate: Double = 0.0, // Rate per carat or unit for stones (stored on product)
    val cwWeight: Double = 0.0, // Carat weight of stones
    val stoneAmount: Double = 0.0, // Calculated stone amount: CW_WT x STONE_RATE × QTY
    val vaCharges: Double = 0.0, // VA charges
    val netWeight: Double = 0.0, // Calculated net weight (totalWeight - lessWeight)
    val totalProductCost: Double = 0.0, // Calculated total product cost
    val hasCustomPrice: Boolean = false, // Checkbox for custom price
    val customPrice: Double = 0.0, // Custom price value when hasCustomPrice is true
    // Field-level visibility configuration
    val show: ProductShowConfig = ProductShowConfig()
)

data class ProductShowConfig(
    val name: Boolean = true,
    val description: Boolean = true,
    val category: Boolean = true,
    val material: Boolean = true,
    val materialType: Boolean = true,
    val quantity: Boolean = true,
    val totalWeight: Boolean = true,
    val price: Boolean = true,
    val defaultMakingRate: Boolean = true,
    val vaCharges: Boolean = true,
    val isOtherThanGold: Boolean = true,
    val lessWeight: Boolean = true,
    val hasStones: Boolean = true,
    val stoneName: Boolean = true,
    val stoneQuantity: Boolean = true,
    val stoneRate: Boolean = true,
    val cwWeight: Boolean = true,
    val stoneAmount: Boolean = true,
    val netWeight: Boolean = true,
    val totalProductCost: Boolean = true,
    val customPrice: Boolean = true,
    val images: Boolean = true,
    val available: Boolean = true,
    val featured: Boolean = true
)

data class Category(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val hasGenderVariants: Boolean = false,
    val order: Int = 0,
    val categoryType: CategoryType = CategoryType.JEWELRY, // New field for better categorization
    val isActive: Boolean = true, // New field to enable/disable categories
    val parentCategoryId: String? = null // New field for hierarchical categories
)

enum class CategoryType {
    RAW_GOLD,      // Raw gold materials
    FINE_GOLD,     // Fine gold jewelry
    CRYSTALS,      // Crystal jewelry
    SILVER,        // Silver jewelry
    DIAMONDS,      // Diamond jewelry
    GEMSTONES,     // Other gemstones
    JEWELRY,       // General jewelry
    ACCESSORIES,   // Jewelry accessories
    REPAIR,        // Repair services
    OTHER          // Other categories
}

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
    val metal: String = "", // Metal type (22K, 18K, etc.)
    val customGoldRate: Double = 0.0, // Custom gold rate per gram
    val selectedWeight: Double = 0.0, // in grams
    val grossWeight: Double = 0.0, // Gross weight in grams
    val lessWeight: Double = 0.0, // Less weight in grams
    val netWeight: Double = 0.0, // Net weight in grams (auto-calculated)
    val makingCharges: Double = 0.0, // Making charges per gram
    val totalMakingCharges: Double = 0.0, // Total making charges (auto-calculated)
    val cwWeight: Double = 0.0, // Carat weight
    val stoneRate: Double = 0.0, // Stone rate per carat
    val stoneAmount: Double = 0.0, // Stone amount (auto-calculated)
    val va: Double = 0.0, // Value addition
    val amount: Double = 0.0, // Base amount (auto-calculated)
    val discountPercent: Double = 0.0, // Discount percentage
    val discountAmount: Double = 0.0, // Discount amount (auto-calculated)
    val totalCharges: Double = 0.0, // Total charges (auto-calculated)
    val taxableAmount: Double = 0.0, // Taxable amount (auto-calculated)
    val cgst: Double = 0.0, // CGST (auto-calculated)
    val sgst: Double = 0.0, // SGST (auto-calculated)
    val igst: Double = 0.0, // IGST (auto-calculated)
    val totalGst: Double = 0.0, // Total GST (auto-calculated)
    val finalAmount: Double = 0.0, // Final amount (auto-calculated)
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

    // Enhanced calculation with karat-specific rates and making charges
    fun calculateTotalPriceWithKarat(goldRates: GoldRates, silverPricePerGram: Double): Double {
        return items.sumOf { item ->
            when {
                item.product.materialType.contains("gold", ignoreCase = true) -> {
                    val goldRate = when (item.product.karat) {
                        24 -> goldRates.rate24k
                        22 -> goldRates.rate22k
                        20 -> goldRates.rate20k
                        18 -> goldRates.rate18k
                        14 -> goldRates.rate14k
                        10 -> goldRates.rate10k
                        else -> goldRates.rate22k // Default to 22k
                    }
                    val goldValue = item.selectedWeight * item.quantity * goldRate
                    val makingCharges = item.selectedWeight * item.quantity * item.product.makingCharges
                    goldValue + makingCharges
                }
                item.product.materialType.contains("silver", ignoreCase = true) -> {
                    item.selectedWeight * item.quantity * silverPricePerGram
                }
                else -> {
                    // Default to 22k gold rate
                    val goldValue = item.selectedWeight * item.quantity * goldRates.rate22k
                    val makingCharges = item.selectedWeight * item.quantity * item.product.makingCharges
                    goldValue + makingCharges
                }
            }
        }
    }
}

data class MetalPrices(
    val goldPricePerGram: Double = 6080.0, // Current gold price per gram in Rs
    val silverPricePerGram: Double = 75.0,  // Current silver price per gram in Rs
    val lastUpdated: Long = System.currentTimeMillis()
)

// Enhanced gold rate system with different karats
data class GoldRates(
    val rate24k: Double = 6080.0, // 24k gold rate (base rate)
    val rate22k: Double = 0.0,    // 22k gold rate (calculated from 24k)
    val rate20k: Double = 0.0,    // 20k gold rate (calculated from 24k)
    val rate18k: Double = 0.0,    // 18k gold rate (calculated from 24k)
    val rate14k: Double = 0.0,    // 14k gold rate (calculated from 24k)
    val rate10k: Double = 0.0,    // 10k gold rate (calculated from 24k)
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        fun calculateKaratRates(rate24k: Double): GoldRates {
            return GoldRates(
                rate24k = rate24k,
                rate22k = rate24k * 0.9167, // 22/24 = 0.9167
                rate20k = rate24k * 0.8333, // 20/24 = 0.8333
                rate18k = rate24k * 0.7500, // 18/24 = 0.7500
                rate14k = rate24k * 0.5833, // 14/24 = 0.5833
                rate10k = rate24k * 0.4167, // 10/24 = 0.4167
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
}

// Silver rates system
data class SilverRates(
    val rate999: Double = 75.0,   // 999 silver rate (base rate)
    val rate925: Double = 0.0,    // 925 silver rate (calculated from 999)
    val rate900: Double = 0.0,    // 900 silver rate (calculated from 999)
    val rate800: Double = 0.0,    // 800 silver rate (calculated from 999)
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        fun calculateSilverRates(rate999: Double): SilverRates {
            return SilverRates(
                rate999 = rate999,
                rate925 = rate999 * 0.925, // 925/999 = 0.925
                rate900 = rate999 * 0.900, // 900/999 = 0.900
                rate800 = rate999 * 0.800, // 800/999 = 0.800
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
}

// Combined rates system for universal access
data class MetalRates(
    val goldRates: GoldRates = GoldRates(),
    val silverRates: SilverRates = SilverRates(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getGoldRateForKarat(karat: Int): Double {
        return when (karat) {
            24 -> goldRates.rate24k
            22 -> goldRates.rate22k
            20 -> goldRates.rate20k
            18 -> goldRates.rate18k
            14 -> goldRates.rate14k
            10 -> goldRates.rate10k
            else -> goldRates.rate22k // Default to 22k
        }
    }
    
    fun getSilverRateForPurity(purity: Int): Double {
        return when (purity) {
            999 -> silverRates.rate999
            925 -> silverRates.rate925
            900 -> silverRates.rate900
            800 -> silverRates.rate800
            else -> silverRates.rate999 // Default to 999
        }
    }
}




// Add these to your existing models.kt file

enum class PaymentMethod {
    CASH,
    CARD,
    UPI,
    NET_BANKING,
    BANK_TRANSFER,
    DUE
}

// Enhanced payment splitting system
data class PaymentSplit(
    val cashAmount: Double = 0.0,
    val cardAmount: Double = 0.0,
    val bankAmount: Double = 0.0,
    val onlineAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val totalAmount: Double = 0.0
) {
    fun isValid(): Boolean {
        return kotlin.math.abs((cashAmount + cardAmount + bankAmount + onlineAmount + dueAmount) - totalAmount) < 0.01
    }
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
    PERCENTAGE,
    TOTAL_PAYABLE // New: Enter total payable amount, system calculates discount
}

data class Order(
    val id: String = "",
    val customerId: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val paymentSplit: PaymentSplit? = null, // New: Detailed payment splitting
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: OrderStatus = OrderStatus.CONFIRMED,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<CartItem> = emptyList(),
    val goldRateUsed: GoldRates? = null // Track which gold rates were used
)

data class PaymentTransaction(
    val id: String = "",
    val cartId: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<CartItem> = emptyList()
)

// Invoice customization models
data class MetalRate(
    val id: String = "",
    val materialId: String = "",
    val materialName: String = "",
    val materialType: String = "",
    val karat: Int = 24, // Base karat (24K)
    val pricePerGram: Double = 0.0, // Price per gram for the base karat
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val previousRate: RateHistory? = null // Single previous rate
)

data class RateHistory(
    val pricePerGram: Double,
    val updatedAt: Long,
    val updatedBy: String = "system"
)

// Helper function to calculate rate based on karat
fun MetalRate.calculateRateForKarat(targetKarat: Int): Double {
    if (targetKarat <= 0 || karat <= 0) return 0.0
    return pricePerGram * (targetKarat.toDouble() / karat.toDouble())
}

data class InvoiceConfig(
    val id: String = "default",
    val companyName: String = "Gagan Jewellers",
    val companyAddress: String = "",
    val companyPhone: String = "",
    val companyEmail: String = "",
    val companyGST: String = "",
    val showCustomerDetails: Boolean = true,
    val showGoldRate: Boolean = true,
    val showMakingCharges: Boolean = true,
    val showPaymentBreakup: Boolean = true,
    val showItemDetails: Boolean = true,
    val showTermsAndConditions: Boolean = true,
    val termsAndConditions: String = "Thank you for your business!",
    val logoUrl: String = "",
    val footerText: String = "Visit us again!",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class InvoiceField(
    val fieldName: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val isRequired: Boolean = false,
    val order: Int = 0
)

data class ExchangeGold(
    val id: String = "",
    val type: String = "GOLD", // TYPE
    val firm: String = "L", // FIRM
    val account: String = "RAW GOLD", // ACCOUNT
    val description: String = "OldGold", // DESC
    val name: String = "OLD GOLD", // NAME
    val grossWeight: Double = 0.0, // GS WT
    val lessWeight: Double = 0.0, // LESS WT
    val netWeight: Double = 0.0, // NT WT
    val tunch: Double = 0.0, // TUNCH
    val fineWeight: Double = 0.0, // FINE WT
    val laborWeight: Double = 0.0, // LBR WT
    val ffnWeight: Double = 0.0, // FFN WT
    val rate: Double = 0.0, // RATE
    val value: Double = 0.0, // VAL
    val averageRate: Double = 0.0, // AVG RATE
    val createdAt: Long = System.currentTimeMillis()
) {
    fun calculateValue(): Double {
        return fineWeight * rate
    }
    
    fun calculateNetWeight(): Double {
        return grossWeight - lessWeight
    }
    
    fun calculateFineWeight(): Double {
        return netWeight * (tunch / 100.0)
    }
}