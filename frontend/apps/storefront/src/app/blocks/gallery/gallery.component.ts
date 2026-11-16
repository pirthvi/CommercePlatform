import { Component, Input } from '@angular/core';
import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-gallery-block', standalone: true, imports: [],
  templateUrl: './gallery.component.html', styleUrl: './gallery.component.css',
})
export class GalleryBlockComponent {
  @Input({required: true}) section!: PageBlock;

}
