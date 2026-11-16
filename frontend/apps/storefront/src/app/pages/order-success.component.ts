import { PageBlock } from '@commerce/layout';
import { StoreInterface, StoreMoneyPipe } from '@commerce/store';
import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../api.service';
import { OrderReceipt } from '../models';

@Component({
  selector: "shop-receipt-block",
  standalone: true, imports: [StoreMoneyPipe, CommonModule, DatePipe, RouterLink],
  templateUrl: './order-success.component.html',
  styleUrl: './order-success.component.css',
})
export class OrderSuccessComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ApiService);
  receipt?: OrderReceipt;
  @Input({required: true}) section!: PageBlock;
  error = '';
  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const orderNumber = params.get('orderNumber');
      if (!orderNumber) { this.error = this.ui.text('order-success.thisLinkIsMissingAnOrderNumberVisit'); return; }
      this.api.receipt(orderNumber).subscribe({ next: (receipt) => this.receipt = receipt, error: () => this.error = this.ui.text('order-success.weCouldnTLoadThisReceiptPleaseCheck') });
    });
  }
}
