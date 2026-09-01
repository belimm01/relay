<script setup lang="ts">
withDefaults(defineProps<{ width?: string; height?: string; radius?: string }>(), {
  width: '100%',
  height: '16px',
  radius: 'var(--radius-sm)',
})
</script>

<template>
  <span
    class="skeleton block"
    :style="{ width, height, borderRadius: radius }"
    aria-hidden="true"
  />
</template>

<style scoped>
.block {
  display: block;
}
</style>
