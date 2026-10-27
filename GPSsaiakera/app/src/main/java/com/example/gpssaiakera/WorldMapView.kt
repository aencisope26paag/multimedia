package com.example.gpssaiakera

import android.location.Location
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

// ---------------------------------------------------------------------------
// Ajustes rápidos del mapa
// ---------------------------------------------------------------------------
private const val ZOOM_SPAN_DEG = 60f          // grados de longitud visibles tras el zoom (menor = más cerca)
private const val ZOOM_DURATION_MS = 2500      // duración de la animación de acercamiento
private val OCEAN = Color(0xFF3F9BD6)
private val LAND = Color(0xFF7CC576)
private val LAND_BORDER = Color(0xFF3F8F46)
private val GRID = Color.White.copy(alpha = 0.18f)

// ---------------------------------------------------------------------------
// Datos: polígonos MUY simplificados en "lon,lat" (grados)
// ---------------------------------------------------------------------------

/** Proyección Mercator: devuelve la Y "en grados" para que X e Y tengan la misma escala. */
private fun mercY(lat: Double): Float {
    val l = lat.coerceIn(-85.0, 85.0) * PI / 180.0
    return (ln(tan(PI / 4 + l / 2)) * 180.0 / PI).toFloat()
}

/** Convierte "lon,lat lon,lat ..." en puntos (x = lon, y = Mercator). */
private fun poly(data: String): List<Offset> =
    data.trim().split(Regex("\\s+")).map {
        val (lon, lat) = it.split(",")
        Offset(lon.toFloat(), mercY(lat.toDouble()))
    }

