package org.example.project.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Centralized singleton for managing metal rates across the entire application
 * This ensures all screens use the same rates and updates are propagated universally
 */
object MetalRatesManager {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Current metal rates state
    private val _metalRates = mutableStateOf(MetalRates())
    val metalRates: State<MetalRates> = _metalRates
    
    // Loading state
    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading
    
    // Error state
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error
    
    // Repository instance
    private var repository: MetalRatesRepository? = null
    
    fun initialize(repository: MetalRatesRepository) {
        this.repository = repository
        loadCurrentRates()
    }
    
    fun loadCurrentRates() {
        repository?.let { repo ->
            viewModelScope.launch {
                _loading.value = true
                try {
                    val rates = repo.getCurrentMetalRates()
                    _metalRates.value = rates
                    _error.value = null
                } catch (e: Exception) {
                    _error.value = "Failed to load metal rates: ${e.message}"
                } finally {
                    _loading.value = false
                }
            }
        }
    }
    
    fun updateGoldRates(rate24k: Double) {
        repository?.let { repo ->
            viewModelScope.launch {
                _loading.value = true
                try {
                    val newGoldRates = GoldRates.calculateKaratRates(rate24k)
                    val success = repo.updateGoldRates(newGoldRates)
                    if (success) {
                        _metalRates.value = _metalRates.value.copy(
                            goldRates = newGoldRates,
                            lastUpdated = System.currentTimeMillis()
                        )
                        _error.value = null
                    } else {
                        _error.value = "Failed to update gold rates"
                    }
                } catch (e: Exception) {
                    _error.value = "Failed to update gold rates: ${e.message}"
                } finally {
                    _loading.value = false
                }
            }
        }
    }
    
    fun updateSilverRates(rate999: Double) {
        repository?.let { repo ->
            viewModelScope.launch {
                _loading.value = true
                try {
                    val newSilverRates = SilverRates.calculateSilverRates(rate999)
                    val success = repo.updateSilverRates(newSilverRates)
                    if (success) {
                        _metalRates.value = _metalRates.value.copy(
                            silverRates = newSilverRates,
                            lastUpdated = System.currentTimeMillis()
                        )
                        _error.value = null
                    } else {
                        _error.value = "Failed to update silver rates"
                    }
                } catch (e: Exception) {
                    _error.value = "Failed to update silver rates: ${e.message}"
                } finally {
                    _loading.value = false
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

