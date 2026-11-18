package com.example.composetodo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class TodoViewModel(
    private val repository: TodoRepository = TodoRepository()
) : ViewModel() {

    val items: StateFlow<List<TodoItem>> = repository.observeItems()
        .catch { emit(emptyList()) }   // e.g. permission denied
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun add(title: String) = repository.add(title)
    fun setDone(item: TodoItem, done: Boolean) = repository.setDone(item.id, done)
    fun delete(item: TodoItem) = repository.delete(item.id)
}