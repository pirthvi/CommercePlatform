  import { Category, StoreInterface } from '@commerce/store';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';

@Component({
  standalone: true, imports: [FormsModule],
  templateUrl: './categories.component.html', styleUrl: './categories.component.css',
})
export class CategoriesComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  private readonly api = inject(AdminApiService);
  rows: Category[] = [];
  editing: number | null = null;
  form = { name: '', description: '' };
  pending = false;
  loading = true;
  error = '';
  ngOnInit(): void { this.load(); }
  load(): void {
    this.api.categories().subscribe({next: rows => { this.rows = rows; this.loading = false; }, error: () => { this.loading = false; this.error = this.ui.text('categories.couldNotLoadCategories'); }});
  }
  edit(row?: Category): void {
    this.editing = row?.id ?? null;
    this.form = { name: row?.name ?? '', description: row?.description ?? '' };
    this.error = '';
  }
  save(): void {
    if (this.pending || !this.form.name.trim()) return;
    this.pending = true;
    this.api.saveCategory(this.editing, this.form).subscribe({
      next: () => { this.pending = false; this.edit(); this.load(); },
      error: err => { this.pending = false; this.error = err.error?.detail || this.ui.text('categories.couldNotSaveCategory'); },
    });
  }
  remove(row: Category): void {
    if (this.pending || !confirm(this.ui.text('categories.confirmDelete', {name: row.name}))) return;
    this.pending = true;
    this.api.deleteCategory(row.id).subscribe({
      next: () => { this.pending = false; if (this.editing === row.id) this.edit(); this.load(); },
      error: err => { this.pending = false; this.error = err.error?.detail || this.ui.text('categories.couldNotRemoveCategory'); },
    });
  }
}
