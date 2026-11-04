package com.rdf112.sweep.domain.billing

import kotlin.test.Test
import kotlin.test.assertEquals

class BillingStatePolicyTest {
    private val allowed = BillingStatePolicy.RECOVERED_PLAY_PRODUCT_IDS

    @Test
    fun mapsOnlyDocumentedPlayStateValues() {
        assertEquals(PlayPurchaseState.UNSPECIFIED, PlayPurchaseState.fromPlayValue(0))
        assertEquals(PlayPurchaseState.PURCHASED, PlayPurchaseState.fromPlayValue(1))
        assertEquals(PlayPurchaseState.PENDING, PlayPurchaseState.fromPlayValue(2))
        assertEquals(PlayPurchaseState.UNKNOWN, PlayPurchaseState.fromPlayValue(4))
        assertEquals(PlayPurchaseState.UNKNOWN, PlayPurchaseState.fromPlayValue(-1))
    }

    @Test
    fun pendingNeverGrantsEntitlement() {
        assertEquals(
            EntitlementDecision.WAIT_FOR_PAYMENT,
            BillingStatePolicy.evaluate(PlayPurchaseState.PENDING, BillingStatePolicy.PRODUCT_ANNUAL, "token", allowed, PurchaseVerification.VERIFIED),
        )
    }

    @Test
    fun unspecifiedAndUnknownMustBeReconciled() {
        for (state in listOf(PlayPurchaseState.UNSPECIFIED, PlayPurchaseState.UNKNOWN)) {
            assertEquals(
                EntitlementDecision.RECONCILE,
                BillingStatePolicy.evaluate(state, BillingStatePolicy.PRODUCT_ANNUAL, "token", allowed, PurchaseVerification.VERIFIED),
            )
        }
    }

    @Test
    fun purchasedNeedsAllowlistedProductTokenAndVerification() {
        fun evaluate(product: String?, token: String?, verification: PurchaseVerification) =
            BillingStatePolicy.evaluate(PlayPurchaseState.PURCHASED, product, token, allowed, verification)

        assertEquals(EntitlementDecision.REJECT, evaluate("not_in_catalog", "token", PurchaseVerification.VERIFIED))
        assertEquals(EntitlementDecision.REJECT, evaluate(BillingStatePolicy.PRODUCT_ANNUAL, " ", PurchaseVerification.VERIFIED))
        assertEquals(EntitlementDecision.VERIFY, evaluate(BillingStatePolicy.PRODUCT_ANNUAL, "token", PurchaseVerification.NOT_VERIFIED))
        assertEquals(EntitlementDecision.REJECT, evaluate(BillingStatePolicy.PRODUCT_ANNUAL, "token", PurchaseVerification.REJECTED))
        assertEquals(EntitlementDecision.GRANT, evaluate(BillingStatePolicy.PRODUCT_ANNUAL, "token", PurchaseVerification.VERIFIED))
        assertEquals(EntitlementDecision.GRANT, evaluate(BillingStatePolicy.PRODUCT_MONTHLY, "token", PurchaseVerification.VERIFIED))
        assertEquals(EntitlementDecision.GRANT, evaluate(BillingStatePolicy.PRODUCT_LIFETIME, "token", PurchaseVerification.VERIFIED))
    }

    @Test
    fun reconcilePurchasesDistinguishesQueryFailureFromEmptyPurchasesAndRejectsStateFourBug() {
        val stateFourBug = EvaluatedPurchaseRecord(
            productId = BillingStatePolicy.PRODUCT_ANNUAL,
            purchaseToken = "tok-4",
            rawPlayState = 4,
            isAcknowledged = true,
            verifiedByTrustedBackend = true,
        )
        assertEquals(
            ProAccessSummaryState.RECONCILIATION_REQUIRED,
            BillingStatePolicy.reconcilePurchases(true, true, listOf(stateFourBug)),
        )
        assertEquals(
            ProAccessSummaryState.RECONCILIATION_REQUIRED,
            BillingStatePolicy.reconcilePurchases(false, true, emptyList()),
        )
        assertEquals(
            ProAccessSummaryState.FREE_NO_ACTIVE_PURCHASE,
            BillingStatePolicy.reconcilePurchases(true, true, emptyList()),
        )
        val unverifiedSignature = EvaluatedPurchaseRecord(
            productId = BillingStatePolicy.PRODUCT_LIFETIME,
            purchaseToken = "tok-1",
            rawPlayState = 1,
            isAcknowledged = true,
            verifiedByTrustedBackend = false,
        )
        assertEquals(
            ProAccessSummaryState.RECONCILIATION_REQUIRED,
            BillingStatePolicy.reconcilePurchases(true, true, listOf(unverifiedSignature)),
        )
        val validPurchase = EvaluatedPurchaseRecord(
            productId = BillingStatePolicy.PRODUCT_LIFETIME,
            purchaseToken = "tok-1",
            rawPlayState = 1,
            isAcknowledged = true,
            verifiedByTrustedBackend = true,
        )
        assertEquals(
            ProAccessSummaryState.PRO_GRANTED,
            BillingStatePolicy.reconcilePurchases(true, true, listOf(validPurchase)),
        )
    }

    @Test
    fun calculatesRealAnnualDiscountOnlyWhenSameCurrencyAndSavingsExist() {
        val discount = BillingStatePolicy.calculateRealAnnualDiscountPercent(
            monthlyPriceMicros = 9_900_000L,
            annualPriceMicros = 59_900_000L,
            monthlyCurrencyCode = "BRL",
            annualCurrencyCode = "BRL",
        )
        assertEquals(50, discount)

        val mismatchCurrency = BillingStatePolicy.calculateRealAnnualDiscountPercent(
            monthlyPriceMicros = 9_900_000L,
            annualPriceMicros = 59_900_000L,
            monthlyCurrencyCode = "BRL",
            annualCurrencyCode = "USD",
        )
        assertEquals(null, mismatchCurrency)
    }
}
