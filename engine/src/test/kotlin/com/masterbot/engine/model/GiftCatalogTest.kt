package com.masterbot.engine.model

import com.masterbot.engine.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GiftCatalogTest {

    @Test
    fun `parses the real gift_catalog yaml with correct coin costs`() {
        val catalog = TestFixtures.loadRealGiftCatalog()

        assertEquals(1, catalog.version)
        assertEquals("AED", catalog.currency)
        assertEquals(100, catalog.coinsPerCurrencyUnit)
        assertTrue(catalog.gifts.isNotEmpty())

        fun gift(id: String) = catalog.gifts.find { it.id == id } ?: error("expected gift '$id' in catalog")

        assertEquals(3.0, gift("karak-chai").priceInCurrency)
        assertEquals(300, gift("karak-chai").coinCost)

        assertEquals(15.0, gift("shawarma").priceInCurrency)
        assertEquals(1500, gift("shawarma").coinCost)

        assertEquals(25.0, gift("mandi").priceInCurrency)
        assertEquals(2500, gift("mandi").coinCost)

        assertEquals(40.0, gift("movie-ticket").priceInCurrency)
        assertEquals(4000, gift("movie-ticket").coinCost)
    }

    @Test
    fun `every gift id is unique`() {
        val catalog = TestFixtures.loadRealGiftCatalog()
        val ids = catalog.gifts.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "gift ids must be unique")
    }

    @Test
    fun `coin cost is always price times the currency rate`() {
        val catalog = TestFixtures.loadRealGiftCatalog()
        catalog.gifts.forEach { gift ->
            val expected = Math.round(gift.priceInCurrency * catalog.coinsPerCurrencyUnit).toInt()
            assertEquals(expected, gift.coinCost, "coin cost mismatch for ${gift.id}")
        }
    }
}
