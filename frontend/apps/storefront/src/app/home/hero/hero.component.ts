import { BlockLinkDirective } from '../../blocks/block-link.directive';
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-hero-section', standalone: true,
  imports: [BlockLinkDirective, CommonModule],
  templateUrl: './hero.component.html', styleUrl: './hero.component.css',
})
export class HeroSectionComponent {
  @Input({ required: true }) section!: PageBlock;

}
