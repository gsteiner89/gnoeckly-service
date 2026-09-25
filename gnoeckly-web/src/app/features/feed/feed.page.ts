import { Component, computed, effect, inject, Injectable, OnDestroy, signal, untracked } from '@angular/core';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { FeedSort, Joke, JokeCategory, Period } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/ui/toast.service';
import { CategorySheet, CategorySheetData, CategorySheetResult } from '../../shared/category-sheet';
import { T } from '../../shared/i18n';
import { JokeCard } from '../../shared/joke-card';
import { PageHeader } from '../../shared/page-header';
import { PullToRefreshDirective } from '../../shared/pull-to-refresh.directive';

/** Merkt sich den Feed beim Verlassen, damit "Zurueck" aus dem Witz-Detail Liste, Filter und Scroll-Position wiederherstellt. */
@Injectable({ providedIn: 'root' })
class FeedMemory {
  snapshot: { sort: FeedSort; period: Period; categoryIds: string[]; favoritesOnly: boolean; jokes: Joke[]; page: number; hasMore: boolean; scrollY: number } | null = null;
}

@Component({
  selector: 'gn-feed',
  imports: [MatIconModule, RouterLink, JokeCard, PageHeader, PullToRefreshDirective],
  template: `
    <gn-page-header [title]="t.app">
      @if (auth.isLoggedIn()) {
        <a class="pill fire" routerLink="/challenges" aria-label="Serie"><mat-icon>local_fire_department</mat-icon>{{ auth.me()?.streak?.current ?? 0 }}</a>
        <a class="pill coin" routerLink="/wallet" aria-label="Gnöcken"><mat-icon>toll</mat-icon>{{ auth.me()?.balance ?? 0 }}</a>
      }
      <a class="gn-icon-btn" [routerLink]="auth.isLoggedIn() ? '/me' : '/auth/login'" [attr.aria-label]="t.nav.me"><mat-icon>account_circle</mat-icon></a>
    </gn-page-header>

    <div class="filters-bar">
      <div class="filters">
        <div class="segment" role="group">
          <button type="button" [class.on]="sort() === 'HOT'" (click)="sort.set('HOT')"><mat-icon>local_fire_department</mat-icon>{{ t.feed.hot }}</button>
          <button type="button" [class.on]="sort() === 'TOP'" (click)="sort.set('TOP')"><mat-icon>trending_up</mat-icon>{{ t.feed.top }}</button>
          <button type="button" [class.on]="sort() === 'NEW'" (click)="sort.set('NEW')"><mat-icon>schedule</mat-icon>{{ t.feed.new }}</button>
        </div>
        <span class="gn-spacer"></span>
        <button type="button" class="filter" [class.active]="filterActive()" (click)="openFilter()">
          <svg viewBox="0 0 256 256" width="16" height="16" aria-hidden="true"><path d="M40 60h176l-68 80v56l-40 20v-76Z" fill="none" stroke="currentColor" stroke-width="16" stroke-linejoin="round" /></svg>
          <span class="filter__label">{{ filterLabel() }}</span>
        </button>
      </div>
    </div>

    <section class="list" gnPullToRefresh (refresh)="reload()">
      @if (loading() && !jokes().length) {
        <div class="gn-skeleton"></div><div class="gn-skeleton"></div><div class="gn-skeleton"></div>
      } @else if (!jokes().length) {
        <div class="gn-empty">{{ t.feed.empty }}</div>
      }
      @for (joke of jokes(); track joke.id) {
        <gn-joke-card [joke]="joke" (changed)="replace($event)" />
      }
      @if (hasMore()) {
        <button type="button" class="gn-btn gn-btn--secondary" (click)="loadMore()" [disabled]="loading()">{{ t.feed.loadMore }}</button>
      }
    </section>
  `,
  styles: `
    :host { display: block; }
    .pill {
      display: inline-flex; align-items: center; gap: 6px; height: 36px; margin-right: 12px; padding: 0 12px;
      border-radius: var(--gn-radius); box-shadow: inset 0 0 0 1px var(--color-divider);
      color: var(--color-text); font-size: 13px; font-weight: 500; text-decoration: none;
    }
    .pill mat-icon, .segment mat-icon { width: 15px; height: 15px; font-size: 15px; }
    .pill.fire mat-icon { color: var(--color-fire); }
    .pill.coin mat-icon { color: var(--color-coin); }

    /* Klebt unter der Top-App-Bar (min-height + Safe-Area + 1px Rand), damit der Filter beim Scrollen erreichbar bleibt */
    .filters-bar {
      position: sticky; top: calc(var(--gn-header-height) + var(--gn-safe-top) + 1px); z-index: 14;
      background: var(--color-bg);
    }
    .filters { display: flex; align-items: center; gap: 8px; padding: 14px 16px 10px; max-width: 640px; margin: 0 auto; box-sizing: border-box; }
    .segment { display: inline-flex; height: 36px; border-radius: var(--gn-radius); box-shadow: inset 0 0 0 1px var(--color-divider); }
    .segment button {
      display: inline-flex; align-items: center; gap: 4px; padding: 0 10px; border: 0; border-radius: var(--gn-radius);
      background: none; color: var(--color-muted); font: inherit; font-size: 13px; cursor: pointer;
    }
    .segment button + button { position: relative; }
    .segment button + button::before { content: ''; position: absolute; left: 0; top: 8px; bottom: 8px; width: 1px; background: var(--color-divider); }
    .segment button.on { color: var(--color-accent); box-shadow: inset 0 0 0 1px var(--color-accent); }
    .segment button.on::before, .segment button.on + button::before { display: none; }

    .filter {
      display: inline-flex; align-items: center; gap: 6px; height: 36px; max-width: 140px; padding: 0 12px;
      border: 0; border-radius: var(--gn-radius); background: none; box-shadow: inset 0 0 0 1px var(--color-divider);
      color: var(--color-muted); font: inherit; font-size: 13px; cursor: pointer;
    }
    .filter.active { color: var(--color-accent); box-shadow: inset 0 0 0 1px var(--color-accent); }
    .filter__label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

    .list { display: flex; flex-direction: column; gap: 18px; padding: 10px 12px calc(84px + var(--gn-safe-bottom)); max-width: 640px; margin: 0 auto; box-sizing: border-box; }
  `,
})
export class FeedPage implements OnDestroy {
  protected readonly t = T;
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly sheet = inject(MatBottomSheet);
  private readonly memory = inject(FeedMemory);
  private readonly router = inject(Router);

