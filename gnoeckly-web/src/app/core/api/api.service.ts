import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthorRanking,
  CoinTransaction,
  FeedSort,
  Joke,
  JokeCategory,
  JokeReport,
  JokeStatus,
  JokeStickerAward,
  LoginResponse,
  MeResponse,
  OwnedSticker,
  PagedResponse,
  Period,
  PublicConfig,
  PublicProfile,
  PurchaseResponse,
  Sticker,
  VoteResponse,
  Wallet,
} from './models';

/** Ein typisierter Client pro Backend-Endpoint; Promises, damit Komponenten mit Signals arbeiten koennen. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  readonly baseUrl = environment.apiBaseUrl;

  // --- Auth ---------------------------------------------------------------------------------
  login(email: string, password: string) {
    return this.post<LoginResponse>('/api/v1/auth/login', { email, password });
  }

  refresh(refreshToken: string) {
    return this.post<LoginResponse>('/api/v1/auth/refresh', { refreshToken });
  }

  register(email: string, password: string, nickname: string) {
    return this.post<LoginResponse>('/api/v1/public/register', { email, password, nickname });
  }

  // --- Public -------------------------------------------------------------------------------
  config() {
    return this.get<PublicConfig>('/api/v1/public/config');
  }

  categories() {
    return this.get<JokeCategory[]>('/api/v1/public/categories');
  }

  feed(sort: FeedSort, period: Period, categoryIds: string[], page: number, size = 20) {
    let params = new HttpParams().set('sort', sort).set('period', period).set('page', page).set('size', size);
    if (categoryIds.length) {
      params = params.set('categoryIds', categoryIds.join(','));
    }
    return this.get<PagedResponse<Joke>>('/api/v1/public/jokes', params);
  }

  joke(id: string) {
    return this.get<Joke>(`/api/v1/public/jokes/${id}`);
  }

  jokeAwards(id: string, page = 0) {
    return this.get<PagedResponse<JokeStickerAward>>(`/api/v1/public/jokes/${id}/stickers`, new HttpParams().set('page', page));
  }

  topJokes(period: Period, page = 0) {
    return this.get<PagedResponse<Joke>>('/api/v1/public/rankings/jokes', new HttpParams().set('period', period).set('page', page));
  }

  topAuthors(period: Period, page = 0) {
    return this.get<PagedResponse<AuthorRanking>>('/api/v1/public/rankings/authors', new HttpParams().set('period', period).set('page', page));
  }

  stickerCatalog() {
    return this.get<Sticker[]>('/api/v1/public/stickers');
  }

  publicProfile(userId: string) {
    return this.get<PublicProfile>(`/api/v1/public/users/${userId}/profile`);
  }

  imageUrl(relative: string | null): string | null {
    return relative ? this.baseUrl + relative : null;
  }

  // --- Ich ----------------------------------------------------------------------------------
  me() {
    return this.get<MeResponse>('/api/v1/me');
  }

  updateProfile(nickname: string, bio: string | null) {
    return this.put<MeResponse>('/api/v1/me/profile', { nickname, bio });
  }

  myJokes(page = 0) {
    return this.get<PagedResponse<Joke>>('/api/v1/me/jokes', new HttpParams().set('page', page));
  }

  myFavorites(page = 0) {
    return this.get<PagedResponse<Joke>>('/api/v1/me/favorites', new HttpParams().set('page', page));
  }

  myStickers() {
    return this.get<OwnedSticker[]>('/api/v1/me/stickers');
  }

  wallet() {
    return this.get<Wallet>('/api/v1/me/wallet');
  }

  transactions(page = 0) {
    return this.get<PagedResponse<CoinTransaction>>('/api/v1/me/wallet/transactions', new HttpParams().set('page', page));
  }

  // --- Witze --------------------------------------------------------------------------------
  submitJoke(title: string | null, text: string, categoryId: string) {
    return this.post<Joke>('/api/v1/jokes', { title, text, categoryId });
  }

  vote(jokeId: string, value: 1 | -1) {
    return this.put<VoteResponse>(`/api/v1/jokes/${jokeId}/vote`, { value });
  }

  removeVote(jokeId: string) {
    return firstValueFrom(this.http.delete<VoteResponse>(`${this.baseUrl}/api/v1/jokes/${jokeId}/vote`));
  }

  favorite(jokeId: string) {
    return firstValueFrom(this.http.post<void>(`${this.baseUrl}/api/v1/jokes/${jokeId}/favorite`, null));
  }

  unfavorite(jokeId: string) {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/api/v1/jokes/${jokeId}/favorite`));
  }

  boost(jokeId: string) {
    return this.post<Joke>(`/api/v1/jokes/${jokeId}/boost`, {});
  }

  withdraw(jokeId: string) {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/api/v1/jokes/${jokeId}`));
  }

  report(jokeId: string, reason: string) {
    return this.post<void>(`/api/v1/jokes/${jokeId}/report`, { reason });
  }

  award(jokeId: string, stickerId: string, message: string | null) {
    return this.post<JokeStickerAward>(`/api/v1/jokes/${jokeId}/stickers`, { stickerId, message });
  }

  purchase(stickerId: string) {
    return this.post<PurchaseResponse>(`/api/v1/stickers/${stickerId}/purchase`, {});
  }

  // --- Admin --------------------------------------------------------------------------------
  adminJokes(status: JokeStatus, page = 0) {
    return this.get<PagedResponse<Joke>>('/api/admin/jokes', new HttpParams().set('status', status).set('page', page));
  }

  approve(jokeId: string) {
    return this.post<Joke>(`/api/admin/jokes/${jokeId}/approve`, {});
  }

  reject(jokeId: string, reason: string) {
    return this.post<Joke>(`/api/admin/jokes/${jokeId}/reject`, { reason });
  }

  adminDeleteJoke(jokeId: string) {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/api/admin/jokes/${jokeId}`));
  }

  reports(page = 0) {
    return this.get<PagedResponse<JokeReport>>('/api/admin/reports', new HttpParams().set('page', page));
  }

  resolveReport(reportId: string) {
    return this.post<void>(`/api/admin/reports/${reportId}/resolve`, {});
  }

  adminCategories() {
    return this.get<JokeCategory[]>('/api/admin/joke-categories');
  }

  createCategory(body: { slug: string; name: string; icon: string | null; sortOrder: number }) {
    return this.post<JokeCategory>('/api/admin/joke-categories', body);
  }

  updateCategory(id: string, body: { name: string; icon: string | null; sortOrder: number; active: boolean }) {
    return this.put<JokeCategory>(`/api/admin/joke-categories/${id}`, body);
  }

  deleteCategory(id: string) {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/api/admin/joke-categories/${id}`));
  }

  adminStickers() {
    return this.get<Sticker[]>('/api/admin/stickers');
  }

  createSticker(body: Record<string, unknown>) {
    return this.post<Sticker>('/api/admin/stickers', body);
  }

  updateSticker(id: string, body: Record<string, unknown>) {
    return this.put<Sticker>(`/api/admin/stickers/${id}`, body);
  }

  deleteSticker(id: string) {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/api/admin/stickers/${id}`));
  }

  uploadStickerImage(id: string, file: File) {
    const form = new FormData();
    form.append('file', file, file.name);
    return firstValueFrom(this.http.post<Sticker>(`${this.baseUrl}/api/admin/stickers/${id}/image`, form));
  }

  // --- intern -------------------------------------------------------------------------------
  private get<T>(path: string, params?: HttpParams) {
    return firstValueFrom(this.http.get<T>(this.baseUrl + path, { params }));
  }

  private post<T>(path: string, body: unknown) {
    return firstValueFrom(this.http.post<T>(this.baseUrl + path, body));
  }

  private put<T>(path: string, body: unknown) {
    return firstValueFrom(this.http.put<T>(this.baseUrl + path, body));
  }
}
