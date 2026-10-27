package com.example.gpssaiakera

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.navigationBarsPadding

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocationScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationScreen(viewModel: LocationViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Pedimos precisa + aproximada: en Android 12+ el usuario puede elegir solo "aproximada"
    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    val hasPermission = permissions.permissions.any { it.status.isGranted }

    // Al abrir la app sin permisos, se piden automáticamente
    LaunchedEffect(Unit) {
        if (!hasPermission) permissions.launchMultiplePermissionRequest()
    }

    // En cuanto hay permiso, se obtiene la ubicación y el mapa hace el zoom
    LaunchedEffect(hasPermission) {
        if (hasPermission && viewModel.uiState.value.location == null) {
            viewModel.fetchCurrentLocation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()   // <- nuevo, va DESPUÉS del scroll
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mapamundi: mundo entero al inicio y zoom al continente cuando llega la ubicación
        WorldMapView(location = state.location)

        Spacer(Modifier.height(16.dp))

        if (!hasPermission) {
            Text("Esta aplicación necesita acceso a la ubicación para funcionar.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { permissions.launchMultiplePermissionRequest() }) {
                Text("Conceder permiso")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            }) {
                Text("Abrir ajustes de la app")
            }
            return@Column
        }

        val location = state.location

        if (location != null) {
            LocationCard(location, state.address)
        } else if (state.isLoading) {
            Text("Buscando tu ubicación…")
        } else if (state.error == null) {
            Text("Pulsa el botón para obtener tu ubicación")
        }

        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = viewModel::fetchCurrentLocation,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
            }
            Text("Obtener ubicación")
        }

        if (location != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { openInMaps(context, location) }, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir en el mapa")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { copyCoordinates(context, location) }, modifier = Modifier.fillMaxWidth()) {
                Text("Copiar coordenadas")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { shareLocation(context, location) }, modifier = Modifier.fillMaxWidth()) {
                Text("Compartir ubicación")
            }
        }
    }
}

@Composable
private fun LocationCard(location: Location, address: String?) {
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(location.time))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            address?.let {
                Text(it, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
            }
            Text("Latitud: ${"%.6f".format(Locale.US, location.latitude)}")
            Text("Longitud: ${"%.6f".format(Locale.US, location.longitude)}")
            if (location.hasAltitude()) Text("Altitud: ${"%.1f".format(location.altitude)} m")
            if (location.hasAccuracy()) Text("Precisión: ±${"%.0f".format(location.accuracy)} m")
            if (location.hasSpeed()) Text("Velocidad: ${"%.1f".format(location.speed * 3.6f)} km/h")
            if (location.hasBearing()) Text("Rumbo: ${"%.0f".format(location.bearing)}° ${compass(location.bearing)}")
            Text("Actualizado: $time", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun compass(bearing: Float): String {
    val dirs = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    return dirs[((bearing + 22.5f) / 45f).toInt() % 8]
}

private fun coordsText(l: Location) =
    "${"%.6f".format(Locale.US, l.latitude)}, ${"%.6f".format(Locale.US, l.longitude)}"

private fun mapsUrl(l: Location) =
    "https://www.google.com/maps/search/?api=1&query=${l.latitude},${l.longitude}"

private fun openInMaps(context: Context, l: Location) {
    val uri = Uri.parse("geo:${l.latitude},${l.longitude}?q=${l.latitude},${l.longitude}(Mi ubicación)")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No hay ninguna app de mapas instalada", Toast.LENGTH_SHORT).show()
    }
}

private fun copyCoordinates(context: Context, l: Location) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Coordenadas", coordsText(l)))
    Toast.makeText(context, "Coordenadas copiadas", Toast.LENGTH_SHORT).show()
}

private fun shareLocation(context: Context, l: Location) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Estoy aquí: ${coordsText(l)}\n${mapsUrl(l)}")
    }
    context.startActivity(Intent.createChooser(intent, "Compartir ubicación"))
}