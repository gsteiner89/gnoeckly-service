import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/ui/toast.service';
import { T } from '../../shared/i18n';
import { PageHeader } from '../../shared/page-header';
import { problemOf } from '../../shared/problem-detail';

@Component({
  selector: 'gn-login',
  imports: [FormsModule, RouterLink, PageHeader],
  template: `
    <gn-page-header title="" [back]="true" />
    <form class="gn-auth" (ngSubmit)="login()">
      <div class="gn-auth__bar"></div>
      <h1>{{ t.auth.login }}</h1>
      <p class="gn-auth__sub">{{ t.auth.loginSubtitle }}</p>
      <label class="gn-field">{{ t.auth.email }}
        <input class="gn-input" type="email" autocomplete="email" required [ngModel]="email()" (ngModelChange)="email.set($event)" name="email" />
      </label>
      <label class="gn-field">{{ t.auth.password }}
        <input class="gn-input" type="password" autocomplete="current-password" required [ngModel]="password()" (ngModelChange)="password.set($event)" name="password" />
      </label>
      <button type="submit" class="gn-btn gn-btn--block" [disabled]="busy() || !email() || !password()">{{ t.auth.login }}</button>
      <p class="gn-auth__link">{{ t.auth.noAccount }} <a routerLink="/auth/register" [queryParams]="{ returnUrl: returnUrl() }">{{ t.auth.register }}</a></p>
    </form>
  `,
})
export class LoginPage {
  readonly returnUrl = input<string>('/feed');
  protected readonly t = T;
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly email = signal('');
  readonly password = signal('');
  readonly busy = signal(false);

  async login(): Promise<void> {
    this.busy.set(true);
    try {
      await this.auth.login(this.email().trim(), this.password());
      await this.router.navigateByUrl(this.returnUrl() || '/feed');
    } catch (error) {
      const status = problemOf(error).status;
      void this.toast.show(status === 401 ? T.auth.badCredentials : problemOf(error).detail || T.common.error);
    } finally {
      this.busy.set(false);
    }
  }
}
