# Server-Push (FCM) einrichten

Gnöckly verschickt Push-Nachrichten vom Backend über Firebase Cloud Messaging (FCM). Ohne Einrichtung
läuft alles normal weiter: `gnoeckly.push.enabled` ist standardmäßig `false`, der `NoopPushSender` loggt
nur. Der Client zeigt die Push-Einstellung ausschließlich in der nativen App.

## Was verschickt wird

| Ereignis | Auslöser | Ziel in der App |
|---|---|---|
| Witz freigegeben / abgelehnt | Moderation (`JokeService.approve/reject`) | `/me` |
| Sticker auf deinen Witz | `StickerService.award` | `/joke/{id}` |
| Witz erreicht 10 / 50 / 100 Punkte | `VoteService.vote` (nur beim Überschreiten) | `/joke/{id}` |
| Serie in Gefahr | `StreakReminderJob`, täglich 18:00 (Vienna) für Serien ab 2 Tagen, gestern zuletzt aktiv | `/challenges` |

Opt-out ist Token löschen (Button auf der Aufgaben-Seite bzw. Logout). Feingranulare Einstellungen je
Kategorie gibt es bewusst nicht.

## 1. Firebase-Projekt und Android

1. In der [Firebase Console](https://console.firebase.google.com) ein Projekt anlegen.
2. Android-App mit dem Paketnamen `at.stoneforge.gnoeckly` hinzufügen.
3. `google-services.json` herunterladen und nach `gnoeckly-web/android/app/google-services.json` legen
   (steht in `.gitignore`, nicht einchecken). Die Gradle-Verkabelung (`com.google.gms.google-services`)
   ist schon vorhanden und greift, sobald die Datei existiert.
4. `cd gnoeckly-web && npx cap sync android`, App neu bauen.

## 2. Service-Account fürs Backend

1. Firebase Console → Projekteinstellungen → Dienstkonten → „Neuen privaten Schlüssel generieren“.
2. Die JSON **außerhalb des Repos** ablegen (steht ebenfalls in `.gitignore`, falls sie doch im Projekt landet).
3. Backend mit Env-Vars starten:

```
GNOECKLY_PUSH_ENABLED=true
GNOECKLY_FIREBASE_CREDENTIALS=/pfad/zur/service-account.json
```

Fehlt der Pfad bei `enabled=true`, bricht der Start mit einer klaren Meldung ab.

## 3. iOS (später)

Das Projekt hat aktuell nur `android/`. Nach `npx cap add ios`: `GoogleService-Info.plist` in
`ios/App/App/` legen, in Firebase einen APNs-Authentifizierungsschlüssel hinterlegen und in Xcode die
Capabilities „Push Notifications“ und „Background Modes → Remote notifications“ aktivieren.

## Erinnerungs-Job

- Cron und Zeitzone: `gnoeckly.push.reminder-cron` (Standard `0 0 18 * * *`) und `gnoeckly.timezone`.
  `"-"` schaltet den Job ab.
- Der Job läuft einmal je Backend-Instanz. Bei mehreren Instanzen kämen Erinnerungen doppelt, dann braucht
  es eine Sperre (z. B. ShedLock).

## Lokal testen

- Ohne Firebase: `bootRun` wie gewohnt, Push-Nachrichten erscheinen als Debug-Zeile des `NoopPushSender`.
- Mit Firebase: Android-Gerät oder Emulator mit Google Play, App installieren, auf der Aufgaben-Seite
  „Benachrichtigungen aktivieren“, dann im Admin einen Witz freigeben.
- Erinnerung sofort auslösen: `gnoeckly.push.reminder-cron` auf einen nahen Zeitpunkt setzen, z. B.
  `"0 */5 * * * *"` für alle fünf Minuten, und einen User mit gestern beendeter Serie ≥ 2 verwenden.
