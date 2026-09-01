<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/api/client'
import type { EventDetail } from '@/api/types'
import { formatTime, prettyJson } from '@/lib/format'
import { pushToast } from '@/lib/toast'
import StatusBadge from '@/components/StatusBadge.vue'
import SkeletonBlock from '@/components/SkeletonBlock.vue'
import AppIcon from '@/components/AppIcon.vue'

const props = defineProps<{ id: string }>()

const event = ref<EventDetail | null>(null)
const loading = ref(true)
const replaying = ref(false)
const replayNote = ref<string | null>(null)

async function load() {
  try {
    event.value = await api.getEvent(props.id)
  } catch {
    pushToast('Could not load this event.', 'error')
  } finally {
    loading.value = false
  }
}

async function replay() {
  replaying.value = true
  replayNote.value = null
  try {
    const result = await api.replayEvent(props.id)
    replayNote.value = `Re-queued ${result.queued} delivery(ies).`
    pushToast(`Re-queued ${result.queued} delivery(ies)`, 'success')
    await load()
  } catch {
    pushToast('Could not replay this event.', 'error')
  } finally {
    replaying.value = false
  }
}

function methodClass(method: string): string {
  return `m-${method.toLowerCase()}`
}

onMounted(load)
</script>

<template>
  <div v-if="loading && !event" class="stack">
    <SkeletonBlock width="120px" height="14px" />
    <SkeletonBlock width="45%" height="28px" />
    <div class="columns">
      <div class="card"><SkeletonBlock height="200px" /></div>
      <div class="card"><SkeletonBlock height="200px" /></div>
    </div>
  </div>

  <div v-else-if="event" class="stack">
    <div>
      <RouterLink
        :to="{ name: 'endpoint', params: { id: event.endpointId } }"
        class="muted back"
      >
        <AppIcon name="arrowLeft" :size="15" />
        Endpoint
      </RouterLink>
      <div class="spread head">
        <h1>
          <span class="method" :class="methodClass(event.method)">{{ event.method }}</span>
          {{ event.path }}
        </h1>
        <button class="primary icon-button" :disabled="replaying" @click="replay">
          <AppIcon name="replay" :size="15" />
          <span>{{ replaying ? 'Replaying…' : 'Replay' }}</span>
        </button>
      </div>
      <p class="muted">
        Received {{ formatTime(event.receivedAt) }}
        <template v-if="event.sourceIp"> · from {{ event.sourceIp }}</template>
      </p>
      <p v-if="replayNote" class="note">{{ replayNote }}</p>
    </div>

    <div class="columns">
      <section class="card stack">
        <h2>Request</h2>
        <div>
          <label>Headers</label>
          <table class="headers">
            <tbody>
              <tr v-for="(value, key) in event.headers" :key="key">
                <td class="mono hkey">{{ key }}</td>
                <td class="mono">{{ value }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div>
          <label>Body</label>
          <pre v-if="event.body">{{ prettyJson(event.body) }}</pre>
          <p v-else class="muted">Empty body</p>
        </div>
      </section>

      <section class="stack">
        <h2>Deliveries</h2>
        <p v-if="event.deliveries.length === 0" class="muted">
          No destinations matched this event.
        </p>

        <article
          v-for="delivery in event.deliveries"
          :key="delivery.id"
          class="card stack"
        >
          <div class="spread">
            <div>
              <strong>{{ delivery.destinationName }}</strong>
              <div class="mono url">{{ delivery.targetUrl }}</div>
            </div>
            <StatusBadge :status="delivery.status" />
          </div>

          <ol class="timeline">
            <li
              v-for="attempt in delivery.attempts"
              :key="attempt.attemptNo"
              :class="attempt.succeeded ? 'ok' : 'fail'"
            >
              <div class="spread">
                <span>
                  Attempt {{ attempt.attemptNo }}
                  <span v-if="attempt.responseCode" class="mono code">
                    {{ attempt.responseCode }}
                  </span>
                </span>
                <span class="muted small">
                  <template v-if="attempt.latencyMs != null">
                    {{ attempt.latencyMs }}ms ·
                  </template>
                  {{ formatTime(attempt.attemptedAt) }}
                </span>
              </div>
              <div v-if="attempt.errorMessage" class="err">
                {{ attempt.errorMessage }}
              </div>
            </li>
          </ol>

          <p
            v-if="delivery.status === 'RETRYING' && delivery.nextAttemptAt"
            class="muted small"
          >
            Next attempt {{ formatTime(delivery.nextAttemptAt) }}
          </p>
        </article>
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

.method {
  font-family: var(--mono);
  font-size: var(--text-base);
  font-weight: 700;
  padding: 2px 8px;
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

.note {
  color: var(--status-good);
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

.headers {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--text-sm);
}

.headers td {
  padding: 5px 8px;
  border-bottom: 1px solid var(--border);
  vertical-align: top;
  word-break: break-all;
}

.hkey {
  color: var(--muted);
  white-space: nowrap;
  width: 1%;
}

.url {
  font-size: var(--text-sm);
  color: var(--muted);
  word-break: break-all;
  margin-top: 3px;
}

.code {
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  background: var(--surface-2);
}

.timeline {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.timeline li {
  padding: 9px 11px;
  border-left: 3px solid var(--border);
  background: var(--surface-2);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.timeline li.ok {
  border-left-color: var(--status-good);
}

.timeline li.fail {
  border-left-color: var(--status-critical);
}

.err {
  color: var(--status-critical);
  font-size: var(--text-xs);
  margin-top: 4px;
  font-family: var(--mono);
}

.small {
  font-size: var(--text-xs);
}
</style>
