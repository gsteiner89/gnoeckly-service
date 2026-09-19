import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { Joke, JokeStickerAward } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { NativeService } from '../../core/native/native.service';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { JokeActions } from '../../shared/joke-actions';
import { PageHeader } from '../../shared/page-header';
import { RelativeTimePipe } from '../../shared/pipes';
import { TextSheet, TextSheetData } from '../../shared/text-sheet';
import { AwardSheet } from './award-sheet';

@Component({
  selector: 'gn-joke-detail',
  imports: [MatIconModule, RouterLink, JokeActions, PageHeader, RelativeTimePipe],
  template: `
    <gn-page-header [title]="(joke()?.categoryName ?? t.app).toUpperCase()" [back]="true">
      @if (joke() && auth.isLoggedIn()) {
        <button type="button" class="gn-icon-btn" (click)="report()" [attr.aria-label]="t.joke.report"><mat-icon>flag</mat-icon></button>
      }
    </gn-page-header>
    <section class="page">
      @if (joke(); as j) {
        @if (j.title) { <h1 class="title">{{ j.title }}</h1> }
        <p class="text">{{ j.text }}</p>

        <div class="author">
          <a class="who" [routerLink]="['/profile', j.authorId]">
            <span class="avatar">{{ j.authorNickname.charAt(0).toUpperCase() }}</span>
            <span class="nick">{{ j.authorNickname }}</span>
          </a>
          <span class="gn-muted">· {{ j.approvedAt ?? j.createdAt | relativeTime }}</span>
          @if (j.boosted) { <span class="gn-badge gn-badge--boost"><mat-icon>bolt</mat-icon>{{ t.feed.boosted }}</span> }
        </div>

        <div class="actions">
          <gn-joke-actions [large]="true" [bordered]="true" [joke]="j" (changed)="joke.set($event)" />
          <button type="button" class="gn-btn award" (click)="award()">{{ t.joke.award }}</button>
        </div>

        <hr class="gn-fade-line" />

        <div class="gn-label">{{ t.joke.awards }} · {{ awards().length }}</div>
        @if (!awards().length) { <p class="gn-muted">{{ t.joke.noAwards }}</p> }
        @for (a of awards(); track a.id) {
          <div class="given">
            <span class="tile">
              @if (api.imageUrl(a.imageUrl); as src) { <img [src]="src" [alt]="a.name ?? ''" /> } @else { <mat-icon>star</mat-icon> }
            </span>
            <div class="info">
              <div class="sname">{{ a.name ?? '?' }}</div>
              <div class="gn-muted">von <a [routerLink]="['/profile', a.giverId]" class="giver">{{ a.giverNickname }}</a> · {{ a.createdAt | relativeTime }}</div>
              @if (a.message) { <div class="msg">{{ a.message }}</div> }
            </div>
          </div>
        }
      } @else {
        <div class="gn-skeleton"></div>
      }
    </section>
  `,
  styles: `
    .page { max-width: 640px; margin: 0 auto; padding: 8px 20px calc(84px + var(--gn-safe-bottom)); box-sizing: border-box; display: flex; flex-direction: column; gap: 12px; }
    .title { margin: 0; font-size: 20px; font-weight: 500; }
    .text { margin: 0; font-size: 19px; line-height: 1.65; white-space: pre-wrap; text-wrap: pretty; }
    .author { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; font-size: 13px; }
    .who { display: inline-flex; align-items: center; gap: 8px; text-decoration: none; }
    .avatar { width: 24px; height: 24px; border-radius: 50%; display: grid; place-items: center; font-size: 11px; background: var(--color-accent-800); color: var(--color-accent-100); }
    .nick { color: var(--color-accent-300); font-weight: 500; }
    .gn-badge mat-icon { width: 12px; height: 12px; font-size: 12px; }
    .actions { display: flex; align-items: center; gap: 8px; }
    .award { flex: 1; height: 40px; }
    .given { display: flex; align-items: flex-start; gap: 12px; }
    .tile { flex: none; width: 36px; height: 36px; border-radius: 8px; display: grid; place-items: center; background: var(--color-accent-900); }
    .tile img { width: 100%; height: 100%; object-fit: contain; border-radius: 8px; }
    .tile mat-icon { width: 20px; height: 20px; font-size: 20px; color: var(--color-accent-300); }
    .info { min-width: 0; }
    .sname { font-size: 14px; font-weight: 500; }
    .giver { color: var(--color-accent-300); text-decoration: none; }
    .msg { margin-top: 2px; font-size: 14px; }
  `,
})
export class JokeDetailPage {
  readonly id = input.required<string>();
  protected readonly t = T;
  protected readonly auth = inject(AuthService);
  protected readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly sheet = inject(MatBottomSheet);
  private readonly router = inject(Router);
  private readonly native = inject(NativeService);

  readonly joke = signal<Joke | null>(null);
  readonly awards = signal<JokeStickerAward[]>([]);

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => void this.load(id));
    });
  }

  async load(id: string): Promise<void> {
    try {
      const [joke, awards] = await Promise.all([this.api.joke(id), this.api.jokeAwards(id)]);
      this.joke.set(joke);
      this.awards.set(awards.content);
    } catch (error) {
      this.toast.error(error);
    }
  }

  async award(): Promise<void> {
    const joke = this.joke();
    if (!joke) return;
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    if (joke.authorId === this.auth.me()?.userId) {
      void this.toast.show(T.joke.ownJoke);
      return;
    }
    const ref = this.sheet.open(AwardSheet, { data: { jokeId: joke.id } });
    const awarded = await new Promise<boolean>((resolve) => ref.afterDismissed().subscribe((r) => resolve(!!r)));
    if (awarded) {
      this.native.success();
      void this.toast.show(T.joke.awardDone);
      await this.load(joke.id);
    }
  }

  async report(): Promise<void> {
    const joke = this.joke();
    if (!joke) return;
    const data: TextSheetData = { title: T.joke.report, label: T.joke.reportReason, confirmLabel: T.joke.report, required: true, maxLength: 500 };
    const ref = this.sheet.open(TextSheet, { data });
    const reason = await new Promise<string | undefined>((resolve) => ref.afterDismissed().subscribe(resolve));
    if (!reason) return;
    try {
      await this.api.report(joke.id, reason);
      void this.toast.show(T.joke.reported);
    } catch (error) {
      this.toast.error(error);
    }
  }
}
