/**
 * Entwicklung. Im Android-Emulator ist der Host-Rechner unter 10.0.2.2 erreichbar, am echten
 * Geraet die LAN-IP (Backend-CORS: GNOECKLY_CORS_ALLOWED_ORIGINS erweitern) oder
 * `adb reverse tcp:8080 tcp:8080` (dann bleibt localhost).
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  admob: {
    // Googles offizielle Test-IDs - liefern immer Testanzeigen, nie Umsatz.
    rewardedAdIdAndroid: 'ca-app-pub-3940256099942544/5224354917',
    rewardedAdIdIos: 'ca-app-pub-3940256099942544/1712485313',
    isTesting: true,
  },
};
