import { DOCUMENT } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, InjectionToken, Pipe, PipeTransform, inject, provideAppInitializer, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export function isExternalLink(path: string): boolean { return /^(https?:|mailto:|tel:)/i.test(path); }

export type Surface = 'storefront' | 'studio';
export interface Category { id: number; name: string; slug: string; description: string; }
export interface CommerceOptions { currency: string; deliveryFee: number; freeDeliveryFrom: number | null; }
export interface ProductPageContent {
  texts: Record<string, string>;
  symbols: Record<string, string>;
  images: Record<string, string>;
}
export interface InterfaceConfig {
  pageLayoutVersion?: number;
  brandName: string; brandMark: string; brandTagline: string; logoUrl: string | null;
  commerce: CommerceOptions; texts: Record<string, string>;
}
const SURFACE = new InjectionToken<Surface>('commerce application');

@Injectable({ providedIn: 'root' })
export class StoreInterface {
  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);
  readonly surface = inject(SURFACE);
  readonly config = signal<InterfaceConfig | null>(null);
  readonly failed = signal(false);
  readonly recovery = signal({ message: '', retry: '' });
  private readonly recoveryKey = 'commerce.connection.' + this.surface;
  readonly productIcons = signal<Record<string, string>>({});
  private readonly formatters = new Map<string, Intl.NumberFormat>();

  async load(): Promise<void> {
    try {
      const response = await firstValueFrom(this.http.get<InterfaceConfig>('/api/content/interface/' + this.surface));
      this.config.set(response);
      this.failed.set(false);
      this.document.title = response.brandName;
      this.updateIcon(response);
      const labels = { message: response.texts['connection.unavailable'], retry: response.texts['connection.retry'] };
      this.recovery.set(labels);
      try { sessionStorage.setItem(this.recoveryKey, JSON.stringify(labels)); } catch { /* Storage may be disabled. */ }
    } catch (error) {
      let labels = this.recovery();
      try { labels = JSON.parse(sessionStorage.getItem(this.recoveryKey) || 'null') || labels; } catch { /* No cached labels. */ }
      const diagnostic = error instanceof HttpErrorResponse || error instanceof Error ? error.message : String(error);
      this.recovery.set({ message: labels.message || diagnostic, retry: labels.retry || '' });
      this.failed.set(true);
    }
  }
  private updateIcon(config: InterfaceConfig): void {
    const icon = this.document.querySelector<HTMLLinkElement>('link[rel="icon"]');
    if (!icon) return;
    if (config.logoUrl) {
      icon.href = config.logoUrl;
      return;
    }
    const canvas = this.document.createElement('canvas');
    canvas.width = canvas.height = 64;
    const context = canvas.getContext('2d');
    if (!context) return;
    context.fillStyle = '#222322';
    context.fillRect(0, 0, 64, 64);
    context.fillStyle = '#ffffff';
    context.font = 'bold 30px sans-serif';
    context.textAlign = 'center';
    context.textBaseline = 'middle';
    context.fillText(config.brandMark || config.brandName.slice(0, 1), 32, 33, 54);
    icon.href = canvas.toDataURL('image/png');
  }
  text(key: string, values: Record<string, string | number> = {}): string {
    const value = this.config()?.texts[key] ?? '';
    return value.replace(/\{(\w+)\}/g, (placeholder, name: string) =>
      Object.hasOwn(values, name) ? String(values[name]) : placeholder);
  }
  productPage() { return this.http.get<ProductPageContent>('/api/content/product-page'); }
  applyProductPage(page: ProductPageContent): void {
    const values: Record<string, string> = {};
    for (const [key, value] of Object.entries(page.texts)) values['product-detail.' + key] = value;
    for (const [key, value] of Object.entries(page.symbols)) values['product-icons.' + key] = value;
    this.productIcons.set(page.images);
    this.config.update(config => config ? { ...config, texts: { ...config.texts, ...values } } : config);
  }
  money(value: number, currency = this.config()?.commerce.currency): string {
    if (!currency) return '';
    let formatter = this.formatters.get(currency);
    if (!formatter) {
      formatter = new Intl.NumberFormat(this.document.documentElement.lang || 'en', { style: 'currency', currency });
      this.formatters.set(currency, formatter);
    }
    return formatter.format(value);
  }
  delivery(subtotal: number): number {
    const policy = this.config()?.commerce;
    if (!policy || subtotal <= 0) return 0;
    return policy.freeDeliveryFrom !== null && subtotal >= policy.freeDeliveryFrom ? 0 : policy.deliveryFee;
  }
}

@Pipe({ name: 'storeMoney', standalone: true })
export class StoreMoneyPipe implements PipeTransform {
  private readonly store = inject(StoreInterface);
  transform(value: number, currency?: string): string { return this.store.money(value, currency); }
}

export function provideStoreInterface(surface: Surface) {
  return [
    { provide: SURFACE, useValue: surface },
    provideAppInitializer(() => inject(StoreInterface).load()),
  ];
}
