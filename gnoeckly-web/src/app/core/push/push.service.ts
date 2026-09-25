import { Injectable, Injector, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { PushNotifications } from '@capacitor/push-notifications';
import { ApiService } from '../api/api.service';
import { AuthService } from '../auth/auth.service';
import { NativeService } from '../native/native.service';
import { ToastService } from '../ui/toast.service';

/**
 * Server-Push (FCM/APNs). Nur nativ verfuegbar. Die Berechtigung wird nie beim Start erfragt,
 * sondern erst ueber {@link enable} (Button auf der Aufgaben-Seite); hat der User sie schon erteilt,
 * registriert sich das Geraet nach jedem Login automatisch. Logout meldet den Token vorher am
 * Server ab (Hook in {@link AuthService.logoutHooks}, solange der Access-Token noch gilt).
 */
@Injectable({ providedIn: 'root' })
export class PushService {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly native = inject(NativeService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly injector = inject(Injector);

  readonly available = this.native.isNative;
  /** Berechtigung erteilt und Token beim Server registriert (oder Registrierung laeuft). */
  readonly enabled = signal(false);

  private token: string | null = null;
  private initialized = false;

  /** Einmalig beim App-Start: Listener und Login-Kopplung; im Browser ein No-op. */
  init(): void {
    if (!this.available || this.initialized) {
      return;
    }
    this.initialized = true;
    void PushNotifications.addListener('registration', (registration) => {
      this.token = registration.value;
      void this.api
        .registerPushToken(registration.value, this.native.platform === 'ios' ? 'IOS' : 'ANDROID')
        .catch(() => undefined);
    });
    void PushNotifications.addListener('registrationError', () => this.enabled.set(false));
    void PushNotifications.addListener('pushNotificationReceived', (notification) => {
      const text = [notification.title, notification.body].filter(Boolean).join(': ');
      if (text) {
        void this.toast.show(text);
      }
    });
    void PushNotifications.addListener('pushNotificationActionPerformed', (action) => {
      const route = action.notification.data?.route;
      if (typeof route === 'string' && route.startsWith('/')) {
        void this.router.navigateByUrl(route);
      }
    });
    this.auth.logoutHooks.push(() => this.unregister());
    // init() laeuft nach einem await im App-Initializer (kein Injection-Context mehr): Injector explizit mitgeben.
    effect(
      () => {
        if (this.auth.isLoggedIn()) {
          void this.syncAfterLogin();
        } else {
          this.enabled.set(false);
        }
      },
      { injector: this.injector },
    );
  }

  /** Fragt die Berechtigung an und registriert das Geraet; liefert, ob Push jetzt aktiv ist. */
  async enable(): Promise<boolean> {
    if (!this.available) {
      return false;
    }
    let permission = await PushNotifications.checkPermissions();
    if (permission.receive === 'prompt' || permission.receive === 'prompt-with-rationale') {
      permission = await PushNotifications.requestPermissions();
    }
    const granted = permission.receive === 'granted';
    this.enabled.set(granted);
    if (granted) {
      await PushNotifications.register();
    }
    return granted;
  }

  /** Opt-out: Token am Server loeschen und beim Provider abmelden. */
  async disable(): Promise<void> {
    await this.unregister();
    this.enabled.set(false);
  }

  private async syncAfterLogin(): Promise<void> {
    try {
      const permission = await PushNotifications.checkPermissions();
      if (permission.receive === 'granted') {
        this.enabled.set(true);
        await PushNotifications.register();
      }
    } catch {
      this.enabled.set(false);
    }
  }

  private async unregister(): Promise<void> {
    try {
      if (this.token) {
        await this.api.unregisterPushToken(this.token);
      }
      await PushNotifications.unregister();
    } catch {
      // Abmeldung ist Best Effort: ein toter Token wird serverseitig bei der naechsten Zustellung bereinigt.
    } finally {
      this.token = null;
    }
  }
}
