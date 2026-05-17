package com.freshtrack.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshtrack.domain.catalog.PRODUCT_CATEGORIES
import com.freshtrack.ui.components.FreshProductImage
import com.freshtrack.domain.model.ExpirationStatus
import com.freshtrack.domain.model.Product
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getEffectiveExpirationDate
import com.freshtrack.domain.model.getDaysUntilExpiration
import com.freshtrack.domain.model.getExpirationStatus
import com.freshtrack.domain.model.isActive
import com.freshtrack.domain.usecase.MonthlyData
import com.freshtrack.domain.usecase.StatsResult
import com.freshtrack.ui.theme.ColorExpired
import com.freshtrack.ui.theme.ColorFresh
import com.freshtrack.ui.theme.ColorSoon
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlin.math.roundToInt

@Composable
fun StatsScreen(vm: StatsViewModel = hiltViewModel()) {
    val stats by vm.stats.collectAsState()
    val products by vm.products.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Statistiques", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                "Suivez le frigo, les alertes et l'anti-gaspi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        StatsSegmentedTabs(selected = tab, onSelect = { tab = it })

        val s = stats
        if (s == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingStatsCard()
            }
            return
        }

        key(tab) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    0 -> FrigoTab(products)
                    1 -> AntiGaspiTab(s)
                    2 -> TendancesTab(s)
                }
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

    FreshStatsCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("En stock", Icons.Default.Inventory2, MaterialTheme.colorScheme.primary)
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
        SectionCard("Bientôt à vérifier", Icons.Default.Schedule, ColorSoon) {
            urgent.forEach { product -> UrgentRow(product) }
        }
    }

    if (categoryCounts.isNotEmpty()) {
        SectionCard("Par catégorie", Icons.Default.Category, MaterialTheme.colorScheme.primary) {
            categoryCounts.entries.sortedByDescending { it.value }.forEach { (key, count) ->
                val catIconName = PRODUCT_CATEGORIES.find { it.key == key }?.icon ?: ""
                val catIcon = categoryIcon(catIconName)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(catIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(categoryLabel(key), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(count.toString(), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (active.isEmpty()) {
        EmptyStatsCard("Aucun produit en stock", "Ajoutez des produits pour voir la répartition du frigo.")
    }
}

@Composable
private fun StockDistributionBar(expired: Int, soon: Int, fresh: Int, total: Int) {
    Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(999.dp))) {
        Segment(expired, total, ColorExpired)
        Segment(soon, total, ColorSoon)
        Segment(fresh, total, ColorFresh)
    }
}

@Composable
private fun RowScope.Segment(count: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    if (count <= 0 || total <= 0) return
    Box(Modifier.weight(count.toFloat()).height(14.dp).background(color))
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
    val tz = TimeZone.currentSystemDefault()
    val addedDate = product.addedAt.toLocalDateTime(tz).date
    val effectiveExpiry = product.getEffectiveExpirationDate()
    val totalLifeDays = addedDate.daysUntil(effectiveExpiry).coerceAtLeast(1)
    val lifeProgress = (days.coerceAtLeast(0).toFloat() / totalLifeDays).coerceIn(0f, 1f)
    val barColor = when {
        lifeProgress > 0.5f -> ColorFresh
        lifeProgress > 0.2f -> ColorSoon
        else -> ColorExpired
    }
    FreshStatsCard {
        Column(Modifier.padding(horizontal = 12.dp).padding(top = 12.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FreshProductImage(
                    imageUrl = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    tint = color,
                    fallbackIcon = Icons.Default.Inventory2
                )
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(categoryLabel(product.category ?: "autre"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(label, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { lifeProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                color = barColor,
                trackColor = barColor.copy(alpha = 0.20f)
            )
        }
    }
}

@Composable
private fun AntiGaspiTab(stats: StatsResult) {
    FreshStatsCard {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SectionTitle("Score anti-gaspi ce mois", Icons.Default.Restore, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            AntiGaspiGauge(stats.monthlyScore)
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
        SmallMetric("Série", "${stats.streak}", "mois ≥75%", Icons.Default.Star, Modifier.weight(1f))
        SmallMetric("Utilisation", "${stats.avgUtilizationRate.roundToInt()}%", "vie utilisée", Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f))
        SmallMetric("Conso moy.", "${stats.avgConsumptionDays.roundToInt()}j", "après ajout", Icons.Default.Schedule, Modifier.weight(1f))
    }

    SectionCard("Évolution mensuelle", Icons.AutoMirrored.Filled.TrendingUp, ColorFresh) {
        MonthlyScoreChart(stats.monthly)
        MonthLabels(stats.monthly)
    }

    if (stats.categoryScores.isNotEmpty()) {
        SectionCard("Score par catégorie", Icons.Default.Category, MaterialTheme.colorScheme.primary) {
            stats.categoryScores.forEach { (cat, score) ->
                ProgressRow(categoryLabel(cat), score)
            }
        }
    }

    if (stats.topThrown.isNotEmpty()) {
        SectionCard("Produits les plus gaspillés", Icons.Default.Inventory2, ColorExpired) {
            stats.topThrown.forEachIndexed { i, (name, count) ->
                val medal = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${i + 1}." }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (count >= 3) ColorExpired.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$medal $name", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("x$count", color = ColorExpired, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (stats.topThrown.isEmpty() && stats.categoryScores.isEmpty()) {
        EmptyStatsCard("Pas encore de données anti-gaspi", "Marquez des produits consommés ou jetés pour alimenter ces statistiques.")
    }
}

@Composable
private fun TendancesTab(stats: StatsResult) {
    FreshStatsCard {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Text("Durée moyenne avant consommation", fontWeight = FontWeight.SemiBold)
            }
            Text("${stats.avgConsumptionDays.roundToInt()} jours", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
    }

    SectionCard("Ajouts / consommés / jetés", Icons.AutoMirrored.Filled.TrendingUp, MaterialTheme.colorScheme.primary) {
        MonthlyAddedChart(stats.monthly)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            ChartLegend("Ajoutés", MaterialTheme.colorScheme.primary)
            ChartLegend("Consommés", ColorFresh)
            ChartLegend("Jetés", ColorExpired)
        }
        MonthLabels(stats.monthly)
    }

    if (stats.topRecurrent.isNotEmpty()) {
        SectionCard("Produits récurrents", Icons.Default.Restore, MaterialTheme.colorScheme.primary) {
            stats.topRecurrent.forEachIndexed { i, (name, count) ->
                val medal = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${i + 1}." }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$medal $name", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text("x$count", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }
            }
        }
    } else {
        EmptyStatsCard("Aucune tendance récurrente", "Les produits ajoutés plusieurs fois apparaîtront ici.")
    }
}

@Composable
private fun StatsSegmentedTabs(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("Frigo", "Anti-Gaspi", "Tendances")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected == index) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = if (selected == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FreshStatsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { content() }
    )
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    FreshStatsCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(title, icon, color)
            content()
        }
    }
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun LoadingStatsCard() {
    FreshStatsCard {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text("Calcul des statistiques…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyStatsCard(title: String, subtitle: String) {
    FreshStatsCard {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartLegend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SmallMetric(title: String, value: String, subtitle: String, icon: ImageVector = Icons.Default.Restore, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun categoryIcon(iconName: String): ImageVector = when (iconName) {
    "Apple", "Wheat", "Sparkles" -> Icons.Default.Eco
    "Milk", "Beef", "Fish", "Croissant", "CupSoda", "UtensilsCrossed" -> Icons.Default.Restaurant
    "Snowflake" -> Icons.Default.AcUnit
    "Archive", "Package" -> Icons.Default.Inventory2
    "Egg" -> Icons.Default.CheckCircle
    else -> Icons.Default.Category
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
            lineSeries { series(monthly.map { it.score }) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(rememberLineCartesianLayer()),
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

@Composable
private fun AntiGaspiGauge(score: Float) {
    val gaugeColor = when {
        score >= 75f -> ColorFresh
        score >= 50f -> ColorSoon
        else -> ColorExpired
    }
    val appreciation = when {
        score >= 75f -> "Excellent !"
        score >= 50f -> "Bien !"
        else -> "À améliorer"
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        Modifier.fillMaxWidth().height(160.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 22.dp.toPx()
            val r = (minOf(size.width / 2f, size.height) - sw / 2f - 4.dp.toPx()).coerceAtLeast(0f)
            val cx = size.width / 2f
            val cy = size.height
            val topLeft = Offset(cx - r, cy - r)
            val arcSize = Size(r * 2f, r * 2f)

            drawArc(trackColor, 180f, 180f, false, topLeft, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(gaugeColor, 180f, (score.coerceIn(0f, 100f) / 100f) * 180f, false, topLeft, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(
            Modifier.padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${score.roundToInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = gaugeColor
            )
            Text(
                appreciation,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = gaugeColor
            )
        }
    }
}

private fun categoryLabel(key: String): String =
    PRODUCT_CATEGORIES.firstOrNull { it.key == key }?.label ?: key.replaceFirstChar { it.uppercase() }
