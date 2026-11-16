import { Routes } from '@angular/router';
import { ContentPageComponent } from './pages/content-page.component';
import { customerAuthGuard } from './customer-auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', component: ContentPageComponent, data: { template: 'home' } },
  ...['shop', 'cart', 'checkout', 'login', 'register'].map(path => ({
    path, component: ContentPageComponent, data: { template: path }
  })),
  { path: 'account', component: ContentPageComponent, data: { template: 'account' }, canActivate: [customerAuthGuard] },
  { path: 'products/:slug', component: ContentPageComponent, data: { template: 'product' } },
  { path: 'product/:id', component: ContentPageComponent, data: { template: 'product' } },
  { path: 'order-success/:orderNumber', component: ContentPageComponent, data: { template: 'order-success' } },
  { path: 'layout-preview', component: ContentPageComponent, data: { preview: true } },
  { path: 'pages/:slug', component: ContentPageComponent },
  { path: '**', redirectTo: '' },
];
