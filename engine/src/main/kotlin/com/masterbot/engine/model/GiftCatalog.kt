package com.masterbot.engine.model

import org.yaml.snakeyaml.Yaml
import kotlin.math.roundToInt

data class Gift(
    val id: String,
    val name: String,
    val emoji: String,
    val priceInCurrency: Double,
    val coinCost: Int,
)

data class GiftCatalog(
    val version: Int,
    val currency: String,
    val coinsPerCurrencyUnit: Int,
    val gifts: List<Gift>,
) {
    companion object {
        /** Parses rules/gift_catalog.yaml -- the only place gift prices should ever be read from. */
        @Suppress("UNCHECKED_CAST")
        fun parse(yamlText: String): GiftCatalog {
            val root = Yaml().load<Map<String, Any>>(yamlText)

            val coinsPerCurrencyUnit = (root["coins_per_currency_unit"] as Number).toInt()
            val giftMaps = root["gifts"] as List<Map<String, Any>>
            val gifts = giftMaps.map { g ->
                val price = (g["price"] as Number).toDouble()
                Gift(
                    id = g["id"] as String,
                    name = g["name"] as String,
                    emoji = g["emoji"] as String,
                    priceInCurrency = price,
                    coinCost = (price * coinsPerCurrencyUnit).roundToInt(),
                )
            }

            return GiftCatalog(
                version = (root["version"] as Number).toInt(),
                currency = root["currency"] as String,
                coinsPerCurrencyUnit = coinsPerCurrencyUnit,
                gifts = gifts,
            )
        }
    }
}
