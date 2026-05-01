package com.freshtrack.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE household_id = :householdId ORDER BY expiration_date ASC")
    fun observeByHousehold(householdId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id AND household_id = :householdId")
    fun observeByIdForHousehold(id: String, householdId: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE household_id = :householdId")
    suspend fun getByHousehold(householdId: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM products WHERE id = :id AND household_id = :householdId")
    suspend fun deleteByIdForHousehold(id: String, householdId: String)

    @Query("DELETE FROM products WHERE household_id = :householdId")
    suspend fun deleteByHousehold(householdId: String)

    @Transaction
    suspend fun replaceForHousehold(householdId: String, products: List<ProductEntity>) {
        deleteByHousehold(householdId)
        if (products.isNotEmpty()) {
            insertAll(products)
        }
    }
}
