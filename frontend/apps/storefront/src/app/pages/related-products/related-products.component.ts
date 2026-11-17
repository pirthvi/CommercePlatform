import { Component, Input, inject } from '@angular/core';
import { PageBlock } from '@commerce/layout';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { StoreInterface, StoreMoneyPipe } from '@commerce/store';
import { ProductPageState } from '../product-page-state';

@Component({
  selector: 'shop-related-products', standalone: true,
  imports: [CommonModule, StoreMoneyPipe, RouterLink],
  templateUrl: './related-products.component.html', styleUrl: './related-products.component.css',
})
export class RelatedProductsComponent {
  @Input({required: true}) section!: PageBlock;
  readonly ui = inject(StoreInterface);
  readonly pdp = inject(ProductPageState);
}
