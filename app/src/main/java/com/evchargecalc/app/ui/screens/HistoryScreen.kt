package com.evchargecalc.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.model.ChargeSession
import com.evchargecalc.app.model.ChargerProfile
import com.evchargecalc.app.model.ChartMetric
import com.evchargecalc.app.model.DistanceUnit
import com.evchargecalc.app.model.MapsUtils
import com.evchargecalc.app.model.PeriodFilter
import com.evchargecalc.app.model.VehicleProfile
import com.evchargecalc.app.model.chargeSessionTagOptions
import com.evchargecalc.app.model.formatCurrencyAmount
import com.evchargecalc.app.model.kmToSelected
import com.evchargecalc.app.model.parseHexColor
import com.evchargecalc.app.ui.components.ConfirmationDialog
import com.evchargecalc.app.ui.components.EmptyStateCard
import com.evchargecalc.app.ui.components.SelectionDropdown
import com.evchargecalc.app.ui.components.TechCard
import com.evchargecalc.app.ui.components.format2
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

private const val ALL_NETWORKS = "All Networks"
private const val ALL_TAGS = "All Types"

private data class CurrencyBucket(
    val currencyCode: String,
    val sessions: List<ChargeSession>
) {
    val totalCost: Double get() = sessions.sumOf { it.costAmount }
    val averageCost: Double get() = if (sessions.isEmpty()) 0.0 else totalCost / sessions.size
}

private data class ChartBucket(
    val key: String,
    val label: String,
    val timestampMs: Long
)

private data class ChartSeries(
    val label: String,
    val color: Color,
    val values: List<Double>
)

private data class ChartModel(
    val buckets: List<ChartBucket>,
    val series: List<ChartSeries>,
    val xAxisTitle: String
)

private enum class BucketResolution { DAY, MONTH, YEAR }

private fun formatSessionDate(timestampMs: Long): String {
    val formatter = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}

private fun historyMetricLabel(metric: ChartMetric, currencyCode: String): String {
    return when (metric) {
        ChartMetric.COST -> "Cost ($currencyCode)"
        ChartMetric.ENERGY -> "Energy (kWh)"
        ChartMetric.TIME -> "Time (h)"
    }
}

private fun metricValue(session: ChargeSession, metric: ChartMetric): Double {
    return when (metric) {
        ChartMetric.COST -> session.costAmount
        ChartMetric.ENERGY -> session.energyKwh
        ChartMetric.TIME -> session.timeHours
    }
}

private fun formatAxisValue(value: Double, metric: ChartMetric, currencyCode: String): String {
    return when (metric) {
        ChartMetric.COST -> formatCurrencyAmount(value, currencyCode)
        ChartMetric.ENERGY -> "${format2(value)} kWh"
        ChartMetric.TIME -> "${format2(value)} h"
    }
}

