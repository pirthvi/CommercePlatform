import { PageBlock } from '@commerce/layout';
import { StoreInterface } from '@commerce/store';
import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CustomerAuthService } from '../../customer-auth.service';
import { CartService } from '../../cart.service';

@Component({
  selector: "shop-customer-form-block",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './customer-auth.component.html',
  styleUrl: './customer-auth.component.css',
})
export class CustomerAuthComponent {
  readonly ui = inject(StoreInterface);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(CustomerAuthService);
  private readonly cart = inject(CartService);
  @Input() registering = false;
  fullName = '';
  email = '';
  password = '';
  busy = false;
  error = '';
  @Input({required: true}) section!: PageBlock;


  submit(form: NgForm): void {
    if (!form.valid || this.busy) return;
    this.busy = true;
    this.error = '';
    const request = this.registering
      ? this.auth.register({ fullName: this.fullName, email: this.email, password: this.password })
      : this.auth.login({ email: this.email, password: this.password });
    request.subscribe({
      next: () => {
        const requestedReturnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/account';
        const returnUrl = requestedReturnUrl.startsWith('/') && !requestedReturnUrl.startsWith('//') ? requestedReturnUrl : '/account';
        this.cart.mergeGuestCart().subscribe({
          next: () => { this.busy = false; void this.router.navigateByUrl(returnUrl); },
          error: () => { this.busy = false; this.cart.refresh(); void this.router.navigateByUrl(returnUrl); },
        });
      },
      error: (response: { status?: number; error?: { detail?: string; message?: string } }) => {
        this.busy = false;
        const status = response.status;
        this.error = response.error?.detail || response.error?.message || (status === 0
          ? this.ui.text('customer-auth.weCouldnTReachTheShopJustNow')
          : this.registering
            ? (status === 409 ? this.ui.text('customer-auth.thereSAlreadyAnAccountWithThisEmail') : this.ui.text('customer-auth.weCouldnTCreateYourAccountPleaseCheck'))
            : this.ui.text('customer-auth.thatEmailAndPasswordDonTMatchPlease'));
      },
    });
  }
}
