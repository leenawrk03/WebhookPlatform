import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell" *ngIf="auth.isLoggedIn(); else plain">
      <aside class="sidebar">
        <h1>Webhook Platform</h1>
        <nav>
          <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/events" routerLinkActive="active">Events</a>
          <a routerLink="/destinations" routerLinkActive="active">Destinations</a>
        </nav>
        <p class="muted" style="margin-top:24px">Signed in as {{ auth.username() }}</p>
        <button class="ghost" (click)="logout()">Log out</button>
      </aside>
      <main class="content"><router-outlet /></main>
    </div>
    <ng-template #plain><router-outlet /></ng-template>
  `,
})
export class AppComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
