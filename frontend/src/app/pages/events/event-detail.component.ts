import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { EventDetail } from '../../core/models';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <a routerLink="/events">&larr; Back to events</a>
    <ng-container *ngIf="event() as e">
      <h2>{{ e.eventType }}</h2>
      <p class="muted">{{ e.eventId }} · {{ e.source }} · {{ e.receivedAt | date: 'medium' }}</p>
      <span class="badge {{ e.status.toLowerCase() }}">{{ e.status }}</span>

      <div class="card" style="margin-top:18px">
        <div class="label">Delivery attempts</div>
        <table>
          <thead>
            <tr><th>#</th><th>Destination</th><th>Status</th><th>HTTP</th><th>Duration</th><th>Error</th><th>When</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let a of e.attempts">
              <td>{{ a.attemptNumber }}</td>
              <td>{{ a.destinationName }}</td>
              <td><span class="badge {{ a.status.toLowerCase() }}">{{ a.status }}</span></td>
              <td>{{ a.responseStatus ?? '—' }}</td>
              <td>{{ a.durationMs ?? '—' }} ms</td>
              <td class="muted">{{ a.errorMessage ?? '—' }}</td>
              <td class="muted">{{ a.attemptedAt | date: 'short' }}</td>
            </tr>
            <tr *ngIf="!e.attempts.length"><td colspan="7" class="muted">No attempts recorded.</td></tr>
          </tbody>
        </table>
      </div>

      <div class="card" style="margin-top:18px">
        <div class="label">Payload</div>
        <pre>{{ e.payload }}</pre>
      </div>

      <div class="card" style="margin-top:18px">
        <div class="label">Headers</div>
        <pre>{{ e.headers }}</pre>
      </div>
    </ng-container>
  `,
})
export class EventDetailComponent {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  readonly event = signal<EventDetail | null>(null);

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.event(id).subscribe((e) => this.event.set(e));
  }
}
