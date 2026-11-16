import { Category, StoreInterface, StoreMoneyPipe } from '@commerce/store';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { AdminApiService, ProductWrite } from './admin-api.service';
import { Product } from './models';
import { RouterLink } from '@angular/router';

const MAX_IMAGE_BYTES = 8 * 1024 * 1024;
const MAX_GALLERY_IMAGES = 8;
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

type ProductForm = Omit<Product, 'id' | 'imageUrl' | 'galleryImages'>;
type ProductVisibility = 'ALL' | 'LIVE' | 'HIDDEN';

const blankProduct = (): ProductForm => ({
  name: '', slug: '', categoryId: null, category: '', description: '', price: 0, stock: 0,
  weight: '', origin: '', tastingNotes: '', pairings: '', ingredients: '', highlights: '',
  badge: '', featured: false, active: true,
});

@Component({
  selector: 'admin-products', standalone: true,
  imports: [StoreMoneyPipe, CommonModule, FormsModule, RouterLink],
  templateUrl: './products.component.html',
  styleUrl: './products.component.css',
})
export class ProductsComponent implements OnInit, OnDestroy {
  readonly ui = inject(StoreInterface);
  private readonly api = inject(AdminApiService);
  products: Product[] = [];
  categories: Category[] = [];
  private previousStock?: number;
  form = blankProduct();
  loading = true;
  editorOpen = false;
  saving = false;
  editingId: number | null = null;
  error = '';
  success = '';
  searchText = '';
  visibility: ProductVisibility = 'ALL';
  currentMainImage = '';
  mainImageFile: File | null = null;
  mainPreviewUrl = '';
  existingGalleryImages: string[] = [];
  galleryImageFiles: File[] = [];
  galleryPreviewUrls: string[] = [];
  clearGalleryFlag = false;

  get filteredProducts(): Product[] {
    const query = this.searchText.trim().toLowerCase();
    return this.products.filter((product) => {
      const visibilityMatches = this.visibility === 'ALL' || (this.visibility === 'LIVE' ? product.active : !product.active);
      const queryMatches = !query || `${product.name} ${product.category} ${product.slug}`.toLowerCase().includes(query);
      return visibilityMatches && queryMatches;
    });
  }

  get mainImageRequired(): boolean {
    return !this.editingId;
  }

  get mainImageExternal(): boolean {
    return !!this.editingId && !!this.currentMainImage && !this.isStoredImage(this.currentMainImage);
  }

  get canSaveProduct(): boolean {
    return !this.mainImageRequired || !!this.mainImageFile;
  }

  get externalGalleryImageCount(): number {
    return this.existingGalleryImages.filter((url) => !this.isStoredImage(url)).length;
  }

  ngOnInit(): void {
    this.load();
    this.api.categories().subscribe({next: rows => this.categories = rows, error: () => this.error = this.ui.text('products.couldNotLoadCategories')});
  }

  ngOnDestroy(): void { this.releasePreviewUrls(); }

  load(): void {
    this.loading = true;
    this.api.products().subscribe({
      next: (items) => { this.products = items; this.loading = false; },
      error: (err) => { this.loading = false; this.error = this.message(err, this.ui.text('products.couldNotLoadProducts')); },
    });
  }

  newProduct(): void {
    this.resetEditorImages();
    this.form = blankProduct();
    this.previousStock = undefined;
    if (this.categories.length === 1) this.form.categoryId = this.categories[0].id;
    this.editingId = null;
    this.editorOpen = true;
    this.error = '';
    this.success = '';
  }

  editProduct(product: Product): void {
    this.resetEditorImages();
    const { id, imageUrl, galleryImages, ...rest } = product;
    this.form = {
      ...rest,
      weight: rest.weight || '', origin: rest.origin || '', tastingNotes: rest.tastingNotes || '',
      pairings: rest.pairings || '', ingredients: rest.ingredients || '', highlights: rest.highlights || '', badge: rest.badge || '',
    };
    this.editingId = id;
    this.previousStock = product.stock;
    this.currentMainImage = imageUrl;
    this.mainPreviewUrl = imageUrl;
    this.existingGalleryImages = (galleryImages || '').split(/\r?\n/).map((url) => url.trim()).filter(Boolean);
    this.editorOpen = true;
    this.error = '';
    this.success = '';
  }

  syncSlug(): void {
    if (this.editingId) return;
    this.form.slug = this.form.name.toLowerCase().trim().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
  }

