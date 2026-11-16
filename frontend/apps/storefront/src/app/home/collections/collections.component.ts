import { BlockLinkDirective } from '../../blocks/block-link.directive';
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Product } from '../../models';
import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-collections-section', standalone: true,
  imports: [BlockLinkDirective, CommonModule, RouterLink],
  templateUrl: './collections.component.html', styleUrl: './collections.component.css',
})
export class CollectionsSectionComponent {
  @Input({ required: true }) section!: PageBlock;

  @Input() products: Product[] = [];
  @Input() loading = false;
  collectionProducts(section: PageBlock): Product[] {
    const selectedCategories = section.items.map((item) => item.category).filter((category): category is string => !!category);
    const products = selectedCategories.length
      ? selectedCategories.map((category) => this.products.find((product) => product.categorySlug === category || product.category === category)).filter((product): product is Product => !!product)
      : this.products.filter((product, index, all) => all.findIndex((item) => item.categoryId === product.categoryId) === index);
    return products.slice(0, 4);
  }
}
