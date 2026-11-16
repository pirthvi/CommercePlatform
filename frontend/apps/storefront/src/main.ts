import { provideStoreInterface } from '@commerce/store';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { customerJwtInterceptor } from './app/customer-jwt.interceptor';

bootstrapApplication(AppComponent, { providers: [provideZoneChangeDetection(), provideHttpClient(withInterceptors([customerJwtInterceptor])), provideStoreInterface('storefront'), provideRouter(routes)] }).catch((err) => console.error(err));
