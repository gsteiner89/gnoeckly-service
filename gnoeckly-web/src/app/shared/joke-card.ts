import { Component, inject, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api/api.service';
import { Joke } from '../core/api/models';
import { T } from './i18n';
import { JokeActions } from './joke-actions';
import { RelativeTimePipe } from './pipes';

/**
 * Witz-Karte (Nocturne) fuer Feed und "Meine Witze". Votes/Lesezeichen stecken in {@link JokeActions}.
 */
@Component({
  selector: 'gn-joke-card',
  imports: [MatIconModule, RouterLink, RelativeTimePipe, JokeActions],
  template: `
    <article class="card" [class.card--boosted]="joke().boosted">
      <header class="meta">
        @if (showStatus()) {
          <span class="gn-badge" [class.gn-badge--rejected]="joke().status === 'REJECTED'" [class.gn-badge--ok]="joke().status === 'APPROVED'">{{ t.me.status[joke().status] }}</span>
        } @else {
          <a class="author" [routerLink]="['/profile', joke().authorId]">
            <span class="avatar" aria-hidden="true">{{ initial() }}</span>
            <span class="nick">{{ joke().authorNickname }}</span>
          </a>
        }
        <span class="time">· {{ joke().approvedAt ?? joke().createdAt | relativeTime }}</span>
        <span class="gn-spacer"></span>
        @if (joke().categoryName) { <span class="kicker">{{ joke().categoryName }}</span> }
      </header>
      <a class="body" [routerLink]="linkToDetail() ? ['/joke', joke().id] : null">
        @if (joke().title) { <h3 class="title">{{ joke().title }}</h3> }
        <p class="text">{{ joke().text }}</p>
      </a>
      @if (joke().rejectionReason && showStatus()) {
        <p class="reason">{{ joke().rejectionReason }}</p>
      }
      @if (joke().stickers.length) {
        <div class="stickers">
          <span class="label">Sticker</span>
          @for (s of joke().stickers; track s.stickerId) {
            <span class="tile" [title]="s.name">
              @if (src(s.imageUrl); as url) {
                <img [src]="url" [alt]="s.name" loading="lazy" />
              } @else {
                <mat-icon class="glyph">star</mat-icon>
              }
              @if (s.count > 1) { <span class="count">{{ s.count }}</span> }
            </span>
          }
        </div>
      }
      <footer class="actions">
        <ng-content />
        <span class="gn-spacer"></span>
        <gn-joke-actions [joke]="joke()" (changed)="changed.emit($event)" />
      </footer>
    </article>
  `,
  styles: `
    :host { display: block; flex: none; }
    .card {
      display: flex; flex-direction: column; gap: 8px; overflow: hidden;
      padding: 0 0 8px; border-radius: var(--gn-radius); background: var(--color-surface);
    }
    .card--boosted { box-shadow: var(--gn-shadow-boost); }

    .meta {
      display: flex; align-items: center; gap: 6px; padding: 8px 14px; font-size: 12px;
      background: var(--color-card-header);
      border-bottom: 1px solid color-mix(in srgb, var(--color-text) 6%, transparent);
    }
    .author { display: inline-flex; align-items: center; gap: 6px; min-width: 0; text-decoration: none; }
    .avatar {
      flex: none; width: 20px; height: 20px; border-radius: 50%; display: grid; place-items: center;
      background: var(--color-accent-800); color: var(--color-accent-100); font-size: 10px; text-transform: uppercase;
    }
    .nick { color: var(--color-accent-300); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .time { color: var(--color-muted); white-space: nowrap; }
    .kicker { font-size: 10px; text-transform: uppercase; letter-spacing: .1em; color: var(--color-accent); white-space: nowrap; }

    .body { display: block; padding: 0 14px; color: inherit; text-decoration: none; }
    .title { margin: 0 0 12px; font-size: 16px; font-weight: 500; }
    .text { margin: 0; font-size: 17px; line-height: 1.6; white-space: pre-wrap; text-wrap: pretty; }
    .reason { margin: 0 14px; padding: 8px 12px; border-radius: var(--gn-radius); font-size: 13px; background: var(--color-neutral-900); }

    .stickers {
      display: flex; align-items: center; flex-wrap: wrap; gap: 6px;
      margin: 8px 14px 0; padding-top: 10px; border-top: 1px solid color-mix(in srgb, var(--color-text) 6%, transparent);
    }
    .label { margin-right: 4px; font-size: 10px; text-transform: uppercase; letter-spacing: .08em; color: var(--color-muted); }
    .tile {
      position: relative; flex: none; width: 30px; height: 30px; border-radius: 8px; display: grid; place-items: center;
      background: var(--color-accent-900); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--color-text) 8%, transparent);
    }
    .tile img { width: 100%; height: 100%; border-radius: 8px; object-fit: contain; }
    .glyph { width: 16px; height: 16px; font-size: 16px; color: var(--color-accent-300); }
    .count {
      position: absolute; top: -6px; right: -6px; min-width: 16px; height: 16px; padding: 0 3px; box-sizing: border-box;
      border-radius: 8px; display: grid; place-items: center; font-size: 10px; background: var(--color-accent); color: var(--color-bg);
    }

    .actions { display: flex; align-items: center; gap: 4px; padding: 0 8px 0 14px; }
  `,
})
export class JokeCard {
  readonly joke = input.required<Joke>();
  readonly linkToDetail = input(true);
  readonly showStatus = input(false);
  readonly changed = output<Joke>();

  protected readonly t = T;
  private readonly api = inject(ApiService);

  protected initial(): string {
    return this.joke().authorNickname.trim().charAt(0);
  }

  protected src(imageUrl: string | null): string | null {
    return this.api.imageUrl(imageUrl);
  }
}
