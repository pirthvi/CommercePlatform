import { CommonModule } from '@angular/common';
import { Component, DestroyRef, HostListener, OnDestroy, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, catchError, merge, of, switchMap } from 'rxjs';
import { PageBlock, PageLayout } from '@commerce/layout';
import { StoreInterface } from '@commerce/store';
import { ApiService } from '../api.service';
import { legacyPageLayout } from '../legacy-page-layout';
import { Product, StorefrontContent } from '../models';
import { PageMetaDirective } from '../page-meta.directive';
import { HeroSectionComponent } from '../home/hero/hero.component';
import { BannerSectionComponent } from '../home/banner/banner.component';
import { StorySectionComponent } from '../home/story/story.component';
import { EditorialSectionComponent } from '../home/editorial/editorial.component';
import { BenefitsSectionComponent } from '../home/benefits/benefits.component';
import { ValuesSectionComponent } from '../home/values/values.component';
import { FeaturedSectionComponent } from '../home/featured/featured.component';
import { CollectionsSectionComponent } from '../home/collections/collections.component';
import { TextBlockComponent } from '../blocks/text/text.component';
import { ButtonBlockComponent } from '../blocks/button/button.component';
import { GalleryBlockComponent } from '../blocks/gallery/gallery.component';
import { FaqBlockComponent } from '../blocks/faq/faq.component';
import { ProductGridBlockComponent } from '../blocks/product-grid/product-grid.component';
import { ShopComponent } from './shop.component';
import { CartComponent } from './cart.component';
import { CheckoutComponent } from './checkout.component';
import { CustomerAuthComponent } from './customer-auth.component';
import { CustomerAccountComponent } from './customer-account.component';
import { OrderSuccessComponent } from './order-success.component';
import { ProductPageState } from './product-page-state';
import { ProductGalleryComponent } from './product-gallery.component';
import { ProductDetailComponent } from './product-detail.component';
import { RelatedProductsComponent } from './related-products.component';
import { ProductIconComponent } from './product-icon/product-icon.component';
import { BlockColorDirective } from '../block-color.directive';
import { AnnouncementBlockComponent } from '../blocks/announcement/announcement.component';
import { HeaderComponent } from '../layout/header/header.component';
import { FooterComponent } from '../layout/footer/footer.component';

@Component({
  standalone: true, providers: [ProductPageState],
  imports: [CommonModule, RouterLink, PageMetaDirective, HeroSectionComponent, BannerSectionComponent, StorySectionComponent,
    EditorialSectionComponent, BenefitsSectionComponent, ValuesSectionComponent, FeaturedSectionComponent, CollectionsSectionComponent,
    BlockColorDirective, HeaderComponent, FooterComponent, AnnouncementBlockComponent, TextBlockComponent, ButtonBlockComponent, GalleryBlockComponent, FaqBlockComponent, ProductGridBlockComponent,
    ShopComponent, CartComponent, CheckoutComponent, CustomerAuthComponent, CustomerAccountComponent, OrderSuccessComponent,
    ProductGalleryComponent, ProductDetailComponent, RelatedProductsComponent, ProductIconComponent],
  templateUrl: './content-page.component.html', styleUrl: './content-page.component.css',
})
export class ContentPageComponent implements OnInit, OnDestroy {
  readonly ui = inject(StoreInterface);
  readonly pdp = inject(ProductPageState);
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy = inject(DestroyRef);
  private readonly reload = new Subject<void>();
  readonly preview = !!this.route.snapshot.data['preview'];
  private readonly studioOrigin = new URL(location.origin);
  private previewUrls: string[] = [];
  page?: PageLayout;
  rows: PageBlock[][] = [];
  error = false;
  site?: StorefrontContent;
  siteError = false;
  products: Product[] = [];
  catalogLoading = false;
  catalogError = '';
  private catalogRequested = false;

  ngOnInit(): void {
    this.api.siteContent().pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: site => this.site = site,
      error: () => this.siteError = true,
    });
    if (this.preview) {
      if (this.studioOrigin.port === '4200') this.studioOrigin.port = '4201';
      if (window.parent !== window) window.parent.postMessage({type: 'page-preview-ready'}, this.studioOrigin.origin);
      return;
    }
    merge(this.route.paramMap, this.reload).pipe(switchMap(() => {
      const template: string | null = this.route.snapshot.data['template'] ?? null;
      const params = this.route.snapshot.paramMap;
      const key = template ?? params.get('slug') ?? '';
      this.page = undefined;
      this.error = false;
      if (template === 'product') this.pdp.open(params.get('slug'), params.get('id'));
      const response = this.ui.config()?.pageLayoutVersion
        ? this.api.layout(template ? 'template' : 'page', key) : legacyPageLayout(this.api, template, key);
      return response.pipe(catchError(() => of(null)));
    }), takeUntilDestroyed(this.destroy)).subscribe(layout => {
      if (layout) this.display(layout);
      else this.error = true;
    });
  }

  @HostListener('window:message', ['$event'])
  receivePreview(event: MessageEvent): void {
    if (!this.preview || window.parent === window || event.source !== window.parent || event.origin !== this.studioOrigin.origin ||
        event.data?.type !== 'page-preview' || !Array.isArray(event.data.layout?.sections)) return;
    const layout: PageLayout = event.data.layout;
    this.releaseImages();
    const images: Record<string, File> = event.data.images || {};
    for (const section of layout.sections) {
      for (const entry of [section, ...section.items]) {
        const file = images[entry.id];
        if (file instanceof Blob) {
          entry.imageUrl = URL.createObjectURL(file);
          this.previewUrls.push(entry.imageUrl);
        }
      }
    }
    if (layout.templateKey === 'product' && this.page?.templateKey !== 'product') {
      this.pdp.open(null, null, layout.sections.find(block => block.type === 'related-products'));
    }
    this.display(layout);
  }

  private display(layout: PageLayout): void {
    this.page = layout;
    const sections = layout.sections.filter(s => s.visible);
    const rows: PageBlock[][] = [];
    for (let i = 0; i < sections.length; i++) {
      const first = sections[i], second = sections[i + 1];
      const pair = (first.type === 'product-gallery' && second?.type === 'product-purchase') ||
                   (first.type === 'product-purchase' && second?.type === 'product-gallery');
      rows.push(pair ? [first, sections[++i]] : [first]);
    }
    this.rows = rows;
    if (!this.catalogRequested && sections.some(s => ['collections', 'featured', 'product-grid'].includes(s.type))) this.loadProducts();
  }

  hasBlock(layout: PageLayout, type: string): boolean { return layout.sections.some(section => section.type === type); }
  isPrimaryText(layout: PageLayout, block: PageBlock): boolean {
    return layout.sections.find(section => !['header', 'footer', 'announcement'].includes(section.type)) === block;
  }

  retry(): void { this.reload.next(); }
  loadProducts(): void {
    this.catalogRequested = true; this.catalogLoading = true; this.catalogError = '';
    this.api.products().pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: rows => { this.products = rows; this.catalogLoading = false; },
      error: () => { this.catalogError = this.ui.text('home.ourShopIsTakingAMomentToLoad'); this.catalogLoading = false; },
    });
  }
  ngOnDestroy(): void { this.releaseImages(); }
  private releaseImages(): void { this.previewUrls.forEach(url => URL.revokeObjectURL(url)); this.previewUrls = []; }
}
