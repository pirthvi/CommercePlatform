import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { StoreInterface } from '@commerce/store';
import { AdminApiService, SitemapJobStatus } from '../../admin-api.service';
import { storefrontUrl } from '../../studio-navigation';

@Component({
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './sitemap.component.html',
  styleUrl: './sitemap.component.css',
})
export class SitemapComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  private readonly api = inject(AdminApiService);
  status?: SitemapJobStatus;
  siteUrl = '';
  loading = true;
  busy: 'save' | 'generate' | null = null;
  error = '';
  message = '';

  get publicSitemapUrl(): string { return this.status?.publicSitemapUrl || storefrontUrl('/sitemap.xml'); }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.error = '';
    this.api.sitemap().subscribe({
      next: status => { this.status = status; this.siteUrl = status.siteUrl; this.loading = false; },
      error: err => { this.loading = false; this.error = err.error?.detail || this.ui.text('sitemap.loadFailed'); },
    });
  }

  save(): void {
    if (this.busy || !this.siteUrl.trim()) return;
    this.busy = 'save'; this.error = ''; this.message = '';
    this.api.saveSitemap(this.siteUrl.trim()).subscribe({
      next: status => { this.update(status); this.busy = null; this.message = this.ui.text(status.lastRunSucceeded ? 'sitemap.saved' : 'sitemap.savedButGenerationFailed'); },
      error: err => { this.busy = null; this.error = err.error?.detail || this.ui.text('sitemap.saveFailed'); },
    });
  }

  generate(): void {
    if (this.busy) return;
    this.busy = 'generate'; this.error = ''; this.message = '';
    this.api.generateSitemap().subscribe({
      next: status => { this.update(status); this.busy = null; this.message = this.ui.text(status.lastRunSucceeded ? 'sitemap.generated' : 'sitemap.generateFailed'); },
      error: err => { this.busy = null; this.error = err.error?.detail || this.ui.text('sitemap.generateFailed'); },
    });
  }

  private update(status: SitemapJobStatus): void {
    this.status = status;
    this.siteUrl = status.siteUrl;
  }
}
