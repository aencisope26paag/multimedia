package com.example.basic_layouts.data

import androidx.annotation.DrawableRes
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemCategory {
    ALIGN_BODY,
    FAVORITE_COLLECTION
}

@Entity(tableName = "soothe_items")
data class SootheItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @DrawableRes val imageRes: Int,
    val title: String,
    val category: ItemCategory
)