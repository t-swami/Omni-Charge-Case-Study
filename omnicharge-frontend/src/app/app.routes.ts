import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  {
    path: 'home',
    loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'user/login',
    loadComponent: () => import('./pages/user-login/user-login.component').then(m => m.UserLoginComponent)
  },
  {
    path: 'user/register',
    loadComponent: () => import('./pages/user-register/user-register.component').then(m => m.UserRegisterComponent)
  },
  {
    path: 'admin/login',
    loadComponent: () => import('./pages/admin-login/admin-login.component').then(m => m.AdminLoginComponent)
  },
  {
    path: 'admin/register',
    loadComponent: () => import('./pages/admin-register/admin-register.component').then(m => m.AdminRegisterComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/user-dashboard/user-dashboard.component').then(m => m.UserDashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'recharge',
    loadComponent: () => import('./pages/recharge/recharge.component').then(m => m.RechargeComponent),
    canActivate: [authGuard]
  },
  {
    path: 'payment/:rechargeId',
    loadComponent: () => import('./pages/payment/payment.component').then(m => m.PaymentComponent),
    canActivate: [authGuard]
  },
  {
    path: 'history',
    loadComponent: () => import('./pages/history/history.component').then(m => m.HistoryComponent),
    canActivate: [authGuard]
  },
  {
    path: 'profile',
    loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [authGuard]
  },
  {
    path: 'admin/dashboard',
    loadComponent: () => import('./pages/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent),
    canActivate: [adminGuard]
  },
  { path: '**', redirectTo: '/home' }
];
