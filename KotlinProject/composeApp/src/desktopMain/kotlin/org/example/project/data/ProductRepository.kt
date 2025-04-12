package org.example.project.data

// Repository.kt
import com.google.cloud.firestore.Firestore
import com.google.cloud.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ProductRepository {
    suspend fun getAllProducts(): List<Product>
    suspend fun getProductById(id: String): Product?
    suspend fun addProduct(product: Product): String
    suspend fun updateProduct(product: Product): Boolean
    suspend fun deleteProduct(id: String): Boolean
    suspend fun getCategories(): List<Category>
    suspend fun getMaterials(): List<Material>
    suspend fun getProductImage(url: String): ByteArray?
}

class FirestoreProductRepository(private val firestore: Firestore, private val storage: Storage) :
    ProductRepository {

    override suspend fun getAllProducts(): List<Product> = withContext(Dispatchers.IO) {
        val productsCollection = firestore.collection("products")
        val future = productsCollection.get()

        val snapshot = future.get()
        snapshot.documents.map { doc ->
            val data = doc.data
            Product(
                id = doc.id,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                categoryId = data["category_id"] as? String ?: "",
                materialId = data["material_id"] as? String ?: "",
                materialType = data["material_type"] as? String ?: "",
                gender = data["gender"] as? String ?: "",
                weight = data["weight"] as? String ?: "",
                available = data["available"] as? Boolean ?: true,
                featured = data["featured"] as? Boolean ?: false,
                images = (data["images"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
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
                Product(
                    id = snapshot.id,
                    name = data?.get("name") as? String ?: "",
                    description = data?.get("description") as? String ?: "",
                    price = (data?.get("price") as? Number)?.toDouble() ?: 0.0,
                    categoryId = data?.get("category_id") as? String ?: "",
                    materialId = data?.get("material_id") as? String ?: "",
                    materialType = data?.get("material_type") as? String ?: "",
                    gender = data?.get("gender") as? String ?: "",
                    weight = data?.get("weight") as? String ?: "",
                    available = data?.get("available") as? Boolean ?: true,
                    featured = data?.get("featured") as? Boolean ?: false,
                    images = (data?.get("images") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    createdAt = (data?.get("created_at") as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun addProduct(product: Product): String = withContext(Dispatchers.IO) {
        val productsCollection = firestore.collection("products")
        val docRef = productsCollection.document()
        val newProductId = docRef.id

        val productMap = mapOf(
            "id" to newProductId,
            "name" to product.name,
            "description" to product.description,
            "price" to product.price,
            "category_id" to product.categoryId,
            "material_id" to product.materialId,
            "material_type" to product.materialType,
            "gender" to product.gender,
            "weight" to product.weight,
            "available" to product.available,
            "featured" to product.featured,
            "images" to product.images,
            "created_at" to System.currentTimeMillis()
        )

        docRef.set(productMap).get()

        // Add to category_products as well
        val categoryProductsRef = firestore.collection("category_products").document(product.categoryId)
        val categoryDoc = categoryProductsRef.get().get()

        if (categoryDoc.exists()) {
            val data = categoryDoc.data
            val currentProductIds = (data?.get("product_ids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val updatedProductIds = currentProductIds + newProductId
            categoryProductsRef.update("product_ids", updatedProductIds).get()
        } else {
            categoryProductsRef.set(mapOf("product_ids" to listOf(newProductId))).get()
        }

        newProductId
    }

    override suspend fun updateProduct(product: Product): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("products").document(product.id)

            val productMap = mapOf(
                "name" to product.name,
                "description" to product.description,
                "price" to product.price,
                "category_id" to product.categoryId,
                "material_id" to product.materialId,
                "material_type" to product.materialType,
                "gender" to product.gender,
                "weight" to product.weight,
                "available" to product.available,
                "featured" to product.featured,
                "images" to product.images
            )

            docRef.update(productMap).get()
            true
        } catch (e: Exception) {
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
                order = (data["order"] as? Number)?.toInt() ?: 0
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

    override suspend fun getProductImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (url.startsWith("gs://")) {
                val bucketAndPath = url.removePrefix("gs://")
                val pathParts = bucketAndPath.split("/", limit = 2)

                if (pathParts.size == 2) {
                    val bucket = pathParts[0]
                    val path = pathParts[1]

                    val blob = storage.get(bucket, path)
                    blob?.getContent()
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}