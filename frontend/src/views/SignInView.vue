<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, ApiError } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const key = ref('')
const error = ref<string | null>(null)
const busy = ref(false)

async function submit() {
  error.value = null
  busy.value = true
  auth.signIn(key.value)
  try {
    // Validate the key against a guarded endpoint before committing to the app.
    await api.listEndpoints()
    const redirect = (route.query.redirect as string) ?? '/'
    router.push(redirect)
  } catch (e) {
    auth.signOut()
    error.value =
      e instanceof ApiError && e.status === 401
        ? 'That API key was not accepted.'
        : 'Could not reach the Relay backend.'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <div class="card stack">
      <div>
        <h1>Sign in to Relay</h1>
        <p class="muted">
          Paste the API key printed in the backend log on first start.
        </p>
      </div>
      <form @submit.prevent="submit">
        <div class="field">
          <label for="key">API key</label>
          <input
            id="key"
            v-model="key"
            class="mono"
            type="password"
            placeholder="rk_…"
            autofocus
          />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button class="primary" type="submit" :disabled="busy || !key">
          {{ busy ? 'Checking…' : 'Continue' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 400px;
  margin: 8vh auto 0;
}

.error {
  color: var(--red);
  margin: 0 0 12px;
}
</style>
