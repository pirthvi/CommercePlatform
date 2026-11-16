import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { CustomerAuthService } from './customer-auth.service';

export const customerJwtInterceptor: HttpInterceptorFn = (request, next) => {
  const usesCustomerIdentity = /^\/api\/(?:cart(?:[/?]|$)|account\/|auth\/me(?:[/?]|$))/.test(request.url)
    || (request.method === 'POST' && request.url === '/api/orders');
  if (!usesCustomerIdentity) return next(request);
  const auth = inject(CustomerAuthService);
  const token = auth.accessToken;
  const authorizedRequest = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authorizedRequest).pipe(catchError((error: unknown) => {
    if (token && error instanceof HttpErrorResponse && error.status === 401) auth.signOut();
    return throwError(() => error);
  }));
};
