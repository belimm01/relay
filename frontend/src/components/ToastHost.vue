<script setup lang="ts">
import { dismissToast, useToasts } from '@/lib/toast'
import AppIcon from '@/components/AppIcon.vue'
import type { IconName } from '@/lib/icons'

const { toasts } = useToasts()

const iconFor: Record<string, IconName> = {
  success: 'check',
  error: 'alert',
  info: 'activity',
}
</script>

<template>
  <div class="toast-host" aria-live="polite" aria-atomic="false">
    <TransitionGroup name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast"
        :class="toast.kind"
        role="status"
      >
        <AppIcon :name="iconFor[toast.kind]" :size="16" />
        <span class="msg">{{ toast.message }}</span>
        <button
          class="ghost dismiss"
          aria-label="Dismiss notification"
          @click="dismissToast(toast.id)"
        >
          <AppIcon name="close" :size="14" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed;
  right: var(--space-5);
  bottom: var(--space-5);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  max-width: min(360px, calc(100vw - 32px));
}

.toast {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 10px 12px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-left: 3px solid var(--accent);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-2);
  font-size: var(--text-sm);
}

.toast.success {
  border-left-color: var(--status-good);
  color: var(--status-good);
}

.toast.error {
  border-left-color: var(--status-critical);
  color: var(--status-critical);
}

.toast.info {
  border-left-color: var(--accent);
  color: var(--accent);
}

.msg {
  flex: 1;
  color: var(--text);
}

.dismiss {
  border: none;
  background: transparent;
  color: var(--muted);
  padding: 2px;
}

.dismiss:hover {
  color: var(--text);
}

.toast-enter-active,
.toast-leave-active {
  transition: transform var(--dur-base) var(--ease-emphasized),
    opacity var(--dur-base) var(--ease-standard);
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(24px);
}
</style>
