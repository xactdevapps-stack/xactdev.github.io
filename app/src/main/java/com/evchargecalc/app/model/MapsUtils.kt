package com.evchargecalc.app.model

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * OpenStreetMap integration utilities for charger location picking and directions
 * No API key required - uses free OpenStreetMap service
 */
object MapsUtils {

    /**
     * Open OpenStreetMap to view charger location and get directions
     * @param context The context to launch the intent
     * @param chargerName The name of the charger (for display)
     * @param latitude The latitude of the charger (if available)
     * @param longitude The longitude of the charger (if available)
     * @param location The location name/address (fallback if no coordinates)
     */
    fun openDirectionsInMaps(
        context: Context,
        chargerName: String,
        latitude: Double?,
        longitude: Double?,
        location: String
    ) {
        val webUri = if (latitude != null && longitude != null) {
            // Use OpenStreetMap with coordinates and zoom level
            Uri.parse("https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude&zoom=15&layers=M")
        } else {
            // Fallback to search by location/charger name
            Uri.parse("https://www.openstreetmap.org/search?query=${Uri.encode(location + " " + chargerName)}")
        }

        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        try {
            context.startActivity(webIntent)
        } catch (e: Exception) {
            // Silently fail if no browser available
            e.printStackTrace()
        }
    }

    /**
     * Open OpenStreetMap for location reference
     * Note: Users can manually enter coordinates in the LocationPickerDialog instead
     */
    fun openMapsForLocationPicking(context: Context) {
        val webUri = Uri.parse("https://www.openstreetmap.org/")
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        try {
            context.startActivity(webIntent)
        } catch (e: Exception) {
            // Silently fail if no browser available
            e.printStackTrace()
        }
    }

    /**
     * Format coordinates for display
     */
    fun formatCoordinates(latitude: Double?, longitude: Double?): String {
        return if (latitude != null && longitude != null) {
            "${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}"
        } else {
            "No coordinates"
        }
    }
}