private fun buildBuckets(
    period: PeriodFilter,
    sessions: List<ChargeSession>,
    nowMs: Long
): Pair<List<ChartBucket>, BucketResolution> {
    if (sessions.isEmpty()) return emptyList<ChartBucket>() to BucketResolution.DAY

    val zone = ZoneId.systemDefault()
    val nowDate = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()

    return when (period) {
        PeriodFilter.ALL -> {
            val firstMonth = YearMonth.from(Instant.ofEpochMilli(sessions.first().timestampMs).atZone(zone))
            val lastMonth = YearMonth.from(Instant.ofEpochMilli(sessions.last().timestampMs).atZone(zone))
            val monthCount = ChronoUnit.MONTHS.between(firstMonth, lastMonth) + 1
            if (monthCount > 12) {
                val buckets = (firstMonth.year..lastMonth.year).map { year ->
                    val start = LocalDate.of(year, 1, 1)
                    ChartBucket(
                        key = year.toString(),
                        label = year.toString(),
                        timestampMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
                    )
                }
                buckets to BucketResolution.YEAR
            } else {
                val monthFormatter = DateTimeFormatter.ofPattern("MMM yy")
                val buckets = buildList {
                    var cursor = firstMonth
                    while (!cursor.isAfter(lastMonth)) {
                        val start = cursor.atDay(1)
                        add(
                            ChartBucket(
                                key = cursor.toString(),
                                label = cursor.format(monthFormatter),
                                timestampMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
                            )
                        )
                        cursor = cursor.plusMonths(1)
                    }
                }
                buckets to BucketResolution.MONTH
            }
        }

        PeriodFilter.LAST_YEAR -> {
            val startMonth = YearMonth.from(nowDate).minusMonths(11)
            val monthFormatter = DateTimeFormatter.ofPattern("MMM yy")
            val buckets = buildList {
                var cursor = startMonth
                repeat(12) {
                    val start = cursor.atDay(1)
                    add(
                        ChartBucket(
                            key = cursor.toString(),
                            label = cursor.format(monthFormatter),
                            timestampMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
                        )
                    )
                    cursor = cursor.plusMonths(1)
                }
            }
            buckets to BucketResolution.MONTH
        }

        PeriodFilter.LAST_MONTH -> {
            val startDate = nowDate.minusDays(29)
            val dayFormatter = DateTimeFormatter.ofPattern("dd MMM")
            val buckets = (0L..29L).map { offset ->
                val day = startDate.plusDays(offset)
                ChartBucket(
                    key = day.toString(),
                    label = day.format(dayFormatter),
                    timestampMs = day.atStartOfDay(zone).toInstant().toEpochMilli()
                )
            }
            buckets to BucketResolution.DAY
        }

        PeriodFilter.LAST_WEEK -> {
            val startDate = nowDate.minusDays(6)
            val dayFormatter = DateTimeFormatter.ofPattern("dd MMM")
            val buckets = (0L..6L).map { offset ->
                val day = startDate.plusDays(offset)
                ChartBucket(
                    key = day.toString(),
                    label = day.format(dayFormatter),
                    timestampMs = day.atStartOfDay(zone).toInstant().toEpochMilli()
                )
            }
            buckets to BucketResolution.DAY
        }
    }
}

private fun bucketKeyForTimestamp(timestampMs: Long, resolution: BucketResolution): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(timestampMs).atZone(zone)
    return when (resolution) {
        BucketResolution.DAY -> dateTime.toLocalDate().toString()
        BucketResolution.MONTH -> YearMonth.from(dateTime).toString()
        BucketResolution.YEAR -> dateTime.year.toString()
    }
}

private fun buildChartModel(
    sessions: List<ChargeSession>,
    vehicles: List<VehicleProfile>,
    period: PeriodFilter,
    metric: ChartMetric,
    nowMs: Long
): ChartModel {
    val (buckets, resolution) = buildBuckets(period, sessions.sortedBy { it.timestampMs }, nowMs)
    val bucketTotals = buckets.associate { it.key to mutableMapOf<String, Double>() }

    sessions.forEach { session ->
        val bucketKey = bucketKeyForTimestamp(session.timestampMs, resolution)
        val vehicleTotals = bucketTotals[bucketKey] ?: return@forEach
        vehicleTotals[session.vehicleId] = (vehicleTotals[session.vehicleId] ?: 0.0) + metricValue(session, metric)
    }

    val vehicleColorMap = vehicles.associate { it.id to parseHexColor(it.chartColorHex) }
    val vehicleNameMap = vehicles.associate { it.id to "${it.make} ${it.model}" }
    val orderedVehicleIds = sessions
        .map { it.vehicleId }
        .distinct()
        .sortedBy { vehicleNameMap[it] ?: sessions.firstOrNull { session -> session.vehicleId == it }?.vehicleName ?: it }

    val series = orderedVehicleIds.map { vehicleId ->
        val fallbackName = sessions.firstOrNull { it.vehicleId == vehicleId }?.vehicleName ?: vehicleId
        ChartSeries(
            label = vehicleNameMap[vehicleId] ?: fallbackName,
            color = vehicleColorMap[vehicleId] ?: parseHexColor("#9FFF5E"),
            values = buckets.map { bucket -> bucketTotals[bucket.key]?.get(vehicleId) ?: 0.0 }
        )
    }

    val xAxisTitle = when (resolution) {
        BucketResolution.DAY -> "Day"
        BucketResolution.MONTH -> "Month"
        BucketResolution.YEAR -> "Year"
    }

    return ChartModel(
        buckets = buckets,
        series = series,
        xAxisTitle = xAxisTitle
    )
}

