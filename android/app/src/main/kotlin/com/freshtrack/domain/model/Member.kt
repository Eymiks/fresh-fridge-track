package com.freshtrack.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Instant

@Immutable
data class Member(
    val id: String,
    val householdId: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val joinedAt: Instant
)
