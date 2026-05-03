import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router, NavigationEnd } from '@angular/router';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
  phone: string;
}

export interface AdminRegisterRequest extends RegisterRequest {
  adminSecretKey: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = '/api/auth';
  private currentUserSubject = new BehaviorSubject<AuthResponse | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  // Idle timeout configuration (15 minutes)
  private readonly IDLE_TIMEOUT_MS = 15 * 60 * 1000;
  private idleTimer: any = null;
  private readonly ACTIVITY_EVENTS = ['mousemove', 'mousedown', 'keypress', 'scroll', 'touchstart', 'click'];

  constructor(private http: HttpClient, private router: Router, private ngZone: NgZone) {
    // Clear old storage types to ensure clean migration to cookies
    localStorage.removeItem('currentUser');
    localStorage.removeItem('token');
    sessionStorage.removeItem('currentUser');
    sessionStorage.removeItem('token');

    const stored = this.getCookie('currentUser');
    if (stored) {
      this.currentUserSubject.next(JSON.parse(stored));
      this.startIdleTimer();
    }

    // Global listener: If user navigates to a public entry page, kill the session.
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        const url = event.urlAfterRedirects;
        if (url === '/home' || url === '/' || url === '/user/login' || url === '/admin/login') {
          this.clearAndLogout();
        }
      }
    });
  }

  registerUser(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request);
  }

  registerAdmin(request: AdminRegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register-admin`, request);
  }

  loginUser(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/user/login`, request).pipe(
      tap(response => this.storeUser(response))
    );
  }

  loginAdmin(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/admin/login`, request).pipe(
      tap(response => this.storeUser(response))
    );
  }

  private storeUser(response: AuthResponse): void {
    // Setting cookies without "Expires" makes them "Session Cookies"
    // They are shared across tabs but deleted when the browser closes.
    this.setCookie('currentUser', JSON.stringify(response));
    this.setCookie('token', response.token);
    this.currentUserSubject.next(response);
    this.startIdleTimer();
  }

  logout(): void {
    this.deleteCookie('currentUser');
    this.deleteCookie('token');
    this.currentUserSubject.next(null);
    this.stopIdleTimer();
  }

  clearAndLogout(): void {
    if (this.isLoggedIn()) {
      this.logout();
    }
  }

  getToken(): string | null {
    return this.getCookie('token');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'ROLE_ADMIN';
  }

  // ── Cookie Helpers ────────────────────────────────────────────────────────

  private setCookie(name: string, value: string): void {
    // No "Expires" or "Max-Age" means it's a session cookie
    document.cookie = `${name}=${encodeURIComponent(value)}; path=/; SameSite=Lax`;
  }

  private getCookie(name: string): string | null {
    const nameLenPlus = (name.length + 1);
    return document.cookie
      .split(';')
      .map(c => c.trim())
      .filter(cookie => {
        return cookie.substring(0, nameLenPlus) === `${name}=`;
      })
      .map(cookie => {
        return decodeURIComponent(cookie.substring(nameLenPlus));
      })[0] || null;
  }

  private deleteCookie(name: string): void {
    document.cookie = `${name}=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;`;
  }

  // ── Idle Timeout ──────────────────────────────────────────────────────────

  private startIdleTimer(): void {
    this.stopIdleTimer();
    this.ngZone.runOutsideAngular(() => {
      this.ACTIVITY_EVENTS.forEach(event => {
        window.addEventListener(event, this.resetIdleTimer, { passive: true });
      });
    });
    this.resetIdleTimer();
  }

  private stopIdleTimer(): void {
    if (this.idleTimer) {
      clearTimeout(this.idleTimer);
      this.idleTimer = null;
    }
    this.ACTIVITY_EVENTS.forEach(event => {
      window.removeEventListener(event, this.resetIdleTimer);
    });
  }

  private resetIdleTimer = (): void => {
    if (this.idleTimer) {
      clearTimeout(this.idleTimer);
    }
    this.idleTimer = setTimeout(() => {
      this.ngZone.run(() => {
        if (this.isLoggedIn()) {
          this.logout();
          this.router.navigate(['/home']);
          alert('You have been logged out due to inactivity.');
        }
      });
    }, this.IDLE_TIMEOUT_MS);
  };
}
