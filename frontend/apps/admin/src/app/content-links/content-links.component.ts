import { Category, StoreInterface } from '@commerce/store';
import { inject, Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SiteNavigationLink, moveItem } from '../storefront-content.models';

@Component({
  selector: 'admin-content-links',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './content-links.component.html',
  styleUrl: './content-links.component.css',
})
export class ContentLinksComponent {
  readonly ui = inject(StoreInterface);
  @Input() categories: Category[] = [];
  @Input({ required: true }) links!: SiteNavigationLink[];
  readonly move = moveItem;
}
