import request, { type ApiResponse } from './request'

export interface AdminProfile {
  userId: number
  username: string
  nickname: string
  avatar: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  adminInfo: AdminProfile
}

export function login(data: LoginPayload) {
  return request.post<ApiResponse<LoginResult>>('/auth/login', data)
}

export function logout() {
  return request.post<ApiResponse<null>>('/auth/logout')
}

export function getInfo() {
  return request.get<ApiResponse<AdminProfile>>('/auth/info')
}

export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put<ApiResponse<null>>('/auth/password', data)
}
