import { Category, StoreInterface } from '@commerce/store';
import { inject, Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SiteFooterColumn, StorefrontContent, moveItem } from '../storefront-content.models';
import { ContentLinksComponent } from '../content-links/content-links.component';

@Component({
  selector: 'admin-content-footer',
  standalone: true,
  imports: [FormsModule, ContentLinksComponent],
  templateUrl: './content-footer.component.html',
  styleUrl: './content-footer.component.css',
})
export class ContentFooterComponent {
  readonly ui = inject(StoreInterface);
  @Input() categories: Category[] = [];
  @Input({ required: true }) site!: StorefrontContent;
  readonly move = moveItem;
  readonly yearPlaceholder = '{year}';
  newGroup?: SiteFooterColumn;

  addGroup(): void {
    this.newGroup = { heading: '', links: [] };
    this.site.footerColumns.push(this.newGroup);
  }
}
