package com.freshtrack.domain.usecase

import androidx.compose.runtime.Immutable
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
data class MonthlyData(
    val label: String,      // "Jan 25"
    val added: Int,
    val consumed: Int,
    val thrown: Int,
    val score: Float        // 0-100
)

@Immutable
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

        // Pré-calcul des buckets mensuels en un seul pass par liste source.
        // Clé = (year, monthNumber) ; valeur = compteur cumulé pour le mois.
        val addedByMonth = HashMap<Long, Int>()
        for (p in allProducts) {
            val date = p.addedAt?.toLocalDateTime(tz)?.date ?: continue
            val key = monthKey(date.year, date.monthNumber)
            addedByMonth[key] = (addedByMonth[key] ?: 0) + 1
        }
        val consumedByMonth = HashMap<Long, Int>()
        for (p in consumed) {
            val date = p.statusChangedAt?.toLocalDateTime(tz)?.date ?: continue
            val key = monthKey(date.year, date.monthNumber)
            consumedByMonth[key] = (consumedByMonth[key] ?: 0) + 1
        }
        val thrownByMonth = HashMap<Long, Int>()
        for (p in thrown) {
            val date = p.statusChangedAt?.toLocalDateTime(tz)?.date ?: continue
            val key = monthKey(date.year, date.monthNumber)
            thrownByMonth[key] = (thrownByMonth[key] ?: 0) + 1
        }

        fun scoreForProducts(c: List<Product>, t: List<Product>): Float {
            val total = c.size + t.size
            return if (total == 0) 0f else (c.size.toFloat() / total * 100)
        }

        // Build monthly buckets (6 months) à partir des index pré-calculés.
        val monthly = (5 downTo 0).map { monthsBack ->
            val targetDate = today.minus(monthsBack.toLong(), DateTimeUnit.MONTH)
            val yr = targetDate.year; val mo = targetDate.monthNumber
            val key = monthKey(yr, mo)
            val frMonths = arrayOf("Jan","Fév","Mar","Avr","Mai","Juin","Juil","Août","Sep","Oct","Nov","Déc")
            val label = frMonths[targetDate.monthNumber - 1] + " ${yr % 100}"

            val monthAdded = addedByMonth[key] ?: 0
            val monthConsumed = consumedByMonth[key] ?: 0
            val monthThrown = thrownByMonth[key] ?: 0
            val total = monthConsumed + monthThrown
            val monthScore = if (total == 0) 0f else monthConsumed.toFloat() / total * 100f
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

    private fun monthKey(year: Int, month: Int): Long = year * 12L + month
}
