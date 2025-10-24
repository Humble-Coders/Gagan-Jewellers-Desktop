package org.example.project.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.AvailabilityRepository
import org.example.project.data.BookingStatus
import org.example.project.viewModels.AppointmentViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppointmentScreen(
    viewModel: AppointmentViewModel,
    availabilityRepository: AvailabilityRepository,
    onBack: () -> Unit
) {
    var showAddSlots by remember { mutableStateOf(false) }
    
    val selectedDate by remember { derivedStateOf { viewModel.selectedDate } }
    val appointments by remember { derivedStateOf { viewModel.appointments } }
    val datesWithAppointments by remember { derivedStateOf { viewModel.datesWithAppointments } }
    val loading by remember { derivedStateOf { viewModel.loading } }
    val error by remember { derivedStateOf { viewModel.error } }

    if (showAddSlots) {
        AddSlotsScreen(
            availabilityRepository = availabilityRepository,
            onBack = { showAddSlots = false }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        AppointmentHeader(
            selectedDate = selectedDate,
            appointmentCount = appointments.size,
            onBack = onBack,
            onRefresh = { viewModel.refreshAppointments() },
            onNavigateToToday = { viewModel.navigateToToday() },
            onPreviousDay = { viewModel.navigateToPreviousDay() },
            onNextDay = { viewModel.navigateToNextDay() },
            onAddSlots = { showAddSlots = true }
        )

        // Error message
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colors.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.body2
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }

        // Main content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Calendar (Left side)
            AppointmentCalendar(
                selectedDate = selectedDate,
                datesWithAppointments = datesWithAppointments,
                onDateSelected = { date -> viewModel.selectDate(date) },
                modifier = Modifier.width(320.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Timeline (Right side)
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                if (loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading appointments...",
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    AppointmentTimeline(
                        appointments = appointments,
                        onStatusUpdate = { bookingId, status ->
                            viewModel.updateBookingStatus(bookingId, status)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentHeader(
    selectedDate: Date,
    appointmentCount: Int,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToToday: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onAddSlots: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.primary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button and title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Appointments",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${appointmentCount} appointment${if (appointmentCount != 1) "s" else ""} scheduled",
                        style = MaterialTheme.typography.body2,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Actions on the extreme right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add Slots button
                Button(
                    onClick = onAddSlots,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.White.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colors.primary
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Slots", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                // Refresh button
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
