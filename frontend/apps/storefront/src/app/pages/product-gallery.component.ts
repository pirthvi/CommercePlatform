import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StoreInterface } from '@commerce/store';
import { ProductPageState } from './product-page-state';

@Component({
  selector: 'shop-product-gallery', standalone: true,
  imports: [CommonModule],
  templateUrl: './product-gallery.component.html', styleUrl: './product-gallery.component.css',
})
export class ProductGalleryComponent {
  readonly pdp = inject(ProductPageState);
  readonly ui = inject(StoreInterface);
}
