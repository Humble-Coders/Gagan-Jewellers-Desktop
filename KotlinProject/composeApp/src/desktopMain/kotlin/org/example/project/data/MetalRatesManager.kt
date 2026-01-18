package org.example.project.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Centralized singleton for managing metal rates across the entire application
 * This ensures all screens use the same rates and updates are propagated universally
 * Now observes StateFlow from MetalRateRepository for real-time updates
 */
object MetalRatesManager {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Repository instance (now using merged MetalRateRepository)
    private var repository: MetalRateRepository? = null
    
    // Current metal rates state (Compose State for UI compatibility)
    private val _metalRates = mutableStateOf(
        MetalRates(
            goldRates = GoldRates.calculateKaratRates(6080.0),
            silverRates = SilverRates.calculateSilverRates(75.0)
        )
    )
    val metalRates: State<MetalRates> = _metalRates
    
    // Loading state (Compose State for UI compatibility)
    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading
    
    // Error state (Compose State for UI compatibility)
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error
    
    fun initialize(repository: MetalRateRepository) {
        this.repository = repository
        println("✅ METAL RATES MANAGER: Initialized")
        println("   - Observing StateFlow from repository for real-time updates")
        println("   - Repository instance: ${repository.hashCode()}")
        
        // Observe repository StateFlow and update Compose State
        viewModelScope.launch {
            repository.currentMetalRates.collect { rates ->
                _metalRates.value = rates
            }
        }
        
        viewModelScope.launch {
            repository.loading.collect { isLoading ->
                _loading.value = isLoading
            }
        }
        
        viewModelScope.launch {
            repository.error.collect { errorMessage ->
                _error.value = errorMessage
            }
        }
    }
    
    fun loadCurrentRates() {
        // No longer needed - current metal rates are automatically updated via StateFlow listener
        // This method is kept for backward compatibility but does nothing
        println("ℹ️ METAL RATES MANAGER: loadCurrentRates() called but current metal rates are now managed by repository listener")
    }
    
    fun updateGoldRates(rate24k: Double) {
        repository?.let { repo ->
            viewModelScope.launch {
                try {
                    val newGoldRates = GoldRates.calculateKaratRates(rate24k)
                    val success = repo.updateGoldRates(newGoldRates)
                    if (success) {
                        // Listener will automatically update currentMetalRates StateFlow - no manual refresh needed
                        println("✅ METAL RATES MANAGER: Gold rates updated")
                        println("   - Listener will automatically detect this change and update StateFlow")
                    } else {
                        println("❌ METAL RATES MANAGER: Failed to update gold rates")
                    }
                } catch (e: Exception) {
                    println("❌ METAL RATES MANAGER: Error updating gold rates: ${e.message}")
                }
            }
        }
    }
    
    fun updateSilverRates(rate999: Double) {
        repository?.let { repo ->
            viewModelScope.launch {
                try {
                    val newSilverRates = SilverRates.calculateSilverRates(rate999)
                    val success = repo.updateSilverRates(newSilverRates)
                    if (success) {
                        // Listener will automatically update currentMetalRates StateFlow - no manual refresh needed
                        println("✅ METAL RATES MANAGER: Silver rates updated")
                        println("   - Listener will automatically detect this change and update StateFlow")
                    } else {
                        println("❌ METAL RATES MANAGER: Failed to update silver rates")
                    }
                } catch (e: Exception) {
                    println("❌ METAL RATES MANAGER: Error updating silver rates: ${e.message}")
                }
            }
        }
    }
    
    fun getGoldRateForKarat(karat: Int): Double {
        return _metalRates.value.getGoldRateForKarat(karat)
    }
    
    fun getSilverRateForPurity(purity: Int): Double {
        return _metalRates.value.getSilverRateForPurity(purity)
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun cleanup() {
        viewModelScope.cancel()
    }
}

