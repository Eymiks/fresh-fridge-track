package com.freshtrack.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Instant

@Immutable
data class Household(
    val id: String,
    val name: String,
    val inviteCode: String,
    val createdBy: String,
    val createdAt: Instant
)
