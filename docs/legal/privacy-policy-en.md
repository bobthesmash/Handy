# Privacy Policy — Handy

**Effective:** 2026-05-17  
**Version:** 1.2

## Operator / contact

**Handy** is built so that **sensitive processing stays on your device**. We do not operate a companion cloud service that stores your raw voice, transcripts, or voice embeddings.

For questions about this policy, use the developer contact published with the app distribution channel (e.g. Google Play developer email).

## Summary

- **Voice and audio** are **not sent** from the phone for core speech recognition — recognition runs **locally** using on-device models.
- The **voice embedding** (numerical voice print) is stored in **encrypted storage** on the device ([Android EncryptedSharedPreferences](https://developer.android.com/topic/security/data)).
- **Telemetry is off by default.** If you enable it under **Settings → Diagnostics**, only a **local NDJSON file** on the device is written (`filesDir`; **no upload** to our servers). The file does **not** include **audio** or **beta feedback message text** — if beta feedback is saved while telemetry is on, only the **star rating** is recorded in that stream.
- **Profile backup** is **user-initiated**: you export an encrypted file and choose where it is stored.
- The **in-app application version line** can be copied to the **Android system clipboard** via a **long-press gesture** — it only happens when **you explicitly do it** (e.g. sharing version info with support). The app does **not** send that clipboard text anywhere automatically.

## Data processed on the device

| Category | Purpose | Stays on device |
|----------|---------|-----------------|
| Microphone audio | wake word, commands, enrollment, destructive confirmation | yes (RAM / short-lived buffers) |
| Voice embedding | speaker verification | encrypted prefs |
| Contacts / numbers (if permitted) | calls & SMS as you command | in-memory + system APIs |
| Contact aliases | “what you say” → name/number | Room DB |
| ASR transcript text | NLU and action execution | in-memory |
| Notifications (if enabled) | read/reply per your command | per Android rules |
| Optional local telemetry | debugging: event types and metrics (e.g. completed intents, latency, low-confidence ASR retries, false wake-ups; beta feedback saves record **stars only**); **no** audio or feedback message text in this file | NDJSON file under `filesDir` |
| App version line (shown in Settings / related screens) | display; optional copy to system clipboard on long press (user gesture) | device clipboard only if you trigger copy; no automatic transmission from Handy triggered by this action |

## Network access (INTERNET)

The app may declare the **INTERNET** permission. Behaviour depends on product configuration:

- **Picovoice Porcupine** may perform **initial or occasional** licence / validation traffic to Picovoice — this is **not** a definition of “we upload your voice to the app author’s servers” for core recognition.
- The **assistant core** in this repo is intended to run **offline**.

## Android permissions (privacy relevance)

Permissions are requested only where needed for functionality, for example:

- **RECORD_AUDIO** — listening and commands.
- **POST_NOTIFICATIONS** / **foreground service** — background listening with a visible notification.
- **BLUETOOTH_CONNECT** — headsets.
- **CALL_PHONE**, **READ_CONTACTS**, **SEND_SMS** — calls and messages (optional, system-granted).
- **CAMERA** — torch control (optional).
- **RECEIVE_BOOT_COMPLETED** — restart listening after reboot (no audio export by itself).

**Notification listener access** is enabled in **system settings**, not as a simple runtime permission — the in-app onboarding explains why it exists.

## Children

The app is not directed at children under 13.

## International transfers

We do not operate a cloud that receives the categories above as “our service”. Third-party endpoints (e.g. Picovoice licence checks) are governed by **their** documents and your OS.

## Changes

We will publish updates with a new effective date. Store distribution may notify you about app updates.

## Your rights (EU GDPR — orientation)

Because processing is **local on your device**, you exercise many rights directly in Android (clear app data, revoke permissions, uninstall). For third parties (e.g. Picovoice), follow their terms.

---

*This text is informational and is not a substitute for legal advice. Have it reviewed before store publication.*
