import { ref } from 'vue'

export type ToastKind = 'success' | 'error' | 'info'

export interface Toast {
  id: number
  message: string
  kind: ToastKind
}

const toasts = ref<Toast[]>([])
let seq = 0

export function useToasts() {
  return { toasts }
}

export function dismissToast(id: number) {
  toasts.value = toasts.value.filter((t) => t.id !== id)
}

export function pushToast(message: string, kind: ToastKind = 'info', timeout = 3800) {
  const id = ++seq
  toasts.value.push({ id, message, kind })
  if (timeout > 0) {
    window.setTimeout(() => dismissToast(id), timeout)
  }
  return id
}
