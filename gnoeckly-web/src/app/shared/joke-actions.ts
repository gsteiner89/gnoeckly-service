import { Component, inject, input, output } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../core/api/api.service';
import { Joke } from '../core/api/models';
import { AuthService } from '../core/auth/auth.service';
import { NativeService } from '../core/native/native.service';
import { ToastService } from '../core/ui/toast.service';
import { T } from './i18n';

/**
 * Lesezeichen + Vote-Cluster eines Witzes (Karte: 36 px, Detail: 40 px). Votes und Favoriten laufen
 * optimistisch (sofortige Anzeige, Rollback bei Fehler), Haptik bei jedem Tipp; ohne Login fuehrt
 * die Aktion zum Login.
 */
@Component({
  selector: 'gn-joke-actions',
  template: `
    <button type="button" class="icon-btn" [class.on]="joke().myFavorite" [class.bordered]="bordered()" (click)="toggleFavorite()"
            [attr.aria-label]="t.joke.favorite" [attr.aria-pressed]="joke().myFavorite">
      <svg viewBox="0 0 256 256" width="20" height="20" aria-hidden="true">
        <path d="M184 224l-56-40-56 40V48a8 8 0 0 1 8-8h96a8 8 0 0 1 8 8Z"
              [attr.fill]="joke().myFavorite ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="16" stroke-linejoin="round" />
      </svg>
    </button>
    <div class="votes">
      <button type="button" class="vote" [class.on]="joke().myVote === 1" (click)="vote(1)" aria-label="Upvote" [disabled]="joke().status !== 'APPROVED'">
        <svg viewBox="0 0 256 256" width="18" height="18" aria-hidden="true"><path d="M128 216V40M56 112l72-72 72 72" fill="none" stroke="currentColor" stroke-width="16" stroke-linecap="round" stroke-linejoin="round" /></svg>
      </button>
      <span class="score" [class.pos]="joke().score > 0" [class.neg]="joke().score < 0">{{ joke().score }}</span>
      <button type="button" class="vote" [class.on]="joke().myVote === -1" (click)="vote(-1)" aria-label="Downvote" [disabled]="joke().status !== 'APPROVED'">
        <svg viewBox="0 0 256 256" width="18" height="18" aria-hidden="true"><path d="M128 40v176M56 144l72 72 72-72" fill="none" stroke="currentColor" stroke-width="16" stroke-linecap="round" stroke-linejoin="round" /></svg>
      </button>
    </div>
  `,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 4px; --size: 36px; }
    :host(.lg) { --size: 40px; gap: 8px; }
    .icon-btn, .vote { display: grid; place-items: center; border: 0; background: none; color: var(--color-muted); cursor: pointer; padding: 0; }
    .icon-btn { width: var(--size); height: var(--size); border-radius: var(--gn-radius); }
    .icon-btn.bordered { box-shadow: inset 0 0 0 1px var(--color-divider); }
    .icon-btn.on { color: var(--color-accent); }
    .votes { display: inline-flex; align-items: center; height: var(--size); border-radius: var(--gn-radius); box-shadow: inset 0 0 0 1px var(--color-divider); }
    .vote { width: 40px; height: 100%; border-radius: var(--gn-radius); }
    .vote.on { color: var(--color-accent); }
    .icon-btn:hover:not(:disabled), .vote:hover:not(:disabled) { background: color-mix(in srgb, var(--color-accent) 12%, transparent); }
    .icon-btn:active:not(:disabled), .vote:active:not(:disabled) { background: color-mix(in srgb, var(--color-accent) 22%, transparent); }
    .vote:disabled { opacity: .45; cursor: default; }
    .score { min-width: 20px; text-align: center; font-size: 13px; font-weight: 500; color: var(--color-muted); }
    .pos { color: var(--color-accent-300); } .neg { color: var(--color-neutral-500); }
  `,
  host: { '[class.lg]': 'large()' },
})
export class JokeActions {
  readonly joke = input.required<Joke>();
  /** 40 px (Detail) statt 36 px (Karte); Lesezeichen bekommt dann einen Rand. */
  readonly large = input(false);
  readonly bordered = input(false);
  readonly changed = output<Joke>();

  protected readonly t = T;
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly native = inject(NativeService);
  private busy = false;
  private favoriteBusy = false;

  async vote(value: 1 | -1): Promise<void> {
    if (!this.requireLogin(T.joke.loginToVote) || this.busy) {
      return;
    }
    this.native.tap();
    const current = this.joke();
    const remove = current.myVote === value;
    const optimistic = this.applyLocally(current, remove ? null : value);
    this.changed.emit(optimistic);
    this.busy = true;
    try {
      const result = remove ? await this.api.removeVote(current.id) : await this.api.vote(current.id, value);
      this.changed.emit({ ...optimistic, upvotes: result.upvotes, downvotes: result.downvotes, score: result.score, myVote: result.myVote });
      if (!remove && this.auth.me()?.streak.state !== 'ACTIVE_TODAY') {
        void this.auth.reloadMe();
      }
    } catch (error) {
      this.changed.emit(current);
      this.toast.error(error);
    } finally {
      this.busy = false;
    }
  }

  async toggleFavorite(): Promise<void> {
    if (!this.requireLogin(T.joke.loginToVote) || this.favoriteBusy) {
      return;
    }
    this.native.tap();
    const current = this.joke();
    const adding = !current.myFavorite;
    this.changed.emit({ ...current, myFavorite: adding });
    this.favoriteBusy = true;
    try {
      await (adding ? this.api.favorite(current.id) : this.api.unfavorite(current.id));
      void this.toast.show(adding ? T.joke.favoriteAdded : T.joke.favoriteRemoved);
    } catch (error) {
      this.changed.emit(current);
      this.toast.error(error);
    } finally {
      this.favoriteBusy = false;
    }
  }

  private requireLogin(message: string): boolean {
    if (this.auth.isLoggedIn()) {
      return true;
    }
    void this.toast.show(message);
    void this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
    return false;
  }

  private applyLocally(joke: Joke, next: 1 | -1 | null): Joke {
    let { upvotes, downvotes } = joke;
    if (joke.myVote === 1) upvotes--;
    if (joke.myVote === -1) downvotes--;
    if (next === 1) upvotes++;
    if (next === -1) downvotes++;
    return { ...joke, upvotes, downvotes, score: upvotes - downvotes, myVote: next };
  }
}
