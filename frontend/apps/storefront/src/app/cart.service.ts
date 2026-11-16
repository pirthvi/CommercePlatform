import { StoreInterface } from '@commerce/store';
import { commerceStorageKey } from '@commerce/storage';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, EMPTY, Observable, Subject, catchError, concatMap, map, tap } from 'rxjs';
import { CartLine, CartResponse, Product } from './models';

type CartCommand = () => Observable<CartResponse>;

@Injectable({ providedIn: 'root' })
export class CartService {
  readonly ui = inject(StoreInterface);
  private readonly http = inject(HttpClient);
  private readonly guestKey = commerceStorageKey(localStorage, 'guest-cart-id');
  private readonly state = new BehaviorSubject<CartLine[]>([]);
  private readonly commands = new Subject<CartCommand>();
  private initialized = false;
  private loaded = false;
  private revision = 0;
  readonly lines$ = this.state.asObservable();
  readonly errors$ = new BehaviorSubject<string | null>(null);

  constructor() {
    this.commands.pipe(concatMap((command) => command().pipe(
      catchError((error: { error?: { detail?: string; message?: string } }) => {
        this.errors$.next(error.error?.detail || error.error?.message || this.ui.text('cart.yourCartCouldNotBeUpdatedPleaseTry'));
        this.refresh();
        return EMPTY;
      }),
    ))).subscribe();
  }

  get lines(): CartLine[] { return this.state.value; }
  get count(): number { return this.lines.reduce((sum, line) => sum + line.quantity, 0); }
  get subtotal(): number { return this.lines.reduce((sum, line) => sum + line.product.price * line.quantity, 0); }
  get isLoaded(): boolean { return this.loaded; }
  get guestCartId(): string {
    let id = localStorage.getItem(this.guestKey);
    if (!id) {
      id = typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
          const random = Math.random() * 16 | 0;
          return (char === 'x' ? random : (random & 0x3 | 0x8)).toString(16);
        });
      localStorage.setItem(this.guestKey, id);
    }
    return id;
  }

  preview(product?: Product): void {
    this.state.next(product ? [{product, quantity: 1}] : []);
    this.loaded = true;
  }

  initialize(): void {
    if (this.initialized) return;
    this.initialized = true;
    this.refresh();
  }

  refresh(): void {
    const requestedRevision = this.revision;
    this.http.get<CartResponse>('/api/cart', { headers: this.headers() }).subscribe({
      next: (response) => { if (requestedRevision === this.revision) this.state.next(response.items ?? []); this.loaded = true; },
      error: () => { this.loaded = true; if (requestedRevision === this.revision) this.errors$.next(this.ui.text('cart.yourCartCouldNotBeLoadedPleaseRefresh')); },
    });
  }

  add(product: Product, quantity = 1): void {
    if (product.stock < 1 || quantity < 1) return;
    const existing = this.lines.find((line) => line.product.id === product.id);
    const nextQuantity = Math.min(product.stock, (existing?.quantity ?? 0) + quantity);
    this.setOptimistic(existing
      ? this.lines.map((line) => line.product.id === product.id ? { ...line, product, quantity: nextQuantity } : line)
      : [...this.lines, { product, quantity: nextQuantity }]);
    this.enqueue(() => this.http.post<CartResponse>('/api/cart/items', { productId: product.id, quantity }, { headers: this.headers() }));
  }

  quantity(id: number, quantity: number): void {
    if (quantity < 1) { this.remove(id); return; }
    this.setOptimistic(this.lines.map((line) => line.product.id === id
      ? { ...line, quantity: Math.min(line.product.stock, quantity) } : line));
    this.enqueue(() => this.http.put<CartResponse>(`/api/cart/items/${id}`, { quantity }, { headers: this.headers() }));
  }

  remove(id: number): void {
    this.setOptimistic(this.lines.filter((line) => line.product.id !== id));
    this.enqueue(() => this.http.delete<CartResponse>(`/api/cart/items/${id}`, { headers: this.headers() }));
  }

  clear(): void {
    this.setOptimistic([]);
    this.enqueue(() => this.http.delete<CartResponse>('/api/cart', { headers: this.headers() }));
  }

  mergeGuestCart(): Observable<void> {
    const requestRevision = ++this.revision;
    return this.http.post<CartResponse>('/api/cart/merge', {}, { headers: this.headers() }).pipe(
      tap((response) => { if (requestRevision === this.revision) this.state.next(response.items ?? []); this.loaded = true; this.errors$.next(null); }),
      map(() => undefined),
    );
  }

  private setOptimistic(lines: CartLine[]): void { this.state.next(lines); this.errors$.next(null); }
  private enqueue(command: CartCommand): void {
    const commandRevision = ++this.revision;
    this.commands.next(() => command().pipe(tap((response) => {
      if (commandRevision === this.revision) { this.state.next(response.items ?? []); this.loaded = true; this.errors$.next(null); }
    })));
  }
  private headers(): HttpHeaders { return new HttpHeaders({ 'X-Guest-Cart-Id': this.guestCartId }); }
}
