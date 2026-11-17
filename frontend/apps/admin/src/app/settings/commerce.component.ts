import { CommerceOptions, StoreFontFamily, StoreInterface } from '@commerce/store';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';

@Component({
  standalone: true, imports: [FormsModule],
  templateUrl: './commerce.component.html', styleUrl: './commerce.component.css',
})
export class CommerceSettingsComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  readonly ui = inject(StoreInterface);
  options!: CommerceOptions;
  readonly fontFamilies: { value: StoreFontFamily; label: string }[] = [
    { value: 'brand', label: 'commerce.brandFontPairing' },
    { value: 'dm-sans', label: 'commerce.fontDmSans' },
    { value: 'playfair-display', label: 'commerce.fontPlayfairDisplay' },
    { value: 'georgia', label: 'commerce.fontGeorgia' },
    { value: 'arial', label: 'commerce.fontArial' },
  ];
  message = '';
  busy = false;
  ngOnInit(): void { this.options = { ...this.ui.config()!.commerce }; }
  save(): void {
    if (this.busy) return;
    this.busy = true; this.message = '';
    this.api.saveCommerce(this.options).subscribe({
      next: saved => { this.options = {...saved}; this.ui.config.update(c => c ? {...c, commerce:saved} : c); this.busy = false; this.message = this.ui.text('commerce.settingsSaved'); },
      error: err => { this.busy = false; this.message = err.error?.detail || this.ui.text('commerce.couldNotSaveSettings'); },
    });
  }
}
