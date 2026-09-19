import { Component, inject, signal } from '@angular/core';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { Joke, OwnedSticker, PublicConfig } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { NativeService } from '../../core/native/native.service';
import { ToastService } from '../../core/ui/toast.service';
import { ConfirmSheet, ConfirmSheetData } from '../../shared/confirm-sheet';
import { T } from '../../shared/i18n';
import { JokeCard } from '../../shared/joke-card';
import { JokeCompact } from '../../shared/joke-compact';
import { PageHeader } from '../../shared/page-header';
import { codeOf } from '../../shared/problem-detail';
import { StickerTile } from '../../shared/sticker-tile';
import { EditProfileSheet } from './edit-profile-sheet';

type MeTab = 'jokes' | 'favs' | 'coll';

@Component({
  selector: 'gn-me',
  imports: [MatIconModule, RouterLink, JokeCard, JokeCompact, PageHeader, StickerTile],
  template: `
    <gn-page-header [title]="t.me.title">
      <button type="button" class="gn-icon-btn" (click)="editProfile()" aria-label="Profil bearbeiten"><mat-icon>edit</mat-icon></button>
      <button type="button" class="gn-icon-btn" (click)="auth.logout()" aria-label="Abmelden"><mat-icon>logout</mat-icon></button>
    </gn-page-header>
    @if (auth.me(); as me) {
      <section class="page">
        <div class="head">
          <div class="avatar">{{ me.nickname.charAt(0).toUpperCase() }}</div>
          <div class="who">
            <div class="nick">{{ me.nickname }}</div>
            @if (me.bio) { <div class="gn-muted bio">{{ me.bio }}</div> }
          </div>
        </div>

        <div class="strip">
          <a class="cell" routerLink="/wallet"><b class="coins"><mat-icon>toll</mat-icon>{{ me.balance }}</b><span>{{ t.currency }}</span></a>
          <div class="cell"><b>{{ me.karma }}</b><span>{{ t.me.karma }}</span></div>
          <div class="cell"><b>{{ stickerCount() }}</b><span>{{ t.nav.market }}</span></div>
        </div>

        @if (me.superAdmin) {
          <div class="admin">
            <a class="gn-btn gn-btn--secondary gn-btn--sm" routerLink="/moderation"><mat-icon>gavel</mat-icon>{{ t.me.moderation }}</a>
            <a class="gn-btn gn-btn--secondary gn-btn--sm" routerLink="/joke-categories">{{ t.me.categories }}</a>
            <a class="gn-btn gn-btn--secondary gn-btn--sm" routerLink="/stickers-admin"><mat-icon>note_stack</mat-icon>{{ t.me.stickers }}</a>
          </div>
        }

        <div class="tabs" role="tablist">
          <button type="button" role="tab" [class.on]="tab() === 'jokes'" [attr.aria-selected]="tab() === 'jokes'" (click)="tab.set('jokes')">{{ t.me.myJokes }}</button>
          <button type="button" role="tab" [class.on]="tab() === 'favs'" [attr.aria-selected]="tab() === 'favs'" (click)="tab.set('favs')">
            {{ t.me.favorites }} @if (favoriteTotal() > 0) { <span class="count">{{ favoriteTotal() }}</span> }
          </button>
          <button type="button" role="tab" [class.on]="tab() === 'coll'" [attr.aria-selected]="tab() === 'coll'" (click)="tab.set('coll')">{{ t.me.collection }}</button>
        </div>

        <div class="tab">
          @switch (tab()) {
            @case ('jokes') {
              <div class="list">
                @if (!jokes().length) { <div class="gn-empty">{{ t.me.noJokes }}</div> }
                @for (joke of jokes(); track joke.id) {
                  <gn-joke-card [joke]="joke" [showStatus]="true" [linkToDetail]="joke.status === 'APPROVED'" (changed)="replace($event)">
                    @if (joke.status === 'APPROVED' && !joke.boosted) {
                      <button type="button" class="gn-btn gn-btn--ghost gn-btn--sm" (click)="boost(joke)">{{ t.me.boost(config()?.boostFee ?? 50, config()?.boostDurationHours ?? 24) }}</button>
                    }
                    @if (joke.boosted) {
                      <span class="gn-muted">{{ t.me.boostedLeft(hoursLeft(joke)) }}</span>
                    }
                    @if (joke.status === 'PENDING') {
                      <button type="button" class="gn-btn gn-btn--ghost gn-btn--sm" (click)="withdraw(joke)">{{ t.me.withdraw }}</button>
                    }
                  </gn-joke-card>
                }
                @if (hasMore()) { <button type="button" class="gn-btn gn-btn--secondary" (click)="loadMore()">{{ t.feed.loadMore }}</button> }
              </div>
            }
            @case ('favs') {
              <div class="list">
                @if (!favorites().length) { <div class="gn-empty">{{ t.me.noFavorites }}</div> }
                @for (joke of favorites(); track joke.id) {
                  <gn-joke-compact [joke]="joke" [unfavoritable]="true" (unfavorite)="removeFavorite($event)" />
                }
                @if (favoritesMore()) { <button type="button" class="gn-btn gn-btn--secondary" (click)="loadMoreFavorites()">{{ t.feed.loadMore }}</button> }
              </div>
            }
            @case ('coll') {
              @if (!stickers().length) { <div class="gn-empty">{{ t.me.noStickers }}</div> }
              <div class="grid">
                @for (s of stickers(); track s.stickerId) {
                  <gn-sticker-tile [name]="s.name" [imageUrl]="s.imageUrl" [quantity]="s.quantity" [seed]="s.slug" />
                }
              </div>
            }
          }
        </div>
      </section>
    }
  `,
  styles: `
    .page { max-width: 640px; margin: 0 auto; padding-bottom: calc(84px + var(--gn-safe-bottom)); }
    .head { display: flex; align-items: center; gap: 12px; padding: 4px 16px 12px; }
    .avatar {
      flex: none; width: 48px; height: 48px; border-radius: 50%; display: grid; place-items: center;
      font-size: 20px; font-weight: 500; background: var(--color-accent-800); color: var(--color-accent-100);
    }
    .who { min-width: 0; }
    .nick { font-size: 18px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .bio { font-size: 13px; }

    .strip {
      display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); margin: 0 16px;
      border-top: 1px solid var(--color-divider); border-bottom: 1px solid var(--color-divider);
    }
    .cell { padding: 10px 12px; color: inherit; text-decoration: none; display: flex; flex-direction: column; gap: 2px; }
    .cell + .cell { border-left: 1px solid var(--color-divider); }
    .cell b { font-size: 20px; font-weight: 500; }
    .cell span { font-size: 11px; text-transform: uppercase; letter-spacing: .08em; color: var(--color-muted); }
    .coins { display: inline-flex; align-items: center; gap: 6px; color: var(--color-accent-300); }
    .coins mat-icon { width: 18px; height: 18px; font-size: 18px; }

    .admin { display: flex; flex-wrap: wrap; gap: 8px; padding: 12px 16px 0; }
    .admin mat-icon { width: 16px; height: 16px; font-size: 16px; }

    .tabs { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); margin-top: 12px; }
    .tabs button {
      display: inline-flex; align-items: center; justify-content: center; gap: 6px; height: 40px; border: 0; background: none;
      border-bottom: 1px solid var(--color-divider); color: var(--color-muted); font: inherit; font-size: 14px; cursor: pointer;
    }
    .tabs button.on { color: var(--color-accent); border-bottom-color: var(--color-accent); }
    .count { min-width: 18px; padding: 0 5px; box-sizing: border-box; border-radius: 9px; font-size: 11px; line-height: 18px; background: var(--color-accent-800); color: var(--color-accent-100); }

    .tab { padding: 12px; }
    .list { display: flex; flex-direction: column; gap: 10px; }
    .grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
  `,
})
export class MePage {
  protected readonly t = T;
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly sheet = inject(MatBottomSheet);
  private readonly native = inject(NativeService);

