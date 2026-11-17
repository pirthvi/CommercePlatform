import { provideStoreInterface } from '@commerce/store';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { AppComponent } from './app/app-root/app.component';
import { adminAuthInterceptor } from './app/admin-auth.interceptor';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { studioRoutes } from './app/studio.routes';

bootstrapApplication(AppComponent, {
  providers: [
    provideZoneChangeDetection(),
    provideHttpClient(withInterceptors([adminAuthInterceptor])),
    provideStoreInterface('studio'),
    provideRouter(studioRoutes, withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })),
  ],
}).catch(err => console.error(err));
