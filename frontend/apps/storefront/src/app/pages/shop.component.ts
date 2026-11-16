import { includesBlockCategory, PageBlock } from '@commerce/layout';
import { Category, StoreInterface } from '@commerce/store';
import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { Product } from '../models';
import { ProductCardComponent } from '../product-card.component';

@Component({
  selector: "shop-catalog-block",
  standalone: true, imports: [CommonModule, FormsModule, ProductCardComponent, RouterLink],
  templateUrl: './shop.component.html',
  styleUrl: './shop.component.css',
})
export class ShopComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  products: Product[] = [];
  @Input({required: true}) section!: PageBlock;
  categories: Category[] = [];
  private requestedCategory = '';
  private readonly router = inject(Router);
  query = '';
  loading = true;
  error = '';
  get offeredCategories(): Category[] {
    return this.categories.filter(category => includesBlockCategory(this.section, category.slug));
  }
  get offeredProducts(): Product[] {
    return this.products.filter(product => includesBlockCategory(this.section, product.categorySlug));
  }
  get activeCategory(): Category | undefined {
    const term = this.requestedCategory;
    return this.offeredCategories.find(c => c.slug === term || c.name.toLowerCase() === term ||
      c.slug === term.replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, ''));
  }
  get visibleProducts(): Product[] {
    const term = this.query.trim().toLowerCase();
    const active = this.activeCategory;
    return this.offeredProducts.filter((product) => (!active || product.categoryId === active.id)
      && (!term || `${product.name} ${product.description} ${product.category}`.toLowerCase().includes(term)));
  }
  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => { this.requestedCategory = (params.get('category') || '').toLowerCase(); });
    this.loadProducts();
  }
  loadProducts(): void {
    this.loading = true;
    this.error = '';
    forkJoin({products: this.api.products(), categories: this.api.categories()}).subscribe({ next: ({products, categories}) => {
      this.products = products;
      this.categories = categories;
      this.loading = false;
    }, error: () => { this.loading = false; this.error = this.ui.text('shop.ourCollectionIsTakingALittleLongerTo'); } });
  }
  filterBy(category?: Category): void {
    void this.router.navigate([], {relativeTo:this.route, queryParams:{category:category?.slug ?? null}, queryParamsHandling:'merge'});
  }
}
