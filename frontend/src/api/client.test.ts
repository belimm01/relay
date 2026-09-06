import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./session', () => ({ getApiKey: () => 'rk_test' }))

import { api } from './client'

describe('API client failures', () => {
  beforeEach(() => {
    vi.unstubAllGlobals()
  })

  it('surfaces the API error message and preserves authentication', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ message: 'Database unavailable' }), {
        status: 503,
        statusText: 'Service Unavailable',
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.listEndpoints()).rejects.toEqual(
      expect.objectContaining({
        name: 'ApiError',
        status: 503,
        message: 'Database unavailable',
      }),
    )

    const [, init] = fetchMock.mock.calls[0]
    expect(init.headers).toBeInstanceOf(Headers)
    expect(init.headers.get('X-Api-Key')).toBe('rk_test')
  })

  it('falls back to the HTTP status text for a non-JSON failure', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('upstream reset', {
          status: 502,
          statusText: 'Bad Gateway',
          headers: { 'Content-Type': 'text/plain' },
        }),
      ),
    )

    await expect(api.getOverview()).rejects.toEqual(
      expect.objectContaining({
        status: 502,
        message: 'Bad Gateway',
      }),
    )
  })

  it('propagates a network failure without converting it to an HTTP error', async () => {
    const failure = new TypeError('fetch failed')
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(failure))

    await expect(api.getOverview()).rejects.toBe(failure)
  })
})
