package org.example.project.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.MetalRate
import org.example.project.data.MetalRateRepository
import org.example.project.data.calculateRateForKarat
import org.example.project.data.normalizeMaterialType

class MetalRateViewModel(private val metalRateRepository: MetalRateRepository) : ViewModel() {
    
    // Observe StateFlow from repository instead of maintaining separate state
    val metalRates: StateFlow<List<MetalRate>> = metalRateRepository.metalRates
    val loading: StateFlow<Boolean> = metalRateRepository.loading
    val error: StateFlow<String?> = metalRateRepository.error
    
    init {
        // No longer need to call loadMetalRates() - repository listener handles it
        println("✅ METAL RATE VIEWMODEL: Initialized")
        println("   - ViewModel instance: ${this.hashCode()}")
        println("   - Using shared StateFlow from repository")
        println("   - Repository instance: ${metalRateRepository.hashCode()}")
    }
    
    fun loadMetalRates() {
        // No longer needed - metal rates are automatically updated via StateFlow listener
        // This method is kept for backward compatibility but does nothing
        println("ℹ️ METAL RATE VIEWMODEL: loadMetalRates() called but metal rates are now managed by repository listener")
    }
    
    fun addMetalRate(metalRate: MetalRate) {
        viewModelScope.launch {
            try {
                val newId = metalRateRepository.addMetalRate(metalRate)
                // Listener will automatically update metalRates StateFlow - no manual refresh needed
                println("✅ METAL RATE VIEWMODEL: Metal rate added with ID: $newId")
                println("   - Listener will automatically detect this change and update StateFlow")
            } catch (e: Exception) {
                println("❌ METAL RATE VIEWMODEL: Error adding metal rate: ${e.message}")
            }
        }
    }
    
    fun updateMetalRate(metalRate: MetalRate) {
        viewModelScope.launch {
            try {
                val success = metalRateRepository.updateMetalRate(metalRate)
                if (success) {
                    // Listener will automatically update metalRates StateFlow - no manual refresh needed
                    println("✅ METAL RATE VIEWMODEL: Metal rate updated: ${metalRate.id}")
                    println("   - Listener will automatically detect this change and update StateFlow")
                } else {
                    println("❌ METAL RATE VIEWMODEL: Failed to update metal rate")
                }
            } catch (e: Exception) {
                println("❌ METAL RATE VIEWMODEL: Error updating metal rate: ${e.message}")
            }
        }
    }
    
    fun deleteMetalRate(id: String) {
        viewModelScope.launch {
            try {
                val success = metalRateRepository.deleteMetalRate(id)
                if (success) {
                    // Listener will automatically update metalRates StateFlow - no manual refresh needed
                    println("✅ METAL RATE VIEWMODEL: Metal rate deleted: $id")
                    println("   - Listener will automatically detect this change and update StateFlow")
                } else {
                    println("❌ METAL RATE VIEWMODEL: Failed to delete metal rate")
                }
            } catch (e: Exception) {
                println("❌ METAL RATE VIEWMODEL: Error deleting metal rate: ${e.message}")
            }
        }
    }
    
    fun calculateRateForMaterial(materialId: String, materialType: String, karat: Int): Double {
        return try {
            // Normalize the input materialType for consistent comparison
            val normalizedMaterialType = normalizeMaterialType(materialType)
            
            // Try exact match first - normalize both stored and input materialType for comparison
            val exact = metalRates.value.find { 
                it.materialId == materialId && 
                normalizeMaterialType(it.materialType) == normalizedMaterialType && 
                it.isActive 
            }
            if (exact != null) {
                // When Firestore already stores price for this material type, use it directly
                // without recalculating by karat to avoid double-adjusting the rate.
                println("💰 Using Firestore rate for $normalizedMaterialType (no recalculation): ${exact.pricePerGram}")
                return exact.pricePerGram
            }

            // Fallback: use any active rate for this material and convert to target karat
            val anyForMaterial = metalRates.value.filter { it.materialId == materialId && it.isActive }
            val best = anyForMaterial.minByOrNull { kotlin.math.abs(it.karat - karat) }
            if (best != null) {
                val rate = best.calculateRateForKarat(karat)
                println("🔄 Fallback rate for ${best.materialType} ${best.karat}K -> ${karat}K = $rate")
                return rate
            }

            println("⚠️ No metal rate found for material: $materialId")
            0.0
        } catch (e: Exception) {
            println("❌ Error calculating rate: ${e.message}")
            0.0
        }
    }
    
    fun updateOrCreateMetalRate(materialId: String, materialName: String, materialType: String, pricePerGram: Double) {
        viewModelScope.launch {
            try {
                val rateId = metalRateRepository.updateOrCreateMetalRate(materialId, materialName, materialType, pricePerGram)
                if (rateId.isNotEmpty()) {
                    // Listener will automatically update metalRates StateFlow - no manual refresh needed
                    println("✅ METAL RATE VIEWMODEL: Successfully updated/created metal rate: $materialName $materialType")
                    println("   - Listener will automatically detect this change and update StateFlow")
                } else {
                    println("❌ METAL RATE VIEWMODEL: Failed to update/create metal rate")
                }
            } catch (e: Exception) {
                println("❌ METAL RATE VIEWMODEL: Error updating/creating metal rate: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun clearError() {
        // Error is now managed by repository StateFlow
        println("ℹ️ METAL RATE VIEWMODEL: clearError() called but error is now managed by repository StateFlow")
    }

    fun getMetalRateByMaterialAndType(materialId: String, materialType: String): MetalRate? {
        return metalRates.value.find { 
            it.materialId == materialId && 
            it.materialType == materialType && 
            it.isActive 
        }
    }
    
    fun updateMetalRateWithHistory(materialId: String, materialName: String, materialType: String, newPricePerGram: Double) {
        viewModelScope.launch {
            try {
                val success = metalRateRepository.updateMetalRateWithHistory(materialId, materialName, materialType, newPricePerGram)
                if (success) {
                    // Listener will automatically update metalRates StateFlow - no manual refresh needed
                    println("✅ METAL RATE VIEWMODEL: Metal rate updated with history: $materialName $materialType")
                    println("   - Listener will automatically detect this change and update StateFlow")
                } else {
                    println("❌ METAL RATE VIEWMODEL: Failed to update metal rate")
                }
            } catch (e: Exception) {
                println("❌ METAL RATE VIEWMODEL: Error updating metal rate with history: ${e.message}")
            }
        }
    }
    
    fun updateStoneRate(stoneName: String, purity: String, rate: Double) {
        viewModelScope.launch {
            try {
                // Update stone rate in stones collection
                val repository = metalRateRepository as? org.example.project.data.FirestoreMetalRateRepository
                if (repository != null) {
                    repository.updateStoneTypesArray(stoneName, purity, rate.toString())
                    // Listener will automatically update metalRates StateFlow - no manual refresh needed
                    println("✅ METAL RATE VIEWMODEL: Stone rate updated: $stoneName $purity")
                    println("   - Listener will automatically detect this change and update StateFlow")
                } else {
                    println("❌ METAL RATE VIEWMODEL: Failed to update stone rate")
                }
            } catch (e: Exception) {
                println("❌ METAL RATE VIEWMODEL: Error updating stone rate: ${e.message}")
            }
        }
    }
}
