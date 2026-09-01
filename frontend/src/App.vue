<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/lib/theme'
import AppIcon from '@/components/AppIcon.vue'
import ToastHost from '@/components/ToastHost.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const auth = useAuthStore()
const router = useRouter()
const signedIn = computed(() => auth.apiKey !== null)
const { theme, toggle } = useTheme()

function signOut() {
  auth.signOut()
  router.push({ name: 'sign-in' })
}
</script>

<template>
  <header class="topbar">
    <div class="left">
      <RouterLink to="/" class="brand">
        <span class="dot" />
        Relay
      </RouterLink>
      <nav v-if="signedIn" class="nav">
        <RouterLink :to="{ name: 'overview' }" class="nav-link">Overview</RouterLink>
        <RouterLink :to="{ name: 'endpoints' }" class="nav-link">Endpoints</RouterLink>
      </nav>
    </div>
    <div class="row">
      <button
        class="ghost icon-button toggle"
        :aria-label="theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'"
        @click="toggle"
      >
        <AppIcon :name="theme === 'dark' ? 'sun' : 'moon'" :size="16" />
      </button>
      <button v-if="signedIn" class="ghost" @click="signOut">Sign out</button>
    </div>
  </header>
  <main class="page">
    <RouterView />
  </main>
  <ToastHost />
  <ConfirmDialog />
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 28px;
  border-bottom: 1px solid var(--border);
  background: color-mix(in srgb, var(--surface) 82%, transparent);
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 10;
}

.left {
  display: flex;
  align-items: center;
  gap: var(--space-6);
}

.brand {
  display: flex;
  align-items: center;
  gap: 9px;
  font-weight: 700;
  font-size: var(--text-lg);
  letter-spacing: 0.2px;
}

.brand .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 12px var(--accent);
}

.nav {
  display: flex;
  gap: var(--space-1);
}

.nav-link {
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  color: var(--muted);
  font-weight: 550;
  transition: color var(--dur-fast) var(--ease-standard),
    background var(--dur-fast) var(--ease-standard);
}

.nav-link:hover {
  color: var(--text);
  background: var(--surface-2);
}

.nav-link.router-link-active {
  color: var(--accent);
  background: var(--accent-weak);
}

.toggle {
  padding: 7px;
}

.page {
  max-width: 1040px;
  margin: 0 auto;
  padding: 28px;
}

@media (max-width: 640px) {
  .topbar {
    padding: 12px 16px;
  }

  .left {
    gap: var(--space-3);
  }

  .page {
    padding: 20px 16px;
  }
}
</style>
