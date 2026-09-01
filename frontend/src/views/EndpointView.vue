<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import type { Endpoint, EventSummary } from '@/api/types'
import { relativeTime } from '@/lib/format'
import { useEventStream } from '@/lib/live'
import { pushToast } from '@/lib/toast'
import { confirmDialog } from '@/lib/confirm'
import CopyButton from '@/components/CopyButton.vue'
import DestinationsPanel from '@/components/DestinationsPanel.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonBlock from '@/components/SkeletonBlock.vue'
import AppIcon from '@/components/AppIcon.vue'

const props = defineProps<{ id: string }>()
const router = useRouter()

const endpoint = ref<Endpoint | null>(null)
const events = ref<EventSummary[]>([])
const loading = ref(true)

const ingestUrl = computed(() =>
  endpoint.value ? window.location.origin + endpoint.value.ingestPath : '',
)

async function load() {
  try {
    const [meta, page] = await Promise.all([
      api.getEndpoint(props.id),
      api.listEvents(props.id, 0, 50),
    ])
    endpoint.value = meta
    events.value = page.items
  } catch {
    pushToast('Could not load this endpoint.', 'error')
  } finally {
    loading.value = false
  }
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
  try {
    endpoint.value = await api.setEndpointActive(endpoint.value.id, !endpoint.value.active)
    pushToast(endpoint.value.active ? 'Endpoint resumed' : 'Endpoint paused', 'success')
  } catch {
    pushToast('Could not update the endpoint.', 'error')
  }
}

async function remove() {
  if (!endpoint.value) return
  const ok = await confirmDialog({
    title: 'Delete endpoint?',
    message: `"${endpoint.value.name}" and all of its captured events will be permanently removed.`,
    confirmLabel: 'Delete',
    danger: true,
  })
  if (!ok) return
  try {
    await api.deleteEndpoint(endpoint.value.id)
    pushToast('Endpoint deleted', 'success')
    router.push({ name: 'endpoints' })
  } catch {
    pushToast('Could not delete the endpoint.', 'error')
  }
}

function methodClass(method: string): string {
  return `m-${method.toLowerCase()}`
}

onMounted(load)
</script>

<template>
  <div v-if="loading && !endpoint" class="stack">
    <SkeletonBlock width="140px" height="14px" />
    <SkeletonBlock width="40%" height="28px" />
    <div class="card"><SkeletonBlock width="70%" height="16px" /></div>
    <div class="columns">
      <div class="card"><SkeletonBlock height="180px" /></div>
      <div class="card"><SkeletonBlock height="180px" /></div>
    </div>
  </div>

  <div v-else-if="endpoint" class="stack">
    <div>
      <RouterLink :to="{ name: 'endpoints' }" class="muted back">
        <AppIcon name="arrowLeft" :size="15" />
        Endpoints
      </RouterLink>
      <div class="spread head">
        <h1>{{ endpoint.name }}</h1>
        <div class="row">
          <button class="ghost icon-button" @click="toggleActive">
            <AppIcon :name="endpoint.active ? 'pause' : 'play'" :size="15" />
            <span>{{ endpoint.active ? 'Pause' : 'Resume' }}</span>
          </button>
          <button class="ghost danger icon-button" @click="remove">
            <AppIcon name="trash" :size="15" />
            <span>Delete</span>
          </button>
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

        <EmptyState
          v-if="events.length === 0"
          title="Waiting for events"
          hint="Send a request to the ingest URL and it will appear here instantly."
        />
        <ul v-else class="feed">
          <li v-for="event in events" :key="event.id">
            <RouterLink :to="{ name: 'event', params: { id: event.id } }" class="spread">
              <span class="row">
                <span class="method" :class="methodClass(event.method)">{{ event.method }}</span>
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
.back {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.back:hover {
  color: var(--text);
}

.head {
  margin-top: 8px;
}

.url {
  flex: 1;
  padding: 8px 11px;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
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
  border-radius: var(--radius-sm);
  transition: background var(--dur-fast) var(--ease-standard);
}

.feed a:hover {
  background: var(--surface-2);
}

.method {
  font-weight: 700;
  font-size: var(--text-xs);
  letter-spacing: 0.03em;
  padding: 2px 7px;
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  color: var(--muted);
}

.m-get {
  color: var(--status-good);
  background: color-mix(in srgb, var(--status-good) 14%, transparent);
}

.m-post {
  color: var(--accent);
  background: var(--accent-weak);
}

.m-put,
.m-patch {
  color: var(--status-warning);
  background: color-mix(in srgb, var(--status-warning) 14%, transparent);
}

.m-delete {
  color: var(--status-critical);
  background: color-mix(in srgb, var(--status-critical) 14%, transparent);
}

.ct {
  font-size: var(--text-xs);
  color: var(--muted);
}

.small {
  font-size: var(--text-xs);
}

.live {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--text-xs);
  color: var(--muted);
}

.live .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--muted);
}

.live.live .dot {
  background: var(--status-good);
  box-shadow: 0 0 8px var(--status-good);
}
</style>
