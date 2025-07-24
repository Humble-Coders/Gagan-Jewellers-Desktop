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
import kotlinx.coroutines.withContext
import org.example.project.data.Cart
import org.example.project.data.CartItem
import org.example.project.data.CartRepository
import org.example.project.data.MetalPrices
import org.example.project.data.Product
import org.example.project.data.ProductRepository
import org.example.project.utils.ImageLoader
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

    // Metal prices state
    private val _metalPrices = mutableStateOf(MetalPrices())
    val metalPrices: State<MetalPrices> = _metalPrices

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

    init {
        loadMetalPrices()
    }

    private fun loadMetalPrices() {
        viewModelScope.launch {
            try {
                _metalPrices.value = cartRepository.getMetalPrices()
            } catch (e: Exception) {
                _error.value = "Failed to load metal prices: ${e.message}"
            }
        }
    }

    // Calculate gross total (subtotal + making charges)
    fun getGrossTotal(): Double {
        return getSubtotal() + getMakingCharges()
    }

    // Calculate 18% GST on gross total
    fun getGST(): Double {
        return getGrossTotal() * 0.18 // 18% GST
    }

    // Calculate final total
    fun getFinalTotal(): Double {
        return getGrossTotal() + getGST()
    }

    // Get total weight of all items
    fun getTotalWeight(): Double {
        return _cart.value.items.sumOf { cartItem ->
            val weight = parseWeight(cartItem.product.weight)
            weight * cartItem.quantity
        }
    }



    fun addToCart(product: Product) {
        val currentCart = _cart.value
        val existingItem = currentCart.items.find { it.productId == product.id }
        val currentQuantityInCart = existingItem?.quantity ?: 0

        // Check if we can add more to cart
        if (currentQuantityInCart >= product.quantity) {
            _error.value = "Cannot add more items. Only ${product.quantity} available in stock."
            return
        }

        val existingItemIndex = currentCart.items.indexOfFirst { it.productId == product.id }

        val updatedItems = if (existingItemIndex >= 0) {
            // Update existing item quantity
            currentCart.items.toMutableList().apply {
                this[existingItemIndex] = this[existingItemIndex].copy(
                    quantity = this[existingItemIndex].quantity + 1
                )
            }
        } else {
            // Add new item with product's fixed weight from Firestore
            val productWeight = parseWeight(product.weight)
            currentCart.items + CartItem(
                productId = product.id,
                product = product,
                quantity = 1,
                selectedWeight = productWeight
            )
        }

        _cart.value = currentCart.copy(
            items = updatedItems,
            updatedAt = System.currentTimeMillis()
        )

        // Clear error if successful
        _error.value = null

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

    fun clearCart() {
        _cart.value = Cart()
        imageCache.clear()
        _cartImages.value = emptyMap()
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

    // Update metal prices (admin functionality)
    fun updateMetalPrices(goldPrice: Double, silverPrice: Double) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val newPrices = MetalPrices(
                    goldPricePerGram = goldPrice,
                    silverPricePerGram = silverPrice
                )

                val success = cartRepository.updateMetalPrices(newPrices)
                if (success) {
                    _metalPrices.value = newPrices
                    _error.value = null
                } else {
                    _error.value = "Failed to update metal prices"
                }
            } catch (e: Exception) {
                _error.value = "Error updating metal prices: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Clean up resources
    fun onCleared() {
        viewModelScope.cancel()
        imageCache.clear()
    }

    // Add these methods to your CartViewModel class

    // Fixed weight-based calculation method with quantity
    private fun calculateItemPrice(cartItem: CartItem): Double {
        val weight = parseWeight(cartItem.product.weight)
        val pricePerGram = when {
            cartItem.product.materialType.contains("gold", ignoreCase = true) -> _metalPrices.value.goldPricePerGram
            cartItem.product.materialType.contains("silver", ignoreCase = true) -> _metalPrices.value.silverPricePerGram
            else -> _metalPrices.value.goldPricePerGram // Default to gold
        }

        // Fixed: Include quantity in calculation
        val metalCost = weight * cartItem.quantity * pricePerGram
        val makingCharges = weight * cartItem.quantity * 100.0 // ₹100 per gram making charges

        return metalCost + makingCharges
    }

    // Fixed subtotal calculation - weight based with quantity
    fun getSubtotal(): Double {
        return _cart.value.items.sumOf { cartItem ->
            val weight = parseWeight(cartItem.product.weight)
            val pricePerGram = when {
                cartItem.product.materialType.contains("gold", ignoreCase = true) -> _metalPrices.value.goldPricePerGram
                cartItem.product.materialType.contains("silver", ignoreCase = true) -> _metalPrices.value.silverPricePerGram
                else -> _metalPrices.value.goldPricePerGram
            }
            weight * cartItem.quantity * pricePerGram // Metal cost only
        }
    }

    // Calculate making charges (₹100 per gram) with quantity
    fun getMakingCharges(): Double {
        return _cart.value.items.sumOf { cartItem ->
            val weight = parseWeight(cartItem.product.weight)
            weight * cartItem.quantity * 100.0 // ₹100 per gram making charges
        }
    }

    // Get weight-based price for a specific item with quantity
    fun getItemPrice(cartItem: CartItem): Double {
        val weight = parseWeight(cartItem.product.weight)
        val pricePerGram = when {
            cartItem.product.materialType.contains("gold", ignoreCase = true) -> _metalPrices.value.goldPricePerGram
            cartItem.product.materialType.contains("silver", ignoreCase = true) -> _metalPrices.value.silverPricePerGram
            else -> _metalPrices.value.goldPricePerGram
        }

        val metalCost = weight * cartItem.quantity * pricePerGram
        val makingCharges = weight * cartItem.quantity * 100.0

        return metalCost + makingCharges
    }

    // Get total weight for a specific item (weight * quantity)
    fun getItemWeight(cartItem: CartItem): Double {
        return parseWeight(cartItem.product.weight)
    }

    // Get price per gram for a specific item
    fun getItemPricePerGram(cartItem: CartItem): Double {
        return when {
            cartItem.product.materialType.contains("gold", ignoreCase = true) -> _metalPrices.value.goldPricePerGram
            cartItem.product.materialType.contains("silver", ignoreCase = true) -> _metalPrices.value.silverPricePerGram
            else -> _metalPrices.value.goldPricePerGram
        }
    }
}