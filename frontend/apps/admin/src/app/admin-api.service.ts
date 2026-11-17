import { Category, CommerceOptions, InterfaceConfig, ProductPageContent, Surface } from '@commerce/store';
import { PageLayout } from '@commerce/layout';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ContentPage, CustomerOrder, Product } from './models';
import { StorefrontContent } from './storefront-content.models';
import { ContentArea } from './studio-navigation';

export type ProductWrite = Omit<Product, 'id' | 'imageUrl' | 'galleryImages' | 'categorySlug'> & { replaceGallery: boolean; previousStock?: number };
export interface StockMovement { id: number; quantityChange: number; balance: number; reason: string; createdAt: string; }
export type PageWrite = Omit<ContentPage, 'id' | 'updatedAt' | 'heroImageUrl'> & { clearHeroImage: boolean };

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/admin';
  products(): Observable<Product[]> { return this.http.get<Product[]>(`${this.base}/products`); }
  categories() { return this.http.get<Category[]>(this.base + '/categories'); }
  saveCategory(id: number | null, body: Pick<Category, 'name' | 'description'>) {
    const url = this.base + '/categories';
    return id === null ? this.http.post<Category>(url, body) : this.http.put<Category>(url + '/' + id, body);
  }
  deleteCategory(id: number) { return this.http.delete<void>(this.base + '/categories/' + id); }
  adjustStock(id: number, change: number, expectedStock: number, reason: string) {
    return this.http.post<Product>(this.base + '/inventory/' + id + '/adjustments', { change, expectedStock, reason });
  }
  stockHistory(id: number) { return this.http.get<StockMovement[]>(this.base + '/inventory/' + id + '/movements'); }
  interfaceConfig(surface: Surface) { return this.http.get<InterfaceConfig>('/api/content/interface/' + surface); }
  saveTexts(surface: Surface, values: Record<string, string>) { return this.http.patch<void>(this.base + '/interface/' + surface, { values }); }
  saveCommerce(options: CommerceOptions, storeName?: string) { return this.http.put<CommerceOptions>(this.base + '/commerce', { ...options, ...(storeName === undefined ? {} : { storeName }) }); }
  saveProductPage(texts: Record<string, string>, symbols: Record<string, string>, uploads: Record<string, File | null>) {
    const form = new FormData();
    const removeImages = Object.keys(uploads).filter(slot => uploads[slot] === null);
    form.append('changes', new Blob([JSON.stringify({ texts, symbols, removeImages })], { type: 'application/json' }));
    for (const [slot, file] of Object.entries(uploads)) {
      if (file) form.append('icon_' + slot, file, file.name);
    }
    return this.http.put<ProductPageContent>(this.base + '/product-page', form);
  }
  createProduct(body: ProductWrite, mainImage: File, galleryImages: File[]): Observable<Product> {
    return this.http.post<Product>(`${this.base}/products`, this.productForm(body, mainImage, galleryImages));
  }
  updateProduct(id: number, body: ProductWrite, mainImage: File | null, galleryImages: File[]): Observable<Product> {
    return this.http.put<Product>(`${this.base}/products/${id}`, this.productForm(body, mainImage, galleryImages));
  }
  deleteProduct(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/products/${id}`); }
  layout(key: string) { return this.http.get<PageLayout>(this.base + '/page-layouts/' + key); }
  saveLayout(page: PageLayout, uploads: Record<string, File>) {
    const form = new FormData();
    const { title, slug, metaDescription, published, revision, sections } = page;
    form.append('layout', new Blob([JSON.stringify({ title, slug, metaDescription, published, revision, sections })], { type: 'application/json' }));
    for (const [key, file] of Object.entries(uploads)) form.append('image_' + key, file, file.name);
    const url = this.base + '/page-layouts';
    return page.id === null ? this.http.post<PageLayout>(url, form) : this.http.put<PageLayout>(url + '/' + page.id, form);
  }
  pages(): Observable<ContentPage[]> { return this.http.get<ContentPage[]>(`${this.base}/pages`); }
  createPage(body: PageWrite, heroImage: File | null): Observable<ContentPage> {
    return this.http.post<ContentPage>(`${this.base}/pages`, this.pageForm(body, heroImage));
  }
  updatePage(id: number, body: PageWrite, heroImage: File | null): Observable<ContentPage> {
    return this.http.put<ContentPage>(`${this.base}/pages/${id}`, this.pageForm(body, heroImage));
  }
  deletePage(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/pages/${id}`); }
  siteContent(): Observable<StorefrontContent> { return this.http.get<StorefrontContent>(`${this.base}/site-content`); }
  saveContentSection(area: ContentArea, body: Partial<StorefrontContent> & { clearLogo?: boolean }, uploads: Record<string, File>): Observable<StorefrontContent> {
    const form = new FormData();
    form.append('content', new Blob([JSON.stringify(body)], { type: 'application/json' }));
    for (const [part, file] of Object.entries(uploads)) form.append(part, file, file.name);
    return this.http.put<StorefrontContent>(`${this.base}/site-content/${area}`, form);
  }
  orders(): Observable<CustomerOrder[]> { return this.http.get<CustomerOrder[]>(`${this.base}/orders`); }
  updateOrderStatus(id: number, status: string): Observable<CustomerOrder> { return this.http.patch<CustomerOrder>(`${this.base}/orders/${id}/status`, { status }); }

  private productForm(body: ProductWrite, mainImage: File | null, galleryImages: File[]): FormData {
    const form = new FormData();
    form.append('product', new Blob([JSON.stringify(body)], { type: 'application/json' }));
    if (mainImage) form.append('mainImage', mainImage, mainImage.name);
    galleryImages.forEach((image) => form.append('galleryImages', image, image.name));
    return form;
  }

  private pageForm(body: PageWrite, heroImage: File | null): FormData {
    const form = new FormData();
    form.append('page', new Blob([JSON.stringify(body)], { type: 'application/json' }));
    if (heroImage) form.append('heroImage', heroImage, heroImage.name);
    return form;
  }
}
