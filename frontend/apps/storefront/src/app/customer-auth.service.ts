import { Injectable, inject } from '@angular/core';
import { commerceStorageKey } from '@commerce/storage';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { OrderReceipt } from './models';

export interface CustomerProfile { id: number; fullName: string; email: string; createdAt: string; }
export interface CustomerAuthResponse { accessToken: string; tokenType: string; expiresAt: string; customer: CustomerProfile; }

@Injectable({ providedIn: 'root' })
export class CustomerAuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenKey = commerceStorageKey(sessionStorage, 'customer-jwt');
  private readonly customerState = new BehaviorSubject<CustomerProfile | null>(null);
  readonly customer$ = this.customerState.asObservable();
  private readonly base = '/api';

  get customer(): CustomerProfile | null { return this.customerState.value; }
  get accessToken(): string | null { return sessionStorage.getItem(this.tokenKey); }
  get isAuthenticated(): boolean { return !!this.accessToken; }

  register(body: { fullName: string; email: string; password: string }): Observable<CustomerAuthResponse> {
    return this.http.post<CustomerAuthResponse>(`${this.base}/auth/register`, body).pipe(tap((response) => this.save(response)));
  }

  login(body: { email: string; password: string }): Observable<CustomerAuthResponse> {
    return this.http.post<CustomerAuthResponse>(`${this.base}/auth/login`, body).pipe(tap((response) => this.save(response)));
  }

  me(): Observable<CustomerProfile> { return this.http.get<CustomerProfile>(`${this.base}/auth/me`); }
  orders(): Observable<OrderReceipt[]> { return this.http.get<OrderReceipt[]>(`${this.base}/account/orders`); }

  resume(): void {
    if (!this.accessToken) return;
    this.me().subscribe({ next: (customer) => this.customerState.next(customer), error: () => this.signOut() });
  }

  signOut(): void {
    sessionStorage.removeItem(this.tokenKey);
    this.customerState.next(null);
  }

  private save(response: CustomerAuthResponse): void {
    sessionStorage.setItem(this.tokenKey, response.accessToken);
    this.customerState.next(response.customer);
  }
}