private val landmasses: List<List<Offset>> = listOf(
    // América del Norte + Central
    poly("-166,68.8 -162,70 -156,71.3 -150,70.5 -141,69.6 -135,69 -130,70 -125,70 -115,68.5 -110,68 -104,68 -95,68 -90,69 -85,68 -82,66 -87,64 -94,61 -94,58.7 -92,57 -87,56 -82.2,55 -82,52.9 -79.5,51.5 -79,56 -77,60 -78,62 -74,62.3 -70,61 -66,58.5 -62,57 -57,53 -56,51.5 -60,50 -66,50 -70,47 -65,48.3 -64.5,46.5 -61,45.5 -66,44 -70,43.5 -74,40.5 -75.5,38 -76,35 -78.5,33.8 -81,31.5 -80,27 -80.3,25.2 -81.8,26 -82.8,28 -84,30 -87,30.4 -89.5,30 -90.5,29 -94,29.5 -97,27.5 -97.5,25 -97.5,22 -96,19 -94,18.2 -91,18.7 -90.5,21 -87,21.5 -87.5,18.5 -88.5,16 -84,15.8 -83.2,12 -83.5,10.5 -81.5,9 -79.5,9.5 -77.5,8.5 -78,7.5 -80,7.3 -82.5,8.3 -85.5,9.9 -87.5,13 -91.5,14 -94,16 -97,15.8 -101,17.3 -105.3,20 -105.5,23 -109.5,26 -112.5,29.5 -114.8,31.7 -117.1,32.5 -118.5,34 -120.6,34.6 -122.5,37.5 -124.2,40.3 -124,43 -124.1,46.3 -124.7,48.4 -123,49 -127,50.5 -130,54 -133,57 -136,58.5 -140,59.8 -146,60.7 -150,59.5 -152,58 -155,57 -158,56.4 -162,55 -164.5,54.6 -162,56.2 -158,58.7 -162,59.6 -165,60.8 -165,62.6 -161,64.5 -166,64.6 -168,65.7 -164,66.6 -162,68"),
    // América del Sur
    poly("-77.5,8 -72,12 -64,10.5 -60,8.5 -52,5 -50,0 -44,-2.5 -35,-5.5 -35,-9 -39,-14 -39,-18 -41,-22 -48,-25.5 -48.5,-28 -52.5,-33 -58,-34.5 -57,-38 -62,-39 -65,-41 -63.5,-42.5 -67.5,-46 -66,-48 -69,-51 -68.5,-53.5 -72,-54 -75,-50 -73.5,-44 -73.5,-37 -71.5,-30 -70.5,-22 -70.3,-18 -76,-14 -81,-6 -80.5,-3 -80,0 -79,2 -77.5,4"),
    // África
    poly("-17,21 -13,27.5 -10,30 -9.5,32 -6,35.8 -2,35 3,36.8 10,37.2 11,33.5 15,32.3 20,32 20,30.5 25,31.8 29,30.9 32,31.2 32.6,29.9 34,27 35.5,23.5 37.2,19 39,15.6 41,14 43.2,12.6 44,10.5 51.2,11.8 51,10.5 49,6 46,2 42,-1 40,-3 39,-6 39.5,-9 40.5,-14 37,-18 35,-20 35,-24 33,-26 32.5,-28.5 30,-31.5 27,-33.5 22,-34 18.5,-34.2 18,-31 16.5,-28.5 15,-26 14.5,-22.5 13.5,-17 12,-13 13.5,-9 12,-5.5 9,-1 9.5,3.5 6,4.3 4,6.3 -2,5 -8,4.5 -13,8 -15,11 -17,14.7 -16.5,19"),
    // Eurasia (Europa + Asia + Arabia), empezando en Gibraltar
    poly("-5.4,36.1 -2,36.8 0,38.7 0.8,40.8 3.2,41.8 3.2,43.3 6,43.1 8,43.9 9,44.3 10,44 12,43 13,42 15.5,40 15.7,38 17.2,39 17.1,40.3 18.5,40.1 16.5,41.2 14,42.5 12.5,44 12.3,45.3 13.7,45.6 14.5,44.8 15.5,43.8 17.5,42.8 19.4,41.9 19.5,40.4 21,38.5 22,37 23,36.5 24,38 22.8,40 24,40.6 26,40.8 26.3,40 27,38.5 28,36.7 30,36.3 32.5,36.1 36,36.6 35.8,35.5 35.1,33.5 34.5,31.8 34.2,31.3 32.5,31.1 32.6,29.9 34.2,27.8 35,28 36.6,26 39,22 41,19 42.7,16.5 43.3,13 45,12.8 48,14 52,16 55,17.5 57.5,19.5 59.8,22.5 58.5,24 56.5,26.4 57,27 60,25.3 62,25.2 66.5,25.4 68.2,23.8 69,22.3 70.5,20.8 72.5,21.2 72.8,19 73.5,16 74.8,13 76.3,9.5 77.5,8 78.5,9 79.8,10.3 80,13.5 80.3,15.5 82,17 85,19.7 87,21.5 89,21.8 91.5,22.5 92.5,20.5 94,18.5 94.5,16 97.5,16.5 98.5,13 98.3,10 98.5,8 100.3,6 101.5,3.5 103.5,1.4 104,2.5 103.3,5 102.2,6.2 100.8,7.2 100,9 99.2,10.5 100,12.5 100.9,13.3 102.5,12 103,11 104.5,10.4 105,8.7 106.8,10.4 109,11.5 109.3,13 108.8,15.3 107,17.5 105.8,19 106.7,20.5 108,21.5 110,21 111,21.5 113,22.2 116,22.8 118,24.5 120,26.5 121.8,29.5 121.9,31 120.8,32.5 120.5,34 119,35 120.5,36.3 122.5,37 120.5,37.8 118.5,37.2 117.7,38.8 121,40.6 122.2,40.4 124.5,39.8 125,37.5 126.5,36.5 126.5,34.5 128.5,34.8 129.5,35.2 129.3,37 128,38.6 127.5,39.7 129.7,41 131,42.5 133,42.8 135.5,43.7 138,46 140.5,48.5 140.7,50 140.5,53 137,54.5 143,59 155,59.5 156,57 155.7,53 156.7,51 158.5,52.5 162,56 163.5,59.7 167,60 170,60.3 180,65 180,68.5 170,69.8 160,69.5 150,71.3 140,72.5 130,71 127,73.5 113,73.5 112,76 105,77.5 100,76.5 95,76 88,75.2 80,72.5 73,72.5 69,73 68,69 60,69.5 55,68.3 44,68.3 41,66.3 36,66.2 34,66.7 32,69.2 28.5,71 25,71 19,70 15,68.5 12,65.5 10,63.5 5.2,62 5,60 6,58.2 8,58.1 8.2,57 8.1,55.5 8.6,54.8 8.5,53.7 4.7,52.9 4.5,52.2 3.6,51.4 2.5,51.1 1.6,50.8 1.5,49.9 0,49.6 -1.5,49.7 -1.5,48.7 -3.5,48.8 -4.7,48.4 -4.3,47.8 -2.5,47.3 -1.2,46 -1.3,44.6 -1.8,43.4 -4,43.4 -8,43.7 -9.3,43 -8.8,41.5 -9.5,38.8 -8.9,37.1 -7.4,37.1 -6.3,36.5 -5.6,36"),
    // Groenlandia
    poly("-73,78 -60,82 -30,83.5 -18,80 -20,75 -22,70 -33,68 -42,60 -49,61 -53,67 -58,75"),
    // Australia
    poly("114,-22 114.5,-26 115,-34 118,-35 123.5,-34 129,-31.6 131,-31.5 135,-34.8 138,-35.5 138.5,-34 140,-37.5 144,-38.3 146.5,-39 150,-37.5 153,-31 153.5,-28 153,-25 150,-22 146,-18.8 145.3,-15 143.5,-14 142.5,-10.8 141.5,-13.5 141.5,-17 139,-17.5 136,-15.5 137,-12 135,-12 132.5,-11.5 130,-12.3 129,-15 126,-14 124,-17 122,-18 118,-20.3"),
    // Islas
    poly("-5.7,50 -3,50.5 1.3,51.1 1.7,52.7 0.2,53.5 -1.5,55 -2,56 -2.2,57.6 -4,57.7 -3.1,58.6 -5,58.6 -6,57 -5.6,55.5 -4.8,54.7 -3.1,54.8 -3.2,53.4 -4.5,53.3 -4.2,52.2 -5.2,51.7 -3,51.3 -4.5,51.2"), // Gran Bretaña
    poly("-6,52.2 -6.1,53.9 -5.5,54.6 -7.3,55.3 -8.7,54.4 -10,54 -10.3,52 -9.5,51.5 -8,51.7"), // Irlanda
    poly("-24,65.5 -22,66.4 -16,66.5 -13.5,65 -15,64.2 -19,63.4 -22.5,63.8"), // Islandia
    poly("-89,73 -80,73.8 -72,71.5 -62,67 -64,63.5 -72,64.5 -78,65 -74,68 -85,69.5"), // Baffin
    poly("-118,72 -105,73.2 -100,70.5 -105,69 -115,69.5"), // Victoria
    poly("-90,76.5 -75,78.5 -62,82 -80,83 -90,81"), // Ellesmere
    poly("-59,47.7 -56,51.6 -52.8,47.5 -55.5,46.8"), // Terranova
    poly("-84.9,21.9 -82,23.1 -79,22.5 -74.2,20.2 -77.5,19.9 -80,21.7 -82.5,22.2"), // Cuba
    poly("-74.4,18.4 -72,19.8 -68.4,18.6 -71,17.8"), // La Española
    poly("12.5,38 15.6,38.2 15.1,36.7 12.5,37.6"), // Sicilia
    poly("8.2,41 9.7,41.1 9.6,39.2 8.5,39"), // Cerdeña
    poly("8.6,42.9 9.5,43 9.4,41.4 8.7,41.6"), // Córcega
    poly("2.4,39.6 3.4,39.8 3.2,39.3 2.6,39.4"), // Mallorca
    poly("23.5,35.3 26.3,35.2 26,35 23.7,35"), // Creta
    poly("32.3,35 34.5,35.6 33.8,34.7 32.5,34.7"), // Chipre
    poly("130.5,31.5 131.5,33 132,34 135,34 136.5,34.5 138.5,35 140,35 141,38 142,40 141.5,41.5 140,40.5 139.5,38 137,37 136,36 133.5,35.5 131,34.5 130.5,33.5"), // Japón (Honshu y sur)
    poly("140,42 141,45.5 145,44 143,42"), // Hokkaido
    poly("120.2,23 121.5,25.2 122,24.5 121,22"), // Taiwán
    poly("120,18.5 122,18.4 122.2,16 124,13 122,13.8 120.6,14 119.8,16"), // Luzón
    poly("122,7.5 124,8.5 126.5,7.5 125.5,5.8 123,7"), // Mindanao
    poly("95.3,5.6 98,4 103.5,-1 106,-3 105.8,-5.8 102,-4 98.8,0 95.5,3"), // Sumatra
    poly("105.2,-6.8 108.5,-6.5 114.5,-7.6 114.5,-8.7 106,-7.5"), // Java
    poly("109,1.5 111,2 114,4.5 117,7 119,5 117.5,1 116,-3.5 114,-4 110.5,-3 109.5,-1"), // Borneo
    poly("131,-1 135,-3.2 141,-2.6 145.5,-5.3 150.5,-10.5 147,-10 143,-9 141,-9.2 138,-8.2 138,-5.5 134,-4 132,-2.8"), // Nueva Guinea
    poly("144.7,-40.7 148.3,-40.9 147.9,-43.2 146,-43.5"), // Tasmania
    poly("172.7,-34.5 175.5,-37 178.5,-37.7 175.2,-41.6 174.5,-39.5 173.5,-35.5"), // Nueva Zelanda (norte)
    poly("172.7,-40.5 174.3,-41.7 171,-44.5 169,-46.6 166.5,-46 168,-44 171.5,-41.5"), // Nueva Zelanda (sur)
    poly("49.3,-12 50.5,-15.5 47.1,-25 43.5,-23 44.3,-16.5 46.5,-15.7"), // Madagascar
    poly("79.8,9.5 81.8,7.5 80.5,5.9 79.9,7.2"), // Sri Lanka
)

