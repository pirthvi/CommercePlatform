import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-values-section', standalone: true,
  imports: [CommonModule],
  templateUrl: './values.component.html', styleUrl: './values.component.css',
})
export class ValuesSectionComponent {
  @Input({ required: true }) section!: PageBlock;

}
