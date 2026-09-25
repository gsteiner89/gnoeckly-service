# Gnöckly

Witze-App für Android und iOS (Capacitor) mit Community-Features: öffentlicher Feed ohne Login,
Self-Registration, Einreichen mit Admin-Freigabe, Reddit-artige Votes, Kategorien, Rankings,
In-App-Währung **Gnöcken** (nur über AdMob Rewarded Ads verdienbar, kein Payment) und ein
admin-kuratierter **Sticker-Marktplatz** (Sticker kaufen, sammeln, an Witze anderer verleihen).
Zweiter Consumer von `midgard-core` nach saga-service. Backend (`gnoeckly-service`, Wurzel dieses
Repos) und Frontend (`gnoeckly-web/`) liegen wie bei Saga in einem Repo.

## Stack & Koordinaten

- Backend: Java 21, Spring Boot 3.3.4, Gradle (Kotlin DSL), PostgreSQL, GroupId `at.stoneforge.gnoeckly`
- `midgard-core` 0.1.0-SNAPSHOT aus `mavenLocal()` (`I:\Entwicklung\midgard-framework`,
  dort `./gradlew publishToMavenLocal`), inklusive `testFixtures(...)` für die Test-Basis
- Frontend: `gnoeckly-web/`, Angular 21 + Angular Material (Material 3) + Capacitor,
  **kein** `midgard-ui`/Optimus (das ist die Desktop-Admin-Shell von saga-web)

## Build & Test

```
./gradlew test                          # echtes Postgres (embedded, kein Docker), ~30 s
./gradlew --refresh-dependencies test   # nach jedem midgard-core publishToMavenLocal (SNAPSHOT!)
./gradlew bootRun --args="--spring.profiles.active=dev"
```

Env-Vars zum Start: `GNOECKLY_DB_USER`, `GNOECKLY_DB_PASSWORD` (`GNOECKLY_DB_URL` optional),
`MIDGARD_JWT_SECRET` (≥ 32 Bytes), `MIDGARD_BOOTSTRAP_ADMIN_EMAIL` + `_PASSWORD` für den
Superadmin (Tenant, Kategorien und Sticker entstehen auch ohne, siehe Regel 5).

Jeder Endpoint hat einen Integrationstest über `GnoecklyIntegrationTestBase` (echter Context,
MockMvc mit echter Filter-Chain). Testdaten kommen über `GnoecklyTestData` durch die echten
HTTP-Flows (Registrierung, Einreichen, Freigabe); nur Gutschriften laufen direkt über
`WalletService` innerhalb von `GnoecklyTestData.inTenant(...)`.

## Midgard-Consumer-Regeln (zusätzlich zu midgard-framework/CLAUDE.md)

1. **`-parameters` in `build.gradle.kts` nicht entfernen** (siehe midgard CLAUDE.md).
2. **`spring.jpa.open-in-view=true` bleibt** – Midgards TenantFilter und unser
   `DefaultTenantFilter` brauchen die request-gebundene Session.
3. **Single-Tenant mit Default-Tenant.** Tokenlose Requests (`/api/v1/public/**`, Registrierung,
   AdMob-SSV, Sticker-Bilder) bekommen den Bootstrap-Tenant vom `DefaultTenantFilter`
   (Order `DEFAULT_FILTER_ORDER - 5`, nach Midgards TenantFilter). Ohne ihn sehen Public-Reads
   alle Tenants und Writes scheitern an `tenant_id NOT NULL`.
4. **`midgard.security.public-paths` / `superadmin-paths` in `application.yml` sind
   sicherheitsrelevant.** Midgards Stammdaten-Endpoints (`/api/v1/users`, `groups`, `menu-items`,
   `menu-categories`, `modules`, `system-options`) haben kein `@PreAuthorize`; mit
   Self-Registration könnte sonst jeder User `gnoeckly.submitFee=0` setzen. Neue öffentliche
   Endpoints ausschließlich unter `/api/v1/public/**`, Admin-Endpoints unter `/api/admin/**` mit
   `@PreAuthorize("hasAuthority('SUPERADMIN')")`.
