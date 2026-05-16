# Spotřeba v idle listening (`F0-T08`)

## Stav měření

**Žádná vyplněná měření z tohoto repa** — tabulky „Záznam“ / „Záznam 8h“ jsou prázdné šablony pro Battery Historian / Nastavení baterie po běhu na zařízení. Dokud řádky chybí, **nelze tvrdit** splnění cíle %/h z Definition of Done. `[F0-T08]` je v `progress.html` **`blocked`** do dostupnosti HW.

### Referenční HW (`[D-001]`)

**Samsung Galaxy S20** — battery optimalizace Samsungu / One UI významně mění výsledky; při měření zapisovat přesný Android a zda jsou např. „nekontrolovat optimalizaci“ / výjimka pro Handy zapnutá.

Cílová metrika ve F0 Definition of Done: **≤ ~6 %/h** baterie v režimu poslechu (wake + ring buffer bez plného ASR pipeline).

## Postup Battery Historian (Android toolchain)

1. USB debugging zapnutý, vývojářské nabíjení vypnuto pro realistické číslo.
2. Nahraj bugreport po **≥ 45 min** běhu v popředí + lockscreen scenario.
3. Otevři `battery historian` rozhraní nad bugreport ZIP.
4. Najdi graf **Awake** × **Estimated power use** procesu aplikace a **CPU / Audio** dílčí řádek.

### Záznam

| Datum | Build | Scenario (lockscreen/BT/on-device) | %/hodina hrubě | Poznámka |
|-------|-------|------------------------------------|----------------|----------|
| _ | _ | _ | _ | _ |

## 8h scénář (`F3-T03`)

Cíl F3: **≤ 5 %/h** v listening idle (viz Definition of Done F3). Tento blok doplňuje krátkodobé měření z úvodní sekce.

### Checklist před během

- [ ] Stejný build APK jako v CI / release candidate.
- [ ] Baterie ≥ 90 % nebo znáte počáteční stav v %.
- [ ] Vypnuto nabíjení po dobu testu.
- [ ] Screen timeout rozumný (např. 30 s); scénář buď „always listening“ přes FGS, nebo periodický wake dle produktu.
- [ ] Bluetooth: buď reálný headset zapnutý celou dobu, nebo explicitně „jen vestavěný mikrofon“ — zapsat.

### Po ~8 h

1. Bugreport + Battery Historian (viz výše), nebo  
2. Nastavení → Baterie → spotřeba **Handy** za období testu.

### Záznam 8h

| Datum | Build | Zařízení / Android | BT ano/ne | Odhad %/8 h nebo %/h | Historian odkaz / poznámka |
|-------|-------|----------------------|----------|----------------------|----------------------------|
| _ | _ | _ | _ | _ | _ |

