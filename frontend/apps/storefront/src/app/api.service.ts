import { PageLayout } from '@commerce/layout';
import { Category } from '@commerce/store';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { ContentPage, OrderReceipt, Product, StorefrontContent } from './models';
import { CartService } from './cart.service';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly cart = inject(CartService);
  private readonly base = '/api';
  products(): Observable<Product[]> { return this.http.get<Product[]>(`${this.base}/products`); }
  categories(): Observable<Category[]> { return this.http.get<Category[]>(this.base + '/categories'); }
  product(id: number): Observable<Product> { return this.http.get<Product>(`${this.base}/products/${id}`); }
  productBySlug(slug: string): Observable<Product> { return this.http.get<Product>(`${this.base}/products/slug/${encodeURIComponent(slug)}`); }
  layout(kind: 'page' | 'template', key: string) {
    return this.http.get<PageLayout>(this.base + '/content/layouts/' + kind + '/' + encodeURIComponent(key));
  }
  pages(): Observable<ContentPage[]> { return this.http.get<ContentPage[]>(`${this.base}/content`).pipe(catchError(() => of([]))); }
  page(slug: string): Observable<ContentPage> { return this.http.get<ContentPage>(`${this.base}/content/${encodeURIComponent(slug)}`); }
  siteContent(): Observable<StorefrontContent> { return this.http.get<StorefrontContent>(`${this.base}/content/site`); }
  placeOrder(order: unknown): Observable<OrderReceipt> { return this.http.post<OrderReceipt>(`${this.base}/orders`, order, { headers: new HttpHeaders({ 'X-Guest-Cart-Id': this.cart.guestCartId }) }); }
  receipt(orderNumber: string): Observable<OrderReceipt> { return this.http.get<OrderReceipt>(`${this.base}/orders/${encodeURIComponent(orderNumber)}`); }
}
