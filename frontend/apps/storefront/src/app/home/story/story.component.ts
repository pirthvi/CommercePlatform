import { BlockLinkDirective } from '../../blocks/block-link.directive';
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-story-section', standalone: true,
  imports: [BlockLinkDirective, CommonModule],
  templateUrl: './story.component.html', styleUrl: './story.component.css',
})
export class StorySectionComponent {
  @Input({ required: true }) section!: PageBlock;

}
