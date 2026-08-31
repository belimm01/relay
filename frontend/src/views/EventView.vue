<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/api/client'
import type { EventDetail } from '@/api/types'
import { formatTime, prettyJson } from '@/lib/format'
import StatusBadge from '@/components/StatusBadge.vue'

const props = defineProps<{ id: string }>()

const event = ref<EventDetail | null>(null)
const loading = ref(true)
const replaying = ref(false)
const replayNote = ref<string | null>(null)

async function load() {
  event.value = await api.getEvent(props.id)
  loading.value = false
}

async function replay() {
  replaying.value = true
  replayNote.value = null
  try {
    const result = await api.replayEvent(props.id)
    replayNote.value = `Re-queued ${result.queued} delivery(ies).`
    await load()
  } finally {
    replaying.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-if="event" class="stack">
    <div>
      <RouterLink
        :to="{ name: 'endpoint', params: { id: event.endpointId } }"
        class="muted"
      >
        ← Endpoint
      </RouterLink>
      <div class="spread head">
        <h1>
          <span class="method">{{ event.method }}</span>
          {{ event.path }}
        </h1>
        <button class="primary" :disabled="replaying" @click="replay">
          {{ replaying ? 'Replaying…' : 'Replay' }}
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
                  <span v-if="attempt.responseCode" class="mono">
                    → {{ attempt.responseCode }}
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
.head {
  margin-top: 8px;
}

.method {
  color: var(--accent);
  font-family: var(--mono);
}

.note {
  color: var(--green);
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
  font-size: 13px;
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
  font-size: 13px;
  color: var(--muted);
  word-break: break-all;
  margin-top: 3px;
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
  border-radius: 0 8px 8px 0;
}

.timeline li.ok {
  border-left-color: var(--green);
}

.timeline li.fail {
  border-left-color: var(--red);
}

.err {
  color: var(--red);
  font-size: 12px;
  margin-top: 4px;
  font-family: var(--mono);
}

.small {
  font-size: 12px;
}
</style>
