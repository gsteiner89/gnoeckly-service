import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api/api.service';
import { JokeCategory } from '../../core/api/models';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { PageHeader } from '../../shared/page-header';

/** Kategorien anlegen, umbenennen, sortieren, (de)aktivieren, loeschen - schlanke Inline-Formulare. */
@Component({
  selector: 'gn-categories-admin',
  imports: [FormsModule, MatIconModule, PageHeader],
  template: `
    <gn-page-header [title]="t.admin.categories" [back]="true" />
    <section class="page">
      <form class="card" (ngSubmit)="create()">
        <label class="gn-field">{{ t.admin.slug }}
          <input class="gn-input gn-input--compact" required pattern="[a-z0-9-]{2,40}" [ngModel]="slug()" (ngModelChange)="slug.set($event)" name="slug" />
        </label>
        <label class="gn-field">{{ t.admin.name }}
          <input class="gn-input gn-input--compact" required maxlength="80" [ngModel]="name()" (ngModelChange)="name.set($event)" name="name" />
        </label>
        <label class="gn-field">{{ t.admin.icon }}
          <input class="gn-input gn-input--compact" maxlength="80" [ngModel]="icon()" (ngModelChange)="icon.set($event)" name="icon" />
        </label>
        <button type="submit" class="gn-btn create" [disabled]="!slug() || !name()">{{ t.admin.create }}</button>
      </form>

      <hr class="gn-fade-line" />
      <div class="gn-label">{{ t.me.categories }} · {{ categories().length }}</div>

      @for (c of categories(); track c.id) {
        <div class="card" [class.inactive]="!c.active">
          <div class="gn-row head">
            <span class="slug">{{ c.slug }}</span>
            <span class="gn-spacer"></span>
            <label class="switch">
              <input type="checkbox" [checked]="c.active" (change)="save(c, { active: $any($event.target).checked })" />
              {{ t.admin.active }}
            </label>
          </div>
          <div class="gn-row">
            <label class="gn-field grow">{{ t.admin.name }}
              <input class="gn-input gn-input--compact" [ngModel]="c.name" (ngModelChange)="c.name = $event" [name]="'name-' + c.id" />
            </label>
            <label class="gn-field sort">{{ t.admin.sortOrder }}
              <input class="gn-input gn-input--compact" type="number" [ngModel]="c.sortOrder" (ngModelChange)="c.sortOrder = +$event" [name]="'sort-' + c.id" />
            </label>
          </div>
          <div class="gn-row">
            <button type="button" class="gn-btn gn-btn--ghost gn-btn--sm" (click)="remove(c)">
              <mat-icon>delete</mat-icon>{{ t.moderation.delete }}
            </button>
            <span class="gn-spacer"></span>
            <button type="button" class="gn-btn gn-btn--secondary gn-btn--sm" (click)="save(c, {})">{{ t.admin.update }}</button>
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
    .head { min-height: 24px; }
    .slug { font-size: 12px; letter-spacing: .04em; color: var(--color-muted); }
    .grow { flex: 1; min-width: 0; }
    .sort { width: 96px; }
    .switch { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; cursor: pointer; }
    .switch input { width: 16px; height: 16px; margin: 0; accent-color: var(--color-accent); }
    .gn-btn mat-icon { width: 16px; height: 16px; font-size: 16px; }
  `,
})
export class CategoriesAdminPage {
  protected readonly t = T;
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  readonly categories = signal<JokeCategory[]>([]);
  readonly slug = signal('');
  readonly name = signal('');
  readonly icon = signal('');

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    try {
      this.categories.set(await this.api.adminCategories());
    } catch (error) {
      this.toast.error(error);
    }
  }

  async create(): Promise<void> {
    try {
      await this.api.createCategory({ slug: this.slug().trim(), name: this.name().trim(), icon: this.icon().trim() || null, sortOrder: (this.categories().length + 1) * 10 });
      this.slug.set(''); this.name.set(''); this.icon.set('');
      void this.toast.show(T.admin.saved);
      await this.load();
    } catch (error) {
      this.toast.error(error);
    }
  }

  async save(c: JokeCategory, patch: Partial<JokeCategory>): Promise<void> {
    const next = { ...c, ...patch };
    try {
      await this.api.updateCategory(c.id, { name: next.name, icon: next.icon, sortOrder: next.sortOrder, active: next.active });
      void this.toast.show(T.admin.saved);
      await this.load();
    } catch (error) {
      this.toast.error(error);
    }
  }

  async remove(c: JokeCategory): Promise<void> {
    try {
      await this.api.deleteCategory(c.id);
      void this.toast.show(T.admin.deleted);
      await this.load();
    } catch (error) {
      this.toast.error(error);
    }
  }
}
