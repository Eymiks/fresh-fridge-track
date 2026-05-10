package com.freshtrack.data.household

import com.freshtrack.data.supabase.HouseholdInsertRow
import com.freshtrack.data.supabase.HouseholdRow
import com.freshtrack.data.supabase.MemberInsertRow
import com.freshtrack.data.supabase.MemberRow
import com.freshtrack.domain.model.Household
import com.freshtrack.domain.model.Member
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseholdRepository @Inject constructor(private val supabase: SupabaseClient) {

    suspend fun createHousehold(name: String, userId: String, displayName: String): Household {
        val row = HouseholdInsertRow(name = name, createdBy = userId)
        val inserted = supabase.from("households").insert(row) { select() }
            .decodeSingle<HouseholdRow>()

        supabase.from("household_members").insert(
            MemberInsertRow(
                householdId = inserted.id, userId = userId,
                displayName = displayName
            )
        )
        return Household(inserted.id, inserted.name, inserted.inviteCode, inserted.createdBy,
            Instant.parse(inserted.createdAt))
    }

    suspend fun joinByCode(code: String, displayName: String) {
        supabase.postgrest.rpc("join_household_by_code", buildJsonObject {
            put("p_invite_code", code.uppercase())
            put("p_display_name", displayName)
        })
    }

    suspend fun updateDisplayName(memberId: String, displayName: String) {
        supabase.from("household_members")
            .update(mapOf("display_name" to displayName)) {
                filter { eq("id", memberId) }
            }
    }

    suspend fun updateHouseholdName(householdId: String, name: String) {
        supabase.from("households")
            .update(mapOf("name" to name)) {
                filter { eq("id", householdId) }
            }
    }

    suspend fun removeMember(memberId: String) {
        supabase.from("household_members").delete { filter { eq("id", memberId) } }
    }

    suspend fun uploadAvatar(householdId: String, userId: String, bytes: ByteArray, ext: String): MemberRow {
        val safeExt = ext.lowercase().takeIf { it in setOf("jpg", "jpeg", "png", "webp") } ?: "jpg"
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val path = "$householdId/avatar_${userId}_$timestamp.$safeExt"
        val bucket = supabase.storage.from("product-images")
        bucket.upload(path, bytes)
        val publicUrl = bucket.publicUrl(path)
        val updated = supabase.from("household_members")
            .update(mapOf("avatar_url" to publicUrl)) {
                select()
                filter {
                    eq("household_id", householdId)
                    eq("user_id", userId)
                }
            }
            .decodeList<MemberRow>()
        return updated.firstOrNull()
            ?: error("Avatar envoyé, mais aucun profil n'a été mis à jour.")
    }

    suspend fun getMembers(householdId: String): List<Member> =
        supabase.from("household_members")
            .select { filter { eq("household_id", householdId) } }
            .decodeList<MemberRow>()
            .map { Member(it.id, it.householdId, it.userId, it.displayName, it.avatarUrl, Instant.parse(it.joinedAt)) }
}
