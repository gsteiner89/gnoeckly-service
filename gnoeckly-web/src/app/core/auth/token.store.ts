import { Injectable, signal } from '@angular/core';
import { Preferences } from '@capacitor/preferences';

const REFRESH_TOKEN_KEY = 'gnoeckly.refreshToken';

/**
 * Access-Token nur im Speicher (30 Minuten Laufzeit), Refresh-Token (7 Tage) in Capacitor
 * Preferences: nativ SharedPreferences/UserDefaults, im Browser localStorage. Beim App-Start wird
 * daraus still ein neues Access-Token geholt (AuthService.init).
 */
@Injectable({ providedIn: 'root' })
export class TokenStore {
  readonly accessToken = signal<string | null>(null);
  private refreshToken: string | null = null;

  async loadRefreshToken(): Promise<string | null> {
    try {
      const { value } = await Preferences.get({ key: REFRESH_TOKEN_KEY });
      this.refreshToken = value;
    } catch {
      this.refreshToken = null;
    }
    return this.refreshToken;
  }

  currentRefreshToken(): string | null {
    return this.refreshToken;
  }

  async set(accessToken: string, refreshToken: string): Promise<void> {
    this.accessToken.set(accessToken);
    this.refreshToken = refreshToken;
    try {
      await Preferences.set({ key: REFRESH_TOKEN_KEY, value: refreshToken });
    } catch {
      // Ohne persistenten Speicher (z.B. Private-Mode) bleibt die Session auf den Speicher beschraenkt.
    }
  }

  async clear(): Promise<void> {
    this.accessToken.set(null);
    this.refreshToken = null;
    try {
      await Preferences.remove({ key: REFRESH_TOKEN_KEY });
    } catch {
      // siehe set()
    }
  }
}
