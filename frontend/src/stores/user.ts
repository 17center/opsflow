import { defineStore } from 'pinia'
import { getToken, setToken, clearToken } from '@/utils/request'
import type { UserInfo } from '@/types/api'

interface UserState {
  token: string | null
  userInfo: UserInfo | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getToken(),
    userInfo: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    permissions: (state) => state.userInfo?.permissions ?? []
  },

  actions: {
    /** 保存登录信息 */
    setLoginInfo(token: string, refreshToken: string, userInfo: UserInfo) {
      setToken(token, refreshToken)
      this.token = token
      this.userInfo = userInfo
    },

    /** 登出 */
    logout() {
      clearToken()
      this.token = null
      this.userInfo = null
    }
  }
})