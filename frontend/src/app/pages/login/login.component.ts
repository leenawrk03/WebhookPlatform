import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-wrap">
      <form class="card login-card" (ngSubmit)="submit()">
        <h2 style="margin:0">Webhook Platform</h2>
        <p class="muted" style="margin:0">Sign in to manage destinations and deliveries.</p>
        <input name="username" placeholder="Username" [(ngModel)]="username" autocomplete="username" />
        <input name="password" type="password" placeholder="Password" [(ngModel)]="password"
               autocomplete="current-password" />
        <button type="submit" [disabled]="loading()">{{ loading() ? 'Signing in...' : 'Sign in' }}</button>
        <p class="error" *ngIf="error()">{{ error() }}</p>
      </form>
    </div>
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  username = 'admin';
  password = '';
  readonly loading = signal(false);
  readonly error = signal('');

  submit(): void {
    this.error.set('');
    this.loading.set(true);
    this.auth.login(this.username, this.password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: () => {
        this.error.set('Invalid username or password.');
        this.loading.set(false);
      },
    });
  }
}
