import { Injectable, inject, signal } from '@angular/core';
import { Location } from '@angular/common';
import { Router } from '@angular/router';
import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';
import { Haptics, ImpactStyle, NotificationType } from '@capacitor/haptics';
import { Network } from '@capacitor/network';
import { SplashScreen } from '@capacitor/splash-screen';
import { StatusBar, Style } from '@capacitor/status-bar';
import { ToastService } from '../ui/toast.service';
import { T } from '../../shared/i18n';

const ROOT_ROUTES = ['/', '/feed', '/rankings', '/market', '/challenges', '/me'];

/**
 * Android-typisches Verhalten: Hardware-Back-Button navigiert zurueck, auf einer Root-Seite
 * beendet doppeltes Tippen die App; Statusleiste in Markenfarbe; Haptik bei Vote/Kauf;
 * Offline-Signal. Alles no-op im Browser (Capacitor.isNativePlatform() === false).
 */
@Injectable({ providedIn: 'root' })
export class NativeService {
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly toast = inject(ToastService);

  readonly isNative = Capacitor.isNativePlatform();
  readonly platform = Capacitor.getPlatform();
  readonly online = signal(true);
  private lastBackPress = 0;

  async init(): Promise<void> {
    try {
      const status = await Network.getStatus();
      this.online.set(status.connected);
      await Network.addListener('networkStatusChange', (s) => this.online.set(s.connected));
    } catch {
      // Network-Plugin nicht verfuegbar (z.B. Test) - online annehmen.
    }
    if (!this.isNative) {
      return;
    }
    try {
      await StatusBar.setStyle({ style: Style.Dark });
      if (this.platform === 'android') {
        await StatusBar.setBackgroundColor({ color: '#5B3DF5' });
      }
    } catch {
      // StatusBar optional
    }
    await App.addListener('backButton', () => this.onBackButton());
    try {
      await SplashScreen.hide();
    } catch {
      // bereits ausgeblendet
    }
  }

  private onBackButton(): void {
    const isRoot = ROOT_ROUTES.includes(this.router.url.split('?')[0]);
    if (!isRoot) {
      this.location.back();
      return;
    }
    const now = Date.now();
    if (now - this.lastBackPress < 2000) {
      void App.exitApp();
      return;
    }
    this.lastBackPress = now;
    void this.toast.show(T.common.exitHint, undefined, 2000);
  }

  tap(): void {
    if (this.isNative) {
      void Haptics.impact({ style: ImpactStyle.Light }).catch(() => undefined);
    }
  }

  success(): void {
    if (this.isNative) {
      void Haptics.notification({ type: NotificationType.Success }).catch(() => undefined);
    }
  }
}
