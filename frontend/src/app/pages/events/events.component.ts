import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { WebhookEvent } from '../../core/models';

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <h2>Events</h2>
    <div class="row">
      <input placeholder="Filter by source (stripe, github...)" [(ngModel)]="source" />
      <button (click)="load(0)">Apply</button>
      <button class="ghost" (click)="source = ''; load(0)">Clear</button>
    </div>

    <div class="card" style="margin-top:18px">
      <table>
        <thead>
          <tr><th>ID</th><th>Source</th><th>Type</th><th>Status</th><th>Received</th><th></th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let e of events()">
            <td class="muted">{{ e.eventId }}</td>
            <td>{{ e.source }}</td>
            <td>{{ e.eventType }}</td>
            <td><span class="badge {{ e.status.toLowerCase() }}">{{ e.status }}</span></td>
            <td class="muted">{{ e.receivedAt | date: 'short' }}</td>
            <td><a [routerLink]="['/events', e.id]">Details</a></td>
          </tr>
          <tr *ngIf="!events().length"><td colspan="6" class="muted">No events yet.</td></tr>
        </tbody>
      </table>

      <div class="row" style="margin-top:14px">
        <button class="ghost" [disabled]="page() === 0" (click)="load(page() - 1)">Previous</button>
        <span class="muted">Page {{ page() + 1 }} of {{ totalPages() || 1 }}</span>
        <button class="ghost" [disabled]="page() + 1 >= totalPages()" (click)="load(page() + 1)">Next</button>
      </div>
    </div>
  `,
})
export class EventsComponent {
  private readonly api = inject(ApiService);

  source = '';
  readonly events = signal<WebhookEvent[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);

  constructor() {
    this.load(0);
  }

  load(page: number): void {
    this.api.events(page, 20, this.source.trim() || undefined).subscribe((res) => {
      this.events.set(res.content);
      this.page.set(res.number);
      this.totalPages.set(res.totalPages);
    });
  }
}
