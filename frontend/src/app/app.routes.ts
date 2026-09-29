import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'events',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/events/events.component').then((m) => m.EventsComponent),
  },
  {
    path: 'events/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/events/event-detail.component').then((m) => m.EventDetailComponent),
  },
  {
    path: 'destinations',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/destinations/destinations.component').then((m) => m.DestinationsComponent),
  },
  { path: '**', redirectTo: 'dashboard' },
];
