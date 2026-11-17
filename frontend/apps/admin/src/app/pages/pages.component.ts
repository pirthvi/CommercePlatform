import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StoreInterface } from '@commerce/store';
import { AdminApiService } from '../admin-api.service';
import { ContentPage } from '../models';
import { storefrontPagePath, storefrontUrl } from '../studio-navigation';

@Component({
  standalone:true, imports:[CommonModule, RouterLink],
  templateUrl:'./pages.component.html', styleUrl:'./pages.component.css',
})
export class PagesComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  private readonly api = inject(AdminApiService);
  pages: ContentPage[] = [];
  loading = true;
  error = '';
  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading = true; this.error = '';
    this.api.pages().subscribe({
      next: pages => { this.pages = pages; this.loading = false; },
      error: err => { this.error = err.error?.detail || this.ui.text('pages.couldNotLoadPages'); this.loading = false; },
    });
  }
  editor(page: ContentPage): (string | number)[] {
    if (page.templateKey === 'home') return ['/pages/home'];
    if (page.templateKey === 'product') return ['/pages/product-page'];
    return ['/pages/edit', page.id];
  }
  url(page: ContentPage): string | null {
    if (page.templateKey === 'product' || page.templateKey === 'order-success') return null;
    return storefrontUrl(storefrontPagePath(page.slug));
  }
  remove(page: ContentPage): void {
    if (page.templateKey || !window.confirm(this.ui.text('pages.confirmRemove',{name:page.title}))) return;
    this.api.deletePage(page.id).subscribe({
      next: () => this.load(),
      error: err => this.error = err.error?.detail || this.ui.text('pages.couldNotRemoveThisPage'),
    });
  }
}