  readonly tab = signal<MeTab>('jokes');
  readonly jokes = signal<Joke[]>([]);
  readonly favorites = signal<Joke[]>([]);
  readonly favoriteTotal = signal(0);
  readonly stickers = signal<OwnedSticker[]>([]);
  readonly config = signal<PublicConfig | null>(null);
  readonly hasMore = signal(false);
  readonly favoritesMore = signal(false);
  private page = 0;
  private favoritesPage = 0;

  constructor() {
    void this.api.config().then((c) => this.config.set(c)).catch(() => undefined);
    void this.reload();
  }

  stickerCount(): number {
    return this.stickers().reduce((sum, s) => sum + s.quantity, 0);
  }

  hoursLeft(joke: Joke): number {
    return Math.max(1, Math.ceil((new Date(joke.boostedUntil ?? 0).getTime() - Date.now()) / 3_600_000));
  }

  async reload(): Promise<void> {
    this.page = 0;
    this.favoritesPage = 0;
    await Promise.all([this.loadJokes(true), this.loadFavorites(true), this.loadStickers(), this.auth.reloadMe()]);
  }

  async loadMore(): Promise<void> {
    this.page++;
    await this.loadJokes(false);
  }

  async loadMoreFavorites(): Promise<void> {
    this.favoritesPage++;
    await this.loadFavorites(false);
  }

