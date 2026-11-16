import { StoreInterface } from '@commerce/store';
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from './auth.service';
import { LoginComponent } from './login.component';
import { AdminShellComponent } from './shell.component';

@Component({ selector: 'admin-root', standalone: true, imports: [CommonModule, LoginComponent, AdminShellComponent], templateUrl: './app.component.html',
  styleUrl: './app.component.css' })
export class AppComponent implements OnInit {
  readonly ui = inject(StoreInterface);
  readonly auth = inject(AuthService);
  checking = !!this.auth.authorization;
  ngOnInit(): void {
    if (!this.checking) return;
    this.auth.verify().subscribe(() => this.checking = false);
  }
}
