package com.evchargecalc.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.evchargecalc.app.BuildConfig
import com.evchargecalc.app.R
import com.evchargecalc.app.model.defaultAboutMetadata
import com.evchargecalc.app.ui.components.TechCard

@Composable
fun AboutScreen() {
    val info = defaultAboutMetadata

    TechCard(title = "About") {
        Image(
            painter = painterResource(id = R.drawable.about_banner),
            contentDescription = "Watt Tracker banner",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(info.appName, style = MaterialTheme.typography.titleMedium)
            Text(info.tagline, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            Text("Package: ${BuildConfig.APPLICATION_ID}")
            Text("Developer: ${info.companyOrAuthor}")
            Text("Copyright: ${info.copyrightLine}")
            Text("Support: ${info.supportEmail}")
            Text("Website: ${info.websiteUrl}")
            Text("Privacy Policy: ${info.privacyPolicyUrl}")
            Text("Terms: ${info.termsUrl}")
            Text("License: ${info.licenseName}")
            Text("Acknowledgements: ${info.acknowledgements}")
        }
    }
}
