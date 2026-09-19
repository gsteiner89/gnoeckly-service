import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatBottomSheetRef } from '@angular/material/bottom-sheet';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { codeOf } from '../../shared/problem-detail';

@Component({
  selector: 'gn-edit-profile-sheet',
  imports: [FormsModule],
  template: `
    <div class="gn-sheet sheet">
      <div class="gn-sheet__handle"></div>
      <h3 class="gn-title">{{ t.me.editProfile }}</h3>
      <label class="gn-field">{{ t.auth.nickname }}
        <input class="gn-input gn-input--compact" maxlength="20" [ngModel]="nickname()" (ngModelChange)="nickname.set($event)" name="nickname" />
        <span class="gn-muted">{{ t.auth.nicknameHint }}</span>
      </label>
      <label class="gn-field">{{ t.me.bio }}
        <textarea class="gn-input area" rows="3" maxlength="280" [ngModel]="bio()" (ngModelChange)="bio.set($event)" name="bio"></textarea>
      </label>
      <div class="gn-row buttons">
        <button type="button" class="gn-btn gn-btn--secondary" (click)="ref.dismiss()">{{ t.common.cancel }}</button>
        <button type="button" class="gn-btn" [disabled]="busy() || !valid()" (click)="save()">{{ t.me.save }}</button>
      </div>
    </div>
  `,
  styles: `
    .sheet { display: flex; flex-direction: column; gap: 12px; }
    .area { font-size: 15px; line-height: 1.5; }
    .buttons { justify-content: flex-end; }
  `,
})
export class EditProfileSheet {
  protected readonly t = T;
  readonly ref = inject(MatBottomSheetRef<EditProfileSheet>);
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly nickname = signal(this.auth.me()?.nickname ?? '');
  readonly bio = signal(this.auth.me()?.bio ?? '');
  readonly busy = signal(false);

  valid(): boolean {
    return /^[A-Za-z0-9_]{3,20}$/.test(this.nickname());
  }

  async save(): Promise<void> {
    this.busy.set(true);
    try {
      const me = await this.api.updateProfile(this.nickname().trim(), this.bio().trim() || null);
      this.auth.me.set(me);
      this.ref.dismiss();
    } catch (error) {
      void this.toast.show(codeOf(error) === 'NICKNAME_TAKEN' ? T.auth.nicknameTaken : T.common.error);
    } finally {
      this.busy.set(false);
    }
  }
}
