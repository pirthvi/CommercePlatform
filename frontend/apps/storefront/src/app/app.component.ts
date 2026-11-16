import { StoreInterface } from '@commerce/store';
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ApiService } from './api.service';
import { CartService } from './cart.service';
import { CustomerAuthService } from './customer-auth.service';
import { HeaderComponent } from './layout/header/header.component';
import { FooterComponent } from './layout/footer/footer.component';

@Component({
  selector: 'shop-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, HeaderComponent, FooterComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  readonly cart = inject(CartService);
  readonly api = inject(ApiService);
  readonly customerAuth = inject(CustomerAuthService);
  readonly siteContent$ = this.api.siteContent().pipe(catchError(() => { this.siteContentError = true; return of(null); }));
  siteContentError = false;
  readonly preview = location.pathname === '/layout-preview';

  ngOnInit(): void {
    if (this.preview) {
      this.api.products().subscribe({
        next: products => this.cart.preview(products.find(product => product.stock > 0)),
        error: () => this.cart.preview(),
      });
      return;
    }
    this.customerAuth.resume();
    this.cart.initialize();
  }

}
