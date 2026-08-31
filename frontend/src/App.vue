<script setup lang="ts">
import { RouterView, useRouter } from 'vue-router'
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const signedIn = computed(() => auth.apiKey !== null)

function signOut() {
  auth.signOut()
  router.push({ name: 'sign-in' })
}
</script>

<template>
  <header class="topbar">
    <RouterLink to="/" class="brand">
      <span class="dot" />
      Relay
    </RouterLink>
    <button v-if="signedIn" class="ghost" @click="signOut">Sign out</button>
  </header>
  <main class="page">
    <RouterView />
  </main>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 28px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand {
  display: flex;
  align-items: center;
  gap: 9px;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 0.3px;
}

.brand .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 12px var(--accent);
}

.page {
  max-width: 1040px;
  margin: 0 auto;
  padding: 28px;
}
</style>