  isStoredImage(url: string): boolean { return url.startsWith('/api/products/'); }

  allowDrop(event: DragEvent): void { event.preventDefault(); }

  dropMainImage(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files?.[0];
    if (file) this.setMainImage(file);
  }

  selectMainImage(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) this.setMainImage(file);
  }

  selectGalleryImages(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []);
    input.value = '';
    this.error = '';
    if (files.length > MAX_GALLERY_IMAGES) {
      this.error = this.ui.text('products.chooseUpTo8GalleryPhotosAtA');
      return;
    }
    const invalid = files.find((file) => !this.validImage(file));
    if (invalid) {
      this.error = this.ui.text('products.galleryPhotosMustBeJpegPngOrWebp');
      return;
    }
    this.releaseGalleryPreviewUrls();
    this.galleryImageFiles = files;
    this.galleryPreviewUrls = files.map((file) => URL.createObjectURL(file));
    this.clearGalleryFlag = false;
  }

  removeGalleryImage(index: number): void {
    URL.revokeObjectURL(this.galleryPreviewUrls[index]);
    this.galleryImageFiles.splice(index, 1);
    this.galleryPreviewUrls.splice(index, 1);
  }

  clearGallery(): void {
    this.releaseGalleryPreviewUrls();
    this.galleryImageFiles = [];
    this.existingGalleryImages = [];
    this.clearGalleryFlag = true;
  }

  discardMainImage(): void {
    this.releaseMainPreviewUrl();
    this.mainImageFile = null;
    this.mainPreviewUrl = this.currentMainImage;
  }

  closeEditor(): void {
    this.editorOpen = false;
    this.releasePreviewUrls();
  }

  save(form: NgForm): void {
    if (!form.valid || this.saving || !this.canSaveProduct) return;
    this.saving = true;
    this.error = '';
    const body: ProductWrite = { ...this.form, previousStock: this.previousStock, replaceGallery: this.clearGalleryFlag || this.galleryImageFiles.length > 0 };
    const request = this.editingId
      ? this.api.updateProduct(this.editingId, body, this.mainImageFile, this.galleryImageFiles)
      : this.api.createProduct(body, this.mainImageFile!, this.galleryImageFiles);

    request.subscribe({
      next: () => {
        this.saving = false;
        this.editorOpen = false;
        this.releasePreviewUrls();
        this.success = this.editingId ? this.ui.text('products.yourProductDetailsAreSavedUploadedPhotosAre') : this.ui.text('products.productCreated');
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = this.message(err, this.ui.text('products.weCouldNotSaveThisProductCheckThat'));
      },
    });
  }

  deleteProduct(product: Product): void {
    if (!window.confirm(this.ui.text('products.confirmRemove', {name: product.name}))) return;
    this.api.deleteProduct(product.id).subscribe({
      next: () => { this.success = this.ui.text('products.removed', {name: product.name}); this.load(); },
      error: (err) => this.error = this.message(err, this.ui.text('products.couldNotRemoveThisProduct')),
    });
  }

  private setMainImage(file: File): void {
    this.error = '';
    if (!this.validImage(file)) {
      this.error = this.ui.text('products.chooseAJpegPngOrWebpImageUnder');
      return;
    }
    this.releaseMainPreviewUrl();
    this.mainImageFile = file;
    this.mainPreviewUrl = URL.createObjectURL(file);
  }

  private validImage(file: File): boolean { return IMAGE_TYPES.includes(file.type) && file.size > 0 && file.size <= MAX_IMAGE_BYTES; }

  private resetEditorImages(): void {
    this.releasePreviewUrls();
    this.currentMainImage = '';
    this.mainImageFile = null;
    this.mainPreviewUrl = '';
    this.existingGalleryImages = [];
    this.galleryImageFiles = [];
    this.galleryPreviewUrls = [];
    this.clearGalleryFlag = false;
  }

  private releasePreviewUrls(): void {
    this.releaseMainPreviewUrl();
    this.releaseGalleryPreviewUrls();
  }

  private releaseMainPreviewUrl(): void {
    if (this.mainImageFile && this.mainPreviewUrl.startsWith('blob:')) URL.revokeObjectURL(this.mainPreviewUrl);
  }

  private releaseGalleryPreviewUrls(): void { this.galleryPreviewUrls.forEach((url) => URL.revokeObjectURL(url)); }

  private message(error: { error?: { detail?: string; message?: string } }, fallback: string): string {
    return error.error?.detail || error.error?.message || fallback;
  }
}