// Mares interiores / lagos: se dibujan encima de la tierra con el color del océano
private val inlandWaters: List<List<Offset>> = listOf(
    poly("11,54.3 14,53.9 19,54.4 21,56.5 21.5,57.5 23.5,57 24.5,58.5 23.5,59.4 28,59.6 30,60 26,60.4 22,60 21.2,61.5 21.5,63.5 25,65.1 24.5,65.7 22,65.7 19,63 17.3,61.5 17.8,60 18.8,59.4 17,58.6 16.5,57 15.8,56.2 14,55.4 12.8,55.7 12.5,55 11,54.8"), // Báltico
    poly("28,41.5 28.5,43.5 30,45.5 33,46 33.5,44.5 36.5,45.2 38,47 39.5,43.5 41.5,41.7 36,41.3 32,41.9 29.1,41.2"), // Mar Negro
    poly("47.5,45 50.5,46.8 53,46.5 53,44 51,43.2 52.8,41.5 53.8,38.5 54,37.2 50.5,36.8 49,38.5 49.8,40.4 48.5,41.8 47.3,43.5"), // Mar Caspio
    poly("56.3,26.4 51.5,24.3 50.6,26.2 48.3,29.8 50,30 52.5,28 54.5,26.6 56.5,27.2"), // Golfo Pérsico
)

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

