import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { PageHeader } from '../../shared/page-header';
import { problemOf } from '../../shared/problem-detail';

@Component({
  selector: 'gn-register',
  imports: [FormsModule, RouterLink, PageHeader],
  template: `
    <gn-page-header title="" [back]="true" />
    <form class="gn-auth" (ngSubmit)="register()">
      <div class="gn-auth__bar"></div>
      <h1>{{ t.auth.register }}</h1>
      <p class="gn-auth__sub">{{ t.auth.registerSubtitle(welcomeCoins()) }}</p>
      <label class="gn-field">{{ t.auth.nickname }}
        <input class="gn-input" maxlength="20" required pattern="[A-Za-z0-9_]{3,20}" [ngModel]="nickname()" (ngModelChange)="nickname.set($event)" name="nickname" />
        <span class="gn-muted">{{ t.auth.nicknameHint }}</span>
      </label>
      <label class="gn-field">{{ t.auth.email }}
        <input class="gn-input" type="email" autocomplete="email" required [ngModel]="email()" (ngModelChange)="email.set($event)" name="email" />
      </label>
      <label class="gn-field">{{ t.auth.password }}
        <input class="gn-input" type="password" autocomplete="new-password" minlength="8" required [ngModel]="password()" (ngModelChange)="password.set($event)" name="password" />
      </label>
      <button type="submit" class="gn-btn gn-btn--block" [disabled]="busy() || !valid()">{{ t.auth.register }}</button>
      <p class="gn-auth__link">{{ t.auth.haveAccount }} <a routerLink="/auth/login" [queryParams]="{ returnUrl: returnUrl() }">{{ t.auth.login }}</a></p>
    </form>
  `,
})
export class RegisterPage {
  readonly returnUrl = input<string>('/feed');
  protected readonly t = T;
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly nickname = signal('');
  readonly email = signal('');
  readonly password = signal('');
  readonly busy = signal(false);
  readonly welcomeCoins = signal(100);

  constructor() {
    void this.api.config().then((c) => this.welcomeCoins.set(c.welcomeCoins)).catch(() => undefined);
  }

  valid(): boolean {
    return /^[A-Za-z0-9_]{3,20}$/.test(this.nickname()) && this.email().includes('@') && this.password().length >= 8;
  }

  async register(): Promise<void> {
    this.busy.set(true);
    try {
      await this.auth.register(this.email().trim(), this.password(), this.nickname().trim());
      await this.router.navigateByUrl(this.returnUrl() || '/feed');
    } catch (error) {
      const problem = problemOf(error);
      const message = problem.code === 'EMAIL_TAKEN' ? T.auth.emailTaken
        : problem.code === 'NICKNAME_TAKEN' ? T.auth.nicknameTaken
        : problem.fieldErrors?.[0]?.message || problem.detail || T.common.error;
      void this.toast.show(message);
    } finally {
      this.busy.set(false);
    }
  }
}
