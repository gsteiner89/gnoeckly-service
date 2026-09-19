import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatBottomSheetRef, MAT_BOTTOM_SHEET_DATA } from '@angular/material/bottom-sheet';
import { T } from './i18n';

export interface TextSheetData {
  title: string;
  label: string;
  confirmLabel: string;
  required?: boolean;
  maxLength?: number;
  initial?: string;
}

/** Bottom-Sheet mit einem Textfeld (Ablehnungsgrund, Meldung, Sticker-Nachricht). Ergebnis: string | undefined. */
@Component({
  selector: 'gn-text-sheet',
  imports: [FormsModule],
  template: `
    <div class="gn-sheet sheet">
      <div class="gn-sheet__handle"></div>
      <h3 class="gn-title">{{ data.title }}</h3>
      <label class="gn-field">{{ data.label }}
        <textarea class="gn-input area" rows="3" [ngModel]="value()" (ngModelChange)="value.set($event)" [maxlength]="data.maxLength ?? 500" name="value"></textarea>
      </label>
      <div class="gn-row buttons">
        <button type="button" class="gn-btn gn-btn--secondary" (click)="ref.dismiss(undefined)">{{ t.common.cancel }}</button>
        <button type="button" class="gn-btn" [disabled]="data.required && !value().trim()" (click)="ref.dismiss(value().trim())">{{ data.confirmLabel }}</button>
      </div>
    </div>
  `,
  styles: `
    .sheet { display: flex; flex-direction: column; gap: 12px; }
    .area { font-size: 15px; line-height: 1.5; }
    .buttons { justify-content: flex-end; }
  `,
})
export class TextSheet {
  protected readonly t = T;
  readonly data = inject<TextSheetData>(MAT_BOTTOM_SHEET_DATA);
  readonly ref = inject(MatBottomSheetRef<TextSheet, string | undefined>);
  readonly value = signal(this.data.initial ?? '');
}
