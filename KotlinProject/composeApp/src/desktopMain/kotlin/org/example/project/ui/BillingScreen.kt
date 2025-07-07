package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import org.example.project.JewelryAppInitializer
import org.example.project.utils.ImageLoader
import org.example.project.viewModels.CartViewModel
import org.example.project.viewModels.CustomerViewModel
import org.example.project.viewModels.ProductsViewModel

enum class BillingStep {
    CUSTOMER, CART, PAYMENT, RECEIPT
}

// BillingScreen.kt
@Composable
fun BillingScreen(
    customerViewModel: CustomerViewModel = JewelryAppInitializer.getCustomerViewModel(),
    cartViewModel: CartViewModel = JewelryAppInitializer.getCartViewModel(),
    productsViewModel: ProductsViewModel = JewelryAppInitializer.getViewModel(),
    imageLoader: ImageLoader = JewelryAppInitializer.getImageLoader()
) {
    var currentStep by remember { mutableStateOf(BillingStep.CUSTOMER) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            "Billing",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Step indicator
        StepIndicator(
            currentStep = currentStep,
            onStepSelected = { currentStep = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Content based on current step
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                when (currentStep) {
                    BillingStep.CUSTOMER -> CustomerSelectionStep(
                        viewModel = customerViewModel,
                        onCustomerSelected = { /* Select the customer */ },
                        onContinue = {
                            // Move to next step
                            currentStep = BillingStep.CART
                        }
                    )
                    BillingStep.CART -> {
                        // Cart Building Step - Use the existing CardBuildingScreen
                        ShopMainScreen(
                            productsViewModel = productsViewModel,
                            cartViewModel = cartViewModel,
                            imageLoader = imageLoader
                        )
                    }
                    BillingStep.PAYMENT -> Text("Payment Processing Step (Coming Soon)")
                    BillingStep.RECEIPT -> Text("Receipt Generation Step (Coming Soon)")
                }
            }
        }
    }
}

@Composable
fun StepIndicator(
    currentStep: BillingStep,
    onStepSelected: (BillingStep) -> Unit
) {
    // Define step information
    val steps = listOf(
        StepInfo(BillingStep.CUSTOMER, "Customer", Icons.Default.Person),
        StepInfo(BillingStep.CART, "Cart", Icons.Default.ShoppingCart),
        StepInfo(BillingStep.PAYMENT, "Payment", Icons.Default.Check),
        StepInfo(BillingStep.RECEIPT, "Receipt", Icons.Default.MailOutline)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, step ->
                if (index > 0) {
                    // Connector line between steps
                    Divider(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp),
                        color = if (steps[index].step.ordinal <= currentStep.ordinal)
                            MaterialTheme.colors.primary
                        else
                            Color.LightGray
                    )
                }

                // Step indicator
                StepDot(
                    step = step,
                    isActive = step.step == currentStep,
                    isCompleted = step.step.ordinal < currentStep.ordinal,
                    onClick = { onStepSelected(step.step) }
                )
            }
        }
    }
}

@Composable
fun StepDot(
    step: StepInfo,
    isActive: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(if (isHovered) 1.1f else 1f)  // Scale up when hovered
                .hoverable(interactionSource)
                .background(
                    color = when {
                        isActive -> MaterialTheme.colors.primary
                        isCompleted -> MaterialTheme.colors.primary.copy(alpha = 0.7f)
                        else -> Color.LightGray
                    },
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = when {
                        isActive -> MaterialTheme.colors.primary
                        isCompleted -> MaterialTheme.colors.primary
                        else -> Color.LightGray
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Only the icon is clickable with the newer ripple implementation
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClick),  // Use the simpler clickable without custom indication
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        step.icon,
                        contentDescription = step.label,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = step.label,
            color = when {
                isActive -> MaterialTheme.colors.primary
                isCompleted -> MaterialTheme.colors.primary
                else -> Color.Gray
            },
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            modifier = Modifier.width(70.dp)
        )
    }
}

data class StepInfo(
    val step: BillingStep,
    val label: String,
    val icon: ImageVector
)