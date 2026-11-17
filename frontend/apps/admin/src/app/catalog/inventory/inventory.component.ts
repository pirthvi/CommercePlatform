import { StoreInterface } from '@commerce/store';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../admin-api.service';
import { DatePipe } from '@angular/common';
import { StockMovement } from '../../admin-api.service';
import { Product } from '../../models';

@Component({
  standalone: true, imports: [FormsModule, DatePipe],
  templateUrl: './inventory.component.html', styleUrl: './inventory.component.css',
})
export class InventoryComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  private readonly api = inject(AdminApiService);
  products: Product[] = [];
  selected?: Product;
  history: StockMovement[] = [];
  term = '';
  delta: number | null = null;
  reason = '';
  error = '';
  busy = false;
  ngOnInit(): void { this.refresh(); }
  get filtered(): Product[] {
    const term = this.term.trim().toLowerCase();
    return this.products.filter(p => (p.name + ' ' + p.category).toLowerCase().includes(term));
  }
  refresh(): void {
    this.api.products().subscribe({next: rows => {
      this.products = rows;
      if (this.selected) this.select(rows.find(p => p.id === this.selected!.id));
    }, error: () => this.error = this.ui.text('inventory.couldNotLoadInventory')});
  }
  select(product?: Product): void {
    this.selected = product; this.delta = null; this.reason = ''; this.history = [];
    if (product) this.api.stockHistory(product.id).subscribe({
      next: rows => { if (this.selected?.id === product.id) this.history = rows; },
      error: () => this.error = this.ui.text('inventory.couldNotLoadStockHistory'),
    });
  }
  save(): void {
    if (!this.selected || !this.delta || !Number.isInteger(this.delta) || !this.reason.trim() || this.busy) return;
    this.busy = true; this.error = '';
    this.api.adjustStock(this.selected.id, this.delta, this.selected.stock, this.reason.trim()).subscribe({
      next: product => { this.busy = false; this.products = this.products.map(p => p.id === product.id ? product : p); this.select(product); },
      error: err => { this.busy = false; this.error = err.error?.detail || this.ui.text('inventory.couldNotAdjustInventory'); },
    });
  }
  movementLabel(reason: string): string {
    if (reason === 'opening' || reason === 'product_edit') return this.ui.text('inventory.' + reason);
    const [action, number] = reason.split('/');
    if (number && ['order', 'cancelled', 'reopened'].includes(action)) {
      return this.ui.text('inventory.' + action, { number: number.slice(0, 8).toUpperCase() });
    }
    return reason;
  }
}
