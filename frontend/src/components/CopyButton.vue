<script setup lang="ts">
import { ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { pushToast } from '@/lib/toast'

const props = defineProps<{ value: string; label?: string }>()
const copied = ref(false)

async function copy() {
  try {
    await navigator.clipboard.writeText(props.value)
    copied.value = true
    pushToast('Copied to clipboard', 'success', 1800)
    window.setTimeout(() => (copied.value = false), 1200)
  } catch {
    pushToast('Could not copy to clipboard', 'error')
  }
}
</script>

<template>
  <button class="ghost icon-button" :aria-label="label ?? 'Copy to clipboard'" @click="copy">
    <AppIcon :name="copied ? 'check' : 'copy'" :size="15" />
    <span>{{ copied ? 'Copied' : (label ?? 'Copy') }}</span>
  </button>
</template>