  replace(updated: Joke): void {
    this.jokes.update((list) => list.map((j) => (j.id === updated.id ? updated : j)));
  }

  async removeFavorite(joke: Joke): Promise<void> {
    this.native.tap();
    const before = this.favorites();
    this.favorites.set(before.filter((j) => j.id !== joke.id));
    this.favoriteTotal.update((n) => Math.max(0, n - 1));
    try {
      await this.api.unfavorite(joke.id);
      void this.toast.show(T.joke.favoriteRemoved);
    } catch (error) {
      this.favorites.set(before);
      this.favoriteTotal.update((n) => n + 1);
      this.toast.error(error);
    }
  }

  async boost(joke: Joke): Promise<void> {
    const fee = this.config()?.boostFee ?? 50;
    const data: ConfirmSheetData = { title: T.me.boost(fee, this.config()?.boostDurationHours ?? 24), confirmLabel: 'Boost' };
    const ref = this.sheet.open(ConfirmSheet, { data });
    const ok = await new Promise<boolean>((resolve) => ref.afterDismissed().subscribe((r) => resolve(!!r)));
    if (!ok) return;
    try {
      this.replace(await this.api.boost(joke.id));
      this.native.success();
      void this.toast.show(T.me.boosted);
      await this.auth.reloadMe();
    } catch (error) {
      if (codeOf(error) === 'INSUFFICIENT_COINS') {
        void this.toast.show(T.submit.insufficient);
      } else {
        this.toast.error(error);
      }
    }
  }

  async withdraw(joke: Joke): Promise<void> {
    const data: ConfirmSheetData = { title: T.me.withdraw + '?', text: joke.text.slice(0, 120), confirmLabel: T.me.withdraw };
    const ref = this.sheet.open(ConfirmSheet, { data });
    const ok = await new Promise<boolean>((resolve) => ref.afterDismissed().subscribe((r) => resolve(!!r)));
    if (!ok) return;
    try {
      await this.api.withdraw(joke.id);
      this.jokes.update((list) => list.filter((j) => j.id !== joke.id));
      void this.toast.show(T.me.withdrawn);
    } catch (error) {
      this.toast.error(error);
    }
  }

  async editProfile(): Promise<void> {
    const ref = this.sheet.open(EditProfileSheet);
    await new Promise<void>((resolve) => ref.afterDismissed().subscribe(() => resolve()));
  }

  private async loadJokes(reset: boolean): Promise<void> {
    try {
      const result = await this.api.myJokes(this.page);
      this.jokes.update((list) => (reset ? result.content : [...list, ...result.content]));
      this.hasMore.set(result.page + 1 < result.totalPages);
    } catch (error) {
      this.toast.error(error);
    }
  }

  private async loadFavorites(reset: boolean): Promise<void> {
    try {
      const result = await this.api.myFavorites(this.favoritesPage);
      this.favorites.update((list) => (reset ? result.content : [...list, ...result.content]));
      this.favoriteTotal.set(result.totalElements);
      this.favoritesMore.set(result.page + 1 < result.totalPages);
    } catch {
      // Favoriten sind ein Zusatz: Fehler blockieren das Profil nicht.
    }
  }

  private async loadStickers(): Promise<void> {
    try {
      this.stickers.set(await this.api.myStickers());
    } catch {
      this.stickers.set([]);
    }
  }
}
