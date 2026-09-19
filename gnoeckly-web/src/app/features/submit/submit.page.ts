import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { JokeCategory, PublicConfig } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { NativeService } from '../../core/native/native.service';
import { ToastService } from '../../core/ui/toast.service';
import { CategorySheet, CategorySheetData, CategorySheetResult } from '../../shared/category-sheet';
import { T } from '../../shared/i18n';
import { PageHeader } from '../../shared/page-header';
import { codeOf } from '../../shared/problem-detail';

@Component({
  selector: 'gn-submit',
  imports: [FormsModule, MatIconModule, PageHeader],
  template: `
    <gn-page-header [title]="t.submit.title" [back]="true" />
    <form class="page" (ngSubmit)="submit()">
      <div class="gn-field">{{ t.submit.category }}
        <button type="button" class="picker" (click)="pickCategory()">
          <span [class.placeholder]="!categoryName()">{{ categoryName() ?? t.submit.categoryPlaceholder }}</span>
          <svg viewBox="0 0 256 256" width="16" height="16" aria-hidden="true"><path d="M48 96l80 80 80-80" fill="none" stroke="currentColor" stroke-width="16" stroke-linecap="round" stroke-linejoin="round" /></svg>
        </button>
      </div>
      <label class="gn-field">{{ t.submit.jokeTitle }}
        <input class="gn-input gn-input--compact" maxlength="120" [ngModel]="title()" (ngModelChange)="title.set($event)" name="title" />
      </label>
      <label class="gn-field">{{ t.submit.text }}
        <textarea class="gn-input" rows="8" maxlength="2000" required [ngModel]="text()" (ngModelChange)="text.set($event)" name="text"></textarea>
        <span class="counter">{{ text().length }}/2000</span>
      </label>
      @if (config(); as c) {
        @if (auth.isSuperAdmin()) {
          <p class="fee"><mat-icon>verified_user</mat-icon>{{ t.submit.adminFree }}</p>
        } @else {
          <p class="fee"><mat-icon>toll</mat-icon>{{ t.submit.fee(c.submitFee) }} ({{ auth.me()?.balance ?? 0 }} {{ t.currency }} vorhanden)</p>
        }
      }
      <button type="submit" class="gn-btn gn-btn--lg" [disabled]="busy() || !text().trim() || !categoryId()">{{ t.submit.send }}</button>
    </form>
  `,
  styles: `
    .page { max-width: 640px; margin: 0 auto; padding: 8px 16px calc(84px + var(--gn-safe-bottom)); box-sizing: border-box; display: flex; flex-direction: column; gap: 14px; }
    .picker {
      display: flex; align-items: center; justify-content: space-between; height: 40px; padding: 0 12px;
      border: 1px solid var(--color-divider); border-radius: var(--gn-radius); background: var(--color-surface);
      color: var(--color-text); font: inherit; font-size: 15px; cursor: pointer;
    }
    .placeholder { color: var(--color-muted); }
    .counter { align-self: flex-end; font-size: 11px; color: var(--color-muted); }
    .fee { display: flex; align-items: flex-start; gap: 8px; margin: 0; font-size: 13px; color: var(--color-muted); }
    .fee mat-icon { flex: none; width: 16px; height: 16px; font-size: 16px; color: var(--color-accent); }
    .gn-btn { align-self: flex-start; }
  `,
})
export class SubmitPage {
  protected readonly t = T;
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly native = inject(NativeService);
  private readonly sheet = inject(MatBottomSheet);

  readonly categories = signal<JokeCategory[]>([]);
  readonly config = signal<PublicConfig | null>(null);
  readonly categoryId = signal<string | null>(null);
  readonly title = signal('');
  readonly text = signal('');
  readonly busy = signal(false);
  protected readonly categoryName = computed(() => this.categories().find((c) => c.id === this.categoryId())?.name ?? null);

  constructor() {
    void this.api.categories().then((c) => this.categories.set(c)).catch((e) => this.toast.error(e));
    void this.api.config().then((c) => this.config.set(c)).catch(() => undefined);
  }

  pickCategory(): void {
    const id = this.categoryId();
    const data: CategorySheetData = { categories: this.categories(), selected: id ? [id] : [], single: true };
    this.sheet.open<CategorySheet, CategorySheetData, CategorySheetResult>(CategorySheet, { data }).afterDismissed().subscribe((result) => {
      if (result?.selected.length) {
        this.categoryId.set(result.selected[0]);
      }
    });
  }

  async submit(): Promise<void> {
    const categoryId = this.categoryId();
    if (!categoryId || !this.text().trim()) return;
    this.busy.set(true);
    try {
      await this.api.submitJoke(this.title().trim() || null, this.text().trim(), categoryId);
      this.native.success();
      await this.auth.reloadMe();
      void this.toast.show(T.submit.sent);
      await this.router.navigate(['/me']);
    } catch (error) {
      if (codeOf(error) === 'INSUFFICIENT_COINS') {
        const goWatch = await this.toast.show(T.submit.insufficient, T.submit.watchAd, 6000);
        if (goWatch) await this.router.navigate(['/wallet']);
      } else {
        this.toast.error(error);
      }
    } finally {
      this.busy.set(false);
    }
  }
}
