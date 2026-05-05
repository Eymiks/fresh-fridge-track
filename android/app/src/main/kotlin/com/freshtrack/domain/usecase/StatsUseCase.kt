package com.freshtrack.domain.usecase

import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class MonthlyData(
    val label: String,      // "Jan 25"
    val added: Int,
    val consumed: Int,
    val thrown: Int,
    val score: Float        // 0-100
)

data class StatsResult(
    val monthlyScore: Float,
    val prevMonthScore: Float,
    val trend: Float,
    val streak: Int,
    val avgUtilizationRate: Float,
    val categoryScores: List<Pair<String, Float>>,   // (categoryKey, score 0-100)
    val topThrown: List<Pair<String, Int>>,           // (name, count)
    val topRecurrent: List<Pair<String, Int>>,        // (name/barcode, count)
    val avgConsumptionDays: Float,
    val monthly: List<MonthlyData>
)

@Singleton
class StatsUseCase @Inject constructor() {

    fun compute(allProducts: List<Product>): StatsResult {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(tz)

        val consumed = allProducts.filter { it.status == ProductStatus.CONSUMED }
        val thrown = allProducts.filter { it.status == ProductStatus.THROWN }

        fun scoreForProducts(c: List<Product>, t: List<Product>): Float {
            val total = c.size + t.size
            return if (total == 0) 0f else (c.size.toFloat() / total * 100)
        }

        // Build monthly buckets (6 months)
        val monthly = (5 downTo 0).map { monthsBack ->
            val targetDate = today.minus(monthsBack.toLong(), DateTimeUnit.MONTH)
            val yr = targetDate.year; val mo = targetDate.monthNumber
            val label = targetDate.month.name.take(3).lowercase()
                .replaceFirstChar { it.uppercase() } + " ${yr % 100}"

            val monthAdded = allProducts.count { inMonth(it.addedAt?.toLocalDateTime(tz)?.date, yr, mo) }
            val monthConsumed = consumed.count { inMonth(it.statusChangedAt?.toLocalDateTime(tz)?.date, yr, mo) }
            val monthThrown = thrown.count { inMonth(it.statusChangedAt?.toLocalDateTime(tz)?.date, yr, mo) }
            val monthScore = scoreForProducts(
                consumed.filter { inMonth(it.statusChangedAt?.toLocalDateTime(tz)?.date, yr, mo) },
                thrown.filter { inMonth(it.statusChangedAt?.toLocalDateTime(tz)?.date, yr, mo) }
            )
            MonthlyData(label, monthAdded, monthConsumed, monthThrown, monthScore)
        }

        val currentScore = monthly.lastOrNull()?.score ?: 0f
        val prevScore = monthly.getOrNull(monthly.size - 2)?.score ?: 0f
        val trend = currentScore - prevScore

        // Streak: consecutive months with score >= 75%
        val streak = monthly.reversed().takeWhile { it.score >= 75f }.size

        // Avg consumption days
        val consumedWithDates = consumed.filter { it.addedAt != null && it.statusChangedAt != null }
        val avgConsumptionDays = if (consumedWithDates.isEmpty()) 0f else {
            consumedWithDates.map { p ->
                val addedDate = p.addedAt.toLocalDateTime(tz).date
                val changedDate = p.statusChangedAt!!.toLocalDateTime(tz).date
                addedDate.daysUntil(changedDate).toFloat()
            }.average().toFloat()
        }

        val utilizationRates = consumed.mapNotNull { p ->
            val changedDate = p.statusChangedAt?.toLocalDateTime(tz)?.date ?: return@mapNotNull null
            val addedDate = p.addedAt.toLocalDateTime(tz).date
            val totalLife = addedDate.daysUntil(p.expirationDate)
            if (totalLife <= 0) return@mapNotNull null
            val usedLife = addedDate.daysUntil(changedDate).coerceIn(0, totalLife)
            usedLife.toFloat() / totalLife.toFloat() * 100f
        }
        val avgUtilizationRate = if (utilizationRates.isEmpty()) 0f else {
            utilizationRates.average().toFloat()
        }

        // Category scores
        val allCategoryKeys = (consumed + thrown).mapNotNull { it.category }.distinct()
        val categoryScores = allCategoryKeys.map { cat ->
            val c = consumed.filter { it.category == cat }
            val t = thrown.filter { it.category == cat }
            cat to scoreForProducts(c, t)
        }.sortedBy { it.second }

        // Top thrown
        val topThrown = thrown
            .groupBy { it.name }
            .map { (name, list) -> name to list.size }
            .sortedByDescending { it.second }
            .take(3)

        // Top recurrent consumed
        val topRecurrent = (consumed + allProducts.filter { it.status == ProductStatus.ACTIVE })
            .groupBy { it.barcode?.takeIf { b -> b.isNotBlank() } ?: it.name }
            .filter { (_, list) -> list.size >= 2 }
            .map { (key, list) -> (list.firstOrNull()?.name ?: key) to list.size }
            .sortedByDescending { it.second }
            .take(3)

        return StatsResult(
            monthlyScore = currentScore,
            prevMonthScore = prevScore,
            trend = trend,
            streak = streak,
            avgUtilizationRate = avgUtilizationRate,
            categoryScores = categoryScores,
            topThrown = topThrown,
            topRecurrent = topRecurrent,
            avgConsumptionDays = avgConsumptionDays,
            monthly = monthly
        )
    }

    private fun inMonth(date: LocalDate?, year: Int, month: Int) =
        date != null && date.year == year && date.monthNumber == month
}
