import { PageBlock } from '@commerce/layout';
import { StoreInterface, StoreMoneyPipe } from '@commerce/store';
import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CartService } from '../../cart.service';

@Component({
  selector: "shop-cart-block",
  standalone: true, imports: [StoreMoneyPipe, CommonModule, RouterLink],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.css',
})
export class CartComponent {
  readonly ui = inject(StoreInterface);
  readonly cart = inject(CartService);
  @Input({required: true}) section!: PageBlock;
}
