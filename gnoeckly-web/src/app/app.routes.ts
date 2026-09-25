import { Routes } from '@angular/router';
import { authGuard, superadminGuard } from './core/auth/guards';

export const routes: Routes = [
  { path: '', redirectTo: 'feed', pathMatch: 'full' },
  { path: 'feed', loadComponent: () => import('./features/feed/feed.page').then((m) => m.FeedPage) },
  { path: 'joke/:id', loadComponent: () => import('./features/joke-detail/joke-detail.page').then((m) => m.JokeDetailPage) },
  { path: 'submit', canActivate: [authGuard], loadComponent: () => import('./features/submit/submit.page').then((m) => m.SubmitPage) },
  { path: 'rankings', loadComponent: () => import('./features/rankings/rankings.page').then((m) => m.RankingsPage) },
  { path: 'market', loadComponent: () => import('./features/marketplace/marketplace.page').then((m) => m.MarketplacePage) },
  { path: 'me', canActivate: [authGuard], loadComponent: () => import('./features/me/me.page').then((m) => m.MePage) },
  { path: 'challenges', canActivate: [authGuard], loadComponent: () => import('./features/challenges/challenges.page').then((m) => m.ChallengesPage) },
  { path: 'wallet', canActivate: [authGuard], loadComponent: () => import('./features/wallet/wallet.page').then((m) => m.WalletPage) },
  { path: 'profile/:id', loadComponent: () => import('./features/profile/public-profile.page').then((m) => m.PublicProfilePage) },
  { path: 'auth/login', loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage) },
  { path: 'auth/register', loadComponent: () => import('./features/auth/register.page').then((m) => m.RegisterPage) },
  { path: 'moderation', canActivate: [superadminGuard], loadComponent: () => import('./features/moderation/moderation.page').then((m) => m.ModerationPage) },
  { path: 'joke-categories', canActivate: [superadminGuard], loadComponent: () => import('./features/admin/categories-admin.page').then((m) => m.CategoriesAdminPage) },
  { path: 'stickers-admin', canActivate: [superadminGuard], loadComponent: () => import('./features/admin/stickers-admin.page').then((m) => m.StickersAdminPage) },
  { path: '**', redirectTo: 'feed' },
];
