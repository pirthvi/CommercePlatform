import { Component, Input } from '@angular/core';
import { PageBlock } from '@commerce/layout';

@Component({
  selector: 'shop-announcement-block', standalone: true,
  templateUrl: './announcement.component.html', styleUrl: './announcement.component.css',
})
export class AnnouncementBlockComponent {
  @Input({ required: true }) section!: PageBlock;
}
