package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PreviewHomeScreen(
    modifier: Modifier = Modifier,
    categories: List<Category> = emptyList(),
    featuredProducts: List<Product> = emptyList(),
    collections: List<ThemedCollection> = emptyList(),
    carouselItems: List<CarouselItem> = emptyList(),
    onProductClick: (String) -> Unit = {},
    onCollectionClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {}
) {

    // Hardcoded testimonials data
    val testimonials = listOf(
        Testimonial(
            name = "Akanksha Khanna",
            age = 27,
            text = "Obsessed with my engagement ring, my husband chose perfectly and it's everything I wanted in a ring. Handcrafted with love!",
            rotation = -6f,
            clipPosition = 0.3f,
            stringDepth = 0.25f
        ),
        Testimonial(
            name = "Nutan Mishra",
            age = 33,
            text = "I got a necklace for my baby boy from this brand and it's so beautiful! It gave me happiness and security knowing it's pure.",
            rotation = 4f,
            clipPosition = 0.7f,
            stringDepth = 0.35f
        ),
        Testimonial(
            name = "Sarah Johnson",
            age = 28,
            text = "Amazing quality and beautiful designs. The customer service was exceptional and I couldn't be happier!",
            rotation = -2f,
            clipPosition = 0.4f,
            stringDepth = 0.2f
        ),
        Testimonial(
            name = "Priya Sharma",
            age = 25,
            text = "The jewelry is stunning and exactly what I was looking for. Fast delivery and beautiful packaging too!",
            rotation = 5f,
            clipPosition = 0.6f,
            stringDepth = 0.4f
        )
    )

    // Hardcoded jewelry items data for the grid
    val jewelryGridItems = listOf(
        JewelryItem(isLarge = true),
        JewelryItem(),
        JewelryItem(),
        JewelryItem(),
        JewelryItem(),
        JewelryItem(),
        JewelryItem(),
        JewelryItem(isLarge = true),
        JewelryItem(),
        JewelryItem(),
        JewelryItem(),
        JewelryItem()
    )

    // Main content
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Gradient header with bangles image and promotional text
        PreviewGradientHeaderWithBangles(
            categories = categories,
            onCategoryClick = onCategoryClick
        )

        // Categories section
        if (categories.isNotEmpty()) {
            PreviewCategoryRow(categories, onCategoryClick = onCategoryClick)
        } else {
            PreviewShimmerCategoryPlaceholder()
        }

        // Recently viewed section
        if (featuredProducts.isNotEmpty()) {
            PreviewRecentlyViewedSection(
                products = featuredProducts.take(3), // Use featured products as recently viewed
                isLoading = false,
                onProductClick = onProductClick,
                onFavoriteClick = { /* Empty for now */ }
            )
        }

        // Video section
        PreviewVideoSection()
        
        Spacer(modifier = Modifier.height(32.dp))

        // Carousel section
        if (carouselItems.isNotEmpty()) {
            PreviewElegantCarouselSection(carouselItems)
        } else {
            PreviewShimmerCarouselPlaceholder()
        }

        // Featured products title
        PreviewFeaturedProductsTitle()

        // Featured products
        featuredProducts.chunked(2).forEach { productPair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                productPair.forEach { product ->
                    PreviewFeaturedProductCard(
                        product = product,
                        onProductClick = onProductClick,
                        onFavoriteClick = { /* Empty for now */ },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (productPair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Collections section
        if (collections.isNotEmpty()) {
            PreviewThemedCollectionsSection(collections, onCollectionClick)
        } else {
            PreviewShimmerCollectionsPlaceholder()
        }

        // Customer testimonials
        PreviewCustomerTestimonialsWithCurvedString(
            testimonials = testimonials,
            clipDrawableRes = 0 // TODO: Add paper clip drawable
        )

        // Jewelry grid
        PreviewExactPatternJewelryGrid(
            items = jewelryGridItems,
            modifier = Modifier.fillMaxWidth()
        )

        // Bottom spacer
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Data classes (copied from original files)
data class Testimonial(
    val name: String,
    val age: Int,
    val text: String,
    val rotation: Float = 0f,
    val clipPosition: Float = 0.5f,
    val stringDepth: Float = 0.3f
)

data class JewelryItem(
    val isLarge: Boolean = false
)

// Preview Component Functions (simplified versions for preview)
@Composable
fun PreviewGradientHeaderWithBangles(
    categories: List<Category>,
    onCategoryClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF896C6C),
                        Color(0xFFD4AF37)
                    )
                )
            )
    ) {
        // Simplified header content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Gagan Jewellers",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Premium Jewelry Collection",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun PreviewCategoryRow(categories: List<Category>, onCategoryClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        items(categories) { category ->
            PreviewCategoryItem(category, onCategoryClick)
        }
    }
}

@Composable
fun PreviewCategoryItem(category: Category, onCategoryClick: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onCategoryClick(category.id) }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFD4AF37)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = category.name,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF896C6C),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PreviewRecentlyViewedSection(
    products: List<Product>,
    isLoading: Boolean,
    onProductClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Recently Viewed",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D2D2D),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(products) { product ->
                PreviewRecentlyViewedItem(product, onProductClick, onFavoriteClick)
            }
        }
    }
}

