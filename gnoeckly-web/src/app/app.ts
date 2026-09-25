import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from './core/auth/auth.service';
import { NativeService } from './core/native/native.service';
import { T } from './shared/i18n';

/**
 * App-Shell nach Android-Muster: Inhalt oben, persistente Bottom-Navigation mit sechs Spalten (Neu-Button an dritter Stelle).
 * Die Top-App-Bar gehoert den Seiten selbst (Titel/Back-Arrow variieren), die Bottom-Nav wird auf
 * Auth-Seiten ausgeblendet, damit Formulare den Platz bekommen.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (!native.online()) {
      <div class="gn-offline">{{ t.common.offline }}</div>
    }
    <main class="gn-main">
      <router-outlet />
    </main>
    @if (showNav()) {
      <nav class="gn-nav">
        <a routerLink="/feed" routerLinkActive="active" (click)="native.tap()">
          <svg viewBox="0 0 256 256" aria-hidden="true"><circle cx="128" cy="128" r="96" /><circle cx="92" cy="108" r="6" fill="currentColor" /><circle cx="164" cy="108" r="6" fill="currentColor" /><path d="M88 152a48 48 0 0 0 80 0" /></svg>
          <span>{{ t.nav.feed }}</span>
        </a>
        <a routerLink="/rankings" routerLinkActive="active" (click)="native.tap()">
          <svg viewBox="0 0 256 256" aria-hidden="true"><path d="M80 56h96v56a48 48 0 0 1-96 0ZM80 72H40v16a32 32 0 0 0 40 31M176 72h40v16a32 32 0 0 1-40 31M128 160v40M88 208h80" /></svg>
          <span>{{ t.nav.rankings }}</span>
        </a>
        <a class="new" routerLink="/submit" routerLinkActive="active" (click)="native.tap()" [attr.aria-label]="t.feed.submitFab">
          <span class="new__btn"><svg viewBox="0 0 256 256" aria-hidden="true"><path d="M40 128h176M128 40v176" /></svg></span>
          <span>{{ t.nav.new }}</span>
        </a>
        <a routerLink="/market" routerLinkActive="active" (click)="native.tap()">
          <svg viewBox="0 0 256 256" aria-hidden="true"><path d="M40 96 56 40h144l16 56M40 96a29 29 0 0 0 58 0 29 29 0 0 0 60 0 29 29 0 0 0 60 0M48 122v94h160v-94M104 216v-56h48v56" /></svg>
          <span>{{ t.nav.market }}</span>
        </a>
        <a routerLink="/challenges" routerLinkActive="active" (click)="native.tap()">
          <svg viewBox="0 0 256 256" aria-hidden="true"><path d="M64 224V40M64 48h128l-24 40 24 40H64" /></svg>
          <span>{{ t.nav.challenges }}</span>
        </a>
        <a [routerLink]="auth.isLoggedIn() ? '/me' : '/auth/login'" [class.active]="meActive()" (click)="native.tap()">
          <svg viewBox="0 0 256 256" aria-hidden="true"><circle cx="128" cy="96" r="56" /><path d="M32 216a96 96 0 0 1 192 0" /></svg>
          <span>{{ t.nav.me }}</span>
        </a>
      </nav>
    }
  `,
  styles: `
    :host { display: block; min-height: 100dvh; }
    .gn-offline {
      position: sticky; top: 0; z-index: 20; text-align: center; padding: 6px;
      background: var(--color-neutral-800); color: var(--color-neutral-100); font-size: 12px;
    }
    .gn-nav {
      position: fixed; left: 0; right: 0; bottom: 0; z-index: 10;
      display: grid; grid-template-columns: 1fr 1fr 72px 1fr 1fr 1fr;
      height: calc(var(--gn-nav-height) + var(--gn-safe-bottom));
      padding-bottom: var(--gn-safe-bottom);
      background: var(--color-bg);
      user-select: none;
    }
    .gn-nav::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 1px; background: var(--gn-fade-line); }
    .gn-nav a {
      display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 3px;
      text-decoration: none; color: var(--color-muted); font-size: 11px; font-weight: 500;
    }
    .gn-nav a.active { color: var(--color-accent); }
    .gn-nav svg { width: 22px; height: 22px; fill: none; stroke: currentColor; stroke-width: 16; stroke-linecap: round; stroke-linejoin: round; }
    .gn-nav .new__btn {
      display: grid; place-items: center; width: 46px; height: 46px; margin-top: -24px; border-radius: 50%;
      background: var(--color-accent); color: var(--color-bg);
      box-shadow: 0 0 0 6px var(--color-bg), 0 6px 16px rgba(0, 0, 0, .5);
    }
    .gn-nav .new { color: var(--color-muted); }
    .gn-nav .new.active { color: var(--color-accent); }
  `,
})
export class App {
  protected readonly t = T;
  protected readonly auth = inject(AuthService);
  protected readonly native = inject(NativeService);
  private readonly router = inject(Router);

  private readonly url = toSignal(
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd), map(() => this.router.url)),
    { initialValue: this.router.url },
  );

  protected readonly showNav = computed(() => !this.url().split('?')[0].startsWith('/auth/'));
  protected readonly meActive = computed(() => ['/me', '/wallet'].some((p) => this.url().split('?')[0].startsWith(p)));
}
