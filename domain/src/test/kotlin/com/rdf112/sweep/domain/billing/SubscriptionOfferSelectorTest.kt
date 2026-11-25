package com.rdf112.sweep.domain.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionOfferSelectorTest {

    private val baseAnnualOffer = RawSubscriptionOfferCandidate(
        offerId = null,
        offerToken = "token-annual-base",
        phases = listOf(
            RawPricingPhaseCandidate(
                formattedPrice = "R$ 79,99",
                priceAmountMicros = 79_990_000L,
                priceCurrencyCode = "BRL",
                billingPeriodIso = "P1Y",
            ),
        ),
    )

    private val trial3dAnnualOffer = RawSubscriptionOfferCandidate(
        offerId = "annual-trial-3d",
        offerToken = "token-annual-trial-3d",
        phases = listOf(
            RawPricingPhaseCandidate(
                formattedPrice = "Grátis",
                priceAmountMicros = 0L,
                priceCurrencyCode = "BRL",
                billingPeriodIso = "P3D",
            ),
            RawPricingPhaseCandidate(
                formattedPrice = "R$ 79,99",
                priceAmountMicros = 79_990_000L,
                priceCurrencyCode = "BRL",
                billingPeriodIso = "P1Y",
            ),
        ),
    )

    @Test
    fun `selects 3-day free trial offer exclusively for eligible annual plan`() {
        val selected = SubscriptionOfferSelector.selectBestOffer(
            productId = BillingStatePolicy.PRODUCT_ANNUAL,
            offers = listOf(baseAnnualOffer, trial3dAnnualOffer),
        )
        assertNotNull(selected)
        assertTrue(selected!!.hasFreeTrialOffer)
        assertEquals("P3D", selected.freeTrialPeriodIso)
        assertEquals("token-annual-trial-3d", selected.preferredOfferToken)
        assertEquals("R$ 79,99", selected.formattedRecurringPrice)
        assertEquals(79_990_000L, selected.recurringPriceMicros)
    }

    @Test
    fun `falls back to paid annual base offer when user is ineligible for trial`() {
        val selected = SubscriptionOfferSelector.selectBestOffer(
            productId = BillingStatePolicy.PRODUCT_ANNUAL,
            offers = listOf(baseAnnualOffer),
        )
        assertNotNull(selected)
        assertFalse(selected!!.hasFreeTrialOffer)
        assertNull(selected.freeTrialPeriodIso)
        assertEquals("token-annual-base", selected.preferredOfferToken)
        assertEquals("R$ 79,99", selected.formattedRecurringPrice)
    }

    @Test
    fun `never selects free trial on monthly plan to preserve annual anchoring`() {
        val accidentalMonthlyTrial = trial3dAnnualOffer.copy(
            offerId = "monthly-trial",
            offerToken = "token-monthly-trial",
        )
        val baseMonthly = RawSubscriptionOfferCandidate(
            offerId = null,
            offerToken = "token-monthly-base",
            phases = listOf(
                RawPricingPhaseCandidate(
                    formattedPrice = "R$ 14,99",
                    priceAmountMicros = 14_990_000L,
                    priceCurrencyCode = "BRL",
                    billingPeriodIso = "P1M",
                ),
            ),
        )
        val selected = SubscriptionOfferSelector.selectBestOffer(
            productId = BillingStatePolicy.PRODUCT_MONTHLY,
            offers = listOf(accidentalMonthlyTrial, baseMonthly),
        )
        assertNotNull(selected)
        assertFalse(selected!!.hasFreeTrialOffer)
        assertEquals("token-monthly-base", selected.preferredOfferToken)
        assertEquals("R$ 14,99", selected.formattedRecurringPrice)
    }
}
