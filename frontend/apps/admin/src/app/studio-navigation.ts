export const editorLabels = {
  brand: 'navigation.logo',
  header: 'navigation.header',
  navigation: 'navigation.navigation',
  footer: 'navigation.footer',
  home: 'navigation.home',
} as const;

export type ContentArea = keyof typeof editorLabels;
export const siteEditors = [
  { area: 'brand', slug: 'logo' },
  { area: 'navigation', slug: 'navigation' },
  { area: 'footer', slug: 'footer' },
] as const;
export const siteTabs = siteEditors.map(({ area, slug }) => ({
  label: editorLabels[area], path: '/site-content/' + slug,
}));
export const pageTabs = [
  { label: 'navigation.allPages', path: '/pages' },
  { label: editorLabels.home, path: '/pages/home' },
  { label: 'navigation.productPage', path: '/pages/product-page' },
];

export function storefrontPagePath(slug: string): string {
  if (slug === 'home') return '/';
  if (['shop', 'cart', 'checkout', 'login', 'register', 'account'].includes(slug)) return '/' + slug;
  return '/pages/' + slug;
}

export function storefrontUrl(path = '/'): string {
  const url = new URL(path, window.location.origin);
  if (url.port === '4201') url.port = '4200';
  return url.href;
}
