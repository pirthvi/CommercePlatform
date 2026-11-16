import { BlockLinkDirective } from '../../blocks/block-link.directive';
import { StoreInterface } from '@commerce/store';
import { inject, Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ProductCardComponent } from '../../product-card.component';
import { Product } from '../../models';
import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-featured-section', standalone: true,
  imports: [BlockLinkDirective, CommonModule, ProductCardComponent],
  templateUrl: './featured.component.html', styleUrl: './featured.component.css',
})
export class FeaturedSectionComponent {
  readonly ui = inject(StoreInterface);
  @Input({ required: true }) section!: PageBlock;

  @Input() products: Product[] = [];
  @Input() loading = false;
  @Input() error = '';
  @Output() readonly retry = new EventEmitter<void>();
  activeTab = 0;

  get displayedProducts(): Product[] {
    const tab = this.section.items[this.activeTab];
    let selected = this.products;
    if (tab?.path === 'featured') selected = selected.filter(product => product.featured);
    if (tab?.path === 'category') selected = selected.filter(product => !!tab.category && product.categorySlug === tab.category);
    return selected.slice(0, 8);
  }
}
