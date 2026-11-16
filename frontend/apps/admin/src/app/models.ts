export interface Product {
  categoryId: number | null; categorySlug?: string;
  id: number; name: string; slug: string; category: string; description: string;
  price: number; stock: number; imageUrl: string; badge?: string | null; featured: boolean; active: boolean;
  galleryImages?: string | null; weight?: string | null; origin?: string | null; tastingNotes?: string | null;
  pairings?: string | null; ingredients?: string | null; highlights?: string | null;
}
export interface ContentPage {
  id: number; templateKey?: string | null; title: string; slug: string; eyebrow?: string; body: string; metaDescription?: string;
  heroImageUrl?: string | null; buttonLabel?: string | null; buttonLink?: string | null; published: boolean; updatedAt: string;
}
export interface CustomerOrder {
  currency: string;
  id: number; orderNumber: string; customerName: string; email: string; phone: string;
  address: string; city: string; postalCode: string; subtotal: number; shipping: number; total: number;
  status: 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'; createdAt: string;
  items: { productId: number; productName: string; quantity: number; unitPrice: number }[];
}
