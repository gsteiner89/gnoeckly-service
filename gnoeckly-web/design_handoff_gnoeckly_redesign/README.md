# Handoff: Gnöckly Redesign (gnoeckly-web)

## Overview
Redesign der Gnöckly-App (Angular 21 + Angular Material 3 + Capacitor, `gnoeckly-web/`) im Nocturne-Stil: dunkle, kompakte Oberfläche, ein Akzent (Blurple), Inter als Schrift. Ziele: Witz zuerst lesbar, Kategorien in einer Zeile statt zwei, Profil bricht auf 360 px nicht mehr um, Sticker klar abgesetzt, neues Feature „Favoriten“.

## About the Design Files
`Gnöckly Redesign.dc.html` (+ `support.js`, `android-frame.jsx`, `_ds/…`) ist ein **klickbarer HTML-Prototyp**, keine Produktionscode-Vorlage. Aufgabe: die Screens in `gnoeckly-web/src/app/` mit den bestehenden Angular-Komponenten, Signals, Routen und `i18n.ts` nachbauen. Die Pfad-/Dateinamen unten beziehen sich auf den bestehenden Code. Öffnen: `Gnöckly Redesign.dc.html` im Browser (Ordner komplett entpacken; `support.js` und `_ds/` müssen daneben liegen).

## Fidelity
**Hi-fi.** Farben, Typo, Abstände, Radien und Zustände sind final. Material-Komponenten (`mat-button-toggle`, `mat-chip`, `mat-fab`, `mat-tab-group`, `mat-card`) werden **nicht** mehr im M3-Look benutzt: entweder mit Nocturne-Tokens umthemen oder durch leichte eigene Komponenten ersetzen. Empfehlung: `styles.scss` auf die Tokens unten umstellen und `mat.theme` nur noch für Bottom-Sheet, Snackbar, Form-Fields nutzen.

## Design Tokens (in `styles.scss` als CSS-Variablen anlegen)
Farben (Dark, Standard):
- `--color-bg: #161826` · `--color-surface: #232532` · `--color-text: #e9e9ed`
- `--color-accent: #9184d9` (nur für Linien, Icons, Rahmen, große Zahlen – nicht für Fließtext)
- `--color-divider: color-mix(in srgb, #e9e9ed 16%, transparent)`
- Neutral-Ramp: 100 `#f3f5fe` · 200 `#e4e7f5` · 300 `#cfd3e5` · 400 `#b2b6ca` · 500 `#9397ab` · 600 `#75798c` · 700 `#595d6c` · 800 `#3f424d` · 900 `#292b31`
- Accent-Ramp: 100 `#f5f4ff` · 200 `#e7e5fe` · 300 `#d2cefd` · 400 `#b5abfc` · 500 `#968ae0` · 600 `#796cbf` · 700 `#5d5294` · 800 `#423a6a` · 900 `#2b2741`
- Muted Text: `color-mix(in srgb, var(--color-text) 55%, transparent)`
- Karten-Header-Leiste: `color-mix(in srgb, var(--color-surface) 45%, var(--color-bg))`

Light (bei `prefers-color-scheme: light`, `[data-theme=light]`):
`--color-bg #e4e7f5 · --color-surface #f3f5fe · --color-text #292b31 · --color-accent #796cbf · --color-accent-300 #5d5294 · --color-accent-800 #e7e5fe · --color-accent-100 #2b2741 · --color-accent-900 #d2cefd · --color-neutral-900 #cfd3e5 · --color-neutral-800 #b2b6ca · --color-neutral-100 #292b31`

Typo: Inter 400/500 (kein 600/700 für Überschriften). Body 15 px / 1.5. Witztext **17 px / 1.6**, Detail 19 px / 1.65. Titel 16 px / 500 (Detail 20 px), 12 px Abstand zum Text. Meta 12 px, Kicker (Kategorie) 10 px uppercase letter-spacing .1em in `--color-accent`. Labels 11 px uppercase .08em muted. Bottom-Nav-Label 11 px / 500.

