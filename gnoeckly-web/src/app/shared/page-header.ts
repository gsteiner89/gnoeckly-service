import { Location } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Top-App-Bar: Titel, optional Back-Arrow (Detailseiten), Aktionen per ng-content. */
@Component({
  selector: 'gn-page-header',
  imports: [MatIconModule],
  template: `
    <header class="gn-appbar">
      @if (back()) {
        <button type="button" class="gn-icon-btn" (click)="goBack()" aria-label="Zurück"><mat-icon>arrow_back</mat-icon></button>
      }
      <div class="gn-appbar__title">{{ title() }}</div>
      <ng-content />
    </header>
  `,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly back = input(false);
  private readonly location = inject(Location);

  goBack(): void {
    this.location.back();
  }
}
