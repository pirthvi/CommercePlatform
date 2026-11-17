import { Category, StoreInterface } from '@commerce/store';
import { Component, Input, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ContentImagePickerComponent } from '../content-image-picker/content-image-picker.component';
import { SiteHomeItem, SiteHomeSection, moveItem } from '../storefront-content.models';
import { StudioContentDrafts } from '../studio-content-drafts';

type Layout = SiteHomeSection['type'];
const layoutLabels: Record<Layout, string> = {
  announcement: 'layouts.announcement',
  hero: 'layouts.hero', benefits: 'layouts.benefits', collections: 'layouts.collections', featured: 'layouts.featured',
  story: 'layouts.story', values: 'layouts.values', banner: 'layouts.banner', editorial: 'layouts.editorial',
};

@Component({
  selector: 'admin-home-editor',
  standalone: true,
  imports: [FormsModule, ContentImagePickerComponent],
  templateUrl: './home-editor.component.html',
  styleUrl: './home-editor.component.css',
})
export class HomeEditorComponent {
  readonly ui = inject(StoreInterface);
  @Input() categories: Category[] = [];
  @Input({ required: true }) sections!: SiteHomeSection[];
  readonly drafts = inject(StudioContentDrafts);
  readonly labels = layoutLabels;
  readonly layouts = Object.keys(layoutLabels) as Layout[];
  readonly move = moveItem;
  newLayout: Layout = 'editorial';
  selectedIndex = 0;

  get current(): SiteHomeSection | undefined {
    return this.sections[Math.min(this.selectedIndex, this.sections.length - 1)];
  }

  title(section: SiteHomeSection): string {
    if (section.type === 'benefits' || section.type === 'values') return this.ui.text(this.labels[section.type]);
    return section.heading?.trim() || this.ui.text(this.labels[section.type]);
  }

  add(): void {
    const section: SiteHomeSection = { type: this.newLayout, visible: true, items: [] };
    if (this.newLayout === 'featured') {
      section.items = [{ title: this.ui.text('navigation.allProducts'), path: 'all' }, { title: this.ui.text('home-editor.featured'), path: 'featured' }];
    }
    this.sections.push(section);
    this.selectedIndex = this.sections.length - 1;
  }

  moveSection(section: SiteHomeSection, direction: number): void {
    const from = this.sections.indexOf(section);
    moveItem(this.sections, from, from + direction);
    this.selectedIndex = this.sections.indexOf(section);
  }

  remove(section: SiteHomeSection): void {
    const index = this.sections.indexOf(section);
    this.drafts.forgetPhoto(section);
    this.sections.splice(index, 1);
    this.selectedIndex = Math.max(0, Math.min(index, this.sections.length - 1));
  }

  addItem(section: SiteHomeSection): void {
    const item: SiteHomeItem = { title: '' };
    if (section.type === 'featured') item.path = 'all';
    section.items ??= [];
    section.items.push(item);
  }
  toggleColorOverride(section: SiteHomeSection, event: Event): void {
    const enabled = (event.target as HTMLInputElement).checked;
    if (enabled) section.color = this.ui.config()?.commerce.componentColor || '#536344';
    else delete section.color;
  }
}