  readonly sort = signal<FeedSort>('HOT');
  readonly period = signal<Period>('WEEK');
  readonly categoryIds = signal<string[]>([]);
  readonly favoritesOnly = signal(false);
  readonly categories = signal<JokeCategory[]>([]);
  readonly jokes = signal<Joke[]>([]);
  readonly loading = signal(false);
  readonly hasMore = signal(false);
  private page = 0;

  protected readonly filterActive = computed(() => this.categoryIds().length > 0 || this.favoritesOnly() || (this.sort() === 'TOP' && this.period() !== 'ALL'));
  protected readonly filterLabel = computed(() => {
    const ids = this.categoryIds();
    if (this.favoritesOnly() && ids.length === 0) {
      return T.feed.favorites;
    }
    const base = ids.length === 0 ? T.feed.all
      : ids.length === 1 ? (this.categories().find((c) => c.id === ids[0])?.name ?? T.feed.all)
      : T.feed.categoriesN(ids.length);
    if (this.sort() === 'TOP' && ids.length === 0) {
      return `${base} · ${{ DAY: T.feed.day, WEEK: T.feed.week, ALL: T.feed.allTime }[this.period()]}`;
    }
    return base;
  });

  constructor() {
    void this.api.categories().then((c) => this.categories.set(c)).catch(() => undefined);
    // Nur bei Browser-/Geraete-Zurueck wiederherstellen; ein Klick auf den Tab "Witze" laedt frisch.
    const saved = this.router.getCurrentNavigation()?.trigger === 'popstate' ? this.memory.snapshot : null;
    this.memory.snapshot = null;
    let restored = false;
    if (saved) {
      restored = true;
      this.sort.set(saved.sort);
      this.period.set(saved.period);
      this.categoryIds.set(saved.categoryIds);
      this.favoritesOnly.set(saved.favoritesOnly);
      this.jokes.set(saved.jokes);
      this.hasMore.set(saved.hasMore);
      this.page = saved.page;
      // Nach dem ersten Rendern der Liste die Scroll-Position setzen.
      requestAnimationFrame(() => window.scrollTo({ top: saved.scrollY, behavior: 'instant' }));
    }
    effect(() => {
      this.sort();
      this.period();
      this.categoryIds();
      this.favoritesOnly();
      if (restored) {
        restored = false; // erster Lauf: gemerkten Zustand behalten statt neu zu laden
        return;
      }
      untracked(() => void this.reload());
    });
  }

  ngOnDestroy(): void {
    this.memory.snapshot = {
      sort: this.sort(), period: this.period(), categoryIds: this.categoryIds(), favoritesOnly: this.favoritesOnly(),
      jokes: this.jokes(), page: this.page, hasMore: this.hasMore(), scrollY: window.scrollY,
    };
  }

  openFilter(): void {
    const data: CategorySheetData = {
      categories: this.categories(),
      selected: this.categoryIds(),
      period: this.sort() === 'TOP' ? this.period() : null,
      favoritesOnly: this.auth.isLoggedIn() ? this.favoritesOnly() : undefined,
    };
    this.sheet.open<CategorySheet, CategorySheetData, CategorySheetResult>(CategorySheet, { data }).afterDismissed().subscribe((result) => {
      if (!result) {
        return;
      }
      this.categoryIds.set(result.selected);
      if (this.auth.isLoggedIn()) {
        this.favoritesOnly.set(result.favoritesOnly);
      }
      if (result.period) {
        this.period.set(result.period);
      }
    });
  }

  async reload(): Promise<void> {
    this.page = 0;
    await this.load(true);
  }

  async loadMore(): Promise<void> {
    this.page++;
    await this.load(false);
  }

  replace(updated: Joke): void {
    this.jokes.update((list) => list.map((j) => (j.id === updated.id ? updated : j)));
  }

  private async load(reset: boolean): Promise<void> {
    this.loading.set(true);
    try {
      const result = await this.api.feed(this.sort(), this.sort() === 'TOP' ? this.period() : 'ALL', this.categoryIds(), this.page, this.favoritesOnly());
      this.jokes.update((list) => (reset ? result.content : [...list, ...result.content]));
      this.hasMore.set(result.page + 1 < result.totalPages);
    } catch (error) {
      this.toast.error(error);
    } finally {
      this.loading.set(false);
    }
  }
}
