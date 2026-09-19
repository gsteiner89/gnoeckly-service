import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Wartet auf AuthService.init() (stiller Refresh beim App-Start), bevor entschieden wird. */
async function ensureInitialized(auth: AuthService): Promise<void> {
  if (auth.initialized()) {
    return;
  }
  await auth.init();
}

export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await ensureInitialized(auth);
  if (auth.isLoggedIn()) {
    return true;
  }
  return router.createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } });
};

export const superadminGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await ensureInitialized(auth);
  if (auth.isSuperAdmin()) {
    return true;
  }
  return auth.isLoggedIn()
    ? router.createUrlTree(['/'])
    : router.createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } });
};