private fun sparseAxisLabels(labels: List<String>): List<String> {
    if (labels.size <= 6) return labels

    val maxTicks = 5
    val step = (labels.lastIndex.toDouble() / (maxTicks - 1)).coerceAtLeast(1.0)
    val indexes = buildList {
        repeat(maxTicks) { index ->
            add((index * step).toInt().coerceIn(0, labels.lastIndex))
        }
    }.distinct()

    return indexes.map { labels[it] }
}

private fun csvEscape(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}

private fun csvNumber(value: Double): String = String.format(Locale.US, "%.6f", value)

private fun estimateNetworkName(session: ChargeSession, chargers: List<ChargerProfile>): String {
    if (session.chargerNetwork.isNotBlank()) return session.chargerNetwork
    return chargers.firstOrNull { it.id == session.chargerId }?.networkName.orEmpty()
}

private fun buildHistoryCsv(
    sessions: List<ChargeSession>,
    chargers: List<ChargerProfile>,
    distanceUnit: DistanceUnit
): String {
    val unitLabel = if (distanceUnit == DistanceUnit.MI) "mi" else "km"
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val header = listOf(
        "id",
        "timestamp_ms",
        "date_local",
        "vehicle",
        "charger",
        "network",
        "location",
        "tag",
        "energy_kwh",
        "time_h",
        "cost",
        "currency",
        "distance_km",
        "distance_${unitLabel}",
        "eff_kwh_per_100km",
        "eff_${unitLabel}_per_kwh",
        "notes"
    ).joinToString(",")

    val rows = sessions.map { session ->
        val distanceKm = session.distanceDrivenKm
        val distanceSelected = distanceKm?.kmToSelected(distanceUnit)
        val effPer100Km = if (distanceKm != null && distanceKm > 0.0) {
            (session.energyKwh / distanceKm) * 100.0
        } else {
            null
        }
        val effDistancePerKwh = if (distanceSelected != null && session.energyKwh > 0.0) {
            distanceSelected / session.energyKwh
        } else {
            null
        }

        listOf(
            csvEscape(session.id),
            session.timestampMs.toString(),
            csvEscape(dateFormat.format(Date(session.timestampMs))),
            csvEscape(session.vehicleName),
            csvEscape(session.chargerName),
            csvEscape(estimateNetworkName(session, chargers)),
            csvEscape(session.chargerLocation),
            csvEscape(session.sessionTag),
            csvNumber(session.energyKwh),
            csvNumber(session.timeHours),
            csvNumber(session.costAmount),
            csvEscape(session.currencyCode),
            distanceKm?.let { csvNumber(it) } ?: "",
            distanceSelected?.let { csvNumber(it) } ?: "",
            effPer100Km?.let { csvNumber(it) } ?: "",
            effDistancePerKwh?.let { csvNumber(it) } ?: "",
            csvEscape(session.notes)
        ).joinToString(",")
    }

    return (listOf(header) + rows).joinToString("\n")
}

