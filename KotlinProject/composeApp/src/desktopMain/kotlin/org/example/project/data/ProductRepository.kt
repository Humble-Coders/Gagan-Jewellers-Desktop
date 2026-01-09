package org.example.project.data

// Repository.kt
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Transaction
import com.google.cloud.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

interface ProductRepository {
    suspend fun getAllProducts(): List<Product>
    suspend fun getProductById(id: String): Product?
    suspend fun addProduct(product: Product): String
    suspend fun updateProduct(product: Product): Boolean
    suspend fun deleteProduct(id: String): Boolean
    suspend fun getCategories(): List<Category>
    suspend fun getMaterials(): List<Material>
    suspend fun getProductImage(url: String): ByteArray?
    // New: suggestion management
    suspend fun addCategory(name: String): String
    suspend fun addCompleteCategory(category: Category): String
    suspend fun updateCategory(category: Category): Boolean
    suspend fun deleteCategory(categoryId: String): Boolean
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
                price = (data["price"] as? Number)?.toDouble() 
                    ?: (data["price"] as? String)?.toDoubleOrNull() 
                    ?: 0.0,
                categoryId = data["category_id"] as? String ?: "",
                materialId = data["material_id"] as? String ?: "",
                materialType = data["material_type"] as? String ?: "",
                materialName = data["material_name"] as? String ?: "",
                quantity = (data["quantity"] as? String)?.toIntOrNull() ?: 0,
                totalWeight = (data["total_weight"] as? String)?.toDoubleOrNull() ?: 0.0,
                hasStones = when (val value = data["has_stones"]) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> false
                },
                stones = if (data["stones"] != null) {
                    // New format: stones array
                    parseProductStones(data["stones"])
                } else {
                    // Backward compatibility: parse from old format fields
                    val stoneName = data["stone_name"] as? String
                    val stoneQuantity = (data["stone_quantity"] as? String)?.toDoubleOrNull()
                    val stoneRate = (data["stone_rate"] as? String)?.toDoubleOrNull()
                    val stoneAmount = (data["stone_amount"] as? String)?.toDoubleOrNull()
                    val cwWeight = (data["cw_weight"] as? String)?.toDoubleOrNull() // Backward compatibility: old field name
                    parseProductStonesFromOldFormat(stoneName, stoneQuantity, stoneRate, stoneAmount, cwWeight)
                },
                materialWeight = (data["material_weight"] as? Number)?.toDouble() 
                    ?: (data["material_weight"] as? String)?.toDoubleOrNull() 
                    ?: (data["gold_weight"] as? Number)?.toDouble() // Backward compatibility: gold_weight deprecated
                    ?: (data["gold_weight"] as? String)?.toDoubleOrNull() // Backward compatibility: gold_weight deprecated
                    ?: 0.0,
                // Calculate stoneWeight and stoneAmount from stones array
                stoneWeight = if (data["stones"] != null) {
                    parseProductStones(data["stones"]).sumOf { it.weight }
                } else {
                    (data["stone_weight"] as? String)?.toDoubleOrNull() ?: 0.0 // Fallback to stored value for backward compatibility
                },
                makingPercent = (data["making_percent"] as? String)?.toDoubleOrNull() ?: 0.0,
                labourCharges = (data["labour_charges"] as? String)?.toDoubleOrNull() ?: 0.0,
                effectiveWeight = (data["effective_weight"] as? String)?.toDoubleOrNull() ?: 0.0,
                effectiveMetalWeight = (data["effective_metal_weight"] as? String)?.toDoubleOrNull() ?: (data["effective_gold_weight"] as? String)?.toDoubleOrNull() ?: 0.0, // Backward compatibility
                labourRate = (data["labour_rate"] as? String)?.toDoubleOrNull() ?: 0.0,
                // Calculate stoneAmount from stones array, with backward compatibility for stone_rate and stone_amount
                stoneAmount = if (data["stones"] != null) {
                    parseProductStones(data["stones"]).sumOf { it.amount }
                } else {
                    (data["stone_rate"] as? String)?.toDoubleOrNull() 
                        ?: (data["stone_amount"] as? String)?.toDoubleOrNull() 
                        ?: 0.0 // Fallback to stored value for backward compatibility
                },
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
                autoGenerateId = when (val value = data["auto_generate_id"]) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> false
                },
                createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                // Collection product fields
                isCollectionProduct = when (val value = data["is_collection_product"]) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> false
                },
                collectionId = data["collection_id"] as? String ?: "",
                show = ProductShowConfig(
                    name = showMap["name"] as? Boolean ?: true,
                    description = showMap["description"] as? Boolean ?: true,
                    category = showMap["category_id"] as? Boolean ?: true,
                    material = showMap["material_id"] as? Boolean ?: true,
                    materialType = showMap["material_type"] as? Boolean ?: true,
                    quantity = showMap["quantity"] as? Boolean ?: true,
                    totalWeight = showMap["total_weight"] as? Boolean ?: true,
                    price = showMap["price"] as? Boolean ?: true,
                    hasStones = showMap["has_stones"] as? Boolean ?: true,
                    stones = showMap["stones"] as? Boolean ?: true,
                    customPrice = showMap["custom_price"] as? Boolean ?: true,
                    materialWeight = (showMap["material_weight"] as? Boolean) ?: (showMap["gold_weight"] as? Boolean) ?: true, // Backward compatibility
                    stoneWeight = showMap["stone_weight"] as? Boolean ?: true,
                    makingPercent = showMap["making_percent"] as? Boolean ?: true,
                    labourCharges = showMap["labour_charges"] as? Boolean ?: true,
                    effectiveWeight = showMap["effective_weight"] as? Boolean ?: true,
                    effectiveMetalWeight = (showMap["effective_metal_weight"] as? Boolean) ?: (showMap["effective_gold_weight"] as? Boolean) ?: true, // Backward compatibility
                    labourRate = showMap["labour_rate"] as? Boolean ?: true,
                    stoneAmount = showMap["stone_amount"] as? Boolean ?: true,
                    images = showMap["images"] as? Boolean ?: true,
                    available = showMap["available"] as? Boolean ?: true,
                    featured = showMap["featured"] as? Boolean ?: true,
                    isCollectionProduct = showMap["is_collection_product"] as? Boolean ?: true,
                    collectionId = showMap["collection_id"] as? Boolean ?: true
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
                    price = (data?.get("price") as? Number)?.toDouble() 
                        ?: (data?.get("price") as? String)?.toDoubleOrNull() 
                        ?: 0.0,
                    categoryId = data?.get("category_id") as? String ?: "",
                    materialId = data?.get("material_id") as? String ?: "",
                    materialType = data?.get("material_type") as? String ?: "",
                    materialName = data?.get("material_name") as? String ?: "",
                    quantity = (data?.get("quantity") as? String)?.toIntOrNull() ?: 0,
                    totalWeight = (data?.get("total_weight") as? String)?.toDoubleOrNull() ?: 0.0,
                    hasStones = when (val value = data?.get("has_stones")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
                    stones = if (data?.get("stones") != null) {
                        // New format: stones array
                        parseProductStones(data?.get("stones"))
                    } else {
                        // Backward compatibility: parse from old format fields
                        val stoneName = data?.get("stone_name") as? String
                        val stoneQuantity = (data?.get("stone_quantity") as? String)?.toDoubleOrNull()
                        val stoneRate = (data?.get("stone_rate") as? String)?.toDoubleOrNull()
                        val stoneAmount = (data?.get("stone_amount") as? String)?.toDoubleOrNull()
                        val cwWeight = (data?.get("cw_weight") as? String)?.toDoubleOrNull() // Backward compatibility: old field name
                        parseProductStonesFromOldFormat(stoneName, stoneQuantity, stoneRate, stoneAmount, cwWeight)
                    },
                    materialWeight = (data?.get("material_weight") as? Number)?.toDouble() 
                        ?: (data?.get("material_weight") as? String)?.toDoubleOrNull() 
                        ?: (data?.get("gold_weight") as? Number)?.toDouble() // Backward compatibility: gold_weight deprecated
                        ?: (data?.get("gold_weight") as? String)?.toDoubleOrNull() // Backward compatibility: gold_weight deprecated
                        ?: 0.0,
                    // Calculate stoneWeight and stoneAmount from stones array
                    stoneWeight = if (data?.get("stones") != null) {
                        parseProductStones(data?.get("stones")).sumOf { it.weight }
                    } else {
                        (data?.get("stone_weight") as? String)?.toDoubleOrNull() ?: 0.0 // Fallback to stored value for backward compatibility
                    },
                    makingPercent = (data?.get("making_percent") as? String)?.toDoubleOrNull() ?: 0.0,
                    labourCharges = (data?.get("labour_charges") as? String)?.toDoubleOrNull() ?: 0.0,
                    effectiveWeight = (data?.get("effective_weight") as? String)?.toDoubleOrNull() ?: 0.0,
                    effectiveMetalWeight = (data?.get("effective_metal_weight") as? String)?.toDoubleOrNull() ?: (data?.get("effective_gold_weight") as? String)?.toDoubleOrNull() ?: 0.0, // Backward compatibility
                    labourRate = (data?.get("labour_rate") as? String)?.toDoubleOrNull() ?: 0.0,
                    // Calculate stoneAmount from stones array, with backward compatibility for stone_rate and stone_amount
                    stoneAmount = if (data?.get("stones") != null) {
                        parseProductStones(data?.get("stones")).sumOf { it.amount }
                    } else {
                        (data?.get("stone_rate") as? String)?.toDoubleOrNull()
                            ?: (data?.get("stone_amount") as? String)?.toDoubleOrNull()
                            ?: 0.0 // Fallback to stored value for backward compatibility
                    },
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
                    autoGenerateId = when (val value = data?.get("auto_generate_id")) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> false
                    },
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
                        hasStones = showMap["has_stones"] as? Boolean ?: true,
                        stones = showMap["stones"] as? Boolean ?: true,
                    customPrice = showMap["custom_price"] as? Boolean ?: true,
                    materialWeight = (showMap["material_weight"] as? Boolean) ?: (showMap["gold_weight"] as? Boolean) ?: true, // Backward compatibility
                    stoneWeight = showMap["stone_weight"] as? Boolean ?: true,
                    makingPercent = showMap["making_percent"] as? Boolean ?: true,
                    labourCharges = showMap["labour_charges"] as? Boolean ?: true,
                    effectiveWeight = showMap["effective_weight"] as? Boolean ?: true,
                    effectiveMetalWeight = (showMap["effective_metal_weight"] as? Boolean) ?: (showMap["effective_gold_weight"] as? Boolean) ?: true, // Backward compatibility
                    labourRate = showMap["labour_rate"] as? Boolean ?: true,
                    stoneAmount = showMap["stone_amount"] as? Boolean ?: true,
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

    override suspend fun addProduct(product: Product): String = withContext(Dispatchers.IO) {
        println("=== ProductRepository.addProduct() called ===")
        println("Product details - Name: '${product.name}', ID: '${product.id}'")
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
            productMap["material_id"] = product.materialId.takeIf { it.isNotBlank() }
            productMap["material_type"] = product.materialType.takeIf { it.isNotBlank() }
            productMap["quantity"] = if (product.quantity > 0) product.quantity.toString() else null
            productMap["total_weight"] = if (product.totalWeight > 0) product.totalWeight.toString() else null
            productMap["has_stones"] = product.hasStones
            // Store stones array
            productMap["stones"] = product.stones.map { stone ->
                mapOf(
                    "name" to stone.name,
                    "purity" to stone.purity,
                    "quantity" to stone.quantity.toString(),
                    "rate" to stone.rate.toString(),
                    "weight" to stone.weight.toString(),
                    "amount" to stone.amount.toString()
                )
            }
            productMap["material_weight"] = if (product.materialWeight > 0) product.materialWeight.toString() else null
            productMap["stone_weight"] = if (product.stoneWeight > 0) product.stoneWeight.toString() else null
            // Store stone_rate (sum of all stone amounts/prices)
            productMap["stone_rate"] = if (product.stoneAmount > 0) product.stoneAmount.toString() else null
            productMap["making_percent"] = if (product.makingPercent > 0) product.makingPercent.toString() else null
            productMap["labour_charges"] = if (product.labourCharges > 0) product.labourCharges.toString() else null
            productMap["effective_weight"] = if (product.effectiveWeight > 0) product.effectiveWeight.toString() else null
            productMap["effective_metal_weight"] = if (product.effectiveMetalWeight > 0) product.effectiveMetalWeight.toString() else null
            productMap["labour_rate"] = if (product.labourRate > 0) product.labourRate.toString() else null
            productMap["has_custom_price"] = product.hasCustomPrice
            productMap["custom_price"] = if (product.customPrice > 0) product.customPrice.toString() else null
            productMap["available"] = product.available
            productMap["featured"] = product.featured
            productMap["images"] = product.images.takeIf { it.isNotEmpty() } ?: emptyList<String>()
            productMap["auto_generate_id"] = product.autoGenerateId
            // Collection product fields
            productMap["is_collection_product"] = product.isCollectionProduct
            productMap["collection_id"] = product.collectionId.takeIf { it.isNotBlank() }

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
                "has_stones" to product.show.hasStones,
                "stones" to product.show.stones,
                "custom_price" to product.show.customPrice,
                "material_weight" to product.show.materialWeight,
                "stone_weight" to product.show.stoneWeight,
                "making_percent" to product.show.makingPercent,
                "labour_charges" to product.show.labourCharges,
                "effective_weight" to product.show.effectiveWeight,
                "effective_metal_weight" to product.show.effectiveMetalWeight,
                "labour_rate" to product.show.labourRate,
                "stone_amount" to product.show.stoneAmount,
                "images" to product.show.images,
                "available" to product.show.available,
                "featured" to product.show.featured,
                "is_collection_product" to product.show.isCollectionProduct,
                "collection_id" to product.show.collectionId
            )
            productMap["show"] = showMap

        println("📝 Product data prepared for Firestore: ${productMap.keys}")
        println("📝 Key values - Name: '${productMap["name"]}'")
        println("📝 Note: total_product_cost is not stored in Firestore")

        // Use Firebase transaction to atomically create product and update category/featured/collection lists
        val categoryProductsRef = product.categoryId.takeIf { it.isNotBlank() }?.let { 
            firestore.collection("category_products").document(it) 
        }
        val featuredProductsRef = if (product.featured) {
            firestore.collection("featured_products").document("featured_list")
        } else null
        val collectionRef = if (product.isCollectionProduct && product.collectionId.isNotBlank()) {
            firestore.collection("themed_collections").document(product.collectionId)
        } else null

        firestore.runTransaction { transaction: Transaction ->
            // STEP 1: ALL READS FIRST (Firebase requirement)
            // Wrap reads in try-catch to prevent transaction failure if documents don't exist
            var categoryDoc: DocumentSnapshot? = null
            var featuredDoc: DocumentSnapshot? = null
            var collectionDoc: DocumentSnapshot? = null
            
            // Read category document if needed
            categoryProductsRef?.let { catRef ->
                try {
                    println("📂 Reading category: ${product.categoryId}")
                    categoryDoc = transaction.getDocument(catRef)
                } catch (e: Exception) {
                    println("⚠️ Failed to read category document (non-critical): ${e.message}")
                    // Continue - product creation will still succeed
                }
            }
            
            // Read featured_products document if needed
            featuredProductsRef?.let { featuredRef ->
                try {
                    println("⭐ Reading featured_products collection")
                    featuredDoc = transaction.getDocument(featuredRef)
                } catch (e: Exception) {
                    println("⚠️ Failed to read featured_products document (non-critical): ${e.message}")
                    // Continue - product creation will still succeed
                }
            }
            
            // Read themed collection document if needed
            collectionRef?.let { collRef ->
                try {
                    println("🎨 Reading themed collection: ${product.collectionId}")
                    collectionDoc = transaction.getDocument(collRef)
                } catch (e: Exception) {
                    println("⚠️ Failed to read themed collection document (non-critical): ${e.message}")
                    // Continue - product creation will still succeed
                }
            }
            
            // STEP 2: ALL WRITES AFTER READS
            // Create product document
            transaction.set(docRef, productMap)
            println("✅ Product document created in Firestore with ID: $newProductId")

            // Add to category_products
            categoryProductsRef?.let { catRef ->
                println("📂 Adding product to category: ${product.categoryId}")
                if (categoryDoc?.exists() == true) {
                    val data = categoryDoc?.data
                    val currentProductIds = (data?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val updatedProductIds = currentProductIds + newProductId
                    transaction.update(catRef, "product_ids", updatedProductIds)
                    println("✅ Updated category with new product ID")
                } else {
                    transaction.set(catRef, mapOf("product_ids" to listOf(newProductId)))
                    println("✅ Created new category document with product ID")
                }
            }

            // Add to featured_products if product is featured
            featuredProductsRef?.let { featuredRef ->
                println("⭐ Adding product to featured_products collection")
                if (featuredDoc?.exists() == true) {
                    val data = featuredDoc?.data
                    val currentFeaturedIds = (data?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    if (!currentFeaturedIds.contains(newProductId)) {
                        val updatedFeaturedIds = currentFeaturedIds + newProductId
                        transaction.update(featuredRef, "product_ids", updatedFeaturedIds)
                        println("✅ Added product to featured_products list")
                    } else {
                        println("ℹ️ Product already in featured_products list")
                    }
                } else {
                    transaction.set(featuredRef, mapOf("product_ids" to listOf(newProductId)))
                    println("✅ Created featured_products document with product ID")
                }
            }

            // Add to themed collection if product is a collection product
            collectionRef?.let { collRef ->
                println("🎨 Adding product to themed collection: ${product.collectionId}")
                if (collectionDoc?.exists() == true) {
                    val data = collectionDoc?.data
                    val currentProductIds = (data?.get("productIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    if (!currentProductIds.contains(newProductId)) {
                        val updatedProductIds = currentProductIds + newProductId
                        transaction.update(collRef, "productIds", updatedProductIds)
                        println("✅ Added product to themed collection")
                    } else {
                        println("ℹ️ Product already in themed collection")
                    }
                } else {
                    println("⚠️ Themed collection not found: ${product.collectionId}")
                }
            }
            Unit // Return Unit for transaction function
        }.get()

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
            productMap["material_id"] = product.materialId.takeIf { it.isNotBlank() }
            productMap["material_type"] = product.materialType.takeIf { it.isNotBlank() }
            productMap["material_name"] = product.materialName.takeIf { it.isNotBlank() }
            productMap["quantity"] = if (product.quantity > 0) product.quantity.toString() else null
            productMap["total_weight"] = if (product.totalWeight > 0) product.totalWeight.toString() else null
            productMap["has_stones"] = product.hasStones
            // Store stones array
            productMap["stones"] = product.stones.map { stone ->
                mapOf(
                    "name" to stone.name,
                    "purity" to stone.purity,
                    "quantity" to stone.quantity.toString(),
                    "rate" to stone.rate.toString(),
                    "weight" to stone.weight.toString(),
                    "amount" to stone.amount.toString()
                )
            }
            productMap["material_weight"] = if (product.materialWeight > 0) product.materialWeight.toString() else null
            productMap["stone_weight"] = if (product.stoneWeight > 0) product.stoneWeight.toString() else null
            // Store stone_rate (sum of all stone amounts/prices)
            productMap["stone_rate"] = if (product.stoneAmount > 0) product.stoneAmount.toString() else null
            productMap["making_percent"] = if (product.makingPercent > 0) product.makingPercent.toString() else null
            productMap["labour_charges"] = if (product.labourCharges > 0) product.labourCharges.toString() else null
            productMap["effective_weight"] = if (product.effectiveWeight > 0) product.effectiveWeight.toString() else null
            productMap["effective_metal_weight"] = if (product.effectiveMetalWeight > 0) product.effectiveMetalWeight.toString() else null
            productMap["labour_rate"] = if (product.labourRate > 0) product.labourRate.toString() else null
            productMap["has_custom_price"] = product.hasCustomPrice
            productMap["custom_price"] = if (product.customPrice > 0) product.customPrice.toString() else null
            productMap["available"] = product.available
            productMap["featured"] = product.featured
            productMap["images"] = product.images.takeIf { it.isNotEmpty() } ?: emptyList<String>()
            productMap["auto_generate_id"] = product.autoGenerateId
            // Collection product fields
            productMap["is_collection_product"] = product.isCollectionProduct
            productMap["collection_id"] = product.collectionId.takeIf { it.isNotBlank() }

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
                "has_stones" to product.show.hasStones,
                "stones" to product.show.stones,
                "custom_price" to product.show.customPrice,
                "material_weight" to product.show.materialWeight,
                "stone_weight" to product.show.stoneWeight,
                "making_percent" to product.show.makingPercent,
                "labour_charges" to product.show.labourCharges,
                "effective_weight" to product.show.effectiveWeight,
                "effective_metal_weight" to product.show.effectiveMetalWeight,
                "labour_rate" to product.show.labourRate,
                "stone_amount" to product.show.stoneAmount,
                "images" to product.show.images,
                "available" to product.show.available,
                "featured" to product.show.featured,
                "is_collection_product" to product.show.isCollectionProduct,
                "collection_id" to product.show.collectionId
            )
            productMap["show"] = showMap

            // Use Firebase transaction to atomically update product and category/featured/collection lists
            val featuredProductsRef = firestore.collection("featured_products").document("featured_list")
            val collectionRef = if (product.isCollectionProduct && product.collectionId.isNotBlank()) {
                firestore.collection("themed_collections").document(product.collectionId)
            } else null

            // WORKAROUND FOR TRANSACTION QUERY LIMITATION:
            // Query collections containing this product BEFORE the transaction
            // Then use transaction.get() for each reference inside the transaction
            // Note: This creates a potential race condition - collections queried here may change before transaction executes
            // However, this is acceptable as the transaction will validate the product is still in the collection before removing
            val collectionsToRemoveFrom = if (!product.isCollectionProduct || product.collectionId.isBlank()) {
                try {
                    val collectionsQuery = firestore.collection("themed_collections")
                        .whereArrayContains("productIds", product.id)
                        .get()
                        .get()
                    val refs = collectionsQuery.documents.map { it.reference }
                    println("📋 Found ${refs.size} themed collection(s) containing product ${product.id}")
                    refs
                } catch (e: Exception) {
                    println("⚠️ Error querying collections for product removal: ${e.message}")
                    emptyList()
                }
            } else {
                emptyList()
            }

            firestore.runTransaction { transaction: Transaction ->
                // STEP 1: ALL READS FIRST (Firebase requirement)
                val featuredDoc = transaction.getDocument(featuredProductsRef)
                var collectionDoc: DocumentSnapshot? = null
                if (product.isCollectionProduct && product.collectionId.isNotBlank()) {
                    collectionRef?.let { collRef ->
                        println("🎨 Reading themed collection: ${product.collectionId}")
                        collectionDoc = transaction.getDocument(collRef)
                    }
                }
                
                // Read all collections that need to be updated (for removal)
                // Validate that product is still in each collection (may have been removed by another transaction)
                val collectionDocsToUpdate = mutableMapOf<com.google.cloud.firestore.DocumentReference, DocumentSnapshot>()
                if (!product.isCollectionProduct || product.collectionId.isBlank()) {
                    collectionsToRemoveFrom.forEach { collRef ->
                        try {
                            val docSnapshot = transaction.getDocument(collRef)
                            // Verify product is still in the collection before adding to update list
                            if (docSnapshot.exists()) {
                                val data = docSnapshot.data
                                val currentProductIds = (data?.get("productIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                if (currentProductIds.contains(product.id)) {
                                    collectionDocsToUpdate[collRef] = docSnapshot
                                } else {
                                    println("ℹ️ Product ${product.id} no longer in collection ${collRef.id} (may have been removed by another transaction)")
                                }
                            }
                        } catch (e: Exception) {
                            println("⚠️ Failed to read collection ${collRef.id} in transaction (non-critical): ${e.message}")
                            // Continue - don't fail entire transaction
                        }
                    }
                }
                
                // STEP 2: ALL WRITES AFTER READS
                // Update product document
                transaction.update(docRef, productMap)

                // Handle featured_products collection
                if (product.featured) {
                    // Add to featured_products if not already there
                    println("⭐ Updating featured_products: adding product ${product.id}")
                    if (featuredDoc.exists()) {
                        val data = featuredDoc.data
                        val currentFeaturedIds = (data?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        if (!currentFeaturedIds.contains(product.id)) {
                            val updatedFeaturedIds = currentFeaturedIds + product.id
                            transaction.update(featuredProductsRef, "product_ids", updatedFeaturedIds)
                            println("✅ Added product to featured_products list")
                        } else {
                            println("ℹ️ Product already in featured_products list")
                        }
                    } else {
                        transaction.set(featuredProductsRef, mapOf("product_ids" to listOf(product.id)))
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
                            transaction.update(featuredProductsRef, "product_ids", updatedFeaturedIds)
                            println("✅ Removed product from featured_products list")
                        } else {
                            println("ℹ️ Product not in featured_products list")
                        }
                    }
                }

                // Handle themed collection
                if (product.isCollectionProduct && product.collectionId.isNotBlank()) {
                    println("🎨 Updating themed collection: adding product ${product.id} to ${product.collectionId}")
                    collectionRef?.let { collRef ->
                        if (collectionDoc?.exists() == true) {
                            val data = collectionDoc?.data
                            val currentProductIds = (data?.get("productIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            if (!currentProductIds.contains(product.id)) {
                                val updatedProductIds = currentProductIds + product.id
                                transaction.update(collRef, "productIds", updatedProductIds)
                                println("✅ Added product to themed collection")
                            } else {
                                println("ℹ️ Product already in themed collection")
                            }
                        } else {
                            println("⚠️ Themed collection not found: ${product.collectionId}")
                        }
                    }
                } else {
                    // Remove from all themed collections that contain this product
                    // We queried these collections BEFORE the transaction, now we update them atomically
                    println("🎨 Updating themed collection: removing product ${product.id} from ${collectionsToRemoveFrom.size} collection(s)")
                    collectionDocsToUpdate.forEach { (collRef, docSnapshot) ->
                        if (docSnapshot.exists()) {
                            val data = docSnapshot.data
                            val currentProductIds = (data?.get("productIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            if (currentProductIds.contains(product.id)) {
                                val updatedProductIds = currentProductIds.filter { it != product.id }
                                transaction.update(collRef, "productIds", updatedProductIds)
                                transaction.update(collRef, "updatedAt", System.currentTimeMillis())
                                println("✅ Removed product from themed collection: ${collRef.id}")
                            }
                        }
                    }
                }
            }.get()

            true
        } catch (e: Exception) {
            println("❌ Error updating product: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteProduct(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            println("🗑️ PRODUCT REPOSITORY: Starting product deletion")
            println("   - Product ID: $id")
            
            val productRef = firestore.collection("products").document(id)
            val featuredProductsRef = firestore.collection("featured_products").document("featured_list")
            
            // First, read product to get categoryId and collectionId (outside transaction since we need it to determine refs)
            // This is safe because we only use it to determine which documents to read in the transaction
            // The transaction will re-read the product to ensure it still exists
            val productDoc = try {
                productRef.get().get()
            } catch (e: Exception) {
                println("❌ PRODUCT REPOSITORY: Error reading product document: ${e.message}")
                return@withContext false
            }
            
            if (!productDoc.exists()) {
                println("⚠️ PRODUCT REPOSITORY: Product document does not exist: $id")
                return@withContext false
            }
            
            val data = productDoc.data
            val categoryId = data?.get("category_id") as? String
            val collectionId = data?.get("collection_id") as? String
            val categoryProductsRef = categoryId?.takeIf { it.isNotBlank() }?.let {
                firestore.collection("category_products").document(it)
            }
            
            // Query themed collections containing this product BEFORE the transaction
            // (Firebase transactions cannot perform queries)
            val collectionsToRemoveFrom = try {
                val collectionsQuery = firestore.collection("themed_collections")
                    .whereArrayContains("productIds", id)
                    .get()
                    .get()
                collectionsQuery.documents.map { it.reference }
            } catch (e: Exception) {
                println("⚠️ PRODUCT REPOSITORY: Error querying themed collections (non-critical): ${e.message}")
                emptyList()
            }
            
            println("   - Found ${collectionsToRemoveFrom.size} themed collection(s) containing this product")
            
            // Use Firebase transaction to atomically delete product and update all related lists
            // Retry up to 3 times for transaction conflicts
            var retries = 3
            var success = false
            var lastError: Exception? = null
            
            while (retries > 0 && !success) {
                try {
                    firestore.runTransaction { transaction: Transaction ->
                        // STEP 1: ALL READS FIRST (Firebase requirement)
                        
                        // Read product document in transaction
                        val productDocSnapshot = transaction.getDocument(productRef)
                        if (!productDocSnapshot.exists()) {
                            throw Exception("Product not found: $id")
                        }
                        
                        // Read category_products if category exists
                        categoryProductsRef?.let { catRef ->
                            try {
                                val categoryDocSnapshot = transaction.getDocument(catRef)
                                if (categoryDocSnapshot.exists()) {
                                    val categoryData = categoryDocSnapshot.data
                                    val currentProductIds = (categoryData?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                    if (currentProductIds.contains(id)) {
                                        val updatedProductIds = currentProductIds.filter { it != id }
                                        transaction.update(catRef, "product_ids", updatedProductIds)
                                        println("✅ PRODUCT REPOSITORY: Removed product from category_products list")
                                    }
                                }
                            } catch (e: Exception) {
                                // Non-critical: category_products might not exist, continue with deletion
                                println("⚠️ PRODUCT REPOSITORY: Could not update category_products (non-critical): ${e.message}")
                            }
                        }
                        
                        // Read featured_products
                        try {
                            val featuredDocSnapshot = transaction.getDocument(featuredProductsRef)
                            if (featuredDocSnapshot.exists()) {
                                val featuredData = featuredDocSnapshot.data
                                val currentFeaturedIds = (featuredData?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                if (currentFeaturedIds.contains(id)) {
                                    val updatedFeaturedIds = currentFeaturedIds.filter { it != id }
                                    transaction.update(featuredProductsRef, "product_ids", updatedFeaturedIds)
                                    println("✅ PRODUCT REPOSITORY: Removed product from featured_products list")
                                }
                            }
                        } catch (e: Exception) {
                            // Non-critical: featured_products might not exist, continue with deletion
                            println("⚠️ PRODUCT REPOSITORY: Could not update featured_products (non-critical): ${e.message}")
                        }
                        
                        // Read themed collections that need to be updated
                        val collectionDocsToUpdate = mutableMapOf<com.google.cloud.firestore.DocumentReference, DocumentSnapshot>()
                        collectionsToRemoveFrom.forEach { collRef ->
                            try {
                                collectionDocsToUpdate[collRef] = transaction.getDocument(collRef)
                            } catch (e: Exception) {
                                println("⚠️ PRODUCT REPOSITORY: Could not read themed collection ${collRef.id} (non-critical): ${e.message}")
                            }
                        }
                        
                        // STEP 2: ALL WRITES AFTER READS
                        
                        // Update themed collections
                        collectionDocsToUpdate.forEach { (collRef, docSnapshot) ->
                            try {
                                if (docSnapshot.exists()) {
                                    val collData = docSnapshot.data
                                    val currentProductIds = (collData?.get("productIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                    if (currentProductIds.contains(id)) {
                                        val updatedProductIds = currentProductIds.filter { it != id }
                                        transaction.update(collRef, "productIds", updatedProductIds)
                                        transaction.update(collRef, "updatedAt", System.currentTimeMillis())
                                        println("✅ PRODUCT REPOSITORY: Removed product from themed collection: ${collRef.id}")
                                    }
                                }
                            } catch (e: Exception) {
                                // Non-critical: continue with deletion even if collection update fails
                                println("⚠️ PRODUCT REPOSITORY: Could not update themed collection ${collRef.id} (non-critical): ${e.message}")
                            }
                        }
                        
                        // Delete the product document (this is the critical operation)
                        transaction.delete(productRef)
                        
                        println("✅ PRODUCT REPOSITORY: Product deleted atomically with all related updates")
                        Unit
                    }.get()
                    success = true
                } catch (e: Exception) {
                    lastError = e
                    retries--
                    
                    // Check if error is retryable (transaction conflict)
                    val isRetryable = e.message?.contains("ABORTED", ignoreCase = true) == true ||
                            (e is com.google.cloud.firestore.FirestoreException && 
                             e.message?.contains("ABORTED", ignoreCase = true) == true)
                    
                    if (retries == 0 || !isRetryable) {
                        // Not retryable or out of retries - log and throw
                        println("❌ PRODUCT REPOSITORY: Product deletion failed: ${e.message}")
                        e.printStackTrace()
                        throw e
                    }
                    
                    // Wait before retry (exponential backoff)
                    val delayMs = 100L * (3 - retries)
                    println("⚠️ PRODUCT REPOSITORY: Transaction conflict, retrying in ${delayMs}ms (${retries} retries left)")
                    kotlinx.coroutines.delay(delayMs)
                }
            }
            
            if (!success) {
                throw lastError ?: Exception("Failed to delete product after retries")
            }
            
            println("✅ PRODUCT REPOSITORY: Product deletion completed successfully")
            true
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ PRODUCT REPOSITORY: Firestore error deleting product: ${e.message}")
            e.printStackTrace()
            false
        } catch (e: Exception) {
            println("❌ PRODUCT REPOSITORY: Error deleting product: ${e.message}")
            e.printStackTrace()
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
                categoryType = data["category_type"] as? String ?: "JEWELRY",
                isActive = data["is_active"] as? Boolean ?: true,
                createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }.sortedBy { it.order } // Sort by order field
    }

    override suspend fun getMaterials(): List<Material> = withContext(Dispatchers.IO) {
        val materialsCollection = firestore.collection("materials")
        val future = materialsCollection.get()

        val snapshot = future.get()
        snapshot.documents.map { doc ->
            val data = doc.data
            // Parse types array - can be List<String> (old format) or List<Map> (new format)
            val typesList = mutableListOf<MaterialType>()
            val typesData = data["types"]
            
            when (typesData) {
                is List<*> -> {
                    typesData.forEach { typeItem ->
                        when (typeItem) {
                            is String -> {
                                // Old format: just string types, convert to MaterialType with empty rate
                                // Normalize purity (22, 22K, 22k should be same)
                                val normalizedPurity = org.example.project.data.normalizeMaterialType(typeItem)
                                typesList.add(MaterialType(purity = normalizedPurity, rate = ""))
                            }
                            is Map<*, *> -> {
                                // New format: map with purity and rate
                                val purity = (typeItem["purity"] as? String) ?: (typeItem["purity"] as? Number)?.toString() ?: ""
                                val rate = (typeItem["rate"] as? String) ?: (typeItem["rate"] as? Number)?.toString() ?: ""
                                // Normalize purity (22, 22K, 22k should be same)
                                val normalizedPurity = org.example.project.data.normalizeMaterialType(purity)
                                typesList.add(MaterialType(purity = normalizedPurity, rate = rate))
                            }
                        }
                    }
                }
            }
            
            Material(
                id = doc.id,
                name = data["name"] as? String ?: "",
                imageUrl = data["image_url"] as? String ?: "",
                types = typesList,
                createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
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
            "category_type" to "JEWELRY",
            "is_active" to true,
            "created_at" to System.currentTimeMillis()
        )
        docRef.set(data).get()
        id
    }

    override suspend fun addCompleteCategory(category: Category): String = withContext(Dispatchers.IO) {
        val categoriesCollection = firestore.collection("categories")
        val docRef = categoriesCollection.document()
        val id = docRef.id
        val data = mapOf(
            "name" to category.name,
            "description" to category.description,
            "image_url" to category.imageUrl,
            "has_gender_variants" to category.hasGenderVariants,
            "order" to category.order,
            "category_type" to category.categoryType,
            "is_active" to category.isActive,
            "created_at" to category.createdAt
        )
        docRef.set(data).get()
        println("✅ Category created in Firestore with ID: $id")
        id
    }

    override suspend fun updateCategory(category: Category): Boolean = withContext(Dispatchers.IO) {
        try {
            val categoriesCollection = firestore.collection("categories")
            val docRef = categoriesCollection.document(category.id)
            val data = mapOf(
                "name" to category.name,
                "description" to category.description,
                "image_url" to category.imageUrl,
                "has_gender_variants" to category.hasGenderVariants,
                "order" to category.order,
                "category_type" to category.categoryType,
                "is_active" to category.isActive,
                "created_at" to category.createdAt
            )
            docRef.set(data).get()
            println("✅ Category updated in Firestore with ID: ${category.id}")
            true
        } catch (e: Exception) {
            println("❌ Error updating category: ${e.message}")
            false
        }
    }

    override suspend fun deleteCategory(categoryId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val categoriesCollection = firestore.collection("categories")
            val docRef = categoriesCollection.document(categoryId)
            docRef.delete().get()
            println("✅ Category deleted from Firestore with ID: $categoryId")
            true
        } catch (e: Exception) {
            println("❌ Error deleting category: ${e.message}")
            false
        }
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
            
            // Use transaction to atomically read and update types array
            firestore.runTransaction { transaction: Transaction ->
                val materialDocSnapshot = transaction.getDocument(materialRef)
                if (!materialDocSnapshot.exists()) {
                    throw Exception("Material document not found: $materialId")
                }
                
                val data = materialDocSnapshot.data ?: throw Exception("Material document has no data")
                val currentTypes = (data["types"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                
                // Check if type already exists (case-insensitive)
                if (currentTypes.any { it.equals(type, ignoreCase = true) }) {
                    // Type already exists, no update needed
                    return@runTransaction Unit
                }
                
                val updated = currentTypes + type
                transaction.update(materialRef, "types", updated)
                Unit
            }.get()
            
            true
        } catch (e: Exception) {
            println("❌ Error adding material type: ${e.message}")
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

// Helper function to parse product stones from Firestore
private fun parseProductStones(data: Any?): List<ProductStone> {
    return when (data) {
        is List<*> -> {
            data.mapNotNull { item ->
                when (item) {
                    is Map<*, *> -> {
                        ProductStone(
                            name = (item["name"] as? String) ?: "",
                            purity = (item["purity"] as? String) ?: "",
                            quantity = (item["quantity"] as? Number)?.toDouble() ?: (item["quantity"] as? String)?.toDoubleOrNull() ?: 0.0,
                            rate = (item["rate"] as? Number)?.toDouble() ?: (item["rate"] as? String)?.toDoubleOrNull() ?: 0.0,
                            weight = (item["weight"] as? Number)?.toDouble() ?: (item["weight"] as? String)?.toDoubleOrNull() ?: (item["cw_weight"] as? Number)?.toDouble() ?: (item["cw_weight"] as? String)?.toDoubleOrNull() ?: 0.0, // Backward compatibility: support both weight and cw_weight
                            amount = (item["amount"] as? Number)?.toDouble() ?: (item["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                        )
                    }
                    else -> null
                }
            }
        }
        else -> emptyList()
    }
}

// Helper function to parse product stones from old format (backward compatibility)
private fun parseProductStonesFromOldFormat(
    stoneName: String?,
    stoneQuantity: Double?,
    stoneRate: Double?,
    stoneAmount: Double?,
    cwWeight: Double?
): List<ProductStone> {
    if (stoneName.isNullOrBlank()) return emptyList()
    
    return listOf(
        ProductStone(
            name = stoneName,
            purity = "", // Old format didn't have purity
            quantity = stoneQuantity ?: 0.0,
            rate = stoneRate ?: 0.0,
            weight = cwWeight ?: 0.0, // Renamed from cwWeight to weight
            amount = stoneAmount ?: 0.0
        )
    )
}
