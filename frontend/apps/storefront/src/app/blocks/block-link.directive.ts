import { Location } from '@angular/common';
import { Directive, HostListener, Input, OnChanges, inject } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { isExternalLink } from '@commerce/store';

@Directive({selector:'a[shopBlockLink]', standalone:true, host:{'[attr.href]':'href'}})
export class BlockLinkDirective implements OnChanges {
  @Input({required:true}) shopBlockLink = '';
  @Input() blockCategory?: string;
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private tree?: UrlTree;
  href = '';

  ngOnChanges(): void {
    const path = this.shopBlockLink;
    this.tree = undefined;
    this.href = path;
    if (!path || isExternalLink(path) || path.startsWith('#')) return;
    const tree = this.router.parseUrl(path);
    if (this.blockCategory) tree.queryParams = {...tree.queryParams, category:this.blockCategory};
    this.tree = tree;
    this.href = this.location.prepareExternalUrl(this.router.serializeUrl(tree));
  }
  @HostListener('click', ['$event']) follow(event: MouseEvent): void {
    if (!this.tree || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.altKey || event.shiftKey) return;
    event.preventDefault();
    void this.router.navigateByUrl(this.tree);
  }
}
