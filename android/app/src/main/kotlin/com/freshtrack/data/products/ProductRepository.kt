package com.freshtrack.data.products

import android.util.Log
import com.freshtrack.data.db.ProductDao
import com.freshtrack.data.supabase.ProductRow
import com.freshtrack.domain.model.Member
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val productDao: ProductDao
) {
    companion object {
        private const val GUEST_HOUSEHOLD_ID = "guest-local"
        private const val GUEST_USER_ID = "guest"
        private const val GUEST_DISPLAY_NAME = "Invité"
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun observeProducts(householdId: String, members: List<Member> = emptyList()): Flow<List<Product>> =
        productDao.observeByHousehold(householdId).map { entities ->
            entities.map { it.toDomain(members) }
        }

    fun observeProduct(householdId: String, productId: String, members: List<Member> = emptyList()): Flow<Product?> =
        productDao.observeByIdForHousehold(productId, householdId).map { entity ->
            entity?.toDomain(members)
        }

    fun observeGuestProducts(): Flow<List<Product>> =
        productDao.observeByHousehold(GUEST_HOUSEHOLD_ID).map { entities ->
            entities.map { it.toDomain().copy(addedByName = GUEST_DISPLAY_NAME) }
        }

    fun observeGuestProduct(productId: String): Flow<Product?> =
        productDao.observeByIdForHousehold(productId, GUEST_HOUSEHOLD_ID).map { entity ->
            entity?.toDomain()?.copy(addedByName = GUEST_DISPLAY_NAME)
        }

    suspend fun importGuestProductsToHousehold(householdId: String, userId: String): Int {
        val guestProducts = productDao.getByHousehold(GUEST_HOUSEHOLD_ID)
            .map { it.toDomain().copy(id = "", addedBy = userId, addedByName = null) }

        if (guestProducts.isEmpty()) return 0

        val rows = guestProducts.map { it.toInsertRow(householdId, userId) }
        val inserted = supabase.from("products").insert(rows) { select() }
            .decodeList<ProductRow>()

        productDao.insertAll(inserted.map { it.toEntity() })
        clearGuestProducts()
        return inserted.size
    }

    suspend fun clearGuestProducts() {
        productDao.deleteByHousehold(GUEST_HOUSEHOLD_ID)
    }

    suspend fun fetchAndCache(householdId: String) {
        val rows = supabase.from("products")
            .select { filter { eq("household_id", householdId) } }
            .decodeList<ProductRow>()
        productDao.replaceForHousehold(householdId, rows.map { it.toEntity() })
    }

    suspend fun findExistingImageForBarcode(householdId: String, barcode: String): String? =
        productDao.getByHousehold(householdId)
            .firstOrNull { it.barcode == barcode && !it.imageUrl.isNullOrBlank() }
            ?.imageUrl

    suspend fun getProductsSnapshot(
        householdId: String,
        members: List<Member> = emptyList()
    ): List<Product> =
        productDao.getByHousehold(householdId).map { it.toDomain(members) }

    suspend fun getGuestProductsSnapshot(): List<Product> =
        productDao.getByHousehold(GUEST_HOUSEHOLD_ID).map {
            it.toDomain().copy(addedByName = GUEST_DISPLAY_NAME)
        }

    suspend fun addProduct(product: Product, householdId: String, userId: String): Product {
        val row = product.toInsertRow(householdId, userId)
        val inserted = supabase.from("products").insert(row) { select() }
            .decodeSingle<ProductRow>()
        productDao.upsert(inserted.toEntity())
        return inserted.toEntity().toDomain()
    }

    suspend fun updateProduct(product: Product, householdId: String) {
        val row = product.toUpdateJsonObject()
        supabase.from("products").update(row) { filter { eq("id", product.id) } }
        productDao.upsert(product.toEntity(householdId))
    }

    suspend fun removeProduct(productId: String) {
        supabase.from("products").delete { filter { eq("id", productId) } }
        productDao.deleteById(productId)
    }

    suspend fun addGuestProduct(product: Product): Product {
        val localProduct = product.copy(
            id = UUID.randomUUID().toString(),
            addedBy = GUEST_USER_ID,
            addedByName = GUEST_DISPLAY_NAME
        )
        productDao.upsert(localProduct.toEntity(GUEST_HOUSEHOLD_ID))
        return localProduct
    }

    suspend fun updateGuestProduct(product: Product) {
        productDao.upsert(
            product.copy(addedBy = GUEST_USER_ID, addedByName = GUEST_DISPLAY_NAME)
                .toEntity(GUEST_HOUSEHOLD_ID)
        )
    }

    suspend fun removeGuestProduct(productId: String) {
        productDao.deleteByIdForHousehold(productId, GUEST_HOUSEHOLD_ID)
    }

    suspend fun setStatus(product: Product, householdId: String, status: ProductStatus) {
        val now = Clock.System.now()
        val updated = when (status) {
            ProductStatus.ACTIVE -> product.copy(
                status = status,
                statusChangedAt = now,
                openedAt = null,
                daysAfterOpening = null
            )
            ProductStatus.OPENED -> product.copy(status = status, openedAt = now, statusChangedAt = now)
            else -> product.copy(status = status, statusChangedAt = now)
        }
        updateProduct(updated, householdId)
    }

    suspend fun setGuestStatus(product: Product, status: ProductStatus) {
        val now = Clock.System.now()
        val updated = when (status) {
            ProductStatus.ACTIVE -> product.copy(
                status = status,
                statusChangedAt = now,
                openedAt = null,
                daysAfterOpening = null
            )
            ProductStatus.OPENED -> product.copy(status = status, openedAt = now, statusChangedAt = now)
            else -> product.copy(status = status, statusChangedAt = now)
        }
        updateGuestProduct(updated)
    }

    suspend fun uploadImage(householdId: String, productId: String, bytes: ByteArray, ext: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val path = "$householdId/${productId}_$timestamp.$ext"
        supabase.storage.from("product-images").upload(path, bytes)
        return supabase.storage.from("product-images").publicUrl(path)
    }

    fun subscribeToRealtime(householdId: String): Flow<Unit> = callbackFlow {
        val instanceId = UUID.randomUUID().toString()
        val channel = supabase.channel("products:$householdId:$instanceId")

        val realtimeJob = launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "products"
                filter("household_id", FilterOperator.EQ, householdId)
            }.collect { action ->
                try {
                    when (action) {
                        is PostgresAction.Insert -> {
                            val row = json.decodeFromJsonElement<ProductRow>(action.record)
                            if (row.householdId == householdId) {
                                productDao.upsert(row.toEntity())
                            }
                        }
                        is PostgresAction.Update -> {
                            val row = json.decodeFromJsonElement<ProductRow>(action.record)
                            if (row.householdId == householdId) {
                                productDao.upsert(row.toEntity())
                            }
                        }
                        is PostgresAction.Delete -> {
                            val oldHouseholdId = action.oldRecord["household_id"]?.jsonPrimitive?.contentOrNull
                            if (oldHouseholdId == null || oldHouseholdId == householdId) {
                                val id = action.oldRecord["id"]?.jsonPrimitive?.contentOrNull
                                id?.let { productDao.deleteByIdForHousehold(it, householdId) }
                            }
                        }
                        else -> {}
                    }
                    trySend(Unit)
                } catch (e: Exception) {
                    Log.w("ProductRepo", "Realtime update ignoré", e)
                }
            }
        }

        channel.subscribe()
        supabase.realtime.connect()

        awaitClose {
            realtimeJob.cancel()
            launch { runCatching { channel.unsubscribe(); supabase.realtime.disconnect() } }
        }
    }
}
