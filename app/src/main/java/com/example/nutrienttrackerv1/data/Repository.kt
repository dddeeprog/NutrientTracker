package com.example.nutrienttrackerv1.data

import java.time.LocalDate
import java.time.LocalDateTime

class Repository(private val db: AppDatabase) {

    // —— 对应你 DaoDatabase.kt 里的 DAO —— //
    private val foodDao = db.foodDao()
    private val entryDao = db.entryDao()
    private val goalDao = db.goalDao()
    private val customDao = db.customEntryDao()
    private val apiSettingDao = db.apiSettingDao()
    private val aiSessionDao = db.aiSessionDao()
    // API Setting



    // ------------ Food -------------- //
    suspend fun foods(): List<Food> = foodDao.all()
    suspend fun addFood(food: Food) = foodDao.insert(food)
    suspend fun foodNameExists(name: String) = foodDao.countByName(name) > 0

    // ------------ Entry ------------- //
    suspend fun addEntry(
        date: LocalDate,
        foodId: Int,
        amountG: Double,
        notes: String?
    ) {
        val e = Entry(
            id = 0,
            eat_date = date.toString(),
            food_id = foodId,
            amount_g = amountG,
            notes = notes,
            created_at = LocalDateTime.now().toString()
        )
        entryDao.insert(e)
    }

    suspend fun deleteEntry(id: Int) = entryDao.delete(id)

    suspend fun entriesByDate(date: LocalDate): List<EntryWithFood> =
        entryDao.getByDateWithFood(date.toString())

    suspend fun entriesBetween(start: LocalDate, end: LocalDate): List<Entry> =
        entryDao.getBetween(start.toString(), end.toString())

    // ----------- CustomEntry -------- //
    suspend fun customByDate(date: LocalDate): List<CustomEntry> =
        customDao.byDate(date.toString())

    suspend fun customBetween(start: LocalDate, end: LocalDate): List<CustomEntry> =
        customDao.between(start.toString(), end.toString())

    suspend fun addCustomEntry(
        date: LocalDate,
        label: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double,
        sugar: Double,
        sodium: Double,
        calcium: Double,
        iron: Double,
        vitaminC: Double,
        notes: String?,
        source: String?
    ) {
        val ce = CustomEntry(
            id = 0,
            eat_date = date.toString(),
            label = label,
            calories_kcal = calories,
            protein_g = protein,
            carbs_g = carbs,
            fat_g = fat,
            fiber_g = fiber,
            sugar_g = sugar,
            sodium_mg = sodium,
            calcium_mg = calcium,
            iron_mg = iron,
            vitamin_c_mg = vitaminC,
            notes = notes,
            source = source,
            created_at = LocalDateTime.now().toString()
        )
        customDao.insert(ce)              // ← 关键：用你的 customEntryDao()
    }

    suspend fun deleteCustom(id: Int) = customDao.delete(id)
    suspend fun customMinDate(): String? = customDao.minDate()

    // -------------- Goal ------------ //
    suspend fun getGoal(): Goal? = goalDao.get()
    suspend fun upsertGoal(goal: Goal) = goalDao.upsert(goal)

    // ---------- API Setting --------- //
    suspend fun apiSetting(provider: String): ApiSetting? = apiSettingDao.byProvider(provider)
    suspend fun apiSettings(): List<ApiSetting> = apiSettingDao.all()
    suspend fun upsertApiSetting(setting: ApiSetting) = apiSettingDao.upsert(setting)
    suspend fun deleteApiSetting(provider: String) = apiSettingDao.delete(provider)

    // ----------- AI Session --------- //
    suspend fun insertAiSession(session: AiSession) = aiSessionDao.insert(session)


}
