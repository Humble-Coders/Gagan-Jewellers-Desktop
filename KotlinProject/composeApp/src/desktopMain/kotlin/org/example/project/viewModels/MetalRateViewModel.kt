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

class MetalRateViewModel(private val metalRateRepository: MetalRateRepository) : ViewModel() {
    
    private val _metalRates = MutableStateFlow<List<MetalRate>>(emptyList())
    val metalRates: StateFlow<List<MetalRate>> = _metalRates.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadMetalRates()
    }
    
    fun loadMetalRates() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _metalRates.value = metalRateRepository.getAllMetalRates()
                _error.value = null
                println("📊 Loaded ${_metalRates.value.size} metal rates")
            } catch (e: Exception) {
                _error.value = "Failed to load metal rates: ${e.message}"
                println("❌ Error loading metal rates: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun addMetalRate(metalRate: MetalRate) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val newId = metalRateRepository.addMetalRate(metalRate)
                loadMetalRates() // Refresh the list
                _error.value = null
                println("✅ Metal rate added with ID: $newId")
            } catch (e: Exception) {
                _error.value = "Failed to add metal rate: ${e.message}"
                println("❌ Error adding metal rate: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun updateMetalRate(metalRate: MetalRate) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val success = metalRateRepository.updateMetalRate(metalRate)
                if (success) {
                    loadMetalRates() // Refresh the list
                    _error.value = null
                    println("✅ Metal rate updated: ${metalRate.id}")
                } else {
                    _error.value = "Failed to update metal rate"
                }
            } catch (e: Exception) {
                _error.value = "Failed to update metal rate: ${e.message}"
                println("❌ Error updating metal rate: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun deleteMetalRate(id: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val success = metalRateRepository.deleteMetalRate(id)
                if (success) {
                    loadMetalRates() // Refresh the list
                    _error.value = null
                    println("✅ Metal rate deleted: $id")
                } else {
                    _error.value = "Failed to delete metal rate"
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete metal rate: ${e.message}"
                println("❌ Error deleting metal rate: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun calculateRateForMaterial(materialId: String, materialType: String, karat: Int): Double {
        return try {
            val metalRate = _metalRates.value.find { 
                it.materialId == materialId && 
                it.materialType == materialType && 
                it.isActive 
            }
            if (metalRate != null) {
                val calculatedRate = metalRate.calculateRateForKarat(karat)
                println("💰 Calculated rate for $materialType $karat K: $calculatedRate (base: ${metalRate.pricePerGram} for ${metalRate.karat}K)")
                calculatedRate
            } else {
                println("⚠️ No metal rate found for material: $materialId, type: $materialType")
                0.0
            }
        } catch (e: Exception) {
            println("❌ Error calculating rate: ${e.message}")
            0.0
        }
    }
    
    fun updateOrCreateMetalRate(materialId: String, materialName: String, materialType: String, pricePerGram: Double) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val rateId = metalRateRepository.updateOrCreateMetalRate(materialId, materialName, materialType, pricePerGram)
                if (rateId.isNotEmpty()) {
                    loadMetalRates() // Refresh the list
                    _error.value = null
                    println("✅ Successfully updated/created metal rate: $materialName $materialType")
                } else {
                    _error.value = "Failed to update/create metal rate"
                }
            } catch (e: Exception) {
                _error.value = "Failed to update/create metal rate: ${e.message}"
                println("❌ Error updating/creating metal rate: ${e.message}")
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun getMetalRateByMaterialAndType(materialId: String, materialType: String): MetalRate? {
        return _metalRates.value.find { 
            it.materialId == materialId && 
            it.materialType == materialType && 
            it.isActive 
        }
    }
    
    fun updateMetalRateWithHistory(materialId: String, materialName: String, materialType: String, newPricePerGram: Double) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val success = metalRateRepository.updateMetalRateWithHistory(materialId, materialName, materialType, newPricePerGram)
                if (success) {
                    loadMetalRates() // Refresh the list
                    _error.value = null
                    println("✅ Metal rate updated with history: $materialName $materialType")
                } else {
                    _error.value = "Failed to update metal rate"
                }
            } catch (e: Exception) {
                _error.value = "Failed to update metal rate: ${e.message}"
                println("❌ Error updating metal rate with history: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
}
