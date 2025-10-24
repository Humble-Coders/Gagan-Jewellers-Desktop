package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.data.AvailabilityRepository
import org.example.project.data.AvailabilitySlot
import org.example.project.data.AvailabilitySlotRequest
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddSlotsScreen(
    onBack: () -> Unit,
    availabilityRepository: AvailabilityRepository
) {
    // State variables remain unchanged
    var showAddSlotsForm by remember { mutableStateOf(false) }
    var allSlots by remember { mutableStateOf<List<AvailabilitySlot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Real-time listener unchanged
    LaunchedEffect(Unit) {
        try {
            availabilityRepository.getAvailabilitySlotsStream().collect { slots ->
                allSlots = slots
                println("📅 Updated slots: ${slots.size} slots received")
            }
        } catch (e: Exception) {
            println("❌ Error collecting availability slots: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(20.dp) // Reduced padding for more content space
    ) {
        // Refined header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Availability Management",
                style = MaterialTheme.typography.h5, // Reduced from h4 for better space utilization
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) // Reduced spacing

        // Main content with two panels
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp) // Reduced spacing
        ) {
            // Left Panel - Add Slots Form
            Column(
                modifier = Modifier.width(380.dp) // Adjusted width
            ) {
                AddSlotsForm(
                    availabilityRepository = availabilityRepository,
                    onSlotsCreated = { createdCount ->
                        showSuccess = true
                        keyboardController?.hide()
                    },
                    onError = { error ->
                        errorMessage = error
                    },
                    onCancel = { }
                )

                // Success and error messages remain unchanged
                if (showSuccess) {
                    LaunchedEffect(showSuccess) {
                        kotlinx.coroutines.delay(3000)
                        showSuccess = false
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        elevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ Availability slots created successfully!",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFF44336).copy(alpha = 0.1f),
                        elevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "❌ $error",
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Right Panel - Optimized slots display
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Header with stats
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 4.dp,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Available Slots",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )
                            Text(
                                text = "Real-time updates",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Total slots counter
                        Card(
                            backgroundColor = MaterialTheme.colors.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${allSlots.size} total slots",
                                style = MaterialTheme.typography.caption,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (allSlots.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        backgroundColor = MaterialTheme.colors.surface,
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📅",
                                    fontSize = 48.sp // Instead of typography.h2
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No availability slots found",
                                    style = MaterialTheme.typography.h6,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Use the form on the left to create your first availability slot",
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Group slots by date
                    val slotsByDate = allSlots.groupBy { slot ->
                        val date = Date(slot.startTime)
                        SimpleDateFormat("yyyy-MM-dd").format(date)
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(slotsByDate.toList().sortedBy { it.first }) { (dateStr, slots) ->
                            DateSlotCard(
                                dateStr = dateStr,
                                slots = slots.sortedBy { it.startTime }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateSlotCard(
    dateStr: String,
    slots: List<AvailabilitySlot>
) {
    val date = SimpleDateFormat("yyyy-MM-dd").parse(dateStr)
    val formattedDate = SimpleDateFormat("MMM dd, yyyy").format(date!!)
    val dayOfWeek = SimpleDateFormat("EEEE").format(date)

    Card(
        modifier = Modifier
            .width(300.dp) // Slightly reduced width
            .height(400.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 6.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp) // Reduced padding
        ) {
            // Date header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                elevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.subtitle1, // Reduced from h6
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dayOfWeek,
                            style = MaterialTheme.typography.caption, // Reduced from subtitle2
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )

                        // Slot count badge
                        Card(
                            backgroundColor = MaterialTheme.colors.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${slots.size} slot${if (slots.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.caption,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp, // Explicit small font size
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Slots list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp), // Reduced spacing
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(slots) { slot ->
                    SlotItem(slot = slot)
                }
            }
        }
    }
}

@Composable
fun SlotItem(
    slot: AvailabilitySlot
) {
    val startTime = Date(slot.startTime)
    val endTime = Date(slot.endTime)
    val timeFormat = SimpleDateFormat("h:mm a")

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.background,
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), // Reduced padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time indicator
                Card(
                    shape = RoundedCornerShape(6.dp),
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp) // Reduced size
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "🕐",
                            fontSize = 12.sp, // Explicit small font size
                            style = MaterialTheme.typography.caption
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Time text
                Text(
                    text = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}",
                    style = MaterialTheme.typography.caption, // Reduced from body1
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onSurface
                )
            }

            // Duration badge
            Card(
                backgroundColor = MaterialTheme.colors.primary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${slot.slotDuration}min",
                    style = MaterialTheme.typography.caption,
                    fontSize = 10.sp, // Explicit small font size
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun AddSlotsForm(
    availabilityRepository: AvailabilityRepository,
    onSlotsCreated: (Int) -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit
) {
    // State variables remain unchanged
    var startDate by remember { mutableStateOf(Date()) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("17:00") }
    var slotDuration by remember { mutableStateOf(30) }
    var excludeWeekends by remember { mutableStateOf(false) }
    var isRangeMode by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(), // Fill all available height
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 6.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween // Distribute content evenly
        ) {
            // Top content area (all form fields)
            Column(
                modifier = Modifier.weight(1f) // Takes as much space as available
            ) {
                // Enhanced header with background
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    elevation = 0.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add New Availability Slots",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }

                // Content scrollable area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()) // Make content scrollable
                ) {
                    // Range mode toggle - enhanced
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colors.background,
                        elevation = 0.dp,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isRangeMode,
                                onCheckedChange = {
                                    isRangeMode = it
                                    if (!it) endDate = null
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colors.primary
                                ),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Add slots for multiple days",
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section header
                    Text(
                        text = "DATE & TIME",
                        style = MaterialTheme.typography.overline,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )

                    Divider(
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Date inputs - enhanced
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Date",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = 1.dp,
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = MaterialTheme.colors.background
                            ) {
                                DatePickerField(
                                    selectedDate = startDate,
                                    onDateSelected = { startDate = it },
                                    showPicker = showStartDatePicker,
                                    onShowPicker = { showStartDatePicker = true },
                                    onHidePicker = { showStartDatePicker = false }
                                )
                            }
                        }

                        if (isRangeMode) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "End Date",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = 1.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    backgroundColor = MaterialTheme.colors.background
                                ) {
                                    DatePickerField(
                                        selectedDate = endDate ?: Date(),
                                        onDateSelected = { endDate = it },
                                        showPicker = showEndDatePicker,
                                        onShowPicker = { showEndDatePicker = true },
                                        onHidePicker = { showEndDatePicker = false },
                                        placeholder = "Select end date"
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Time inputs - enhanced
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Time",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = 1.dp,
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = MaterialTheme.colors.background
                            ) {
                                TimePickerField(
                                    selectedTime = startTime,
                                    onTimeSelected = { startTime = it },
                                    showPicker = showStartTimePicker,
                                    onShowPicker = { showStartTimePicker = true },
                                    onHidePicker = { showStartTimePicker = false }
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "End Time",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = 1.dp,
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = MaterialTheme.colors.background
                            ) {
                                TimePickerField(
                                    selectedTime = endTime,
                                    onTimeSelected = { endTime = it },
                                    showPicker = showEndTimePicker,
                                    onShowPicker = { showEndTimePicker = true },
                                    onHidePicker = { showEndTimePicker = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section header for configuration
                    Text(
                        text = "CONFIGURATION",
                        style = MaterialTheme.typography.overline,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )

                    Divider(
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Duration input - enhanced
                    Column {
                        Text(
                            text = "Slot Duration (minutes)",
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 1.dp,
                            shape = RoundedCornerShape(8.dp),
                            backgroundColor = MaterialTheme.colors.background
                        ) {
                            OutlinedTextField(
                                value = slotDuration.toString(),
                                onValueChange = {
                                    val newValue = it.toIntOrNull() ?: slotDuration
                                    if (newValue in 15..480) { // 15 minutes to 8 hours
                                        slotDuration = newValue
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.body2,
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number
                                ),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colors.primary,
                                    unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                                    backgroundColor = MaterialTheme.colors.background
                                ),
                                trailingIcon = {
                                    Text(
                                        text = "min",
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Exclude weekends - enhanced
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colors.background,
                        elevation = 1.dp,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Exclude Weekends",
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = excludeWeekends,
                                onCheckedChange = { excludeWeekends = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colors.primary,
                                    checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }

                    // Add some extra space at the bottom of scrollable content
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom section - action buttons
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                backgroundColor = MaterialTheme.colors.background.copy(alpha = 0.5f),
                elevation = 0.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colors.primary
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.button,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                try {
                                    val request = AvailabilitySlotRequest(
                                        startDate = startDate,
                                        endDate = if (isRangeMode) endDate else null,
                                        startTime = startTime,
                                        endTime = endTime,
                                        slotDuration = slotDuration,
                                        excludeWeekends = excludeWeekends
                                    )

                                    val createdSlots = availabilityRepository.createSlotsFromRequest(request)
                                    onSlotsCreated(createdSlots.size)

                                    // Reset form
                                    startDate = Date()
                                    endDate = null
                                    startTime = "09:00"
                                    endTime = "17:00"
                                    slotDuration = 30
                                    excludeWeekends = false
                                    isRangeMode = false

                                } catch (e: Exception) {
                                    onError(e.message ?: "Failed to create slots")
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = Color.White,
                            disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.3f)
                        ),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 3.dp,
                            pressedElevation = 6.dp
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            "Create Availability Slots",
                            style = MaterialTheme.typography.button,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// Add this extension function for Switch scaling
private fun Modifier.scale(scale: Float) = this.then(
    Modifier.graphicsLayer(
        scaleX = scale,
        scaleY = scale
    )
)

// Time and date picker fields remain the same but with smaller text sizes
@Composable
fun DatePickerField(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    showPicker: Boolean,
    onShowPicker: () -> Unit,
    onHidePicker: () -> Unit,
    placeholder: String = "Select date"
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy")

    OutlinedButton(
        onClick = onShowPicker,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp), // Reduced padding
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onSurface
        )
    ) {
        Text(
            text = dateFormat.format(selectedDate),
            style = MaterialTheme.typography.body2, // Reduced from body1
            fontSize = 12.sp // Explicit small font size
        )
    }

    if (showPicker) {
        NativeDatePicker(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                onDateSelected(date)
                onHidePicker()
            },
            onDismiss = onHidePicker
        )
    }
}

@Composable
fun TimePickerField(
    selectedTime: String,
    onTimeSelected: (String) -> Unit,
    showPicker: Boolean,
    onShowPicker: () -> Unit,
    onHidePicker: () -> Unit
) {
    OutlinedButton(
        onClick = onShowPicker,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp), // Reduced padding
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onSurface
        )
    ) {
        Text(
            text = selectedTime,
            style = MaterialTheme.typography.body2, // Reduced from body1
            fontSize = 12.sp // Explicit small font size
        )
    }

    if (showPicker) {
        NativeTimePicker(
            selectedTime = selectedTime,
            onTimeSelected = { time ->
                onTimeSelected(time)
                onHidePicker()
            },
            onDismiss = onHidePicker
        )
    }
}

@Composable
fun NativeDatePicker(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    var currentDate by remember { mutableStateOf(selectedDate) }
    val calendar = remember { Calendar.getInstance() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Date",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h6
            )
        },
        text = {
            Column {
                // Month and year navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            calendar.time = currentDate
                            calendar.add(Calendar.MONTH, -1)
                            currentDate = calendar.time
                        },
                        modifier = Modifier.size(36.dp) // Reduced size
                    ) {
                        Text("◀", fontSize = 16.sp) // Reduced size
                    }

                    Text(
                        text = SimpleDateFormat("MMMM yyyy").format(currentDate),
                        style = MaterialTheme.typography.subtitle1, // Reduced from h6
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(
                        onClick = {
                            calendar.time = currentDate
                            calendar.add(Calendar.MONTH, 1)
                            currentDate = calendar.time
                        },
                        modifier = Modifier.size(36.dp) // Reduced size
                    ) {
                        Text("▶", fontSize = 16.sp) // Reduced size
                    }
                }

                Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing

                // Calendar grid
                CalendarGrid(
                    selectedDate = currentDate,
                    onDateSelected = { date ->
                        currentDate = date
                    }
                )

                Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing

                // Quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        currentDate = Date()
                    }) {
                        Text("Today", fontSize = 12.sp) // Reduced size
                    }
                    TextButton(onClick = {
                        calendar.time = Date()
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                        currentDate = calendar.time
                    }) {
                        Text("Tomorrow", fontSize = 12.sp) // Reduced size
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(currentDate)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CalendarGrid(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    val today = Date()

    // Get first day of month and number of days
    calendar.time = selectedDate
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column {
        // Day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.caption,
                    fontSize = 10.sp, // Smaller text
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp), // Reduced width
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp)) // Reduced spacing

        // Calendar days
        var currentWeek = 0
        var currentDay = 1

        repeat(6) { week ->
            if (currentDay <= daysInMonth) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(7) { dayOfWeek ->
                        val dayNumber = if (week == 0 && dayOfWeek < firstDayOfMonth - 1) {
                            null
                        } else if (currentDay <= daysInMonth) {
                            val day = currentDay++
                            day
                        } else {
                            null
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp) // Reduced size
                                .background(
                                    color = when {
                                        dayNumber == null -> Color.Transparent
                                        dayNumber == selectedDate.date &&
                                                SimpleDateFormat("yyyy-MM").format(selectedDate) == SimpleDateFormat("yyyy-MM").format(Date()) -> MaterialTheme.colors.primary
                                        dayNumber == today.date &&
                                                SimpleDateFormat("yyyy-MM").format(today) == SimpleDateFormat("yyyy-MM").format(Date()) -> MaterialTheme.colors.primary.copy(alpha = 0.3f)
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(18.dp) // Half of the size
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            dayNumber?.let { day ->
                                TextButton(
                                    onClick = {
                                        calendar.time = selectedDate
                                        calendar.set(Calendar.DAY_OF_MONTH, day)
                                        onDateSelected(calendar.time)
                                    },
                                    modifier = Modifier.size(36.dp), // Reduced size
                                    contentPadding = PaddingValues(0.dp), // Remove padding
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = when {
                                            day == selectedDate.date &&
                                                    SimpleDateFormat("yyyy-MM").format(selectedDate) == SimpleDateFormat("yyyy-MM").format(Date()) -> Color.White
                                            day == today.date &&
                                                    SimpleDateFormat("yyyy-MM").format(today) == SimpleDateFormat("yyyy-MM").format(Date()) -> MaterialTheme.colors.primary
                                            else -> MaterialTheme.colors.onSurface
                                        }
                                    )
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.body2,
                                        fontSize = 11.sp, // Smaller text
                                        fontWeight = if (day == selectedDate.date) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NativeTimePicker(
    selectedTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(selectedTime.split(":")[0].toInt()) }
    var minute by remember { mutableStateOf(selectedTime.split(":")[1].toInt()) }
    var isAM by remember { mutableStateOf(hour < 12) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Time",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h6
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time display
                Text(
                    text = String.format("%02d:%02d %s",
                        if (hour == 0) 12 else if (hour > 12) hour - 12 else hour,
                        minute,
                        if (isAM) "AM" else "PM"
                    ),
                    style = MaterialTheme.typography.h5, // Reduced from h4
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(vertical = 12.dp) // Reduced padding
                )

                Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing

                // Time pickers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp), // Reduced spacing
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", style = MaterialTheme.typography.caption, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp)) // Reduced spacing
                        Column(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colors.surface,
                                    RoundedCornerShape(6.dp) // Smaller radius
                                )
                                .padding(6.dp) // Reduced padding
                        ) {
                            IconButton(
                                onClick = {
                                    val newHour = if (isAM) {
                                        if (hour == 1) 12 else hour - 1
                                    } else {
                                        if (hour == 13) 12 else hour - 1
                                    }
                                    hour = newHour
                                },
                                modifier = Modifier.size(32.dp) // Reduced size
                            ) {
                                Text("▲", fontSize = 14.sp) // Reduced size
                            }

                            Text(
                                text = String.format("%02d", if (hour == 0) 12 else if (hour > 12) hour - 12 else hour),
                                style = MaterialTheme.typography.subtitle1, // Reduced from h6
                                modifier = Modifier.padding(horizontal = 12.dp) // Reduced padding
                            )

                            IconButton(
                                onClick = {
                                    val newHour = if (isAM) {
                                        if (hour == 12) 1 else hour + 1
                                    } else {
                                        if (hour == 12) 13 else hour + 1
                                    }
                                    hour = newHour
                                },
                                modifier = Modifier.size(32.dp) // Reduced size
                            ) {
                                Text("▼", fontSize = 14.sp) // Reduced size
                            }
                        }
                    }

                    Text(":", style = MaterialTheme.typography.h5) // Reduced from h4

                    // Minute picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", style = MaterialTheme.typography.caption, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp)) // Reduced spacing
                        Column(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colors.surface,
                                    RoundedCornerShape(6.dp) // Smaller radius
                                )
                                .padding(6.dp) // Reduced padding
                        ) {
                            IconButton(
                                onClick = {
                                    minute = if (minute == 0) 45 else minute - 15
                                },
                                modifier = Modifier.size(32.dp) // Reduced size
                            ) {
                                Text("▲", fontSize = 14.sp) // Reduced size
                            }

                            Text(
                                text = String.format("%02d", minute),
                                style = MaterialTheme.typography.subtitle1, // Reduced from h6
                                modifier = Modifier.padding(horizontal = 12.dp) // Reduced padding
                            )

                            IconButton(
                                onClick = {
                                    minute = if (minute == 45) 0 else minute + 15
                                },
                                modifier = Modifier.size(32.dp) // Reduced size
                            ) {
                                Text("▼", fontSize = 14.sp) // Reduced size
                            }
                        }
                    }

                    // AM/PM picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Period", style = MaterialTheme.typography.caption, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp)) // Reduced spacing
                        Column(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colors.surface,
                                    RoundedCornerShape(6.dp) // Smaller radius
                                )
                                .padding(6.dp) // Reduced padding
                        ) {
                            IconButton(
                                onClick = { isAM = !isAM },
                                modifier = Modifier.size(32.dp) // Reduced size
                            ) {
                                Text("▲", fontSize = 14.sp) // Reduced size
                            }

                            Text(
                                text = if (isAM) "AM" else "PM",
                                style = MaterialTheme.typography.subtitle1, // Reduced from h6
                                modifier = Modifier.padding(horizontal = 12.dp) // Reduced padding
                            )

                            IconButton(
                                onClick = { isAM = !isAM },
                                modifier = Modifier.size(32.dp) // Reduced size
                            ) {
                                Text("▼", fontSize = 14.sp) // Reduced size
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing

                // Quick time buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("09:00", "12:00", "15:00", "18:00").forEach { time ->
                        val (h, m) = time.split(":").map { it.toInt() }
                        TextButton(
                            onClick = {
                                hour = h
                                minute = m
                                isAM = h < 12
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp) // Reduced padding
                        ) {
                            Text(time, fontSize = 12.sp) // Smaller text
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val finalHour = if (isAM && hour == 12) 0 else if (!isAM && hour != 12) hour + 12 else hour
                val timeString = String.format("%02d:%02d", finalHour, minute)
                onTimeSelected(timeString)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}