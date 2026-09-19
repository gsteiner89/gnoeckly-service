import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from '../core/api/models';

/** Liest Midgards RFC-7807-Body aus einem Fehler; `code` ist der stabile, sprachunabhaengige Schluessel. */
export function problemOf(error: unknown): ProblemDetail {
  if (error instanceof HttpErrorResponse) {
    const body = error.error;
    if (body && typeof body === 'object') {
      return { status: error.status, ...(body as ProblemDetail) };
    }
    return { status: error.status, detail: error.message };
  }
  return { detail: error instanceof Error ? error.message : 'Unbekannter Fehler' };
}

export function codeOf(error: unknown): string | undefined {
  return problemOf(error).code;
}

export function isOffline(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 0;
}
