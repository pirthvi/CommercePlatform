import { StoreInterface, Surface } from '@commerce/store';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';

@Component({
  standalone: true, imports: [FormsModule],
  templateUrl: './interface-text.component.html', styleUrl: './interface-text.component.css',
})
export class InterfaceTextComponent implements OnInit {
  private readonly api = inject(AdminApiService);
  readonly ui = inject(StoreInterface);
  surface: Surface = 'storefront';
  query = '';
  group = '';
  original: Record<string,string> = {};
  values: Record<string,string> = {};
  groups: string[] = [];
  message = '';
  busy = false;
  ngOnInit(): void { this.load(this.surface); }
  get dirty(): boolean { return Object.keys(this.values).some(k => this.values[k] !== this.original[k]); }
  get keys(): string[] {
    const term = this.query.trim().toLowerCase();
    return Object.keys(this.values).filter(k => k.split('.')[0] === this.group && (k+' '+this.values[k]).toLowerCase().includes(term));
  }
  load(surface: Surface): void {
    if (this.dirty && !confirm(this.ui.text('interface-text.discard'))) return;
    this.busy = true;
    this.api.interfaceConfig(surface).subscribe({next: cfg => {
      this.surface = surface; this.original = cfg.texts; this.values = {...cfg.texts}; this.groups = [...new Set(Object.keys(cfg.texts).map(k=>k.split('.')[0]))].sort();
      this.group = this.groups[0] ?? ''; this.query = ''; this.message = ''; this.busy = false;
    }, error: () => { this.busy = false; this.message = this.ui.text('interface-text.couldNotLoadInterfaceText'); }});
  }
  save(): void {
    const edited = Object.fromEntries(Object.entries(this.values).filter(([k,v])=>this.original[k] !== v));
    if (this.busy || !Object.keys(edited).length) return;
    this.busy = true;
    this.api.saveTexts(this.surface,edited).subscribe({next: () => {
      this.original = {...this.values}; if (this.surface === this.ui.surface) this.ui.config.update(c=>c ? {...c,texts:{...c.texts,...edited}} : c);
      this.busy = false; this.message = this.ui.text('interface-text.interfaceTextSaved');
    }, error: err => { this.busy = false; this.message = err.error?.detail || this.ui.text('interface-text.couldNotSaveInterfaceText'); }});
  }
}
