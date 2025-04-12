package org.example.project.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.example.project.data.ProductRepository
import java.util.concurrent.ConcurrentHashMap

// Image loading utility
class ImageLoader(private val repository: ProductRepository) {
    private val cache = ConcurrentHashMap<String, ByteArray>()
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun loadImage(url: String): ByteArray? {
        // Check cache first
        cache[url]?.let { return it }

        // Load from repository
        val imageData = repository.getProductImage(url)
        if (imageData != null) {
            cache[url] = imageData
        }
        return imageData
    }

    fun clearCache() {
        cache.clear()
    }
}