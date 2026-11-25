package com.rdf112.sweep.domain.billing

data class RawPricingPhaseCandidate(
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val billingPeriodIso: String,
)

data class RawSubscriptionOfferCandidate(
    val offerId: String?,
    val offerToken: String,
    val phases: List<RawPricingPhaseCandidate>,
)

data class SelectedSubscriptionOffer(
    val formattedRecurringPrice: String,
    val recurringPriceMicros: Long?,
    val priceCurrencyCode: String?,
    val hasFreeTrialOffer: Boolean,
    val freeTrialPeriodIso: String?,
    val preferredOfferToken: String?,
)

/**
 * Pure Google Play Subscriptions offer selector enforcing price anchoring & Play Billing policy:
 * - Free Trial (`priceAmountMicros == 0L`, e.g. `P3D` / `annual-trial-3d`) is selected ONLY for
 *   `BillingStatePolicy.PRODUCT_ANNUAL` when Google Play returns the offer as eligible for the account.
 * - `PRODUCT_MONTHLY` always selects the paid base offer (no trial), anchoring conversions to Annual.
 * - Returning subscribers ineligible for trial automatically fall back to the paid base offer with
 *   `hasFreeTrialOffer = false` so trial CTAs are never shown to ineligible users.
 */
object SubscriptionOfferSelector {
    const val PREFERRED_ANNUAL_TRIAL_OFFER_ID = "annual-trial-3d"

    fun selectBestOffer(
        productId: String,
        offers: List<RawSubscriptionOfferCandidate>,
    ): SelectedSubscriptionOffer? {
        if (offers.isEmpty()) return null
        val validOffers = offers.filter { it.offerToken.isNotBlank() && it.phases.isNotEmpty() }
        if (validOffers.isEmpty()) return null

        val allowFreeTrial = productId == BillingStatePolicy.PRODUCT_ANNUAL

        if (allowFreeTrial) {
            val trialCandidates = validOffers.filter { offer ->
                offer.phases.any { it.priceAmountMicros == 0L } &&
                    offer.phases.last().priceAmountMicros > 0L
            }
            val chosenTrial = trialCandidates.firstOrNull {
                it.offerId.equals(PREFERRED_ANNUAL_TRIAL_OFFER_ID, ignoreCase = true)
            } ?: trialCandidates.firstOrNull { offer ->
                offer.phases.any { it.priceAmountMicros == 0L && it.billingPeriodIso.equals("P3D", ignoreCase = true) }
            } ?: trialCandidates.firstOrNull()

            if (chosenTrial != null) {
                val freePhase = chosenTrial.phases.first { it.priceAmountMicros == 0L }
                val paidPhase = chosenTrial.phases.last()
                return SelectedSubscriptionOffer(
                    formattedRecurringPrice = paidPhase.formattedPrice,
                    recurringPriceMicros = paidPhase.priceAmountMicros,
                    priceCurrencyCode = paidPhase.priceCurrencyCode,
                    hasFreeTrialOffer = true,
                    freeTrialPeriodIso = freePhase.billingPeriodIso.ifBlank { "P3D" },
                    preferredOfferToken = chosenTrial.offerToken,
                )
            }
        }

        val basePaidOffer = validOffers.firstOrNull { offer ->
            offer.phases.none { it.priceAmountMicros == 0L } &&
                offer.phases.last().priceAmountMicros > 0L
        } ?: validOffers.first()

        val paidPhase = basePaidOffer.phases.last()
        return SelectedSubscriptionOffer(
            formattedRecurringPrice = paidPhase.formattedPrice,
            recurringPriceMicros = paidPhase.priceAmountMicros,
            priceCurrencyCode = paidPhase.priceCurrencyCode,
            hasFreeTrialOffer = false,
            freeTrialPeriodIso = null,
            preferredOfferToken = basePaidOffer.offerToken,
        )
    }
}
