<script setup lang="ts">
defineProps<{ title: string; hint?: string }>()
</script>

<template>
  <div class="empty">
    <p class="title">{{ title }}</p>
    <p v-if="hint" class="muted">{{ hint }}</p>
    <slot />
  </div>
</template>

<style scoped>
.empty {
  text-align: center;
  padding: 40px 20px;
  border: 1px dashed var(--border);
  border-radius: var(--radius);
}

.title {
  margin: 0 0 6px;
  font-weight: 600;
}
</style>
