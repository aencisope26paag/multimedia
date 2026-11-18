package com.example.composetodo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel = viewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var newTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My To-Dos") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("New task") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (newTitle.isNotBlank()) {
                        viewModel.add(newTitle.trim())
                        newTitle = ""
                    }
                }) { Text("Add") }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(items, key = { it.id }) { item ->
                    TodoRow(
                        item = item,
                        onCheckedChange = { viewModel.setDone(item, it) },
                        onDelete = { viewModel.delete(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun TodoRow(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.done, onCheckedChange = onCheckedChange)
        Text(
            text = item.title,
            modifier = Modifier.weight(1f),
            textDecoration = if (item.done) TextDecoration.LineThrough else null
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}