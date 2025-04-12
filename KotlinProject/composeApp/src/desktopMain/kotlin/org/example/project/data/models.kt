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