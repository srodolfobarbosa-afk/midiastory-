package com.rdf112.sweep.presentation.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class GlobalHubToolsTest {

    @Test
    fun `convertCurrency converts BRL INR JPY accurately using baseline rates`() {
        val rates = mapOf("USD" to 1.0, "BRL" to 5.0, "INR" to 80.0, "JPY" to 150.0)
        assertEquals(500.0, GlobalHubTools.convertCurrency(100.0, "USD", "BRL", rates)!!, 0.0001)
        assertEquals(1600.0, GlobalHubTools.convertCurrency(100.0, "BRL", "INR", rates)!!, 0.0001)
        assertEquals(187.5, GlobalHubTools.convertCurrency(100.0, "INR", "JPY", rates)!!, 0.0001)
        assertNull(GlobalHubTools.convertCurrency(10.0, "USD", "UNKNOWN", rates))
        assertNull(GlobalHubTools.convertCurrency(Double.NaN, "USD", "BRL", rates))
    }

    @Test
    fun `formatCountryClock computes exact UTC offset and delta across hemispheres`() {
        val india = GlobalHubTools.COUNTRIES.first { it.id == "IN" }
        val brazil = GlobalHubTools.COUNTRIES.first { it.id == "BR" }
        val utcTz = TimeZone.getTimeZone("UTC")
        // Fixed mid-year UTC instant (no daylight saving variance in Brazil/India)
        val fixedUtcMillis = 1_780_000_000_000L

        val inClock = GlobalHubTools.formatCountryClock(india, utcTz, fixedUtcMillis)
        assertEquals("UTC+5:30", inClock.utcOffsetLabel)
        assertEquals(330, inClock.deltaMinutes)

        val brClock = GlobalHubTools.formatCountryClock(brazil, utcTz, fixedUtcMillis)
        assertEquals("UTC-3", brClock.utcOffsetLabel)
        assertEquals(-180, brClock.deltaMinutes)

        val indiaFromBrazil = GlobalHubTools.formatCountryClock(
            india,
            TimeZone.getTimeZone("America/Sao_Paulo"),
            fixedUtcMillis,
        )
        assertEquals(510, indiaFromBrazil.deltaMinutes)
        assertEquals("+8h30", GlobalHubTools.formatOffsetDelta(indiaFromBrazil.deltaMinutes))
    }

    @Test
    fun `global country catalog contains top global population hubs`() {
        val ids = GlobalHubTools.COUNTRIES.map { it.id }.toSet()
        assertTrue(ids.containsAll(listOf("BR", "IN", "US", "AE", "SA", "RU", "CN", "JP", "ID", "BD", "KR")))
        GlobalHubTools.COUNTRIES.forEach { country ->
            assertNotNull(TimeZone.getTimeZone(country.timeZoneId))
            assertTrue(country.landmarksEn.isNotEmpty())
            assertTrue(country.landmarksPt.isNotEmpty())
        }
    }
}
