import { Component, inject, signal } from '@angular/core';
import { MatBottomSheetRef, MAT_BOTTOM_SHEET_DATA } from '@angular/material/bottom-sheet';
import { JokeCategory, Period } from '../core/api/models';
import { T } from './i18n';

export interface CategorySheetData {
  categories: JokeCategory[];
  selected: string[];
  /** Einzelauswahl (Einreichen): Radio-Punkte, schliesst bei Auswahl. Sonst Mehrfachauswahl (Feed). */
  single?: boolean;
  /** Nur im Feed bei Sort = Top: Zeitraum-Auswahl. */
  period?: Period | null;
  /** Nur im Feed und nur eingeloggt: Zustand des Favoriten-Toggles; undefined blendet ihn aus. */
  favoritesOnly?: boolean;
}

export interface CategorySheetResult {
  selected: string[];
  period: Period | null;
  favoritesOnly: boolean;
}

/** Bottom-Sheet fuer Kategorie-Filter (Mehrfach) bzw. Kategorie-Auswahl (einzeln). */
@Component({
  selector: 'gn-category-sheet',
  template: `
    <div class="gn-sheet">
      <div class="gn-sheet__handle"></div>
      <h3 class="gn-title">{{ single ? t.submit.category : t.feed.filterTitle }}</h3>
      <div class="grid">
        @if (!single) {
          <button type="button" class="opt" [class.on]="!selected().length" (click)="selected.set([])">
            <span class="box"></span>{{ t.feed.all }}
          </button>
        }
        @for (c of data.categories; track c.id) {
          <button type="button" class="opt" [class.on]="selected().includes(c.id)" (click)="toggle(c.id)">
            <span class="box" [class.radio]="single"></span>{{ c.name }}
          </button>
        }
      </div>
      @if (data.favoritesOnly !== undefined) {
        <button type="button" class="opt fav" [class.on]="favoritesOnly()" (click)="favoritesOnly.set(!favoritesOnly())">
          <span class="box"></span>{{ t.feed.favorites }}
        </button>
      }
      @if (data.period) {
        <div class="gn-label section">{{ t.feed.periodTitle }}</div>
        <div class="segment">
          @for (p of periods; track p.value) {
            <button type="button" [class.on]="period() === p.value" (click)="period.set(p.value)">{{ p.label }}</button>
          }
        </div>
      }
      @if (!single) {
        <button type="button" class="done" (click)="close()">{{ t.feed.done }}</button>
      }
    </div>
  `,
  styles: `
    :host { display: block; }
    .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-top: 12px; }
    .opt {
      display: flex; align-items: center; gap: 10px; height: 44px; padding: 0 12px; border-radius: var(--gn-radius);
      border: 1px solid var(--color-divider); background: transparent; color: var(--color-text);
      font: inherit; font-size: 14px; text-align: left; cursor: pointer;
    }
    .opt.on { border-color: var(--color-accent); }
    .fav { width: 100%; margin-top: 8px; }
    .box {
      flex: none; width: 16px; height: 16px; border-radius: 4px; box-sizing: border-box;
      border: 1px solid var(--color-neutral-600); display: grid; place-items: center;
    }
    .box.radio { border-radius: 50%; }
    .on .box { background: var(--color-accent); border-color: var(--color-accent); }
    .on .box::after { content: ''; width: 8px; height: 4px; border: solid var(--color-bg); border-width: 0 0 2px 2px; transform: rotate(-45deg) translate(1px, -1px); }
    .on .box.radio::after { width: 6px; height: 6px; border: 0; border-radius: 50%; background: var(--color-bg); transform: none; }
    .section { margin: 16px 0 8px; }
    .segment { display: inline-flex; height: 36px; border-radius: var(--gn-radius); box-shadow: inset 0 0 0 1px var(--color-divider); }
    .segment button {
      padding: 0 14px; border: 0; border-radius: var(--gn-radius); background: none; color: var(--color-muted);
      font: inherit; font-size: 13px; cursor: pointer;
    }
    .segment button.on { color: var(--color-accent); box-shadow: inset 0 0 0 1px var(--color-accent); }
    .done {
      display: block; width: 100%; height: 40px; margin-top: 16px; border-radius: var(--gn-radius);
      border: 1px solid var(--color-divider); background: transparent; color: var(--color-accent);
      font: inherit; font-size: 14px; font-weight: 500; cursor: pointer;
    }
  `,
})
export class CategorySheet {
  protected readonly t = T;
  protected readonly data = inject<CategorySheetData>(MAT_BOTTOM_SHEET_DATA);
  private readonly ref = inject(MatBottomSheetRef<CategorySheet, CategorySheetResult>);

  constructor() {
    this.ref.backdropClick().subscribe(() => this.close());
  }

  protected readonly single = !!this.data.single;
  protected readonly selected = signal<string[]>([...this.data.selected]);
  protected readonly period = signal<Period>(this.data.period ?? 'WEEK');
  protected readonly favoritesOnly = signal(!!this.data.favoritesOnly);
  protected readonly periods: { value: Period; label: string }[] = [
    { value: 'DAY', label: T.feed.day },
    { value: 'WEEK', label: T.feed.week },
    { value: 'ALL', label: T.feed.allTime },
  ];

  protected toggle(id: string): void {
    if (this.single) {
      this.selected.set([id]);
      this.close();
      return;
    }
    this.selected.update((list) => (list.includes(id) ? list.filter((x) => x !== id) : [...list, id]));
  }

  protected close(): void {
    this.ref.dismiss({
      selected: this.selected(),
      period: this.data.period ? this.period() : null,
      favoritesOnly: this.favoritesOnly(),
    } satisfies CategorySheetResult);
  }
}
