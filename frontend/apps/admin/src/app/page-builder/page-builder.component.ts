import { Component, DestroyRef, ElementRef, HostListener, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, catchError, forkJoin, merge, of, switchMap } from 'rxjs';
import { BlockItem, BlockType, PageBlock, PageLayout, contentTypes, newBlockId } from '@commerce/layout';
import { Category, StoreInterface } from '@commerce/store';
import { AdminApiService } from '../admin-api.service';
import { Product } from '../models';
import { moveItem } from '../storefront-content.models';
import { storefrontUrl } from '../studio-navigation';
import { StudioContentDrafts } from '../studio-content-drafts';
import { BlockEditorComponent } from './block-editor.component';

@Component({
  standalone: true, imports: [FormsModule, RouterLink, BlockEditorComponent],
  templateUrl: './page-builder.component.html', styleUrl: './page-builder.component.css',
})
export class PageBuilderComponent implements OnInit, OnDestroy {
  readonly ui = inject(StoreInterface);
  private readonly drafts = inject(StudioContentDrafts);
  private readonly api = inject(AdminApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy = inject(DestroyRef);
  private readonly reload = new Subject<void>();
  private readonly previewOrigin = new URL(storefrontUrl()).origin;
  readonly previewUrl = inject(DomSanitizer).bypassSecurityTrustResourceUrl(storefrontUrl('/layout-preview'));
  @ViewChild('frame') frame?: ElementRef<HTMLIFrameElement>;
  page?: PageLayout;
  selected?: PageBlock;
  library = false;
  preview = false;
  get dirty(): boolean { return this.drafts.pageEditorDirty; }
  set dirty(value: boolean) { this.drafts.pageEditorDirty = value; }
  saving = false;
  loading = true;
  conflict = false;
  error = '';
  notice = '';
  choicesFailed = false;
  products: Product[] = [];
  categories: Category[] = [];
  previews: Record<string, string> = {};
  private uploads: Record<string, File> = {};
  private customSlug = false;
  get available(): readonly BlockType[] {
    return this.page?.templateKey === 'product' && !this.page.sections.some(b => b.type === 'related-products')
      ? [...contentTypes, 'related-products'] : contentTypes;
  }

  ngOnInit(): void {
    this.loadChoices();
    merge(this.route.paramMap, this.reload).pipe(switchMap(() => {
      this.resetFiles(); this.error = ''; this.notice = ''; this.conflict = false;
      this.page = undefined; this.selected = undefined; this.dirty = false; this.loading = true;
      this.preview = false; this.library = false;
      const id = this.route.snapshot.paramMap.get('id');
      const template: string = this.route.snapshot.data['layoutTemplate'];
      return id || template ? this.api.layout(id || 'template/' + template).pipe(catchError(err => {
        this.error = this.loadError(err); return of(null);
      })) : of(this.blank());
    }), takeUntilDestroyed(this.destroy)).subscribe(page => {
      this.loading = false;
      if (page) this.open(page);
    });
  }

  loadChoices(): void {
    this.choicesFailed = false;
    forkJoin({ products: this.api.products(), categories: this.api.categories() }).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: value => { this.products = value.products; this.categories = value.categories; },
      error: () => this.choicesFailed = true,
    });
  }
  private blank(): PageLayout {
    return { id: null, title: '', slug: '', metaDescription: '', templateKey: null, published: false, revision: 0, required: [], sections: [] };
  }
  private open(page: PageLayout): void {
    const selection = this.selected?.id;
    this.page = page;
    this.selected = page.sections.find(s => s.id === selection) || page.sections[0];
    this.customSlug = page.id !== null;
    this.dirty = false;
    this.resetFiles();
  }
  editTitle(title: string): void {
    if (!this.page) return;
    this.page.title = title;
    if (!this.customSlug) this.page.slug = title.toLowerCase().trim().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
    this.change();
  }
  editSlug(): void { this.customSlug = true; this.change(); }
  change(): void { this.dirty = true; this.notice = ''; }
  add(type: BlockType): void {
    if (!this.page) return;
    const section: PageBlock = { id: newBlockId(), type, visible: true, items: [] };
    this.page.sections.push(section);
    this.selected = section; this.library = false; this.change();
  }
  move(index: number, distance: number): void {
    if (this.page) { moveItem(this.page.sections, index, index + distance); this.change(); }
  }
  remove(block: PageBlock): void {
    if (!this.page || this.page.required.includes(block.type)) return;
    const i = this.page.sections.indexOf(block);
    this.page.sections.splice(i, 1);
    this.dropFile(block.id);
    block.items.forEach(item => this.dropFile(item.id));
    if (this.selected === block) this.selected = this.page.sections[i] || this.page.sections[i-1];
    this.change();
  }
  photo(entry: PageBlock | BlockItem, file: File | null): void {
    this.dropFile(entry.id);
    if (file) { this.uploads[entry.id] = file; this.previews[entry.id] = URL.createObjectURL(file); }
    else entry.imageUrl = '';
    this.change();
  }
  dropFile(id: string): void {
    if (this.previews[id]) URL.revokeObjectURL(this.previews[id]);
    delete this.previews[id]; delete this.uploads[id];
  }
  reloadPage(): void { if (this.canLeave()) this.reload.next(); }
  canLeave(): boolean { return !this.dirty || (!this.saving && window.confirm(this.ui.text('builder.discard'))); }
  @HostListener('window:beforeunload', ['$event']) beforeUnload(event: BeforeUnloadEvent): void {
    if (this.dirty) { event.preventDefault(); event.returnValue = ''; }
  }
  @HostListener('window:message', ['$event']) previewReady(event: MessageEvent): void {
    const frame = this.frame?.nativeElement.contentWindow;
    if (this.preview && frame && event.source === frame && event.origin === this.previewOrigin && event.data?.type === 'page-preview-ready') {
      frame.postMessage({type: 'page-preview', layout: this.page, images: this.uploads}, this.previewOrigin);
    }
  }
  save(): void {
    if (!this.page || this.saving || this.conflict || !this.dirty) return;
    if (!this.page.title.trim() || !/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(this.page.slug)) {
      this.error = this.ui.text('builder.titleMissing'); return;
    }
    const isNew = !this.page.id;
    this.saving = true; this.error = ''; this.notice = '';
    this.api.saveLayout(this.page, this.uploads).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: page => {
        this.saving = false; this.open(page); this.notice = this.ui.text('builder.saved');
        if (isNew) void this.router.navigate(['/pages/edit', page.id], {replaceUrl: true});
      },
      error: err => {
        this.saving = false;
        this.conflict = err.status === 409 && err.error?.code === 'layoutConflict';
        this.error = err.error?.detail || this.ui.text('pages.weCouldNotSaveThisPageCheckThat');
      },
    });
  }
  private loadError(err: {error?: {detail?: string}; message?: string}): string {
    return this.ui.config()?.pageLayoutVersion ? err.error?.detail || this.ui.text('builder.loadFailed') :
      this.ui.text('builder.missing') || 'Restart the updated Spring Boot application to load the page builder.';
  }
  ngOnDestroy(): void { this.dirty = false; this.resetFiles(); }
  private resetFiles(): void { Object.keys(this.previews).forEach(id => this.dropFile(id)); }
}
