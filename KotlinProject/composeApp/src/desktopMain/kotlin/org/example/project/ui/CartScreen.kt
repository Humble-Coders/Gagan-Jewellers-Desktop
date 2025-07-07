//package org.example.project.ui
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.Clear
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.ImageBitmap
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import org.example.project.JewelryAppInitializer
//import org.example.project.data.CartItem
//import org.example.project.viewModels.CartViewModel
//
//@Composable
//fun CartScreen(
//    cartViewModel: CartViewModel = JewelryAppInitializer.getCartViewModel(),
//    onProceedToPayment: () -> Unit = {},
//    onContinueShopping: () -> Unit = {}
//) {
//    val cart by cartViewModel.cart
//    val cartImages by cartViewModel.cartImages
//    val metalPrices by cartViewModel.metalPrices
//    val isLoading by cartViewModel.loading
//
//    LaunchedEffect(Unit) {
//        cartViewModel.loadCartImages()
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        // Header
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                "Your Cart (${cart.totalItems} Items)",
//                fontSize = 24.sp,
//                fontWeight = FontWeight.Bold
//            )
//
//            // Metal prices display
//            Column(horizontalAlignment = Alignment.End) {
//                Text(
//                    "₹${metalPrices.goldPricePerGram}/g",
//                    fontSize = 14.sp,
//                    color = Color(0xFFFFD700),
//                    fontWeight = FontWeight.Bold
//                )
//                Text(
//                    "₹${metalPrices.silverPricePerGram}/g",
//                    fontSize = 14.sp,
//                    color = Color(0xFFC0C0C0),
//                    fontWeight = FontWeight.Bold
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        if (cart.items.isEmpty()) {
//            // Empty cart state
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .weight(1f),
//                contentAlignment = Alignment.Center
//            ) {
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center
//                ) {
//                    Text(
//                        "Your cart is empty",
//                        fontSize = 18.sp,
//                        color = Color.Gray,
//                        fontWeight = FontWeight.Medium
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        "Add some products to get started",
//                        fontSize = 14.sp,
//                        color = Color.Gray
//                    )
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Button(
//                        onClick = onContinueShopping,
//                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
//                    ) {
//                        Text("Continue Shopping", color = Color.White)
//                    }
//                }
//            }
//        } else {
//            // Cart items
//            LazyColumn(
//                modifier = Modifier.weight(1f),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                items(cart.items) { cartItem ->
//                    CartItemCard(
//                        cartItem = cartItem,
//                        image = cartImages[cartItem.productId],
//                        goldPrice = metalPrices.goldPricePerGram,
//                        silverPrice = metalPrices.silverPricePerGram,
//                        onUpdateQuantity = { newQuantity ->
//                            cartViewModel.updateQuantity(cartItem.productId, newQuantity)
//                        },
//                        onUpdateWeight = { newWeight ->
//                            cartViewModel.updateWeight(cartItem.productId, newWeight)
//                        },
//                        onRemove = {
//                            cartViewModel.removeFromCart(cartItem.productId)
//                        }
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Cart summary
//            CartSummary(
//                subtotal = cartViewModel.getSubtotal(),
//                gst = cartViewModel.getGST(),
//                total = cartViewModel.getFinalTotal(),
//                onClearCart = { cartViewModel.clearCart() },
//                onProceedToPayment = onProceedToPayment
//            )
//        }
//    }
//}
//
//@Composable
//fun CartItemCard(
//    cartItem: CartItem,
//    image: ImageBitmap?,
//    goldPrice: Double,
//    silverPrice: Double,
//    onUpdateQuantity: (Int) -> Unit,
//    onUpdateWeight: (Double) -> Unit,
//    onRemove: () -> Unit
//) {
//    // Calculate price based on weight and material
//    val pricePerGram = when {
//        cartItem.product.materialType.contains("gold", ignoreCase = true) -> goldPrice
//        cartItem.product.materialType.contains("silver", ignoreCase = true) -> silverPrice
//        else -> goldPrice
//    }
//    val itemTotal = cartItem.selectedWeight * cartItem.quantity * pricePerGram
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        elevation = 4.dp,
//        shape = RoundedCornerShape(8.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Product image
//            Box(
//                modifier = Modifier
//                    .size(80.dp)
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(Color(0xFFF5F5F5))
//            ) {
//                if (image != null) {
//                    Image(
//                        bitmap = image,
//                        contentDescription = cartItem.product.name,
//                        modifier = Modifier.fillMaxSize(),
//                        contentScale = ContentScale.Crop
//                    )
//                } else {
//                    Box(
//                        modifier = Modifier.fillMaxSize(),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        CircularProgressIndicator(
//                            modifier = Modifier.size(24.dp),
//                            strokeWidth = 2.dp
//                        )
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.width(16.dp))
//
//            // Product details
//            Column(
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = cartItem.product.name,
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 16.sp,
//                    maxLines = 2,
//                    overflow = TextOverflow.Ellipsis
//                )
//
//                Spacer(modifier = Modifier.height(4.dp))
//
//                Text(
//                    text = cartItem.product.materialType,
//                    color = MaterialTheme.colors.primary,
//                    fontSize = 14.sp,
//                    fontWeight = FontWeight.Medium
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Weight input
//                Row(
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        "Weight: ",
//                        fontSize = 14.sp,
//                        color = Color.Gray
//                    )
//
//                    var weightText by remember { mutableStateOf(cartItem.selectedWeight.toString()) }
//
//                    OutlinedTextField(
//                        value = weightText,
//                        onValueChange = { newValue ->
//                            weightText = newValue
//                            newValue.toDoubleOrNull()?.let { weight ->
//                                if (weight > 0) {
//                                    onUpdateWeight(weight)
//                                }
//                            }
//                        },
//                        modifier = Modifier.width(80.dp),
//                        singleLine = true,
//                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
//                    )
//
//                    Text(
//                        " g",
//                        fontSize = 14.sp,
//                        color = Color.Gray
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Quantity controls
//                Row(
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    IconButton(
//                        onClick = { onUpdateQuantity(cartItem.quantity - 1) },
//                        modifier = Modifier.size(32.dp)
//                    ) {
//                        Icon(
//                            Icons.Default.Clear,
//                            contentDescription = "Decrease quantity",
//                            modifier = Modifier.size(16.dp)
//                        )
//                    }
//
//                    Text(
//                        text = cartItem.quantity.toString(),
//                        modifier = Modifier.padding(horizontal = 16.dp),
//                        fontWeight = FontWeight.Bold
//                    )
//
//                    IconButton(
//                        onClick = { onUpdateQuantity(cartItem.quantity + 1) },
//                        modifier = Modifier.size(32.dp)
//                    ) {
//                        Icon(
//                            Icons.Default.Add,
//                            contentDescription = "Increase quantity",
//                            modifier = Modifier.size(16.dp)
//                        )
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.width(16.dp))
//
//            // Price and remove button
//            Column(
//                horizontalAlignment = Alignment.End
//            ) {
//                Text(
//                    text = "₹${String.format("%.0f", itemTotal)}",
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 16.sp,
//                    color = MaterialTheme.colors.primary
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                IconButton(
//                    onClick = onRemove,
//                    modifier = Modifier
//                        .size(36.dp)
//                        .background(Color(0xFFFFEBEE), RoundedCornerShape(18.dp))
//                ) {
//                    Icon(
//                        Icons.Default.Delete,
//                        contentDescription = "Remove item",
//                        tint = Color(0xFFD32F2F),
//                        modifier = Modifier.size(20.dp)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun CartSummary(
//    subtotal: Double,
//    gst: Double,
//    total: Double,
//    onClearCart: () -> Unit,
//    onProceedToPayment: () -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        elevation = 4.dp,
//        shape = RoundedCornerShape(8.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            Text(
//                "Order Summary",
//                fontSize = 18.sp,
//                fontWeight = FontWeight.Bold
//            )
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Subtotal
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text("Subtotal:", fontSize = 16.sp)
//                Text(
//                    "₹${String.format("%.0f", subtotal)}",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium
//                )
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // GST
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text("GST (3.0%):", fontSize = 16.sp)
//                Text(
//                    "₹${String.format("%.0f", gst)}",
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium
//                )
//            }
//
//            Divider(modifier = Modifier.padding(vertical = 12.dp))
//
//            // Total
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(
//                    "Total:",
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold
//                )
//                Text(
//                    "₹${String.format("%.0f", total)}",
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = MaterialTheme.colors.primary
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Action buttons
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                OutlinedButton(
//                    onClick = onClearCart,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Text("Clear Cart")
//                }
//
//                Button(
//                    onClick = onProceedToPayment,
//                    modifier = Modifier.weight(2f),
//                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
//                ) {
//                    Text(
//                        "Proceed to Payment",
//                        color = Color.White,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//            }
//        }
//    }
//}