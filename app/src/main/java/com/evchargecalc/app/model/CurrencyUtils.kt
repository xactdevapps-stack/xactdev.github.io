package com.evchargecalc.app.model

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class CurrencyOption(val code: String, val label: String)

fun allCurrencyOptions(): List<CurrencyOption> {
    return Currency.getAvailableCurrencies()
        .map { currency ->
            val symbol = runCatching {
                currency.getSymbol(localeForCurrency(currency.currencyCode))
            }.getOrDefault(currency.currencyCode)
            CurrencyOption(currency.currencyCode, "${currency.currencyCode} - ${currency.displayName} ($symbol)")
        }
        .sortedBy { it.code }
}

fun formatCurrencyAmount(amount: Double, currencyCode: String): String {
    val locale = localeForCurrency(currencyCode)
    val formatter = NumberFormat.getCurrencyInstance(locale)
    formatter.currency = Currency.getInstance(currencyCode)
    return formatter.format(amount)
}

private fun localeForCurrency(currencyCode: String): Locale {
    val target = Currency.getInstance(currencyCode)
    val match = Locale.getAvailableLocales().firstOrNull { locale ->
        runCatching { Currency.getInstance(locale) }.getOrNull() == target
    }
    return match ?: Locale.UK
}
