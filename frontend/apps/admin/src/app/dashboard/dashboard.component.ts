import { StoreInterface, StoreMoneyPipe } from '@commerce/store';
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminApiService } from '../admin-api.service';
import { ContentPage, CustomerOrder, Product } from '../models';

@Component({
  selector: 'admin-dashboard', standalone: true,
  imports: [StoreMoneyPipe, CommonModule, DatePipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  private readonly api = inject(AdminApiService);
  products: Product[] = [];
  orders: CustomerOrder[] = [];
  pages: ContentPage[] = [];
  today = new Date();
  error = '';
  get processing(): number { return this.orders.filter((order) => order.status === 'PROCESSING').length; }
  get publishedPages(): number { return this.pages.filter((page) => page.published).length; }
  get activeProducts(): number { return this.products.filter((product) => product.active).length; }
  ngOnInit(): void {
    this.api.products().subscribe({ next: (value) => this.products = value, error: () => this.error = this.ui.text('dashboard.productDataIsUnavailable') });
    this.api.orders().subscribe({ next: (value) => this.orders = value, error: () => this.error = this.ui.text('dashboard.orderDataIsUnavailable') });
    this.api.pages().subscribe({ next: (value) => this.pages = value, error: () => this.error = this.ui.text('dashboard.pageDataIsUnavailable') });
  }
}