Radien: 8 px (Karten, Buttons, Inputs), 6 px (Tags), 10 px (Sticker-Kacheln), 14 px (Sheets oben), 50 % (Avatare, FAB).
Abstände: Gutter 16 px, Kartenabstand 10 px, Karteninnen 14 px, Header 52 px, Bottom-Nav 64 px.
Trennlinien: 1 px, an den Enden ausblendend: `linear-gradient(to right, transparent, var(--color-divider) 48px, var(--color-divider) calc(100% - 48px), transparent)`.
Schatten: Sheets/Menüs `0 0 0 1px #595d6c, 0 6px 18px rgba(0,0,0,.55)`. Geboostete Karte: `0 0 0 1px var(--color-accent), 0 0 22px color-mix(in srgb, var(--color-accent) 18%, transparent)`.
Buttons: **immer outlined** (1 px `--color-accent`, transparent, Text accent, 14 px / 500, Höhe 36–46 px). Hover `color-mix(accent 12%)`, Active 22 %. Sekundär: Rand `--color-divider`. Ghost: kein Rand. Disabled: Opacity .45. Fokus: `outline: 2px solid var(--color-accent); outline-offset: 2px`.
Icons: Phosphor (https://phosphoricons.com), Stroke ~1.6, statt Material Symbols.

## Shell (`app.ts`)
Bottom-Nav 64 px, `position: fixed`, `background: var(--color-bg)`, oben eine ausblendende 1-px-Linie. **5 Spalten** `1fr 1fr 72px 1fr 1fr`: Witze (Smiley) · Rangliste (Trophy) · **Neu** · Sticker (Storefront) · Ich (User). Icon 22 px + Label 11 px, Gap 3 px; aktiv `--color-accent`, sonst muted. „Ich“ ist auch bei `/wallet` aktiv.
**Neu-Button** (ersetzt den `mat-fab`): Kreis 46 px, `margin-top: -24px` (ragt über die Nav-Linie), `background: var(--color-accent)`, Icon `--color-bg` (Plus 22 px), `box-shadow: 0 0 0 6px var(--color-bg), 0 6px 16px rgba(0,0,0,.5)`, Label „Neu“ darunter. Führt zu `/submit`. Auf Auth-Seiten keine Nav.
Content-Padding unten: 84 px (Nav + Luft).

## Screens

### Feed (`feed.page.ts`)
- Header 52 px: „Gnöckly“ 18 px / 500 links; rechts Gnöcken-Pill (Coin-Icon + Balance, 36 px hoch, Rand divider, 13 px / 500) → `/wallet`. Nur eingeloggt.
- **Eine Filterzeile** (Padding 4 16 10): links Segment-Control 36 px (Rand divider, Radius 8) mit Heiß / Top / Neu (Icon 15 px + 13 px Text, Padding 0 10–11 px, Trenner 1 px); aktiv: Text accent + `inset 0 0 0 1px accent`. Rechts **Filter-Button** (Funnel-Icon + Label, 36 px, max 140 px): Label „Alle“ (bei Top: „Alle · Woche“), bei einer gewählten Kategorie deren Name, bei mehreren „3 Kategorien“; aktiver Filter → Rand + Text accent. Die zwei alten Chip-Reihen entfallen.
- **Filter-Sheet** (`MatBottomSheet`, Surface, Radius 14 oben, Handle 32×4): Titel „Feed filtern“ 17 px / 500; Kategorien als 2-spaltiges Grid von 44-px-Buttons (Rand divider/accent, 14 px Text) mit **Checkbox** 16 px (Radius 4, gefüllt accent mit Häkchen in `--color-bg`). „Alle“ setzt zurück. **Mehrfachauswahl** → API `categoryId` muss zu `categoryIds[]` werden (Backend-Änderung). Bei Sort = Top zusätzlich Abschnitt „Zeitraum für Top“ mit Segment Heute / Woche / Immer. Button „Fertig“ (sekundär, 40 px).
- Liste: Padding 0 12 84, Gap 10. Leerzustand zentriert, muted 14 px.

### Witz-Karte (`joke-card.ts`) — überall gleich
Surface, Radius 8, `overflow: hidden`, `padding: 0 0 8px`, Gap 8, `flex: none`.
1. **Header-Leiste** (Padding 8 14, Hintergrund Karten-Header-Farbe, unten 1 px `color-mix(text 6%)`): Avatar 20 px (Kreis, accent-800 / Initiale accent-100 10 px) + Nickname (accent-300, 12 px / 500) · Zeit (muted) · rechts Kategorie-Kicker.
2. Body (Padding 0 14): optional Titel 16 px / 500 mit 12 px Abstand, dann Witztext 17 px / 1.6, `white-space: pre-wrap`, `text-wrap: pretty`. Tap → Detail.
3. **Sticker-Zeile** nur wenn vorhanden: `margin: 8px 14px 0; padding-top: 10px; border-top: 1px color-mix(text 6%)`; Label „STICKER“ 10 px muted, dann Kacheln 30 px (Radius 8, Sticker-Tint, Glyph 16 px, `inset 0 0 0 1px color-mix(text 8%)`), Zähler > 1 als Badge 16 px oben rechts (accent-Füllung, Text `--color-bg`, 10 px). Später: Sticker-Bild statt Glyph.
4. Aktionszeile (Padding 0 8 0 14): Spacer · **Lesezeichen-Button** 36 px (Phosphor Bookmark, gefüllt + accent wenn Favorit, sonst muted) · Vote-Cluster 36 px (Rand divider, Radius 8): ▲ 40 px · Score 13 px / 500 (positiv accent-300, negativ neutral-500, 0 muted) · ▼ 40 px; aktive Richtung accent.
Geboostet: Karten-Shadow s. o.; im Detail Tag „Geboostet“ (outline accent, 11 px, Blitz-Icon).
Eigene Witze („Meine Witze“): statt Avatar ein Status-Tag (In Prüfung neutral-900/100 · Freigegeben accent-800/100 · Abgelehnt neutral-800/100), Ablehnungsgrund als Box neutral-900 13 px; Aktionen als Ghost-Buttons 12 px: „Boost · 50 Gnöcken, 24 h“, „Zurückziehen“, Hinweis „Geboostet · noch 19 h“.

### Witz-Detail (`joke-detail.page.ts`)
Header: Back 40 px, Kategorie-Kicker als Titel, rechts Flag-Icon (Melden). Inhalt Padding 8 20: Titel 20 px / 500 (+12 px), Text 19 px / 1.65; Autorzeile (Avatar 24 + Name accent-300 13 px · Zeit, Boost-Tag); Aktionsreihe: Vote-Cluster 40 px · Lesezeichen 40 px (Rand divider) · „Sticker verleihen“ primär, `flex: 1`. Ausblendende Linie. Label „VERLIEHENE STICKER · n“; Einträge: Kachel 36 px (Tint + Glyph 20 px) · Stickername 14 px / 500 · „von <Nick> · Zeit“ 12 px · Nachricht 14 px.
Award-Sheet: Titel 17 px, Grid 4 Kacheln (aspect-ratio 1, radial-gradient Tint → bg, Glyph 30 px, Menge-Badge, Auswahl `0 0 0 2px accent`), Input „Nachricht (optional)“, Abbrechen (sekundär) / Verleihen (primär, disabled bis Auswahl).

### Profil (`me.page.ts`)
Header „Ich“ + Icon-Buttons Pencil / SignOut (40 px). Kopf: Avatar 48 px (accent-800, Initiale 20 px / 500), Nick 18 px / 500 (ellipsis), Bio 13 px muted.
**Stat-Streifen** statt 3 Kacheln: Grid `repeat(3, minmax(0,1fr))`, Rand oben/unten 1 px divider, Zellen mit linkem 1-px-Trenner, Padding 10 12: Wert 20 px / 500 (Gnöcken in accent-300 mit Coin-Icon 18, tappbar → Wallet), Label 11 px uppercase muted („Gnöcken“, „Karma“, „Sticker“).
Admin: `flex-wrap` Zeile aus 32-px-Sekundärbuttons 12 px (Gavel „Moderation“ + Badge accent-800, Tag „Kategorien“, Stack „Sticker verwalten“) – darf umbrechen.
**Tabs** (3, gleich breit, 40 px, nur untere 1-px-Linie: aktiv accent + Text accent, sonst divider + muted): „Meine Witze“ · „Favoriten“ (Badge mit Anzahl) · „Sammlung“.
Sammlung: Grid `repeat(4, minmax(0,1fr))`, Gap 10; Kachel aspect-ratio 1, Radius 10, `radial-gradient(circle at 50% 35%, <tint>, var(--color-surface) 78%)`, Glyph 30 px, Mengen-Badge 20 px oben rechts (accent-800/100), Name 11 px ellipsis.
Favoriten: kompakte Karten (Padding 12 14): Titel/Text, darunter Autor · Kicker · Score · gefülltes Lesezeichen (entfernt).

### Öffentliches Profil (`public-profile.page.ts`)
Wie Profil-Kopf; Stat-Streifen mit 2 Zellen (Karma, „Freigegebene Witze“); Label „SAMMLUNG · n“ + gleiches Kachel-Grid; Label „BESTE WITZE“ + bis zu 3 kompakte Karten (Text 15 px, Score rechts accent-300).

### Rangliste (`rankings.page.ts`)
Header „Rangliste“ + Segment Heute / Woche / Immer 32 px rechts. Tabs Witze / Autoren (2, wie oben). Witze: Rang-Zahl 20 px / 500 links (Platz 1 accent, 2–3 accent-300, sonst muted, Breite 26) + kompakte Karte (Text 16 px, Autor · Kicker · Score). Autoren: Zeilen 56 px mit unterer 1-px-Linie: Rang · Avatar 36 · Nick 15 px / 500 + „n Witze“ 12 px · Karma accent-300 + Label „KARMA“ 10 px.

### Marktplatz (`marketplace.page.ts`)
Header „Sticker“ + Gnöcken-Pill. Grid 2 Spalten, Gap 12; Karte Surface Padding 12: Bild-Kachel aspect-ratio 1 (radial-gradient Tint → neutral-900, Glyph 48 px), Badge „n× deins“ oben links; Name 14 px / 500; Beschreibung 12 px muted (min-height 34 px); „Noch n übrig“ 11 px accent; Kaufen-Button primär 36 px mit Coin-Icon + Preis, ausverkauft: „Ausverkauft“ disabled, Karte Opacity .55.

### Wallet (`wallet.page.ts`)
Header Back + „Gnöcken“. Label „DEIN GUTHABEN“, Betrag 44 px / 500 accent-300 mit Coin 36 px, Button „Werbung ansehen · +10“ primär 44 px (links ausgerichtet, nicht Block). Ausblendende Linie. „VERLAUF“: Zeilen mit Icon-Kachel 32 px (Surface, accent-300), Typ 14 px, „Beschreibung · Zeit“ 12 px muted, Betrag rechts 14 px / 500 (+ accent-300, − muted, Minus als „−“).

### Einreichen (`submit.page.ts`)
Header Back + „Witz einreichen“. Felder mit Label 12 px (70 % Text): **Kategorie** als Button (Surface, Rand divider, Caret) → gleiches Bottom-Sheet im Einzelauswahl-Modus (Radio-Punkte statt Checkbox, schließt bei Auswahl), Titel-Input 40 px, Textarea 8 Zeilen **17 px / 1.6** (gleiche Lesegröße wie Feed), Zähler „n/2000“ 11 px rechts, Gebühren-Hinweis 13 px mit Coin-Icon, „Einreichen“ primär 44 px (disabled ohne Text/Kategorie).

### Anmelden / Konto erstellen (`login.page.ts`, `register.page.ts`)
Header nur Back. Padding 24: kurzer Akzent-Strich 32×2, Überschrift 28 px / 500, Untertitel 14 px muted („Zum Abstimmen, Einreichen und Sammeln.“ / „Du startest mit 100 Gnöcken.“). Inputs 44 px (Surface, Rand divider, Fokus accent, 15 px). Primär-Button 46 px Block. Link-Zeile 14 px muted mit accent-300-Link. Keine Bottom-Nav.

## Interactions & Behavior
- Votes optimistisch wie bisher; aktive Richtung accent, Toggle entfernt Vote.
- **Favoriten (neu)**: Lesezeichen toggelt; Toast „Als Favorit gemerkt.“ / „Aus Favoriten entfernt.“; Liste unter Ich → Favoriten. Backend: neue Endpoints `POST/DELETE /api/v1/jokes/{id}/favorite`, `GET /api/v1/me/favorites` (paged), Feld `myFavorite: boolean` am `Joke`-Record.
- **Mehrfach-Kategorien**: `GET /api/v1/public/feed?categoryIds=a,b`.
- Sheets: Backdrop `color-mix(neutral-900 60%)`, Sheet slide-in 200 ms ease-out (translateY 24 px → 0), Backdrop fade 150 ms.
- Toast: unten über der Nav (bottom 76 px), neutral-900/100, 13 px, 2,2 s.
- Hover/Active/Focus s. Tokens; Touch-Ziele ≥ 36 px, Nav-Ziele volle Zellhöhe.
- Theme folgt `prefers-color-scheme` (Dark Standard).

## State Management
Bestehende Signals bleiben; neu: `categoryIds: string[]` statt `categoryId`, `favorites` (Set von Joke-IDs, optimistisch), `meTab: 'jokes' | 'favs' | 'coll'`, `filterSheetOpen`.

## Assets
- Sticker-Bilder kommen aus dem Backend (`/api/v1/public/stickers/{id}/image`); im Prototyp Platzhalter-Glyphen (Träne, Feder, Keks, Krone, Blitz, Medaille) mit Tints aus den Ramps. Sobald Bilder da sind: Bild in die Kachel, Tint als Hintergrund behalten.
- Icons: Phosphor (regular). Schrift: Inter (Google Fonts oder gebündelt).

## Files
- `Gnöckly Redesign.dc.html` – klickbarer Prototyp aller Screens (Tweaks: `metaPosition`, `theme`).
- `Gnöckly Aktuell.dc.html` – Nachbau des Ist-Zustands zum Vergleich.
- `support.js`, `android-frame.jsx`, `_ds/nocturne-…/styles.css` – Laufzeit und Token-Stylesheet (Quelle der Farben oben).
