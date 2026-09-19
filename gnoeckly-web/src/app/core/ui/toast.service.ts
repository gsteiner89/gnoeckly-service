import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { T } from '../../shared/i18n';
import { isOffline, problemOf } from '../../shared/problem-detail';

/** Snackbar-Wrapper (Android-Muster: kurze Meldung unten, optional eine Aktion). */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly snackBar = inject(MatSnackBar);

  show(message: string, action?: string, duration = 3000): Promise<boolean> {
    const ref = this.snackBar.open(message, action, { duration, verticalPosition: 'bottom' });
    return new Promise((resolve) => {
      let acted = false;
      ref.onAction().subscribe(() => (acted = true));
      ref.afterDismissed().subscribe(() => resolve(acted));
    });
  }

  /** Zeigt Midgards `detail`, bei Netzfehler den Offline-Hinweis. Gibt den `code` zurueck. */
  error(error: unknown, fallback = T.common.error): string | undefined {
    if (isOffline(error)) {
      void this.show(T.common.offline);
      return undefined;
    }
    const problem = problemOf(error);
    void this.show(problem.detail || fallback);
    return problem.code;
  }
}
