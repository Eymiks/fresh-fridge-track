package com.freshtrack.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshtrack.domain.catalog.PRODUCT_CATEGORIES
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getDaysUntilExpiration
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.domain.model.isActive
import com.freshtrack.domain.usecase.MonthlyData
import com.freshtrack.domain.usecase.StatsResult
import com.freshtrack.ui.theme.ColorExpired
import com.freshtrack.ui.theme.ColorFresh
import com.freshtrack.ui.theme.ColorSoon
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import kotlin.math.roundToInt

@Composable
fun StatsScreen(vm: StatsViewModel = hiltViewModel()) {
    val stats by vm.stats.collectAsState()
    val products by vm.products.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Frigo") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Anti-Gaspi") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Tendances") })
        }

        if (stats == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tab) {
                0 -> FrigoTab(products)
                1 -> AntiGaspiTab(stats!!)
                2 -> TendancesTab(stats!!)
            }
        }
    }
}

@Composable
private fun FrigoTab(products: List<Product>) {
    val active = products.filter { it.isActive() }
    val expired = active.filter { it.getExpirationStatus() == ExpirationStatus.EXPIRED }
    val soon = active.filter { it.getExpirationStatus() == ExpirationStatus.SOON }
    val fresh = active.filter { it.getExpirationStatus() == ExpirationStatus.FRESH }
    val urgent = active.sortedBy { it.getDaysUntilExpiration() }.take(5)
    val categoryCounts = active.groupingBy { it.category ?: "autre" }.eachCount()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("En stock", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(active.size.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            if (active.isNotEmpty()) {
                StockDistributionBar(expired.size, soon.size, fresh.size, active.size)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Legend("Périmés", expired.size, ColorExpired)
                    Legend("Bientôt", soon.size, ColorSoon)
                    Legend("Frais", fresh.size, ColorFresh)
                }
            } else {
                Text("Aucun produit en stock", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (urgent.isNotEmpty()) {
        Text("Bientôt à vérifier", fontWeight = FontWeight.SemiBold)
        urgent.forEach { product -> UrgentRow(product) }
    }

    if (categoryCounts.isNotEmpty()) {
        Text("Par catégorie", fontWeight = FontWeight.SemiBold)
        categoryCounts.entries.sortedByDescending { it.value }.forEach { (key, count) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(categoryLabel(key), modifier = Modifier.weight(1f))
                Text(count.toString(), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StockDistributionBar(expired: Int, soon: Int, fresh: Int, total: Int) {
    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp))) {
        Segment(expired, total, ColorExpired)
        Segment(soon, total, ColorSoon)
        Segment(fresh, total, ColorFresh)
    }
}

@Composable
private fun RowScope.Segment(count: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    if (count <= 0 || total <= 0) return
    Box(Modifier.weight(count.toFloat()).height(10.dp).background(color))
}

@Composable
private fun Legend(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(999.dp)).background(color))
        Spacer(Modifier.size(4.dp))
        Text("$count $label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UrgentRow(product: Product) {
    val days = product.getDaysUntilExpiration()
    val color = when (product.getExpirationStatus()) {
        ExpirationStatus.EXPIRED -> ColorExpired
        ExpirationStatus.SOON -> ColorSoon
        ExpirationStatus.FRESH -> ColorFresh
    }
    val label = when {
        days < 0 -> "Périmé depuis ${-days}j"
        days == 0 -> "Aujourd'hui"
        days == 1 -> "Demain"
        else -> "${days}j"
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Inventory2, contentDescription = null, tint = color)
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(categoryLabel(product.category ?: "autre"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(label, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AntiGaspiTab(stats: StatsResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Recycling, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Text("Score anti-gaspi ce mois", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${stats.monthlyScore.roundToInt()}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            val trend = stats.trend
            Text(
                when {
                    trend > 0 -> "+${trend.roundToInt()}% vs mois dernier"
                    trend < 0 -> "${trend.roundToInt()}% vs mois dernier"
                    else -> "Stable vs mois dernier"
                },
                color = if (trend >= 0) ColorFresh else ColorExpired,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallMetric("Série", "${stats.streak}", "mois ≥75%", Modifier.weight(1f))
        SmallMetric("Utilisation", "${stats.avgUtilizationRate.roundToInt()}%", "vie utilisée", Modifier.weight(1f))
        SmallMetric("Conso moy.", "${stats.avgConsumptionDays.roundToInt()}j", "après ajout", Modifier.weight(1f))
    }

    Text("Évolution mensuelle", fontWeight = FontWeight.SemiBold)
    MonthlyScoreChart(stats.monthly)
    MonthLabels(stats.monthly)

    if (stats.categoryScores.isNotEmpty()) {
        Text("Score par catégorie", fontWeight = FontWeight.SemiBold)
        stats.categoryScores.forEach { (cat, score) ->
            ProgressRow(categoryLabel(cat), score)
        }
    }

    if (stats.topThrown.isNotEmpty()) {
        Text("Produits les plus gaspillés", fontWeight = FontWeight.SemiBold)
        stats.topThrown.forEachIndexed { i, (name, count) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${i + 1}. $name", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("x$count", color = ColorExpired, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TendancesTab(stats: StatsResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Text("Durée moyenne avant consommation", fontWeight = FontWeight.SemiBold)
            }
            Text("${stats.avgConsumptionDays.roundToInt()} jours", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
    }

    Text("Ajouts / consommés / jetés", fontWeight = FontWeight.SemiBold)
    MonthlyAddedChart(stats.monthly)
    MonthLabels(stats.monthly)

    if (stats.topRecurrent.isNotEmpty()) {
        Text("Produits récurrents", fontWeight = FontWeight.SemiBold)
        stats.topRecurrent.forEachIndexed { i, (name, count) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${i + 1}. $name", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("x$count", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SmallMetric(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProgressRow(label: String, score: Float) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("${score.roundToInt()}%", color = if (score >= 75) ColorFresh else ColorExpired, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun MonthlyScoreChart(monthly: List<MonthlyData>) {
    if (monthly.isEmpty()) return
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(monthly) {
        modelProducer.runTransaction {
            columnSeries { series(monthly.map { it.score }) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(rememberColumnCartesianLayer()),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxWidth().height(160.dp)
    )
}

@Composable
private fun MonthlyAddedChart(monthly: List<MonthlyData>) {
    if (monthly.isEmpty()) return
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(monthly) {
        modelProducer.runTransaction {
            columnSeries {
                series(monthly.map { it.added.toFloat() })
                series(monthly.map { it.consumed.toFloat() })
                series(monthly.map { it.thrown.toFloat() })
            }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(rememberColumnCartesianLayer()),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxWidth().height(160.dp)
    )
}

@Composable
private fun MonthLabels(monthly: List<MonthlyData>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        monthly.forEach { m ->
            Text(m.label.substringBefore(" "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun categoryLabel(key: String): String =
    PRODUCT_CATEGORIES.firstOrNull { it.key == key }?.label ?: key.replaceFirstChar { it.uppercase() }
