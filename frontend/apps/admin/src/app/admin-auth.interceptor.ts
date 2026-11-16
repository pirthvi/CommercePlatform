import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const adminAuthInterceptor: HttpInterceptorFn = (request, next) => {
  const isLoginRequest = request.method === 'POST' && request.url.endsWith('/api/admin/session');
  if (!request.url.startsWith('/api/admin/') || isLoginRequest) return next(request);
  const auth = inject(AuthService);
  const authorization = auth.authorization;
  const authorizedRequest = authorization
    ? request.clone({ setHeaders: { Authorization: authorization } })
    : request;
  return next(authorizedRequest).pipe(catchError((error: unknown) => {
    if (error instanceof HttpErrorResponse && error.status === 401) auth.signOut();
    return throwError(() => error);
  }));
};
