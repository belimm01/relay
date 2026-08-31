<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/api/client'
import type { Endpoint } from '@/api/types'
import { relativeTime } from '@/lib/format'
import EmptyState from '@/components/EmptyState.vue'

const endpoints = ref<Endpoint[]>([])
const loading = ref(true)
const name = ref('')
const creating = ref(false)

async function load() {
  endpoints.value = await api.listEndpoints()
  loading.value = false
}

async function create() {
  if (!name.value.trim()) return
  creating.value = true
  try {
    const created = await api.createEndpoint(name.value.trim())
    endpoints.value.unshift(created)
    name.value = ''
  } finally {
    creating.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <div class="spread">
      <h1>Endpoints</h1>
    </div>

    <form class="card row" @submit.prevent="create">
      <input v-model="name" placeholder="New endpoint name, e.g. Stripe webhooks" />
      <button class="primary" type="submit" :disabled="creating || !name.trim()">
        Create
      </button>
    </form>

    <p v-if="loading" class="muted">Loading…</p>

    <EmptyState
      v-else-if="endpoints.length === 0"
      title="No endpoints yet"
      hint="Create one above to get a unique URL you can point webhooks at."
    />

    <div v-else class="grid">
      <RouterLink
        v-for="endpoint in endpoints"
        :key="endpoint.id"
        :to="{ name: 'endpoint', params: { id: endpoint.id } }"
        class="card endpoint"
      >
        <div class="spread">
          <h3>{{ endpoint.name }}</h3>
          <span :class="['pill', endpoint.active ? 'on' : 'off']">
            {{ endpoint.active ? 'active' : 'paused' }}
          </span>
        </div>
        <code class="muted path">{{ endpoint.ingestPath }}</code>
        <span class="muted small">created {{ relativeTime(endpoint.createdAt) }}</span>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.endpoint {
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: border-color 0.15s ease;
}

.endpoint:hover {
  border-color: var(--accent);
}

.path {
  font-size: 13px;
  word-break: break-all;
}

.small {
  font-size: 12px;
}

.pill {
  font-size: 12px;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--border);
}

.pill.on {
  color: var(--green);
  border-color: rgba(62, 207, 142, 0.4);
}

.pill.off {
  color: var(--muted);
}
</style>
