import { PageBlock } from '@commerce/layout';
import { StoreInterface, StoreMoneyPipe } from '@commerce/store';
import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { CartService } from '../cart.service';
import { CustomerAuthService } from '../customer-auth.service';

@Component({
  selector: "shop-checkout-block",
  standalone: true, imports: [StoreMoneyPipe, CommonModule, FormsModule, RouterLink],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css',
})
export class CheckoutComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  readonly cart = inject(CartService);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly auth = inject(CustomerAuthService);
  @Input({required: true}) section!: PageBlock;
  submitting = false;
  error = '';
  form = { customerName: '', email: '', phone: '', address: '', city: '', postalCode: '' };

  ngOnInit(): void {
    if (this.auth.customer) {
      this.form.customerName = this.auth.customer.fullName;
      this.form.email = this.auth.customer.email;
    }
  }

  submit(form: NgForm): void {
    if (!form.valid || this.submitting || !this.cart.lines.length) return;
    this.submitting = true; this.error = '';
    const payload = { ...this.form, items: this.cart.lines.map((line) => ({ productId: line.product.id, quantity: line.quantity })) };
    this.api.placeOrder(payload).subscribe({
      next: (receipt) => { this.cart.clear(); void this.router.navigate(['/order-success', receipt.orderNumber]); },
      error: (err: { error?: { detail?: string; message?: string } }) => { this.error = err.error?.detail || err.error?.message || this.ui.text('checkout.weCouldNotPlaceYourOrderPleaseCheck'); this.submitting = false; },
    });
  }
}
