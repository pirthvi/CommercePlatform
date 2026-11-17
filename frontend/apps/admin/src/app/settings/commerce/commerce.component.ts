import { CommerceOptions, StoreFontFamily, StoreInterface } from '@commerce/store';
import { DOCUMENT } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../admin-api.service';
import { StudioContentDrafts } from '../../studio-content-drafts';

@Component({
  standalone: true, imports: [FormsModule],
  templateUrl: './commerce.component.html', styleUrl: './commerce.component.css',
})
export class CommerceSettingsComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly document = inject(DOCUMENT);
  private readonly drafts = inject(StudioContentDrafts);
  readonly ui = inject(StoreInterface);
  options!: CommerceOptions;
  storeName = '';
  readonly fontFamilies: { value: StoreFontFamily; label: string }[] = [
    { value: 'brand', label: 'commerce.brandFontPairing' },
    { value: 'dm-sans', label: 'commerce.fontDmSans' },
    { value: 'playfair-display', label: 'commerce.fontPlayfairDisplay' },
    { value: 'georgia', label: 'commerce.fontGeorgia' },
    { value: 'arial', label: 'commerce.fontArial' },
  ];
  message = '';
  busy = false;
  ngOnInit(): void {
    const config = this.ui.config()!;
    this.options = { ...config.commerce };
    this.storeName = config.brandName;
  }
  save(): void {
    if (this.busy) return;
    this.busy = true; this.message = '';
    const storeName = this.storeName.trim();
    this.api.saveCommerce(this.options, storeName).subscribe({
      next: saved => {
        this.options = {...saved};
        this.storeName = storeName;
        this.drafts.syncBrandName(storeName);
        this.ui.config.update(c => c ? {...c, commerce:saved, brandName:storeName} : c);
        this.document.title = [this.ui.text('navigation.commerce'), storeName].filter(Boolean).join(' · ');
        this.busy = false; this.message = this.ui.text('commerce.settingsSaved');
      },
      error: err => { this.busy = false; this.message = err.error?.detail || this.ui.text('commerce.couldNotSaveSettings'); },
    });
  }
}
