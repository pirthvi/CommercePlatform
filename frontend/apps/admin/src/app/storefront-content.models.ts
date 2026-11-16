export interface SiteNavigationLink {
  label: string;
  path: string;
  category?: string;
}

export interface SiteFooterLink extends SiteNavigationLink {}

export interface SiteFooterColumn {
  heading: string;
  links: SiteFooterLink[];
}

export interface SiteHomeItem {
  icon?: string;
  title: string;
  body?: string;
  category?: string;
  path?: string;
}

export interface SiteHomeSection {
  type: 'hero' | 'benefits' | 'collections' | 'featured' | 'story' | 'values' | 'banner' | 'editorial';
  visible: boolean;
  imageKey?: string;
  clearImage?: boolean;
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
  items: SiteHomeItem[];
}

export interface StorefrontContent {
  announcementText: string;
  announcementSecondary: string;
  brandMark: string;
  brandName: string;
  brandTagline: string;
  searchLabel: string;
  signInLabel: string;
  accountLabel: string;
  bagLabel: string;
  logoUrl?: string | null;
  footerStory: string;
  navigation: SiteNavigationLink[];
  footerColumns: SiteFooterColumn[];
  footerBottom: string[];
  homeSections: SiteHomeSection[];
}

export function moveItem<T>(items: T[], from: number, to: number): void {
  if (to < 0 || to >= items.length || from < 0 || from >= items.length || from === to) return;
  const [moved] = items.splice(from, 1);
  items.splice(to, 0, moved);
}

