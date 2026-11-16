import { DestroyRef, Injectable, inject } from '@angular/core';
import { includesBlockCategory, PageBlock } from '@commerce/layout';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { Subscription, catchError, forkJoin, map, of, shareReplay } from 'rxjs';
import { StoreInterface } from '@commerce/store';
import { ApiService } from '../api.service';
import { CartService } from '../cart.service';
import { Product } from '../models';

@Injectable()
export class ProductPageState {
  readonly ui = inject(StoreInterface);
  private readonly api = inject(ApiService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);
  private readonly destroy = inject(DestroyRef);
  private readonly catalog$ = this.api.products().pipe(catchError(() => of([] as Product[])), shareReplay({bufferSize: 1, refCount: true}));
  private request?: Subscription;
  private timer?: ReturnType<typeof setTimeout>;
  private catalog: Product[] = [];
  product?: Product;
  error = '';
  added = false;
  quantity = 1;
  activeImage = '';

  constructor() { this.destroy.onDestroy(() => clearTimeout(this.timer)); }

  relatedProducts(block: PageBlock): Product[] {
    return this.catalog.filter(item => item.id !== this.product?.id && includesBlockCategory(block, item.categorySlug)).slice(0, 4);
  }
  get images(): string[] {
    if (!this.product) return [];
    return [...new Set([this.product.imageUrl, ...(this.product.galleryImages ?? '').split(/\r?\n/)].map(url => url.trim()).filter(Boolean))];
  }
  get highlights(): string[] { return (this.product?.highlights ?? '').split(/\r?\n/).map(s => s.trim()).filter(Boolean); }

  open(slug: string | null, id: string | null, previewCategories?: PageBlock): void {
    this.request?.unsubscribe();
    clearTimeout(this.timer);
    this.product = undefined; this.error = ''; this.added = false; this.quantity = 1;
    const product = slug ? this.api.productBySlug(slug) : id ? this.api.product(Number(id)) :
      this.catalog$.pipe(map(rows => rows.find(item => includesBlockCategory(previewCategories, item.categorySlug))));
    this.request = forkJoin({
      product: product.pipe(catchError(() => of(null))),
      wording: this.ui.productPage().pipe(catchError(() => of(null))),
      catalog: this.catalog$,
    }).pipe(takeUntilDestroyed(this.destroy)).subscribe(result => {
      if (result.wording) this.ui.applyProductPage(result.wording);
      this.catalog = result.catalog;
      if (!result.product) { this.error = this.ui.text('product-detail.thisProductIsUnavailableBrowseTheCatalogFor'); return; }
      this.product = result.product;
      this.activeImage = this.images[0] ?? '';
    });
  }

  add(): void {
    if (!this.product || this.product.stock < 1) return;
    this.cart.add(this.product, this.quantity);
    this.added = true;
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.added = false, 1600);
  }
  buyNow(): void {
    if (!this.product || this.product.stock < 1) return;
    this.add();
    void this.router.navigate(['/checkout']);
  }
}
