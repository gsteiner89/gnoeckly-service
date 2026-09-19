import { Component, inject } from '@angular/core';
import { MatBottomSheetRef, MAT_BOTTOM_SHEET_DATA } from '@angular/material/bottom-sheet';
import { T } from './i18n';

export interface ConfirmSheetData {
  title: string;
  text?: string;
  confirmLabel: string;
}

/** Bottom-Sheet-Bestaetigung (Android-Muster statt Dialog): Titel, Text, zwei Buttons. */
@Component({
  selector: 'gn-confirm-sheet',
  imports: [],
  template: `
    <div class="gn-sheet">
      <div class="gn-sheet__handle"></div>
      <h3 class="gn-title">{{ data.title }}</h3>
      @if (data.text) { <p class="gn-muted">{{ data.text }}</p> }
      <div class="gn-row actions">
        <button type="button" class="gn-btn gn-btn--secondary" (click)="ref.dismiss(false)">{{ t.common.cancel }}</button>
        <button type="button" class="gn-btn" (click)="ref.dismiss(true)">{{ data.confirmLabel }}</button>
      </div>
    </div>
  `,
  styles: `.actions { margin-top: 16px; justify-content: flex-end; }`,
})
export class ConfirmSheet {
  protected readonly t = T;
  readonly data = inject<ConfirmSheetData>(MAT_BOTTOM_SHEET_DATA);
  readonly ref = inject(MatBottomSheetRef<ConfirmSheet, boolean>);
}
