package org.example.project.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.example.project.data.AppointmentWithUser
import org.example.project.data.BookingRepository
import org.example.project.data.BookingStatus
import java.util.*

class AppointmentViewModel(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    var selectedDate by mutableStateOf(Date())
        private set

    var appointments by mutableStateOf<List<AppointmentWithUser>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var datesWithAppointments by mutableStateOf<Set<Date>>(emptySet())
        private set

    init {
        loadAppointmentsForDate(selectedDate)
        loadDatesWithAppointments()
    }

    fun selectDate(date: Date) {
        selectedDate = date
        loadAppointmentsForDate(date)
    }

    fun loadAppointmentsForDate(date: Date) {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                appointments = bookingRepository.getBookingsWithUserInfo(date)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load appointments"
                appointments = emptyList()
            } finally {
                loading = false
            }
        }
    }

    fun loadDatesWithAppointments() {
        viewModelScope.launch {
            try {
                // Load all bookings and extract unique dates
                val allBookings = bookingRepository.getAllBookings()
                datesWithAppointments = allBookings.map { booking ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = booking.startTime
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.time
                }.toSet()
            } catch (e: Exception) {
                println("Error loading dates with appointments: ${e.message}")
                datesWithAppointments = emptySet()
            }
        }
    }

    fun updateBookingStatus(bookingId: String, status: BookingStatus) {
        viewModelScope.launch {
            try {
                val success = bookingRepository.updateBookingStatus(bookingId, status)
                if (success) {
                    // Reload appointments for the current date
                    loadAppointmentsForDate(selectedDate)
                    // Reload dates with appointments
                    loadDatesWithAppointments()
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to update booking status"
            }
        }
    }

    fun refreshAppointments() {
        loadAppointmentsForDate(selectedDate)
        loadDatesWithAppointments()
    }

    fun clearError() {
        error = null
    }

    fun navigateToToday() {
        selectDate(Date())
    }

    fun navigateToPreviousDay() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        selectDate(calendar.time)
    }

    fun navigateToNextDay() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        selectDate(calendar.time)
    }
}
