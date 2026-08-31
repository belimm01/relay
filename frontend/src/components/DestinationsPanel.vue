<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api/client'
import type { Destination } from '@/api/types'

const props = defineProps<{ endpointId: string }>()

const destinations = ref<Destination[]>([])
const loading = ref(true)
const showForm = ref(false)
const saving = ref(false)
const draft = reactive({ name: '', targetUrl: '', filterExpr: '' })

async function load() {
  destinations.value = await api.listDestinations(props.endpointId)
  loading.value = false
}

async function create() {
  if (!draft.name.trim() || !draft.targetUrl.trim()) return
  saving.value = true
  try {
    const created = await api.createDestination(props.endpointId, {
      name: draft.name.trim(),
      targetUrl: draft.targetUrl.trim(),
      filterExpr: draft.filterExpr.trim() || null,
    })
    destinations.value.push(created)
    draft.name = ''
    draft.targetUrl = ''
    draft.filterExpr = ''
    showForm.value = false
  } finally {
    saving.value = false
  }
}

async function toggle(destination: Destination) {
  const updated = await api.updateDestination(destination.id, {
    name: destination.name,
    targetUrl: destination.targetUrl,
    filterExpr: destination.filterExpr,
    active: !destination.active,
  })
  Object.assign(destination, updated)
}

async function remove(destination: Destination) {
  await api.deleteDestination(destination.id)
  destinations.value = destinations.value.filter((d) => d.id !== destination.id)
}

onMounted(load)
</script>

<template>
  <section class="card stack">
    <div class="spread">
      <h2>Destinations</h2>
      <button class="ghost" @click="showForm = !showForm">
        {{ showForm ? 'Cancel' : 'Add destination' }}
      </button>
    </div>

    <form v-if="showForm" class="stack" @submit.prevent="create">
      <div class="field">
        <label>Name</label>
        <input v-model="draft.name" placeholder="Orders service" />
      </div>
      <div class="field">
        <label>Target URL</label>
        <input v-model="draft.targetUrl" class="mono" placeholder="https://…" />
      </div>
      <div class="field">
        <label>Filter (optional)</label>
        <input
          v-model="draft.filterExpr"
          class="mono"
          placeholder='body.type == "order.created"'
        />
      </div>
      <button class="primary" type="submit" :disabled="saving">Save destination</button>
    </form>

    <p v-if="loading" class="muted">Loading…</p>
    <p v-else-if="destinations.length === 0" class="muted">
      No destinations. Add one to start forwarding events.
    </p>

    <ul v-else class="list">
      <li v-for="destination in destinations" :key="destination.id">
        <div class="spread">
          <div>
            <strong>{{ destination.name }}</strong>
            <span v-if="!destination.active" class="muted"> · paused</span>
            <div class="mono url">{{ destination.targetUrl }}</div>
            <code v-if="destination.filterExpr" class="filter">{{
              destination.filterExpr
            }}</code>
          </div>
          <div class="row">
            <button class="ghost" @click="toggle(destination)">
              {{ destination.active ? 'Pause' : 'Resume' }}
            </button>
            <button class="ghost danger" @click="remove(destination)">Delete</button>
          </div>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list li {
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
}

.url {
  font-size: 13px;
  color: var(--muted);
  word-break: break-all;
  margin-top: 3px;
}

.filter {
  display: inline-block;
  margin-top: 6px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--accent-weak);
  color: var(--accent);
}
</style>
