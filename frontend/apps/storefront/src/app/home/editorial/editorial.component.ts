import { BlockLinkDirective } from '../../blocks/block-link.directive';
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-editorial-section', standalone: true,
  imports: [BlockLinkDirective, CommonModule],
  templateUrl: './editorial.component.html', styleUrl: './editorial.component.css',
})
export class EditorialSectionComponent {
  @Input({ required: true }) section!: PageBlock;

}
