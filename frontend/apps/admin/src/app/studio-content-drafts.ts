import { StoreInterface } from '@commerce/store';
import { DestroyRef, Injectable, OnDestroy, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminApiService } from './admin-api.service';
import { SiteHomeSection, StorefrontContent } from './storefront-content.models';
import { ContentArea, editorLabels } from './studio-navigation';

const fields: Record<ContentArea, readonly (keyof StorefrontContent)[]> = {
  brand: ['brandMark', 'brandName', 'brandTagline'],
  header: ['announcementText', 'announcementSecondary', 'searchLabel', 'signInLabel', 'accountLabel', 'bagLabel'],
  navigation: ['navigation'],
  footer: ['footerStory', 'footerColumns', 'footerBottom'],
  home: ['homeSections'],
};

function sectionValues(site: StorefrontContent, area: ContentArea): Partial<StorefrontContent> {
  return Object.fromEntries(fields[area].map(key => [key, site[key]]));
}

// Scoped to the signed-in shell so moving between editors keeps the text and selected files.
@Injectable()
export class StudioContentDrafts implements OnDestroy {
  readonly ui = inject(StoreInterface);
  private readonly api = inject(AdminApiService);
  private readonly destroyRef = inject(DestroyRef);
  private published?: StorefrontContent;
  private readonly uploads = new Map<string, { file: File; preview: string }>();
  private clearLogo = false;
  pageEditorDirty = false;

  draft?: StorefrontContent;
  loading = false;
  loadError = '';
  saving: ContentArea | null = null;
  readonly errors: Partial<Record<ContentArea, string>> = {};
  readonly notices: Partial<Record<ContentArea, string>> = {};

  load(): void {
    if (this.loading || this.draft) return;
    this.loading = true;
    this.loadError = '';
    this.api.siteContent().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: site => {
        this.published = site;
        this.draft = structuredClone(site);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.loadError = this.ui.text('studio-content-drafts.couldNotLoadStorefrontContentCheckYourConnection');
      },
    });
  }

  changed(area: ContentArea): boolean {
    if (!this.draft || !this.published) return false;
    if (area === 'brand' && (this.clearLogo || this.uploads.has('logoImage'))) return true;
    if (area === 'home' && this.draft.homeSections.some(s => this.uploads.has('homeImage_' + s.imageKey))) return true;
    return JSON.stringify(sectionValues(this.draft, area)) !== JSON.stringify(sectionValues(this.published, area));
  }

  get hasPendingChanges(): boolean {
    return this.pageEditorDirty || (Object.keys(fields) as ContentArea[]).some(area => this.changed(area));
  }

  save(area: ContentArea): void {
    if (!this.draft || this.saving || !this.changed(area)) return;
    const body: Partial<StorefrontContent> & { clearLogo?: boolean } = sectionValues(this.draft, area);
    const files: Record<string, File> = {};
    if (area === 'brand') {
      body.clearLogo = this.clearLogo;
      const logo = this.uploads.get('logoImage');
      if (logo) files['logoImage'] = logo.file;
    }
    if (area === 'home') {
      for (const section of this.draft.homeSections) {
        const part = 'homeImage_' + section.imageKey;
        const image = this.uploads.get(part);
        if (image) files[part] = image.file;
      }
    }
    this.saving = area;
    delete this.errors[area];
    delete this.notices[area];
    this.api.saveContentSection(area, body, files).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        // Refresh untouched panels from the server while keeping any local drafts in the other panels.
        const refresh = (Object.keys(fields) as ContentArea[]).filter(key => key === area || !this.changed(key));
        this.published = saved;
        for (const key of refresh) this.restore(key);
        if (area === 'brand') this.ui.config.update(config => config ? { ...config, brandName: saved.brandName, brandMark: saved.brandMark, brandTagline: saved.brandTagline, logoUrl: saved.logoUrl ?? null } : config);
        this.notices[area] = this.ui.text('editor.saved', { section: this.ui.text(editorLabels[area]) });
        this.saving = null;
      },
      error: (err: HttpErrorResponse) => {
        this.saving = null;
        this.errors[area] = err.error?.detail || this.ui.text('studio-content-drafts.couldNotSaveYourEditsAreStillHere');
      },
    });
  }

  discard(area: ContentArea): void {
    if (this.saving === area) return;
    this.restore(area);
    delete this.errors[area];
    this.notices[area] = this.ui.text('studio-content-drafts.changesDiscarded');
  }

  get logoPreview(): string {
    return this.uploads.get('logoImage')?.preview ?? (this.clearLogo ? '' : this.draft?.logoUrl ?? '');
  }

  setLogo(file: File | null): void {
    this.selectFile('logoImage', file);
    this.clearLogo = !file && !!this.published?.logoUrl;
  }

  homePhoto(section: SiteHomeSection): string {
    return this.uploads.get('homeImage_' + section.imageKey)?.preview ?? (section.clearImage ? '' : section.imageUrl ?? '');
  }

  setHomePhoto(section: SiteHomeSection, file: File | null): void {
    if (!section.imageKey && file) {
      const suffix = Array.from(crypto.getRandomValues(new Uint32Array(3)), n => n.toString(16)).join('-');
      section.imageKey = 'home-' + suffix;
    }
    this.selectFile('homeImage_' + section.imageKey, file);
    section.clearImage = !file && !!section.imageUrl;
  }

  forgetPhoto(section: SiteHomeSection): void {
    if (section.imageKey) this.selectFile('homeImage_' + section.imageKey, null);
  }

  private restore(area: ContentArea): void {
    if (!this.draft || !this.published) return;
    Object.assign(this.draft, structuredClone(sectionValues(this.published, area)));
    if (area === 'brand') {
      this.draft.logoUrl = this.published.logoUrl;
      this.clearLogo = false;
      this.selectFile('logoImage', null);
    }
    if (area === 'home') {
      for (const key of this.uploads.keys()) {
        if (key.startsWith('homeImage_')) this.selectFile(key, null);
      }
    }
  }

  private selectFile(key: string, file: File | null): void {
    const previous = this.uploads.get(key);
    if (previous) URL.revokeObjectURL(previous.preview);
    this.uploads.delete(key);
    if (file) this.uploads.set(key, { file, preview: URL.createObjectURL(file) });
  }

  ngOnDestroy(): void {
    for (const upload of this.uploads.values()) URL.revokeObjectURL(upload.preview);
  }
}
