import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { http } from '@/api/http'

const TOKEN_KEY = 'sk_token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')

  const isLoggedIn = computed(() => !!token.value)

  function setToken(t) {
    token.value = t
    if (t) localStorage.setItem(TOKEN_KEY, t)
    else localStorage.removeItem(TOKEN_KEY)
  }

  async function login(username, password) {
    const { data } = await http.post('/api/auth/login', { username, password })
    setToken(data.token)
    return data
  }

  async function register(username, password) {
    const { data } = await http.post('/api/auth/register', { username, password })
    setToken(data.token)
    return data
  }

  function logout() {
    setToken('')
  }

  return { token, isLoggedIn, setToken, login, register, logout }
})
