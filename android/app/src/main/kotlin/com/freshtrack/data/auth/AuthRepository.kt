package com.freshtrack.data.auth

import android.util.Log
import com.freshtrack.data.prefs.AppPreferences
import com.freshtrack.data.supabase.HouseholdRow
import com.freshtrack.data.supabase.MemberRow
import com.freshtrack.domain.model.Household
import com.freshtrack.domain.model.Member
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    object Loading : AuthState()
    object NotAuthenticated : AuthState()
    object Guest : AuthState()
    data class Authenticated(
        val userId: String,
        val email: String,
        val displayName: String?,
        val household: Household?,
        val members: List<Member>
    ) : AuthState()
}

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val prefs: AppPreferences
) {
    private val refreshToken = MutableStateFlow(0)

    val sessionStatus: Flow<SessionStatus> get() = supabase.auth.sessionStatus

    val authState: Flow<AuthState> = combine(
        supabase.auth.sessionStatus,
        prefs.isGuestMode,
        refreshToken
    ) { session, isGuest, _ ->
        when {
            isGuest -> AuthState.Guest
            session is SessionStatus.Authenticated -> {
                val user = session.session.user
                val userId = user?.id ?: return@combine AuthState.NotAuthenticated
                val email = user.email ?: ""
                val displayName = user.userMetadata?.get("display_name")
                    ?.toString()?.trim('"')
                try {
                    val (household, members) = loadHouseholdData(userId)
                    AuthState.Authenticated(userId, email, displayName, household, members)
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Échec chargement foyer pour $userId", e)
                    AuthState.Authenticated(userId, email, displayName, null, emptyList())
                }
            }
            session is SessionStatus.NotAuthenticated -> AuthState.NotAuthenticated
            else -> AuthState.Loading
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            this.data = buildJsonObject { put("display_name", displayName) }
        }
    }

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
        prefs.setGuestMode(false)
    }

    suspend fun setGuestMode() {
        prefs.setGuestMode(true)
    }

    suspend fun disableGuestMode() {
        prefs.setGuestMode(false)
    }

    fun currentUserId(): String? {
        val session = supabase.auth.currentSessionOrNull() ?: return null
        return session.user?.id
    }

    private suspend fun loadHouseholdData(userId: String): Pair<Household?, List<Member>> {
        val memberRows = supabase.from("household_members")
            .select { filter { eq("user_id", userId) } }
            .decodeList<MemberRow>()

        val householdId = memberRows.firstOrNull()?.householdId ?: return null to emptyList()

        val householdRows = supabase.from("households")
            .select { filter { eq("id", householdId) } }
            .decodeList<HouseholdRow>()

        val household = householdRows.firstOrNull()?.let {
            Household(it.id, it.name, it.inviteCode, it.createdBy, Instant.parse(it.createdAt))
        }

        val allMembers = supabase.from("household_members")
            .select { filter { eq("household_id", householdId) } }
            .decodeList<MemberRow>()
            .map { r ->
                Member(r.id, r.householdId, r.userId, r.displayName, r.avatarUrl, Instant.parse(r.joinedAt))
            }

        return household to allMembers
    }

    suspend fun refreshHousehold(): Pair<Household?, List<Member>> {
        val userId = currentUserId() ?: return null to emptyList()
        val data = loadHouseholdData(userId)
        refreshToken.update { it + 1 }
        return data
    }
}
