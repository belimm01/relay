export type DeliveryStatus = 'PENDING' | 'RETRYING' | 'DELIVERED' | 'FAILED'

export interface Endpoint {
  id: string
  name: string
  ingestToken: string
  ingestPath: string
  active: boolean
  createdAt: string
}

export interface Destination {
  id: string
  endpointId: string
  name: string
  targetUrl: string
  filterExpr: string | null
  active: boolean
  createdAt: string
}

export interface EventSummary {
  id: string
  method: string
  path: string
  contentType: string | null
  sourceIp: string | null
  receivedAt: string
}

export interface Attempt {
  attemptNo: number
  succeeded: boolean
  responseCode: number | null
  responseBody: string | null
  latencyMs: number | null
  errorMessage: string | null
  attemptedAt: string
}

export interface Delivery {
  id: string
  destinationId: string
  destinationName: string
  targetUrl: string
  status: DeliveryStatus
  attemptCount: number
  nextAttemptAt: string | null
  lastResponseCode: number | null
  lastError: string | null
  createdAt: string
  updatedAt: string
  attempts: Attempt[]
}

export interface EventDetail {
  id: string
  endpointId: string
  method: string
  path: string
  queryString: string | null
  headers: Record<string, string>
  body: string | null
  contentType: string | null
  sourceIp: string | null
  receivedAt: string
  deliveries: Delivery[]
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}
