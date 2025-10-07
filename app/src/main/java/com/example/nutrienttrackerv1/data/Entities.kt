package com.example.nutrienttrackerv1.data

import androidx.room.*
import java.time.LocalDate

// ===== Entities =====

@Entity(tableName = "foods", indices = [Index(value = ["name"], unique = true)])
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val serving_size_g: Double = 100.0,
    val calories_kcal_per_100g: Double = 0.0,
    val protein_g_per_100g: Double = 0.0,
    val carbs_g_per_100g: Double = 0.0,
    val fat_g_per_100g: Double = 0.0,
    val fiber_g_per_100g: Double = 0.0,
    val sugar_g_per_100g: Double = 0.0,
    val sodium_mg_per_100g: Double = 0.0,
    val calcium_mg_per_100g: Double = 0.0,
    val iron_mg_per_100g: Double = 0.0,
    val vitamin_c_mg_per_100g: Double = 0.0
)

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = Food::class,
            parentColumns = ["id"],
            childColumns = ["food_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("food_id"), Index("eat_date")]
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eat_date: String,         // ISO yyyy-MM-dd
    val food_id: Int,
    val amount_g: Double,
    val notes: String? = null,
    val created_at: String        // ISO date-time
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: Int = 1,
    val calories_kcal: Double? = 2000.0,
    val protein_g: Double? = 100.0,
    val carbs_g: Double? = 250.0,
    val fat_g: Double? = 70.0,
    val fiber_g: Double? = 25.0,
    val sugar_g: Double? = 40.0,
    val sodium_mg: Double? = 2000.0,
    val calcium_mg: Double? = 1000.0,
    val iron_mg: Double? = 18.0,
    val vitamin_c_mg: Double? = 90.0
)

@Entity(tableName = "custom_entries", indices = [Index("eat_date")])
data class CustomEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eat_date: String,
    val label: String,
    val calories_kcal: Double = 0.0,
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0,
    val fiber_g: Double = 0.0,
    val sugar_g: Double = 0.0,
    val sodium_mg: Double = 0.0,
    val calcium_mg: Double = 0.0,
    val iron_mg: Double = 0.0,
    val vitamin_c_mg: Double = 0.0,
    val notes: String? = null,
    val source: String? = null,
    val created_at: String
)

@Entity(tableName = "ai_sessions")
data class AiSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val created_at: String,
    val provider: String?,
    val model: String?,
    val prompt_hash: String?,
    val result_json: String?,
    val note: String?
)

@Entity(tableName = "api_settings")
data class ApiSetting(
    @PrimaryKey val provider: String,
    val api_key: String?,
    val model: String?,
    val endpoint: String?
)

// ===== Relations / DTOs =====

data class EntryWithFood(
    @Embedded val entry: Entry,
    @Relation(parentColumn = "food_id", entityColumn = "id")
    val food: Food
)

// 与 Python 保持一致的营养键清单
data class NutrientMeta(val key: String, val label: String, val per100Column: String)

val NUTRIENTS_META = listOf(
    NutrientMeta("calories_kcal", "千卡 (kcal)", "calories_kcal_per_100g"),
    NutrientMeta("protein_g", "蛋白质 (g)", "protein_g_per_100g"),
    NutrientMeta("carbs_g", "碳水 (g)", "carbs_g_per_100g"),
    NutrientMeta("fat_g", "脂肪 (g)", "fat_g_per_100g"),
    NutrientMeta("fiber_g", "膳食纤维 (g)", "fiber_g_per_100g"),
    NutrientMeta("sugar_g", "糖 (g)", "sugar_g_per_100g"),
    NutrientMeta("sodium_mg", "钠 (mg)", "sodium_mg_per_100g"),
    NutrientMeta("calcium_mg", "钙 (mg)", "calcium_mg_per_100g"),
    NutrientMeta("iron_mg", "铁 (mg)", "iron_mg_per_100g"),
    NutrientMeta("vitamin_c_mg", "维生素C (mg)", "vitamin_c_mg_per_100g")
)


