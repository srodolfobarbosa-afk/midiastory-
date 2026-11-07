# Midiastore plan matrix and Brazil pricing recommendation

## Feature split

| Plan | Payment | Included features |
|---|---|---|
| Free | No payment | Scan for large media files; review candidates and approve each deletion through Android |
| Pro Monthly | Recurring monthly subscription | Exact-copy and older-file cleanup tools, unused-app suggestions with Android uninstall confirmation, file browsing, and cleanup report export |
| Pro Annual | Recurring annual subscription | The same Pro tools as Monthly |
| Pro Lifetime | One-time purchase | The same Pro tools without renewal |

Monthly, annual, and lifetime are purchase terms for the same Pro entitlement. The app must not claim an annual discount unless the prices returned by Google Play confirm it.

## Suggested Brazil prices

| Product ID | Suggested price | Product type |
|---|---:|---|
| `sweep_pro_monthly` | R$ 14,90 per month | Subscription |
| `sweep_pro_annual` | R$ 79,90 per year | Subscription |
| `sweep_pro_lifetime` | R$ 149,90 once | One-time product |

This is an initial pricing recommendation, not a Play Console configuration. It positions the annual plan below a R$ 14,90 monthly renewal total of R$ 178,80, while placing lifetime near two annual terms. Public comparison points include Avast's Brazilian store listing annual cleanup prices around R$ 99–119 and CCleaner's Brazilian App Store listing subscriptions around R$ 19,90 and annual offers around R$ 119,90. These are directional anchors; they are not a verified Android Play price quote for the Midiastore account.

Google Play is the source of the charged price. Configure these amounts and subscription base plans/offers in Play Console; the app reads and displays the localized price from Play Billing rather than hard-coding R$ values. Keep the three product IDs and types aligned with the source.

## Release and purchase gates

The app shows the Brazil amounts as **proposed, not active** until the Play catalog returns actual localized prices. These recommendations are not configured in Play Console.

- The app currently blocks purchase buttons until a trusted purchase-token verification backend is configured. A local Pro preview exists only in a debuggable build and does not simulate payment.
- Before enabling real purchases, configure products and license testers in Play Console, deploy server-side verification/reconciliation, and test purchase, pending, cancelled, restore, acknowledgement, refund, and expiry paths.
- The app does not request `MANAGE_EXTERNAL_STORAGE`. Android media permissions limit scanning to the media the user grants; deletions still require Android's consent flow.
- Unused-app suggestions require the user to grant Usage Access in Android Settings. Midiastore only launches the system uninstall confirmation; it never uninstalls apps automatically.
- The Home share action uses Android's Sharesheet and the Google Play listing link. It does not post to social networks automatically; referral attribution/payouts are a separate future service, not a Google Play commission feature.
- Current in-app language resources are English and Brazilian Portuguese. Additional world languages and localized store listings still need reviewed translations.

## Public reference points checked 2026-11-07

- [Avast Brazil store](https://www.avast.com/pt-br/store)
- [CCleaner Brazil App Store listing](https://apps.apple.com/br/app/ccleaner-limpeza-de-celular/id6450901367)
