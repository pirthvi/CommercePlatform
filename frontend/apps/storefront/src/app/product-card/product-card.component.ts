import { StoreInterface, StoreMoneyPipe } from '@commerce/store';
import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Product } from '../models';
import { CartService } from '../cart.service';

@Component({
  selector: 'shop-product-card', standalone: true, imports: [StoreMoneyPipe, CommonModule, RouterLink],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.css',
})
export class ProductCardComponent {
  readonly ui = inject(StoreInterface);
  @Input({ required: true }) product!: Product;
  private readonly cart = inject(CartService);
  added = false;
  add(): void { this.cart.add(this.product); this.added = true; setTimeout(() => this.added = false, 1200); }
}
