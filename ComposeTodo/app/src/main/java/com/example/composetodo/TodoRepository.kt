package com.example.composetodo

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TodoRepository {

    private val collection = Firebase.firestore.collection("todos")

    // READ: a Flow that emits a new list every time the data changes
    fun observeItems(): Flow<List<TodoItem>> = callbackFlow {
        val listener = collection
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects<TodoItem>() ?: emptyList())
            }
        // Called when nobody collects the Flow anymore: stop listening
        awaitClose { listener.remove() }
    }

    // CREATE
    fun add(title: String) {
        collection.add(TodoItem(title = title))
    }

    // UPDATE
    fun setDone(id: String, done: Boolean) {
        collection.document(id).update("done", done)
    }

    // DELETE
    fun delete(id: String) {
        collection.document(id).delete()
    }
}