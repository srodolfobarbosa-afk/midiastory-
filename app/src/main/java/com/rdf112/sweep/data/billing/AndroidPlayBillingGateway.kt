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
import com.rdf112.sweep.domain.billing.EvaluatedPurchaseRecord
import com.rdf112.sweep.domain.billing.ProAccessSummaryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class PlayCatalogProductUi(
    val productId: String,
    val formattedPrice: String,
    val isAvailable: Boolean,
    val priceAmountMicros: Long? = null,
    val priceCurrencyCode: String? = null,
)

/**
 * Integração Google Play Billing Library 8 fail-closed com verificação server-side no Supabase:
 * - Consulta catálogo real via `queryProductDetailsAsync` (`productDetailsList` + `unfetchedProductList`).
 * - Inicia fluxo oficial via `launchBillingFlow`.
 * - Não concede acesso a partir de preferências legadas (`is_pro` ignorado na inicialização).
 * - Exige `PURCHASED = 1`, produto na allowlist (`sweep_pro_monthly`, `sweep_pro_annual`, `sweep_pro_lifetime`),
 *   `purchaseToken` não vazio e verificação criptográfica/API no backend Supabase antes de liberar Pro
 *   e fazer `acknowledgePurchase`.
 */
class AndroidPlayBillingGateway(context: Context) {
    private val appContext = context.applicationContext
    private val isConnecting = AtomicBoolean(false)
    private val verificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val verifiedTokensCache = ConcurrentHashMap<String, Boolean>()

    // Inicializa sempre em estado não concedido até que o Play Billing + Backend confirmem na sessão atual (fail-closed).
    private val _accessState = MutableStateFlow(ProAccessSummaryState.RECONCILIATION_REQUIRED)
    val accessState: StateFlow<ProAccessSummaryState> = _accessState.asStateFlow()

    private val _catalog = MutableStateFlow<Map<String, PlayCatalogProductUi>>(emptyMap())
    val catalog: StateFlow<Map<String, PlayCatalogProductUi>> = _catalog.asStateFlow()

    private val fetchedDetails = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, _ ->
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> refreshExistingPurchases()
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
            val oneTime = pd.oneTimePurchaseOfferDetails ?: pd.oneTimePurchaseOfferDetailsList?.firstOrNull()
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

    /** Purchase verification backend is active via Supabase Edge Function (`verify-play-purchase`). */
    fun securePurchaseVerificationAvailable(): Boolean = PURCHASE_VERIFICATION_BACKEND_CONFIGURED

    fun launchPurchaseFlow(activity: Activity, productId: String = BillingStatePolicy.PRODUCT_ANNUAL): Boolean {
        if (!PURCHASE_VERIFICATION_BACKEND_CONFIGURED) return false
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
        } else if (details.productType == BillingClient.ProductType.INAPP) {
            val offerToken = (details.oneTimePurchaseOfferDetails
                ?: details.oneTimePurchaseOfferDetailsList?.firstOrNull())?.offerToken
            if (!offerToken.isNullOrBlank()) {
                paramsBuilder.setOfferToken(offerToken)
            }
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
        if (purchases.isEmpty()) {
            _accessState.value = BillingStatePolicy.reconcilePurchases(
                subsQuerySucceeded = subsOk,
                inAppQuerySucceeded = inAppOk,
                purchases = emptyList(),
            )
            return
        }

        verificationScope.launch {
            val evaluatedRecords = mutableListOf<EvaluatedPurchaseRecord>()
            for (purchase in purchases) {
                val products = purchase.products.ifEmpty { listOf<String?>(null) }
                for (productId in products) {
                    val token = purchase.purchaseToken
                    val isPurchasedState = purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    val isVerified = if (isPurchasedState && !productId.isNullOrBlank() && token.isNotBlank()) {
                        verifyTokenWithBackend(productId, token).also { verified ->
                            if (verified && !purchase.isAcknowledged && billingClient.isReady) {
                                val ackParams = AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(token)
                                    .build()
                                billingClient.acknowledgePurchase(ackParams) { }
                            }
                        }
                    } else {
                        false
                    }
                    evaluatedRecords += EvaluatedPurchaseRecord(
                        productId = productId,
                        purchaseToken = token,
                        rawPlayState = purchase.purchaseState,
                        isAcknowledged = purchase.isAcknowledged,
                        verifiedByTrustedBackend = isVerified,
                    )
                }
            }

            _accessState.value = BillingStatePolicy.reconcilePurchases(
                subsQuerySucceeded = subsOk,
                inAppQuerySucceeded = inAppOk,
                purchases = evaluatedRecords,
            )
        }
    }

    private fun verifyTokenWithBackend(productId: String, purchaseToken: String): Boolean {
        val cacheKey = "$productId:$purchaseToken"
        verifiedTokensCache[cacheKey]?.let { return it }

        return runCatching {
            val connection = (URL(VERIFICATION_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val payload = JSONObject().apply {
                put("packageName", CANONICAL_PACKAGE_NAME)
                put("productId", productId)
                put("purchaseToken", purchaseToken)
            }.toString()

            connection.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return false
            }
            val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            connection.disconnect()

            val json = JSONObject(responseText)
            val verified = json.optBoolean("verified", false) && json.optBoolean("entitlementActive", false)
            if (verified) {
                verifiedTokensCache[cacheKey] = true
            }
            verified
        }.getOrDefault(false)
    }

    companion object {
        private const val PURCHASE_VERIFICATION_BACKEND_CONFIGURED = true
        private const val CANONICAL_PACKAGE_NAME = "com.rdf112.midiastore"
        private const val VERIFICATION_ENDPOINT =
            "https://usmqtnlsaavxqguwrtix.supabase.co/functions/v1/verify-play-purchase"
    }

    fun endConnection() {
        verificationScope.cancel()
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
