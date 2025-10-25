package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.data.CashAmount
import org.example.project.data.CashTransactionType
import org.example.project.data.User

@Composable
fun CashAmountDialog(
    customer: User,
    onDismiss: () -> Unit,
    onSave: (CashAmount) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf(CashTransactionType.GIVE) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💰 Cash Transaction",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }

                // Customer Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFF8F9FA),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF8E1),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = customer.name.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB8973D)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = customer.name.ifBlank { "Unnamed Customer" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                text = "Current Balance: ₹${String.format("%.2f", customer.balance)}",
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }

                // Transaction Type Selection
                Text(
                    text = "Transaction Type",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Give Option
                    Card(
                        modifier = Modifier.weight(1f),
                        backgroundColor = if (transactionType == CashTransactionType.GIVE) Color(0xFFDCFCE7) else Color(0xFFF8F9FA),
                        shape = RoundedCornerShape(12.dp),
                        elevation = if (transactionType == CashTransactionType.GIVE) 4.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { transactionType = CashTransactionType.GIVE }
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💸 Give",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (transactionType == CashTransactionType.GIVE) Color(0xFF16A34A) else Color(0xFF6B7280)
                            )
                            Text(
                                text = "Give cash to customer\n(Increases balance)",
                                fontSize = 12.sp,
                                color = if (transactionType == CashTransactionType.GIVE) Color(0xFF16A34A) else Color(0xFF9CA3AF),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Receive Option
                    Card(
                        modifier = Modifier.weight(1f),
                        backgroundColor = if (transactionType == CashTransactionType.RECEIVE) Color(0xFFFEE2E2) else Color(0xFFF8F9FA),
                        shape = RoundedCornerShape(12.dp),
                        elevation = if (transactionType == CashTransactionType.RECEIVE) 4.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { transactionType = CashTransactionType.RECEIVE }
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💰 Receive",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (transactionType == CashTransactionType.RECEIVE) Color(0xFFDC2626) else Color(0xFF6B7280)
                            )
                            Text(
                                text = "Receive cash from customer\n(Decreases balance)",
                                fontSize = 12.sp,
                                color = if (transactionType == CashTransactionType.RECEIVE) Color(0xFFDC2626) else Color(0xFF9CA3AF),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFB8973D),
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        cursorColor = Color(0xFFB8973D)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFB8973D),
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        cursorColor = Color(0xFFB8973D)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Error Message
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF6B7280)
                        )
                    ) {
                        Text("Cancel", fontSize = 16.sp)
                    }

                    Button(
                        onClick = {
                            val amountValue = amount.toDoubleOrNull()
                            if (amountValue == null || amountValue <= 0) {
                                errorMessage = "Please enter a valid amount"
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null

                            val cashAmount = CashAmount(
                                customerId = customer.customerId,
                                amount = amountValue,
                                transactionType = transactionType,
                                notes = notes.trim()
                            )

                            onSave(cashAmount)
                        },
                        enabled = !isLoading && amount.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFB8973D),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Transaction", fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
