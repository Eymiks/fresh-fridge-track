package com.freshtrack.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ProductEntity::class], version = 2, exportSchema = true)
abstract class FreshTrackDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_products_household_id ON products(household_id)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_products_status ON products(status)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_products_expiration_date ON products(expiration_date)"
                )
            }
        }
    }
}
