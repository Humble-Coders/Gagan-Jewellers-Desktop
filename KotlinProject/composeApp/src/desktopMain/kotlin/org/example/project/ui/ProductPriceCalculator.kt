package org.example.project.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.focus.FocusDirection
import org.example.project.utils.CurrencyFormatter
import java.text.NumberFormat
import java.util.Locale

/**
 * Data class for product price calculation inputs
 */
data class ProductPriceInputs(
    val grossWeight: Double,              // grams
    val goldPurity: String,                // e.g., "22K", "18K"
    val goldWeight: Double,                // grams (can be same as grossWeight or different)
    val makingPercentage: Double,          // percentage of gross weight
    val labourRatePerGram: Double,         // ₹/gram
    val kundanPrice: Double,                // ₹ (sum of all Kundan prices)
    val kundanWeight: Double,               // grams (sum of all Kundan weights)
    val jarkanPrice: Double,               // ₹ (sum of all Jarkan prices)
    val jarkanWeight: Double,              // grams (sum of all Jarkan weights)
    val goldRatePerGram: Double            // ₹/gram (fetched from materials section)
)

/**
 * Data class for product price calculation result
 */
data class ProductPriceResult(
    val makingWeight: Double,
    val newWeight: Double,
    val labourCharges: Double,
    val effectiveGoldWeight: Double,
    val goldPrice: Double,
    val kundanPrice: Double,
    val jarkanPrice: Double,
    val totalProductPrice: Double
)

/**
 * Pure function to calculate product price
 * Unit-test friendly, no Compose dependencies
 */
fun calculateProductPrice(inputs: ProductPriceInputs): ProductPriceResult {
    // Handle edge cases: ensure non-negative values
    val grossWeight = inputs.grossWeight.coerceAtLeast(0.0)
    val makingPercentage = inputs.makingPercentage.coerceIn(0.0, 100.0)
    val labourRatePerGram = inputs.labourRatePerGram.coerceAtLeast(0.0)
    val goldRatePerGram = inputs.goldRatePerGram.coerceAtLeast(0.0)
    
    // Step 1: Making Weight
    val makingWeight = (makingPercentage / 100.0) * grossWeight
    
    // Step 2: New Weight
    val newWeight = grossWeight + makingWeight
    
    // Step 3: Labour Charges
    // Always calculate as: Labour Rate × New Weight
    val labourCharges = labourRatePerGram * newWeight
    
    // Step 4: Handle Kundan/Jarkan
    // Both Jarkan and Kundan weights are subtracted from new weight
    val kundanWeight = inputs.kundanWeight.coerceAtLeast(0.0)
    val kundanPrice = inputs.kundanPrice.coerceAtLeast(0.0)
    val jarkanWeight = inputs.jarkanWeight.coerceAtLeast(0.0)
    val jarkanPrice = inputs.jarkanPrice.coerceAtLeast(0.0)
    
    // Step 5: Effective Metal Weight (for pricing)
    // Effective Metal Weight = New Weight - (Jarkan Weight + Kundan Weight)
    val effectiveGoldWeight = (newWeight - jarkanWeight - kundanWeight).coerceAtLeast(0.0)
    
    // Step 6: Gold Price
    val goldPrice = effectiveGoldWeight * goldRatePerGram
    
    // Step 7: Final Product Price
    // Total = Gold Price + Kundan Price + Jarkan Price + Labour Charges
    val totalProductPrice = goldPrice + kundanPrice + jarkanPrice + labourCharges
    
    return ProductPriceResult(
        makingWeight = makingWeight,
        newWeight = newWeight,
        labourCharges = labourCharges,
        effectiveGoldWeight = effectiveGoldWeight,
        goldPrice = goldPrice,
        kundanPrice = kundanPrice,
        jarkanPrice = jarkanPrice,
        totalProductPrice = totalProductPrice
    )
}

/**
 * Format currency for display
 */

/**
 * Format weight for display
 */
private fun formatWeight(weight: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2
    return formatter.format(weight)
}

/**
 * Reusable composable for product price calculation and display
 */
