import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { from, switchMap, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { TokenStore } from './token.store';

const NO_RETRY_PATHS = ['/api/v1/auth/', '/api/v1/public/register'];

/**
 * Haengt das Bearer-Token an alle Requests ans eigene Backend. Bei **401** (midgard: fehlender oder
 * abgelaufener Token) einmal Refresh und Retry; schlaegt der Refresh fehl → ausloggen und zum
 * Login mit returnUrl. Bei 403 (authentifiziert, aber nicht berechtigt) passiert bewusst nichts -
 * ein Refresh wuerde daran nichts aendern (midgard CLAUDE.md Regel 11).
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const tokens = inject(TokenStore);
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!request.url.startsWith(environment.apiBaseUrl)) {
    return next(request);
  }
  const authorized = withToken(request, tokens.accessToken());
  return next(authorized).pipe(
    catchError((error: unknown) => {
      const isUnauthorized = error instanceof HttpErrorResponse && error.status === 401;
      const retryable = !NO_RETRY_PATHS.some((path) => request.url.includes(path));
      if (!isUnauthorized || !retryable || !tokens.currentRefreshToken()) {
        return throwError(() => error);
      }
      return from(auth.refresh()).pipe(
        switchMap((refreshed) => {
          if (!refreshed) {
            void router.navigate(['/auth/login'], { queryParams: { returnUrl: router.url } });
            return throwError(() => error);
          }
          return next(withToken(request, tokens.accessToken()));
        }),
      );
    }),
  );
};

function withToken(request: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
}
