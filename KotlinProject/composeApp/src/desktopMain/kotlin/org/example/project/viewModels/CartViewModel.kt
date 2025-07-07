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

    fun addToCart(product: Product) {
        val currentCart = _cart.value
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
                selectedWeight = productWeight // Fixed weight from Firestore
            )
        }

        _cart.value = currentCart.copy(
            items = updatedItems,
            updatedAt = System.currentTimeMillis()
        )

        // Load image for the added product
        loadProductImage(product)
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

    fun updateQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(productId)
            return
        }

        val currentCart = _cart.value
        val existingItemIndex = currentCart.items.indexOfFirst { it.productId == productId }

        if (existingItemIndex >= 0) {
            val updatedItems = currentCart.items.toMutableList().apply {
                this[existingItemIndex] = this[existingItemIndex].copy(quantity = newQuantity)
            }

            _cart.value = currentCart.copy(
                items = updatedItems,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // Weight is now fixed from Firestore, remove this method or make it no-op
    fun updateWeight(productId: String, newWeight: Double) {
        // Weight is fixed from Firestore, no update needed
        // This method is kept for compatibility but does nothing
        println("Weight update ignored - using fixed Firestore weight")
    }

    fun clearCart() {
        _cart.value = Cart()
        imageCache.clear()
        _cartImages.value = emptyMap()
    }

    // Calculate subtotal (before GST)
    fun getSubtotal(): Double {
        return _cart.value.calculateTotalPrice(
            _metalPrices.value.goldPricePerGram,
            _metalPrices.value.silverPricePerGram
        )
    }

    // Calculate 18% GST (updated from 3%)
    fun getGST(): Double {
        return getSubtotal() * 0.18 // 18% GST
    }

    // Calculate final total (subtotal + 18% GST)
    fun getFinalTotal(): Double {
        return getSubtotal() + getGST()
    }

    // Legacy method with custom GST rate (deprecated)
    @Deprecated("Use getGST() instead - GST is now fixed at 18%")
    fun getGST(gstRate: Double): Double {
        return getSubtotal() * (gstRate / 100)
    }

    // Legacy method with custom GST rate (deprecated)
    @Deprecated("Use getFinalTotal() instead - GST is now fixed at 18%")
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
}