package com.example.gpssaiakera

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Lista de lugares guardados. Pulsar un lugar lo abre en la app de mapas;
 * "Borrar" lo elimina de Firestore.
 */
@Composable
fun SavedPlacesSection(
    places: List<SavedPlace>,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (places.isEmpty()) return
    val context = LocalContext.current

    Column(modifier.fillMaxWidth()) {
        Text("Lugares guardados", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        places.forEach { place ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable {
                                val uri = Uri.parse(
                                    "geo:${place.latitude},${place.longitude}" +
                                        "?q=${place.latitude},${place.longitude}(${Uri.encode(place.name)})"
                                )
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, "No hay ninguna app de mapas instalada", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Text(place.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${"%.5f".format(Locale.US, place.latitude)}, ${"%.5f".format(Locale.US, place.longitude)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = { onDelete(place.id) }) { Text("Borrar") }
                }
            }
        }
    }
}
