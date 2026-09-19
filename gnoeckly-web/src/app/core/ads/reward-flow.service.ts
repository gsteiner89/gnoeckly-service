import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from '../api/api.service';
import { AuthService } from '../auth/auth.service';
import { NativeService } from '../native/native.service';
import { ToastService } from '../ui/toast.service';
import { T } from '../../shared/i18n';
import { AdMobService } from './admob.service';

const POLL_DELAYS_MS = [2000, 4000, 8000, 16000, 30000];

/**
 * Kompletter Ablauf "Werbung ansehen": Ad zeigen, dann das Wallet mit Backoff pollen, bis die
 * SSV-Gutschrift aus dem Backend sichtbar ist (Google ruft den Callback asynchron, Sekunden bis
 * Minuten). Das Rewarded-Event des Clients ist kein Nachweis und loest keine Buchung aus.
 */
@Injectable({ providedIn: 'root' })
export class RewardFlowService {
  private readonly admob = inject(AdMobService);
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly native = inject(NativeService);

  readonly busy = signal(false);
  readonly pending = signal(false);

  async watchAd(coinsPerAd: number): Promise<void> {
    const me = this.auth.me();
    if (!me || this.busy()) {
      return;
    }
    if (!this.admob.available) {
      void this.toast.show(T.wallet.notAvailable);
      return;
    }
    this.busy.set(true);
    try {
      const before = (await this.api.wallet()).balance;
      const result = await this.admob.showRewarded(me.userId);
      if (result === 'failed' || result === 'unavailable') {
        void this.toast.show(T.wallet.adFailed);
        return;
      }
      if (result === 'dismissed') {
        return;
      }
      this.pending.set(true);
      void this.toast.show(T.wallet.pending);
      const credited = await this.pollUntilCredited(before);
      if (credited) {
        this.native.success();
        void this.toast.show(T.wallet.credited(coinsPerAd));
      } else {
        void this.toast.show(T.wallet.timeout, undefined, 6000);
      }
      await this.auth.reloadMe();
    } finally {
      this.pending.set(false);
      this.busy.set(false);
    }
  }

  private async pollUntilCredited(before: number): Promise<boolean> {
    for (const delay of POLL_DELAYS_MS) {
      await new Promise((resolve) => setTimeout(resolve, delay));
      try {
        if ((await this.api.wallet()).balance > before) {
          return true;
        }
      } catch {
        // Netzfehler: naechster Versuch
      }
    }
    return false;
  }
}
