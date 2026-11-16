import { Directive, Input, OnDestroy, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { StoreInterface } from '@commerce/store';

interface PageMetadata {
  title?: string;
  name?: string;
  metaDescription?: string;
  description?: string;
}

@Directive({ selector: '[storePageMeta]', standalone: true })
export class PageMetaDirective implements OnDestroy {
  private readonly store = inject(StoreInterface);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);

  @Input() set storePageMeta(page: PageMetadata | undefined) {
    const brand = this.store.config()?.brandName ?? '';
    const name = page?.title || page?.name;
    this.title.setTitle(name && name !== brand ? [name, brand].filter(Boolean).join(' · ') : brand);
    this.meta.updateTag({ name: 'description', content: page?.metaDescription || page?.description || '' });
  }

  ngOnDestroy(): void { this.storePageMeta = undefined; }
}
