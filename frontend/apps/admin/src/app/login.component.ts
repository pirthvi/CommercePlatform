import { StoreInterface } from '@commerce/store';
import { PageLayout } from '@commerce/layout';
import { HttpClient } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of } from 'rxjs';
import { storefrontUrl } from './studio-navigation';
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from './auth.service';

@Component({ selector: 'admin-login', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './login.component.html',
  styleUrl: './login.component.css' })
export class LoginComponent {
  readonly ui = inject(StoreInterface);
  readonly shopUrl = storefrontUrl();
  readonly signInPhoto = toSignal(inject(HttpClient).get<PageLayout>('/api/content/layouts/template/login').pipe(
    map(page => page.sections.find(block => block.type === 'customer-form')?.imageUrl),
    catchError(() => of(undefined)),
  ));
  private readonly auth = inject(AuthService);
  username = '';
  password = '';
  busy = false;
  error = '';
  submit(): void {
    if (this.busy || !this.username || !this.password) return;
    this.busy = true; this.error = '';
    this.auth.signIn(this.username, this.password).subscribe((valid) => {
      this.busy = false;
      if (!valid) this.error = this.auth.lastSignInStatus === 0
        ? this.ui.text('login.theStudioCouldNotReachTheBackendCheck')
        : this.auth.lastSignInStatus === 401
          ? this.ui.text('login.checkTheStudioUsernameAndPasswordAndTry')
          : this.ui.text('login.thoseDetailsDidNotOpenTheStudioPlease');
    });
  }
}
