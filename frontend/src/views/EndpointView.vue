<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import type { Endpoint, EventSummary } from '@/api/types'
import { relativeTime } from '@/lib/format'
import { useEventStream } from '@/lib/live'
import CopyButton from '@/components/CopyButton.vue'
import DestinationsPanel from '@/components/DestinationsPanel.vue'
import EmptyState from '@/components/EmptyState.vue'

const props = defineProps<{ id: string }>()
const router = useRouter()

const endpoint = ref<Endpoint | null>(null)
const events = ref<EventSummary[]>([])
const loading = ref(true)

const ingestUrl = computed(() =>
  endpoint.value ? window.location.origin + endpoint.value.ingestPath : '',
)

async function load() {
  const [meta, page] = await Promise.all([
    api.getEndpoint(props.id),
    api.listEvents(props.id, 0, 50),
  ])
  endpoint.value = meta
  events.value = page.items
  loading.value = false
}

const { state } = useEventStream(
  () => props.id,
  (event) => {
    events.value.unshift(event)
    if (events.value.length > 100) events.value.pop()
  },
)

async function toggleActive() {
  if (!endpoint.value) return
  endpoint.value = await api.setEndpointActive(endpoint.value.id, !endpoint.value.active)
}

async function remove() {
  if (!endpoint.value) return
  if (!confirm(`Delete endpoint "${endpoint.value.name}" and all its events?`)) return
  await api.deleteEndpoint(endpoint.value.id)
  router.push({ name: 'endpoints' })
}

onMounted(load)
</script>

<template>
  <div v-if="endpoint" class="stack">
    <div>
      <RouterLink to="/" class="muted">← Endpoints</RouterLink>
      <div class="spread head">
        <h1>{{ endpoint.name }}</h1>
        <div class="row">
          <button class="ghost" @click="toggleActive">
            {{ endpoint.active ? 'Pause' : 'Resume' }}
          </button>
          <button class="ghost danger" @click="remove">Delete</button>
        </div>
      </div>
    </div>

    <div class="card">
      <label>Ingest URL — send webhooks here</label>
      <div class="row">
        <code class="mono url">{{ ingestUrl }}</code>
        <CopyButton :value="ingestUrl" />
      </div>
    </div>

    <div class="columns">
      <DestinationsPanel :endpoint-id="endpoint.id" />

      <section class="card stack">
        <div class="spread">
          <h2>Live events</h2>
          <span class="live" :class="state">
            <span class="dot" />
            {{ state === 'live' ? 'Live' : 'Connecting' }}
          </span>
        </div>

        <p v-if="loading" class="muted">Loading…</p>
        <EmptyState
          v-else-if="events.length === 0"
          title="Waiting for events"
          hint="Send a request to the ingest URL and it will appear here instantly."
        />
        <ul v-else class="feed">
          <li v-for="event in events" :key="event.id">
            <RouterLink :to="{ name: 'event', params: { id: event.id } }" class="spread">
              <span class="row">
                <span class="method">{{ event.method }}</span>
                <span class="mono ct">{{ event.contentType ?? '—' }}</span>
              </span>
              <span class="muted small">{{ relativeTime(event.receivedAt) }}</span>
            </RouterLink>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<style scoped>
.head {
  margin-top: 8px;
}

.url {
  flex: 1;
  padding: 8px 11px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  word-break: break-all;
}

.columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  align-items: start;
}

@media (max-width: 820px) {
  .columns {
    grid-template-columns: 1fr;
  }
}

.feed {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.feed li {
  border-bottom: 1px solid var(--border);
}

.feed li:last-child {
  border-bottom: none;
}

.feed a {
  padding: 10px 4px;
}

.feed a:hover {
  color: var(--accent);
}

.method {
  font-weight: 700;
  font-size: 12px;
  color: var(--accent);
}

.ct {
  font-size: 12px;
  color: var(--muted);
}

.small {
  font-size: 12px;
}

.live {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--muted);
}

.live .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--muted);
}

.live.live .dot {
  background: var(--green);
  box-shadow: 0 0 8px var(--green);
}
</style>
