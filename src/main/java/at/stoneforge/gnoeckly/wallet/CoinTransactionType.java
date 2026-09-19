package at.stoneforge.gnoeckly.wallet;

public enum CoinTransactionType {
    /** Gutschrift nach AdMob-Server-Side-Verification. */
    AD_REWARD,
    /** Gebuehr beim Einreichen eines Witzes. */
    SUBMIT_FEE,
    /** Boost (zeitlich begrenzte Hervorhebung) eines eigenen Witzes. */
    BOOST,
    /** Kauf eines Stickers im Marktplatz. */
    STICKER_PURCHASE,
    /** Belohnung fuer den Autor, wenn ein Witz freigegeben wird. */
    JOKE_APPROVED,
    /** Startguthaben bei Registrierung. */
    WELCOME,
    /** Manuelle Korrektur durch den Superadmin. */
    ADMIN_ADJUSTMENT
}
