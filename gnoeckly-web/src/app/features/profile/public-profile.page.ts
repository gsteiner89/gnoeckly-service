import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { ApiService } from '../../core/api/api.service';
import { PublicProfile } from '../../core/api/models';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { PageHeader } from '../../shared/page-header';
import { StickerTile } from '../../shared/sticker-tile';

@Component({
  selector: 'gn-public-profile',
  imports: [PageHeader, StickerTile],
  template: `
    <gn-page-header [title]="profile()?.nickname ?? ''" [back]="true" />
    @if (profile(); as p) {
      <section class="page">
        <div class="head">
          <div class="avatar">{{ p.nickname.charAt(0).toUpperCase() }}</div>
          <div class="who">
            <div class="nick">{{ p.nickname }}</div>
            @if (p.bio) { <div class="gn-muted bio">{{ p.bio }}</div> }
          </div>
        </div>
        <div class="strip">
          <div class="cell"><b>{{ p.karma }}</b><span>{{ t.me.karma }}</span></div>
          <div class="cell"><b>{{ p.approvedJokes }}</b><span>{{ t.rankings.approvedLabel }}</span></div>
        </div>
        <div class="gn-label section">{{ t.me.collection }} · {{ p.stickers.length }}</div>
        @if (!p.stickers.length) { <p class="gn-muted empty">{{ t.me.noStickers }}</p> }
        <div class="grid">
          @for (s of p.stickers; track s.stickerId) {
            <gn-sticker-tile [name]="s.name" [imageUrl]="s.imageUrl" [quantity]="s.quantity" [seed]="s.slug" />
          }
        </div>
      </section>
    }
  `,
  styles: `
    .page { max-width: 640px; margin: 0 auto; padding-bottom: calc(84px + var(--gn-safe-bottom)); }
    .head { display: flex; align-items: center; gap: 12px; padding: 4px 16px 12px; }
    .avatar {
      flex: none; width: 48px; height: 48px; border-radius: 50%; display: grid; place-items: center;
      font-size: 20px; font-weight: 500; background: var(--color-accent-800); color: var(--color-accent-100);
    }
    .who { min-width: 0; }
    .nick { font-size: 18px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .bio { font-size: 13px; }
    .strip {
      display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin: 0 16px;
      border-top: 1px solid var(--color-divider); border-bottom: 1px solid var(--color-divider);
    }
    .cell { padding: 10px 12px; display: flex; flex-direction: column; gap: 2px; }
    .cell + .cell { border-left: 1px solid var(--color-divider); }
    .cell b { font-size: 20px; font-weight: 500; }
    .cell span { font-size: 11px; text-transform: uppercase; letter-spacing: .08em; color: var(--color-muted); }
    .section { margin: 16px 16px 8px; }
    .empty { margin: 0 16px; }
    .grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; padding: 0 16px; }
  `,
})
export class PublicProfilePage {
  readonly id = input.required<string>();
  protected readonly t = T;
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  readonly profile = signal<PublicProfile | null>(null);

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => void this.api.publicProfile(id).then((p) => this.profile.set(p)).catch((e) => this.toast.error(e)));
    });
  }
}
