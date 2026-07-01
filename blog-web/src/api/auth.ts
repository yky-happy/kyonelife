import request from '../utils/request'

export interface UserInfo {
  id: number
  account: string
  nickname: string
  avatar: string | null
  email: string
}

export interface AuthResult {
  token: string
  user: UserInfo
}

export const sendEmailCode = (email: string) =>
  request.post<unknown, void>('/api/auth/email/code', { email })

export const register = (data: { email: string; code: string; password: string; nickname?: string }) =>
  request.post<unknown, AuthResult>('/api/auth/register', data)

export const login = (identifier: string, password: string) =>
  request.post<unknown, AuthResult>('/api/auth/login', { identifier, password })

export const resetPassword = (email: string, code: string, password: string) =>
  request.post<unknown, void>('/api/auth/reset-password', { email, code, password })

export const getMe = () => request.get<unknown, UserInfo | null>('/api/auth/me')

export const logout = () => request.post<unknown, void>('/api/auth/logout')
