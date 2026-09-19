/**
 * Handgeschriebene Spiegelung der Backend-Records (gnoeckly-service). Feldnamen und Typen
 * entsprechen 1:1 den Java-Records; bei Aenderungen im Backend hier nachziehen
 * (Alternative fuer spaeter: openapi-generator gegen /v3/api-docs).
 */

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

export interface MeResponse {
  userId: string;
  email: string;
  nickname: string;
  bio: string | null;
  superAdmin: boolean;
  balance: number;
  karma: number;
}

export interface PublicConfig {
  submitFee: number;
  boostFee: number;
  boostDurationHours: number;
  coinsPerAd: number;
  coinsPerApprovedJoke: number;
  welcomeCoins: number;
}

export type JokeStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type FeedSort = 'HOT' | 'TOP' | 'NEW';
export type Period = 'DAY' | 'WEEK' | 'ALL';

export interface JokeStickerSummary {
  stickerId: string;
  slug: string;
  name: string;
  imageUrl: string | null;
  count: number;
}

export interface Joke {
  id: string;
  title: string | null;
  text: string;
  categoryId: string;
  categoryName: string | null;
  authorId: string;
  authorNickname: string;
  status: JokeStatus;
  upvotes: number;
  downvotes: number;
  score: number;
  boosted: boolean;
  boostedUntil: string | null;
  approvedAt: string | null;
  createdAt: string;
  myVote: 1 | -1 | null;
  rejectionReason: string | null;
  stickers: JokeStickerSummary[];
  /** Fehlt, solange das Backend Favoriten nicht liefert; dann blendet die Karte das Lesezeichen aus. */
  myFavorite?: boolean;
}

export interface JokeCategory {
  id: string;
  slug: string;
  name: string;
  icon: string | null;
  sortOrder: number;
  active: boolean;
}

export interface VoteResponse {
  jokeId: string;
  upvotes: number;
  downvotes: number;
  score: number;
  myVote: 1 | -1 | null;
}

export interface AuthorRanking {
  userId: string;
  nickname: string;
  karma: number;
  approvedJokes: number;
}

export interface Wallet {
  balance: number;
  updatedAt: string;
}

export type CoinTransactionType =
  | 'AD_REWARD'
  | 'SUBMIT_FEE'
  | 'BOOST'
  | 'STICKER_PURCHASE'
  | 'JOKE_APPROVED'
  | 'WELCOME'
  | 'ADMIN_ADJUSTMENT';

export interface CoinTransaction {
  id: string;
  type: CoinTransactionType;
  amount: number;
  balanceAfter: number;
  referenceId: string | null;
  description: string | null;
  createdAt: string;
}

export interface Sticker {
  id: string;
  slug: string;
  name: string;
  description: string | null;
  price: number;
  imageUrl: string | null;
  active: boolean;
  soldOut: boolean;
  stockTotal: number | null;
  stockSold: number;
  availableFrom: string | null;
  availableUntil: string | null;
  sortOrder: number;
}

export interface OwnedSticker {
  stickerId: string;
  slug: string;
  name: string;
  imageUrl: string | null;
  quantity: number;
  purchasedTotal: number;
}

export interface PurchaseResponse {
  sticker: OwnedSticker;
  balance: number;
}

export interface JokeStickerAward {
  id: string;
  stickerId: string;
  slug: string | null;
  name: string | null;
  imageUrl: string | null;
  giverId: string;
  giverNickname: string;
  message: string | null;
  createdAt: string;
}

export interface PublicProfile {
  userId: string;
  nickname: string;
  bio: string | null;
  karma: number;
  approvedJokes: number;
  stickers: OwnedSticker[];
}

export interface JokeReport {
  id: string;
  jokeId: string;
  jokeText: string | null;
  reporterId: string;
  reporterNickname: string;
  reason: string;
  createdAt: string;
  resolvedAt: string | null;
}

/** RFC 7807 mit Midgards Extension-Properties. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  fieldErrors?: { field: string; code: string; message: string }[];
  required?: number;
  balance?: number;
}
