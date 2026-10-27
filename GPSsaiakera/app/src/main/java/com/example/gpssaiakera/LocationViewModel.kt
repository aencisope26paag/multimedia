package com.example.gpssaiakera

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationUiState(
    val location: Location? = null,
    val address: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class LocationViewModel(app: Application) : AndroidViewModel(app) {

    private val locationUtils = LocationUtils(app)

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()
    private var lastGeocoded: Location? = null

    private fun ensureReady(): Boolean {
        val error = when {
            !locationUtils.hasLocationPermission() -> "Permisos de ubicación no otorgados."
            !locationUtils.isLocationEnabled() -> "El GPS está desactivado. Actívalo en los ajustes del dispositivo."
            else -> null
        }
        if (error != null) _uiState.update { it.copy(error = error) }
        return error == null
    }

    /** Una sola lectura. */
    fun fetchCurrentLocation() {
        if (!ensureReady()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val location = locationUtils.getCurrentLocation()
                if (location != null) {
                    publish(location)
                } else {
                    _uiState.update { it.copy(error = "No se pudo obtener la ubicación. Inténtalo de nuevo en exteriores.") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Error al consultar la ubicación.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun publish(location: Location) {
        _uiState.update { it.copy(location = location, error = null) }
        // Solo se vuelve a geocodificar si nos hemos movido más de 50 m
        val last = lastGeocoded
        if (last == null || last.distanceTo(location) > 50f) {
            lastGeocoded = location
            val address = locationUtils.getAddress(location)
            _uiState.update { it.copy(address = address ?: it.address) }
        }
    }
}