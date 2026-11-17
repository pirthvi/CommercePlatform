import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProductPageContent, StoreInterface } from '@commerce/store';
import { AdminApiService } from '../../admin-api.service';
import { ContentImagePickerComponent } from '../../content-image-picker/content-image-picker.component';
import { storefrontUrl } from '../../studio-navigation';

const fields: Record<string, string[]> = {
  navigation: ['home', 'shopAll', 'product'],
  purchase: ['quantity', 'inStock', 'outOfStock', 'addToCart', 'addedToYourCart', 'buyItNow', 'stickyAddToCart', 'added'],
  delivery: ['cashOnDelivery', 'payAtYourDoor', 'freeDelivery', 'deliveryThreshold'],
  details: ['description', 'origin', 'productDetails', 'materialsOrIngredients', 'usage', 'goodToKnow'],
  related: ['moreFromTheStore', 'relatedProducts'],
  states: ['loadingProduct', 'thisProductIsUnavailableBrowseTheCatalogFor', 'browseAllProducts'],
  accessibility: ['productImages', 'chooseProductImage', 'showProductImage', 'view', 'decreaseQuantity', 'increaseQuantity', 'deliveryAndProductPromises'],
};

function changes(current: Record<string, string>, original: Record<string, string>) {
  return Object.fromEntries(Object.entries(current).filter(([key, value]) => value !== original[key]));
}

@Component({
  standalone: true,
  imports: [FormsModule, ContentImagePickerComponent],
  templateUrl: './product-page.component.html',
  styleUrl: './product-page.component.css',
})
export class ProductPageComponent implements OnInit, OnDestroy {
  readonly ui = inject(StoreInterface);
  private readonly api = inject(AdminApiService);
  original?: ProductPageContent;
  texts: Record<string, string> = {};
  symbols: Record<string, string> = {};
  uploads: Record<string, File | null> = {};
  previews: Record<string, string> = {};
  groups: Record<string, string[]> = {};
  tabs: string[] = [];
  iconSlots: string[] = [];
  selected = 'purchase';
  error = '';
  message = '';
  previewUrl = '';
  saving = false;

  ngOnInit(): void {
    this.load();
    this.api.products().subscribe({ next: products => {
      const first = products.find(product => product.active);
      if (first) this.previewUrl = storefrontUrl('/products/' + encodeURIComponent(first.slug));
    }, error: () => {} });
  }

  load(): void {
    this.error = '';
    this.ui.productPage().subscribe({
      next: page => this.use(page),
      error: () => this.error = this.ui.text('pdpEditor.loadError'),
    });
  }

  get dirty(): boolean {
    return !!this.original && (Object.keys(this.uploads).length > 0
      || Object.keys(changes(this.texts, this.original.texts)).length > 0
      || Object.keys(changes(this.symbols, this.original.symbols)).length > 0);
  }

  preview(slot: string): string {
    if (slot in this.uploads) return this.previews[slot] || '';
    return this.original?.images[slot] || '';
  }

  pick(slot: string, file: File | null): void {
    if (this.previews[slot]) URL.revokeObjectURL(this.previews[slot]);
    delete this.previews[slot];
    if (file) this.previews[slot] = URL.createObjectURL(file);
    if (!file && !this.original?.images[slot]) delete this.uploads[slot];
    else this.uploads[slot] = file;
    this.message = '';
  }

  discard(): void {
    if (this.original) this.use(this.original);
    this.message = '';
    this.error = '';
  }

  save(): void {
    if (!this.original || this.saving || !this.dirty) return;
    this.saving = true;
    this.error = '';
    this.message = '';
    this.api.saveProductPage(changes(this.texts, this.original.texts), changes(this.symbols, this.original.symbols), this.uploads)
      .subscribe({
        next: page => {
          this.use(page);
          this.saving = false;
          this.message = this.ui.text('pdpEditor.saved');
        },
        error: err => {
          this.saving = false;
          this.error = err.error?.detail || this.ui.text('pdpEditor.failed');
        },
      });
  }

  private use(page: ProductPageContent): void {
    this.releasePreviews();
    this.original = page;
    this.texts = { ...page.texts };
    this.symbols = { ...page.symbols };
    this.iconSlots = Object.keys(page.symbols);
    this.groups = Object.fromEntries(Object.entries(fields).map(([group, keys]) => [group, keys.filter(key => key in page.texts)]));
    const known = new Set(Object.values(fields).flat());
    const extra = Object.keys(page.texts).filter(key => !known.has(key));
    if (extra.length) this.groups['other'] = extra;
    this.tabs = [...Object.keys(this.groups), 'icons'];
    this.uploads = {};
  }

  ngOnDestroy(): void { this.releasePreviews(); }
  private releasePreviews(): void {
    Object.values(this.previews).forEach(url => URL.revokeObjectURL(url));
    this.previews = {};
  }
}
