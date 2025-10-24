package org.example.project.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppointmentCalendar(
    selectedDate: Date,
    datesWithAppointments: Set<Date>,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    
    // Initialize current month with selected date
    LaunchedEffect(selectedDate) {
        val cal = Calendar.getInstance()
        cal.time = selectedDate
        currentMonth = cal
    }

    Card(
        modifier = modifier.fillMaxHeight(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with month navigation
            CalendarHeader(
                currentMonth = currentMonth,
                onPreviousMonth = {
                    val newMonth = Calendar.getInstance()
                    newMonth.time = currentMonth.time
                    newMonth.add(Calendar.MONTH, -1)
                    currentMonth = newMonth
                },
                onNextMonth = {
                    val newMonth = Calendar.getInstance()
                    newMonth.time = currentMonth.time
                    newMonth.add(Calendar.MONTH, 1)
                    currentMonth = newMonth
                },
                onTodayClick = {
                    val today = Calendar.getInstance()
                    currentMonth = today
                    onDateSelected(today.time)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Day headers (Sun, Mon, Tue, etc.)
            DayHeaders()

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            CalendarGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                datesWithAppointments = datesWithAppointments,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous Month",
                tint = MaterialTheme.colors.primary
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface
            )
            
            TextButton(
                onClick = onTodayClick,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    Icons.Default.Today,
                    contentDescription = "Today",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Today")
            }
        }

        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Next Month",
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
private fun DayHeaders() {
    val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dayHeaders.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: Calendar,
    selectedDate: Date,
    datesWithAppointments: Set<Date>,
    onDateSelected: (Date) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.time = currentMonth.time
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    
    // Get the first day of the month and how many days to show before it
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    // Calculate total cells needed (6 weeks * 7 days)
    val totalCells = 42
    val daysBeforeMonth = firstDayOfWeek - Calendar.SUNDAY
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(6) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { dayOfWeek ->
                    val dayIndex = week * 7 + dayOfWeek
                    val dayOfMonth = dayIndex - daysBeforeMonth + 1
                    
                    CalendarDay(
                        dayOfMonth = dayOfMonth,
                        isCurrentMonth = dayOfMonth in 1..daysInMonth,
                        isSelected = isSameDay(dayOfMonth, daysInMonth, currentMonth, selectedDate),
                        hasAppointments = hasAppointmentsOnDay(dayOfMonth, daysInMonth, currentMonth, datesWithAppointments),
                        isToday = isToday(dayOfMonth, daysInMonth, currentMonth),
                        onClick = {
                            if (dayOfMonth in 1..daysInMonth) {
                                val selectedCalendar = Calendar.getInstance()
                                selectedCalendar.time = currentMonth.time
                                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                onDateSelected(selectedCalendar.time)
                            }
                        }
                    )
                }
            }
            if (week < 5) Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CalendarDay(
    dayOfMonth: Int,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    hasAppointments: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            isToday -> 0.3f
            else -> 0f
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )
    
    val contentColor = when {
        !isCurrentMonth -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
        isSelected -> Color.White
        else -> MaterialTheme.colors.onSurface
    }
    
    val borderColor = if (hasAppointments && !isSelected) {
        MaterialTheme.colors.primary.copy(alpha = 0.6f)
    } else {
        Color.Transparent
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colors.primary
                    isToday -> MaterialTheme.colors.primary.copy(alpha = backgroundColor)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (hasAppointments && !isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(enabled = isCurrentMonth) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isCurrentMonth) dayOfMonth.toString() else "",
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            )
            
            // Small dot indicator for appointments
            if (hasAppointments && isCurrentMonth) {
                Spacer(modifier = Modifier.height(1.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color.White 
                            else MaterialTheme.colors.primary
                        )
                )
            }
        }
    }
}

private fun isSameDay(dayOfMonth: Int, daysInMonth: Int, monthCalendar: Calendar, date: Date): Boolean {
    if (dayOfMonth !in 1..daysInMonth) return false
    
    val calendar = Calendar.getInstance()
    calendar.time = date
    
    val monthCal = Calendar.getInstance()
    monthCal.time = monthCalendar.time
    monthCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
    
    return calendar.get(Calendar.YEAR) == monthCal.get(Calendar.YEAR) &&
           calendar.get(Calendar.MONTH) == monthCal.get(Calendar.MONTH) &&
           calendar.get(Calendar.DAY_OF_MONTH) == monthCal.get(Calendar.DAY_OF_MONTH)
}

private fun hasAppointmentsOnDay(dayOfMonth: Int, daysInMonth: Int, monthCalendar: Calendar, datesWithAppointments: Set<Date>): Boolean {
    if (dayOfMonth !in 1..daysInMonth) return false
    
    val dayCalendar = Calendar.getInstance()
    dayCalendar.time = monthCalendar.time
    dayCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
    dayCalendar.set(Calendar.HOUR_OF_DAY, 0)
    dayCalendar.set(Calendar.MINUTE, 0)
    dayCalendar.set(Calendar.SECOND, 0)
    dayCalendar.set(Calendar.MILLISECOND, 0)
    
    return datesWithAppointments.contains(dayCalendar.time)
}

private fun isToday(dayOfMonth: Int, daysInMonth: Int, monthCalendar: Calendar): Boolean {
    if (dayOfMonth !in 1..daysInMonth) return false
    
    val today = Calendar.getInstance()
    val dayCalendar = Calendar.getInstance()
    dayCalendar.time = monthCalendar.time
    dayCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
    
    return today.get(Calendar.YEAR) == dayCalendar.get(Calendar.YEAR) &&
           today.get(Calendar.MONTH) == dayCalendar.get(Calendar.MONTH) &&
           today.get(Calendar.DAY_OF_MONTH) == dayCalendar.get(Calendar.DAY_OF_MONTH)
}