5. **Seed läuft bei jedem Start idempotent über `GnoecklySeedRunner`** (ApplicationRunner), nicht
   als versioniertes `MidgardUpgrade`: `MidgardUpgradeRunner` verbucht ein Upgrade auch dann als
   angewendet, wenn es mangels `MIDGARD_BOOTSTRAP_ADMIN_*` nichts getan hat – ein einziger Start
   ohne Env-Vars hätte Tenant und Seed dauerhaft übersprungen. Der Runner legt Tenant, Modul,
   Kategorien, Sticker immer an, den Superadmin sobald die Env-Vars gesetzt sind. Falls doch einmal
   ein echtes, einmaliges `MidgardUpgrade` nötig wird: Versionen `2026.11.NNN` (midgard belegt
   `2026.09.x`, saga `2026.10.x`, gemeinsame `midgard_upgrade_history`). Alles, was ausserhalb eines
   Requests tenant-gebundene Entities anfasst, setzt `TenantContext` **und**
   `TenantHibernateFilterActivator.activate` (Muster `GnoecklySeedRunner`).
6. **Flyway `V{n}__...sql` unter `db/migration`** (eigene `flyway_schema_history`, Midgards Skripte
   laufen davor). Jede Entity braucht ihr eigenes `@Audited` und eine handgeschriebene `_aud`-Tabelle
   (alle Nicht-PK-Spalten nullable, `fk rev -> revinfo`); `ddl-auto=validate` prüft beides.
7. **Soft-Delete ist nie automatisch gefiltert** – Repositories filtern `deletedAt` selbst.
   Nie manuell `tenant_id` in Queries (Hibernate-Filter).
8. **Kein `@Async`/`@Scheduled`** ohne explizites Tenant-Handling (`TenantContext` ist ThreadLocal).
   Bewusste Ausnahmen mit explizitem Tenant-Handling: `StreakReminderJob` (`@Scheduled`, setzt Default-Tenant +
   Hibernate-Filter selbst, Muster `GnoecklySeedRunner`) und der Push-Versand (`PushService`: Tokens werden im
   Request-Thread geladen, der eigene Sende-Thread fasst keine Entities an; `PushTokenCleaner` setzt den Tenant selbst).
   Deshalb: Boost-Ablauf wird zur Query-Zeit ausgewertet, Karma wird aggregiert, kein Job.

## Fachliche Regeln

- **Gnöcken** heißen im Code `Wallet`/`CoinTransaction` (ASCII). Jede Buchung nur über
  `WalletService` (PESSIMISTIC_WRITE + Ledger mit `balanceAfter`), immer in der Transaktion des
  fachlichen Aufrufers. Zu wenig Guthaben → 409 `INSUFFICIENT_COINS`.
- **Gutschriften nur serverseitig**: AdMob-SSV-Callback (`/api/v1/public/admob/ssv`, Signatur
  gegen Googles Keys, idempotent per `transaction_id`), Freigabe eines Witzes
  (`coinsPerApprovedJoke`), Startguthaben bei der Registrierung (`welcomeCoins`, Vorgabe 100).
  Der Client bucht nie selbst. Beträge sind SystemOptions (`GnoecklySystemOptionDefinitions`).
- **Superadmins zahlen keine Gebühren** (Nutzer-Vorgabe): `WalletService.debit` überspringt die
  Abbuchung für `superAdmin`-User ohne Ledger-Eintrag. Gilt damit automatisch für Einreichen,
  Boost und Sticker-Kauf. Der Admin bekommt auch kein Startguthaben (Wallet mit 0 aus dem Seed).
- **Vote-Zähler** am Witz nur über `JokeRepository.applyVoteDelta` (atomarer nativer UPDATE inkl.
  Hot-Score). Hot-Formel dort und in `JokeService.initialHotScore` synchron halten.
