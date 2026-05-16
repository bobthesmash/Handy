# Zásady ochrany osobních údajů — Handy

**Účinnost:** 2026-05-17  
**Verze:** 1.2

## Správce

Aplikace **Handy** je navržena tak, aby **zpracovávala citlivá data výhradně na zařízení** uživatele. Neprovozujeme vlastní cloudovou službu pro ukládání hlasu, přepisů ani hlasových otisků.

Kontakt pro dotazy k těmto zásadám: uveďte kontakt uvedený u distribuce aplikace (např. e-mail vývojáře v obchodě Google Play).

## Shrnutí

- **Hlas a audio** se **nepřenášejí** z telefonu kvůli rozpoznávání řeči — rozpoznávání běží lokálně (offline modely).
- **Hlasový otisk (embedding)** je uložen v **šifrovaném úložišti** na zařízení ([Android EncryptedSharedPreferences](https://developer.android.com/topic/security/data)).
- **Telemetrie** je **ve výchozím stavu vypnutá**. Pokud ji zapnete v **Nastavení → Diagnostika**, ukládá se jen **lokální soubor NDJSON** na zařízení (`filesDir`; žádné odesílání na naše servery). Do souboru se **nezapisuje audio** ani **text hodnocení beta** — u uložené beta zpětné vazyby jen agregované hvězdy.
- **Záloha profilu** je čistě **vaše akce**: exportuje se šifrovaný soubor; kam ho uložíte, určujete vy.
- **Řádek verze aplikace** zobrazený v některých obrazovkách aplikace lze **dlouhým klepnutím zkopírovat do systémové schránky** zařízení — proběhne to **jen tehdy**, když tuto akci spustíte **vy** (typicky kvůli podpoře nebo dokumentaci problému). Z aplikace se text verze automaticky přes síť **neodesílá**.

## Jaká data aplikace zpracovává na zařízení

| Kategorie | Účel | Kde zůstává |
|-----------|------|-------------|
| Audio z mikrofonu | probuzení, rozpoznávání příkazů, zápis hlasu, potvrzení citlivých akcí | zařízení, RAM / krátkodobé buffery |
| Hlasový embedding (číselný vektor) | ověření majitele hlasu | šifrované prefs na zařízení |
| Kontakty / čísla (při uděleném oprávnění) | hovory a SMS podle příkazu | zpracování v paměti, volání systémového rozhraní |
| Aliasy kontaktů | mapování „jak říkáš“ → jméno/číslo | lokální databáze Room na zařízení |
| Text přepisu (ASR) | NLU a provedení příkazu | zpracování v paměti |
| Oznámení (při zapnutém přístupu) | čtení / odpověď na notifikace dle příkazu | dle systému Android |
| Volitelná lokální telemetrie | ladění: typy událostí a metriky (např. dokončené intenty, latence, nízká jistota ASR, falešná probuzení; u beta zpětné vazyby jen počet hvězd); bez záznamu audia ani textu zpětné vazyby v tomto souboru | soubor NDJSON v `filesDir` na zařízení |
| Verze aplikace (název + číslo buildu v UI) | zobrazení v aplikaci; volitelná kopie do systémové schránky po dlouhém klepnutí (uživatelské gesto) | schránka Android jen je-li aktivováno kopírování uživatelem; žádný automatický přenos aplikace z tohoto úkonu |

## Síťové připojení (INTERNET)

V manifestu může být oprávnění **INTERNET**. Použití závisí na konfiguraci produktu:

- **Picovoice Porcupine** může vyžadovat **jednorázové nebo občasné** stažení licence či ověření — probíhá mezi zařízením a službami Picovoice, nikoli „nahrávání hlasu na náš server“.
- **Jádro asistenta (ASR, NLU, speaker verification v této aplikaci)** je zamýšleno jako **offline**.

Pokud používáte výhradně open-source modely bez síťové aktivace, může být síť v praxi využita minimálně — záleží na konkrétní verzi sestavení.

## Oprávnění Android (význam pro soukromí)

Aplikace žádá oprávnění jen tam, kde je to pro funkci nutné. Typicky:

- **RECORD_AUDIO** — poslech a příkazy.
- **POST_NOTIFICATIONS** / **FOREGROUND_SERVICE** — běh poslechu na pozadí s viditelnou notifikací.
- **BLUETOOTH_CONNECT** — headset.
- **CALL_PHONE**, **READ_CONTACTS**, **SEND_SMS** — hlasové volání a zprávy (volitelně dle vašeho souhlasu v systému).
- **CAMERA** — ovládání svítilny (volitelné).
- **RECEIVE_BOOT_COMPLETED** — obnovení služby po restartu (bez přenosu audia mimo zařízení).

**Přístup k oznámením** (Notification Listener) se zapíná v **systémovém nastavení Androidu**, není to běžné runtime oprávnění — popis účelu je v průvodci aplikace.

## Děti

Aplikace není zaměřena na děti mladší 13 let.

## Mezinárodní přenosy

Osobní údaje v rozsahu popsaném výše **neodevzdáváme** jako „službu v cloudu“ vlastnímu provozovateli aplikace. Případné volání třetích stran (např. licence Porcupine) řídí **jejich** dokumentace a váš prohlížeč/OS.

## Změny zásad

Změny zveřejníme novou verzí dokumentu a datem účinnosti. U aplikace distribuované přes obchod vás může obchod informovat o aktualizaci.

## Vaše práva (EU / GDPR — orientační)

Jelikož zpracování probíhá **lokálně na vašem zařízení**, mnohá práva uplatníte přímo v aplikaci nebo v systému Android (smazání dat aplikace, odvolání oprávnění, odinstalace). U třetích stran (např. Picovoice) postupujte dle jejich podmínek.

---

*Text je informativní a nenahrazuje právní posudek. Před publikací v obchodě ho nechte zkontrolovat právníkem.*
