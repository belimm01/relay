import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from './client'
import { clearApiKey, setApiKey } from './session'

describe('API client session and mutation contracts', () => {
  beforeEach(() => {
    const values = new Map<string, string>()
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => values.get(key) ?? null,
      setItem: (key: string, value: string) => values.set(key, value),
      removeItem: (key: string) => values.delete(key),
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('reads the current session for every request and omits authentication after sign-out', async () => {
    const fetchMock = vi.fn().mockImplementation(async () => Response.json([]))
    vi.stubGlobal('fetch', fetchMock)

    await api.listEndpoints()
    setApiKey('rk_first_session')
    await api.listEndpoints()
    setApiKey('rk_replacement_session')
    await api.listEndpoints()
    clearApiKey()
    await api.listEndpoints()

    expect(fetchMock).toHaveBeenCalledTimes(4)
    expect(fetchMock.mock.calls.map(([path]) => path)).toEqual([
      '/api/endpoints', '/api/endpoints', '/api/endpoints', '/api/endpoints',
    ])
    expect(fetchMock.mock.calls.map(([, init]) => init.headers.get('X-Api-Key'))).toEqual([
      null, 'rk_first_session', 'rk_replacement_session', null,
    ])
    for (const [, init] of fetchMock.mock.calls) {
      expect(init.headers.has('Content-Type')).toBe(false)
      expect(init.body).toBeUndefined()
    }
  })

  it('serializes destination updates without dropping false or null values', async () => {
    setApiKey('rk_mutation_session')
    const id = '9fc16597-9ecb-4f01-a03b-3c5e97a636c1'
    const body = {
      name: 'Orders "EU"',
      targetUrl: 'https://receiver.example/webhook',
      filterExpr: null,
      active: false,
    }
    const destination = { id, ...body }
    const fetchMock = vi.fn().mockResolvedValue(Response.json(destination))
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.updateDestination(id, body)).resolves.toEqual(destination)

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [path, init] = fetchMock.mock.calls[0]
    expect(path).toBe(`/api/destinations/${id}`)
    expect(init.method).toBe('PATCH')
    expect(init.headers.get('Content-Type')).toBe('application/json')
    expect(init.headers.get('X-Api-Key')).toBe('rk_mutation_session')
    expect(JSON.parse(init.body)).toEqual(body)
  })

  it.each([
    ['endpoint', api.deleteEndpoint, '/api/endpoints/'],
    ['destination', api.deleteDestination, '/api/destinations/'],
  ] as const)('accepts an empty 204 response when deleting an %s', async (_, remove, prefix) => {
    setApiKey('rk_delete_session')
    const id = '9fc16597-9ecb-4f01-a03b-3c5e97a636c1'
    const response = new Response(null, { status: 204 })
    const readJson = vi.spyOn(response, 'json')
    const fetchMock = vi.fn().mockResolvedValue(response)
    vi.stubGlobal('fetch', fetchMock)

    await expect(remove(id)).resolves.toBeUndefined()

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [path, init] = fetchMock.mock.calls[0]
    expect(path).toBe(`${prefix}${id}`)
    expect(init.method).toBe('DELETE')
    expect(init.body).toBeUndefined()
    expect(init.headers.get('X-Api-Key')).toBe('rk_delete_session')
    expect(init.headers.has('Content-Type')).toBe(false)
    expect(readJson).not.toHaveBeenCalled()
  })
})
