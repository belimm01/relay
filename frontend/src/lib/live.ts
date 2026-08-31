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

  function connect() {
    const id = endpointId()
    client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 3000,
      onConnect: () => {
        state.value = 'live'
        client?.subscribe(`/topic/endpoints/${id}/events`, (message) => {
          onEvent(JSON.parse(message.body) as EventSummary)
        })
      },
      onWebSocketClose: () => {
        state.value = 'connecting'
      },
    })
    client.activate()
  }

  function disconnect() {
    state.value = 'closed'
    client?.deactivate()
    client = null
  }

  connect()
  onBeforeUnmount(disconnect)

  return { state }
}