- **Moderation**: Einreichungen sind `PENDING`; öffentlich sichtbar nur `APPROVED`. Nur der
  Superadmin moderiert (Midgards `Role` ist ungenutzt, kein Moderator-Rollenmodell).
- **Streak** (`streak/`): aktiv = Vote setzen oder Witz einreichen (`StreakService.touch` in der Transaktion
  des Aufrufers); Riss/Freeze-Verbrauch werden lazy aus `lastActiveDate` abgeleitet, kein Job. Tageswechsel
  über den `Clock`-Bean (`gnoeckly.timezone`, Vorgabe `Europe/Vienna`; Tests ersetzen ihn). Meilensteine
  7/30/100 zahlen Gnöcken (SystemOptions `gnoeckly.streak.*`) und schenken den Sticker `streak-<tag>`
  (`purchasable=false`, nicht im Marktplatz kaufbar, Slugs im Seed).
- **Daily Quests** (`quest/`): drei feste Quests (`VOTE` 5 fremde Witze bewerten, `SUBMIT` 1 Einreichung, `AWARD` 1 Sticker
  verleihen). Fortschritt wird zur Abrufzeit aus den Tagesdaten berechnet (Tagesgrenze über den `Clock`-Bean), nie gespeichert;
  Einlösen manuell, Idempotenz durch Unique-Index auf `gnoeckly_daily_quest_claim (user, quest, date)`. Belohnungen sind
  SystemOptions `gnoeckly.quest.*`. Midgards Auditing nutzt die `Clock` nicht (`created_at` = echte Zeit): Tests lassen die
  `MutableClock` (`TestClockConfiguration`) auf dem heutigen Datum.
- **Push** (`push/`, `docs/push-fcm.md`): FCM über Firebase Admin, standardmäßig aus (`gnoeckly.push.enabled=false`, `NoopPushSender`).
  `PushService.notify` lädt Tokens im Aufrufer und versendet nach Commit in eigenem Thread; Versandfehler brechen nie die
  Fachaktion ab, `UNREGISTERED`-Tokens werden gelöscht. Ereignisse: Freigabe/Ablehnung, Sticker auf Witz, Score-Schwelle
  10/50/100, Streak-Erinnerung 18:00. Ein Geräte-Token gehört genau einem User (Unique-Index, wird umgehängt).
  Tests ersetzen den Sender durch `RecordingPushSender` (`TestSupportConfiguration`), `gnoeckly.push.async=false`.
- **Sticker-Bilder** liegen in Midgards `StorageService` (Namespace `stickers`, ≤ 512 KB) und
  werden über `/api/v1/public/stickers/{id}/image` mit Cache-Headern gestreamt.
- **Fehlerformat** RFC 7807 mit `code` (midgard Regel 12). Eigene Codes in
  `GnoecklyExceptionHandler` (nicht von `ResponseEntityExceptionHandler` ableiten – Kollision mit
  Midgards Handler): `INSUFFICIENT_COINS`, `NICKNAME_TAKEN`, `EMAIL_TAKEN`, `STICKER_SOLD_OUT`,
  `STICKER_UNAVAILABLE`, `STICKER_NOT_OWNED`, `STREAK_FREEZE_LIMIT`, `QUEST_ALREADY_CLAIMED`, `QUEST_NOT_COMPLETED`.

## Bekannte Stolpersteine

- `@SpringBootTest(classes = GnoecklyApplication.class)` sammelt verschachtelte
  `@TestConfiguration`s **nicht** automatisch ein – explizit `@Import` (siehe `AdMobSsvIT`).
- `GnoecklyApplication` braucht `@EntityScan`/`@EnableJpaRepositories` auf `at.stoneforge.gnoeckly`,
  weil Midgards explizites `@EnableJpaRepositories` Boots Default-Scan abschaltet.
- AdMob lokal: `docs/admob-ssv-lokal.md` (Testkey + Signier-Tool `tools/SsvSignTool.java`).
