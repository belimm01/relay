<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { resolveConfirm, useConfirm } from '@/lib/confirm'

const { current } = useConfirm()
const confirmButton = ref<HTMLButtonElement | null>(null)

watch(current, async (value) => {
  if (value) {
    await nextTick()
    confirmButton.value?.focus()
  }
})

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    resolveConfirm(false)
  }
}
</script>

<template>
  <Transition name="fade">
    <div
      v-if="current"
      class="overlay"
      @click.self="resolveConfirm(false)"
      @keydown="onKeydown"
    >
      <div
        class="dialog card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
      >
        <h2 id="confirm-title">{{ current.title }}</h2>
        <p v-if="current.message" class="muted message">{{ current.message }}</p>
        <div class="actions">
          <button class="ghost" @click="resolveConfirm(false)">
            {{ current.cancelLabel ?? 'Cancel' }}
          </button>
          <button
            ref="confirmButton"
            :class="current.danger ? 'primary confirm-danger' : 'primary'"
            @click="resolveConfirm(true)"
          >
            {{ current.confirmLabel ?? 'Confirm' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
  background: var(--overlay);
  backdrop-filter: blur(2px);
}

.dialog {
  width: min(420px, 100%);
  box-shadow: var(--shadow-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  animation: dialog-in var(--dur-base) var(--ease-emphasized);
}

.message {
  margin: 0;
  line-height: var(--leading);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.confirm-danger {
  background: var(--red);
  border-color: var(--red);
  color: #fff;
}

.confirm-danger:hover:not(:disabled) {
  background: color-mix(in srgb, var(--red) 82%, #000);
  border-color: color-mix(in srgb, var(--red) 82%, #000);
}

@keyframes dialog-in {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
  }
}

@media (prefers-reduced-motion: reduce) {
  .dialog {
    animation: none;
  }
}
</style>
