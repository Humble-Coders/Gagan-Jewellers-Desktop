package org.example.project.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun AddKaratRateDialog(
    materialId: String,
    materialName: String,
    karatInput: String,
    rateInput: String,
    onKaratInputChange: (String) -> Unit,
    onRateInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    loading: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight(0.6f),
            elevation = 8.dp,
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            "Add New Karat Rate",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onSurface
                        )
                        Text(
                            "Add karat and rate for $materialName",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                // Material Name (read-only)
                OutlinedTextField(
                    value = materialName,
                    onValueChange = { },
                    label = { Text("Material Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        disabledTextColor = MaterialTheme.colors.onSurface,
                        disabledBorderColor = Color(0xFFE0E0E0)
                    )
                )

                // Karat Input
                OutlinedTextField(
                    value = karatInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""\d*K?""", RegexOption.IGNORE_CASE))) {
                            onKaratInputChange(input)
                        }
                    },
                    label = { Text("Karat (e.g., 24K, 22K)") },
                    placeholder = { Text("e.g., 24K") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.primary,
                        cursorColor = MaterialTheme.colors.primary,
                        focusedLabelColor = MaterialTheme.colors.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Rate Input
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            onRateInputChange(input)
                        }
                    },
                    label = { Text("Rate per gram (₹)") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colors.primary,
                        cursorColor = MaterialTheme.colors.primary,
                        focusedLabelColor = MaterialTheme.colors.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = !loading && karatInput.isNotEmpty() && rateInput.isNotEmpty() && rateInput.toDoubleOrNull() ?: 0.0 > 0,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adding...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Add Rate",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

