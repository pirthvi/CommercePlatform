import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StoreInterface, StoreMoneyPipe } from '@commerce/store';
import { ProductPageState } from '../product-page-state';
import { ProductIconComponent } from '../product-icon/product-icon.component';

@Component({
  selector: 'shop-product-purchase', standalone: true,
  imports: [CommonModule, ProductIconComponent, StoreMoneyPipe],
  templateUrl: './product-detail.component.html', styleUrl: './product-detail.component.css',
})
export class ProductDetailComponent {
  readonly pdp = inject(ProductPageState);
  readonly ui = inject(StoreInterface);
  readonly Math = Math;
}
