# ADR 0005: Direct Boot a šifrovaný profil

## Stav

- **Rozhodnutí:** Handy v současné fázi **nepodporuje Direct Boot** (běh před prvním odemčením uživatele po startu zařízení).
- Foreground služba (`EarService`), TTS, přístup k `EncryptedSharedPreferences` (profil hlasu) a většina systémových akcí předpokládá běžný uživatelský kontext po odemknutí.

## Důvody

1. **EncryptedSharedPreferences** — klíče pro ECAPA embedding jsou vázané na credential lock; bez user unlock nejsou dostupné konzistentně napříč OEM.
2. **Oprávnění a UI** — onboarding a destruktivní confirm gate vyžadují interakci; Direct Boot flow by vyžadoval oddělený „minimum“ režim a audit všech intentů.
3. **Náklad vs přínos** — cílový uživatel má často pomocníka k odemknutí; priorita je spolehlivost po unlock, ne před.

## Důsledky

- Po rebootu, dokud uživatel alespoň jednou neodemkne zařízení, Handy neinicializuje plnou pipeline (stejně jako dnes závislost na `BOOT_COMPLETED` + běžný kontext).
- Dokumentace a QA matrix explicitně uvažují „po prvním unlock“ jako předpoklad pro hands-free režim.

## Budoucí náhled (měkká opc)

 Pokud vznikne požadavek na **částečný** Direct Boot:

- Oddělit „wake-only“ lehký modul bez šifrovaného profilu (nebo obecný lock‑screen PIN rozhraní Androidu).
- ADR rozšířit o hrozby (replay, spoofing bez enrolled profilu).
- Vyžaduje samostatný mileston a retest lockscreen matice.

## Odkazy

- Android Direct Boot: [https://developer.android.com/guide/topics/connectivity/telecom/direct-boot](https://developer.android.com/guide/topics/connectivity/telecom/direct-boot)
