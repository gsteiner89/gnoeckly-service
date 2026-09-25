import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../api/api.service';
import { MeResponse } from '../api/models';
import { TokenStore } from './token.store';

/**
 * Login/Registrierung/Refresh/Logout plus der geladene {@link MeResponse} als Signal. Nickname und
 * Gnoecken-Balance stehen nicht im JWT, deshalb wird /api/v1/me nach Login, Refresh, Reward, Kauf
 * und Einreichung neu geladen ({@link reloadMe}).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly tokens = inject(TokenStore);
  private readonly router = inject(Router);

  readonly me = signal<MeResponse | null>(null);
  readonly initialized = signal(false);
  readonly isLoggedIn = computed(() => this.me() !== null);
  readonly isSuperAdmin = computed(() => this.me()?.superAdmin === true);

  /** Vor dem Logout auszufuehrende Aufraeumarbeiten (z. B. Push-Token abmelden); laufen, solange der Access-Token noch gilt. */
  readonly logoutHooks: Array<() => Promise<void>> = [];

  private refreshInFlight: Promise<boolean> | null = null;

  /** App-Start: Refresh-Token laden, still erneuern, Profil holen. Fehler = ausgeloggt bleiben. */
  async init(): Promise<void> {
    try {
      const refreshToken = await this.tokens.loadRefreshToken();
      if (refreshToken && (await this.refresh())) {
        await this.reloadMe();
      }
    } finally {
      this.initialized.set(true);
    }
  }

  async login(email: string, password: string): Promise<void> {
    const tokens = await this.api.login(email, password);
    await this.tokens.set(tokens.accessToken, tokens.refreshToken);
    await this.reloadMe();
  }

  async register(email: string, password: string, nickname: string): Promise<void> {
    const tokens = await this.api.register(email, password, nickname);
    await this.tokens.set(tokens.accessToken, tokens.refreshToken);
    await this.reloadMe();
  }

  async logout(navigate = true): Promise<void> {
    await Promise.allSettled(this.logoutHooks.map((hook) => hook()));
    await this.tokens.clear();
    this.me.set(null);
    if (navigate) {
      await this.router.navigateByUrl('/');
    }
  }

  async reloadMe(): Promise<MeResponse | null> {
    try {
      const me = await this.api.me();
      this.me.set(me);
      return me;
    } catch {
      this.me.set(null);
      return null;
    }
  }

  /** Geteiltes In-Flight-Promise: mehrere parallele 401 loesen genau einen Refresh aus. */
  refresh(): Promise<boolean> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.doRefresh().finally(() => (this.refreshInFlight = null));
    }
    return this.refreshInFlight;
  }

  private async doRefresh(): Promise<boolean> {
    const refreshToken = this.tokens.currentRefreshToken();
    if (!refreshToken) {
      return false;
    }
    try {
      const tokens = await this.api.refresh(refreshToken);
      await this.tokens.set(tokens.accessToken, tokens.refreshToken);
      return true;
    } catch {
      await this.tokens.clear();
      this.me.set(null);
      return false;
    }
  }
}
