package org.example.project.data

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

interface BookingRepository {
    suspend fun getAllBookings(): List<Booking>
    suspend fun getBookingsByDate(date: Date): List<Booking>
    suspend fun getBookingsByDateRange(startDate: Date, endDate: Date): List<Booking>
    suspend fun getBookingById(id: String): Booking?
    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Boolean
    suspend fun getBookingsWithUserInfo(date: Date): List<AppointmentWithUser>
}

class FirestoreBookingRepository(private val firestore: Firestore) : BookingRepository {

    override suspend fun getAllBookings(): List<Booking> = withContext(Dispatchers.IO) {
        try {
            val bookingsCollection = firestore.collection("bookings")
            val query = bookingsCollection.whereEqualTo("type", "booking")
            val future = query.get()
            val snapshot = future.get()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data
                    Booking(
                        id = doc.id,
                        type = data["type"] as? String ?: "booking",
                        userId = data["userId"] as? String ?: "",
                        userName = data["userName"] as? String,
                        startTime = (data["startTime"] as? com.google.cloud.Timestamp)?.seconds?.times(1000) 
                            ?: (data["startTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        endTime = (data["endTime"] as? com.google.cloud.Timestamp)?.seconds?.times(1000)
                            ?: (data["endTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        status = try {
                            BookingStatus.valueOf(data["status"] as? String ?: "PENDING")
                        } catch (e: Exception) {
                            BookingStatus.PENDING
                        },
                        createdAt = (data["createdAt"] as? com.google.cloud.Timestamp)?.seconds?.times(1000)
                            ?: (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        wishlistProductIds = (data["wishlistProductIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        notes = data["notes"] as? String ?: "",
                        serviceType = data["serviceType"] as? String ?: "",
                        estimatedDuration = (data["estimatedDuration"] as? Number)?.toInt() ?: 30
                    )
                } catch (e: Exception) {
                    println("Error parsing booking document ${doc.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching bookings: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBookingsByDate(date: Date): List<Booking> = withContext(Dispatchers.IO) {
        try {
            // Fetch all bookings and filter locally by date
            val allBookings = getAllBookings()
            
            // Filter by specific date
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis
            
            allBookings.filter { booking ->
                booking.startTime >= startOfDay && booking.startTime < endOfDay
            }.sortedBy { it.startTime }
        } catch (e: Exception) {
            println("Error fetching bookings by date: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBookingsByDateRange(startDate: Date, endDate: Date): List<Booking> = withContext(Dispatchers.IO) {
        try {
            // Fetch all bookings and filter locally
            val allBookings = getAllBookings()
            
            // Filter by date range locally
            allBookings.filter { booking ->
                val bookingDate = Date(booking.startTime)
                bookingDate >= startDate && bookingDate < endDate
            }.sortedBy { it.startTime }
        } catch (e: Exception) {
            println("Error fetching bookings by date range: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBookingById(id: String): Booking? = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("bookings").document(id)
            val future = docRef.get()
            val snapshot = future.get()
            
            if (snapshot.exists()) {
                val data = snapshot.data
                Booking(
                    id = snapshot.id,
                    type = data?.get("type") as? String ?: "booking",
                    userId = data?.get("userId") as? String ?: "",
                    userName = data?.get("userName") as? String,
                    startTime = (data?.get("startTime") as? com.google.cloud.Timestamp)?.seconds?.times(1000) 
                        ?: (data?.get("startTime") as? Number)?.toLong() ?: System.currentTimeMillis(),
                    endTime = (data?.get("endTime") as? com.google.cloud.Timestamp)?.seconds?.times(1000)
                        ?: (data?.get("endTime") as? Number)?.toLong() ?: System.currentTimeMillis(),
                    status = try {
                        BookingStatus.valueOf(data?.get("status") as? String ?: "PENDING")
                    } catch (e: Exception) {
                        BookingStatus.PENDING
                    },
                    createdAt = (data?.get("createdAt") as? com.google.cloud.Timestamp)?.seconds?.times(1000)
                        ?: (data?.get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis(),
                    wishlistProductIds = (data?.get("wishlistProductIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    notes = data?.get("notes") as? String ?: "",
                    serviceType = data?.get("serviceType") as? String ?: "",
                    estimatedDuration = (data?.get("estimatedDuration") as? Number)?.toInt() ?: 30
                )
            } else null
        } catch (e: Exception) {
            println("Error fetching booking by ID: ${e.message}")
            null
        }
    }

    override suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("bookings").document(bookingId)
            docRef.update("status", status.name).get()
            true
        } catch (e: Exception) {
            println("Error updating booking status: ${e.message}")
            false
        }
    }

    override suspend fun getBookingsWithUserInfo(date: Date): List<AppointmentWithUser> = withContext(Dispatchers.IO) {
        try {
            // Get bookings for the specific date
            val bookings = getBookingsByDate(date)
            
            // Fetch user info for all unique user IDs
            val uniqueUserIds = bookings.map { it.userId }.distinct().filter { it.isNotEmpty() }
            val userCache = mutableMapOf<String, User?>()
            
            // Fetch all users in parallel
            uniqueUserIds.forEach { userId ->
                userCache[userId] = getUserById(userId)
            }
            
            // Create appointments with user info
            bookings.map { booking ->
                AppointmentWithUser(
                    booking = booking,
                    user = userCache[booking.userId]
                )
            }
        } catch (e: Exception) {
            println("Error fetching bookings with user info: ${e.message}")
            emptyList()
        }
    }

    private suspend fun getUserById(userId: String): User? = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("users").document(userId)
            val future = docRef.get()
            val snapshot = future.get()
            
            if (snapshot.exists()) {
                val data = snapshot.data
                User(
                    id = snapshot.id,
                    email = data?.get("email") as? String ?: "",
                    name = data?.get("name") as? String ?: "",
                    phone = data?.get("phone") as? String ?: "",
                    address = data?.get("address") as? String ?: "",
                    createdAt = (data?.get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            println("Error fetching user by ID: ${e.message}")
            null
        }
    }
}
