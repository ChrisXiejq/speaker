import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
  },
  {
    path: '/practice',
    name: 'practice',
    component: () => import('@/views/PracticeView.vue'),
  },
  {
    path: '/history',
    name: 'history',
    component: () => import('@/views/HistoryView.vue'),
  },
  {
    path: '/bank',
    name: 'bank',
    component: () => import('@/views/BankView.vue'),
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/AdminHomeView.vue'),
  },
  {
    path: '/admin/bank',
    name: 'admin-bank',
    component: () => import('@/views/AdminBankView.vue'),
  },
  {
    path: '/admin/bank/manage',
    name: 'admin-bank-manage',
    component: () => import('@/views/AdminBankManageView.vue'),
  },
  {
    path: '/session/:id',
    name: 'session-detail',
    component: () => import('@/views/SessionDetailView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
