package com.lifeordered.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.data.models.TagModel
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentDao {
    @Query("SELECT * FROM moments WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllMomentsFlow(): Flow<List<Moment>>

    @Query("SELECT * FROM moments WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedMomentsFlow(): Flow<List<Moment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoment(moment: Moment): Long

    @Update
    suspend fun updateMoment(moment: Moment)

    @Delete
    suspend fun deleteMoment(moment: Moment)
}

@Dao
interface ConsumableDao {
    @Query("SELECT * FROM consumables WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllConsumablesFlow(): Flow<List<Consumable>>

    @Query("SELECT * FROM consumables WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedConsumablesFlow(): Flow<List<Consumable>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumable(consumable: Consumable): Long

    @Update
    suspend fun updateConsumable(consumable: Consumable)

    @Delete
    suspend fun deleteConsumable(consumable: Consumable)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventories WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllInventoriesFlow(): Flow<List<Inventory>>

    @Query("SELECT * FROM inventories WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedInventoriesFlow(): Flow<List<Inventory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: Inventory): Long

    @Update
    suspend fun updateInventory(inventory: Inventory)

    @Delete
    suspend fun deleteInventory(inventory: Inventory)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllTagsFlow(): Flow<List<TagModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagModel): Long

    @Delete
    suspend fun deleteTag(tag: TagModel)
}

@Database(
    entities = [Moment::class, Consumable::class, Inventory::class, TagModel::class],
    version = 4,
    exportSchema = false
)
abstract class LifeOrderedDatabase : RoomDatabase() {
    abstract fun momentDao(): MomentDao
    abstract fun consumableDao(): ConsumableDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: LifeOrderedDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE moments ADD COLUMN type TEXT NOT NULL DEFAULT 'countdown'")
                db.execSQL("ALTER TABLE moments ADD COLUMN notes TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE moments ADD COLUMN repeatType TEXT NOT NULL DEFAULT 'none'")
                db.execSQL("ALTER TABLE moments ADD COLUMN themeColorHex TEXT NOT NULL DEFAULT '#FF7E40'")
                db.execSQL("ALTER TABLE moments ADD COLUMN autoArchive INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE moments ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Moment
                db.execSQL("ALTER TABLE moments ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE moments ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                
                // Consumable
                db.execSQL("ALTER TABLE consumables ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE consumables ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                
                // Inventory
                db.execSQL("ALTER TABLE inventories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE inventories ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE inventories ADD COLUMN shelfLifeDays INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE inventories ADD COLUMN purchaseDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE inventories ADD COLUMN expiryDate INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): LifeOrderedDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeOrderedDatabase::class.java,
                    "life_ordered_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
