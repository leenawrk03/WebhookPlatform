export interface Stats {
  eventsReceived: number;
  successfulDeliveries: number;
  failedDeliveries: number;
  retries: number;
  successRate: number;
  activeDestinations: number;
}

export interface Destination {
  id: number;
  name: string;
  targetUrl: string;
  source: string;
  active: boolean;
  createdAt: string;
}

export interface DestinationRequest {
  name: string;
  targetUrl: string;
  source: string;
  secret?: string;
  active?: boolean;
}

export interface WebhookEvent {
  id: number;
  eventId: string;
  source: string;
  eventType: string;
  status: string;
  receivedAt: string;
}

export interface DeliveryAttempt {
  id: number;
  eventId: number;
  destinationId: number;
  destinationName: string;
  attemptNumber: number;
  status: string;
  responseStatus: number | null;
  errorMessage: string | null;
  durationMs: number | null;
  attemptedAt: string;
}

export interface EventDetail extends WebhookEvent {
  payload: string;
  headers: string;
  attempts: DeliveryAttempt[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}
