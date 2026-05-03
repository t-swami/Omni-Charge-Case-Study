import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Read token from cookies
  const token = document.cookie
    .split('; ')
    .find(row => row.startsWith('token='))
    ?.split('=')[1];

  if (token) {
    const decodedToken = decodeURIComponent(token);
    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${decodedToken}` }
    });
    return next(cloned);
  }
  return next(req);
};
