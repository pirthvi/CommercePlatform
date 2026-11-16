import { StoreInterface } from '@commerce/store';
import { inject, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { StorefrontContent } from '../../models';

@Component({
  selector: 'shop-brand', standalone: true, imports: [CommonModule, RouterLink],
  templateUrl: './brand.component.html', styleUrl: './brand.component.css',
})
export class BrandComponent {
  readonly ui = inject(StoreInterface);
  @Input({ required: true }) site!: StorefrontContent;
  @Input() inFooter = false;
}
