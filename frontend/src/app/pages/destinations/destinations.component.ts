import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ApiService } from '../../core/api.service';
import { Destination, DestinationRequest } from '../../core/models';

@Component({
  selector: 'app-destinations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="row" style="justify-content:space-between">
      <h2 style="margin:0">Destinations</h2>
      <button (click)="showForm.set(!showForm())">{{ showForm() ? 'Cancel' : '+ Add destination' }}</button>
    </div>

    <form class="card" style="margin-top:16px" *ngIf="showForm()" (ngSubmit)="save()">
      <div class="row">
        <input name="name" placeholder="Name (e.g. Stripe Payments)" [(ngModel)]="form.name" required />
        <input name="source" placeholder="Source (stripe, github...)" [(ngModel)]="form.source" required />
      </div>
      <div class="row" style="margin-top:10px">
        <input name="targetUrl" style="flex:1" placeholder="https://myapp.com/webhooks"
               [(ngModel)]="form.targetUrl" required />
        <input name="secret" placeholder="Signing secret (optional)" [(ngModel)]="form.secret" />
      </div>
      <div class="row" style="margin-top:10px">
        <label class="muted"><input type="checkbox" name="active" [(ngModel)]="form.active" /> Active</label>
        <button type="submit">{{ editingId() ? 'Update' : 'Create' }}</button>
      </div>
      <p class="error" *ngIf="error()">{{ error() }}</p>
    </form>

    <div class="card" style="margin-top:18px">
      <table>
        <thead>
          <tr><th>Name</th><th>Source</th><th>Target URL</th><th>Status</th><th>Created</th><th></th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let d of destinations()">
            <td>{{ d.name }}</td>
            <td>{{ d.source }}</td>
            <td class="muted">{{ d.targetUrl }}</td>
            <td><span class="badge" [class.active]="d.active">{{ d.active ? 'Active' : 'Paused' }}</span></td>
            <td class="muted">{{ d.createdAt | date: 'short' }}</td>
            <td class="row">
              <a href="javascript:void(0)" (click)="edit(d)">Edit</a>
              <a href="javascript:void(0)" (click)="remove(d)">Delete</a>
            </td>
          </tr>
          <tr *ngIf="!destinations().length">
            <td colspan="6" class="muted">No destinations registered yet.</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
})
export class DestinationsComponent {
  private readonly api = inject(ApiService);

  readonly destinations = signal<Destination[]>([]);
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly error = signal('');

  form: DestinationRequest = this.empty();

  constructor() {
    this.load();
  }

  load(): void {
    this.api.destinations().subscribe((list) => this.destinations.set(list));
  }

  edit(destination: Destination): void {
    this.editingId.set(destination.id);
    this.form = {
      name: destination.name,
      source: destination.source,
      targetUrl: destination.targetUrl,
      secret: '',
      active: destination.active,
    };
    this.showForm.set(true);
  }

  save(): void {
    this.error.set('');
    const id = this.editingId();
    const request = id
      ? this.api.updateDestination(id, this.form)
      : this.api.createDestination(this.form);

    request.subscribe({
      next: () => {
        this.form = this.empty();
        this.editingId.set(null);
        this.showForm.set(false);
        this.load();
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Could not save the destination.'),
    });
  }

  remove(destination: Destination): void {
    if (!confirm(`Delete "${destination.name}"?`)) return;
    this.api.deleteDestination(destination.id).subscribe(() => this.load());
  }

  private empty(): DestinationRequest {
    return { name: '', source: '', targetUrl: '', secret: '', active: true };
  }
}
