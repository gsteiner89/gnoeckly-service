import { Injectable, inject } from '@angular/core';
import { AdMob, AdmobConsentStatus, RewardAdPluginEvents } from '@capacitor-community/admob';
import { environment } from '../../../environments/environment';
import { NativeService } from '../native/native.service';

export type RewardedResult = 'rewarded' | 'dismissed' | 'unavailable' | 'failed';

/**
 * Rewarded Ads ueber das AdMob-Plugin. Der Client schreibt NIE selbst Gnoecken gut - die Belohnung
 * kommt ausschliesslich ueber Googles Server-Side-Verification-Callback ins Backend
 * (gnoeckly-service, docs/admob-ssv-lokal.md). Dafuer wird die User-UUID als SSV-userId mitgegeben.
 * EU-Pflicht: UMP-Consent vor dem ersten Ad (AdMob liefert sonst keine/limitierte Anzeigen).
 * Im Browser (kein natives Plugin) liefert {@link showRewarded} 'unavailable'.
 */
@Injectable({ providedIn: 'root' })
export class AdMobService {
  private readonly native = inject(NativeService);
  private initialized = false;

  get available(): boolean {
    return this.native.isNative;
  }

  async init(): Promise<void> {
    if (this.initialized || !this.available) {
      return;
    }
    await AdMob.initialize({ initializeForTesting: environment.admob.isTesting });
    if (this.native.platform === 'ios') {
      try {
        await AdMob.requestTrackingAuthorization();
      } catch {
        // ATT-Prompt abgelehnt oder nicht verfuegbar - Ads laufen nicht-personalisiert weiter.
      }
    }
    try {
      const consent = await AdMob.requestConsentInfo();
      if (consent.isConsentFormAvailable && consent.status === AdmobConsentStatus.REQUIRED) {
        await AdMob.showConsentForm();
      }
    } catch {
      // UMP nicht konfiguriert (z.B. Testphase) - weiter ohne Formular.
    }
    this.initialized = true;
  }

  /** Laedt und zeigt ein Rewarded Ad; loest erst auf, wenn der User es geschlossen hat. */
  async showRewarded(userId: string): Promise<RewardedResult> {
    if (!this.available) {
      return 'unavailable';
    }
    await this.init();
    const adId = this.native.platform === 'ios' ? environment.admob.rewardedAdIdIos : environment.admob.rewardedAdIdAndroid;

    let rewarded = false;
    const rewardedListener = await AdMob.addListener(RewardAdPluginEvents.Rewarded, () => (rewarded = true));
    const dismissed = new Promise<void>((resolve) => {
      void AdMob.addListener(RewardAdPluginEvents.Dismissed, () => resolve()).then((handle) =>
        dismissed.finally(() => void handle.remove()),
      );
    });
    try {
      await AdMob.prepareRewardVideoAd({ adId, isTesting: environment.admob.isTesting, ssv: { userId } });
      await AdMob.showRewardVideoAd();
      await dismissed;
      return rewarded ? 'rewarded' : 'dismissed';
    } catch {
      return 'failed';
    } finally {
      await rewardedListener.remove();
    }
  }
}
