package com.freshtrack.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "household_id") val householdId: String,
    val name: String,
    val barcode: String? = null,
    @ColumnInfo(name = "expiration_date") val expirationDate: String,
    @ColumnInfo(name = "added_at") val addedAt: String,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    val brand: String? = null,
    @ColumnInfo(name = "nutri_score") val nutriScore: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val status: String = "active",
    @ColumnInfo(name = "status_changed_at") val statusChangedAt: String? = null,
    val quantity: String? = null,
    @ColumnInfo(name = "nova_group") val novaGroup: Int? = null,
    @ColumnInfo(name = "eco_score") val ecoScore: String? = null,
    val allergens: String? = null,
    val ingredients: String? = null,
    @ColumnInfo(name = "opened_at") val openedAt: String? = null,
    @ColumnInfo(name = "days_after_opening") val daysAfterOpening: Int? = null,
    @ColumnInfo(name = "added_by") val addedBy: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "frozen_until") val frozenUntil: String? = null,
    @ColumnInfo(name = "nutrition_data") val nutritionData: String? = null
)
