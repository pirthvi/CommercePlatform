import { Component, Input } from '@angular/core';
import { PageBlock } from '@commerce/layout';
import { BlockLinkDirective } from '../block-link.directive';
@Component({
  selector: 'shop-button-block', standalone: true, imports: [BlockLinkDirective],
  templateUrl: './button.component.html', styleUrl: './button.component.css',
})
export class ButtonBlockComponent {
  @Input({required: true}) section!: PageBlock;
}
