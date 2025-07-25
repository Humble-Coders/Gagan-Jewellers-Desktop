package org.example.project.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.lightColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.JewelryAppInitializer
import org.example.project.viewModels.ProductsViewModel
import org.example.project.ui.AddEditProductScreen
import org.example.project.ui.BillingScreen
import org.example.project.ui.DashboardScreen
import org.example.project.ui.ProductDetailScreen
import org.example.project.ui.SettingsScreen

@Composable
fun JewelryApp(viewModel: ProductsViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val imageLoader = JewelryAppInitializer.getImageLoader()
    val cartViewModel = JewelryAppInitializer.getCartViewModel()
    val customerViewModel = JewelryAppInitializer.getCustomerViewModel()
    val paymentViewModel = JewelryAppInitializer.getPaymentViewModel()

    MaterialTheme(
        colors = lightColors(
            primary = Color(0xFFC9AD6E),
            primaryVariant = Color(0xFFB8973D),
            secondary = Color(0xFF7D7D7D),
            background = Color(0xFFF9F9F9),
            surface = Color.White
        )
    ) {
        Row {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(260.dp)
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFEEEEEE)))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(MaterialTheme.colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Diamond,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Jewelry Inventory",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                NavigationItem(
                    icon = Icons.Outlined.Insights,
                    title = "Dashboard",
                    selected = currentScreen == Screen.DASHBOARD,
                    onClick = { currentScreen = Screen.DASHBOARD }
                )

                NavigationItem(
                    icon = Icons.Outlined.PostAdd,
                    title = "Add Product",
                    selected = currentScreen == Screen.ADD_PRODUCT,
                    onClick = {
                        viewModel.createNewProduct()
                        currentScreen = Screen.ADD_PRODUCT
                    }
                )

                NavigationItem(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "Billing",
                    selected = currentScreen == Screen.BILLING,
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                        }
                        currentScreen = Screen.BILLING
                    }
                )

                NavigationItem(
                    icon = Icons.Outlined.Settings,
                    title = "Settings",
                    selected = currentScreen == Screen.SETTINGS,
                    onClick = { currentScreen = Screen.SETTINGS }
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    "Version 1.0",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            when (currentScreen) {
                                Screen.DASHBOARD -> "Dashboard"
                                Screen.ADD_PRODUCT -> "Add Product"
                                Screen.EDIT_PRODUCT -> "Edit Product"
                                Screen.PRODUCT_DETAIL -> "Product Details"
                                Screen.SETTINGS -> "Settings"
                                Screen.BILLING -> "Billing"
                            },
                            fontSize = 16.sp
                        )
                    },
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = Color.White,
                    elevation = 2.dp,
                    modifier = Modifier.height(48.dp),
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                viewModel.loadProducts()
                                snackbarHostState.showSnackbar("Data refreshed")
                            }
                        }) {
                            Icon(
                                Icons.Outlined.Autorenew,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        Screen.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onAddProduct = {
                                viewModel.createNewProduct()
                                currentScreen = Screen.ADD_PRODUCT
                            },
                            onViewProductDetails = { productId ->
                                viewModel.selectProduct(productId)
                                currentScreen = Screen.PRODUCT_DETAIL
                            }
                        )

                        Screen.ADD_PRODUCT -> AddEditProductScreen(
                            viewModel = viewModel,
                            onSave = {
                                currentScreen = Screen.DASHBOARD
                            },
                            onCancel = {
                                viewModel.clearCurrentProduct()
                                currentScreen = Screen.DASHBOARD
                            }
                        )

                        Screen.EDIT_PRODUCT -> AddEditProductScreen(
                            viewModel = viewModel,
                            onSave = {
                                currentScreen = Screen.DASHBOARD
                            },
                            onCancel = {
                                viewModel.clearCurrentProduct()
                                currentScreen = Screen.DASHBOARD
                            },
                            isEditing = true
                        )

                        Screen.PRODUCT_DETAIL -> ProductDetailScreen(
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onEdit = {
                                currentScreen = Screen.EDIT_PRODUCT
                            },
                            onBack = {
                                currentScreen = Screen.DASHBOARD
                            }
                        )

                        Screen.SETTINGS -> SettingsScreen()

                        Screen.BILLING -> BillingScreen(
                            customerViewModel = customerViewModel,
                            cartViewModel = cartViewModel,
                            productsViewModel = viewModel,
                            imageLoader = imageLoader,
                            paymentViewModel = paymentViewModel
                        )
                    }

                    viewModel.error.value?.let { errorMessage ->
                        if (errorMessage.isNotEmpty()) {
                            LaunchedEffect(errorMessage) {
                                snackbarHostState.showSnackbar(
                                    message = errorMessage,
                                    actionLabel = "Dismiss"
                                )
                            }
                        }
                    }

                    if (viewModel.loading.value) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) Color(0xFFEFEFEF) else Color.Transparent
    val textColor = if (selected) MaterialTheme.colors.primary else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

enum class Screen {
    DASHBOARD,
    ADD_PRODUCT,
    EDIT_PRODUCT,
    PRODUCT_DETAIL,
    SETTINGS,
    BILLING
}
