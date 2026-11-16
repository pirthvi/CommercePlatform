import { BlockLinkDirective } from '../../blocks/block-link.directive';
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-banner-section', standalone: true,
  imports: [BlockLinkDirective, CommonModule],
  templateUrl: './banner.component.html', styleUrl: './banner.component.css',
})
export class BannerSectionComponent {
  @Input({ required: true }) section!: PageBlock;

}
