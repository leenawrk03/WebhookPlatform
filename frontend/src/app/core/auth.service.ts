import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

const TOKEN_KEY = 'whp.token';
const USER_KEY = 'whp.user';

interface LoginResponse {
  token: string;
  username: string;
  expiresInSeconds: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly username = signal<string>(localStorage.getItem(USER_KEY) ?? '');

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/auth/login', { username, password })
      .pipe(
        tap((res) => {
          localStorage.setItem(TOKEN_KEY, res.token);
          localStorage.setItem(USER_KEY, res.username);
          this.token.set(res.token);
          this.username.set(res.username);
        }),
      );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.token.set(null);
    this.username.set('');
  }

  getToken(): string | null {
    return this.token();
  }

  isLoggedIn(): boolean {
    return this.token() !== null;
  }
}
