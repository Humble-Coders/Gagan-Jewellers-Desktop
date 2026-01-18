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
    val diamondPrice: Double,              // ₹ (sum of all Diamond prices: cent * rate)
    val diamondWeight: Double,             // carats (sum of all Diamond cent values in carats, for display)
    val diamondWeightInGrams: Double,      // grams (sum of all Diamond weights in grams, for calculation)
    val solitairePrice: Double,            // ₹ (sum of all Solitaire prices: cent * rate)
    val solitaireWeight: Double,           // carats (sum of all Solitaire cent values in carats, for display)
    val solitaireWeightInGrams: Double,    // grams (sum of all Solitaire weights in grams, for calculation)
    val colorStonesPrice: Double,          // ₹ (sum of all Color Stones prices: weight * rate)
    val colorStonesWeight: Double,         // grams or carats (sum of all Color Stones weights)
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
    val diamondPrice: Double,
    val solitairePrice: Double,
    val colorStonesPrice: Double,
    val totalStonesPrice: Double,         // Sum of all stone prices
    val totalStonesWeight: Double,        // Sum of all stone weights (in grams)
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
    
    // Step 4: Handle all stone types
    // Kundan and Jarkan weights are subtracted from new weight (in grams)
    val kundanWeight = inputs.kundanWeight.coerceAtLeast(0.0)
    val kundanPrice = inputs.kundanPrice.coerceAtLeast(0.0)
    val jarkanWeight = inputs.jarkanWeight.coerceAtLeast(0.0)
    val jarkanPrice = inputs.jarkanPrice.coerceAtLeast(0.0)
    
    // Diamond and Solitaire: prices already calculated (cent * rate)
    // diamondWeight/solitaireWeight are in carats (for display)
    // diamondWeightInGrams/solitaireWeightInGrams are in grams (for calculation)
    val diamondWeight = inputs.diamondWeight.coerceAtLeast(0.0) // carats (for display)
    val diamondPrice = inputs.diamondPrice.coerceAtLeast(0.0)
    val diamondWeightInGrams = inputs.diamondWeightInGrams.coerceAtLeast(0.0) // grams (for calculation)
    val solitaireWeight = inputs.solitaireWeight.coerceAtLeast(0.0) // carats (for display)
    val solitairePrice = inputs.solitairePrice.coerceAtLeast(0.0)
    val solitaireWeightInGrams = inputs.solitaireWeightInGrams.coerceAtLeast(0.0) // grams (for calculation)
    
    // Color Stones: weights in grams, prices already calculated (weight * rate)
    val colorStonesWeight = inputs.colorStonesWeight.coerceAtLeast(0.0)
    val colorStonesPrice = inputs.colorStonesPrice.coerceAtLeast(0.0)
    
    // Step 5: Effective Metal Weight (for pricing)
    // Effective Metal Weight = New Weight - (All Stone Weights in grams)
    // Stone weights: Kundan, Jarkan, Diamond, Solitaire, Color Stones (all in grams)
    val totalStoneWeightInGrams = kundanWeight + jarkanWeight + diamondWeightInGrams + solitaireWeightInGrams + colorStonesWeight
    val effectiveGoldWeight = (newWeight - totalStoneWeightInGrams).coerceAtLeast(0.0)
    
    // Step 6: Gold Price
    val goldPrice = effectiveGoldWeight * goldRatePerGram
    
    // Step 7: Total Stones Price (sum of all stone prices)
    val totalStonesPrice = kundanPrice + jarkanPrice + diamondPrice + solitairePrice + colorStonesPrice
    
    // Step 8: Total Stones Weight (in grams for display)
    val totalStonesWeight = totalStoneWeightInGrams
    
    // Step 9: Final Product Price
    // Total = Gold Price + All Stone Prices + Labour Charges
    val totalProductPrice = goldPrice + totalStonesPrice + labourCharges
    
    return ProductPriceResult(
        makingWeight = makingWeight,
        newWeight = newWeight,
        labourCharges = labourCharges,
        effectiveGoldWeight = effectiveGoldWeight,
        goldPrice = goldPrice,
        kundanPrice = kundanPrice,
        jarkanPrice = jarkanPrice,
        diamondPrice = diamondPrice,
        solitairePrice = solitairePrice,
        colorStonesPrice = colorStonesPrice,
        totalStonesPrice = totalStonesPrice,
        totalStonesWeight = totalStonesWeight,
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
 * Helper function to calculate stone prices from ProductStone list
 * Extracts and sums prices and weights for each stone type
 */
fun calculateStonePrices(stones: List<org.example.project.data.ProductStone>): StonePriceBreakdown {
    // Filter stones by type
    val kundanStones = stones.filter { it.name.equals("Kundan", ignoreCase = true) }
    val jarkanStones = stones.filter { it.name.equals("Jarkan", ignoreCase = true) }
    val diamondStones = stones.filter { it.name.equals("Diamond", ignoreCase = true) }
    val solitaireStones = stones.filter { it.name.equals("Solitaire", ignoreCase = true) }
    val colorStones = stones.filter { stone ->
        !stone.name.equals("Kundan", ignoreCase = true) &&
        !stone.name.equals("Jarkan", ignoreCase = true) &&
        !stone.name.equals("Diamond", ignoreCase = true) &&
        !stone.name.equals("Solitaire", ignoreCase = true)
    }
    
    // Calculate Kundan: amount is direct, weight in grams
    val kundanPrice = kundanStones.sumOf { it.amount }
    val kundanWeight = kundanStones.sumOf { it.weight } // in grams
    
    // Calculate Jarkan: amount is direct (can be 0), weight in grams
    val jarkanPrice = jarkanStones.sumOf { it.amount }
    val jarkanWeight = jarkanStones.sumOf { it.weight } // in grams
    
    // Calculate Diamond: amount = cent * rate (where cent is in carats, stored in quantity)
    // For Diamond, weight (grams) is in weight field, cent (carats) is in quantity field
    val diamondPrice = diamondStones.sumOf { it.amount } // Already calculated: cent (carats) * rate
    val diamondWeightInCarats = diamondStones.sumOf { it.quantity } // cent (carats) from quantity field
    val diamondWeightInGrams = diamondStones.sumOf { it.weight } // weight in grams (for total weight calculation)
    
    // Calculate Solitaire: amount = cent * rate (where cent is in carats, stored in quantity)
    // For Solitaire, weight (grams) is in weight field, cent (carats) is in quantity field
    val solitairePrice = solitaireStones.sumOf { it.amount } // Already calculated: cent (carats) * rate
    val solitaireWeightInCarats = solitaireStones.sumOf { it.quantity } // cent (carats) from quantity field
    val solitaireWeightInGrams = solitaireStones.sumOf { it.weight } // weight in grams (for total weight calculation)
    
    // Calculate Color Stones: amount = weight * rate (weight in grams/carats, rate per gram/carat)
    val colorStonesPrice = colorStones.sumOf { it.amount } // Already calculated: weight * rate
    val colorStonesWeight = colorStones.sumOf { it.weight } // in grams or carats
    
    return StonePriceBreakdown(
        kundanPrice = kundanPrice,
        kundanWeight = kundanWeight,
        jarkanPrice = jarkanPrice,
        jarkanWeight = jarkanWeight,
        diamondPrice = diamondPrice,
        diamondWeight = diamondWeightInCarats, // Return carats for display
        solitairePrice = solitairePrice,
        solitaireWeight = solitaireWeightInCarats, // Return carats for display
        colorStonesPrice = colorStonesPrice,
        colorStonesWeight = colorStonesWeight,
        diamondWeightInGrams = diamondWeightInGrams, // For total weight calculation
        solitaireWeightInGrams = solitaireWeightInGrams // For total weight calculation
    )
}

/**
 * Data class for stone price breakdown
 */
data class StonePriceBreakdown(
    val kundanPrice: Double = 0.0,
    val kundanWeight: Double = 0.0,
    val jarkanPrice: Double = 0.0,
    val jarkanWeight: Double = 0.0,
    val diamondPrice: Double = 0.0,
    val diamondWeight: Double = 0.0, // carats (for display)
    val solitairePrice: Double = 0.0,
    val solitaireWeight: Double = 0.0, // carats (for display)
    val colorStonesPrice: Double = 0.0,
    val colorStonesWeight: Double = 0.0,
    val diamondWeightInGrams: Double = 0.0, // grams (for calculation)
    val solitaireWeightInGrams: Double = 0.0 // grams (for calculation)
)

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
                
                // Diamond Price
                if (result.diamondPrice > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Diamond Price (${formatWeight(inputs.diamondWeight)} carats)",
                            fontSize = 14.sp,
                            color = Color(0xFF555555)
                        )
                        Text(
                            "${CurrencyFormatter.formatRupees(result.diamondPrice)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    }
                }
                
                // Solitaire Price
                if (result.solitairePrice > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Solitaire Price (${formatWeight(inputs.solitaireWeight)} carats)",
                            fontSize = 14.sp,
                            color = Color(0xFF555555)
                        )
                        Text(
                            "${CurrencyFormatter.formatRupees(result.solitairePrice)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    }
                }
                
                // Color Stones Price
                if (result.colorStonesPrice > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Color Stones Price (${formatWeight(inputs.colorStonesWeight)}g)",
                            fontSize = 14.sp,
                            color = Color(0xFF555555)
                        )
                        Text(
                            "${CurrencyFormatter.formatRupees(result.colorStonesPrice)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    }
                }
                
                // Total Stones Weight (show if any stones exist)
                if (result.totalStonesWeight > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total Stones Weight",
                            fontSize = 14.sp,
                            color = Color(0xFF555555),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${formatWeight(result.totalStonesWeight)}g",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555555)
                        )
                    }
                }
                
                // Total Stones Price (show if any stones exist)
                if (result.totalStonesPrice > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total Stones Price",
                            fontSize = 14.sp,
                            color = Color(0xFF555555),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${CurrencyFormatter.formatRupees(result.totalStonesPrice)}",
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
            if (result.goldPrice > 0 || result.totalStonesPrice > 0 || result.labourCharges > 0) {
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

