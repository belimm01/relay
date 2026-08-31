<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{ value: string; label?: string }>()
const copied = ref(false)

async function copy() {
  await navigator.clipboard.writeText(props.value)
  copied.value = true
  window.setTimeout(() => (copied.value = false), 1200)
}
</script>

<template>
  <button class="ghost" @click="copy">
    {{ copied ? 'Copied' : (label ?? 'Copy') }}
  </button>
</template>
