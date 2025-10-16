package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.data.ExchangeGold
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExchangeGoldScreen(
    onExchangeGoldComplete: (ExchangeGold) -> Unit,
    onBack: () -> Unit
) {
    var exchangeGold by remember { mutableStateOf(ExchangeGold()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Editing state management
    var editingField by remember { mutableStateOf<String?>(null) }
    var fieldValues by remember { mutableStateOf<Map<String, TextFieldValue>>(emptyMap()) }

    // Auto-calculate derived values with proper rounding
    val calculatedNetWeight = remember(exchangeGold.grossWeight, exchangeGold.lessWeight) {
        (exchangeGold.grossWeight - exchangeGold.lessWeight).coerceAtLeast(0.0)
    }

    val calculatedFineWeight = remember(calculatedNetWeight, exchangeGold.tunch) {
        if (exchangeGold.tunch > 0) {
            (calculatedNetWeight * exchangeGold.tunch / 100.0)
        } else 0.0
    }

    val calculatedValue = remember(calculatedFineWeight, exchangeGold.rate) {
        (calculatedFineWeight * exchangeGold.rate)
    }

    // Helper functions for field management
    fun onEditingFieldChange(fieldKey: String?) {
        editingField = fieldKey
    }

    fun onFieldValueChange(fieldKey: String, value: TextFieldValue) {
        fieldValues = fieldValues + (fieldKey to value)
    }

    fun onSaveAndCloseEditing() {
        editingField = null
    }

    fun onSaveAndMoveToNextField() {
        onSaveAndCloseEditing()
    }

    // Save field value function with validation
    fun saveFieldValue(fieldKey: String, value: String) {
        when (fieldKey) {
            "type" -> exchangeGold = exchangeGold.copy(type = value)
            "firm" -> exchangeGold = exchangeGold.copy(firm = value)
            "account" -> exchangeGold = exchangeGold.copy(account = value)
            "description" -> exchangeGold = exchangeGold.copy(description = value)
            "name" -> exchangeGold = exchangeGold.copy(name = value)
            "grossWeight" -> {
                val weight = value.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                exchangeGold = exchangeGold.copy(grossWeight = weight)
            }
            "lessWeight" -> {
                val weight = value.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                exchangeGold = exchangeGold.copy(lessWeight = weight)
            }
            "tunch" -> {
                val tunch = value.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                exchangeGold = exchangeGold.copy(tunch = tunch)
            }
            "laborWeight" -> {
                val weight = value.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                exchangeGold = exchangeGold.copy(laborWeight = weight)
            }
            "ffnWeight" -> {
                val weight = value.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                exchangeGold = exchangeGold.copy(ffnWeight = weight)
            }
            "rate" -> {
                val rate = value.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                exchangeGold = exchangeGold.copy(rate = rate)
            }
            "averageRate" -> {
                val avgRate = value.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                exchangeGold = exchangeGold.copy(averageRate = avgRate)
            }
        }
    }

    Dialog(onDismissRequest = onBack) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            shape = RoundedCornerShape(20.dp),
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFBF5),
                                Color(0xFFFFF8E1)
                            )
                        )
                    )
            ) {
                // Header with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFB8973D),
                                    Color(0xFFD4AF37)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Exchange Gold Details",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // Metal Details Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        elevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFB8973D),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Metal Received by Customer",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E2E2E)
                                )
                            }

                            Divider(
                                color = Color(0xFFE0E0E0),
                                thickness = 1.dp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Exchange Gold Form
                            ExchangeGoldForm(
                                exchangeGold = exchangeGold,
                                calculatedNetWeight = calculatedNetWeight,
                                calculatedFineWeight = calculatedFineWeight,
                                calculatedValue = calculatedValue,
                                editingField = editingField,
                                fieldValues = fieldValues,
                                onEditingFieldChange = ::onEditingFieldChange,
                                onFieldValueChange = ::onFieldValueChange,
                                onSaveAndMoveToNextField = ::onSaveAndMoveToNextField,
                                onSaveAndCloseEditing = ::onSaveAndCloseEditing,
                                saveFieldValue = ::saveFieldValue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(12.dp),
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SummaryItem(
                                label = "Net Weight",
                                value = String.format("%.3f g", calculatedNetWeight),
                                icon = Icons.Default.AccountBox
                            )
                            SummaryItem(
                                label = "Fine Weight",
                                value = String.format("%.3f g", calculatedFineWeight),
                                icon = Icons.Default.AccountBox
                            )
                            SummaryItem(
                                label = "Total Value",
                                value = formatCurrency(calculatedValue),
                                icon = Icons.Default.AccountBox,
                                highlight = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error Message
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp),
                            elevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    error,
                                    color = Color(0xFFD32F2F),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF666666)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE0E0E0))
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Button(
                            onClick = {
                                if (isValidExchangeGold(exchangeGold)) {
                                    onExchangeGoldComplete(exchangeGold.copy(
                                        netWeight = calculatedNetWeight,
                                        fineWeight = calculatedFineWeight,
                                        value = calculatedValue
                                    ))
                                    errorMessage = null
                                } else {
                                    errorMessage = "Please fill: Gross Weight, Rate, and Tunch"
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFB8973D)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.elevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Add Exchange",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (highlight) Color(0xFFB8973D) else Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = Color(0xFF666666),
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            fontSize = if (highlight) 16.sp else 14.sp,
            color = if (highlight) Color(0xFFB8973D) else Color(0xFF333333),
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun ExchangeGoldForm(
    exchangeGold: ExchangeGold,
    calculatedNetWeight: Double,
    calculatedFineWeight: Double,
    calculatedValue: Double,
    editingField: String?,
    fieldValues: Map<String, TextFieldValue>,
    onEditingFieldChange: (String?) -> Unit,
    onFieldValueChange: (String, TextFieldValue) -> Unit,
    onSaveAndMoveToNextField: () -> Unit,
    onSaveAndCloseEditing: () -> Unit,
    saveFieldValue: (String, String) -> Unit
) {
    Column {
        // Section: Basic Information
        Text(
            "Basic Information",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditableExchangeField(
                fieldKey = "type",
                label = "TYPE",
                currentValue = exchangeGold.type,
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Text
            )
            EditableExchangeField(
                fieldKey = "firm",
                label = "FIRM",
                currentValue = exchangeGold.firm,
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Text
            )
            EditableExchangeField(
                fieldKey = "account",
                label = "ACCOUNT",
                currentValue = exchangeGold.account,
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Text
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditableExchangeField(
                fieldKey = "description",
                label = "DESCRIPTION",
                currentValue = exchangeGold.description,
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Text
            )
            EditableExchangeField(
                fieldKey = "name",
                label = "NAME",
                currentValue = exchangeGold.name,
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Text
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Weight Details
        Text(
            "Weight Details",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditableExchangeField(
                fieldKey = "grossWeight",
                label = "GROSS WT (g)",
                currentValue = if (exchangeGold.grossWeight == 0.0) "" else String.format("%.3f", exchangeGold.grossWeight),
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Decimal,
                isRequired = true
            )
            EditableExchangeField(
                fieldKey = "lessWeight",
                label = "LESS WT (g)",
                currentValue = if (exchangeGold.lessWeight == 0.0) "" else String.format("%.3f", exchangeGold.lessWeight),
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Decimal
            )
            EditableExchangeField(
                fieldKey = "netWeight",
                label = "NET WT (g)",
                currentValue = String.format("%.3f", calculatedNetWeight),
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Decimal,
                enabled = false,
                isCalculated = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditableExchangeField(
                fieldKey = "tunch",
                label = "TUNCH (%)",
                currentValue = if (exchangeGold.tunch == 0.0) "" else String.format("%.2f", exchangeGold.tunch),
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Decimal,
                isRequired = true
            )
            EditableExchangeField(
                fieldKey = "fineWeight",
                label = "FINE WT (g)",
                currentValue = String.format("%.3f", calculatedFineWeight),
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Decimal,
                enabled = false,
                isCalculated = true
            )
            EditableExchangeField(
                fieldKey = "laborWeight",
                label = "LABOR WT (g)",
                currentValue = if (exchangeGold.laborWeight == 0.0) "" else String.format("%.3f", exchangeGold.laborWeight),
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Decimal
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Financial Details
        Text(
            "Financial Details",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditableExchangeField(
                fieldKey = "ffnWeight",
                label = "FFN WT (g)",
                currentValue = if (exchangeGold.ffnWeight == 0.0) "" else String.format("%.3f", exchangeGold.ffnWeight),
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Decimal
            )
            EditableExchangeField(
                fieldKey = "rate",
                label = "RATE (₹/g)",
                currentValue = if (exchangeGold.rate == 0.0) "" else String.format("%.2f", exchangeGold.rate),
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Decimal,
                isRequired = true
            )
            EditableExchangeField(
                fieldKey = "value",
                label = "VALUE (₹)",
                currentValue = String.format("%.2f", calculatedValue),
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Decimal,
                enabled = false,
                isCalculated = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditableExchangeField(
                fieldKey = "averageRate",
                label = "AVG RATE (₹/g)",
                currentValue = if (exchangeGold.averageRate == 0.0) "" else String.format("%.2f", exchangeGold.averageRate),
                modifier = Modifier.weight(1f),
                editingField = editingField,
                fieldValues = fieldValues,
                onEditingFieldChange = onEditingFieldChange,
                onFieldValueChange = onFieldValueChange,
                onSaveAndMoveToNextField = onSaveAndMoveToNextField,
                onSaveAndCloseEditing = onSaveAndCloseEditing,
                saveFieldValue = saveFieldValue,
                keyboardType = KeyboardType.Decimal
            )
            // Empty spacers for alignment
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EditableExchangeField(
    fieldKey: String,
    label: String,
    currentValue: String,
    modifier: Modifier,
    editingField: String?,
    fieldValues: Map<String, TextFieldValue>,
    onEditingFieldChange: (String?) -> Unit,
    onFieldValueChange: (String, TextFieldValue) -> Unit,
    onSaveAndMoveToNextField: () -> Unit,
    onSaveAndCloseEditing: () -> Unit,
    saveFieldValue: (String, String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    enabled: Boolean = true,
    isCalculated: Boolean = false,
    isRequired: Boolean = false
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRequired) Color(0xFFB8973D) else Color(0xFF666666)
            )
            if (isRequired) {
                Text(
                    text = " *",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            }
            if (isCalculated) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Create,
                    contentDescription = "Auto-calculated",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (editingField == fieldKey && enabled) {
            val focusRequester = remember { FocusRequester() }
            val textFieldValue = fieldValues[fieldKey] ?: TextFieldValue(
                text = currentValue,
                selection = TextRange(0, currentValue.length)
            )

            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { value ->
                    onFieldValueChange(fieldKey, value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val finalValue = textFieldValue.text
                        saveFieldValue(fieldKey, finalValue)
                        onSaveAndMoveToNextField()
                    }
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFB8973D),
                    unfocusedBorderColor = Color(0xFFB8973D),
                    backgroundColor = Color(0xFFFFFBF5),
                    cursorColor = Color(0xFFB8973D)
                ),
                singleLine = true
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(enabled = enabled) {
                        if (enabled) onEditingFieldChange(fieldKey)
                    }
                    .background(
                        color = when {
                            isCalculated -> Color(0xFFE8F5E9)
                            !enabled -> Color(0xFFF5F5F5)
                            else -> Color.White
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = when {
                            isCalculated -> Color(0xFF4CAF50)
                            isRequired && currentValue.isEmpty() -> Color(0xFFFFB74D)
                            else -> Color(0xFFE0E0E0)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentValue.isEmpty()) "---" else currentValue,
                    fontSize = 14.sp,
                    color = when {
                        currentValue.isEmpty() -> Color(0xFFBDBDBD)
                        !enabled -> Color(0xFF666666)
                        isCalculated -> Color(0xFF2E7D32)
                        else -> Color(0xFF333333)
                    },
                    textAlign = TextAlign.Center,
                    fontWeight = if (isCalculated) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private fun isValidExchangeGold(exchangeGold: ExchangeGold): Boolean {
    return exchangeGold.grossWeight > 0.0 &&
            exchangeGold.rate > 0.0 &&
            exchangeGold.tunch > 0.0
}

private fun formatCurrency(amount: Double): String {
    return if (amount == 0.0) {
        "₹0.00"
    } else {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        formatter.format(amount)
    }
}