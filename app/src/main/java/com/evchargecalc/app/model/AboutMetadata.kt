package com.evchargecalc.app.model

data class AboutMetadata(
    val appName: String,
    val tagline: String,
    val companyOrAuthor: String,
    val copyrightLine: String,
    val supportEmail: String,
    val websiteUrl: String,
    val privacyPolicyUrl: String,
    val termsUrl: String,
    val licenseName: String,
    val acknowledgements: String
)

val defaultAboutMetadata = AboutMetadata(
    appName = "EV Charge Calc",
    tagline = "Estimate EV charging cost, time, and range with profile-aware calculations.",
    companyOrAuthor = "Col Dawe",
    copyrightLine = "Col Dawe (2026)",
    supportEmail = "TBA",
    websiteUrl = "TBA",
    privacyPolicyUrl = "See repository file: PRIVACY_POLICY.md",
    termsUrl = "See repository file: TERMS_AND_CONDITIONS.md",
    licenseName = "MIT",
    acknowledgements = "Built with Kotlin, Jetpack Compose, and AndroidX."
)
