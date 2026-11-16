import { BlockType, PageBlock, PageLayout } from '@commerce/layout';
import { Observable, map, of } from 'rxjs';
import { ApiService } from './api.service';

// An already-running pre-builder API can serve the existing site during a frontend-first update.
export function legacyPageLayout(api: ApiService, template: string | null, slug: string): Observable<PageLayout> {
  const layout: PageLayout = { id: null, templateKey: template, title: '', slug, metaDescription: '', published: true, revision: 0, required: [], sections: [] };
  const block = (type: BlockType): PageBlock => ({ id: type, type, visible: true, items: [] });
  if (template === 'home') return api.siteContent().pipe(map(site => {
    const sections = site.homeSections.filter(s => s.visible).map((s, index) => ({
      ...s, id: String(index), items: s.items.map((item, i) => ({...item, id: String(i)})),
    }));
    return { ...layout, sections, metaDescription: sections.find(s => s.type === 'hero')?.body || '' };
  }));
  if (template === 'product') return of({ ...layout, sections: [block('product-gallery'), block('product-purchase'), block('related-products')] });
  const core: Record<string, BlockType> = { shop: 'catalog', cart: 'cart', checkout: 'checkout', register: 'customer-form',
    login: 'customer-form', account: 'account', 'order-success': 'receipt' };
  return api.page(slug).pipe(map(page => {
    const main = { ...block(core[template || ''] || 'text'), eyebrow: page.eyebrow, heading: page.title,
      body: page.body, imageUrl: page.heroImageUrl || '', imageAlt: page.title };
    const sections: PageBlock[] = [main];
    if (page.buttonLabel && page.buttonLink) sections.push({ ...block('button'), buttonLabel: page.buttonLabel, buttonPath: page.buttonLink });
    return { ...layout, id: page.id, title: page.title, metaDescription: page.metaDescription || '', sections };
  }));
}
