import { getApiKey } from './session'
import type {
  Destination,
  Endpoint,
  EventDetail,
  EventSummary,
  Page,
} from './types'

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const key = getApiKey()
  const headers = new Headers(init.headers)
  if (key) {
    headers.set('X-Api-Key', key)
  }
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(path, { ...init, headers })
  if (!response.ok) {
    throw new ApiError(response.status, await errorMessage(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const payload = await response.json()
    return payload.message ?? payload.error ?? response.statusText
  } catch {
    return response.statusText
  }
}

export const api = {
  listEndpoints: () => request<Endpoint[]>('/api/endpoints'),

  getEndpoint: (id: string) => request<Endpoint>(`/api/endpoints/${id}`),

  createEndpoint: (name: string) =>
    request<Endpoint>('/api/endpoints', {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),

  setEndpointActive: (id: string, active: boolean) =>
    request<Endpoint>(`/api/endpoints/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ active }),
    }),

  deleteEndpoint: (id: string) =>
    request<void>(`/api/endpoints/${id}`, { method: 'DELETE' }),

  listDestinations: (endpointId: string) =>
    request<Destination[]>(`/api/endpoints/${endpointId}/destinations`),

  createDestination: (
    endpointId: string,
    body: { name: string; targetUrl: string; filterExpr?: string | null },
  ) =>
    request<Destination>(`/api/endpoints/${endpointId}/destinations`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  updateDestination: (
    id: string,
    body: { name: string; targetUrl: string; filterExpr?: string | null; active: boolean },
  ) =>
    request<Destination>(`/api/destinations/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    }),

  deleteDestination: (id: string) =>
    request<void>(`/api/destinations/${id}`, { method: 'DELETE' }),

  listEvents: (endpointId: string, page = 0, size = 25) =>
    request<Page<EventSummary>>(
      `/api/endpoints/${endpointId}/events?page=${page}&size=${size}`,
    ),

  getEvent: (id: string) => request<EventDetail>(`/api/events/${id}`),

  replayEvent: (id: string) =>
    request<Record<string, unknown>>(`/api/events/${id}/replay`, {
      method: 'POST',
    }),
}
