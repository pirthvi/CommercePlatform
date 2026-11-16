import { Injectable, inject } from '@angular/core';
import { commerceStorageKey } from '@commerce/storage';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, map, of, tap } from 'rxjs';

interface AdminSessionResponse {
  authenticated: boolean;
  accessToken?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';
  private readonly key = commerceStorageKey(sessionStorage, 'admin-auth');
  private readonly state = new BehaviorSubject(false);
  readonly loggedIn$ = this.state.asObservable();
  lastSignInStatus: number | null = null;
  get loggedIn(): boolean { return this.state.value; }
  get authorization(): string | null { const token = sessionStorage.getItem(this.key); return token ? `Bearer ${token}` : null; }
  resume(): void {
    if (!this.authorization) { this.state.next(false); return; }
    this.verify().subscribe((valid) => this.state.next(valid));
  }
  signIn(username: string, password: string): Observable<boolean> {
    this.lastSignInStatus = null;
    this.signOut();
    return this.http.post<AdminSessionResponse>(`${this.base}/admin/session`, { username, password }).pipe(
      map((result) => {
        const valid = result.authenticated && !!result.accessToken;
        if (valid) {
          sessionStorage.setItem(this.key, result.accessToken!);
          this.state.next(true);
        }
        return valid;
      }),
      catchError((error: unknown) => {
        this.lastSignInStatus = error instanceof HttpErrorResponse ? error.status : -1;
        return of(false);
      }),
    );
  }
  verify(): Observable<boolean> {
    const authorization = this.authorization;
    if (!authorization) return of(false);
    return this.http.get<{ authenticated: boolean }>(`${this.base}/admin/session`, { headers: new HttpHeaders({ Authorization: authorization }) }).pipe(
      map((result) => result.authenticated),
      tap((valid) => {
        this.state.next(valid);
        if (!valid) this.signOut();
      }),
      catchError(() => { this.signOut(); return of(false); }),
    );
  }
  signOut(): void { sessionStorage.removeItem(this.key); this.state.next(false); }
}
