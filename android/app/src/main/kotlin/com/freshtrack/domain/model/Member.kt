package com.freshtrack.domain.model

import kotlinx.datetime.Instant

data class Member(
    val id: String,
    val householdId: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val joinedAt: Instant
)
