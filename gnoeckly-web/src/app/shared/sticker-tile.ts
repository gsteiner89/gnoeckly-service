import { Component, computed, inject, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../core/api/api.service';

const TINTS = ['var(--color-accent-700)', 'var(--color-accent-600)', 'var(--color-accent-800)', 'var(--color-neutral-700)', 'var(--color-accent-500)', 'var(--color-neutral-600)'];

/** Sammlungs-Kachel: Bild (oder Platzhalter-Glyph) auf Tint-Verlauf, Mengen-Badge oben rechts, Name darunter. */
@Component({
  selector: 'gn-sticker-tile',
  imports: [MatIconModule],
  template: `
    <div class="tile" [style.background]="background()">
      @if (src(); as url) {
        <img [src]="url" [alt]="name()" loading="lazy" />
      } @else {
        <mat-icon class="glyph">star</mat-icon>
      }
      @if (quantity() > 1) { <span class="badge">{{ quantity() }}</span> }
    </div>
    <div class="name">{{ name() }}</div>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .tile { position: relative; aspect-ratio: 1; border-radius: 10px; display: grid; place-items: center; }
    .tile img { width: 70%; height: 70%; object-fit: contain; }
    .glyph { width: 30px; height: 30px; font-size: 30px; color: var(--color-accent-300); }
    .badge {
      position: absolute; top: 6px; right: 6px; min-width: 20px; height: 20px; padding: 0 5px; box-sizing: border-box;
      border-radius: 10px; display: grid; place-items: center; font-size: 11px; font-weight: 500;
      background: var(--color-accent-800); color: var(--color-accent-100);
    }
    .name { margin-top: 4px; font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `,
})
export class StickerTile {
  readonly name = input.required<string>();
  readonly imageUrl = input<string | null>(null);
  readonly quantity = input(1);
  /** Bestimmt den Tint; gleicher Sticker, gleiche Farbe. */
  readonly seed = input('');

  private readonly api = inject(ApiService);
  protected readonly src = computed(() => this.api.imageUrl(this.imageUrl()));
  protected readonly background = computed(() => {
    const s = this.seed() || this.name();
    let hash = 0;
    for (const ch of s) {
      hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
    }
    return `radial-gradient(circle at 50% 35%, ${TINTS[hash % TINTS.length]}, var(--color-surface) 78%)`;
  });
}
