package com.example.gpssaiakera

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class SavedPlace(
    val id: String = "",
    val name: String = "",
    val address: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val savedAt: Long = 0L
)

/**
 * Guarda los lugares del usuario en Cloud Firestore:
 *   users/{uid}/places/{placeId}
 *
 * Cada instalación usa una cuenta anónima de Firebase Auth, así que cada
 * dispositivo solo ve sus propios lugares y no hace falta pantalla de login.
 */
class SavedPlacesRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun places(uid: String) =
        db.collection("users").document(uid).collection("places")

    /** Inicia sesión anónima si hace falta y devuelve el uid. */
    private fun withUser(onReady: (String) -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            onReady(user.uid)
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) onReady(uid) else onError("No se pudo iniciar sesión en Firebase.")
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "No se pudo iniciar sesión en Firebase.") }
    }

    /** Lista en tiempo real: se vuelve a emitir cada vez que cambia algo en Firestore. */
    fun observe(): Flow<List<SavedPlace>> = callbackFlow {
        var registration: ListenerRegistration? = null

        withUser(
            onReady = { uid ->
                registration = places(uid)
                    .orderBy("savedAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.documents?.map { it.toSavedPlace() } ?: emptyList())
                    }
            },
            onError = { close(Exception(it)) }
        )

        awaitClose { registration?.remove() }
    }

    fun save(place: SavedPlace, onError: (String) -> Unit) {
        withUser(
            onReady = { uid ->
                val data = hashMapOf(
                    "name" to place.name,
                    "address" to place.address,
                    "latitude" to place.latitude,
                    "longitude" to place.longitude,
                    "savedAt" to System.currentTimeMillis()
                )
                places(uid).add(data)
                    .addOnFailureListener { onError(it.localizedMessage ?: "No se pudo guardar el lugar.") }
            },
            onError = onError
        )
    }

    fun delete(id: String, onError: (String) -> Unit) {
        withUser(
            onReady = { uid ->
                places(uid).document(id).delete()
                    .addOnFailureListener { onError(it.localizedMessage ?: "No se pudo borrar el lugar.") }
            },
            onError = onError
        )
    }

    private fun DocumentSnapshot.toSavedPlace() = SavedPlace(
        id = id,
        name = getString("name") ?: "",
        address = getString("address"),
        latitude = getDouble("latitude") ?: 0.0,
        longitude = getDouble("longitude") ?: 0.0,
        savedAt = getLong("savedAt") ?: 0L
    )
}
