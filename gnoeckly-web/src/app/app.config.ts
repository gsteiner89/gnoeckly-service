import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { PreloadAllModules, provideRouter, withComponentInputBinding, withInMemoryScrolling, withPreloading } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { NativeService } from './core/native/native.service';
import { PushService } from './core/push/push.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Lazy-Chunks nach dem ersten Render im Leerlauf nachladen, damit Tab-Wechsel nicht auf das Netz warten.
    provideRouter(routes, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'enabled' }), withPreloading(PreloadAllModules)),
    provideHttpClient(withInterceptors([authInterceptor])),
    // Material Symbols (Rounded) als Standard-Icon-Font, siehe index.html.
    { provide: MAT_ICON_DEFAULT_OPTIONS, useValue: { fontSet: 'material-symbols-rounded' } },
    // Stiller Refresh + Profil laden, bevor die erste Route rendert; Native-Hooks (Back-Button, Statusbar).
    provideAppInitializer(async () => {
      const auth = inject(AuthService);
      const native = inject(NativeService);
      const push = inject(PushService);
      await Promise.all([auth.init(), native.init()]);
      push.init();
    }),
  ],
};
