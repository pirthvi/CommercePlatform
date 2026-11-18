import { PageBuilderComponent } from './page-builder/page-builder/page-builder.component';
import { ProductPageComponent } from './page-templates/product-page/product-page.component';
import { StoreInterface } from '@commerce/store';
import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { OrdersComponent } from './orders/orders.component';
import { PagesComponent } from './pages/pages.component';
import { CategoriesComponent } from './catalog/categories/categories.component';
import { InventoryComponent } from './catalog/inventory/inventory.component';
import { CommerceSettingsComponent } from './settings/commerce/commerce.component';
import { InterfaceTextComponent } from './settings/interface-text/interface-text.component';
import { SitemapComponent } from './settings/sitemap/sitemap.component';
import { ProductsComponent } from './products/products.component';
import { SiteContentComponent } from './site-content/site-content.component';
import { editorLabels, siteEditors } from './studio-navigation';

function pageTitle(key: string) {
  return () => {
    const store = inject(StoreInterface);
    return [store.text(key), store.config()?.brandName].filter(Boolean).join(' · ');
  };
}

export const studioRoutes: Routes = [
  { path: '', pathMatch: 'full', component: DashboardComponent, title: pageTitle('navigation.overview') },
  { path: 'products/categories', component: CategoriesComponent, title: pageTitle('navigation.categories') },
  { path: 'products/inventory', component: InventoryComponent, title: pageTitle('navigation.inventory') },
  { path: 'settings', pathMatch:'full', redirectTo: 'settings/commerce' },
  { path: 'settings/commerce', component: CommerceSettingsComponent, title: pageTitle('navigation.commerce') },
  { path: 'settings/sitemap', component: SitemapComponent, title: pageTitle('navigation.sitemap') },
  { path: 'settings/text', component: InterfaceTextComponent, title: pageTitle('navigation.text') },
  { path: 'products', component: ProductsComponent, title: pageTitle('navigation.products') },
  { path: 'orders', component: OrdersComponent, title: pageTitle('navigation.orders') },
  { path: 'pages', children: [
    { path: '', pathMatch: 'full', component: PagesComponent, title: pageTitle('navigation.pages') },
    ...[
      { path: 'new', key: 'builder.newPage', template: null },
      { path: 'edit/:id', key: 'builder.name', template: null },
      { path: 'home', key: 'navigation.home', template: 'home' },
      { path: 'product-page', key: 'navigation.productPage', template: 'product' },
    ].map(route => ({ path: route.path, pathMatch: 'full' as const, component: PageBuilderComponent,
      data: {layoutTemplate: route.template}, title: pageTitle(route.key),
      canDeactivate: [(editor: PageBuilderComponent) => editor.canLeave()] })),
    { path: 'product-page/wording', component: ProductPageComponent, title: pageTitle('navigation.productPage'),
      canDeactivate: [(editor: ProductPageComponent) => !editor.dirty || (!editor.saving && window.confirm(editor.ui.text('pdpEditor.discardPrompt')))] },
  ] },
  { path: 'site-content', children: [
    { path: '', pathMatch: 'full', redirectTo: 'logo' },
    { path: 'brand', pathMatch: 'full', redirectTo: 'logo' },
    { path: 'header', pathMatch: 'full', redirectTo: 'navigation/header' },
    { path: 'navigation/header', component: SiteContentComponent, data: { editor: 'header' }, title: pageTitle(editorLabels.header) },
    ...siteEditors.map(({ area, slug }) => ({
      path: slug, component: SiteContentComponent, data: { editor: area }, title: pageTitle(editorLabels[area]),
    })),
  ] },
  { path: '**', redirectTo: '' },
];
