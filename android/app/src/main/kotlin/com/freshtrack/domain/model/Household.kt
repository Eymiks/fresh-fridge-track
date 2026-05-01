package com.freshtrack.domain.model

import kotlinx.datetime.Instant

data class Household(
    val id: String,
    val name: String,
    val inviteCode: String,
    val createdBy: String,
    val createdAt: Instant
)
