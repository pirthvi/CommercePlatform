import { StoreInterface, StoreMoneyPipe } from '@commerce/store';
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from './admin-api.service';
import { CustomerOrder } from './models';

@Component({ selector: 'admin-orders', standalone: true, imports: [StoreMoneyPipe, CommonModule, DatePipe, FormsModule], templateUrl: './orders.component.html',
  styleUrl: './orders.component.css', })
export class OrdersComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  private readonly api = inject(AdminApiService);
  orders: CustomerOrder[] = []; loading = true; error = '';
  ngOnInit(): void { this.load(); }
  load(): void { this.loading = true; this.api.orders().subscribe({ next: (orders) => { this.orders = orders; this.loading = false; }, error: (err) => { this.loading = false; this.error = err.error?.detail || err.error?.message || this.ui.text('orders.couldNotLoadOrders'); } }); }
  changeStatus(order: CustomerOrder): void { this.api.updateOrderStatus(order.id, order.status).subscribe({ next: (updated) => { order.status = updated.status; }, error: (err) => { this.error = err.error?.detail || err.error?.message || this.ui.text('orders.couldNotUpdateTheOrderStatus'); this.load(); } }); }
}
