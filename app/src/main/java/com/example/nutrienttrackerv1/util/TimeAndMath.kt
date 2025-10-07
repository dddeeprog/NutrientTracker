package com.example.nutrienttrackerv1.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun LocalDate.isoDate(): String = this.format(DateTimeFormatter.ISO_DATE)
fun isoDateTime(): String = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)

// 营养计算：将某条 entries 与 foods 做换算
fun computeEntryNutrients(amountG: Double, per100: Map<String, Double>): Map<String, Double> {
    val factor = (amountG / 100.0).coerceAtLeast(0.0)
    return per100.mapValues { (_, v) -> factor * (v ?: 0.0) }
}
