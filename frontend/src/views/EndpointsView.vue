<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/api/client'
import type { Endpoint } from '@/api/types'
import { relativeTime } from '@/lib/format'
import { pushToast } from '@/lib/toast'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonBlock from '@/components/SkeletonBlock.vue'
import AppIcon from '@/components/AppIcon.vue'

const endpoints = ref<Endpoint[]>([])
const loading = ref(true)
const name = ref('')
const creating = ref(false)

async function load() {
  try {
    endpoints.value = await api.listEndpoints()
  } catch {
    pushToast('Could not load endpoints.', 'error')
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!name.value.trim()) return
  creating.value = true
  try {
    const created = await api.createEndpoint(name.value.trim())
    endpoints.value.unshift(created)
    name.value = ''
    pushToast(`Created "${created.name}"`, 'success')
  } catch {
    pushToast('Could not create endpoint.', 'error')
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
      <button class="primary icon-button" type="submit" :disabled="creating || !name.trim()">
        <AppIcon name="plus" :size="15" />
        <span>Create</span>
      </button>
    </form>

    <div v-if="loading" class="grid">
      <div v-for="n in 3" :key="n" class="card stack">
        <SkeletonBlock width="50%" height="16px" />
        <SkeletonBlock width="80%" height="13px" />
        <SkeletonBlock width="30%" height="12px" />
      </div>
    </div>

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
  transition: border-color var(--dur-fast) var(--ease-standard),
    transform var(--dur-fast) var(--ease-standard),
    box-shadow var(--dur-fast) var(--ease-standard);
}

.endpoint:hover {
  border-color: var(--accent);
  transform: translateY(-2px);
  box-shadow: var(--shadow-2);
}

.path {
  font-size: var(--text-sm);
  word-break: break-all;
}

.small {
  font-size: var(--text-xs);
}

.pill {
  display: inline-flex;
  align-items: center;
  font-size: var(--text-xs);
  padding: 2px 9px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border);
}

.pill.on {
  color: var(--status-good);
  border-color: color-mix(in srgb, var(--status-good) 40%, transparent);
  background: color-mix(in srgb, var(--status-good) 12%, transparent);
}

.pill.off {
  color: var(--muted);
}
</style>
