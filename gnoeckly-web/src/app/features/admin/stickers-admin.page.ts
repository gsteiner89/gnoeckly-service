import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api/api.service';
import { Sticker } from '../../core/api/models';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { PageHeader } from '../../shared/page-header';

/** Sticker-Katalog pflegen: anlegen, Preis/Bestand/Aktiv aendern, Bild hochladen (PNG/WebP/SVG/JPEG <= 512 KB). */
@Component({
  selector: 'gn-stickers-admin',
  imports: [FormsModule, MatIconModule, PageHeader],
  template: `
    <gn-page-header [title]="t.admin.stickers" [back]="true" />
    <section class="page">
      <form class="card" (ngSubmit)="create()">
        <label class="gn-field">{{ t.admin.slug }}
          <input class="gn-input gn-input--compact" required pattern="[a-z0-9-]{2,40}" [ngModel]="draft.slug" (ngModelChange)="draft.slug = $event" name="slug" />
        </label>
        <label class="gn-field">{{ t.admin.name }}
          <input class="gn-input gn-input--compact" required maxlength="80" [ngModel]="draft.name" (ngModelChange)="draft.name = $event" name="name" />
        </label>
        <label class="gn-field">{{ t.admin.description }}
          <input class="gn-input gn-input--compact" maxlength="280" [ngModel]="draft.description" (ngModelChange)="draft.description = $event" name="description" />
        </label>
        <div class="gn-row">
          <label class="gn-field grow">{{ t.admin.price }}
            <input class="gn-input gn-input--compact" type="number" min="0" required [ngModel]="draft.price" (ngModelChange)="draft.price = +$event" name="price" />
          </label>
          <label class="gn-field grow">{{ t.admin.stockTotal }}
            <input class="gn-input gn-input--compact" type="number" min="1" [ngModel]="draft.stockTotal" (ngModelChange)="draft.stockTotal = $event ? +$event : null" name="stock" />
          </label>
        </div>
        <button type="submit" class="gn-btn create" [disabled]="!draft.slug || !draft.name">{{ t.admin.create }}</button>
      </form>

      <hr class="gn-fade-line" />
      <div class="gn-label">{{ t.me.stickerLabel }} · {{ stickers().length }}</div>

      @for (s of stickers(); track s.id) {
        <div class="card" [class.inactive]="!s.active">
          <div class="gn-row head">
            <span class="tile">
              @if (api.imageUrl(s.imageUrl); as src) { <img [src]="src" [alt]="s.name" /> } @else { <mat-icon>star</mat-icon> }
            </span>
            <div class="info">
              <div class="sname">{{ s.name }}</div>
              <div class="gn-muted">{{ s.slug }} · {{ s.stockSold }}{{ s.stockTotal !== null ? '/' + s.stockTotal : '' }} verkauft</div>
            </div>
            <label class="switch">
              <input type="checkbox" [checked]="s.active" (change)="save(s, { active: $any($event.target).checked })" />
              {{ t.admin.active }}
            </label>
          </div>
          <div class="gn-row">
            <label class="gn-field grow">{{ t.admin.name }}
              <input class="gn-input gn-input--compact" [ngModel]="s.name" (ngModelChange)="s.name = $event" [name]="'name-' + s.id" />
            </label>
            <label class="gn-field num">{{ t.admin.price }}
              <input class="gn-input gn-input--compact" type="number" [ngModel]="s.price" (ngModelChange)="s.price = +$event" [name]="'price-' + s.id" />
            </label>
          </div>
          <div class="gn-row buttons">
            <input type="file" accept="image/png,image/webp,image/svg+xml,image/jpeg" hidden #file (change)="upload(s, file)" />
            <button type="button" class="gn-btn gn-btn--secondary gn-btn--sm" (click)="file.click()"><mat-icon>upload</mat-icon>{{ t.admin.uploadImage }}</button>
            <button type="button" class="gn-btn gn-btn--ghost gn-btn--sm" (click)="remove(s)"><mat-icon>delete</mat-icon>{{ t.moderation.delete }}</button>
            <span class="gn-spacer"></span>
            <button type="button" class="gn-btn gn-btn--sm" (click)="save(s, {})">{{ t.admin.update }}</button>
          </div>
        </div>
      }
    </section>
  `,
  styles: `
    .page { max-width: 640px; margin: 0 auto; padding: 8px 12px calc(84px + var(--gn-safe-bottom)); box-sizing: border-box; display: flex; flex-direction: column; gap: 10px; }
    .card { display: flex; flex-direction: column; gap: 12px; padding: 14px; border-radius: var(--gn-radius); background: var(--color-surface); }
    .card.inactive { opacity: .6; }
    .create { align-self: flex-start; }
    .page > .gn-fade-line { margin: 10px 0 4px; }
    .grow { flex: 1; min-width: 0; }
    .num { width: 110px; }
    .head { gap: 12px; }
    .tile { flex: none; width: 40px; height: 40px; border-radius: 10px; display: grid; place-items: center; background: var(--color-accent-900); }
    .tile img { width: 100%; height: 100%; object-fit: contain; border-radius: 10px; }
    .tile mat-icon { width: 20px; height: 20px; font-size: 20px; color: var(--color-accent-300); }
    .info { flex: 1; min-width: 0; }
    .sname { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .switch { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; cursor: pointer; }
    .switch input { width: 16px; height: 16px; margin: 0; accent-color: var(--color-accent); }
    .buttons { flex-wrap: wrap; }
    .gn-btn mat-icon { width: 16px; height: 16px; font-size: 16px; }
  `,
})
export class StickersAdminPage {
  protected readonly t = T;
  protected readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  readonly stickers = signal<Sticker[]>([]);
  draft = this.emptyDraft();

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    try {
      this.stickers.set(await this.api.adminStickers());
    } catch (error) {
      this.toast.error(error);
    }
  }

  async create(): Promise<void> {
    try {
      await this.api.createSticker({ ...this.draft, description: this.draft.description || null, sortOrder: (this.stickers().length + 1) * 10 });
      this.draft = this.emptyDraft();
      void this.toast.show(T.admin.saved);
      await this.load();
    } catch (error) {
      this.toast.error(error);
    }
  }

  async save(s: Sticker, patch: Partial<Sticker>): Promise<void> {
    const next = { ...s, ...patch };
    try {
      await this.api.updateSticker(s.id, {
        name: next.name, description: next.description, price: next.price, stockTotal: next.stockTotal,
        availableFrom: next.availableFrom, availableUntil: next.availableUntil, sortOrder: next.sortOrder, active: next.active,
      });
      void this.toast.show(T.admin.saved);
      await this.load();
    } catch (error) {
      this.toast.error(error);
    }
  }

  async upload(s: Sticker, input: HTMLInputElement): Promise<void> {
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    try {
      await this.api.uploadStickerImage(s.id, file);
      void this.toast.show(T.admin.saved);
      await this.load();
    } catch (error) {
      this.toast.error(error);
    }
  }

  async remove(s: Sticker): Promise<void> {
    try {
      await this.api.deleteSticker(s.id);
      void this.toast.show(T.admin.deleted);
      await this.load();
    } catch (error) {
      this.toast.error(error);
    }
  }

  private emptyDraft() {
    return { slug: '', name: '', description: '', price: 10, stockTotal: null as number | null };
  }
}
