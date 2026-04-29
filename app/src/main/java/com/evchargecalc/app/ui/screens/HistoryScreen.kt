package com.evchargecalc.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.model.ChargeSession
import com.evchargecalc.app.model.ChartMetric
import com.evchargecalc.app.model.MapsUtils
import com.evchargecalc.app.model.PeriodFilter
import com.evchargecalc.app.model.formatCurrencyAmount
import com.evchargecalc.app.ui.components.ConfirmationDialog
import com.evchargecalc.app.ui.components.EmptyStateCard
import com.evchargecalc.app.ui.components.TechCard
import com.evchargecalc.app.ui.components.format2

private data class CurrencyBucket(
    val currencyCode: String,
    val sessions: List<ChargeSession>
) {
    val totalCost: Double get() = sessions.sumOf { it.costAmount }
    val averageCost: Double get() = if (sessions.isEmpty()) 0.0 else totalCost / sessions.size
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    chargeSessions: List<ChargeSession>,
    currencyCode: String,
    onDeleteSession: (ChargeSession) -> Unit = {},
    onDuplicateSession: (ChargeSession) -> Unit = {}
) {
    val context = LocalContext.current
    var period by remember { mutableStateOf(PeriodFilter.ALL) }
    var metric by remember { mutableStateOf(ChartMetric.COST) }
    var deleteConfirmSessionId by remember { mutableStateOf<String?>(null) }

    val now = System.currentTimeMillis()
    val filtered = chargeSessions
        .filter { session ->
            when (period) {
                PeriodFilter.ALL -> true
                PeriodFilter.LAST_YEAR -> session.timestampMs >= now - 365L * 24 * 60 * 60 * 1000
                PeriodFilter.LAST_MONTH -> session.timestampMs >= now - 30L * 24 * 60 * 60 * 1000
                PeriodFilter.LAST_WEEK -> session.timestampMs >= now - 7L * 24 * 60 * 60 * 1000
            }
        }
        .sortedBy { it.timestampMs }

    val totalEnergy = filtered.sumOf { it.energyKwh }
    val totalTime = filtered.sumOf { it.timeHours }
    val currenciesInPeriod = filtered.map { it.currencyCode }.distinct()
    val hasMixedCurrencies = currenciesInPeriod.size > 1
    val costBuckets = filtered
        .groupBy { it.currencyCode }
        .map { CurrencyBucket(it.key, it.value) }
        .sortedBy { it.currencyCode }
    val totalCost = filtered.sumOf { it.costAmount }

    val avgEnergy = if (filtered.isNotEmpty()) totalEnergy / filtered.size else 0.0
    val avgTime = if (filtered.isNotEmpty()) totalTime / filtered.size else 0.0
    val avgCost = if (filtered.isNotEmpty()) totalCost / filtered.size else 0.0

    val chartValues = filtered.map {
        when (metric) {
            ChartMetric.COST -> it.costAmount
            ChartMetric.ENERGY -> it.energyKwh
            ChartMetric.TIME -> it.timeHours
        }
    }

    val lastSession = filtered.lastOrNull()

    if (deleteConfirmSessionId != null) {
        val sessionToDelete = chargeSessions.firstOrNull { it.id == deleteConfirmSessionId }
        sessionToDelete?.let {
            ConfirmationDialog(
                title = "Delete Session?",
                message = "Remove this charge session from history? This action cannot be undone.",
                confirmText = "Delete",
                onConfirm = {
                    onDeleteSession(it)
                    deleteConfirmSessionId = null
                },
                onDismiss = { deleteConfirmSessionId = null }
            )
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TechCard(title = "History Filters") {
                Text("Period")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PeriodFilter.entries.forEachIndexed { index, entry ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, PeriodFilter.entries.size),
                            selected = period == entry,
                            onClick = { period = entry },
                            icon = {}
                        ) {
                            Text(
                                when (entry) {
                                    PeriodFilter.ALL -> "All"
                                    PeriodFilter.LAST_YEAR -> "Year"
                                    PeriodFilter.LAST_MONTH -> "Month"
                                    PeriodFilter.LAST_WEEK -> "Week"
                                }
                            )
                        }
                    }
                }
                Text("Metric")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ChartMetric.entries.forEachIndexed { index, entry ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index, ChartMetric.entries.size),
                            selected = metric == entry,
                            onClick = { metric = entry },
                            icon = {}
                        ) {
                            Text(
                                when (entry) {
                                    ChartMetric.COST -> "Cost"
                                    ChartMetric.ENERGY -> "Power"
                                    ChartMetric.TIME -> "Time"
                                }
                            )
                        }
                    }
                }
            }
        }

        if (chargeSessions.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Charge History",
                    message = "Your charge sessions will appear here after you save them from the Calculator tab. Start by calculating and saving a charge session!"
                )
            }
        } else {
            if (lastSession != null && filtered.isNotEmpty()) {
                item {
                    TechCard(title = "Quick Actions") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Last Session: ${lastSession.vehicleName} @ ${lastSession.chargerName}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { onDuplicateSession(lastSession) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Duplicate")
                                }
                                Button(
                                    onClick = { deleteConfirmSessionId = lastSession.id },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }
            }

            item {
                TechCard(title = "Totals") {
                    Text("Sessions: ${filtered.size}")
                    Text("Power Charged: ${format2(totalEnergy)} kWh")
                    Text("Time Charged: ${format2(totalTime)} h")
                    if (hasMixedCurrencies) {
                        costBuckets.forEach { bucket ->
                            Text("Cost Charged (${bucket.currencyCode}): ${formatCurrencyAmount(bucket.totalCost, bucket.currencyCode)}")
                        }
                    } else {
                        Text("Cost Charged: ${formatCurrencyAmount(totalCost, currencyCode)}")
                    }
                }
            }

            item {
                TechCard(title = "Averages") {
                    Text("Avg Power / Session: ${format2(avgEnergy)} kWh")
                    Text("Avg Time / Session: ${format2(avgTime)} h")
                    if (hasMixedCurrencies) {
                        costBuckets.forEach { bucket ->
                            Text("Avg Cost / Session (${bucket.currencyCode}): ${formatCurrencyAmount(bucket.averageCost, bucket.currencyCode)}")
                        }
                    } else {
                        Text("Avg Cost / Session: ${formatCurrencyAmount(avgCost, currencyCode)}")
                    }
                }
            }

            item {
                TechCard(title = "Charging Timeline") {
                    if (chartValues.isEmpty()) {
                        Text("No saved charge sessions in this period yet.")
                    } else if (metric == ChartMetric.COST && hasMixedCurrencies) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Cost charts are split by currency.")
                            costBuckets.forEach { bucket ->
                                Text(bucket.currencyCode, color = MaterialTheme.colorScheme.primary)
                                LineChart(values = bucket.sessions.sortedBy { it.timestampMs }.map { it.costAmount })
                            }
                        }
                    } else {
                        LineChart(values = chartValues)
                    }
                }
            }

            items(filtered.reversed(), key = { it.id }) { item ->
                TechCard(title = "${item.vehicleName} @ ${item.chargerLocation}") {
                    Text("Charger: ${item.chargerName}")
                    Text("Power: ${format2(item.energyKwh)} kWh")
                    Text("Time: ${format2(item.timeHours)} h")
                    Text("Cost: ${formatCurrencyAmount(item.costAmount, item.currencyCode)}")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onDuplicateSession(item) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Duplicate")
                        }
                        Button(
                            onClick = {
                                MapsUtils.openDirectionsInMaps(
                                    context,
                                    item.chargerName,
                                    null,
                                    null,
                                    item.chargerLocation
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Maps")
                        }
                        Button(
                            onClick = { deleteConfirmSessionId = item.id },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LineChart(values: List<Double>) {
    val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val min = (values.minOrNull() ?: 0.0)
    val range = (max - min).coerceAtLeast(0.0001)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 8.dp)
    ) {
        if (values.size == 1) {
            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f)
            )
            return@Canvas
        }

        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            val x = index * stepX
            val normalized = ((value - min) / range).toFloat()
            val y = size.height - (normalized * size.height)
            Offset(x, y)
        }

        drawLine(
            color = outlineColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx()
        )

        for (i in 0 until points.size - 1) {
            drawLine(
                color = primaryColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        points.forEach { point ->
            drawCircle(
                color = secondaryColor,
                radius = 3.dp.toPx(),
                center = point,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
