package com.example.nutrienttrackerv1.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.nutrienttrackerv1.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime



/**
 * ViewModel 与 Room/Repository 的桥接。
 * 函数名全部与我们提供的 MainActivity/各 Screen 调用保持一致。
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    // 构建数据库与仓库（你也可以用 DI，这里用最简单的本地构建）
    private val db: AppDatabase = Room.databaseBuilder(
        app,
        AppDatabase::class.java,
        "nutrition.db"
    ).build()

    private val repo = Repository(db)

    // -------- State --------
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private val _foods = MutableStateFlow<List<Food>>(emptyList())
    val foods: StateFlow<List<Food>> = _foods.asStateFlow()

    private val _goal = MutableStateFlow(defaultGoal())
    val goalState: StateFlow<Goal> = _goal.asStateFlow()

    private val _day = MutableStateFlow<DaySummary?>(null)
    val day: StateFlow<DaySummary?> = _day.asStateFlow()

    init {
        viewModelScope.launch {
            loadFoods()
            loadGoal()
            refreshDay()
        }
    }

    // ---------- 加载/刷新 ----------
    private suspend fun loadFoods() {
        _foods.value = repo.foods()
    }

    private suspend fun loadGoal() {
        _goal.value = repo.getGoal() ?: defaultGoal()
    }

    fun refreshDay() {
        viewModelScope.launch {
            val d = today.value
            val foods = repo.entriesByDate(d)       // List<EntryWithFood>
            val customs = repo.customByDate(d)      // List<CustomEntry>
            val totals = computeTotals(foods, customs)
            _day.value = DaySummary(
                goal = _goal.value,
                foods = foods,
                customs = customs,
                totals = totals
            )
        }
    }

    // ---------- 对外操作 ----------
    fun addFood(food: Food, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            if (food.name.isNotBlank() && !repo.foodNameExists(food.name)) {
                repo.addFood(food)
                loadFoods()
                onSuccess?.invoke()
            }
        }
    }

    fun addEntry(amountG: Double, foodId: Int, notes: String?) {
        viewModelScope.launch {
            repo.addEntry(today.value, foodId, amountG, notes)
            refreshDay()
        }
    }

    fun deleteEntry(id: Int) {
        viewModelScope.launch {
            repo.deleteEntry(id)
            refreshDay()
        }
    }

    fun deleteCustom(id: Int) {
        viewModelScope.launch {
            repo.deleteCustom(id)
            refreshDay()
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            repo.upsertGoal(goal)
            _goal.value = goal
            refreshDay()
        }
    }

    // 供 AI 屏调用：把 AI 的营养结果直接写入 custom_entries
    fun addCustomFromAI(
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
        viewModelScope.launch {
            repo.addCustomEntry(
                date = today.value,
                label = label,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                sugar = sugar,
                sodium = sodium,
                calcium = calcium,
                iron = iron,
                vitaminC = vitaminC,
                notes = notes,
                source = source
            )
            refreshDay()
        }
    }

    // 最近 7 天（这里用 custom_entries 的口径；如需合并 foods 也可扩展）
    suspend fun weekTrend(): List<Pair<String, Map<String, Double>>> {
        val end = today.value
        val start = end.minusDays(6)
        val customs = repo.customBetween(start, end) // List<CustomEntry>
        val grouped = customs.groupBy { it.eat_date } // Map<String, List<CustomEntry>>

        val result = mutableListOf<Pair<String, Map<String, Double>>>()
        grouped.toSortedMap().forEach { (dateStr, list) ->
            val sums = mutableMapOf<String, Double>()
            fun add(key: String, v: Double?) { sums[key] = (sums[key] ?: 0.0) + (v ?: 0.0) }

            list.forEach { c ->
                add("calories_kcal", c.calories_kcal)
                add("protein_g", c.protein_g)
                add("carbs_g", c.carbs_g)
                add("fat_g", c.fat_g)
                add("fiber_g", c.fiber_g)
                add("sugar_g", c.sugar_g)
                add("sodium_mg", c.sodium_mg)
                add("calcium_mg", c.calcium_mg)
                add("iron_mg", c.iron_mg)
                add("vitamin_c_mg", c.vitamin_c_mg)
            }
            result.add(dateStr to sums)
        }
        return result
    }

    // ---------- 计算函数 ----------
    private fun computeTotals(
        withFood: List<EntryWithFood>,
        customs: List<CustomEntry>
    ): Map<String, Double> {
        val sums = mutableMapOf<String, Double>(
            "calories_kcal" to 0.0,
            "protein_g" to 0.0,
            "carbs_g" to 0.0,
            "fat_g" to 0.0,
            "fiber_g" to 0.0,
            "sugar_g" to 0.0,
            "sodium_mg" to 0.0,
            "calcium_mg" to 0.0,
            "iron_mg" to 0.0,
            "vitamin_c_mg" to 0.0,
        )

        // entries（按每100g换算）
        withFood.forEach { ewf ->
            val factor = (ewf.entry.amount_g ?: 0.0) / 100.0
            sums["calories_kcal"] = sums["calories_kcal"]!! + factor * (ewf.food.calories_kcal_per_100g ?: 0.0)
            sums["protein_g"]     = sums["protein_g"]!!     + factor * (ewf.food.protein_g_per_100g ?: 0.0)
            sums["carbs_g"]       = sums["carbs_g"]!!       + factor * (ewf.food.carbs_g_per_100g ?: 0.0)
            sums["fat_g"]         = sums["fat_g"]!!         + factor * (ewf.food.fat_g_per_100g ?: 0.0)
            sums["fiber_g"]       = sums["fiber_g"]!!       + factor * (ewf.food.fiber_g_per_100g ?: 0.0)
            sums["sugar_g"]       = sums["sugar_g"]!!       + factor * (ewf.food.sugar_g_per_100g ?: 0.0)
            sums["sodium_mg"]     = sums["sodium_mg"]!!     + factor * (ewf.food.sodium_mg_per_100g ?: 0.0)
            sums["calcium_mg"]    = sums["calcium_mg"]!!    + factor * (ewf.food.calcium_mg_per_100g ?: 0.0)
            sums["iron_mg"]       = sums["iron_mg"]!!       + factor * (ewf.food.iron_mg_per_100g ?: 0.0)
            sums["vitamin_c_mg"]  = sums["vitamin_c_mg"]!!  + factor * (ewf.food.vitamin_c_mg_per_100g ?: 0.0)
        }

        // custom_entries（本来就是“总量”）
        customs.forEach { c ->
            fun add(key: String, v: Double?) { sums[key] = (sums[key] ?: 0.0) + (v ?: 0.0) }
            add("calories_kcal", c.calories_kcal)
            add("protein_g", c.protein_g)
            add("carbs_g", c.carbs_g)
            add("fat_g", c.fat_g)
            add("fiber_g", c.fiber_g)
            add("sugar_g", c.sugar_g)
            add("sodium_mg", c.sodium_mg)
            add("calcium_mg", c.calcium_mg)
            add("iron_mg", c.iron_mg)
            add("vitamin_c_mg", c.vitamin_c_mg)
        }

        return sums
    }

    private fun defaultGoal(): Goal = Goal(
        id = 1,
        calories_kcal = 2000.0,
        protein_g = 100.0,
        carbs_g = 250.0,
        fat_g = 70.0,
        fiber_g = 25.0,
        sugar_g = 40.0,
        sodium_mg = 2000.0,
        calcium_mg = 1000.0,
        iron_mg = 18.0,
        vitamin_c_mg = 90.0
    )
    // --- API 配置列表 --- //
    private val _apiSettings = MutableStateFlow<List<ApiSetting>>(emptyList())
    val apiSettings: StateFlow<List<ApiSetting>> = _apiSettings.asStateFlow()

    private val _activeProvider = MutableStateFlow<String?>(null)
    val activeProvider: StateFlow<String?> = _activeProvider.asStateFlow()

    fun loadApiSettings() {
        viewModelScope.launch { _apiSettings.value = repo.apiSettings() }
    }

    fun selectApiProvider(name: String?) { _activeProvider.value = name }

    fun saveApiSetting(provider: String, apiKey: String, model: String, endpoint: String) {
        viewModelScope.launch {
            repo.upsertApiSetting(
                ApiSetting(provider = provider, api_key = apiKey, model = model, endpoint = endpoint)
            )
            loadApiSettings()
            _activeProvider.value = provider
        }
    }

    fun deleteApiSetting(provider: String) {
        viewModelScope.launch {
            repo.deleteApiSetting(provider)
            loadApiSettings()
            if (_activeProvider.value == provider) _activeProvider.value = null
        }
    }

}

/** 提供给 TodayScreen 使用的聚合数据结构 */
data class DaySummary(
    val goal: Goal,
    val foods: List<EntryWithFood>,
    val customs: List<CustomEntry>,
    val totals: Map<String, Double>
)
