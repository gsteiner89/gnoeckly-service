# Gnöckly

Witze-App für Android und iOS. Öffentlicher Feed, Einreichen mit Moderation, Up-/Down-Votes,
Kategorien, Rankings, die Währung **Gnöcken** (verdient über Rewarded Ads) und ein
Sticker-Marktplatz. Backend `gnoeckly-service` (Spring Boot auf `midgard-core`), Frontend
`gnoeckly-web/` (Angular + Capacitor).

## Backend starten

Voraussetzungen: Java 21, PostgreSQL, `midgard-core` in `~/.m2` (`./gradlew publishToMavenLocal`
in `I:\Entwicklung\midgard-framework`).

```bash
export GNOECKLY_DB_URL=jdbc:postgresql://localhost:5432/gnoeckly
export GNOECKLY_DB_USER=gnoeckly
export GNOECKLY_DB_PASSWORD=...
export MIDGARD_JWT_SECRET=$(openssl rand -base64 48)
export MIDGARD_BOOTSTRAP_ADMIN_EMAIL=admin@example.test     # Superadmin fuer die Moderation; wird beim
export MIDGARD_BOOTSTRAP_ADMIN_PASSWORD=...                  # naechsten Start angelegt, auch nachtraeglich
./gradlew bootRun --args="--spring.profiles.active=dev"
```

Im `dev`-Profil: Swagger-UI unter `http://localhost:8080/swagger-ui.html`, OpenAPI unter
`/v3/api-docs` (Basis für den generierten TypeScript-Client in `gnoeckly-web`).

## Tests

```bash
./gradlew test
```

Integrationstests gegen einen eingebetteten Postgres-Prozess (kein Docker nötig).

## API-Überblick

| Bereich | Pfad | Auth |
|---|---|---|
| Feed, Detail, Kategorien, Rankings, Sticker-Katalog/-Bilder, Profile | `/api/v1/public/**` | keine |
| Registrierung | `POST /api/v1/public/register` | keine |
| AdMob-SSV-Callback | `GET /api/v1/public/admob/ssv` | Signatur |
| Login / Refresh | `/api/v1/auth/**` | keine |
| Ich, Wallet, eigene Witze, Sammlung | `/api/v1/me/**` | JWT |
| Einreichen, Voten, Boosten, Melden, Sticker kaufen/verleihen | `/api/v1/jokes/**`, `/api/v1/stickers/**` | JWT |
| Moderation, Kategorien, Sticker, Meldungen | `/api/admin/**` | Superadmin |

Fehler kommen als RFC 7807 `ProblemDetail` mit stabilem `code` (z.B. `INSUFFICIENT_COINS`).

Weitere Doku: [CLAUDE.md](CLAUDE.md) (Regeln), [docs/admob-ssv-lokal.md](docs/admob-ssv-lokal.md).
