package com.example.nutrienttrackerv1.data

import androidx.room.*

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name")
    suspend fun all(): List<Food>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(food: Food)

    @Query("SELECT COUNT(*) FROM foods WHERE name = :name")
    suspend fun countByName(name: String): Int
}

@Dao
interface EntryDao {
    @Transaction
    @Query("SELECT * FROM entries WHERE eat_date = :date ORDER BY id DESC")
    suspend fun getByDate(date: String): List<Entry>

    @Transaction
    @Query("""
        SELECT entries.* FROM entries 
        WHERE eat_date >= :start AND eat_date <= :end 
        ORDER BY eat_date ASC, id ASC
    """)
    suspend fun getBetween(start: String, end: String): List<Entry>

    @Transaction
    @Query("SELECT * FROM entries WHERE eat_date = :date ORDER BY id DESC")
    suspend fun getByDateWithFood(date: String): List<EntryWithFood>

    @Insert
    suspend fun insert(entry: Entry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: Int)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE id = 1")
    suspend fun get(): Goal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: Goal)
}

@Dao
interface CustomEntryDao {
    @Query("SELECT * FROM custom_entries WHERE eat_date = :date ORDER BY id DESC")
    suspend fun byDate(date: String): List<CustomEntry>

    @Query("""
        SELECT * FROM custom_entries 
        WHERE eat_date >= :start AND eat_date <= :end 
        ORDER BY eat_date ASC, id ASC
    """)
    suspend fun between(start: String, end: String): List<CustomEntry>

    @Insert
    suspend fun insert(entry: CustomEntry)

    @Query("DELETE FROM custom_entries WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT MIN(eat_date) FROM custom_entries")
    suspend fun minDate(): String?
}

@Dao
interface ApiSettingDao {
    @Query("SELECT * FROM api_settings WHERE provider = :provider")
    suspend fun byProvider(provider: String): ApiSetting?

    @Query("SELECT * FROM api_settings ORDER BY provider")
    suspend fun all(): List<ApiSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: ApiSetting)

    @Query("DELETE FROM api_settings WHERE provider = :provider")
    suspend fun delete(provider: String)
}


@Dao
interface AiSessionDao {
    @Insert
    suspend fun insert(session: AiSession)
}

@Database(
    entities = [Food::class, Entry::class, Goal::class, CustomEntry::class, AiSession::class, ApiSetting::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun entryDao(): EntryDao
    abstract fun goalDao(): GoalDao
    abstract fun customEntryDao(): CustomEntryDao
    abstract fun apiSettingDao(): ApiSettingDao
    abstract fun aiSessionDao(): AiSessionDao
}
