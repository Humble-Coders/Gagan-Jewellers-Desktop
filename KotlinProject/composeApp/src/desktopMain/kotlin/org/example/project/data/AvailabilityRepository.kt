package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.*

interface AvailabilityRepository {
    suspend fun getAllAvailabilitySlots(): List<AvailabilitySlot>
    suspend fun getAvailabilitySlotsByDate(date: Date): List<AvailabilitySlot>
    suspend fun getAvailabilitySlotsByDateRange(startDate: Date, endDate: Date): List<AvailabilitySlot>
    suspend fun addAvailabilitySlot(slot: AvailabilitySlot): String
    suspend fun addAvailabilitySlots(slots: List<AvailabilitySlot>): List<String>
    suspend fun deleteAvailabilitySlot(slotId: String): Boolean
    suspend fun updateAvailabilitySlot(slot: AvailabilitySlot): Boolean
    suspend fun createSlotsFromRequest(request: AvailabilitySlotRequest): List<String>
    fun getAvailabilitySlotsStream(): Flow<List<AvailabilitySlot>>
}

class FirestoreAvailabilityRepository(private val firestore: Firestore) : AvailabilityRepository {

    override suspend fun getAllAvailabilitySlots(): List<AvailabilitySlot> = withContext(Dispatchers.IO) {
        try {
            val availabilityCollection = firestore.collection("bookings")
            val query = availabilityCollection.whereEqualTo("type", "availability")
            val future = query.get()
            val snapshot = future.get()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data
                    AvailabilitySlot(
                        id = doc.id,
                        type = data["type"] as? String ?: "availability",
                        startTime = (data["startTime"] as? com.google.cloud.Timestamp)?.seconds?.times(1000) 
                            ?: (data["startTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        endTime = (data["endTime"] as? com.google.cloud.Timestamp)?.seconds?.times(1000)
                            ?: (data["endTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        slotDuration = (data["slotDuration"] as? Number)?.toInt() ?: 30,
                        createdAt = (data["createdAt"] as? com.google.cloud.Timestamp)?.seconds?.times(1000)
                            ?: (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    )
                } catch (e: Exception) {
                    println("Error parsing availability slot document ${doc.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching availability slots: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getAvailabilitySlotsByDate(date: Date): List<AvailabilitySlot> = withContext(Dispatchers.IO) {
        try {
            val allSlots = getAllAvailabilitySlots()
            
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis
            
            allSlots.filter { slot ->
                slot.startTime >= startOfDay && slot.startTime < endOfDay
            }.sortedBy { it.startTime }
        } catch (e: Exception) {
            println("Error fetching availability slots by date: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getAvailabilitySlotsByDateRange(startDate: Date, endDate: Date): List<AvailabilitySlot> = withContext(Dispatchers.IO) {
        try {
            val allSlots = getAllAvailabilitySlots()
            
            val startTime = startDate.time
            val endTime = endDate.time
            
            allSlots.filter { slot ->
                slot.startTime >= startTime && slot.startTime <= endTime
            }.sortedBy { it.startTime }
        } catch (e: Exception) {
            println("Error fetching availability slots by date range: ${e.message}")
            emptyList()
        }
    }

    override suspend fun addAvailabilitySlot(slot: AvailabilitySlot): String = withContext(Dispatchers.IO) {
        try {
            val availabilityCollection = firestore.collection("bookings")
            val docRef = availabilityCollection.document()
            val newSlotId = docRef.id
            
            val slotMap = mapOf(
                "type" to "availability",
                "startTime" to com.google.cloud.Timestamp.of(Date(slot.startTime)),
                "endTime" to com.google.cloud.Timestamp.of(Date(slot.endTime)),
                "slotDuration" to slot.slotDuration,
                "createdAt" to com.google.cloud.Timestamp.of(Date(slot.createdAt)),
            )
            
            docRef.set(slotMap).get()
            println("✅ Availability slot created with ID: $newSlotId")
            newSlotId
        } catch (e: Exception) {
            println("Error adding availability slot: ${e.message}")
            throw e
        }
    }

    override suspend fun addAvailabilitySlots(slots: List<AvailabilitySlot>): List<String> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            val slotIds = mutableListOf<String>()
            
            slots.forEach { slot ->
                val docRef = firestore.collection("bookings").document()
                slotIds.add(docRef.id)
                
                val slotMap = mapOf(
                    "type" to "availability",
                    "startTime" to com.google.cloud.Timestamp.of(Date(slot.startTime)),
                    "endTime" to com.google.cloud.Timestamp.of(Date(slot.endTime)),
                    "slotDuration" to slot.slotDuration,
                    "createdAt" to com.google.cloud.Timestamp.of(Date(slot.createdAt)),
                )
                
                batch.set(docRef, slotMap)
            }
            
            batch.commit().get()
            println("✅ Created ${slots.size} availability slots")
            slotIds
        } catch (e: Exception) {
            println("Error adding multiple availability slots: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteAvailabilitySlot(slotId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("bookings").document(slotId).delete().get()
            println("✅ Deleted availability slot: $slotId")
            true
        } catch (e: Exception) {
            println("Error deleting availability slot: ${e.message}")
            false
        }
    }

    override suspend fun updateAvailabilitySlot(slot: AvailabilitySlot): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("bookings").document(slot.id)
            
            val slotMap = mapOf(
                "type" to "availability",
                "startTime" to com.google.cloud.Timestamp.of(Date(slot.startTime)),
                "endTime" to com.google.cloud.Timestamp.of(Date(slot.endTime)),
                "slotDuration" to slot.slotDuration,
                "createdAt" to com.google.cloud.Timestamp.of(Date(slot.createdAt)),
            )
            
            docRef.update(slotMap).get()
            println("✅ Updated availability slot: ${slot.id}")
            true
        } catch (e: Exception) {
            println("Error updating availability slot: ${e.message}")
            false
        }
    }

    override suspend fun createSlotsFromRequest(request: AvailabilitySlotRequest): List<String> = withContext(Dispatchers.IO) {
        try {
            println("🎯 Creating single slot per date: ${request.startDate} to ${request.endDate}, ${request.startTime} - ${request.endTime}, ${request.slotDuration}min")
            
            val slots = mutableListOf<AvailabilitySlot>()
            val calendar = Calendar.getInstance()
            
            // Parse time strings
            val startTimeParts = request.startTime.split(":")
            val endTimeParts = request.endTime.split(":")
            val startHour = startTimeParts[0].toInt()
            val startMinute = startTimeParts[1].toInt()
            val endHour = endTimeParts[0].toInt()
            val endMinute = endTimeParts[1].toInt()
            
            println("⏰ Parsed times: Start ${startHour}:${startMinute}, End ${endHour}:${endMinute}")
            
            // Start from start date
            calendar.time = request.startDate
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            val endDate = request.endDate ?: request.startDate
            println("📅 Date range: ${request.startDate} to ${endDate}")
            
            // Create ONE slot per date with the exact time range selected
            while (calendar.time <= endDate) {
                val currentDate = calendar.time
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                
                println("📆 Processing date: $currentDate (Day of week: $dayOfWeek)")
                
                // Skip weekends if requested
                if (request.excludeWeekends && (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)) {
                    println("⏭️ Skipping weekend: $currentDate")
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    continue
                }
                
                // Skip excluded dates
                if (request.excludeDates.any { excludedDate -> excludedDate.time == currentDate.time }) {
                    println("⏭️ Skipping excluded date: $currentDate")
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    continue
                }
                
                // Create start and end times for this specific date
                val slotStart = Calendar.getInstance().apply {
                    time = currentDate
                    set(Calendar.HOUR_OF_DAY, startHour)
                    set(Calendar.MINUTE, startMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val slotEnd = Calendar.getInstance().apply {
                    time = currentDate
                    set(Calendar.HOUR_OF_DAY, endHour)
                    set(Calendar.MINUTE, endMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                println("🕐 Creating slot: ${Date(slotStart)} to ${Date(slotEnd)}")
                
                // Create ONE slot for this date with the exact time range
                slots.add(
                    AvailabilitySlot(
                        startTime = slotStart,
                        endTime = slotEnd,
                        slotDuration = request.slotDuration,
                        createdAt = System.currentTimeMillis(),
                    )
                )
                
                println("✅ Created 1 slot for $currentDate")
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            println("📊 Total slots generated: ${slots.size}")
            
            // Add all slots to Firestore
            if (slots.isNotEmpty()) {
                val slotIds = addAvailabilitySlots(slots)
                println("💾 Successfully saved ${slotIds.size} slots to Firestore")
                slotIds
            } else {
                println("❌ No slots were generated - check date range")
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ Error creating slots from request: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    override fun getAvailabilitySlotsStream(): Flow<List<AvailabilitySlot>> = callbackFlow {
        try {
            val availabilityCollection = firestore.collection("bookings")
            val query = availabilityCollection.whereEqualTo("type", "availability")
            
            println("🔄 Setting up real-time listener for availability slots...")
            
            // Create a listener for real-time updates
            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("❌ Error listening to availability slots: ${error.message}")
                    return@addSnapshotListener
                }
                
                snapshot?.let { querySnapshot ->
                    val slots = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data
                            AvailabilitySlot(
                                id = doc.id,
                                type = data["type"] as? String ?: "availability",
                                startTime = (data["startTime"] as? com.google.cloud.Timestamp)?.seconds?.times(1000) 
                                    ?: (data["startTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                endTime = (data["endTime"] as? com.google.cloud.Timestamp)?.seconds?.times(1000)
                                    ?: (data["endTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                slotDuration = (data["slotDuration"] as? Number)?.toInt() ?: 30,
                                createdAt = (data["createdAt"] as? com.google.cloud.Timestamp)?.seconds?.times(1000)
                                    ?: (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            )
                        } catch (e: Exception) {
                            println("Error parsing availability slot document ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    
                    println("📅 Real-time listener received ${slots.size} slots")
                    
                    // Send the slots through the channel
                    trySend(slots.sortedBy { it.startTime })
                }
            }
            
            // Handle cleanup when the flow is cancelled
            awaitClose {
                registration.remove()
                println("🔄 Availability slots listener removed")
            }
            
        } catch (e: Exception) {
            println("❌ Error setting up availability slots stream: ${e.message}")
            close(e)
        }
    }
}
