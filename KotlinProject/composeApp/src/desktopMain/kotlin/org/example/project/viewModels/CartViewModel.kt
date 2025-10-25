package org.example.project.viewModels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.example.project.data.Cart
import org.example.project.data.CartItem
import org.example.project.data.CartRepository
import org.example.project.data.MetalRatesManager
import org.example.project.data.Product
import org.example.project.data.ProductRepository
import org.example.project.data.extractKaratFromMaterialType
import org.example.project.utils.ImageLoader
import org.example.project.JewelryAppInitializer
import org.jetbrains.skia.Image
import java.util.concurrent.ConcurrentHashMap

class CartViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val imageLoader: ImageLoader
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)


 
    // Cart state
    private val _cart = mutableStateOf(Cart())
    val cart: State<Cart> = _cart


    // Loading state
    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    // Error state
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    // Image cache for cart items - ConcurrentHashMap for thread safety
    private val imageCache = ConcurrentHashMap<String, ImageBitmap>()

    // Cart image cache state
    private val _cartImages = mutableStateOf<Map<String, ImageBitmap>>(emptyMap())
    val cartImages: State<Map<String, ImageBitmap>> = _cartImages


    fun addToCart(product: Product, selectedBarcodeIds: List<String> = emptyList()) {
        println("🛒 DEBUG: addToCart called for product: ${product.name}")
        println("   - Product ID: ${product.id}")
        println("   - Selected Barcode IDs: $selectedBarcodeIds")
        println("   - Product Quantity (from document): ${product.quantity}")
        println("   - Product Available: ${product.available}")
        println("   - Product Category: ${product.categoryId}")
        println("   - Product Material Type: ${product.materialType}")
        
        val currentCart = _cart.value
        val existingItem = currentCart.items.find { it.productId == product.id }
        val currentQuantityInCart = existingItem?.quantity ?: 0
        
        println("   - Current quantity in cart: $currentQuantityInCart")
        println("   - Existing item found: ${existingItem != null}")

        // Get inventory-based quantity instead of product.quantity
        val inventoryRepository = JewelryAppInitializer.getInventoryRepository()
        val availableInventoryCount = if (inventoryRepository != null) {
            runBlocking { inventoryRepository.getAvailableInventoryItemsByProductId(product.id).size }
        } else {
            product.quantity // Fallback to product quantity if inventory repository is not available
        }
        
        println("   - Available inventory count: $availableInventoryCount")
        println("   - Selected barcodes count: ${selectedBarcodeIds.size}")

        // Check if we can add more to cart based on inventory availability
        if (currentQuantityInCart >= availableInventoryCount) {
            println("   - ❌ Cannot add more items. Only $availableInventoryCount available in inventory.")
            _error.value = "Cannot add more items. Only $availableInventoryCount available in inventory."
            return
        }

        val existingItemIndex = currentCart.items.indexOfFirst { it.productId == product.id }
        println("   - Existing item index: $existingItemIndex")

        val updatedItems = if (existingItemIndex >= 0) {
            // Update existing item quantity
            println("   - Updating existing item quantity")
            currentCart.items.toMutableList().apply {
                this[existingItemIndex] = this[existingItemIndex].copy(
                    quantity = this[existingItemIndex].quantity + 1,
                    selectedBarcodeIds = selectedBarcodeIds
                )
            }
        } else {
            // Add new item with product's fixed weight from Firestore
            println("   - Adding new item to cart")
            val productWeight = parseWeight(product.weight)
            val currentGoldRate = MetalRatesManager.metalRates.value.getGoldRateForKarat(extractKaratFromMaterialType(product.materialType))
            println("   - Product weight: $productWeight")
            println("   - Current gold rate: $currentGoldRate")
            
            currentCart.items + CartItem(
                productId = product.id,
                product = product,
                quantity = 1,
                selectedBarcodeIds = selectedBarcodeIds,
                metal = "${extractKaratFromMaterialType(product.materialType)}K", // Set metal from product materialType
                customGoldRate = currentGoldRate, // Initialize with current gold rate
                selectedWeight = productWeight,
                grossWeight = productWeight, // Set gross weight to product weight initially
                lessWeight = 0.0, // Default to 0
                makingCharges = product.defaultMakingRate, // Initialize making charges per gram from product default
                cwWeight = 0.0, // Default carat weight
                stoneRate = 0.0, // Default stone rate
                va = 0.0, // Default value addition
                discountPercent = 0.0 // Default discount percentage
            )
        }

        println("   - Updated items count: ${updatedItems.size}")
        _cart.value = currentCart.copy(
            items = updatedItems,
            updatedAt = System.currentTimeMillis()
        )
        println("   - Cart state updated")
        println("   - New cart items count: ${_cart.value.items.size}")
        println("   - New cart item IDs: ${_cart.value.items.map { it.productId }}")
        println("   - Cart state hash: ${_cart.value.hashCode()}")

        // Clear error if successful
        _error.value = null
        println("   - ✅ Successfully added/updated product in cart")
        println("   - Final cart items count: ${_cart.value.items.size}")

        // Load image for the added product
        loadProductImage(product)
    }

    fun updateQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(productId)
            return
        }

        val currentCart = _cart.value
        val existingItemIndex = currentCart.items.indexOfFirst { it.productId == productId }

        if (existingItemIndex >= 0) {
            val cartItem = currentCart.items[existingItemIndex]

            // Check stock availability
            if (newQuantity > cartItem.product.quantity) {
                _error.value = "Cannot add ${newQuantity} items. Only ${cartItem.product.quantity} available in stock."
                return
            }

            val updatedItems = currentCart.items.toMutableList().apply {
                this[existingItemIndex] = this[existingItemIndex].copy(quantity = newQuantity)
            }

            _cart.value = currentCart.copy(
                items = updatedItems,
                updatedAt = System.currentTimeMillis()
            )

            // Clear error if successful
            _error.value = null
        }
    }

    // Add method to validate entire cart against current stock
    fun validateCartAgainstStock(products: List<Product>): List<String> {
        val errors = mutableListOf<String>()

        _cart.value.items.forEach { cartItem ->
            val currentProduct = products.find { it.id == cartItem.productId }
            if (currentProduct == null) {
                errors.add("${cartItem.product.name} is no longer available")
            } else if (cartItem.quantity > currentProduct.quantity) {
                errors.add("${cartItem.product.name}: Only ${currentProduct.quantity} available, but ${cartItem.quantity} in cart")
            }
        }

        return errors
    }

    fun removeFromCart(productId: String) {
        val currentCart = _cart.value
        val updatedItems = currentCart.items.filter { it.productId != productId }

        _cart.value = currentCart.copy(
            items = updatedItems,
            updatedAt = System.currentTimeMillis()
        )

        // Remove image from cache
        imageCache.remove(productId)
        _cartImages.value = _cartImages.value.toMutableMap().apply { remove(productId) }
    }


    // Weight is now fixed from Firestore, remove this method or make it no-op
    fun updateWeight(productId: String, newWeight: Double) {
        // Weight is fixed from Firestore, no update needed
        // This method is kept for compatibility but does nothing
        println("Weight update ignored - using fixed Firestore weight")
    }



    fun clearCart() {
        val currentCustomerId = _cart.value.customerId // Preserve customer ID
        _cart.value = Cart(customerId = currentCustomerId) // Keep customer ID when clearing
        imageCache.clear()
        _cartImages.value = emptyMap()
        println("🛒 Cart cleared but customer ID preserved: $currentCustomerId")
    }
    
    /**
     * Sets the customer ID for the current cart
     * This ensures the cart has a proper reference to the users collection
     */
    fun setCustomerId(customerId: String) {
        val currentCart = _cart.value
        _cart.value = currentCart.copy(
            customerId = customerId,
            updatedAt = System.currentTimeMillis()
        )
        println("🛒 Cart customer ID set: $customerId")
    }

    fun updateCartItem(index: Int, updatedItem: CartItem) {
        val currentCart = _cart.value
        val updatedItems = currentCart.items.toMutableList()
        if (index in updatedItems.indices) {
            updatedItems[index] = updatedItem
            _cart.value = currentCart.copy(
                items = updatedItems,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // Update existing cart items to have current gold rate if not set
    fun updateExistingCartItemsWithGoldRate() {
        // Safety check to prevent null pointer exception
        if (_cart.value.items.isEmpty()) return
        
        val currentCart = _cart.value
        val metalRates = MetalRatesManager.metalRates.value
        val updatedItems = currentCart.items.map { item ->
            if (item.customGoldRate <= 0) {
                // Extract karat from metal field or fallback to product karat
        val metalKarat = if (item.metal.isNotEmpty()) {
            item.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(item.product.materialType)
        } else {
            extractKaratFromMaterialType(item.product.materialType)
        }
                val currentGoldRate = metalRates.getGoldRateForKarat(metalKarat)
                item.copy(customGoldRate = currentGoldRate)
            } else {
                item
            }
        }
        
        if (updatedItems != currentCart.items) {
            _cart.value = currentCart.copy(
                items = updatedItems,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // Update specific fields for real-time calculation
    fun updateCartItemField(index: Int, field: String, value: String) {
        val currentCart = _cart.value
        val updatedItems = currentCart.items.toMutableList()
        if (index in updatedItems.indices) {
            val currentItem = updatedItems[index]
            val updatedItem = when (field) {
                "metal" -> currentItem.copy(metal = value)
                "customGoldRate" -> currentItem.copy(customGoldRate = value.toDoubleOrNull() ?: currentItem.customGoldRate)
                "size" -> currentItem.copy(selectedWeight = value.toDoubleOrNull() ?: currentItem.selectedWeight)
                "qty" -> currentItem.copy(quantity = value.toIntOrNull() ?: currentItem.quantity)
                "grossWeight" -> currentItem.copy(grossWeight = value.toDoubleOrNull() ?: currentItem.grossWeight)
                "lessWeight" -> currentItem.copy(lessWeight = value.toDoubleOrNull() ?: currentItem.lessWeight)
                "netWeight" -> currentItem.copy(netWeight = value.toDoubleOrNull() ?: currentItem.netWeight)
                "makingRate" -> currentItem.copy(makingCharges = value.toDoubleOrNull() ?: currentItem.makingCharges)
                "cwWeight" -> currentItem.copy(cwWeight = value.toDoubleOrNull() ?: currentItem.cwWeight)
                "stoneQuantity" -> currentItem.copy(stoneQuantity = value.toDoubleOrNull() ?: currentItem.stoneQuantity)
                "stoneRate" -> currentItem.copy(stoneRate = value.toDoubleOrNull() ?: currentItem.stoneRate)
                "vaCharges" -> currentItem.copy(va = value.toDoubleOrNull() ?: currentItem.va)
                "discountPercent" -> currentItem.copy(discountPercent = value.toDoubleOrNull() ?: currentItem.discountPercent)
                else -> currentItem
            }
            updatedItems[index] = updatedItem
            _cart.value = currentCart.copy(
                items = updatedItems,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // Calculate subtotal as sum of final amounts from all products (GST already included)
    fun getSubtotal(): Double {
        val metalRates = MetalRatesManager.metalRates.value
        return _cart.value.items.sumOf { cartItem ->
            calculateItemFinalAmount(cartItem, metalRates)
        }
    }

    // Compute GST using split rates: 3% on base amount, 5% on making charges (post-discount)
    fun getGST(): Double {
        val metalRates = MetalRatesManager.metalRates.value
        return _cart.value.items.sumOf { cartItem ->
            calculateItemSplitGST(cartItem, metalRates)
        }
    }

    // Final total is the same as subtotal since GST is already included
    fun getFinalTotal(): Double {
        return getSubtotal()
    }

    // Calculate total by summing all total charges of each product (without GST)
    fun getTotalCharges(): Double {
        val metalRates = MetalRatesManager.metalRates.value
        return _cart.value.items.sumOf { cartItem ->
            calculateItemTotalCharges(cartItem, metalRates)
        }
    }

    // Calculate individual item total charges (without GST)
    fun calculateItemTotalCharges(cartItem: CartItem, metalRates: org.example.project.data.MetalRates): Double {
        // Extract karat from metal field or fallback to product karat
        val metalKarat = if (cartItem.metal.isNotEmpty()) {
            cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(cartItem.product.materialType)
        } else {
            extractKaratFromMaterialType(cartItem.product.materialType)
        }
        
        val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else metalRates.getGoldRateForKarat(metalKarat)
        val silverRate = metalRates.getSilverRateForPurity(999) // Default to 999 purity

        // MANUAL INPUT FIELDS with sensible fallbacks to product values
        val grossWeight = if (cartItem.grossWeight > 0) cartItem.grossWeight else cartItem.product.totalWeight
        val lessWeight = if (cartItem.lessWeight > 0) cartItem.lessWeight else cartItem.product.lessWeight
        val quantity = cartItem.quantity
        val makingChargesPerGram = if (cartItem.makingCharges > 0) cartItem.makingCharges else cartItem.product.defaultMakingRate
        val cwWeight = if (cartItem.cwWeight > 0) cartItem.cwWeight else cartItem.product.cwWeight
        val stoneRate = if (cartItem.stoneRate > 0) cartItem.stoneRate else cartItem.product.stoneRate
        val stoneQuantity = if (cartItem.stoneQuantity > 0) cartItem.stoneQuantity else cartItem.product.stoneQuantity
        val vaCharges = if (cartItem.va > 0) cartItem.va else cartItem.product.vaCharges
        val discountPercent = cartItem.discountPercent

        // AUTO-CALCULATED FIELDS
        
        // 1. NT_WT (Net Weight) = GS_WT - LESS_WT
        val netWeight = grossWeight - lessWeight

        // 2. AMOUNT (Gold Value) = NT_WT × GOLD_RATE × QTY
        val baseAmount = when {
            cartItem.product.materialType.contains("gold", ignoreCase = true) -> netWeight * goldRate * quantity
            cartItem.product.materialType.contains("silver", ignoreCase = true) -> netWeight * silverRate * quantity
            else -> netWeight * goldRate * quantity // Default to gold rate
        }

        // 3. MKG (Making Charges) = NT_WT × MAKING_RATE × QTY
        val makingCharges = netWeight * makingChargesPerGram * quantity

        // 4. Stone Charges = STONE_RATE × STONE_QUANTITY × CW_WT
        val stoneAmount = stoneRate * stoneQuantity * cwWeight

        // 5. T_CHARGES (Total Charges) = AMOUNT + MKG + STONE_AMOUNT + VA_CHARGES
        val totalCharges = baseAmount + makingCharges + stoneAmount + vaCharges

        // 6. D_AMT (Discount Amount) = T_CHARGES × (D_PERCENT ÷ 100)
        val discountAmount = totalCharges * (discountPercent / 100.0)

        // 7. Return totalCharges (before discount) to match detail panel display
        return totalCharges
    }

    // Calculate individual item final amount using jewelry calculation logic
    fun calculateItemFinalAmount(cartItem: CartItem, metalRates: org.example.project.data.MetalRates): Double {
        // Extract karat from metal field or fallback to product karat
        val metalKarat = if (cartItem.metal.isNotEmpty()) {
            cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(cartItem.product.materialType)
        } else {
            extractKaratFromMaterialType(cartItem.product.materialType)
        }
        
        val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else metalRates.getGoldRateForKarat(metalKarat)
        val silverRate = metalRates.getSilverRateForPurity(999) // Default to 999 purity

        // MANUAL INPUT FIELDS with sensible fallbacks to product values
        val grossWeight = if (cartItem.grossWeight > 0) cartItem.grossWeight else cartItem.product.totalWeight
        val lessWeight = if (cartItem.lessWeight > 0) cartItem.lessWeight else cartItem.product.lessWeight
        val quantity = cartItem.quantity
        val makingChargesPerGram = if (cartItem.makingCharges > 0) cartItem.makingCharges else cartItem.product.defaultMakingRate
        val cwWeight = if (cartItem.cwWeight > 0) cartItem.cwWeight else cartItem.product.cwWeight
        val stoneRate = if (cartItem.stoneRate > 0) cartItem.stoneRate else cartItem.product.stoneRate
        val stoneQuantity = if (cartItem.stoneQuantity > 0) cartItem.stoneQuantity else cartItem.product.stoneQuantity
        val vaCharges = if (cartItem.va > 0) cartItem.va else cartItem.product.vaCharges
        val discountPercent = cartItem.discountPercent

        // AUTO-CALCULATED FIELDS
        
        // 1. NT_WT (Net Weight) = GS_WT - LESS_WT
        val netWeight = grossWeight - lessWeight

        // 2. AMOUNT (Gold Value) = NT_WT × GOLD_RATE × QTY
        val baseAmount = when {
            cartItem.product.materialType.contains("gold", ignoreCase = true) -> netWeight * goldRate * quantity
            cartItem.product.materialType.contains("silver", ignoreCase = true) -> netWeight * silverRate * quantity
            else -> netWeight * goldRate * quantity // Default to gold rate
        }

        // 3. MKG (Making Charges) = NT_WT × MAKING_RATE × QTY
        val makingCharges = netWeight * makingChargesPerGram * quantity

        // 4. Stone Charges = STONE_RATE × STONE_QUANTITY × CW_WT
        val stoneAmount = stoneRate * stoneQuantity * cwWeight

        // 5. T_CHARGES (Total Charges) = AMOUNT + MKG + STONE_AMOUNT + VA_CHARGES
        val totalCharges = baseAmount + makingCharges + stoneAmount + vaCharges

        // 6. D_AMT (Discount Amount) = T_CHARGES × (D_PERCENT ÷ 100)
        val discountAmount = totalCharges * (discountPercent / 100.0)

        // 7. TAXABLE_AMOUNT = T_CHARGES - D_AMT
        val taxableAmount = totalCharges - discountAmount

        // Split GST: apply 3% on base amount, 5% on making charges, after discount
        // Distribute discount proportionally across components
        val discountFactor = if (totalCharges > 0) (taxableAmount / totalCharges) else 1.0
        val discountedBase = baseAmount * discountFactor
        val discountedMaking = makingCharges * discountFactor

        val gstOnBase = discountedBase * 0.03
        val gstOnMaking = discountedMaking * 0.05
        val totalGst = gstOnBase + gstOnMaking

        // FINAL AMOUNT = TAXABLE_AMOUNT + TOTAL_GST
        return taxableAmount + totalGst
    }

    // Calculate split GST for a single item (3% on base, 5% on making, after discount)
    private fun calculateItemSplitGST(cartItem: CartItem, metalRates: org.example.project.data.MetalRates): Double {
        // Extract karat from metal field or fallback to product karat
        val metalKarat = if (cartItem.metal.isNotEmpty()) {
            cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(cartItem.product.materialType)
        } else {
            extractKaratFromMaterialType(cartItem.product.materialType)
        }

        val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else metalRates.getGoldRateForKarat(metalKarat)
        val silverRate = metalRates.getSilverRateForPurity(999) // Default to 999 purity

        // Input fields with fallbacks
        val grossWeight = if (cartItem.grossWeight > 0) cartItem.grossWeight else cartItem.product.totalWeight
        val lessWeight = if (cartItem.lessWeight > 0) cartItem.lessWeight else cartItem.product.lessWeight
        val quantity = cartItem.quantity
        val makingChargesPerGram = if (cartItem.makingCharges > 0) cartItem.makingCharges else cartItem.product.defaultMakingRate
        val cwWeight = if (cartItem.cwWeight > 0) cartItem.cwWeight else cartItem.product.cwWeight
        val stoneRate = if (cartItem.stoneRate > 0) cartItem.stoneRate else cartItem.product.stoneRate
        val stoneQuantity = if (cartItem.stoneQuantity > 0) cartItem.stoneQuantity else cartItem.product.stoneQuantity
        val vaCharges = if (cartItem.va > 0) cartItem.va else cartItem.product.vaCharges
        val discountPercent = cartItem.discountPercent

        val netWeight = grossWeight - lessWeight
        val baseAmount = when {
            cartItem.product.materialType.contains("gold", ignoreCase = true) -> netWeight * goldRate * quantity
            cartItem.product.materialType.contains("silver", ignoreCase = true) -> netWeight * silverRate * quantity
            else -> netWeight * goldRate * quantity
        }
        val makingCharges = netWeight * makingChargesPerGram * quantity
        val stoneAmount = stoneRate * stoneQuantity * cwWeight
        val totalCharges = baseAmount + makingCharges + stoneAmount + vaCharges
        val discountAmount = totalCharges * (discountPercent / 100.0)
        val taxableAmount = totalCharges - discountAmount

        val discountFactor = if (totalCharges > 0) (taxableAmount / totalCharges) else 1.0
        val discountedBase = baseAmount * discountFactor
        val discountedMaking = makingCharges * discountFactor

        val gstOnBase = discountedBase * 0.03
        val gstOnMaking = discountedMaking * 0.05
        return gstOnBase + gstOnMaking
    }

    // Calculate individual item subtotal using jewelry calculation logic
    private fun calculateItemSubtotal(cartItem: CartItem, metalRates: org.example.project.data.MetalRates): Double {
        val netWeight = if (cartItem.netWeight > 0) cartItem.netWeight else cartItem.selectedWeight
        val grossWeight = if (cartItem.grossWeight > 0) cartItem.grossWeight else cartItem.selectedWeight
        val lessWeight = cartItem.lessWeight
        val actualNetWeight = if (netWeight > 0) netWeight else (grossWeight - lessWeight)

        // Step 1: Base Gold/Silver Value
        val goldRate = metalRates.getGoldRateForKarat(extractKaratFromMaterialType(cartItem.product.materialType))
        val silverRate = metalRates.getSilverRateForPurity(999)
        val baseAmount = when {
            cartItem.product.materialType.contains("gold", ignoreCase = true) -> actualNetWeight * goldRate
            cartItem.product.materialType.contains("silver", ignoreCase = true) -> actualNetWeight * silverRate
            else -> actualNetWeight * goldRate
        }

        // Step 2: Making Charges (fallback to product default making rate)
        val makingChargesPerGram = if (cartItem.makingCharges > 0) cartItem.makingCharges else cartItem.product.defaultMakingRate
        val makingCharges = makingChargesPerGram * actualNetWeight

        // Step 3: Value Addition
        val valueAddition = if (cartItem.va > 0) cartItem.va else 0.0

        // Step 4: Total Charges (Before Discount & Tax)
        val totalCharges = baseAmount + makingCharges + valueAddition

        // Step 5: Discount Application
        val discountAmount = totalCharges * (cartItem.discountPercent / 100)
        val amountAfterDiscount = totalCharges - discountAmount

        // Step 6: GST Calculation (3% total: 1.5% CGST + 1.5% SGST)
        val gst = amountAfterDiscount * 0.03

        // Final Amount (After Discount + GST)
        return amountAfterDiscount + gst
    }

    // Legacy method with custom GST rate (deprecated)
    @Deprecated("Use getGST() instead - GST is now fixed at 3% for jewelry")
    fun getGST(gstRate: Double): Double {
        return getSubtotal() * (gstRate / 100)
    }

    // Legacy method with custom GST rate (deprecated)
    @Deprecated("Use getFinalTotal() instead - GST is now fixed at 3% for jewelry")
    fun getFinalTotal(gstRate: Double): Double {
        return getSubtotal() + getGST(gstRate)
    }

    private fun loadProductImage(product: Product) {
        if (product.images.isEmpty() || imageCache.containsKey(product.id)) return

        viewModelScope.launch {
            try {
                val imageUrl = product.images.first()
                val imageBytes = imageLoader.loadImage(imageUrl)

                imageBytes?.let {
                    val bitmap = withContext(Dispatchers.IO) {
                        Image.makeFromEncoded(it).toComposeImageBitmap()
                    }

                    // Store in both cache and state
                    imageCache[product.id] = bitmap
                    _cartImages.value = _cartImages.value.toMutableMap().apply {
                        put(product.id, bitmap)
                    }
                }
            } catch (e: Exception) {
                println("Error loading image for product ${product.id}: ${e.message}")
            }
        }
    }

    // Load images for all cart items
    fun loadCartImages() {
        _cart.value.items.forEach { cartItem ->
            if (!imageCache.containsKey(cartItem.productId)) {
                loadProductImage(cartItem.product)
            }
        }
    }

    // Utility function to parse weight from string
    private fun parseWeight(weightStr: String): Double {
        return try {
            // Extract numeric value from weight string (e.g., "8.5g" -> 8.5)
            weightStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }


    // Clean up resources
    fun onCleared() {
        viewModelScope.cancel()
        imageCache.clear()
    }
}