package com.lifeordered.data.repository

import com.lifeordered.data.database.ConsumableDao
import com.lifeordered.data.database.InventoryDao
import com.lifeordered.data.database.MomentDao
import com.lifeordered.data.database.TagDao
import com.lifeordered.data.models.Consumable
import com.lifeordered.data.models.Inventory
import com.lifeordered.data.models.Moment
import com.lifeordered.data.models.TagModel
import kotlinx.coroutines.flow.Flow

class LifeOrderedRepository(
    private val momentDao: MomentDao,
    private val consumableDao: ConsumableDao,
    private val inventoryDao: InventoryDao,
    private val tagDao: TagDao
) {
    val allMoments: Flow<List<Moment>> = momentDao.getAllMomentsFlow()
    val deletedMoments: Flow<List<Moment>> = momentDao.getDeletedMomentsFlow()
    val allConsumables: Flow<List<Consumable>> = consumableDao.getAllConsumablesFlow()
    val deletedConsumables: Flow<List<Consumable>> = consumableDao.getDeletedConsumablesFlow()
    val allInventories: Flow<List<Inventory>> = inventoryDao.getAllInventoriesFlow()
    val deletedInventories: Flow<List<Inventory>> = inventoryDao.getDeletedInventoriesFlow()
    val allTags: Flow<List<TagModel>> = tagDao.getAllTagsFlow()

    suspend fun insertMoment(moment: Moment): Long {
        return momentDao.insertMoment(moment)
    }

    suspend fun updateMoment(moment: Moment) {
        momentDao.updateMoment(moment)
    }

    suspend fun deleteMoment(moment: Moment) {
        momentDao.deleteMoment(moment)
    }

    suspend fun insertConsumable(consumable: Consumable): Long {
        return consumableDao.insertConsumable(consumable)
    }

    suspend fun updateConsumable(consumable: Consumable) {
        consumableDao.updateConsumable(consumable)
    }

    suspend fun deleteConsumable(consumable: Consumable) {
        consumableDao.deleteConsumable(consumable)
    }

    suspend fun insertInventory(inventory: Inventory): Long {
        return inventoryDao.insertInventory(inventory)
    }

    suspend fun updateInventory(inventory: Inventory) {
        inventoryDao.updateInventory(inventory)
    }

    suspend fun deleteInventory(inventory: Inventory) {
        inventoryDao.deleteInventory(inventory)
    }

    suspend fun insertTag(tag: TagModel): Long {
        return tagDao.insertTag(tag)
    }

    suspend fun deleteTag(tag: TagModel) {
        tagDao.deleteTag(tag)
    }
}