/**
 * Mapamundi simplificado. Empieza mostrando todo el planeta y, cuando llega la
 * primera ubicación, hace un zoom animado hacia el continente del usuario.
 */
@Composable
fun WorldMapView(location: Location?, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    // Destino del zoom: x = longitud, y = latitud en Mercator
    var target by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(location != null) {
        if (location != null && target == null) {
            target = Offset(location.longitude.toFloat(), mercY(location.latitude))
            progress.animateTo(1f, tween(ZOOM_DURATION_MS, easing = FastOutSlowInEasing))
        }
    }

    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        val w = size.width
        val h = size.height

        // --- Cámara: de la vista mundial a la vista del continente ---
        val startScale = w / 360f
        val endScale = w / ZOOM_SPAN_DEG
        val startCx = 0f
        val startCy = mercY(25.0)

        val t = progress.value
        val tgt = target
        val scale = if (tgt != null) startScale * (endScale / startScale).pow(t) else startScale
        // Se desplaza el centro según el zoom para que el destino no se salga de la pantalla
        val k = (1f - startScale / scale) / (1f - startScale / endScale)
        val cx = if (tgt != null) startCx + (tgt.x - startCx) * k else startCx
        val cy = if (tgt != null) startCy + (tgt.y - startCy) * k else startCy

        fun sx(lon: Float) = (lon - cx) * scale + w / 2f
        fun sy(my: Float) = (cy - my) * scale + h / 2f

        fun toPath(points: List<Offset>): Path = Path().apply {
            points.forEachIndexed { i, p ->
                if (i == 0) moveTo(sx(p.x), sy(p.y)) else lineTo(sx(p.x), sy(p.y))
            }
            close()
        }

        // Océano
        drawRect(OCEAN)

        // Cuadrícula (meridianos y paralelos cada 15°)
        for (lon in -180..180 step 15) {
            val x = sx(lon.toFloat())
            drawLine(GRID, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }
        for (lat in -75..75 step 15) {
            val y = sy(mercY(lat.toDouble()))
            drawLine(GRID, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        // Continentes e islas
        val border = Stroke(width = 1.5.dp.toPx())
        landmasses.forEach { points ->
            val path = toPath(points)
            drawPath(path, LAND)
            drawPath(path, LAND_BORDER, style = border)
        }

        // Mares interiores
        inlandWaters.forEach { drawPath(toPath(it), OCEAN) }

        // Marcador de la ubicación con efecto de pulso
        if (location != null) {
            val mx = sx(location.longitude.toFloat())
            val my = sy(mercY(location.latitude))
            val p = pulse.value
            drawCircle(
                color = Color.Red.copy(alpha = 0.45f * (1f - p)),
                radius = 8.dp.toPx() + 18.dp.toPx() * p,
                center = Offset(mx, my)
            )
            drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(mx, my))
            drawCircle(Color(0xFFE53935), radius = 5.dp.toPx(), center = Offset(mx, my))
        }
    }
}