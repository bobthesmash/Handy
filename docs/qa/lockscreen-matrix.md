# Lockscreen QA matrix (`F3-T01`)

## Stav QA

Symbol **⬜** znamená **ještě neprovedeno** — ne úspěch. Dokud buňky neobsahují explicitní **OK** / **FAIL** + poznámku z reálného zařízení, **Definition of Done F3** („všechny intenty otestované na matrix“) není splněná jen existencí tohoto souboru. Šablona dokumentu je artefakt úkolu `[F3-T01]` v changelogu; vyplnění matrix je samostatný QA běh (viz též `[F3-T08]` — field test).

**Primární QA telefon (`[D-001]`):** Samsung Galaxy S20 — One UI často mění chování na keyguardu a přístup k oznámením; u **FAIL** bývá v poznámce užitečné uvést SKU (Exynos vs Snapdragon), pokud existuje podezření na rozdíl. Kontext: [`galaxy-s20.md`](../device-notes/galaxy-s20.md), rejstřík [`device-notes/README.md`](../device-notes/README.md).

Šablona pro ruční QA na fyzickém zařízení. V záznamu QA nahrazuje výchozí ⬜ v buňce text **OK** nebo **FAIL** a krátká poznámka (log, OEM, build).

**Předpoklad:** uživatel alespoň jednou odemkl zařízení po startu (viz [ADR 0005 — Direct Boot](../decisions/0005-direct-boot.md)).

Sloupce:

- **A** — Obrazovka zapnutá, odemčeno  
- **B** — Obrazovka vypnutá (idle), odemčeno, zařízení probuzené hlasem/tlačítkem dle scénáře  
- **C** — Zamčený keyguard  
- **D** — BT headset (SCO), stejný scénář jako řádek kde dává smysl  
- **E** — Telefon v kapse / rušné prostředí (subjektivní stabilita wake + ASR)

## Intenty (F1 + F2 + F4 aliasy)

| Intent | A | B | C | D | E |
|--------|---|---|---|---|---|
| CALL | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| SEND_SMS | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| SET_ALARM | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| CANCEL | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| STOP | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| REPEAT | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| VOLUME | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| READ_LAST_NOTIFICATION | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| REPLY_NOTIF | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| PLAY_MEDIA | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| TORCH | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| WHAT_TIME | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| WHAT_DATE | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| WHAT_BATTERY | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| OPEN_APP | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| NAVIGATE | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| TIMER | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| MEDIA_CTRL | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| SET_CONTACT_ALIAS | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| REMOVE_CONTACT_ALIAS | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |

## Poznámky k měření

- Sloupec **D** testovat se spárovaným headsetem a zapnutým `EarService` (VOICE_COMMUNICATION).
- **C** často omezuje systémové intenty; očekávej rozdíly Samsung / Xiaomi / Pixel.
