export interface BlockItem {
  id: string;
  title: string;
  body?: string;
  icon?: string;
  category?: string;
  path?: string;
  imageUrl?: string;
  imageAlt?: string;
}

export interface PageBlock {
  id: string;
  type: BlockType;
  visible: boolean;
  eyebrow?: string;
  heading?: string;
  body?: string;
  imageUrl?: string;
  imageAlt?: string;
  imageCaption?: string;
  buttonLabel?: string;
  buttonPath?: string;
  buttonCategory?: string;
  linkLabel?: string;
  linkPath?: string;
  cardEyebrow?: string;
  cardLinkLabel?: string;
  emptyMessage?: string;
  emptyLinkLabel?: string;
  categoryId?: number | null;
  productIds?: number[];
  items: BlockItem[];
}

export interface PageLayout {
  id: number | null;
  templateKey: string | null;
  title: string;
  slug: string;
  metaDescription: string;
  published: boolean;
  revision: number;
  required: BlockType[];
  sections: PageBlock[];
}

export const contentTypes = ['text', 'hero', 'gallery', 'story', 'button', 'faq', 'collections', 'product-grid',
  'featured', 'banner', 'benefits', 'values', 'editorial'] as const;
export type CommerceBlock = 'catalog' | 'product-gallery' | 'product-purchase' | 'related-products' | 'cart' | 'checkout' | 'customer-form' | 'account' | 'receipt';
export type BlockType = (typeof contentTypes)[number] | CommerceBlock;
export function includesBlockCategory(block: PageBlock | undefined, slug: string | null | undefined): boolean {
  const items = block?.items ?? [];
  return !items.some(item => item.category) || items.some(item => item.category === slug);
}
export type TextField = 'eyebrow' | 'heading' | 'body' | 'imageAlt' | 'imageCaption' | 'buttonLabel' | 'buttonPath' | 'buttonCategory'
  | 'linkLabel' | 'linkPath' | 'cardEyebrow' | 'cardLinkLabel' | 'emptyMessage' | 'emptyLinkLabel';

const copy: TextField[] = ['eyebrow', 'heading', 'body'];
const action: TextField[] = ['buttonLabel', 'buttonPath', 'buttonCategory'];
const photo: TextField[] = ['imageAlt', 'imageCaption'];
export const blockFields: Record<BlockType, TextField[]> = {
  text: [...copy, ...photo],
  hero: [...copy, ...photo, ...action],
  story: [...copy, ...photo, ...action],
  editorial: [...copy, 'imageAlt', ...action],
  banner: ['eyebrow', 'heading', 'imageAlt', ...action],
  benefits: [], values: [],
  collections: [...copy, 'linkLabel', 'linkPath', 'cardEyebrow', 'cardLinkLabel'],
  featured: [...copy, 'linkLabel', 'linkPath', 'emptyMessage', 'emptyLinkLabel'],
  gallery: ['heading'], button: ['buttonLabel', 'buttonPath'], faq: ['heading'],
  'product-grid': ['eyebrow', 'heading', 'emptyMessage'],
  catalog: [...copy, 'imageAlt'], cart: [...copy, 'imageAlt'], checkout: [...copy, 'imageAlt'],
  'customer-form': [...copy, 'imageAlt'], account: [...copy, 'imageAlt'], receipt: [...copy, 'imageAlt'],
  'product-gallery': [], 'product-purchase': [], 'related-products': [],
};

export function newBlockId(): string {
  return typeof crypto.randomUUID === 'function' ? crypto.randomUUID() :
    Array.from(crypto.getRandomValues(new Uint8Array(16)), n => n.toString(16).padStart(2, '0')).join('');
}
