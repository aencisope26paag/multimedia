package com.example.composetodo

import com.google.firebase.firestore.DocumentId

data class TodoItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)