@Composable
fun ProductPriceCalculatorComposable(
    inputs: ProductPriceInputs,
    onMakingPercentageChange: (Double) -> Unit,
    onLabourRateChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate price using pure function
    val result = remember(inputs) {
        calculateProductPrice(inputs)
    }
    
    val focusManager = LocalFocusManager.current
    val enterMovesFocusModifier = remember(focusManager) {
        Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                focusManager.moveFocus(FocusDirection.Down)
                true
            } else {
                false
            }
        }
    }
    
    // Local state for making percentage input - initialize as empty if 0 (like total weight)
    var makingPercentageText by remember { mutableStateOf(if (inputs.makingPercentage > 0) inputs.makingPercentage.toString() else "") }
    var labourRateText by remember { mutableStateOf(inputs.labourRatePerGram.toString()) }
    var isCustomMakingPercent by remember { mutableStateOf(true) }
    
    // Update local state when inputs change (initialize as empty if 0, like total weight)
    LaunchedEffect(inputs.makingPercentage) {
        val newValue = if (inputs.makingPercentage > 0) inputs.makingPercentage.toString() else ""
        if (makingPercentageText != newValue) {
            makingPercentageText = newValue
        }
    }
    
    LaunchedEffect(inputs.labourRatePerGram) {
        if (labourRateText.toDoubleOrNull() != inputs.labourRatePerGram) {
        labourRateText = inputs.labourRatePerGram.toString()
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFFFF8E1),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Price Calculation",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D2D2D)
            )
            
            Divider(color = MaterialTheme.colors.primary.copy(alpha = 0.3f))
            
            // Making Percentage Input with Quick Select Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick select buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5.0, 10.0, 12.0).forEach { percent ->
                        Button(
                            onClick = {
                                makingPercentageText = percent.toString()
                                onMakingPercentageChange(percent)
                                isCustomMakingPercent = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (makingPercentageText.toDoubleOrNull() == percent) {
                                    MaterialTheme.colors.primary
                                } else {
                                    Color(0xFFE0E0E0)
                                },
                                contentColor = if (makingPercentageText.toDoubleOrNull() == percent) {
                                    Color.White
                                } else {
                                    Color.Black
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("${percent.toInt()}%", fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = {
                            isCustomMakingPercent = true
                            // Clear the field when switching to custom so user can enter new value
                            makingPercentageText = ""
                            onMakingPercentageChange(0.0)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isCustomMakingPercent) {
                                MaterialTheme.colors.primary
                            } else {
                                Color(0xFFE0E0E0)
                            },
                            contentColor = if (isCustomMakingPercent) {
                                Color.White
                            } else {
                                Color.Black
                            }
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Custom", fontSize = 12.sp)
                    }
                }
                
                // Making % Text Field
                StyledTextField(
                    value = makingPercentageText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            makingPercentageText = newValue
                            val percentage = newValue.toDoubleOrNull() ?: 0.0
                            onMakingPercentageChange(percentage)
                            // If user manually types a value that matches predefined, unset custom mode
                            if (percentage in listOf(5.0, 10.0, 12.0)) {
                                isCustomMakingPercent = false
                            } else if (newValue.isNotEmpty()) {
                                // If user types something that's not predefined, set to custom mode
                                isCustomMakingPercent = true
                            }
                        }
                    },
                    label = "Making %",
                    placeholder = "0.00",
                    suffix = "%",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(enterMovesFocusModifier)
                )
            }
            
            // Labour Rate Input
            StyledTextField(
                value = labourRateText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*\$"))) {
                        labourRateText = newValue
                        val rate = newValue.toDoubleOrNull() ?: 0.0
                        onLabourRateChange(rate)
                    }
                },
                label = "Labour Rate",
                placeholder = "0.00",
                prefix = "₹",
                suffix = "/g",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(enterMovesFocusModifier)
            )
            
            Divider(color = MaterialTheme.colors.primary.copy(alpha = 0.3f))
            
            // Breakdown Display
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Gross Weight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Gross Weight",
                        fontSize = 14.sp,
                        color = Color(0xFF555555)
                    )
                    Text(
                        "${formatWeight(inputs.grossWeight)}g",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF555555)
                    )
                }
                
                // Making %
                if (inputs.makingPercentage > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Making %",
                            fontSize = 14.sp,
                            color = Color(0xFF555555)
                        )
                        Text(
                            "${formatWeight(inputs.makingPercentage)}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    }
                }
                
                // Making Weight
                if (result.makingWeight > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Making Weight",
                            fontSize = 14.sp,
                            color = Color(0xFF555555)
                        )
                        Text(
                            "${formatWeight(result.makingWeight)}g",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    }
                }
                
                // New Weight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "New Weight",
                        fontSize = 14.sp,
                        color = Color(0xFF555555)
                    )
                    Text(
                        "${formatWeight(result.newWeight)}g",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF555555)
                    )
                }
                
                // Metal Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Metal Rate (${inputs.goldPurity})",
                        fontSize = 14.sp,
                        color = Color(0xFF555555)
                    )
                    Text(
                        "${CurrencyFormatter.formatRupees(inputs.goldRatePerGram)}/g",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF555555)
                    )
                }
                
                // Effective Metal Weight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Effective Metal Weight",
                        fontSize = 14.sp,
                        color = Color(0xFF555555)
                    )
                    Text(
                        "${formatWeight(result.effectiveGoldWeight)}g",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF555555)
                    )
                }
                
                // Metal Price
                if (result.goldPrice > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Metal Price",
                            fontSize = 14.sp,
                            color = Color(0xFF555555)
                        )
                        Text(
                            "${CurrencyFormatter.formatRupees(result.goldPrice)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    }
                }
                
                // Kundan Price
                if (result.kundanPrice > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Kundan Price",
                            fontSize = 14.sp,
                            color = Color(0xFF555555)
                        )
                        Text(
                            "${CurrencyFormatter.formatRupees(result.kundanPrice)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    }
                }
                
                // Jarkan Price
                if (result.jarkanPrice > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Jarkan Price",
                            fontSize = 14.sp,
                            color = Color(0xFF555555)
                        )
                        Text(
                            "${CurrencyFormatter.formatRupees(result.jarkanPrice)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    }
                }
                
                // Labour Charges (always calculated from rate × new weight)
                if (result.labourCharges > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Labour Charges (${formatWeight(result.newWeight)}g × ${CurrencyFormatter.formatRupees(inputs.labourRatePerGram)})",
                            fontSize = 14.sp,
                            color = Color(0xFF555555)
                        )
                        Text(
                            "${CurrencyFormatter.formatRupees(result.labourCharges)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    }
                }
            }
            
            // Divider before total
            if (result.goldPrice > 0 || result.kundanPrice > 0 || result.jarkanPrice > 0 || result.labourCharges > 0) {
                Divider(color = MaterialTheme.colors.primary.copy(alpha = 0.3f))
            }
            
            // Final Total Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total Product Price",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
                Text(
                    "${CurrencyFormatter.formatRupees(result.totalProductPrice)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}

