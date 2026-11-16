import { Component, Input } from '@angular/core';
import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-faq-block', standalone: true, imports: [],
  templateUrl: './faq.component.html', styleUrl: './faq.component.css',
})
export class FaqBlockComponent {
  @Input({required: true}) section!: PageBlock;

}
