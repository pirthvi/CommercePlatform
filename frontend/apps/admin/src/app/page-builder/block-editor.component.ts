import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Category, StoreInterface } from '@commerce/store';
import { BlockItem, PageBlock, blockFields, newBlockId } from '@commerce/layout';
import { ContentImagePickerComponent } from '../content-image-picker.component';
import { Product } from '../models';
import { moveItem } from '../storefront-content.models';

@Component({
  selector: 'admin-block-editor', standalone: true, imports: [FormsModule, ContentImagePickerComponent],
  templateUrl: './block-editor.component.html', styleUrl: './block-editor.component.css',
})
export class BlockEditorComponent {
  readonly ui = inject(StoreInterface);
  readonly fields = blockFields;
  @Input({required: true}) block!: PageBlock;
  @Input() products: Product[] = [];
  @Input() categories: Category[] = [];
  @Input() previews: Record<string, string> = {};
  @Output() changed = new EventEmitter<void>();
  @Output() photo = new EventEmitter<{ entry: PageBlock | BlockItem; file: File | null }>();
  @Output() removed = new EventEmitter<string>();
  get categoryList(): boolean { return this.block.type === 'catalog' || this.block.type === 'related-products'; }
  get hasItems(): boolean { return this.categoryList || ['faq', 'gallery', 'benefits', 'values', 'collections', 'featured'].includes(this.block.type); }

  addItem(): void {
    this.block.items.push({id: newBlockId(), title: '', ...(this.block.type === 'featured' ? { path: 'all' } : {})});
    this.changed.emit();
  }
  removeItem(index: number): void {
    const [item] = this.block.items.splice(index, 1);
    this.removed.emit(item.id);
    this.changed.emit();
  }
  move(index: number, by: number): void {
    moveItem(this.block.items, index, index + by);
    this.changed.emit();
  }
  selectProduct(id: number, selected: boolean): void {
    const ids = this.block.productIds || [];
    this.block.productIds = selected ? [...ids, id] : ids.filter(other => other !== id);
    this.changed.emit();
  }
}
