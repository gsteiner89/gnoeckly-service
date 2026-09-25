import { Component, computed, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api/api.service';
import { Quest, StreakInfo } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { NativeService } from '../../core/native/native.service';
import { PushService } from '../../core/push/push.service';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { PageHeader } from '../../shared/page-header';

const MILESTONES = [7, 30, 100];

/**
 * Aufgaben-Tab: Serie (Streak, Freezes, Meilensteine) und die drei taeglichen Quests. Alles wird
 * serverseitig berechnet; die Seite zeigt nur an und loest ueber die Endpoints ein.
 */
@Component({
  selector: 'gn-challenges',
  imports: [MatIconModule, PageHeader],
  template: `
    <gn-page-header [title]="t.challenges.title" />
    <section class="page">
      @if (streak(); as s) {
        <div class="card streak">
          <div class="row">
            <b class="flame"><mat-icon>local_fire_department</mat-icon>{{ t.streak.days(s.current) }}</b>
            <span class="gn-muted">{{ t.streak.longest(s.longest) }}</span>
          </div>
          <div class="gn-muted text">{{ streakText(s) }}</div>

          <ol class="ladder">
            @for (day of milestones; track day) {
              <li [class.done]="s.current >= day" [class.next]="day === s.nextMilestone">
                <mat-icon>{{ s.current >= day ? 'check_circle' : 'radio_button_unchecked' }}</mat-icon>
                <span>{{ t.streak.days(day) }}</span>
              </li>
            }
          </ol>
          @if (s.nextMilestone; as day) {
            <div class="gn-muted text">{{ t.streak.next(day, s.nextMilestoneCoins ?? 0) }}</div>
          }

          <div class="row foot">
            <span class="gn-muted freeze" [title]="t.streak.freezeHint"><mat-icon>ac_unit</mat-icon>{{ t.streak.freezes(s.freezes, s.maxFreezes) }}</span>
            @if (s.freezes < s.maxFreezes) {
              <button type="button" class="gn-btn gn-btn--ghost gn-btn--sm" [disabled]="busy()" (click)="buyFreeze()">{{ t.streak.buyFreeze(s.freezePrice) }}</button>
            }
          </div>
        </div>
      }

      <div class="gn-label head">{{ t.challenges.quests }}</div>
      <div class="gn-muted text">{{ t.challenges.resetHint }}</div>
      @if (!quests().length && !loading()) { <div class="gn-empty">{{ t.challenges.noQuests }}</div> }
      @for (quest of quests(); track quest.key) {
        <div class="card quest" [class.claimed]="quest.claimed">
          <div class="row">
            <span class="title">{{ t.challenges.quest[quest.key] ? t.challenges.quest[quest.key](quest.target) : quest.key }}</span>
            <span class="reward"><mat-icon>toll</mat-icon>{{ quest.reward }}</span>
          </div>
          <div class="bar" role="progressbar" [attr.aria-valuenow]="quest.progress" aria-valuemin="0" [attr.aria-valuemax]="quest.target">
            <div class="fill" [style.width.%]="(quest.progress / quest.target) * 100"></div>
          </div>
          <div class="row foot">
            <span class="gn-muted">{{ quest.progress }} / {{ quest.target }}</span>
            @if (quest.claimed) {
              <span class="gn-muted done"><mat-icon>check</mat-icon>{{ t.challenges.claimed }}</span>
            } @else {
              <button type="button" class="gn-btn gn-btn--sm" [disabled]="!quest.claimable || busy()" (click)="claim(quest)">{{ t.challenges.claim }}</button>
            }
          </div>
        </div>
      }
      @if (push.available) {
        <div class="gn-label head">{{ t.push.title }}</div>
        <div class="card">
          <div class="gn-muted text">{{ push.enabled() ? t.push.enabled : t.push.hint }}</div>
          @if (push.enabled()) {
            <button type="button" class="gn-btn gn-btn--ghost gn-btn--sm" [disabled]="busy()" (click)="togglePush()">{{ t.push.disable }}</button>
          } @else {
            <button type="button" class="gn-btn gn-btn--sm" [disabled]="busy()" (click)="togglePush()">{{ t.push.enable }}</button>
          }
        </div>
      }
      @if (allDone()) { <div class="gn-muted text center">{{ t.challenges.allDone }}</div> }
    </section>
  `,
  styles: `
    .page { max-width: 640px; margin: 0 auto; padding: 8px 16px calc(84px + var(--gn-safe-bottom)); box-sizing: border-box; display: flex; flex-direction: column; gap: 10px; }
    .card { padding: 12px; border-radius: var(--gn-radius); box-shadow: inset 0 0 0 1px var(--color-divider); display: flex; flex-direction: column; gap: 8px; }
    .row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
    .flame { display: inline-flex; align-items: center; gap: 6px; font-size: 20px; font-weight: 500; color: var(--color-fire); }
    .flame mat-icon, .freeze mat-icon, .done mat-icon { width: 18px; height: 18px; font-size: 18px; }
    .text { font-size: 13px; }
    .center { text-align: center; }
    .head { margin-top: 8px; }
    .foot { margin-top: 2px; }
    .freeze, .done { display: inline-flex; align-items: center; gap: 4px; font-size: 13px; }

    .ladder { list-style: none; margin: 4px 0 0; padding: 0; display: flex; justify-content: space-between; }
    .ladder li { display: flex; flex-direction: column; align-items: center; gap: 2px; font-size: 12px; color: var(--color-muted); }
    .ladder li mat-icon { width: 22px; height: 22px; font-size: 22px; }
    .ladder li.done { color: var(--color-accent-300); }
    .ladder li.next { color: var(--color-text); }

    .title { font-size: 14px; }
    .reward { display: inline-flex; align-items: center; gap: 4px; font-size: 14px; font-weight: 500; color: var(--color-coin); }
    .reward mat-icon { width: 16px; height: 16px; font-size: 16px; color: var(--color-coin); }
    .bar { height: 6px; border-radius: 3px; background: var(--color-surface); overflow: hidden; }
    .fill { height: 100%; background: var(--color-accent); transition: width .3s ease; }
    .quest.claimed { opacity: .6; }
  `,
})
export class ChallengesPage {
  protected readonly t = T;
  protected readonly milestones = MILESTONES;
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly native = inject(NativeService);
  protected readonly push = inject(PushService);

  readonly streak = computed<StreakInfo | null>(() => this.auth.me()?.streak ?? null);
  readonly quests = signal<Quest[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly allDone = computed(() => this.quests().length > 0 && this.quests().every((q) => q.claimed));

  constructor() {
    void this.reload();
  }

  streakText(streak: StreakInfo): string {
    switch (streak.state) {
      case 'ACTIVE_TODAY': return T.streak.activeToday;
      case 'AT_RISK': return T.streak.atRisk;
      case 'BROKEN': return T.streak.broken;
      default: return T.streak.none;
    }
  }

  async reload(): Promise<void> {
    this.loading.set(true);
    try {
      const [quests] = await Promise.all([this.api.quests(), this.auth.reloadMe()]);
      this.quests.set(quests.quests);
    } catch (error) {
      this.toast.error(error);
    } finally {
      this.loading.set(false);
    }
  }

  async claim(quest: Quest): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.native.tap();
    this.busy.set(true);
    try {
      const result = await this.api.claimQuest(quest.key);
      this.quests.set(result.quests);
      await this.auth.reloadMe();
      this.native.success();
      void this.toast.show(T.challenges.claimedToast(quest.reward));
    } catch (error) {
      this.toast.error(error);
      await this.reload();
    } finally {
      this.busy.set(false);
    }
  }

  async togglePush(): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.native.tap();
    this.busy.set(true);
    try {
      if (this.push.enabled()) {
        await this.push.disable();
      } else if (!(await this.push.enable())) {
        void this.toast.show(T.push.denied);
      }
    } catch (error) {
      this.toast.error(error);
    } finally {
      this.busy.set(false);
    }
  }

  async buyFreeze(): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.native.tap();
    this.busy.set(true);
    try {
      await this.api.buyStreakFreeze();
      await this.auth.reloadMe();
      void this.toast.show(T.streak.freezeBought);
    } catch (error) {
      this.toast.error(error);
    } finally {
      this.busy.set(false);
    }
  }
}
