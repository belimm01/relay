import { ref } from 'vue'

export interface ConfirmOptions {
  title: string
  message?: string
  confirmLabel?: string
  cancelLabel?: string
  danger?: boolean
}

interface PendingConfirm extends ConfirmOptions {
  resolve: (value: boolean) => void
}

const current = ref<PendingConfirm | null>(null)

export function useConfirm() {
  return { current }
}

export function confirmDialog(options: ConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    current.value = { ...options, resolve }
  })
}

export function resolveConfirm(value: boolean) {
  current.value?.resolve(value)
  current.value = null
}
