import { beforeEach, describe, expect, it, vi } from 'vitest'

const lifecycle = vi.hoisted(() => ({ unmount: undefined as (() => void) | undefined }))
const stomp = vi.hoisted(() => ({ instances: [] as any[] }))

vi.mock('vue', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue')>()
  return {
    ...actual,
    onBeforeUnmount: vi.fn((callback: () => void) => {
      lifecycle.unmount = callback
    }),
  }
})

vi.mock('@stomp/stompjs', () => ({
  Client: class {
    activate = vi.fn()
    deactivate = vi.fn()
    subscribe = vi.fn()

    constructor(readonly config: Record<string, (...args: any[]) => void>) {
      stomp.instances.push(this)
    }
  },
}))

vi.mock('sockjs-client', () => ({ default: vi.fn(() => ({ transport: 'sockjs' })) }))

import { useEventStream } from './live'

describe('useEventStream', () => {
  beforeEach(() => {
    stomp.instances.length = 0
    lifecycle.unmount = undefined
  })

  it('returns to connecting after a dropped socket and resubscribes after reconnecting', () => {
    const onEvent = vi.fn()
    const { state } = useEventStream(() => 'endpoint-7', onEvent)
    const client = stomp.instances[0]

    expect(client.activate).toHaveBeenCalledOnce()
    expect(state.value).toBe('connecting')

    client.config.onConnect()
    expect(state.value).toBe('live')
    expect(client.subscribe).toHaveBeenCalledWith(
      '/topic/endpoints/endpoint-7/events',
      expect.any(Function),
    )

    client.config.onWebSocketClose()
    expect(state.value).toBe('connecting')

    client.config.onConnect()
    expect(state.value).toBe('live')
    expect(client.subscribe).toHaveBeenCalledTimes(2)

    const handler = client.subscribe.mock.calls[1][1]
    handler({ body: JSON.stringify({ id: 'event-9', method: 'POST' }) })
    expect(onEvent).toHaveBeenCalledWith({ id: 'event-9', method: 'POST' })
  })

  it('ignores late socket callbacks after unmounting', () => {
    const onEvent = vi.fn()
    const { state } = useEventStream(() => 'endpoint-7', onEvent)
    const client = stomp.instances[0]

    client.config.onConnect()
    const handler = client.subscribe.mock.calls[0][1]

    lifecycle.unmount?.()
    expect(client.deactivate).toHaveBeenCalledOnce()
    expect(state.value).toBe('closed')

    client.config.onWebSocketClose()
    client.config.onConnect()
    handler({ body: JSON.stringify({ id: 'late-event' }) })
    expect(state.value).toBe('closed')
    expect(client.subscribe).toHaveBeenCalledOnce()
    expect(onEvent).not.toHaveBeenCalled()
  })
})
