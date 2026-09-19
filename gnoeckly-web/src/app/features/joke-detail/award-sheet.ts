import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatBottomSheetRef, MAT_BOTTOM_SHEET_DATA } from '@angular/material/bottom-sheet';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { OwnedSticker } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { StickerTile } from '../../shared/sticker-tile';

/** Bottom-Sheet "Sticker verleihen": eigene Sammlung (nur quantity > 0) als 4er-Kachelraster, optionale Nachricht. */
@Component({
  selector: 'gn-award-sheet',
  imports: [FormsModule, RouterLink, StickerTile],
  template: `
    <div class="gn-sheet sheet">
      <div class="gn-sheet__handle"></div>
      <h3 class="gn-title">{{ t.joke.award }}</h3>
      @if (!stickers().length) {
        <p class="gn-muted">{{ t.joke.noStickers }}</p>
        <a class="gn-btn" routerLink="/market" (click)="ref.dismiss(false)">{{ t.nav.market }}</a>
      } @else {
        <div class="grid">
          @for (s of stickers(); track s.stickerId) {
            <button type="button" class="item" [class.sel]="selected() === s.stickerId" (click)="selected.set(s.stickerId)">
              <gn-sticker-tile [name]="s.name" [imageUrl]="s.imageUrl" [quantity]="s.quantity" [seed]="s.slug" />
            </button>
          }
        </div>
        <label class="gn-field">
          <input class="gn-input gn-input--compact" [placeholder]="t.joke.awardMessage" maxlength="140" [ngModel]="message()" (ngModelChange)="message.set($event)" />
        </label>
        <div class="gn-row buttons">
          <button type="button" class="gn-btn gn-btn--secondary" (click)="ref.dismiss(false)">{{ t.common.cancel }}</button>
          <button type="button" class="gn-btn" [disabled]="!selected() || busy()" (click)="award()">{{ t.joke.awardAction }}</button>
        </div>
      }
    </div>
  `,
  styles: `
    .sheet { display: flex; flex-direction: column; gap: 12px; }
    .grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
    .item { padding: 0; border: 0; border-radius: 10px; background: none; color: inherit; cursor: pointer; text-align: left; }
    .item.sel gn-sticker-tile { display: block; border-radius: 10px; box-shadow: 0 0 0 2px var(--color-accent); }
    .buttons { justify-content: flex-end; }
  `,
})
export class AwardSheet {
  protected readonly t = T;
  readonly data = inject<{ jokeId: string }>(MAT_BOTTOM_SHEET_DATA);
  readonly ref = inject(MatBottomSheetRef<AwardSheet, boolean>);
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly stickers = signal<OwnedSticker[]>([]);
  readonly selected = signal<string | null>(null);
  readonly message = signal('');
  readonly busy = signal(false);

  constructor() {
    void this.api.myStickers().then((all) => this.stickers.set(all.filter((s) => s.quantity > 0))).catch((e) => this.toast.error(e));
  }

  async award(): Promise<void> {
    const stickerId = this.selected();
    if (!stickerId) return;
    this.busy.set(true);
    try {
      await this.api.award(this.data.jokeId, stickerId, this.message().trim() || null);
      await this.auth.reloadMe();
      this.ref.dismiss(true);
    } catch (error) {
      this.toast.error(error);
    } finally {
      this.busy.set(false);
    }
  }
}
