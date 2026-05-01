package com.freshtrack.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshtrack.domain.usecase.MonthlyData
import com.freshtrack.domain.usecase.StatsResult
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import kotlin.math.roundToInt

@Composable
fun StatsScreen(vm: StatsViewModel = hiltViewModel()) {
    val stats by vm.stats.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Anti-Gaspi") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Tendances") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Catégories") })
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
                0 -> AntiGaspiTab(stats!!)
                1 -> TendancesTab(stats!!)
                2 -> CategoriesTab(stats!!)
            }
        }
    }
}

@Composable
private fun AntiGaspiTab(stats: StatsResult) {
    StatCard("Score anti-gaspi ce mois", "${stats.monthlyScore.roundToInt()}%") {
        val trend = stats.trend
        val trendLabel = when {
            trend > 0 -> "↑ +${trend.roundToInt()}% vs mois dernier"
            trend < 0 -> "↓ ${trend.roundToInt()}% vs mois dernier"
            else -> "= Stable vs mois dernier"
        }
        Text(trendLabel, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (stats.streak > 0) {
        StatCard("Série en cours", "${stats.streak} mois ≥ 75%")
    }
    StatCard("Consommation moyenne", "${stats.avgConsumptionDays.roundToInt()} jours")

    if (stats.topThrown.isNotEmpty()) {
        Text("Top produits gaspillés", fontWeight = FontWeight.SemiBold)
        stats.topThrown.forEachIndexed { i, (name, count) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${i + 1}. $name", style = MaterialTheme.typography.bodyMedium)
                Text("$count×", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    Text("Évolution mensuelle (score %)", fontWeight = FontWeight.SemiBold)
    MonthlyScoreChart(stats.monthly)
    // Labels des mois sous le graphique
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        stats.monthly.forEach { m ->
            Text(m.label.substringBefore(" "), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TendancesTab(stats: StatsResult) {
    StatCard("Durée moy. avant consommation", "${stats.avgConsumptionDays.roundToInt()} jours")
    if (stats.topRecurrent.isNotEmpty()) {
        Text("Produits récurrents", fontWeight = FontWeight.SemiBold)
        stats.topRecurrent.forEachIndexed { i, (name, count) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${i + 1}. $name", style = MaterialTheme.typography.bodyMedium)
                Text("$count×", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text("Produits ajoutés / mois", fontWeight = FontWeight.SemiBold)
    MonthlyAddedChart(stats.monthly)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        stats.monthly.forEach { m ->
            Text(m.label.substringBefore(" "), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoriesTab(stats: StatsResult) {
    if (stats.categoryScores.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pas encore assez de données")
        }
        return
    }
    Text("Score par catégorie (consommé / total)", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    stats.categoryScores.forEach { (cat, score) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(cat, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("${score.roundToInt()}%",
                color = if (score >= 75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
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
private fun StatCard(title: String, value: String, extra: @Composable (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            extra?.invoke()
        }
    }
}
