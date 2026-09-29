import {
  AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, inject, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription, interval, startWith, switchMap } from 'rxjs';
import Chart from 'chart.js/auto';

import { ApiService } from '../../core/api.service';
import { DeliveryAttempt, Stats } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="row" style="justify-content:space-between">
      <h2 style="margin:0">Dashboard</h2>
      <span class="muted">Auto-refreshing every 10s</span>
    </div>

    <div class="cards" style="margin-top:18px">
      <div class="card"><div class="label">Events received</div>
        <div class="value">{{ stats()?.eventsReceived ?? 0 | number }}</div></div>
      <div class="card"><div class="label">Successful deliveries</div>
        <div class="value">{{ stats()?.successfulDeliveries ?? 0 | number }}</div></div>
      <div class="card"><div class="label">Failed deliveries</div>
        <div class="value">{{ stats()?.failedDeliveries ?? 0 | number }}</div></div>
      <div class="card"><div class="label">Retries</div>
        <div class="value">{{ stats()?.retries ?? 0 | number }}</div></div>
      <div class="card"><div class="label">Success rate</div>
        <div class="value">{{ stats()?.successRate ?? 0 }}%</div></div>
      <div class="card"><div class="label">Active destinations</div>
        <div class="value">{{ stats()?.activeDestinations ?? 0 }}</div></div>
    </div>

    <div class="card" style="margin-top:22px">
      <div class="label">Delivery outcomes</div>
      <canvas #chart height="90"></canvas>
    </div>

    <div class="card" style="margin-top:22px">
      <div class="row" style="justify-content:space-between">
        <div class="label">Recent delivery attempts</div>
        <a routerLink="/events">View events</a>
      </div>
      <table>
        <thead>
          <tr><th>Event</th><th>Destination</th><th>Attempt</th><th>Status</th><th>HTTP</th><th>Duration</th><th>When</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let d of deliveries()">
            <td><a [routerLink]="['/events', d.eventId]">#{{ d.eventId }}</a></td>
            <td>{{ d.destinationName }}</td>
            <td>{{ d.attemptNumber }}</td>
            <td><span class="badge {{ d.status.toLowerCase() }}">{{ d.status }}</span></td>
            <td>{{ d.responseStatus ?? '—' }}</td>
            <td>{{ d.durationMs ?? '—' }} ms</td>
            <td class="muted">{{ d.attemptedAt | date: 'short' }}</td>
          </tr>
          <tr *ngIf="!deliveries().length"><td colspan="7" class="muted">No deliveries yet.</td></tr>
        </tbody>
      </table>
    </div>
  `,
})
export class DashboardComponent implements AfterViewInit, OnDestroy {
  private readonly api = inject(ApiService);

  readonly stats = signal<Stats | null>(null);
  readonly deliveries = signal<DeliveryAttempt[]>([]);

  @ViewChild('chart') chartRef?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart;
  private sub?: Subscription;

  ngAfterViewInit(): void {
    this.sub = interval(10_000)
      .pipe(startWith(0), switchMap(() => this.api.stats()))
      .subscribe((stats) => {
        this.stats.set(stats);
        this.renderChart(stats);
      });

    this.api.deliveries(0, 10).subscribe((page) => this.deliveries.set(page.content));
  }

  private renderChart(stats: Stats): void {
    const data = [stats.successfulDeliveries, stats.failedDeliveries, stats.retries];
    if (this.chart) {
      this.chart.data.datasets[0].data = data;
      this.chart.update();
      return;
    }
    if (!this.chartRef) return;
    this.chart = new Chart(this.chartRef.nativeElement, {
      type: 'bar',
      data: {
        labels: ['Successful', 'Failed', 'Retries'],
        datasets: [
          {
            label: 'Deliveries',
            data,
            backgroundColor: ['#34d399', '#f87171', '#fbbf24'],
            borderRadius: 6,
          },
        ],
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } },
        scales: {
          x: { ticks: { color: '#94a3b8' }, grid: { display: false } },
          y: { ticks: { color: '#94a3b8' }, grid: { color: '#253048' } },
        },
      },
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.chart?.destroy();
  }
}
