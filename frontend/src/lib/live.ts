import { onBeforeUnmount, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { EventSummary } from '@/api/types'

export type LiveState = 'connecting' | 'live' | 'closed'

/**
 * Subscribes to the per-endpoint event stream over STOMP/SockJS and invokes
 * {@link onEvent} for every event captured after the connection opens. The
 * connection is torn down automatically when the owning component unmounts.
 */
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
