package com.example.nutrienttrackerv1.ai

// ==== 与 Python 版保持一致的提示 ====
const val ROLE_PROMPT = """
    # Role: 营养分析专家- 你是专业的营养分析助手。请根据图片进行严格的结构化分析。"""

const val AI_JSON_REQUIREMENT =
    "请严格输出 UTF-8 编码的 JSON，仅包含 keys: items(list), serving_context(str), error_margin_pct(number), advice(str)。" +
            "items[i] 含: name, estimated_weight_g, confidence(0-1), notes, " +
            "以及 calories_kcal, protein_g, carbs_g, fat_g, fiber_g, sugar_g, sodium_mg, calcium_mg, iron_mg, vitamin_c_mg（数值为该份总量）。"

data class ImageUrl(val url: String)
data class MsgContent(val type: String, val text: String? = null, val image_url: ImageUrl? = null)
data class Msg(val role: String, val content: List<MsgContent>)
data class ChatRequest(val model: String, val temperature: Int = 0, val messages: List<Msg>)
data class ChoiceMessage(val content: String)
data class Choice(val message: ChoiceMessage)
data class ChatResponse(val choices: List<Choice>)

// 解析 AI 返回的 JSON（业务结构）
data class AiItem(
    val name: String,
    val estimated_weight_g: Double? = 0.0,
    val confidence: Double? = 0.0,
    val notes: String? = "",
    val calories_kcal: Double? = 0.0,
    val protein_g: Double? = 0.0,
    val carbs_g: Double? = 0.0,
    val fat_g: Double? = 0.0,
    val fiber_g: Double? = 0.0,
    val sugar_g: Double? = 0.0,
    val sodium_mg: Double? = 0.0,
    val calcium_mg: Double? = 0.0,
    val iron_mg: Double? = 0.0,
    val vitamin_c_mg: Double? = 0.0,
)

data class AiParsed(
    val items: List<AiItem> = emptyList(),
    val serving_context: String? = null,
    val error_margin_pct: Double? = null,
    val advice: String? = null,
)
