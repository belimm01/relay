<script setup lang="ts">
import { computed } from 'vue'
import type { DeliveryStatus } from '@/api/types'

const props = defineProps<{ status: DeliveryStatus }>()

const label = computed(() => props.status.toLowerCase())
</script>

<template>
  <span class="badge" :class="status.toLowerCase()">{{ label }}</span>
</template>

<style scoped>
.badge {
  display: inline-block;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  text-transform: capitalize;
  border: 1px solid transparent;
}

.delivered {
  color: var(--green);
  background: rgba(62, 207, 142, 0.12);
  border-color: rgba(62, 207, 142, 0.35);
}

.pending,
.retrying {
  color: var(--amber);
  background: rgba(245, 166, 35, 0.12);
  border-color: rgba(245, 166, 35, 0.35);
}

.failed {
  color: var(--red);
  background: rgba(239, 95, 107, 0.12);
  border-color: rgba(239, 95, 107, 0.35);
}
</style>
