# gnoeckly-web

Mobile-first Angular-21-App (Material 3) für Gnöckly, verpackt mit Capacitor für Android und iOS.
Bedienung nach Android-Muster: Bottom-Navigation, FAB zum Einreichen, Bottom-Sheets statt Dialoge,
Hardware-Back-Button, Pull-to-Refresh, Haptik.

## Entwicklung im Browser

```bash
npm install
npm start          # http://localhost:4200, Backend unter http://localhost:8080 (src/environments/environment.ts)
```

Chrome DevTools mit Mobile-Emulation nutzen. Rewarded Ads sind im Browser nicht verfügbar
(Button zeigt einen Hinweis); den SSV-Callback lokal simulieren: `../docs/admob-ssv-lokal.md`.

## Android

Voraussetzungen: Android Studio mit SDK, Emulator mit Google Play Services (für AdMob).

```bash
npm run build:android          # ng build --configuration production && npx cap sync android
npx cap open android           # in Android Studio öffnen, dort Run / Signed App Bundle
```

Backend vom Emulator erreichen: `adb reverse tcp:8080 tcp:8080` (dann bleibt `localhost:8080`),
sonst `apiBaseUrl` auf `http://10.0.2.2:8080` bzw. die LAN-IP setzen und den Origin
`https://localhost` im Backend-CORS erlauben (Standard ist bereits `http://localhost` +
`capacitor://localhost`).

`android/app/src/main/AndroidManifest.xml` enthält die AdMob-**App-ID** (Meta-Data
`com.google.android.gms.ads.APPLICATION_ID`), aktuell Googles Test-ID. Für Produktion die echte
App-ID eintragen und in `src/environments/environment.prod.ts` die Rewarded-**Ad-Unit-IDs** setzen.

## iOS (nur auf einem Mac)

```bash
npx cap add ios
npm run build:ios
npx cap open ios
```

In Xcode: `GADApplicationIdentifier` (AdMob-App-ID), `NSUserTrackingUsageDescription` und
`SKAdNetworkItems` in `Info.plist`, Bundle-ID `at.stoneforge.gnoeckly`, Apple-Developer-Konto.

## Struktur

```
src/app/
├── app.ts / app.config.ts / app.routes.ts   Shell (Bottom-Nav), Provider, Lazy-Routen
├── core/api/        models.ts (Spiegel der Backend-Records), api.service.ts (ein Client pro Endpoint)
├── core/auth/       token.store (Refresh-Token in Capacitor Preferences), auth.service, Interceptor (401 → Refresh), Guards
├── core/ads/        admob.service (Rewarded Ad + UMP-Consent), reward-flow.service (Wallet-Polling nach SSV)
├── core/native/     Back-Button, Statusbar, Haptik, Online-Status
├── core/ui/         Toast (Snackbar)
├── shared/          joke-card, sticker-chip, page-header, confirm-/text-sheet, pull-to-refresh, pipes, i18n.ts
└── features/        feed, joke-detail, submit, rankings, marketplace, me, wallet, profile, auth, moderation, admin
```

Alle Texte liegen in `src/app/shared/i18n.ts`. Fehler vom Backend kommen als RFC 7807 mit
`code` (`INSUFFICIENT_COINS`, `NICKNAME_TAKEN`, …), ausgewertet über `shared/problem-detail.ts`.
