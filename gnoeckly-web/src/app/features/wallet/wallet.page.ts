import { Component, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RewardFlowService } from '../../core/ads/reward-flow.service';
import { ApiService } from '../../core/api/api.service';
import { CoinTransaction, PublicConfig } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { PageHeader } from '../../shared/page-header';
import { RelativeTimePipe } from '../../shared/pipes';

@Component({
  selector: 'gn-wallet',
  imports: [MatIconModule, PageHeader, RelativeTimePipe],
  template: `
    <gn-page-header [title]="t.wallet.title" [back]="true" />
    <section class="page">
      <div class="gn-label">{{ t.wallet.balance }}</div>
      <div class="amount"><mat-icon>toll</mat-icon>{{ auth.me()?.balance ?? 0 }}</div>
      <button type="button" class="gn-btn gn-btn--lg" (click)="watch()" [disabled]="reward.busy()">
        <mat-icon>{{ reward.pending() ? 'hourglass_top' : 'play_circle' }}</mat-icon>
        {{ reward.pending() ? t.wallet.pending : t.wallet.watchAd(config()?.coinsPerAd ?? 10) }}
      </button>

      <hr class="gn-fade-line divider" />

      <div class="gn-label">{{ t.wallet.history }}</div>
      @if (!transactions().length) { <div class="gn-empty">{{ t.wallet.empty }}</div> }
      @for (tx of transactions(); track tx.id) {
        <div class="tx">
          <span class="icon"><mat-icon>{{ icon(tx) }}</mat-icon></span>
          <div class="what">
            <div class="type">{{ t.wallet.types[tx.type] || tx.type }}</div>
            <div class="gn-muted">{{ tx.description }} · {{ tx.createdAt | relativeTime }}</div>
          </div>
          <span class="sum" [class.pos]="tx.amount > 0">{{ signed(tx.amount) }}</span>
        </div>
      }
      @if (hasMore()) { <button type="button" class="gn-btn gn-btn--secondary" (click)="loadMore()">{{ t.feed.loadMore }}</button> }
    </section>
  `,
  styles: `
    .page { max-width: 640px; margin: 0 auto; padding: 8px 20px calc(84px + var(--gn-safe-bottom)); box-sizing: border-box; display: flex; flex-direction: column; align-items: flex-start; gap: 4px; }
    .amount { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; font-size: 44px; line-height: 1.2; font-weight: 500; color: var(--color-accent-300); }
    .amount mat-icon { width: 36px; height: 36px; font-size: 36px; color: var(--color-accent); }
    .divider { align-self: stretch; margin: 20px 0 16px; }
    .tx { align-self: stretch; display: flex; align-items: center; gap: 12px; padding: 8px 0; }
    .icon { flex: none; width: 32px; height: 32px; border-radius: var(--gn-radius); display: grid; place-items: center; background: var(--color-surface); color: var(--color-accent-300); }
    .icon mat-icon { width: 18px; height: 18px; font-size: 18px; }
    .what { flex: 1; min-width: 0; }
    .type { font-size: 14px; }
    .what .gn-muted { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .sum { font-size: 14px; font-weight: 500; white-space: nowrap; color: var(--color-muted); }
    .sum.pos { color: var(--color-accent-300); }
  `,
})
export class WalletPage {
  protected readonly t = T;
  protected readonly auth = inject(AuthService);
  protected readonly reward = inject(RewardFlowService);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  readonly transactions = signal<CoinTransaction[]>([]);
  readonly config = signal<PublicConfig | null>(null);
  readonly hasMore = signal(false);
  private page = 0;

  constructor() {
    void this.api.config().then((c) => this.config.set(c)).catch(() => undefined);
    void this.load(true);
  }

  async watch(): Promise<void> {
    await this.reward.watchAd(this.config()?.coinsPerAd ?? 10);
    await this.load(true);
  }

  async loadMore(): Promise<void> {
    this.page++;
    await this.load(false);
  }

  signed(amount: number): string {
    return `${amount > 0 ? '+' : amount < 0 ? '−' : ''}${Math.abs(amount).toLocaleString('de-AT')}`;
  }

  icon(tx: CoinTransaction): string {
    switch (tx.type) {
      case 'AD_REWARD': return 'play_circle';
      case 'SUBMIT_FEE': return 'edit';
      case 'BOOST': return 'bolt';
      case 'STICKER_PURCHASE': return 'storefront';
      case 'JOKE_APPROVED': return 'verified';
      case 'WELCOME': return 'celebration';
      case 'STREAK_REWARD': return 'local_fire_department';
      case 'STREAK_FREEZE': return 'ac_unit';
      case 'DAILY_QUEST': return 'task_alt';
      default: return 'tune';
    }
  }

  private async load(reset: boolean): Promise<void> {
    if (reset) this.page = 0;
    try {
      const result = await this.api.transactions(this.page);
      this.transactions.update((list) => (reset ? result.content : [...list, ...result.content]));
      this.hasMore.set(result.page + 1 < result.totalPages);
      await this.auth.reloadMe();
    } catch (error) {
      this.toast.error(error);
    }
  }
}
