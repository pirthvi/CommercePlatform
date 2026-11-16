import { Component, Input } from '@angular/core';
import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-text-block', standalone: true, imports: [],
  templateUrl: './text.component.html', styleUrl: './text.component.css',
})
export class TextBlockComponent {
  @Input({required: true}) section!: PageBlock;
  @Input() primary = false;

}
