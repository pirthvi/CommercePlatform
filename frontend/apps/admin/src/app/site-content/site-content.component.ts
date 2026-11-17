import { StoreInterface } from '@commerce/store';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AsyncPipe } from '@angular/common';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AdminApiService } from '../admin-api.service';
import { ContentFooterComponent } from '../content-footer/content-footer.component';
import { ContentImagePickerComponent } from '../content-image-picker/content-image-picker.component';
import { ContentLinksComponent } from '../content-links/content-links.component';
import { HomeEditorComponent } from '../home-editor/home-editor.component';
import { StudioContentDrafts } from '../studio-content-drafts';
import { ContentArea, editorLabels, storefrontPagePath, storefrontUrl } from '../studio-navigation';

@Component({
  selector: 'admin-site-content',
  standalone: true,
  imports: [FormsModule, AsyncPipe, RouterLink, RouterLinkActive, ContentImagePickerComponent, ContentLinksComponent, ContentFooterComponent, HomeEditorComponent],
  templateUrl: './site-content.component.html',
  styleUrl: './site-content.component.css',
})
export class SiteContentComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  readonly drafts = inject(StudioContentDrafts);
  readonly area: ContentArea = inject(ActivatedRoute).snapshot.data['editor'];
  get title(): string { return this.ui.text(editorLabels[this.area]); }
  readonly shopUrl = storefrontUrl();
  readonly categories$ = inject(AdminApiService).categories();
  readonly destinations$ = inject(AdminApiService).pages().pipe(
    map(pages => pages.filter(page => page.published && page.slug !== 'order-success' && page.templateKey !== 'product')
      .map(page => storefrontPagePath(page.slug))),
    catchError(() => of([] as string[])),
  );

  ngOnInit(): void { this.drafts.load(); }

  save(event: Event): void {
    event.preventDefault();
    this.drafts.save(this.area);
  }
}
