import { Component, inject, signal } from '@angular/core';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { OwnedSticker, Sticker } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { NativeService } from '../../core/native/native.service';
import { ToastService } from '../../core/ui/toast.service';
import { ConfirmSheet, ConfirmSheetData } from '../../shared/confirm-sheet';
import { T } from '../../shared/i18n';
import { PageHeader } from '../../shared/page-header';
import { codeOf } from '../../shared/problem-detail';

const TINTS = ['var(--color-accent-700)', 'var(--color-accent-600)', 'var(--color-accent-800)', 'var(--color-neutral-700)', 'var(--color-accent-500)', 'var(--color-neutral-600)'];

@Component({
  selector: 'gn-marketplace',
  imports: [MatIconModule, RouterLink, PageHeader],
  template: `
    <gn-page-header [title]="t.nav.market">
      @if (auth.isLoggedIn()) {
        <a class="pill" routerLink="/wallet" aria-label="Gnöcken"><mat-icon>toll</mat-icon>{{ auth.me()?.balance ?? 0 }}</a>
      }
    </gn-page-header>
    <section class="page">
      @if (!stickers().length) { <div class="gn-empty">{{ t.market.empty }}</div> }
      <div class="grid">
        @for (s of stickers(); track s.id) {
          <div class="item" [class.out]="s.soldOut">
            <div class="art" [style.background]="tint(s.slug)">
              @if (api.imageUrl(s.imageUrl); as src) {
                <img [src]="src" [alt]="s.name" loading="lazy" />
              } @else {
                <mat-icon class="ph">star</mat-icon>
              }
              @if (owned().get(s.id); as n) { <span class="owned">{{ t.market.ownedShort(n) }}</span> }
            </div>
            <div class="name">{{ s.name }}</div>
            <div class="gn-muted desc">{{ s.description }}</div>
            @if (s.stockTotal !== null && !s.soldOut) { <div class="left">{{ t.market.left(s.stockTotal - s.stockSold) }}</div> }
            <button type="button" class="gn-btn buy" [disabled]="s.soldOut || busy()" (click)="buy(s)">
              @if (s.soldOut) { {{ t.market.soldOut }} } @else { <mat-icon>toll</mat-icon>{{ s.price }} }
            </button>
          </div>
        }
      </div>
    </section>
  `,
  styles: `
    .pill {
      display: inline-flex; align-items: center; gap: 6px; height: 36px; margin-right: 12px; padding: 0 12px;
      border-radius: var(--gn-radius); box-shadow: inset 0 0 0 1px var(--color-divider);
      color: var(--color-text); font-size: 13px; font-weight: 500; text-decoration: none;
    }
    .pill mat-icon, .buy mat-icon { width: 15px; height: 15px; font-size: 15px; color: var(--color-accent); }
    .page { max-width: 640px; margin: 0 auto; padding: 4px 16px calc(84px + var(--gn-safe-bottom)); box-sizing: border-box; }
    .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
    .item { display: flex; flex-direction: column; gap: 4px; padding: 12px; border-radius: var(--gn-radius); background: var(--color-surface); }
    .item.out { opacity: .55; }
    .art { position: relative; aspect-ratio: 1; margin-bottom: 4px; border-radius: 10px; display: grid; place-items: center; }
    .art img { width: 70%; height: 70%; object-fit: contain; }
    .ph { width: 48px; height: 48px; font-size: 48px; color: var(--color-accent-300); }
    .owned {
      position: absolute; top: 6px; left: 6px; padding: 2px 6px; border-radius: 6px; font-size: 11px;
      background: var(--color-accent-800); color: var(--color-accent-100);
    }
    .name { font-size: 14px; font-weight: 500; }
    .desc { min-height: 34px; }
    .left { font-size: 11px; color: var(--color-accent); }
    .buy { margin-top: auto; width: 100%; height: 36px; }
  `,
})
export class MarketplacePage {
  protected readonly t = T;
  protected readonly auth = inject(AuthService);
  protected readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly sheet = inject(MatBottomSheet);
  private readonly router = inject(Router);
  private readonly native = inject(NativeService);

  readonly stickers = signal<Sticker[]>([]);
  readonly owned = signal(new Map<string, number>());
  readonly busy = signal(false);

  constructor() {
    void this.load();
  }

  /** Platzhalter-Tint (bis ein Bild da ist); stabil pro Sticker. */
  tint(seed: string): string {
    let hash = 0;
    for (const ch of seed) {
      hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
    }
    return `radial-gradient(circle at 50% 35%, ${TINTS[hash % TINTS.length]}, var(--color-neutral-900) 78%)`;
  }

  async load(): Promise<void> {
    try {
      this.stickers.set(await this.api.stickerCatalog());
      if (this.auth.isLoggedIn()) {
        const mine = await this.api.myStickers();
        this.owned.set(new Map(mine.map((s: OwnedSticker) => [s.stickerId, s.quantity])));
      }
    } catch (error) {
      this.toast.error(error);
    }
  }

  async buy(sticker: Sticker): Promise<void> {
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/auth/login'], { queryParams: { returnUrl: '/market' } });
      return;
    }
    const data: ConfirmSheetData = { title: T.market.confirm(sticker.name, sticker.price), text: sticker.description ?? undefined, confirmLabel: T.market.buy };
    const ref = this.sheet.open(ConfirmSheet, { data });
    const ok = await new Promise<boolean>((resolve) => ref.afterDismissed().subscribe((r) => resolve(!!r)));
    if (!ok) return;
    this.busy.set(true);
    try {
      await this.api.purchase(sticker.id);
      this.native.success();
      void this.toast.show(T.market.bought(sticker.name));
      await this.auth.reloadMe();
      await this.load();
    } catch (error) {
      if (codeOf(error) === 'INSUFFICIENT_COINS') {
        const goWatch = await this.toast.show(T.submit.insufficient, T.submit.watchAd, 6000);
        if (goWatch) await this.router.navigate(['/wallet']);
      } else {
        this.toast.error(error);
        await this.load();
      }
    } finally {
      this.busy.set(false);
    }
  }
}
