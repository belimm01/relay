import { createRouter, createWebHistory } from 'vue-router'
import { getApiKey } from '@/api/session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'endpoints',
      component: () => import('@/views/EndpointsView.vue'),
    },
    {
      path: '/endpoints/:id',
      name: 'endpoint',
      component: () => import('@/views/EndpointView.vue'),
      props: true,
    },
    {
      path: '/events/:id',
      name: 'event',
      component: () => import('@/views/EventView.vue'),
      props: true,
    },
    {
      path: '/sign-in',
      name: 'sign-in',
      component: () => import('@/views/SignInView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  if (to.name !== 'sign-in' && !getApiKey()) {
    return { name: 'sign-in', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
