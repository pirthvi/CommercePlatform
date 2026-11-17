import { StoreInterface } from '@commerce/store';
import { Component, HostListener, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from './auth.service';
import { AdminApiService } from './admin-api.service';
import { StudioContentDrafts } from './studio-content-drafts';
import { editorLabels, pageTabs, siteTabs, storefrontUrl } from './studio-navigation';

@Component({
  selector: 'admin-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  providers: [StudioContentDrafts],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css',
})
export class AdminShellComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  private readonly auth = inject(AuthService);
  private readonly api = inject(AdminApiService);
  private readonly router = inject(Router);
  private readonly drafts = inject(StudioContentDrafts);
  readonly shopUrl = storefrontUrl();
  readonly headerSettingsLabel = editorLabels.header;
  readonly destinations = [
    { path: '/', label: 'navigation.overview', icon: '⌂', children: [] },
    { path: '/products', label: 'navigation.products', icon: '✳', children: [
      {path:'/products', label:'navigation.allProducts'}, {path:'/products/categories', label:'navigation.categories'}, {path:'/products/inventory', label:'navigation.inventory'},
    ] },
    { path: '/orders', label: 'navigation.orders', icon: '⌁', children: [] },
    { path: '/pages', label: 'navigation.pages', icon: '▤', children: pageTabs },
    { path: '/site-content', label: 'navigation.siteContent', icon: '☷', children: siteTabs },
    { path: '/settings', label: 'navigation.settings', icon: '⚙', children: [
      {path:'/settings/commerce', label:'navigation.commerce'}, {path:'/settings/text', label:'navigation.text'},
    ] },
  ];
  path = this.router.url.split(/[?#]/)[0];
  readonly groupExpansion: Record<string, boolean> = {};
  menuOpen = false;
  pendingCount = 0;

  constructor() {
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      takeUntilDestroyed(),
    ).subscribe(event => {
      this.path = event.urlAfterRedirects.split(/[?#]/)[0];
      this.menuOpen = false;
    });
  }

  get group() {
    return this.destinations.find(item => item.path !== '/' && (this.path === item.path || this.path.startsWith(item.path + '/')))
      ?? this.destinations[0];
  }

  get currentTab() {
    const group = this.group;
    return group.children.find(tab => tab.path === this.path ||
      (tab.path !== group.path && this.path.startsWith(tab.path + '/')));
  }

  isGroupExpanded(item: { path: string }): boolean {
    return this.groupExpansion[item.path] ?? this.group.path === item.path;
  }

  toggleGroup(item: { path: string }): void {
    this.groupExpansion[item.path] = !this.isGroupExpanded(item);
  }

  ngOnInit(): void {
    this.api.orders().subscribe({
      next: orders => this.pendingCount = orders.filter(order => order.status === 'PROCESSING').length,
      error: () => this.pendingCount = 0,
    });
  }

  @HostListener('window:beforeunload', ['$event'])
  warnBeforeLeaving(event: BeforeUnloadEvent): void {
    if (this.drafts.hasPendingChanges) {
      event.preventDefault();
      event.returnValue = '';
    }
  }

  logout(): void {
    if (this.drafts.hasPendingChanges && !window.confirm(this.ui.text('shell.discardUnsavedStorefrontChangesAndSignOut'))) return;
    this.auth.signOut();
  }
}
