package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.data.PaymentSplit
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PaymentSplitScreen(
    totalAmount: Double,
    initialPaymentSplit: PaymentSplit? = null,
    onPaymentSplitComplete: (PaymentSplit) -> Unit,
    onBack: () -> Unit
) {
    var cashAmount by remember { mutableStateOf(initialPaymentSplit?.cashAmount?.toString() ?: "") }
    var cardAmount by remember { mutableStateOf(initialPaymentSplit?.cardAmount?.toString() ?: "") }
    var bankAmount by remember { mutableStateOf(initialPaymentSplit?.bankAmount?.toString() ?: "") }
    var onlineAmount by remember { mutableStateOf(initialPaymentSplit?.onlineAmount?.toString() ?: "") }
    var dueAmount by remember { mutableStateOf(initialPaymentSplit?.dueAmount?.toString() ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Calculate due amount automatically
    val calculatedDueAmount = remember(cashAmount, cardAmount, bankAmount, onlineAmount, totalAmount) {
        val paidAmount = (cashAmount.toDoubleOrNull() ?: 0.0) + 
                        (cardAmount.toDoubleOrNull() ?: 0.0) + 
                        (bankAmount.toDoubleOrNull() ?: 0.0) + 
                        (onlineAmount.toDoubleOrNull() ?: 0.0)
        val due = totalAmount - paidAmount
        if (due > 0) due else 0.0
    }

    // Update due amount when other amounts change
    LaunchedEffect(calculatedDueAmount) {
        dueAmount = calculatedDueAmount.toString()
    }

    val paymentSplit = remember(cashAmount, cardAmount, bankAmount, onlineAmount, calculatedDueAmount) {
        PaymentSplit(
            cashAmount = cashAmount.toDoubleOrNull() ?: 0.0,
            cardAmount = cardAmount.toDoubleOrNull() ?: 0.0,
            bankAmount = bankAmount.toDoubleOrNull() ?: 0.0,
            onlineAmount = onlineAmount.toDoubleOrNull() ?: 0.0,
            dueAmount = calculatedDueAmount,
            totalAmount = totalAmount
        )
    }

    val isValid = paymentSplit.isValid()

    Dialog(onDismissRequest = onBack) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            elevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFFFAFAFA)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
            ) {
                // Header
                PaymentSplitHeader(onBack = onBack)

                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total Amount Display
                    TotalAmountCard(totalAmount = totalAmount)

                    // Payment Split Inputs
                    PaymentSplitInputs(
                        cashAmount = cashAmount,
                        cardAmount = cardAmount,
                        bankAmount = bankAmount,
                        onlineAmount = onlineAmount,
                        dueAmount = dueAmount,
                        onCashAmountChange = { cashAmount = it },
                        onCardAmountChange = { cardAmount = it },
                        onBankAmountChange = { bankAmount = it },
                        onOnlineAmountChange = { onlineAmount = it },
                        onDueAmountChange = { dueAmount = it }
                    )

                    // Summary and Validation
                    PaymentSummaryCard(
                        paymentSplit = paymentSplit,
                        isValid = isValid
                    )

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF666666)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                if (isValid) {
                                    onPaymentSplitComplete(paymentSplit)
                                } else {
                                    errorMessage = "Payment amounts must equal the total amount"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFB8973D),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm Payment", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Error Message
                    errorMessage?.let { error ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                error,
                                color = Color(0xFFD32F2F),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentSplitHeader(onBack: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(0.dp),
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF2E2E2E),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                "Payment Split",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E2E2E)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "SPLIT PAYMENT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB8973D),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun TotalAmountCard(totalAmount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Amount",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "₹${formatCurrency(totalAmount)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB8973D)
            )
        }
    }
}

@Composable
private fun PaymentSplitInputs(
    cashAmount: String,
    cardAmount: String,
    bankAmount: String,
    onlineAmount: String,
    dueAmount: String,
    onCashAmountChange: (String) -> Unit,
    onCardAmountChange: (String) -> Unit,
    onBankAmountChange: (String) -> Unit,
    onOnlineAmountChange: (String) -> Unit,
    onDueAmountChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Split Payment Amounts",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            // Cash Amount
            PaymentInputField(
                label = "Cash Amount",
                value = cashAmount,
                onValueChange = onCashAmountChange,
                icon = Icons.Default.AccountBox,
                iconColor = Color(0xFF4CAF50)
            )

            // Card Amount
            PaymentInputField(
                label = "Card Amount",
                value = cardAmount,
                onValueChange = onCardAmountChange,
                icon = Icons.Default.AccountBox,
                iconColor = Color(0xFF2196F3)
            )

            // Bank Transfer Amount
            PaymentInputField(
                label = "Bank Transfer Amount",
                value = bankAmount,
                onValueChange = onBankAmountChange,
                icon = Icons.Default.AccountBox,
                iconColor = Color(0xFF9C27B0)
            )

            // Online Amount
            PaymentInputField(
                label = "Online Amount",
                value = onlineAmount,
                onValueChange = onOnlineAmountChange,
                icon = Icons.Default.AccountBox,
                iconColor = Color(0xFF00BCD4)
            )

            // Due Amount (Auto-calculated)
            PaymentInputField(
                label = "Due Amount (Auto-calculated)",
                value = dueAmount,
                onValueChange = onDueAmountChange,
                icon = Icons.Default.AccountBox,
                iconColor = Color(0xFFFF9800),
                enabled = false
            )
        }
    }
}

@Composable
private fun PaymentInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = if (enabled) Color(0xFFB8973D) else Color(0xFFE0E0E0),
            cursorColor = if (enabled) Color(0xFFB8973D) else Color(0xFFE0E0E0),
            disabledTextColor = Color(0xFF666666),
            disabledBorderColor = Color(0xFFE0E0E0)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun PaymentSummaryCard(
    paymentSplit: PaymentSplit,
    isValid: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = if (isValid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Payment Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            PaymentSummaryRow("Cash", paymentSplit.cashAmount, Color(0xFF4CAF50))
            PaymentSummaryRow("Card", paymentSplit.cardAmount, Color(0xFF2196F3))
            PaymentSummaryRow("Bank Transfer", paymentSplit.bankAmount, Color(0xFF9C27B0))
            PaymentSummaryRow("Online", paymentSplit.onlineAmount, Color(0xFF00BCD4))
            PaymentSummaryRow("Due", paymentSplit.dueAmount, Color(0xFFFF9800))

            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E2E2E)
                )
                Text(
                    "₹${formatCurrency(paymentSplit.cashAmount + paymentSplit.cardAmount + paymentSplit.bankAmount + paymentSplit.onlineAmount + paymentSplit.dueAmount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E2E2E)
                )
            }

            if (!isValid) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Amounts don't match total",
                        fontSize = 14.sp,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Valid",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Payment amounts match total",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentSummaryRow(
    label: String,
    amount: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666)
        )
        Text(
            "₹${formatCurrency(amount)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}


private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}