private fun projectionValues(sessions: List<ChargeSession>): Pair<Double, Double> {
    if (sessions.isEmpty()) return 0.0 to 0.0

    val sorted = sessions.sortedBy { it.timestampMs }
    val zone = ZoneId.systemDefault()
    val startDate = Instant.ofEpochMilli(sorted.first().timestampMs).atZone(zone).toLocalDate()
    val endDate = Instant.ofEpochMilli(sorted.last().timestampMs).atZone(zone).toLocalDate()
    val days = max(1L, ChronoUnit.DAYS.between(startDate, endDate) + 1)
    val totalCost = sessions.sumOf { it.costAmount }
    val perDay = totalCost / days
    return (perDay * 30.4375) to (perDay * 365.25)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vehicles: List<VehicleProfile>,
    chargers: List<ChargerProfile>,
    chargeSessions: List<ChargeSession>,
    distanceUnit: DistanceUnit,
    currencyCode: String,
    onDeleteSession: (ChargeSession) -> Unit = {},
    onDuplicateSession: (ChargeSession) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var period by remember { mutableStateOf(PeriodFilter.ALL) }
    var metric by remember { mutableStateOf(ChartMetric.COST) }
    var deleteConfirmSessionId by remember { mutableStateOf<String?>(null) }
    var networkFilter by remember { mutableStateOf(ALL_NETWORKS) }
    var typeFilter by remember { mutableStateOf(ALL_TAGS) }
    var exportNotice by remember { mutableStateOf<String?>(null) }
    var pendingCsvContent by remember { mutableStateOf("") }
    val segmentedColors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) {
            exportNotice = "Export cancelled."
        } else {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream).use { writer ->
                        writer.write(pendingCsvContent)
                    }
                }
            }.onSuccess {
                exportNotice = "CSV export saved."
            }.onFailure {
                exportNotice = "CSV export failed."
            }
        }
    }

    val chargerNetworkById = remember(chargers) {
        chargers.associate { it.id to it.networkName.trim() }
    }

    fun networkForSession(session: ChargeSession): String {
        if (session.chargerNetwork.isNotBlank()) return session.chargerNetwork
        return chargerNetworkById[session.chargerId].orEmpty()
    }

    val now = System.currentTimeMillis()
    val baseFiltered = chargeSessions
        .filter { session ->
            when (period) {
                PeriodFilter.ALL -> true
                PeriodFilter.LAST_YEAR -> session.timestampMs >= now - 365L * 24 * 60 * 60 * 1000
                PeriodFilter.LAST_MONTH -> session.timestampMs >= now - 30L * 24 * 60 * 60 * 1000
                PeriodFilter.LAST_WEEK -> session.timestampMs >= now - 7L * 24 * 60 * 60 * 1000
            }
        }
        .sortedBy { it.timestampMs }

    val filtered = baseFiltered
        .filter { session ->
            networkFilter == ALL_NETWORKS || networkForSession(session).equals(networkFilter, ignoreCase = true)
        }
        .filter { session ->
            typeFilter == ALL_TAGS || session.sessionTag.equals(typeFilter, ignoreCase = true)
        }

    val networkOptions = remember(chargeSessions, chargers) {
        val discovered = buildSet {
            chargeSessions.forEach { session ->
                val fromSession = session.chargerNetwork.trim()
                if (fromSession.isNotBlank()) add(fromSession)
                val fromCharger = chargers.firstOrNull { it.id == session.chargerId }?.networkName?.trim().orEmpty()
                if (fromCharger.isNotBlank()) add(fromCharger)
            }
        }.sorted()
        listOf(ALL_NETWORKS) + discovered
    }

    if (networkFilter != ALL_NETWORKS && networkFilter !in networkOptions) {
        networkFilter = ALL_NETWORKS
    }

    val totalEnergy = filtered.sumOf { it.energyKwh }
    val totalTime = filtered.sumOf { it.timeHours }
    val totalDistanceKm = filtered.mapNotNull { it.distanceDrivenKm }.sum()
    val totalCost = filtered.sumOf { it.costAmount }
    val currenciesInPeriod = filtered.map { it.currencyCode }.distinct()
    val hasMixedCurrencies = currenciesInPeriod.size > 1
    val costBuckets = filtered
        .groupBy { it.currencyCode }
        .map { CurrencyBucket(it.key, it.value) }
        .sortedBy { it.currencyCode }

    val avgEnergy = if (filtered.isNotEmpty()) totalEnergy / filtered.size else 0.0
    val avgTime = if (filtered.isNotEmpty()) totalTime / filtered.size else 0.0
    val avgCost = if (filtered.isNotEmpty()) totalCost / filtered.size else 0.0
    val lastSession = filtered.lastOrNull()
    val vehicleAccentColors = remember(vehicles) {
        vehicles.associate { it.id to parseHexColor(it.chartColorHex) }
    }

    val drivingKwhPer100Km = if (totalDistanceKm > 0.0) (totalEnergy / totalDistanceKm) * 100.0 else null
    val selectedDistanceLabel = if (distanceUnit == DistanceUnit.MI) "mi" else "km"
    val drivingDistancePerKwh = if (totalEnergy > 0.0 && totalDistanceKm > 0.0) {
        totalDistanceKm.kmToSelected(distanceUnit) / totalEnergy
    } else {
        null
    }

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
                            colors = segmentedColors,
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
                            colors = segmentedColors,
                            icon = {}
                        ) {
                            Text(
                                when (entry) {
                                    ChartMetric.COST -> "Cost"
                                    ChartMetric.ENERGY -> "Energy"
                                    ChartMetric.TIME -> "Time"
                                }
                            )
                        }
                    }
                }

                SelectionDropdown(
                    title = "Network",
                    selectedText = networkFilter,
                    options = networkOptions.map { it to it },
                    onSelect = { selected -> networkFilter = selected ?: ALL_NETWORKS },
                    includeNoneOption = false
                )

                SelectionDropdown(
                    title = "Charge Type",
                    selectedText = typeFilter,
                    options = (listOf(ALL_TAGS) + chargeSessionTagOptions).map { it to it },
                    onSelect = { selected -> typeFilter = selected ?: ALL_TAGS },
                    includeNoneOption = false
                )
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
                                    onClick = {
                                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                        pendingCsvContent = ""
                                        exportNotice = "Preparing CSV export..."
                                        scope.launch {
                                            val csv = withContext(Dispatchers.Default) {
                                                buildHistoryCsv(filtered, chargers, distanceUnit)
                                            }
                                            pendingCsvContent = csv
                                            exportNotice = null
                                            exportLauncher.launch("ev_charge_history_$timestamp.csv")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Export CSV")
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
                            if (exportNotice != null) {
                                Text(
                                    text = exportNotice!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            item {
                TechCard(title = "Totals") {
                    Text("Sessions: ${filtered.size}")
                    Text("Energy Charged: ${format2(totalEnergy)} kWh")
                    Text("Time Charged: ${format2(totalTime)} h")
                    if (totalDistanceKm > 0.0) {
                        Text("Distance Logged: ${format2(totalDistanceKm.kmToSelected(distanceUnit))} $selectedDistanceLabel")
                    }
                    if (hasMixedCurrencies) {
                        costBuckets.forEach { bucket ->
                            Text("Cost Charged (${bucket.currencyCode}): ${formatCurrencyAmount(bucket.totalCost, bucket.currencyCode)}")
                        }
                    } else {
                        Text("Cost Charged: ${formatCurrencyAmount(totalCost, currencyCode)}")
                    }

                    if (drivingKwhPer100Km != null) {
                        Text("Fleet Efficiency: ${format2(drivingKwhPer100Km)} kWh/100km")
                    }
                    if (drivingDistancePerKwh != null) {
                        Text("Distance per kWh: ${format2(drivingDistancePerKwh)} $selectedDistanceLabel/kWh")
                    }
                }
            }

            item {
                TechCard(title = "Averages") {
                    Text("Avg Energy / Session: ${format2(avgEnergy)} kWh")
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
                TechCard(title = "Projected Cost") {
                    if (filtered.isEmpty()) {
                        Text("Not enough data to project yet.")
                    } else if (hasMixedCurrencies) {
                        costBuckets.forEach { bucket ->
                            val (monthly, yearly) = projectionValues(bucket.sessions)
                            Text("${bucket.currencyCode} Monthly: ${formatCurrencyAmount(monthly, bucket.currencyCode)}")
                            Text("${bucket.currencyCode} Yearly: ${formatCurrencyAmount(yearly, bucket.currencyCode)}")
                        }
                    } else {
                        val (monthly, yearly) = projectionValues(filtered)
                        Text("Monthly Projection: ${formatCurrencyAmount(monthly, currencyCode)}")
                        Text("Yearly Projection: ${formatCurrencyAmount(yearly, currencyCode)}")
                    }
                    Text(
                        "Based on filtered sessions and observed session frequency.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            item {
                TechCard(title = "Charging Timeline") {
                    if (filtered.isEmpty()) {
                        Text("No saved charge sessions in this period yet.")
                    } else if (metric == ChartMetric.COST && hasMixedCurrencies) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Cost charts are split by currency.")
                            costBuckets.forEach { bucket ->
                                val chartModel = buildChartModel(
                                    sessions = bucket.sessions.sortedBy { it.timestampMs },
                                    vehicles = vehicles,
                                    period = period,
                                    metric = ChartMetric.COST,
                                    nowMs = now
                                )
                                Text(bucket.currencyCode, color = MaterialTheme.colorScheme.primary)
                                MultiSeriesLineChart(
                                    series = chartModel.series,
                                    metric = ChartMetric.COST,
                                    currencyCode = bucket.currencyCode,
                                    yAxisLabel = historyMetricLabel(ChartMetric.COST, bucket.currencyCode),
                                    xAxisLabels = sparseAxisLabels(chartModel.buckets.map { it.label }),
                                    xAxisTitle = chartModel.xAxisTitle
                                )
                            }
                        }
                    } else {
                        val chartModel = buildChartModel(
                            sessions = filtered,
                            vehicles = vehicles,
                            period = period,
                            metric = metric,
                            nowMs = now
                        )
                        MultiSeriesLineChart(
                            series = chartModel.series,
                            metric = metric,
                            currencyCode = currencyCode,
                            yAxisLabel = historyMetricLabel(metric, currencyCode),
                            xAxisLabels = sparseAxisLabels(chartModel.buckets.map { it.label }),
                            xAxisTitle = chartModel.xAxisTitle
                        )
                    }
                }
            }

            items(filtered.reversed(), key = { it.id }) { item ->
                val accentColor = vehicleAccentColors[item.vehicleId] ?: parseHexColor("#9FFF5E")
                val sessionNetwork = networkForSession(item)
                val sessionDistance = item.distanceDrivenKm?.kmToSelected(distanceUnit)
                val sessionEfficiency = if (item.distanceDrivenKm != null && item.distanceDrivenKm > 0.0) {
                    (item.energyKwh / item.distanceDrivenKm) * 100.0
                } else {
                    null
                }

                TechCard(
                    title = "${item.vehicleName} | ${formatSessionDate(item.timestampMs)}",
                    titleColor = accentColor,
                    accentColor = accentColor
                ) {
                    Text("Charger: ${item.chargerName}")
                    Text("Location: ${item.chargerLocation}")
                    if (sessionNetwork.isNotBlank()) {
                        Text("Network: $sessionNetwork")
                    }
                    Text("Type: ${item.sessionTag}")
                    Text("Energy: ${format2(item.energyKwh)} kWh")
                    Text("Time: ${format2(item.timeHours)} h")
                    Text("Cost: ${formatCurrencyAmount(item.costAmount, item.currencyCode)}")
                    if (sessionDistance != null) {
                        Text("Distance: ${format2(sessionDistance)} $selectedDistanceLabel")
                    }
                    if (sessionEfficiency != null) {
                        Text("Efficiency: ${format2(sessionEfficiency)} kWh/100km")
                    }
                    if (item.notes.isNotBlank()) {
                        Text("Notes: ${item.notes}")
                    }
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
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete session"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiSeriesLineChart(
    series: List<ChartSeries>,
    metric: ChartMetric,
    currencyCode: String,
    yAxisLabel: String,
    xAxisLabels: List<String>,
    xAxisTitle: String
) {
    val maxValue = series.flatMap { it.values }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val outlineColor = MaterialTheme.colorScheme.outline
    val tickValues = listOf(maxValue, maxValue / 2.0, 0.0)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Y-axis: $yAxisLabel",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )

        if (series.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                series.chunked(2).forEach { chunk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        chunk.forEach { item ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(item.color, CircleShape)
                                )
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = item.color
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.height(180.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                tickValues.forEach { tick ->
                    Text(
                        text = formatAxisValue(tick, metric, currencyCode),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .padding(top = 8.dp)
            ) {
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )

                series.forEach { chartSeries ->
                    if (chartSeries.values.isEmpty()) return@forEach

                    if (chartSeries.values.size == 1) {
                        val normalized = (chartSeries.values.first() / maxValue).toFloat()
                        drawCircle(
                            color = chartSeries.color,
                            radius = 5.dp.toPx(),
                            center = Offset(size.width / 2f, size.height - (normalized * size.height))
                        )
                        return@forEach
                    }

                    val stepX = size.width / (chartSeries.values.size - 1)
                    val points = chartSeries.values.mapIndexed { index, value ->
                        val x = index * stepX
                        val normalized = (value / maxValue).toFloat()
                        val y = size.height - (normalized * size.height)
                        Offset(x, y)
                    }

                    for (index in 0 until points.size - 1) {
                        drawLine(
                            color = chartSeries.color,
                            start = points[index],
                            end = points[index + 1],
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    points.forEach { point ->
                        drawCircle(
                            color = chartSeries.color,
                            radius = 3.dp.toPx(),
                            center = point,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }
        }

        if (xAxisLabels.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                xAxisLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "X-axis: $xAxisTitle",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
