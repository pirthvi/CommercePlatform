import { Component, Input, inject } from '@angular/core';
import { StoreInterface } from '@commerce/store';

@Component({
  selector: 'shop-product-icon',
  standalone: true,
  host: { 'aria-hidden': 'true' },
  templateUrl: './product-icon.component.html',
  styleUrl: './product-icon.component.css',
})
export class ProductIconComponent {
  readonly ui = inject(StoreInterface);
  @Input({ required: true }) slot!: string;
}
