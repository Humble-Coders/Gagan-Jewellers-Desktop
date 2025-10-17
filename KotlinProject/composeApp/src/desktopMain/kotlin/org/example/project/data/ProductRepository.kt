package org.example.project.data

// Repository.kt
import com.google.cloud.firestore.Firestore
import com.google.cloud.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

interface ProductRepository {
    suspend fun getAllProducts(): List<Product>
    suspend fun getProductById(id: String): Product?
    suspend fun getProductByBarcodeId(barcodeId: String): Product?
    suspend fun addProduct(product: Product): String
    suspend fun updateProduct(product: Product): Boolean
    suspend fun deleteProduct(id: String): Boolean
    suspend fun getCategories(): List<Category>
    suspend fun getMaterials(): List<Material>
    suspend fun getProductImage(url: String): ByteArray?
    suspend fun generateUniqueBarcodes(quantity: Int, digits: Int): List<String>
    // New: suggestion management
    suspend fun addCategory(name: String): String
    suspend fun addMaterial(name: String): String
    suspend fun addMaterialType(materialId: String, type: String): Boolean
    // Featured products management
    suspend fun getFeaturedProducts(): List<Product>
}

class FirestoreProductRepository(private val firestore: Firestore, private val storage: Storage) :
    ProductRepository {

    override suspend fun getAllProducts(): List<Product> = withContext(Dispatchers.IO) {
        val productsCollection = firestore.collection("products")
        val future = productsCollection.get()

        val snapshot = future.get()
        snapshot.documents.map { doc ->
            val data = doc.data
            val showMap = (data["show"] as? Map<*, *>)?.mapValues { entry ->
                when (val v = entry.value) {
                    is Boolean -> v
                    is String -> v.toBoolean()
                    else -> true
                }
            } ?: emptyMap()
            Product(
                id = doc.id,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                price = (data["price"] as? String)?.toDoubleOrNull() ?: 0.0,
                categoryId = data["category_id"] as? String ?: "",
                materialId = data["material_id"] as? String ?: "",
                materialType = data["material_type"] as? String ?: "",
                quantity = (data["quantity"] as? String)?.toIntOrNull() ?: 0,
                totalWeight = (data["total_weight"] as? String)?.toDoubleOrNull() ?: 0.0,
                defaultMakingRate = (data["default_making_rate"] as? String)?.toDoubleOrNull() ?: 0.0,
                isOtherThanGold = when (val value = data["is_other_than_gold"]) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> false
                },
                lessWeight = (data["less_weight"] as? String)?.toDoubleOrNull() ?: 0.0,
                hasStones = when (val value = data["has_stones"]) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> false
                },
                stoneName = data["stone_name"] as? String ?: "",
                stoneQuantity = (data["stone_quantity"] as? String)?.toDoubleOrNull() ?: 0.0,
                stoneRate = (data["stone_rate"] as? String)?.toDoubleOrNull() ?: 0.0,
                cwWeight = (data["cw_weight"] as? String)?.toDoubleOrNull() ?: 0.0,
                stoneAmount = (data["stone_amount"] as? String)?.toDoubleOrNull() ?: 0.0,
                vaCharges = (data["va_charges"] as? String)?.toDoubleOrNull() ?: 0.0,
                netWeight = (data["net_weight"] as? String)?.toDoubleOrNull() ?: 0.0,
                totalProductCost = (data["total_product_cost"] as? String)?.toDoubleOrNull() ?: 0.0,
                hasCustomPrice = when (val value = data["has_custom_price"]) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> false
                },
                customPrice = (data["custom_price"] as? String)?.toDoubleOrNull() ?: 0.0,
                available = when (val value = data["available"]) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> true
                },
                featured = when (val value = data["featured"]) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> false
                },
                images = (data["images"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                barcodeIds = (data["barcode_ids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                autoGenerateId = when (val value = data["auto_generate_id"]) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> false
                },
                customProductId = data["custom_product_id"] as? String ?: "",
                commonId = data["common_id"] as? String,
                createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                show = ProductShowConfig(
                    name = showMap["name"] as? Boolean ?: true,
                    description = showMap["description"] as? Boolean ?: true,
                    category = showMap["category_id"] as? Boolean ?: true,
                    material = showMap["material_id"] as? Boolean ?: true,
                    materialType = showMap["material_type"] as? Boolean ?: true,
                    quantity = showMap["quantity"] as? Boolean ?: true,
                    totalWeight = showMap["total_weight"] as? Boolean ?: true,
                    price = showMap["price"] as? Boolean ?: true,
                    defaultMakingRate = showMap["default_making_rate"] as? Boolean ?: true,
                    vaCharges = showMap["va_charges"] as? Boolean ?: true,
                    isOtherThanGold = showMap["is_other_than_gold"] as? Boolean ?: true,
                    lessWeight = showMap["less_weight"] as? Boolean ?: true,
                    hasStones = showMap["has_stones"] as? Boolean ?: true,
                    stoneName = showMap["stone_name"] as? Boolean ?: true,
                    stoneQuantity = showMap["stone_quantity"] as? Boolean ?: true,
                    stoneRate = showMap["stone_rate"] as? Boolean ?: true,
                    cwWeight = showMap["cw_weight"] as? Boolean ?: true,
                    stoneAmount = showMap["stone_amount"] as? Boolean ?: true,
                    netWeight = showMap["net_weight"] as? Boolean ?: true,
                    totalProductCost = showMap["total_product_cost"] as? Boolean ?: true,
                    customPrice = showMap["custom_price"] as? Boolean ?: true,
                    images = showMap["images"] as? Boolean ?: true,
                    available = showMap["available"] as? Boolean ?: true,
                    featured = showMap["featured"] as? Boolean ?: true
                )
            )
        }
    }

    override suspend fun getProductById(id: String): Product? = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("products").document(id)
            val future = docRef.get()
            val snapshot = future.get()

            if (snapshot.exists()) {
                val data = snapshot.data
                val showMap = (data?.get("show") as? Map<*, *>)?.mapValues { entry ->
                    when (val v = entry.value) {
                        is Boolean -> v
                        is String -> v.toBoolean()
                        else -> true
                    }
                } ?: emptyMap()
                Product(
                    id = snapshot.id,
                    name = data?.get("name") as? String ?: "",
                    description = data?.get("description") as? String ?: "",
                    price = (data?.get("price") as? String)?.toDoubleOrNull() ?: 0.0,
                    categoryId = data?.get("category_id") as? String ?: "",
                    materialId = data?.get("material_id") as? String ?: "",
                    materialType = data?.get("material_type") as? String ?: "",
                    quantity = (data?.get("quantity") as? String)?.toIntOrNull() ?: 0,
                    totalWeight = (data?.get("total_weight") as? String)?.toDoubleOrNull() ?: 0.0,
                    defaultMakingRate = (data?.get("default_making_rate") as? String)?.toDoubleOrNull() ?: 0.0,
                    isOtherThanGold = when (val value = data?.get("is_other_than_gold")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
                    lessWeight = (data?.get("less_weight") as? String)?.toDoubleOrNull() ?: 0.0,
                    hasStones = when (val value = data?.get("has_stones")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
                    stoneName = data?.get("stone_name") as? String ?: "",
                    stoneQuantity = (data?.get("stone_quantity") as? String)?.toDoubleOrNull() ?: 0.0,
                    stoneRate = (data?.get("stone_rate") as? String)?.toDoubleOrNull() ?: 0.0,
                    cwWeight = (data?.get("cw_weight") as? String)?.toDoubleOrNull() ?: 0.0,
                    stoneAmount = (data?.get("stone_amount") as? String)?.toDoubleOrNull() ?: 0.0,
                    vaCharges = (data?.get("va_charges") as? String)?.toDoubleOrNull() ?: 0.0,
                    netWeight = (data?.get("net_weight") as? String)?.toDoubleOrNull() ?: 0.0,
                    totalProductCost = (data?.get("total_product_cost") as? String)?.toDoubleOrNull() ?: 0.0,
                    hasCustomPrice = when (val value = data?.get("has_custom_price")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
                    customPrice = (data?.get("custom_price") as? String)?.toDoubleOrNull() ?: 0.0,
                    available = when (val value = data?.get("available")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> true
                    },
                    featured = when (val value = data?.get("featured")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
                    images = (data?.get("images") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    barcodeIds = (data?.get("barcode_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    autoGenerateId = when (val value = data?.get("auto_generate_id")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
                    customProductId = data?.get("custom_product_id") as? String ?: "",
                    createdAt = (data?.get("created_at") as? Number)?.toLong() ?: System.currentTimeMillis(),
                    show = ProductShowConfig(
                        name = showMap["name"] as? Boolean ?: true,
                        description = showMap["description"] as? Boolean ?: true,
                        category = showMap["category_id"] as? Boolean ?: true,
                        material = showMap["material_id"] as? Boolean ?: true,
                        materialType = showMap["material_type"] as? Boolean ?: true,
                        quantity = showMap["quantity"] as? Boolean ?: true,
                        totalWeight = showMap["total_weight"] as? Boolean ?: true,
                        price = showMap["price"] as? Boolean ?: true,
                        defaultMakingRate = showMap["default_making_rate"] as? Boolean ?: true,
                        vaCharges = showMap["va_charges"] as? Boolean ?: true,
                        isOtherThanGold = showMap["is_other_than_gold"] as? Boolean ?: true,
                        lessWeight = showMap["less_weight"] as? Boolean ?: true,
                        hasStones = showMap["has_stones"] as? Boolean ?: true,
                        stoneName = showMap["stone_name"] as? Boolean ?: true,
                        stoneQuantity = showMap["stone_quantity"] as? Boolean ?: true,
                        stoneRate = showMap["stone_rate"] as? Boolean ?: true,
                        cwWeight = showMap["cw_weight"] as? Boolean ?: true,
                        stoneAmount = showMap["stone_amount"] as? Boolean ?: true,
                    netWeight = showMap["net_weight"] as? Boolean ?: true,
                    totalProductCost = showMap["total_product_cost"] as? Boolean ?: true,
                    customPrice = showMap["custom_price"] as? Boolean ?: true,
                    images = showMap["images"] as? Boolean ?: true,
                    available = showMap["available"] as? Boolean ?: true,
                    featured = showMap["featured"] as? Boolean ?: true
                )
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getProductByBarcodeId(barcodeId: String): Product? = withContext(Dispatchers.IO) {
        try {
            val productsCollection = firestore.collection("products")
            val query = productsCollection.whereArrayContains("barcode_ids", barcodeId)
            val future = query.get()
            val snapshot = future.get()

            if (!snapshot.isEmpty) {
                val document = snapshot.documents.first()
                val data = document.data
                val showMap = (data?.get("show") as? Map<*, *>)?.mapValues { entry ->
                    when (val v = entry.value) {
                        is Boolean -> v
                        is String -> v.toBoolean()
                        else -> true
                    }
                } ?: emptyMap()

                Product(
                    id = document.id,
                    name = data?.get("name") as? String ?: "",
                    description = data?.get("description") as? String ?: "",
                    price = (data?.get("price") as? String)?.toDoubleOrNull() ?: 0.0,
                    categoryId = data?.get("category_id") as? String ?: "",
                    materialId = data?.get("material_id") as? String ?: "",
                    materialType = data?.get("material_type") as? String ?: "",
                    quantity = (data?.get("quantity") as? String)?.toIntOrNull() ?: 0,
                    totalWeight = (data?.get("total_weight") as? String)?.toDoubleOrNull() ?: 0.0,
                    defaultMakingRate = (data?.get("default_making_rate") as? String)?.toDoubleOrNull() ?: 0.0,
                    isOtherThanGold = when (val value = data?.get("is_other_than_gold")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
                    lessWeight = (data?.get("less_weight") as? String)?.toDoubleOrNull() ?: 0.0,
                    hasStones = when (val value = data?.get("has_stones")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
                    stoneName = data?.get("stone_name") as? String ?: "",
                    stoneQuantity = (data?.get("stone_quantity") as? String)?.toDoubleOrNull() ?: 0.0,
                    stoneRate = (data?.get("stone_rate") as? String)?.toDoubleOrNull() ?: 0.0,
                    cwWeight = (data?.get("cw_weight") as? String)?.toDoubleOrNull() ?: 0.0,
                    stoneAmount = (data?.get("stone_amount") as? String)?.toDoubleOrNull() ?: 0.0,
                    vaCharges = (data?.get("va_charges") as? String)?.toDoubleOrNull() ?: 0.0,
                    netWeight = (data?.get("net_weight") as? String)?.toDoubleOrNull() ?: 0.0,
                    totalProductCost = (data?.get("total_product_cost") as? String)?.toDoubleOrNull() ?: 0.0,
                    hasCustomPrice = when (val value = data?.get("has_custom_price")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
                    customPrice = (data?.get("custom_price") as? String)?.toDoubleOrNull() ?: 0.0,
                    available = when (val value = data?.get("available")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> true
                    },
                    featured = when (val value = data?.get("featured")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
                    images = (data?.get("images") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    barcodeIds = (data?.get("barcode_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    autoGenerateId = when (val value = data?.get("auto_generate_id")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
                    customProductId = data?.get("custom_product_id") as? String ?: "",
                    commonId = data?.get("common_id") as? String,
                    createdAt = (data?.get("created_at") as? Number)?.toLong() ?: System.currentTimeMillis(),
                    show = ProductShowConfig(
                        name = showMap["name"] as? Boolean ?: true,
                        description = showMap["description"] as? Boolean ?: true,
                        category = showMap["category_id"] as? Boolean ?: true,
                        material = showMap["material_id"] as? Boolean ?: true,
                        materialType = showMap["material_type"] as? Boolean ?: true,
                        quantity = showMap["quantity"] as? Boolean ?: true,
                        totalWeight = showMap["total_weight"] as? Boolean ?: true,
                        price = showMap["price"] as? Boolean ?: true,
                        defaultMakingRate = showMap["default_making_rate"] as? Boolean ?: true,
                        vaCharges = showMap["va_charges"] as? Boolean ?: true,
                        isOtherThanGold = showMap["is_other_than_gold"] as? Boolean ?: true,
                        lessWeight = showMap["less_weight"] as? Boolean ?: true,
                        hasStones = showMap["has_stones"] as? Boolean ?: true,
                        stoneName = showMap["stone_name"] as? Boolean ?: true,
                        stoneQuantity = showMap["stone_quantity"] as? Boolean ?: true,
                        stoneRate = showMap["stone_rate"] as? Boolean ?: true,
                        cwWeight = showMap["cw_weight"] as? Boolean ?: true,
                        stoneAmount = showMap["stone_amount"] as? Boolean ?: true,
                        netWeight = showMap["net_weight"] as? Boolean ?: true,
                        totalProductCost = showMap["total_product_cost"] as? Boolean ?: true,
                        customPrice = showMap["custom_price"] as? Boolean ?: true,
                        images = showMap["images"] as? Boolean ?: true,
                        available = showMap["available"] as? Boolean ?: true,
                        featured = showMap["featured"] as? Boolean ?: true
                    )
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun generateRandomBarcode(digits: Int): String {
        val sb = StringBuilder(digits)
        repeat(digits) {
            val d = (0..9).random()
            sb.append(d)
        }
        return sb.toString()
    }

    private fun isBarcodeUnique(barcode: String): Boolean {
        val productsCollection = firestore.collection("products")
        val query = productsCollection.whereArrayContains("barcode_ids", barcode)
        val snapshot = query.get().get()
        return snapshot.isEmpty
    }

    override suspend fun generateUniqueBarcodes(quantity: Int, digits: Int): List<String> = withContext(Dispatchers.IO) {
        val result = mutableSetOf<String>()
        while (result.size < quantity) {
            val code = generateRandomBarcode(digits)
            if (isBarcodeUnique(code)) {
                result.add(code)
            }
        }
        result.toList()
    }

    override suspend fun addProduct(product: Product): String = withContext(Dispatchers.IO) {
        println("=== ProductRepository.addProduct() called ===")
        println("Product details - Name: '${product.name}', ID: '${product.id}', BarcodeIds: ${product.barcodeIds}")
        try {
            val productsCollection = firestore.collection("products")
            val docRef = productsCollection.document()
            val newProductId = docRef.id
            println("📄 Generated new document ID: $newProductId")

            val productMap = mutableMapOf<String, Any?>(
                "id" to newProductId,
                "name" to product.name,
                "created_at" to System.currentTimeMillis()
            )

            // Add fields as strings, use null for empty/unselected values
            productMap["description"] = product.description?.takeIf { it.isNotBlank() }
            // price field is not stored in Firestore (material rate is used instead)
            productMap["category_id"] = product.categoryId.takeIf { it.isNotBlank() }
            productMap["material_id"] = product.materialId?.takeIf { it.isNotBlank() }
            productMap["material_type"] = product.materialType?.takeIf { it.isNotBlank() }
            productMap["quantity"] = if (product.quantity > 0) product.quantity.toString() else null
            productMap["total_weight"] = if (product.totalWeight > 0) product.totalWeight.toString() else null
            productMap["default_making_rate"] = if (product.defaultMakingRate > 0) product.defaultMakingRate.toString() else null
            productMap["is_other_than_gold"] = product.isOtherThanGold
            productMap["less_weight"] = if (product.lessWeight > 0) product.lessWeight.toString() else null
            productMap["has_stones"] = product.hasStones
            productMap["stone_name"] = product.stoneName.takeIf { it.isNotBlank() }
            productMap["stone_quantity"] = if (product.stoneQuantity > 0) product.stoneQuantity.toString() else null
            productMap["stone_rate"] = if (product.stoneRate > 0) product.stoneRate.toString() else null
            productMap["cw_weight"] = if (product.cwWeight > 0) product.cwWeight.toString() else null
            productMap["stone_amount"] = if (product.stoneAmount > 0) product.stoneAmount.toString() else null
            productMap["va_charges"] = if (product.vaCharges > 0) product.vaCharges.toString() else null
            productMap["net_weight"] = if (product.netWeight > 0) product.netWeight.toString() else null
            // Store total_product_cost if provided (as string for consistency with other numeric fields)
            productMap["total_product_cost"] = if (product.totalProductCost > 0) product.totalProductCost.toString() else null
            productMap["has_custom_price"] = product.hasCustomPrice
            productMap["custom_price"] = if (product.customPrice > 0) product.customPrice.toString() else null
            productMap["available"] = product.available
        productMap["featured"] = product.featured
        productMap["images"] = product.images.takeIf { it.isNotEmpty() } ?: emptyList<String>()
        productMap["barcode_ids"] = product.barcodeIds.takeIf { it.isNotEmpty() } ?: emptyList<String>()
        productMap["auto_generate_id"] = product.autoGenerateId
        productMap["custom_product_id"] = product.customProductId?.takeIf { it.isNotBlank() }
        productMap["common_id"] = product.commonId

        // Show map
        val showMap = mapOf(
            "name" to product.show.name,
            "description" to product.show.description,
            "category_id" to product.show.category,
            "material_id" to product.show.material,
            "material_type" to product.show.materialType,
            "quantity" to product.show.quantity,
            "total_weight" to product.show.totalWeight,
            "price" to product.show.price,
            "default_making_rate" to product.show.defaultMakingRate,
            "va_charges" to product.show.vaCharges,
            "is_other_than_gold" to product.show.isOtherThanGold,
            "less_weight" to product.show.lessWeight,
            "has_stones" to product.show.hasStones,
            "stone_name" to product.show.stoneName,
            "stone_quantity" to product.show.stoneQuantity,
                "stone_rate" to product.show.stoneRate,
                "cw_weight" to product.show.cwWeight,
                "stone_amount" to product.show.stoneAmount,
                "net_weight" to product.show.netWeight,
                "total_product_cost" to product.show.totalProductCost,
                "custom_price" to product.show.customPrice,
                "images" to product.show.images,
                "available" to product.show.available,
                "featured" to product.show.featured
            )
            productMap["show"] = showMap

        println("📝 Product data prepared for Firestore: ${productMap.keys}")
        println("📝 Key values - Name: '${productMap["name"]}', BarcodeIds: ${productMap["barcode_ids"]}")
        println("📝 Note: total_product_cost is not stored in Firestore")

        docRef.set(productMap).get()
        println("✅ Product document created in Firestore with ID: $newProductId")

        // Add to category_products as well
        product.categoryId.takeIf { it.isNotBlank() }?.let { catId ->
            println("📂 Adding product to category: $catId")
            val categoryProductsRef = firestore.collection("category_products").document(catId)
            val categoryDoc = categoryProductsRef.get().get()

            if (categoryDoc.exists()) {
                val data = categoryDoc.data
                val currentProductIds = (data?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val updatedProductIds = currentProductIds + newProductId
                categoryProductsRef.update("product_ids", updatedProductIds).get()
                println("✅ Updated category with new product ID")
            } else {
                categoryProductsRef.set(mapOf("product_ids" to listOf(newProductId))).get()
                println("✅ Created new category document with product ID")
            }
        }

        // Add to featured_products if product is featured
        if (product.featured) {
            println("⭐ Adding product to featured_products collection")
            val featuredProductsRef = firestore.collection("featured_products").document("featured_list")
            val featuredDoc = featuredProductsRef.get().get()

            if (featuredDoc.exists()) {
                val data = featuredDoc.data
                val currentFeaturedIds = (data?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                if (!currentFeaturedIds.contains(newProductId)) {
                    val updatedFeaturedIds = currentFeaturedIds + newProductId
                    featuredProductsRef.update("product_ids", updatedFeaturedIds).get()
                    println("✅ Added product to featured_products list")
                } else {
                    println("ℹ️ Product already in featured_products list")
                }
            } else {
                featuredProductsRef.set(mapOf("product_ids" to listOf(newProductId))).get()
                println("✅ Created featured_products document with product ID")
            }
        }

        println("🏁 ProductRepository.addProduct completed successfully")
        newProductId
        } catch (e: Exception) {
            println("💥 Exception in ProductRepository.addProduct: ${e.message}")
            e.printStackTrace()
            throw e // Re-throw to maintain the same behavior
        }
    }

    override suspend fun updateProduct(product: Product): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("products").document(product.id)

            val productMap = mutableMapOf<String, Any?>()

            productMap["name"] = product.name
            productMap["description"] = product.description?.takeIf { it.isNotBlank() }
            // price field is not stored in Firestore (material rate is used instead)
            productMap["category_id"] = product.categoryId.takeIf { it.isNotBlank() }
            productMap["material_id"] = product.materialId?.takeIf { it.isNotBlank() }
            productMap["material_type"] = product.materialType?.takeIf { it.isNotBlank() }
            productMap["quantity"] = if (product.quantity > 0) product.quantity.toString() else null
            productMap["total_weight"] = if (product.totalWeight > 0) product.totalWeight.toString() else null
            productMap["default_making_rate"] = if (product.defaultMakingRate > 0) product.defaultMakingRate.toString() else null
            productMap["is_other_than_gold"] = product.isOtherThanGold
            productMap["less_weight"] = if (product.lessWeight > 0) product.lessWeight.toString() else null
            productMap["has_stones"] = product.hasStones
            productMap["stone_name"] = product.stoneName.takeIf { it.isNotBlank() }
            productMap["stone_quantity"] = if (product.stoneQuantity > 0) product.stoneQuantity.toString() else null
            productMap["stone_rate"] = if (product.stoneRate > 0) product.stoneRate.toString() else null
            productMap["cw_weight"] = if (product.cwWeight > 0) product.cwWeight.toString() else null
            productMap["stone_amount"] = if (product.stoneAmount > 0) product.stoneAmount.toString() else null
            productMap["va_charges"] = if (product.vaCharges > 0) product.vaCharges.toString() else null
            productMap["net_weight"] = if (product.netWeight > 0) product.netWeight.toString() else null
            // Store total_product_cost if provided (as string for consistency with other numeric fields)
            productMap["total_product_cost"] = if (product.totalProductCost > 0) product.totalProductCost.toString() else null
            productMap["has_custom_price"] = product.hasCustomPrice
            productMap["custom_price"] = if (product.customPrice > 0) product.customPrice.toString() else null
            productMap["available"] = product.available
            productMap["featured"] = product.featured
            productMap["images"] = product.images.takeIf { it.isNotEmpty() } ?: emptyList<String>()
            productMap["barcode_ids"] = product.barcodeIds.takeIf { it.isNotEmpty() } ?: emptyList<String>()
            productMap["auto_generate_id"] = product.autoGenerateId
            productMap["custom_product_id"] = product.customProductId?.takeIf { it.isNotBlank() }
        productMap["common_id"] = product.commonId
            // Show map
            val showMap = mapOf(
                "name" to product.show.name,
                "description" to product.show.description,
                "category_id" to product.show.category,
                "material_id" to product.show.material,
                "material_type" to product.show.materialType,
                "quantity" to product.show.quantity,
                "total_weight" to product.show.totalWeight,
                "price" to product.show.price,
                "default_making_rate" to product.show.defaultMakingRate,
                "va_charges" to product.show.vaCharges,
                "is_other_than_gold" to product.show.isOtherThanGold,
                "less_weight" to product.show.lessWeight,
                "has_stones" to product.show.hasStones,
                "stone_name" to product.show.stoneName,
                "stone_quantity" to product.show.stoneQuantity,
                "stone_amount" to product.show.stoneAmount,
                "stone_rate" to product.show.stoneRate,
                "cw_weight" to product.show.cwWeight,
                "net_weight" to product.show.netWeight,
                "total_product_cost" to product.show.totalProductCost,
                "custom_price" to product.show.customPrice,
                "images" to product.show.images,
                "available" to product.show.available,
                "featured" to product.show.featured
            )
            productMap["show"] = showMap

            docRef.update(productMap).get()

            // Handle featured_products collection
            val featuredProductsRef = firestore.collection("featured_products").document("featured_list")
            val featuredDoc = featuredProductsRef.get().get()

            if (product.featured) {
                // Add to featured_products if not already there
                println("⭐ Updating featured_products: adding product ${product.id}")
                if (featuredDoc.exists()) {
                    val data = featuredDoc.data
                    val currentFeaturedIds = (data?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    if (!currentFeaturedIds.contains(product.id)) {
                        val updatedFeaturedIds = currentFeaturedIds + product.id
                        featuredProductsRef.update("product_ids", updatedFeaturedIds).get()
                        println("✅ Added product to featured_products list")
                    } else {
                        println("ℹ️ Product already in featured_products list")
                    }
                } else {
                    featuredProductsRef.set(mapOf("product_ids" to listOf(product.id))).get()
                    println("✅ Created featured_products document with product ID")
                }
            } else {
                // Remove from featured_products if it exists
                if (featuredDoc.exists()) {
                    println("⭐ Updating featured_products: removing product ${product.id}")
                    val data = featuredDoc.data
                    val currentFeaturedIds = (data?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    if (currentFeaturedIds.contains(product.id)) {
                        val updatedFeaturedIds = currentFeaturedIds.filter { it != product.id }
                        featuredProductsRef.update("product_ids", updatedFeaturedIds).get()
                        println("✅ Removed product from featured_products list")
                    } else {
                        println("ℹ️ Product not in featured_products list")
                    }
                }
            }

            true
        } catch (e: Exception) {
            println("❌ Error updating product: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteProduct(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val productDoc = firestore.collection("products").document(id).get().get()

            if (productDoc.exists()) {
                val data = productDoc.data
                val categoryId = data?.get("category_id") as? String

                if (!categoryId.isNullOrBlank()) {
                    // Remove from category_products
                    val categoryProductsRef = firestore.collection("category_products").document(categoryId)
                    val categoryDoc = categoryProductsRef.get().get()

                    if (categoryDoc.exists()) {
                        val categoryData = categoryDoc.data
                        val currentProductIds = (categoryData?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val updatedProductIds = currentProductIds.filter { it != id }
                        categoryProductsRef.update("product_ids", updatedProductIds).get()
                    }
                }

                // Remove from featured_products if it exists
                val featuredProductsRef = firestore.collection("featured_products").document("featured_list")
                val featuredDoc = featuredProductsRef.get().get()
                
                if (featuredDoc.exists()) {
                    val featuredData = featuredDoc.data
                    val currentFeaturedIds = (featuredData?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    if (currentFeaturedIds.contains(id)) {
                        val updatedFeaturedIds = currentFeaturedIds.filter { it != id }
                        featuredProductsRef.update("product_ids", updatedFeaturedIds).get()
                        println("✅ Removed product from featured_products list")
                    }
                }

                // Delete the product
                firestore.collection("products").document(id).delete().get()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        val categoriesCollection = firestore.collection("categories")
        val future = categoriesCollection.get()

        val snapshot = future.get()
        snapshot.documents.map { doc ->
            val data = doc.data
            Category(
                id = doc.id,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                imageUrl = data["image_url"] as? String ?: "",
                hasGenderVariants = data["has_gender_variants"] as? Boolean ?: false,
                order = (data["order"] as? Number)?.toInt() ?: 0,
                categoryType = try {
                    org.example.project.data.CategoryType.valueOf(data["category_type"] as? String ?: "JEWELRY")
                } catch (e: Exception) {
                    org.example.project.data.CategoryType.JEWELRY
                },
                isActive = data["is_active"] as? Boolean ?: true,
                parentCategoryId = data["parent_category_id"] as? String
            )
        }
    }

    override suspend fun getMaterials(): List<Material> = withContext(Dispatchers.IO) {
        val materialsCollection = firestore.collection("materials")
        val future = materialsCollection.get()

        val snapshot = future.get()
        snapshot.documents.map { doc ->
            val data = doc.data
            Material(
                id = doc.id,
                name = data["name"] as? String ?: "",
                imageUrl = data["image_url"] as? String ?: "",
                types = (data["types"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }
    }

    override suspend fun addCategory(name: String): String = withContext(Dispatchers.IO) {
        val categoriesCollection = firestore.collection("categories")
        val docRef = categoriesCollection.document()
        val id = docRef.id
        val data = mapOf(
            "name" to name,
            "description" to "",
            "image_url" to "",
            "has_gender_variants" to false,
            "order" to 0,
            "category_type" to CategoryType.JEWELRY.name,
            "is_active" to true,
            "created_at" to System.currentTimeMillis()
        )
        docRef.set(data).get()
        id
    }

    override suspend fun addMaterial(name: String): String = withContext(Dispatchers.IO) {
        val materialsCollection = firestore.collection("materials")
        val docRef = materialsCollection.document()
        val id = docRef.id
        val data = mapOf(
            "name" to name,
            "image_url" to "",
            "types" to emptyList<String>(),
            "created_at" to System.currentTimeMillis()
        )
        docRef.set(data).get()
        id
    }

    override suspend fun addMaterialType(materialId: String, type: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val materialRef = firestore.collection("materials").document(materialId)
            val snapshot = materialRef.get().get()
            if (!snapshot.exists()) return@withContext false
            val currentTypes = (snapshot.get("types") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (currentTypes.any { it.equals(type, ignoreCase = true) }) return@withContext true
            val updated = currentTypes + type
            materialRef.update("types", updated).get()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getProductImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            when {
                url.startsWith("gs://") -> {
                    // Handle GS storage path
                    val bucketAndPath = url.removePrefix("gs://")
                    val pathParts = bucketAndPath.split("/", limit = 2)

                    if (pathParts.size == 2) {
                        val bucket = pathParts[0]
                        val path = pathParts[1]
                        val blob = storage.get(bucket, path)
                        blob?.getContent()
                    } else null
                }
                url.startsWith("http") -> {
                    // Handle direct HTTPS URL (much faster)
                    val connection = URL(url).openConnection() as HttpURLConnection
                    try {
                        connection.connect()
                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            connection.inputStream.readBytes()
                        } else null
                    } finally {
                        connection.disconnect()
                    }
                }
                else -> null // Unsupported URL format
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getFeaturedProducts(): List<Product> = withContext(Dispatchers.IO) {
        try {
            println("⭐ Fetching featured products from Firestore")
            val featuredProductsRef = firestore.collection("featured_products").document("featured_list")
            val featuredDoc = featuredProductsRef.get().get()

            if (featuredDoc.exists()) {
                val data = featuredDoc.data
                val featuredProductIds = (data?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                println("📋 Found ${featuredProductIds.size} featured product IDs: $featuredProductIds")

                val featuredProducts = mutableListOf<Product>()
                for (productId in featuredProductIds) {
                    val product = getProductById(productId)
                    if (product != null) {
                        featuredProducts.add(product)
                        println("✅ Added featured product: ${product.name} (ID: $productId)")
                    } else {
                        println("⚠️ Featured product ID $productId not found in products collection")
                    }
                }
                println("🏁 Retrieved ${featuredProducts.size} featured products successfully")
                featuredProducts
            } else {
                println("ℹ️ No featured_products document found")
                emptyList()
            }
        } catch (e: Exception) {
            println("💥 Error fetching featured products: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
}