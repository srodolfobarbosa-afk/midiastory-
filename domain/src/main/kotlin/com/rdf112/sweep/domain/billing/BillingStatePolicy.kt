package com.rdf112.sweep.domain.billing

/** Maps the documented Play PurchaseState values without guessing unknown integers. */
enum class PlayPurchaseState {
    PURCHASED,
    PENDING,
    UNSPECIFIED,
    UNKNOWN;

    companion object {
        fun fromPlayValue(value: Int): PlayPurchaseState = when (value) {
            1 -> PURCHASED
            2 -> PENDING
            0 -> UNSPECIFIED
            else -> UNKNOWN
        }
    }
}

enum class PurchaseVerification { VERIFIED, NOT_VERIFIED, REJECTED }
enum class EntitlementDecision { GRANT, WAIT_FOR_PAYMENT, VERIFY, RECONCILE, REJECT }

data class EvaluatedPurchaseRecord(
    val productId: String?,
    val purchaseToken: String?,
    val rawPlayState: Int,
    val isAcknowledged: Boolean,
    /** True only after a trusted server verifies this exact purchase token with Google Play. */
    val verifiedByTrustedBackend: Boolean,
) {
    val verification: PurchaseVerification
        get() = if (verifiedByTrustedBackend) PurchaseVerification.VERIFIED else PurchaseVerification.NOT_VERIFIED
}

enum class ProAccessSummaryState {
    FREE_NO_ACTIVE_PURCHASE,
    PENDING_PAYMENT,
    PRO_GRANTED,
    RECONCILIATION_REQUIRED,
}

/**
 * Pure fail-closed policy.
 * Product IDs recovered from the published AAB (com.rdf112.sweep) via R8/DEX inspection.
 * Never grants Pro from unverified legacy SharedPreferences flags.
 */
object BillingStatePolicy {
    const val PRODUCT_MONTHLY = "sweep_pro_monthly"
    const val PRODUCT_ANNUAL = "sweep_pro_annual"
    const val PRODUCT_LIFETIME = "sweep_pro_lifetime"

    val RECOVERED_PLAY_PRODUCT_IDS: Set<String> = setOf(
        PRODUCT_MONTHLY,
        PRODUCT_ANNUAL,
        PRODUCT_LIFETIME,
    )

    fun evaluate(
        state: PlayPurchaseState,
        productId: String?,
        purchaseToken: String?,
        allowlistedProductIds: Set<String> = RECOVERED_PLAY_PRODUCT_IDS,
        verification: PurchaseVerification,
    ): EntitlementDecision = when (state) {
        PlayPurchaseState.PENDING -> EntitlementDecision.WAIT_FOR_PAYMENT
        PlayPurchaseState.UNSPECIFIED, PlayPurchaseState.UNKNOWN -> EntitlementDecision.RECONCILE
        PlayPurchaseState.PURCHASED -> when {
            productId.isNullOrBlank() || productId !in allowlistedProductIds -> EntitlementDecision.REJECT
            purchaseToken.isNullOrBlank() -> EntitlementDecision.REJECT
            verification == PurchaseVerification.VERIFIED -> EntitlementDecision.GRANT
            verification == PurchaseVerification.NOT_VERIFIED -> EntitlementDecision.VERIFY
            else -> EntitlementDecision.REJECT
        }
    }

    /**
     * Reconciles a batch of active purchase records returned by Play Billing.
     * Query failures (subsQuerySucceeded = false or inAppQuerySucceeded = false) request
     * reconciliation rather than silently revoking or granting unverified access.
     */
    fun reconcilePurchases(
        subsQuerySucceeded: Boolean,
        inAppQuerySucceeded: Boolean,
        purchases: Collection<EvaluatedPurchaseRecord>,
        allowlistedProductIds: Set<String> = RECOVERED_PLAY_PRODUCT_IDS,
    ): ProAccessSummaryState {
        if (!subsQuerySucceeded || !inAppQuerySucceeded) {
            return ProAccessSummaryState.RECONCILIATION_REQUIRED
        }
        val decisions = purchases.map { record ->
            evaluate(
                state = PlayPurchaseState.fromPlayValue(record.rawPlayState),
                productId = record.productId,
                purchaseToken = record.purchaseToken,
                allowlistedProductIds = allowlistedProductIds,
                verification = record.verification,
            )
        }
        return when {
            decisions.any { it == EntitlementDecision.GRANT } -> ProAccessSummaryState.PRO_GRANTED
            decisions.any { it == EntitlementDecision.WAIT_FOR_PAYMENT } -> ProAccessSummaryState.PENDING_PAYMENT
            decisions.any { it == EntitlementDecision.RECONCILE || it == EntitlementDecision.VERIFY } ->
                ProAccessSummaryState.RECONCILIATION_REQUIRED
            else -> ProAccessSummaryState.FREE_NO_ACTIVE_PURCHASE
        }
    }

    /**
     * Calcula o percentual real de economia do plano anual em relação a 12 meses do plano mensal,
     * usando exclusivamente os valores reais em micros (priceAmountMicros) retornados pelo Google Play Billing
     * na mesma moeda. Retorna null se os preços reais não estiverem disponíveis ou não houver desconto.
     */
    fun calculateRealAnnualDiscountPercent(
        monthlyPriceMicros: Long?,
        annualPriceMicros: Long?,
        monthlyCurrencyCode: String?,
        annualCurrencyCode: String?,
    ): Int? {
        if (monthlyPriceMicros == null || annualPriceMicros == null) return null
        if (monthlyPriceMicros <= 0L || annualPriceMicros <= 0L) return null
        if (monthlyCurrencyCode.isNullOrBlank() || annualCurrencyCode.isNullOrBlank()) return null
        if (!monthlyCurrencyCode.equals(annualCurrencyCode, ignoreCase = true)) return null
        val yearlyBaselineMicros = try {
            Math.multiplyExact(monthlyPriceMicros, 12L)
        } catch (_: ArithmeticException) {
            return null
        }
        if (annualPriceMicros >= yearlyBaselineMicros) return null
        val savingsRatio = (yearlyBaselineMicros - annualPriceMicros).toDouble() / yearlyBaselineMicros.toDouble()
        val percent = Math.round(savingsRatio * 100.0).toInt()
        return percent.takeIf { it in 1..95 }
    }
}
