import { isExternalLink } from '@commerce/store';
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { StorefrontContent } from '../../models';
import { BrandComponent } from '../brand/brand.component';

@Component({
  selector: 'shop-footer', standalone: true, imports: [CommonModule, RouterLink, BrandComponent],
  templateUrl: './footer.component.html', styleUrl: './footer.component.css',
})
export class FooterComponent {
  @Input({ required: true }) site!: StorefrontContent;
  readonly isExternal = isExternalLink;
  footerLine(line: string): string {
    return line.replaceAll('{year}', String(new Date().getFullYear())).replaceAll('{store}', this.site.brandName);
  }
}
