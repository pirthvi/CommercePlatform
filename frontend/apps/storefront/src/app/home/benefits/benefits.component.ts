import { StoreInterface } from '@commerce/store';
import { inject, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-benefits-section', standalone: true,
  imports: [CommonModule],
  templateUrl: './benefits.component.html', styleUrl: './benefits.component.css',
})
export class BenefitsSectionComponent {
  readonly ui = inject(StoreInterface);
  @Input({ required: true }) section!: PageBlock;

}