@Composable
fun PreviewRecentlyViewedItem(
    product: Product,
    onProductClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { onProductClick(product.id) },
        elevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFFD4AF37)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = product.name,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "₹${formatCurrency(if (product.hasCustomPrice) product.customPrice else calculateProductTotalCost(product))}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF896C6C)
                )
            }
        }
    }
}

@Composable
fun PreviewVideoSection() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "#RadiantRomance",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF896C6C),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp),
            elevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Video Player",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PreviewElegantCarouselSection(items: List<CarouselItem>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Featured",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D2D2D),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(items) { item ->
                PreviewElegantCarouselItem(item)
            }
        }
    }
}

@Composable
fun PreviewElegantCarouselItem(item: CarouselItem) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp),
        elevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF896C6C).copy(alpha = 0.8f),
                            Color(0xFFD4AF37).copy(alpha = 0.8f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                if (item.titleActive) {
                    Text(
                        item.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (item.subtitleActive) {
                    Text(
                        item.subtitle,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewFeaturedProductsTitle() {
    Text(
        "Featured Products",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2D2D2D),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun PreviewFeaturedProductCard(
    product: Product,
    onProductClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onProductClick(product.id) },
        elevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFFD4AF37)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = product.name,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "₹${formatCurrency(if (product.hasCustomPrice) product.customPrice else calculateProductTotalCost(product))}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF896C6C)
                )
            }
        }
    }
}

@Composable
fun PreviewThemedCollectionsSection(
    collections: List<ThemedCollection>,
    onCollectionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Collections",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D2D2D),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(collections) { collection ->
                PreviewCollectionItem(collection, onCollectionClick)
            }
        }
    }
}

@Composable
fun PreviewCollectionItem(
    collection: ThemedCollection,
    onCollectionClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp)
            .clickable { onCollectionClick(collection.id) },
        elevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5DC))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    collection.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    collection.description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PreviewCustomerTestimonialsWithCurvedString(
    testimonials: List<Testimonial>,
    clipDrawableRes: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F5F3))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Customer Testimonials",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF896C6C),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(testimonials) { testimonial ->
                PreviewHangingTestimonialCard(testimonial)
            }
        }
    }
}

@Composable
fun PreviewHangingTestimonialCard(testimonial: Testimonial) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(300.dp),
        elevation = 4.dp,
        backgroundColor = Color(0xFFFAF8F6)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD4AF37)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Customer photo",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "${testimonial.name}, ${testimonial.age}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2D3748),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = testimonial.text,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color(0xFF4A5568),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PreviewExactPatternJewelryGrid(
    items: List<JewelryItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F6F4))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "#Editorial",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D3748),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.take(8).chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        PreviewJewelryGridItem(
                            item = item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if odd number of items
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewJewelryGridItem(
    item: JewelryItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        backgroundColor = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (item.isLarge) 200.dp else 150.dp)
                .padding(4.dp)
                .background(Color(0xFFD4AF37), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Jewelry item",
                tint = Color.White,
                modifier = Modifier.size(if (item.isLarge) 64.dp else 48.dp)
            )
        }
    }
}

// Shimmer placeholders
@Composable
fun PreviewShimmerCategoryPlaceholder() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        items(5) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun PreviewShimmerCarouselPlaceholder() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(3) {
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(160.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
fun PreviewShimmerCollectionsPlaceholder() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(4) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(120.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

/**
 * Calculate the total product cost based on the same logic used in AddEditProductScreen
 */
private fun calculateProductTotalCost(product: Product): Double {
    // Calculate net weight (total weight - less weight)
    val netWeight = (product.totalWeight - product.lessWeight).coerceAtLeast(0.0)
    
    // Material cost (net weight × material rate)
    val materialRate = getMaterialRateForProduct(product)
    val materialCost = netWeight * materialRate
    
    // Making charges (net weight × making rate)
    val makingCharges = netWeight * product.defaultMakingRate
    
    // Stone amount (if has stones)
    val stoneAmount = if (product.hasStones) {
        if (product.cwWeight > 0 && product.stoneRate > 0 && product.stoneQuantity > 0) {
            product.cwWeight * product.stoneRate * product.stoneQuantity
        } else 0.0
    } else 0.0
    
    // Total = Material Cost + Making Charges + Stone Amount + VA Charges
    return materialCost + makingCharges + stoneAmount + product.vaCharges
}

/**
 * Get material rate for a product based on material and type
 * Uses MetalRatesManager to get current rates from Firestore
 */
private fun getMaterialRateForProduct(product: Product): Double {
    val metalRates = MetalRatesManager.metalRates.value
    
    return when {
        product.materialType.contains("gold", ignoreCase = true) -> {
            // Extract karat and use appropriate gold rate
            val karat = extractKaratFromMaterialTypeString(product.materialType)
            metalRates.getGoldRateForKarat(karat)
        }
        product.materialType.contains("silver", ignoreCase = true) -> {
            // Use silver rate
            metalRates.getSilverRateForPurity(999) // Default to 999 purity
        }
        else -> {
            // Fallback to a reasonable rate
            metalRates.getGoldRateForKarat(22) // Default to 22K gold rate
        }
    }
}

/**
 * Extract karat from material type string (e.g., "18K Gold" -> 18)
 */
private fun extractKaratFromMaterialTypeString(materialType: String): Int {
    val karatRegex = Regex("""(\d+)K""", RegexOption.IGNORE_CASE)
    val match = karatRegex.find(materialType)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: 22 // Default to 22K
}
