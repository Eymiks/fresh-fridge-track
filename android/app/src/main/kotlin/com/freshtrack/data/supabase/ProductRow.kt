package com.freshtrack.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductRow(
    val id: String = "",
    @SerialName("household_id") val householdId: String = "",
    val name: String = "",
    val barcode: String? = null,
    @SerialName("expiration_date") val expirationDate: String = "",
    @SerialName("added_at") val addedAt: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    val brand: String? = null,
    @SerialName("nutri_score") val nutriScore: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val status: String = "active",
    @SerialName("status_changed_at") val statusChangedAt: String? = null,
    val quantity: String? = null,
    @SerialName("nova_group") val novaGroup: Int? = null,
    @SerialName("eco_score") val ecoScore: String? = null,
    val allergens: String? = null,
    val ingredients: String? = null,
    @SerialName("opened_at") val openedAt: String? = null,
    @SerialName("days_after_opening") val daysAfterOpening: Int? = null,
    @SerialName("added_by") val addedBy: String? = null,
    val notes: String? = null,
    @SerialName("frozen_until") val frozenUntil: String? = null,
    @SerialName("nutrition_data") val nutritionData: String? = null
)

@Serializable
data class ProductInsertRow(
    @SerialName("household_id") val householdId: String,
    val name: String,
    val barcode: String? = null,
    @SerialName("expiration_date") val expirationDate: String,
    @SerialName("added_at") val addedAt: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val brand: String? = null,
    @SerialName("nutri_score") val nutriScore: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val status: String = "active",
    @SerialName("status_changed_at") val statusChangedAt: String? = null,
    val quantity: String? = null,
    @SerialName("nova_group") val novaGroup: Int? = null,
    @SerialName("eco_score") val ecoScore: String? = null,
    val allergens: String? = null,
    val ingredients: String? = null,
    @SerialName("opened_at") val openedAt: String? = null,
    @SerialName("days_after_opening") val daysAfterOpening: Int? = null,
    @SerialName("added_by") val addedBy: String? = null,
    val notes: String? = null,
    @SerialName("frozen_until") val frozenUntil: String? = null,
    @SerialName("nutrition_data") val nutritionData: String? = null
)

@Serializable
data class ProductUpdateRow(
    val name: String,
    val barcode: String? = null,
    @SerialName("expiration_date") val expirationDate: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val brand: String? = null,
    @SerialName("nutri_score") val nutriScore: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val status: String = "active",
    @SerialName("status_changed_at") val statusChangedAt: String? = null,
    val quantity: String? = null,
    @SerialName("nova_group") val novaGroup: Int? = null,
    @SerialName("eco_score") val ecoScore: String? = null,
    val allergens: String? = null,
    val ingredients: String? = null,
    @SerialName("opened_at") val openedAt: String? = null,
    @SerialName("days_after_opening") val daysAfterOpening: Int? = null,
    val notes: String? = null,
    @SerialName("frozen_until") val frozenUntil: String? = null,
    @SerialName("nutrition_data") val nutritionData: String? = null
)

@Serializable
data class HouseholdRow(
    val id: String = "",
    val name: String = "",
    @SerialName("invite_code") val inviteCode: String = "",
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class HouseholdInsertRow(
    val name: String,
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class MemberRow(
    val id: String = "",
    @SerialName("household_id") val householdId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("joined_at") val joinedAt: String = ""
)

@Serializable
data class MemberInsertRow(
    @SerialName("household_id") val householdId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String
)
