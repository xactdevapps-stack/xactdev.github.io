package com.evchargecalc.app.model

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class CurrencyOption(val code: String, val label: String)

private val currencyLocalesByCode: Map<String, Locale> by lazy {
    val map = mutableMapOf<String, Locale>()
    Locale.getAvailableLocales().forEach { locale ->
        val code = runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()
        if (code != null && !map.containsKey(code)) {
            map[code] = locale
        }
    }
    map
}

fun allCurrencyOptions(): List<CurrencyOption> {
    return Currency.getAvailableCurrencies()
        .map { currency ->
            val symbol = runCatching {
                currency.getSymbol(localeForCurrencyCode(currency.currencyCode))
            }.getOrDefault(currency.currencyCode)
            CurrencyOption(currency.currencyCode, "${currency.currencyCode} - ${currency.displayName} ($symbol)")
        }
        .sortedBy { it.code }
}

fun formatCurrencyAmount(amount: Double, currencyCode: String): String {
    val locale = localeForCurrencyCode(currencyCode)
    val formatter = NumberFormat.getCurrencyInstance(locale)
    formatter.currency = Currency.getInstance(currencyCode)
    return formatter.format(amount)
}

private fun localeForCurrencyCode(currencyCode: String): Locale {
    return currencyLocalesByCode[currencyCode] ?: Locale.UK
}
