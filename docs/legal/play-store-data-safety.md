# Google Play — Data safety & permissions declaration (Handy)

Podklad pro vyplnění **Data safety** a souvisejících formulářů. Uprav dle finálního buildu (zejména zda je aktivní Porcupine a síťová licence).

## Data collection and sharing (orientační odpovědi)

- **Does your app collect data?**  
  Zamýšlený design: **citlivá data zůstávají na zařízení**; vývojář **nesbírá** audio ani embedingy přes vlastní backend.  
  Pokud Google vyžaduje deklarovat údaje předávané **třetí straně** (např. Picovoice při licenci), doplň podle jejich podmínek.

- **Data types — typicky „No“ nebo „Processed only on device“** tam, kde Play nabízí lokální zpracování:
  - Location — **No**
  - Personal info (name, contacts) — zpracování **v aplikaci** pro funkci; **ne odesíláme** na vlastní server (pokud nemáte backend)
  - Photos and videos — **No** (pokud nečtete galerii — Handy obvykle ne)
  - Audio — **zpracování na zařízení**; bez vlastního odesílání na váš server
  - **Health, financial, etc.** — según aplikace, obvykle **No**

- **Encryption in transit:** u vlastního backendu **N/A**; HTTPS u třetích stran dle nich.

- **Encryption at rest:** embedding v **EncryptedSharedPreferences**; záloha **uživatelské heslo + AES-GCM**.

- **Volitelná diagnostika (F2-T13):** pokud uživatel v **Nastavení → Diagnostika** zapne lokální telemetrický log, zapisuje se **jen soubor NDJSON na zařízení** (typy událostí — bez audio, bez textu beta zpětné vazyby). Nejde o sběr na vlastní backend; hodnotí se jako lokální zpracování / „no upload“.

- **Řádek verze v UI (`AppVersionFooterText`):** zobrazuje se verze aplikace; **dlouhým klepnutím** uživatel může řádek **zkopírovat do systémové schránky** (Android Clipboard). Přenos neiniciuje Handy ani se neodesílá na vlastní backend — je to jen **lokální gesto uživatele** (např. pro podporu). Úmyslné vlastní použití schránky aplikaci v Data safety klasifikovat dle formuláře (často jen „kopírování volitelná akce uživatele“ bez sběru dat vývojářem).

- **Users can request deletion:** odinstalování / vymazání dat aplikace v systému Android; případná práva u třetích stran zvlášť.

## Oprávnění — zdůvodnění (stručně pro Play Console)

| Oprávnění | Zdůvodnění |
|-----------|------------|
| Microphone | Poslech wake-wordu, příkazy, zápis hlasu, potvrzení citlivých akcí. |
| Foreground service (+ microphone type kde platí) | Stabilní poslech na pozadí s notifikací. |
| Post notifications | Zobrazení stavu poslechu (Android 13+). |
| Bluetooth connect | Náhlavní souprava / SCO. |
| Phone / read contacts | Hlasové volání a čísla z kontaktů (uživatel může odmítnout). |
| Send SMS | Odeslání SMS po příkazu a potvrzení (uživatel může odmítnout). |
| Camera | Ovládání svítilny. |
| Internet | Licence/výměna dat knihoven třetích stran (např. Porcupine) dle integrace; jádro offline. |
| Boot completed | Obnovení poslechu po restartu. |

## Notification listener

Nepřidává se jako `uses-permission`, ale vyžaduje **uživatelské zapnutí** v systému — v Data safety popiš, že aplikace **může číst obsah oznámení** pouze pokud uživatel povolí přístup a **účel** je čtení / odpověď na notifikace lokálně.

## Odkazy

- V obchodě uveď URL na **Privacy Policy** (může být hostovaná dokumentace v repo nebo web).
- V aplikaci: **Nastavení → Ochrana osobních údajů** (surový text v `res/raw/` + stejný obsah v `docs/legal/`).
