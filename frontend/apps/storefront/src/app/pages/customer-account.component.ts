import { PageBlock } from '@commerce/layout';
import { StoreInterface, StoreMoneyPipe } from '@commerce/store';
import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CustomerAuthService } from '../customer-auth.service';
import { OrderReceipt } from '../models';
import { CartService } from '../cart.service';

@Component({
  selector: "shop-account-block",
  standalone: true,
  imports: [StoreMoneyPipe, CommonModule, DatePipe, RouterLink],
  templateUrl: './customer-account.component.html',
  styleUrl: './customer-account.component.css',
})
export class CustomerAccountComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  readonly auth = inject(CustomerAuthService);
  private readonly router = inject(Router);
  private readonly cart = inject(CartService);
  @Input({required: true}) section!: PageBlock;
  orders: OrderReceipt[] = [];
  loading = true;
  error = '';

  ngOnInit(): void {
    this.auth.orders().subscribe({
      next: (orders) => { this.orders = orders; this.loading = false; },
      error: () => { this.error = this.ui.text('customer-account.weCouldnTLoadYourOrdersPleaseSign'); this.loading = false; },
    });
  }

  signOut(): void {
    this.auth.signOut();
    this.cart.refresh();
    void this.router.navigate(['/shop']);
  }
}
