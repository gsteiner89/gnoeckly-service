import { Component, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { AuthorRanking, Joke, Period } from '../../core/api/models';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { JokeCompact } from '../../shared/joke-compact';
import { PageHeader } from '../../shared/page-header';

@Component({
  selector: 'gn-rankings',
  imports: [RouterLink, JokeCompact, PageHeader],
  template: `
    <gn-page-header [title]="t.rankings.title">
      <div class="gn-segment gn-segment--sm period" role="group">
        <button type="button" [class.on]="period() === 'DAY'" (click)="period.set('DAY')">{{ t.feed.day }}</button>
        <button type="button" [class.on]="period() === 'WEEK'" (click)="period.set('WEEK')">{{ t.feed.week }}</button>
        <button type="button" [class.on]="period() === 'ALL'" (click)="period.set('ALL')">{{ t.feed.allTime }}</button>
      </div>
    </gn-page-header>
    <div class="gn-tabs page" role="tablist">
      <button type="button" role="tab" [class.on]="tab() === 'jokes'" [attr.aria-selected]="tab() === 'jokes'" (click)="tab.set('jokes')">{{ t.rankings.jokes }}</button>
      <button type="button" role="tab" [class.on]="tab() === 'authors'" [attr.aria-selected]="tab() === 'authors'" (click)="tab.set('authors')">{{ t.rankings.authors }}</button>
    </div>

    <section class="page list">
      @if (tab() === 'jokes') {
        @if (!jokes().length) { <div class="gn-empty">{{ t.feed.empty }}</div> }
        @for (joke of jokes(); track joke.id; let i = $index) {
          <div class="entry">
            <span class="rank" [class.first]="i === 0" [class.podium]="i === 1 || i === 2">{{ i + 1 }}</span>
            <gn-joke-compact [joke]="joke" [size]="16" />
          </div>
        }
      } @else {
        @if (!authors().length) { <div class="gn-empty">{{ t.feed.empty }}</div> }
        @for (a of authors(); track a.userId; let i = $index) {
          <a class="row" [routerLink]="['/profile', a.userId]">
            <span class="rank" [class.first]="i === 0" [class.podium]="i === 1 || i === 2">{{ i + 1 }}</span>
            <span class="avatar">{{ a.nickname.charAt(0).toUpperCase() }}</span>
            <span class="who">
              <span class="nick">{{ a.nickname }}</span>
              <span class="gn-muted">{{ t.rankings.approved(a.approvedJokes) }}</span>
            </span>
            <span class="karma"><b>{{ a.karma }}</b><span>{{ t.me.karma }}</span></span>
          </a>
        }
      }
    </section>
  `,
  styles: `
    .period { margin-right: 12px; }
    .page { max-width: 640px; margin: 0 auto; box-sizing: border-box; }
    .list { display: flex; flex-direction: column; gap: 10px; padding: 12px 12px calc(84px + var(--gn-safe-bottom)); }
    .entry { display: flex; align-items: flex-start; gap: 6px; }
    .entry gn-joke-compact { flex: 1; }
    .rank { flex: none; width: 26px; padding-top: 10px; font-size: 20px; font-weight: 500; text-align: center; color: var(--color-muted); }
    .rank.first { color: var(--color-accent); }
    .rank.podium { color: var(--color-accent-300); }

    .row {
      display: flex; align-items: center; gap: 10px; height: 56px; color: inherit; text-decoration: none;
      border-bottom: 1px solid var(--color-divider);
    }
    .row .rank { padding-top: 0; }
    .avatar {
      flex: none; width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; font-size: 15px;
      background: var(--color-accent-800); color: var(--color-accent-100);
    }
    .who { flex: 1; min-width: 0; display: flex; flex-direction: column; }
    .nick { font-size: 15px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .karma { display: flex; flex-direction: column; align-items: flex-end; }
    .karma b { font-weight: 500; color: var(--color-accent-300); }
    .karma span { font-size: 10px; text-transform: uppercase; letter-spacing: .08em; color: var(--color-muted); }
  `,
})
export class RankingsPage {
  protected readonly t = T;
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  readonly tab = signal<'jokes' | 'authors'>('jokes');
  readonly period = signal<Period>('WEEK');
  readonly jokes = signal<Joke[]>([]);
  readonly authors = signal<AuthorRanking[]>([]);

  constructor() {
    effect(() => {
      const period = this.period();
      untracked(() => void this.load(period));
    });
  }

  private async load(period: Period): Promise<void> {
    try {
      const [jokes, authors] = await Promise.all([this.api.topJokes(period), this.api.topAuthors(period)]);
      this.jokes.set(jokes.content);
      this.authors.set(authors.content);
    } catch (error) {
      this.toast.error(error);
    }
  }
}
