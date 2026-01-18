package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.JewelryAppInitializer
import org.example.project.utils.ImageLoader
import org.example.project.viewModels.CartViewModel
import org.example.project.viewModels.CustomerViewModel
import org.example.project.viewModels.ProductsViewModel
import org.example.project.viewModels.PaymentViewModel
import org.example.project.viewModels.InvoiceViewModel
import org.example.project.data.StoreInfoRepository
import org.example.project.data.FirestoreOrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class BillingStep {
    CUSTOMER, CART, PAYMENT, RECEIPT
}

@Composable
fun BillingScreen(
    customerViewModel: CustomerViewModel = JewelryAppInitializer.getCustomerViewModel(),
    cartViewModel: CartViewModel = JewelryAppInitializer.getCartViewModel(),
    productsViewModel: ProductsViewModel = JewelryAppInitializer.getViewModel(),
    imageLoader: ImageLoader = JewelryAppInitializer.getImageLoader(),
    paymentViewModel: PaymentViewModel = JewelryAppInitializer.getPaymentViewModel()
) {
    var currentStep by remember { mutableStateOf(BillingStep.CUSTOMER) }
    
    // Access state for validation
    val selectedCustomer by customerViewModel.selectedCustomer
    val cart by cartViewModel.cart

    // Prevent invalid navigation with LaunchedEffect
    LaunchedEffect(currentStep, selectedCustomer, cart.items.size) {
        when (currentStep) {
            BillingStep.CART -> {
                // If no customer selected, go back to CUSTOMER step
                if (selectedCustomer == null) {
                    currentStep = BillingStep.CUSTOMER
                }
            }
            BillingStep.PAYMENT -> {
                // If cart is empty, go back to CART step
                if (cart.items.isEmpty()) {
                    currentStep = BillingStep.CART
                }
            }
            else -> { /* No validation needed */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Compact Step indicator without card
        CompactStepIndicator(
            currentStep = currentStep,
            onStepSelected = { newStep ->
                // Validate before allowing navigation
                when (newStep) {
                    BillingStep.CART -> {
                        // Validate customer is selected
                        if (customerViewModel.selectedCustomer.value == null) {
                            // Don't navigate - stay on CUSTOMER step
                            return@CompactStepIndicator
                        }
                    }
                    BillingStep.PAYMENT -> {
                        // Validate cart has items
                        if (cartViewModel.cart.value.items.isEmpty()) {
                            // Don't navigate - stay on CART step
                            return@CompactStepIndicator
                        }
                    }
                    else -> { /* Allow navigation */ }
                }
                currentStep = newStep
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Content based on current step
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                when (currentStep) {
                    BillingStep.CUSTOMER -> CustomerSelectionStep(
                        viewModel = customerViewModel,
                        onCustomerSelected = { customer ->
                            // Set the customer ID in the cart to ensure proper reference to users collection
                            cartViewModel.setCustomerId(customer.id)
                        },
                        onContinue = {
                            // Don't clear cart here - preserve cart data when navigating
                            // Cart should only be cleared when:
                            // 1. Starting a new order (onStartNewOrder in ReceiptScreen)
                            // 2. After successful payment (onPaymentComplete)
                            currentStep = BillingStep.CART
                        }
                    )
                    BillingStep.CART -> {
                        ShopMainScreen(
                            productsViewModel = productsViewModel,
                            cartViewModel = cartViewModel,
                            imageLoader = imageLoader,
                            onProceedToPayment = { currentStep = BillingStep.PAYMENT }
                        )
                    }
                    BillingStep.PAYMENT -> PaymentScreen(
                        paymentViewModel = paymentViewModel,
                        cartViewModel = cartViewModel,
                        customerViewModel = customerViewModel,
                        onBack = {
                            currentStep = BillingStep.CART
                            paymentViewModel.resetPaymentState()
                        },
                        onPaymentComplete = {
                            cartViewModel.clearCart()
                            currentStep = BillingStep.RECEIPT
                        },
                        productsViewModel= productsViewModel
                    )
                    BillingStep.RECEIPT -> {
                        // Use InvoiceScreen with inline editing capability
                        val invoiceViewModel = remember { InvoiceViewModel() }
                        val lastTransaction by paymentViewModel.lastTransaction
                        val selectedCustomer by customerViewModel.selectedCustomer
                        val orderId = lastTransaction?.id
                        val orderRepository = remember { FirestoreOrderRepository(JewelryAppInitializer.getFirestore()) }
                        val storeInfoRepository = remember { StoreInfoRepository() }
                        var order by remember { mutableStateOf<org.example.project.data.Order?>(null) }
                        
                        // Fetch order when orderId is available
                        LaunchedEffect(orderId) {
                            if (orderId != null) {
                                order = withContext(Dispatchers.IO) {
                                    orderRepository.getOrderById(orderId)
                                }
                            }
                        }
                        
                        // Initialize InvoiceViewModel from Order when all data is available
                        LaunchedEffect(order, selectedCustomer, productsViewModel.products.value) {
                            if (order != null && selectedCustomer != null) {
                                val products = productsViewModel.products.value.associateBy { it.id }
                                val cartItems = lastTransaction?.items
                                
                                withContext(Dispatchers.IO) {
                                    val storeInfo = storeInfoRepository.getStoreInfo()
                                    invoiceViewModel.initializeFromOrder(
                                        order = order!!,
                                        customer = selectedCustomer!!,
                                        storeInfo = storeInfo,
                                        products = products,
                                        cartItems = cartItems
                                    )
                                }
                            }
                        }
                        
                        InvoiceScreen(
                            viewModel = invoiceViewModel,
                            orderId = orderId,
                            onBack = {
                                // Keep the receipt but go back to customer selection for new order
                                customerViewModel.clearSelectedCustomer()
                                cartViewModel.clearCart()
                                paymentViewModel.resetPaymentState() // Reset discount and payment state
                                currentStep = BillingStep.CUSTOMER
                            },
                        onStartNewOrder = {
                            // Reset everything and start fresh
                            customerViewModel.clearSelectedCustomer()
                            cartViewModel.clearCart()
                            paymentViewModel.resetPaymentState()
                            currentStep = BillingStep.CUSTOMER
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactStepIndicator(
    currentStep: BillingStep,
    onStepSelected: (BillingStep) -> Unit
) {
    val steps = listOf("Customer", "Cart", "Payment", "Receipt")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, stepName ->
            // Step button with rounded background
            Button(
                onClick = { onStepSelected(BillingStep.values()[index]) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = when {
                        index == currentStep.ordinal -> MaterialTheme.colors.primary
                        index < currentStep.ordinal -> MaterialTheme.colors.primary.copy(alpha = 0.7f)
                        else -> Color.LightGray
                    }
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = if (index == currentStep.ordinal) 4.dp else 2.dp
                )
            ) {
                Text(
                    text = stepName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (index == currentStep.ordinal) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Add spacing between buttons except for the last one
            if (index < steps.size - 1) {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

data class StepInfo(
    val step: BillingStep,
    val label: String,
    val icon: ImageVector
)