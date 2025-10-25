package org.example.project.data

data class CollectionImage(
    val url: String = "",
    val isActive: Boolean = true,
    val order: Int = 0
)

data class CollectionMetadata(
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "admin"
)

data class CarouselItem(
    val id: String = "",
    val title: String = "",
    val titleActive: Boolean = true,
    val subtitle: String = "",
    val subtitleActive: Boolean = true,
    val image: CarouselImage = CarouselImage(),
    val isActive: Boolean = true,
    val order: Int = 0,
    val metadata: CarouselMetadata = CarouselMetadata()
)

data class CarouselImage(
    val url: String = "",
    val isActive: Boolean = true
)

data class CarouselMetadata(
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "admin"
)

data class AppConfig(
    val id: String = "displaySettings",
    val activeCollectionIds: List<String> = emptyList(),
    val activeCarouselIds: List<String> = emptyList(),
    val collectionsEnabled: Boolean = true,
    val carouselsEnabled: Boolean = true,
    val lastPublished: Long = System.currentTimeMillis()
)

data class DraftState(
    val collections: List<ThemedCollection> = emptyList(),
    val carouselItems: List<CarouselItem> = emptyList(),
    val appConfig: AppConfig = AppConfig()
)
