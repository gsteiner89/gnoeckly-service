# AdMob Rewarded Ads lokal verifizieren (Server-Side Verification)

Gnöcken werden ausschließlich über den AdMob-SSV-Callback gutgeschrieben. Der Client meldet
nach dem `Rewarded`-Event nichts an den Server, er pollt nur das Wallet. Damit sich das lokal
ohne Google testen lässt, akzeptiert das `dev`-Profil einen selbst erzeugten Schlüssel.

## Einmalig: Testschlüssel erzeugen

```bash
java tools/SsvSignTool.java keygen
```

Schreibt `src/main/resources/admob-test-keys.json` (Public Key, `key_id` 4242) und
`tools/ssv-private.key` (Private Key, steht in `.gitignore`). `application-dev.yml` zeigt mit
`gnoeckly.admob.ssv.verifier-keys-url: classpath:admob-test-keys.json` auf diese Datei.

## Callback simulieren

1. Backend mit `--spring.profiles.active=dev` starten, einen User registrieren und dessen
   `userId` aus `GET /api/v1/me` notieren.
2. Signierte URL erzeugen:

```bash
java tools/SsvSignTool.java sign <user-uuid>
```

3. Die ausgegebene URL aufrufen (Browser oder HTTPie). Antwort ist `200`, `GET /api/v1/me/wallet`
   zeigt `+10` (`gnoeckly.coinsPerAd`). Derselbe Aufruf ein zweites Mal bucht nichts mehr
   (`transaction_id` ist eindeutig, partieller Unique-Index in `gnoeckly_coin_transaction`).

## Produktion

- `gnoeckly.admob.ssv.verifier-keys-url` auf den Default
  `https://www.gstatic.com/admob/reward/verifier-keys.json` lassen (Keys werden 12 h gecacht,
  bei unbekannter `key_id` einmal nachgeladen, weil Google rotiert).
- In der AdMob-Konsole je Rewarded-Ad-Unit "Server-side verification" aktivieren und als
  Callback `https://<api-host>/api/v1/public/admob/ssv` eintragen. Die URL muss öffentlich per
  HTTPS erreichbar sein (kein Self-Signed-Zertifikat, keine Basic-Auth).
- Die App übergibt beim `prepareRewardVideoAd` die `userId` (unsere User-UUID) als SSV-`userId`;
  ohne sie bucht der Server nichts.
- Der Betrag ist die Server-Wahrheit (`gnoeckly.coinsPerAd` als SystemOption), Googles
  `reward_amount` wird nur bei Abweichung geloggt.
