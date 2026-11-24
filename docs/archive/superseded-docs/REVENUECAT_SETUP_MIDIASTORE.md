# Guia de Integração RevenueCat + Google Play + Supabase

Este documento define a arquitetura de integração do **RevenueCat** no ecossistema **Midiastore (`com.rdf112.midiastore`)** para gestão de assinaturas, testes A/B de paywall e sincronização em tempo real com o Supabase.

---

## 1. Por que RevenueCat + Google Play?
1. **Paywalls Dinâmicos Remotos:** Permite alterar layouts de telas de venda e preços diretamente pelo painel web do RevenueCat sem precisar enviar novas versões do app para aprovação na Play Store.
2. **Webhooks em Tempo Real para o Supabase:** A cada compra, renovação, cancelamento ou expiração de assinatura, o RevenueCat envia um webhook JSON para a nossa Supabase Edge Function (`supabase/functions/revenuecat-webhook`), que atualiza o status Pro do usuário no Postgres instantaneamente.
3. **Compatibilidade Nativa:** Funciona em cima da Google Play Billing Library v7/v8 já presente no aplicativo.

---

## 2. Dependência no Android (`app/build.gradle.kts`)

```kotlin
// RevenueCat SDK com suporte nativo a Compose e Android Views
implementation("com.revenuecat.purchases:purchases:8.12.2")
```

---

## 3. Catálogo de Produtos Vinculado

| Identificador no RevenueCat (Entitlement: `pro`) | Product ID no Google Play Console | Tipo |
| :--- | :--- | :--- |
| `monthly` | `sweep_pro_monthly` | Assinatura Mensal (SUBS) |
| `annual` | `sweep_pro_annual` | Assinatura Anual (SUBS) |
| `lifetime` | `sweep_pro_lifetime` | Compra Única Vitalícia (INAPP) |

---

## 4. Inicialização Segura no `Application` ou `MainActivity`

```kotlin
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

Purchases.configure(
    PurchasesConfiguration.Builder(context, "goog_api_key_publica_aqui")
        .appUserID(userIdOrNull) // ID do usuário autenticado ou UUID anônimo
        .build()
)
```

---

## 5. Webhook RevenueCat -> Supabase Edge Function

URL configurada no painel do RevenueCat:
`https://usmqtnlsaavxqguwrtix.supabase.co/functions/v1/revenuecat-webhook`

Eventos tratados:
- `INITIAL_PURCHASE`: Ativa status Pro e grava em `public.beta_testers` ou tabela de assinantes.
- `RENEWAL`: Renova a data de expiração.
- `CANCELLATION` / `EXPIRATION`: Revoga acesso Pro quando o ciclo terminar.
