# False-accept: 30 nahrávek cizích hlasů ([F1-T23])

## Cíl

Speaker verification musí **< 2 %** vzorků klasifikovat jako **StrongAccept** (nebo projektem definovaný průchod) proti uloženému embeddingu majitele.

## Příprava

1. **Zařízení:** referenční **[D-001]: Samsung Galaxy S20** (nebo ekvivalent se zdůvodněním); na zařízení nahraný embedding majitele ve stejném režimu jako produkce.
2. **30 krátkých nezávislých řečových vzorků** od různých mluvčích (ne majitel), mono 16 kHz pokud možno.
3. **Žádné odesílání audia mimo zařízení** (viz pravidla projektu).

Kontext k referenčnímu Samsungu / One UI: [`galaxy-s20.md`](../device-notes/galaxy-s20.md) · rejstřík [`device-notes/README.md`](../device-notes/README.md).

## Protokol

| # | Zdroj / poznámka | Výsledek verify | Pass (měl zamítnout) |
|---|------------------|-----------------|----------------------|
| 1 | … | score / label | ✓ / ✗ |
| … | … | … | … |
| 30 | … | … | … |

- **False accept:** vzorek prošel jako ověřený majitel, ač neměl.

## Výsledek

- **False accepts:** \_\_\_ / 30 = \_\_\_ %  
- **Datum / verze / zařízení:**  

Pokud je podíl false accept **< 2 %**, lze **[F1-T23]** v plánu uzavřít s odkazem na tento dokument a agregované číslo (bez ukládání audia).
