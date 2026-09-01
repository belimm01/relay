<script setup lang="ts">
import { computed } from 'vue'
import type { DeliveryStatus } from '@/api/types'

const props = defineProps<{ status: DeliveryStatus }>()

const label = computed(() => props.status.toLowerCase())
</script>

<template>
  <span class="badge" :class="status.toLowerCase()">
    <span class="dot" />
    {{ label }}
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  font-size: var(--text-xs);
  font-weight: 600;
  text-transform: capitalize;
  border: 1px solid transparent;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.delivered {
  color: var(--status-good);
  background: color-mix(in srgb, var(--status-good) 14%, transparent);
  border-color: color-mix(in srgb, var(--status-good) 38%, transparent);
}

.pending {
  color: var(--status-warning);
  background: color-mix(in srgb, var(--status-warning) 14%, transparent);
  border-color: color-mix(in srgb, var(--status-warning) 38%, transparent);
}

.retrying {
  color: var(--status-serious);
  background: color-mix(in srgb, var(--status-serious) 14%, transparent);
  border-color: color-mix(in srgb, var(--status-serious) 38%, transparent);
}

.failed {
  color: var(--status-critical);
  background: color-mix(in srgb, var(--status-critical) 14%, transparent);
  border-color: color-mix(in srgb, var(--status-critical) 38%, transparent);
}
</style>
