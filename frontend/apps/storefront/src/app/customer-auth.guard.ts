import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { CustomerAuthService } from './customer-auth.service';

export const customerAuthGuard: CanActivateFn = (_route, state) => {
  const auth = inject(CustomerAuthService);
  const router = inject(Router);
  return auth.isAuthenticated ? true : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
