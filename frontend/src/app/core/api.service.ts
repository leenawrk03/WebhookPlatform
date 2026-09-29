import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DeliveryAttempt,
  Destination,
  DestinationRequest,
  EventDetail,
  Page,
  Stats,
  WebhookEvent,
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  stats(): Observable<Stats> {
    return this.http.get<Stats>('/api/stats');
  }

  events(page = 0, size = 20, source?: string): Observable<Page<WebhookEvent>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (source) {
      params = params.set('source', source);
    }
    return this.http.get<Page<WebhookEvent>>('/api/events', { params });
  }

  event(id: number): Observable<EventDetail> {
    return this.http.get<EventDetail>(`/api/events/${id}`);
  }

  deliveries(page = 0, size = 10): Observable<Page<DeliveryAttempt>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<DeliveryAttempt>>('/api/deliveries', { params });
  }

  destinations(): Observable<Destination[]> {
    return this.http.get<Destination[]>('/api/destinations');
  }

  createDestination(body: DestinationRequest): Observable<Destination> {
    return this.http.post<Destination>('/api/destinations', body);
  }

  updateDestination(id: number, body: DestinationRequest): Observable<Destination> {
    return this.http.put<Destination>(`/api/destinations/${id}`, body);
  }

  deleteDestination(id: number): Observable<void> {
    return this.http.delete<void>(`/api/destinations/${id}`);
  }
}
