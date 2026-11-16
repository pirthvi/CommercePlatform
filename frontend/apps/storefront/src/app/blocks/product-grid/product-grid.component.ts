import { Component, Input } from '@angular/core';
import { PageBlock } from '@commerce/layout';
import { Product } from '../../models';
import { ProductCardComponent } from '../../product-card.component';
@Component({
  selector: 'shop-product-grid-block', standalone: true, imports: [ProductCardComponent],
  templateUrl: './product-grid.component.html', styleUrl: './product-grid.component.css',
})
export class ProductGridBlockComponent {
  @Input({required: true}) section!: PageBlock;
  @Input({required: true}) products: Product[] = [];
  get selected(): Product[] {
    const available = this.section.categoryId ? this.products.filter(p => p.categoryId === this.section.categoryId) : this.products;
    const selection = this.section.productIds || [];
    if (!selection.length) return available;
    const byId = new Map(available.map(p => [p.id, p]));
    return selection.map(id => byId.get(id)).filter((p): p is Product => !!p);
  }
}
