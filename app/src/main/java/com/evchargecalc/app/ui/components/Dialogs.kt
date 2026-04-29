package com.evchargecalc.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.model.MapsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class GeocodeMatch(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

private fun sanitizeSignedDecimalInput(raw: String): String {
    val kept = raw.filter { it.isDigit() || it == '.' || it == '-' || it == '+' }
    if (kept.isEmpty()) return ""

    val sign = if (kept.first() == '-' || kept.first() == '+') kept.first().toString() else ""
    val body = kept
        .removePrefix("-")
        .removePrefix("+")
        .replace("-", "")
        .replace("+", "")

    val dotIndex = body.indexOf('.')
    val normalizedBody = if (dotIndex >= 0) {
        body.substring(0, dotIndex + 1) + body.substring(dotIndex + 1).replace(".", "")
    } else {
        body
    }

    return sign + normalizedBody
}

private fun lookupAddressWithNominatim(query: String, userAgent: String): GeocodeMatch? {
    val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
    val endpoint = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$encoded"
    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("User-Agent", userAgent)
        setRequestProperty("Accept", "application/json")
    }

    return try {
        runCatching {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val items = JSONArray(response)
            if (items.length() == 0) return@runCatching null
            val first = items.getJSONObject(0)
            GeocodeMatch(
                displayName = first.optString("display_name", query),
                latitude = first.getString("lat").toDouble(),
                longitude = first.getString("lon").toDouble()
            )
        }.getOrNull()
    } finally {
        connection.disconnect()
    }
}

/**
 * Confirmation dialog for destructive actions
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * Location picker dialog with manual lat/long input
 * Note: Coordinates can be used with any map provider
 */
@Composable
fun LocationPickerDialog(
    currentLocation: String = "",
    currentLat: Double? = null,
    currentLng: Double? = null,
    onLocationSelected: (location: String, lat: Double?, lng: Double?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locationText by remember { mutableStateOf(currentLocation) }
    var latStr by remember { mutableStateOf(currentLat?.toString() ?: "") }
    var lngStr by remember { mutableStateOf(currentLng?.toString() ?: "") }
    var addressLookupStatus by remember { mutableStateOf<String?>(null) }
    var isAddressLookupRunning by remember { mutableStateOf(false) }
    var coordinateValidationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentLocation) {
        if (locationText.isBlank()) {
            locationText = currentLocation
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Charger Location") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Enter location and coordinates (optional).",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val query = locationText.trim()
                            if (query.isBlank()) {
                                addressLookupStatus = "Enter a location to search."
                                return@Button
                            }

                            isAddressLookupRunning = true
                            addressLookupStatus = "Looking up address..."
                            scope.launch {
                                val userAgent = "EVChargeCalc/1.0 (${context.packageName})"
                                val result = withContext(Dispatchers.IO) {
                                    lookupAddressWithNominatim(query, userAgent)
                                }
                                if (result != null) {
                                    // Keep user's search phrase as concise location label.
                                    locationText = query
                                    latStr = result.latitude.toString()
                                    lngStr = result.longitude.toString()
                                    addressLookupStatus = "Address found: ${result.displayName}"
                                } else {
                                    addressLookupStatus = "No match found. Try a more specific address."
                                }
                                isAddressLookupRunning = false
                            }
                        },
                        enabled = !isAddressLookupRunning
                    ) {
                        Text(if (isAddressLookupRunning) "Searching..." else "Lookup")
                    }
                    Button(onClick = { MapsUtils.openMapsForLocationPicking(context) }) {
                        Text("Open Map")
                    }
                }
                if (addressLookupStatus != null) {
                    Text(
                        text = addressLookupStatus!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (coordinateValidationError != null) {
                    Text(
                        text = coordinateValidationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    "GPS Coordinates (optional - for map directions):",
                    style = MaterialTheme.typography.labelSmall
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latStr,
                        onValueChange = { latStr = sanitizeSignedDecimalInput(it) },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = lngStr,
                        onValueChange = { lngStr = sanitizeSignedDecimalInput(it) },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }
                Text(
                    "Tip: Negative values are valid (e.g., latitude -37.8136, longitude 144.9631).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "Tip: Use Lookup for free OpenStreetMap search or Open Map for manual map browsing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latStr.toDoubleOrNull()
                    val lng = lngStr.toDoubleOrNull()
                    coordinateValidationError = null

                    if (latStr.isNotBlank() && lat == null) {
                        coordinateValidationError = "Latitude must be a valid number."
                        return@Button
                    }
                    if (lngStr.isNotBlank() && lng == null) {
                        coordinateValidationError = "Longitude must be a valid number."
                        return@Button
                    }
                    if (lat != null && lat !in -90.0..90.0) {
                        coordinateValidationError = "Latitude must be between -90 and 90."
                        return@Button
                    }
                    if (lng != null && lng !in -180.0..180.0) {
                        coordinateValidationError = "Longitude must be between -180 and 180."
                        return@Button
                    }

                    onLocationSelected(locationText.trim(), lat, lng)
                }
            ) {
                Text("Save Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Empty state card shown when no data is available
 */
@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    TechCard(title = title) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}
