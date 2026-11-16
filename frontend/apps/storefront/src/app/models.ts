export interface Product {
  categoryId: number; categorySlug: string;
  id: number; name: string; slug: string; category: string; description: string;
  price: number; stock: number; imageUrl: string; badge?: string | null; featured: boolean; active: boolean;
  galleryImages?: string | null; weight?: string | null; origin?: string | null; tastingNotes?: string | null;
  pairings?: string | null; ingredients?: string | null; highlights?: string | null;
}
export interface ContentPage {
  id: number; title: string; slug: string; eyebrow?: string; body: string; metaDescription?: string;
  heroImageUrl?: string | null; buttonLabel?: string | null; buttonLink?: string | null;
  published: boolean; updatedAt: string;
}
export interface StorefrontContent {
  announcementText: string; announcementSecondary: string; brandMark: string; brandName: string; brandTagline: string;
  searchLabel: string; signInLabel: string; accountLabel: string; bagLabel: string; logoUrl?: string | null;
  footerStory: string; navigation: { label: string; path: string; category?: string }[];
  footerColumns: { heading: string; links: { label: string; path: string; category?: string }[] }[];
  footerBottom: string[]; homeSections: HomeSection[];
}
export interface HomeItem { icon?: string; title: string; body?: string; category?: string; path?: string; }
export interface HomeSection {
  type: 'hero' | 'benefits' | 'collections' | 'featured' | 'story' | 'values' | 'banner' | 'editorial';
  visible: boolean; imageKey?: string; clearImage?: boolean; eyebrow?: string; heading?: string; body?: string; imageUrl?: string; imageAlt?: string;
  imageCaption?: string; buttonLabel?: string; buttonPath?: string; buttonCategory?: string;
  linkLabel?: string; linkPath?: string; cardEyebrow?: string; cardLinkLabel?: string;
  emptyMessage?: string; emptyLinkLabel?: string; items: HomeItem[];
}
export interface CartLine { product: Product; quantity: number; }
export interface CartResponse { items: CartLine[]; }
export interface OrderReceipt {
  currency: string;
  orderNumber: string; createdAt: string; status: string; subtotal: number; shipping: number; total: number;
  items: { productId: number; productName: string; quantity: number; unitPrice: number }[];
}
