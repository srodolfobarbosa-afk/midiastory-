package com.rdf112.sweep.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.rdf112.sweep.domain.billing.BillingStatePolicy
import com.rdf112.sweep.domain.billing.EntitlementDecision
import com.rdf112.sweep.domain.billing.EvaluatedPurchaseRecord
import com.rdf112.sweep.domain.billing.PlayPurchaseState
import com.rdf112.sweep.domain.billing.ProAccessSummaryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

data class PlayCatalogProductUi(
    val productId: String,
    val formattedPrice: String,
    val isAvailable: Boolean,
    val priceAmountMicros: Long? = null,
    val priceCurrencyCode: String? = null,
)

/**
 * Integração Google Play Billing Library 8.0.0 fail-closed:
 * - Consulta catálogo real via `queryProductDetailsAsync` (`productDetailsList` + `unfetchedProductList`).
 * - Inicia fluxo oficial via `launchBillingFlow`.
 * - Não concede acesso a partir de preferências legadas (`is_pro` ignorado na inicialização).
 * - Exige `PURCHASED = 1`, produto na allowlist (`sweep_pro_monthly`, `sweep_pro_annual`, `sweep_pro_lifetime`),
 *   `purchaseToken` não vazio e assinatura da Google Play presente (`purchase.signature` e `originalJson` não vazios).
 */
class AndroidPlayBillingGateway(context: Context) {
    private val appContext = context.applicationContext
    private val isConnecting = AtomicBoolean(false)

    // Inicializa sempre em estado não concedido até que o Play Billing confirme na sessão atual (fail-closed).
    private val _accessState = MutableStateFlow(ProAccessSummaryState.RECONCILIATION_REQUIRED)
    val accessState: StateFlow<ProAccessSummaryState> = _accessState.asStateFlow()

    private val _catalog = MutableStateFlow<Map<String, PlayCatalogProductUi>>(emptyMap())
    val catalog: StateFlow<Map<String, PlayCatalogProductUi>> = _catalog.asStateFlow()

    private val fetchedDetails = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    applyPurchases(subsOk = true, inAppOk = true, purchases = purchases)
                } else {
                    refreshExistingPurchases()
                }
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> refreshExistingPurchases()
            else -> Unit
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun startAndReconcile() {
        if (billingClient.isReady) {
            queryCatalogDetails()
            refreshExistingPurchases()
            return
        }
        if (!isConnecting.compareAndSet(false, true)) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                isConnecting.set(false)
                if (result.responseCode == BillingResponseCode.OK) {
                    queryCatalogDetails()
                    refreshExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting.set(false)
            }
        })
    }

    fun queryCatalogDetails() {
        if (!billingClient.isReady) return
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingStatePolicy.PRODUCT_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingStatePolicy.PRODUCT_ANNUAL)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { result: BillingResult, detailsResult: QueryProductDetailsResult ->
            if (result.responseCode == BillingResponseCode.OK) {
                updateCatalogMap(detailsResult)
            }
        }

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingStatePolicy.PRODUCT_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(inAppParams) { result: BillingResult, detailsResult: QueryProductDetailsResult ->
            if (result.responseCode == BillingResponseCode.OK) {
                updateCatalogMap(detailsResult)
            }
        }
    }

    private fun updateCatalogMap(detailsResult: QueryProductDetailsResult) {
        val next = _catalog.value.toMutableMap()
        for (pd in detailsResult.productDetailsList) {
            fetchedDetails[pd.productId] = pd
            val subPhase = pd.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.lastOrNull()
            val oneTime = pd.oneTimePurchaseOfferDetails
            val price = subPhase?.formattedPrice ?: oneTime?.formattedPrice ?: ""
            val micros = subPhase?.priceAmountMicros ?: oneTime?.priceAmountMicros
            val currency = subPhase?.priceCurrencyCode ?: oneTime?.priceCurrencyCode
            next[pd.productId] = PlayCatalogProductUi(
                productId = pd.productId,
                formattedPrice = price,
                isAvailable = price.isNotBlank(),
                priceAmountMicros = micros,
                priceCurrencyCode = currency,
            )
        }
        for (unfetched in detailsResult.unfetchedProductList) {
            if (!fetchedDetails.containsKey(unfetched.productId)) {
                next[unfetched.productId] = PlayCatalogProductUi(
                    productId = unfetched.productId,
                    formattedPrice = "",
                    isAvailable = false,
                )
            }
        }
        _catalog.value = next
    }

    fun launchPurchaseFlow(activity: Activity, productId: String = BillingStatePolicy.PRODUCT_ANNUAL): Boolean {
        if (!billingClient.isReady || productId !in BillingStatePolicy.RECOVERED_PLAY_PRODUCT_IDS) {
            startAndReconcile()
            return false
        }
        val details = fetchedDetails[productId] ?: run {
            queryCatalogDetails()
            return false
        }
        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken.isNullOrBlank()) return false
            paramsBuilder.setOfferToken(offerToken)
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(paramsBuilder.build()))
            .build()
        val res = billingClient.launchBillingFlow(activity, flowParams)
        return res.responseCode == BillingResponseCode.OK
    }

    fun refreshExistingPurchases() {
        if (!billingClient.isReady) return
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { subsResult, subsPurchases ->
            billingClient.queryPurchasesAsync(inAppParams) { inAppResult, inAppPurchases ->
                val subsOk = subsResult.responseCode == BillingResponseCode.OK
                val inAppOk = inAppResult.responseCode == BillingResponseCode.OK
                val combined = buildList {
                    if (subsOk) addAll(subsPurchases)
                    if (inAppOk) addAll(inAppPurchases)
                }
                applyPurchases(subsOk = subsOk, inAppOk = inAppOk, purchases = combined)
            }
        }
    }

    private fun applyPurchases(subsOk: Boolean, inAppOk: Boolean, purchases: List<Purchase>) {
        val evaluatedRecords = purchases.flatMap { purchase ->
            val hasPlaySignature = purchase.signature.isNotBlank() &&
                purchase.originalJson.isNotBlank() &&
                purchase.packageName == appContext.packageName
            val products = purchase.products.ifEmpty { listOf<String?>(null) }
            products.map { productId ->
                EvaluatedPurchaseRecord(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    rawPlayState = purchase.purchaseState,
                    isAcknowledged = purchase.isAcknowledged,
                    signaturePresentAndValid = hasPlaySignature,
                )
            }
        }

        for (purchase in purchases) {
            val hasPlaySignature = purchase.signature.isNotBlank() &&
                purchase.originalJson.isNotBlank() &&
                purchase.packageName == appContext.packageName
            val state = PlayPurchaseState.fromPlayValue(purchase.purchaseState)
            val eligible = hasPlaySignature && purchase.products.any { productId ->
                BillingStatePolicy.evaluate(
                    state = state,
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    verification = com.rdf112.sweep.domain.billing.PurchaseVerification.VERIFIED,
                ) == EntitlementDecision.GRANT
            }
            if (eligible && !purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { }
            }
        }

        _accessState.value = BillingStatePolicy.reconcilePurchases(
            subsQuerySucceeded = subsOk,
            inAppQuerySucceeded = inAppOk,
            purchases = evaluatedRecords,
        )
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
