import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Joke } from '../core/api/models';

/** Kompakte Witz-Zeile fuer Favoriten, Rangliste und "Beste Witze": Text, darunter Autor · Kicker · Score. */
@Component({
  selector: 'gn-joke-compact',
  imports: [RouterLink],
  template: `
    <article class="card">
      <a class="body" [routerLink]="['/joke', joke().id]">
        @if (joke().title) { <div class="title">{{ joke().title }}</div> }
        <p class="text" [style.font-size.px]="size()">{{ joke().text }}</p>
      </a>
      <div class="meta">
        @if (showAuthor()) { <span class="nick">{{ joke().authorNickname }}</span> }
        @if (joke().categoryName) { <span class="kicker">{{ joke().categoryName }}</span> }
        <span class="gn-spacer"></span>
        <span class="score" [class.pos]="joke().score > 0" [class.neg]="joke().score < 0">{{ joke().score }}</span>
        @if (unfavoritable()) {
          <button type="button" class="bookmark" aria-label="Aus Favoriten entfernen" (click)="unfavorite.emit(joke())">
            <svg viewBox="0 0 256 256" width="18" height="18" aria-hidden="true"><path d="M184 224l-56-40-56 40V48a8 8 0 0 1 8-8h96a8 8 0 0 1 8 8Z" fill="currentColor" stroke="currentColor" stroke-width="16" stroke-linejoin="round" /></svg>
          </button>
        }
      </div>
    </article>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .card { padding: 12px 14px; border-radius: var(--gn-radius); background: var(--color-surface); }
    .body { display: block; color: inherit; text-decoration: none; }
    .title { margin-bottom: 4px; font-weight: 500; }
    .text {
      margin: 0; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 4; -webkit-box-orient: vertical;
      overflow: hidden; white-space: pre-wrap; text-wrap: pretty;
    }
    .meta { display: flex; align-items: center; gap: 8px; min-height: 24px; margin-top: 8px; font-size: 12px; }
    .nick { color: var(--color-accent-300); font-weight: 500; }
    .kicker { font-size: 10px; text-transform: uppercase; letter-spacing: .1em; color: var(--color-cat); }
    .score { font-weight: 500; color: var(--color-muted); }
    .score.pos { color: var(--color-up); }
    .score.neg { color: var(--color-down); }
    .bookmark { display: grid; place-items: center; width: 28px; height: 28px; margin: -2px -6px -2px 0; border: 0; background: none; color: var(--color-coin); cursor: pointer; }
  `,
})
export class JokeCompact {
  readonly joke = input.required<Joke>();
  readonly size = input(15);
  readonly showAuthor = input(true);
  /** Zeigt das gefuellte Lesezeichen, das den Favoriten entfernt. */
  readonly unfavoritable = input(false);
  readonly unfavorite = output<Joke>();
}
