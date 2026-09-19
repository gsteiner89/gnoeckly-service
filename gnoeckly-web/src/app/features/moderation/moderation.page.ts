import { Component, inject, signal } from '@angular/core';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { Joke, JokeReport } from '../../core/api/models';
import { NativeService } from '../../core/native/native.service';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { JokeCard } from '../../shared/joke-card';
import { PageHeader } from '../../shared/page-header';
import { RelativeTimePipe } from '../../shared/pipes';
import { TextSheet, TextSheetData } from '../../shared/text-sheet';

@Component({
  selector: 'gn-moderation',
  imports: [RouterLink, JokeCard, PageHeader, RelativeTimePipe],
  template: `
    <gn-page-header [title]="t.moderation.title" [back]="true" />
    <div class="gn-tabs page" role="tablist">
      <button type="button" role="tab" [class.on]="tab() === 'queue'" [attr.aria-selected]="tab() === 'queue'" (click)="tab.set('queue')">
        {{ t.moderation.queue }} @if (queue().length) { <span class="count">{{ queue().length }}</span> }
      </button>
      <button type="button" role="tab" [class.on]="tab() === 'reports'" [attr.aria-selected]="tab() === 'reports'" (click)="tab.set('reports')">
        {{ t.moderation.reports }} @if (reports().length) { <span class="count">{{ reports().length }}</span> }
      </button>
    </div>

    <section class="page list">
      @if (tab() === 'queue') {
        @if (!queue().length) { <div class="gn-empty">{{ t.moderation.emptyQueue }}</div> }
        @for (joke of queue(); track joke.id) {
          <gn-joke-card [joke]="joke" [linkToDetail]="false" [showStatus]="true">
            <button type="button" class="gn-btn gn-btn--secondary gn-btn--sm" (click)="reject(joke)">{{ t.moderation.reject }}</button>
            <button type="button" class="gn-btn gn-btn--sm" (click)="approve(joke)">{{ t.moderation.approve }}</button>
          </gn-joke-card>
        }
      } @else {
        @if (!reports().length) { <div class="gn-empty">{{ t.moderation.emptyReports }}</div> }
        @for (r of reports(); track r.id) {
          <article class="report">
            <a class="text" [routerLink]="['/joke', r.jokeId]">{{ r.jokeText }}</a>
            <div class="gn-muted">{{ t.moderation.reportedBy(r.reporterNickname) }} · {{ r.createdAt | relativeTime }}</div>
            <div class="reason">{{ r.reason }}</div>
            <div class="buttons">
              <button type="button" class="gn-btn gn-btn--ghost gn-btn--sm" (click)="deleteJoke(r)">{{ t.moderation.delete }}</button>
              <span class="gn-spacer"></span>
              <button type="button" class="gn-btn gn-btn--sm" (click)="resolve(r)">{{ t.moderation.resolve }}</button>
            </div>
          </article>
        }
      }
    </section>
  `,
  styles: `
    .page { max-width: 640px; margin: 0 auto; box-sizing: border-box; }
    .gn-tabs button { display: inline-flex; align-items: center; justify-content: center; gap: 6px; }
    .count { min-width: 18px; padding: 0 5px; box-sizing: border-box; border-radius: 9px; font-size: 11px; line-height: 18px; background: var(--color-accent-800); color: var(--color-accent-100); }
    .list { display: flex; flex-direction: column; gap: 10px; padding: 12px 12px calc(84px + var(--gn-safe-bottom)); }
    .report { display: flex; flex-direction: column; gap: 8px; padding: 14px; border-radius: var(--gn-radius); background: var(--color-surface); }
    .text { color: inherit; text-decoration: none; font-size: 15px; line-height: 1.5; white-space: pre-wrap; text-wrap: pretty; }
    .reason { padding: 8px 12px; border-radius: var(--gn-radius); font-size: 13px; background: var(--color-neutral-900); }
    .buttons { display: flex; align-items: center; gap: 8px; }
  `,
})
export class ModerationPage {
  protected readonly t = T;
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly sheet = inject(MatBottomSheet);
  private readonly native = inject(NativeService);

  readonly tab = signal<'queue' | 'reports'>('queue');
  readonly queue = signal<Joke[]>([]);
  readonly reports = signal<JokeReport[]>([]);

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    try {
      const [queue, reports] = await Promise.all([this.api.adminJokes('PENDING'), this.api.reports()]);
      this.queue.set(queue.content);
      this.reports.set(reports.content);
    } catch (error) {
      this.toast.error(error);
    }
  }

  async approve(joke: Joke): Promise<void> {
    try {
      await this.api.approve(joke.id);
      this.native.success();
      this.queue.update((list) => list.filter((j) => j.id !== joke.id));
    } catch (error) {
      this.toast.error(error);
    }
  }

  async reject(joke: Joke): Promise<void> {
    const data: TextSheetData = { title: T.moderation.reject, label: T.moderation.rejectReason, confirmLabel: T.moderation.reject, required: true };
    const ref = this.sheet.open(TextSheet, { data });
    const reason = await new Promise<string | undefined>((resolve) => ref.afterDismissed().subscribe(resolve));
    if (!reason) return;
    try {
      await this.api.reject(joke.id, reason);
      this.queue.update((list) => list.filter((j) => j.id !== joke.id));
    } catch (error) {
      this.toast.error(error);
    }
  }

  async resolve(report: JokeReport): Promise<void> {
    try {
      await this.api.resolveReport(report.id);
      this.reports.update((list) => list.filter((r) => r.id !== report.id));
    } catch (error) {
      this.toast.error(error);
    }
  }

  async deleteJoke(report: JokeReport): Promise<void> {
    try {
      await this.api.adminDeleteJoke(report.jokeId);
      await this.api.resolveReport(report.id);
      this.reports.update((list) => list.filter((r) => r.jokeId !== report.jokeId));
    } catch (error) {
      this.toast.error(error);
    }
  }
}
