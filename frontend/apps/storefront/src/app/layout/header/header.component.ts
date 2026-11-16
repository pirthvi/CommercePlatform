import { StoreInterface, isExternalLink } from '@commerce/store';
import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { StorefrontContent } from '../../models';
import { BrandComponent } from '../brand/brand.component';
import { CartService } from '../../cart.service';
import { CustomerAuthService } from '../../customer-auth.service';

@Component({
  selector: 'shop-header', standalone: true, imports: [CommonModule, RouterLink, BrandComponent, RouterLinkActive],
  templateUrl: './header.component.html', styleUrl: './header.component.css',
})
export class HeaderComponent {
  readonly ui = inject(StoreInterface);
  @Input({ required: true }) site!: StorefrontContent;
  readonly cart = inject(CartService);
  readonly customerAuth = inject(CustomerAuthService);
  readonly isExternal = isExternalLink;
  menuOpen = false;
}
