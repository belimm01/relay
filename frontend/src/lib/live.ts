import { onBeforeUnmount, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { EventSummary } from '@/api/types'

export type LiveState = 'connecting' | 'live' | 'closed'

export function useEventStream(
  endpointId: () => string,
  onEvent: (event: EventSummary) => void,
) {
  const state = ref<LiveState>('connecting')
  let client: Client | null = null
  let stopped = false

  function connect() {
    const id = endpointId()
    client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 3000,
      onConnect: () => {
        if (stopped) return
        state.value = 'live'
        client?.subscribe(`/topic/endpoints/${id}/events`, (message) => {
          if (!stopped) onEvent(JSON.parse(message.body) as EventSummary)
        })
      },
      onWebSocketClose: () => {
        if (!stopped) state.value = 'connecting'
      },
    })
    client.activate()
  }

  function disconnect() {
    stopped = true
    state.value = 'closed'
    client?.deactivate()
    client = null
  }

  connect()
  onBeforeUnmount(disconnect)

  return { state }
}
